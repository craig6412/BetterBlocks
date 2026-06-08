package com.betterblocks

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.betterblocks.economy.EconomyConfig
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
        // IMPORTANT: product IDs must not change; only in-game coin grants are updated.
        val amounts: Map<String, Int> = EconomyConfig.COIN_PACK_GRANTS
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
                    deliverPendingCoinGrants()
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

     // Always log this mapping request; do not gate on BuildConfig.DEBUG so logs show in debug/test environments.
        Log.d(BILLING_MAP_TAG, "queryAvailableProducts: requesting ids=${productIds.joinToString(",")}")

        billingClient.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingResponseCode.OK) {
                _productDetailsMap.value = list.associateBy { it.productId }
                Log.d(TAG, "Loaded ${list.size} product details")

                // Always log what Play returned; diagnosis depends on this even when purchases cannot complete.
                list.forEach { details ->
                    val formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "(no price)"

                    // INAPP products may not have basePlanId/offerId. If Play provides subscription-style offers,
                    // surface identifiers anyway (safe, best-effort).
                    val offerTokenFromSubs = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.offerToken

                    val basePlanId = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.basePlanId

                    val offerId = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.offerId

                    Log.d(
                        BILLING_MAP_TAG,
                        "productId=${details.productId} name=${details.name} formattedPrice=$formattedPrice basePlanId=${basePlanId ?: "(n/a)"} offerId=${offerId ?: "(n/a)"} offerToken=${offerTokenFromSubs ?: "(n/a)"}"
                    )
                }

                val returnedIds = list.map { it.productId }.toSet()
                val missing = productIds.filter { it !in returnedIds }
                if (missing.isNotEmpty()) {
                    Log.w(BILLING_MAP_TAG, "Missing ProductDetails for ids=${missing.joinToString(",")}")
                }
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

    /**
     * Recovery path for the tiny crash window after Play consumes a purchase but before
     * coins are delivered. Pending grants are written before consumeAsync and cleared
     * only after the coins are granted and the purchase token is marked processed.
     */
    private fun deliverPendingCoinGrants() {
        val prefs = context.getSharedPreferences(BILLING_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(PENDING_COIN_PREFIX)) return@forEach

            val token = key.removePrefix(PENDING_COIN_PREFIX)
            val processedKey = processedPurchaseKey(token)
            val coins = value as? Int ?: return@forEach

            if (coins <= 0 || prefs.getBoolean(processedKey, false)) {
                prefs.edit().remove(key).apply()
                return@forEach
            }

            Log.w(TAG, "Recovering pending coin grant: token=$token coins=$coins")
            coroutineScope.launch(Dispatchers.Main) {
                try {
                    onCoinsPurchased(coins)
                    prefs.edit()
                        .putBoolean(processedKey, true)
                        .remove(key)
                        .apply()
                    Log.d(TAG, "Recovered pending coin grant for token=$token coins=$coins")
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to recover pending coin grant for token=$token", ex)
                }
            }
        }
    }

    private fun processedPurchaseKey(token: String): String = "$PROCESSED_PURCHASE_PREFIX$token"

    private fun pendingCoinKey(token: String): String = "$PENDING_COIN_PREFIX$token"

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
            val token = purchase.purchaseToken
            val prefs = context.getSharedPreferences(BILLING_PREFS_NAME, Context.MODE_PRIVATE)
            val processedKey = processedPurchaseKey(token)
            val pendingKey = pendingCoinKey(token)

            // 🔒 Prevent duplicate coin grants for the same purchase token.
            if (prefs.getBoolean(processedKey, false)) {
                Log.w(TAG, "Purchase already processed, skipping grant: $productId")
                return
            }

            // Save a durable pending grant before consuming. If the app dies after consume
            // succeeds but before delivery, deliverPendingCoinGrants() can recover it.
            prefs.edit()
                .putInt(pendingKey, coins)
                .apply()

            // Even if purchase.isAcknowledged is true, consumables should be consumed to allow repurchase.
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build()

            billingClient.consumeAsync(consumeParams) { result, _ ->
                if (result.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Consumed purchase $productId -> granting $coins coins")
                    coroutineScope.launch(Dispatchers.Main) {
                        try {
                            onCoinsPurchased(coins)
                            prefs.edit()
                                .putBoolean(processedKey, true)
                                .remove(pendingKey)
                                .apply()
                            Log.d(TAG, "Delivered and marked processed: $productId")
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error delivering coins for $productId; pending grant retained", ex)
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
        private const val BILLING_MAP_TAG = "BILLING_MAP"
        private const val BILLING_PREFS_NAME = "billing_prefs"
        private const val PROCESSED_PURCHASE_PREFIX = "processed_"
        private const val PENDING_COIN_PREFIX = "pending_coin_"
    }
}
