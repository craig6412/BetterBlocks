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
    private val onCoinsPurchased: (Int) -> Unit // Callback to give coins to user
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
        if (purchase.isAcknowledged) return // 🔒 DOUBLE-REWARD GUARD

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { result ->
            if (result.responseCode == BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")

                val productId = purchase.products.firstOrNull()
                val coins = CoinPacks.amounts[productId] ?: 0

                if (coins > 0) {
                    coroutineScope.launch(Dispatchers.Main) {
                        onCoinsPurchased(coins)
                    }
                }
            } else {
                Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
