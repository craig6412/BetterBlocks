package com.betterblocks

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Google Play Billing connection, purchase flow, and acknowledgement.
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onCoinsPurchased: (Int) -> Unit, // Callback to give coins to user
    private val isTestMode: Boolean = BuildConfig.DEBUG // Debug-only bypass for local testing
) {

    // Exposed product details for UI
    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap = _productDetailsMap.asStateFlow()

    // Product IDs (must EXACTLY match Play Console)
    private val productIds = listOf(
        "coins_small",
        "coins_medium",
        "coins_large",
        "coins_mega"
    )
    object CoinPacks {
        val amounts = mapOf(
            "coins_small" to 3_000,
            "coins_medium" to 25_000,
            "coins_large" to 100_000,
            "coins_mega" to 400_000
        )
    }
    // Purchase update listener
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    // -----------------------------
    // CONNECTION
    // -----------------------------

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryAvailableProducts()
                    queryExistingPurchases() // 🔒 CRITICAL FIX
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    // -----------------------------
    // PRODUCT QUERIES
    // -----------------------------

    private fun queryAvailableProducts() {
        val products = productIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingResponseCode.OK) {
                _productDetailsMap.value = list.associateBy { it.productId }
                Log.d(TAG, "Loaded ${list.size} product details")
            } else {
                Log.e(TAG, "Product query failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Handles purchases that already exist (crash recovery / reinstall safety)
     */
    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    // -----------------------------
    // PURCHASE FLOW
    // -----------------------------

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        // Test-mode bypass: immediately grant coins without contacting Play Billing
        if (isTestMode) {
            val coins = CoinPacks.amounts[productId] ?: 0
            Log.d(TAG, "Test mode: granting $coins coins for $productId")
            coroutineScope.launch(Dispatchers.Main) {
                try {
                    if (coins > 0) onCoinsPurchased(coins)
                } catch (ex: Exception) {
                    Log.e(TAG, "Error granting test coins for $productId", ex)
                }
            }
            return
        }

        val productDetails = _productDetailsMap.value[productId]
        if (productDetails == null) {
            Log.e(TAG, "No ProductDetails for $productId")
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, params)
    }

    // -----------------------------
    // PURCHASE HANDLING
    // -----------------------------

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val productId = purchase.products.firstOrNull()
        if (productId == null) {
            Log.e(TAG, "Purchased product id missing")
            return
        }

        val coins = CoinPacks.amounts[productId] ?: 0

        // If this is a coin pack (consumable), consume it then grant coins in the consume callback.
        if (coins > 0) {
            // Even if purchase.isAcknowledged is true, consumables should be consumed to allow repurchase.
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { result, _ ->
                if (result.responseCode == BillingResponseCode.OK) {
                    val token = purchase.purchaseToken
                    val prefs = context.getSharedPreferences("billing_prefs", Context.MODE_PRIVATE)

                    // 🔒 Prevent duplicate coin grants
                    if (prefs.getBoolean(token, false)) {
                        Log.w(TAG, "Purchase already processed, skipping grant: $productId")
                        return@consumeAsync
                    }

                    // Mark as processed BEFORE granting coins
                    prefs.edit().putBoolean(token, true).apply()

                    Log.d(TAG, "Consumed purchase $productId -> granting $coins coins")
                    coroutineScope.launch(Dispatchers.Main) {
                        try {
                            onCoinsPurchased(coins)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error delivering coins for $productId", ex)
                        }
                    }
                } else {
                    Log.e(TAG, "Consume failed for $productId: ${result.debugMessage}")
                }
            }


            return
        }

        // Non-consumable / entitlement handling: acknowledge if not already acknowledged
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")

                    // If there is any non-consumable entitlement side-effect, handle it here
                    // (e.g., unlock feature). For now we only log.
                    Log.d(TAG, "Non-consumable purchase processed: $productId")
                } else {
                    Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        } else {
            Log.d(TAG, "Purchase already acknowledged for $productId")
        }
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
