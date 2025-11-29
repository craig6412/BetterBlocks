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
import kotlinx.coroutines.withContext

/**
 * Manages Google Play Billing connection, purchase flow, and acknowledgement.
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onCoinsPurchased: (Int) -> Unit // Callback to give coins to user
) {

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap = _productDetailsMap.asStateFlow()

    // Defined Product IDs from Google Play Console
    private val productIds = listOf(
        "coins_small",
        "coins_medium",
        "coins_large",
        "coins_mega" // NEW
    )

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase.")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected successfully.")
                    queryAvailableProducts()
                } else {
                    Log.e(TAG, "Billing connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected. Retrying...")
                // Logic to retry connection could go here
            }
        })
    }

    /**
     * Queries Google Play for the details (price, name) of our coin packs.
     */
    private fun queryAvailableProducts() {
        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                // Map Product ID -> Details for easy lookup
                val newMap = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = newMap
                Log.d(TAG, "Found ${productDetailsList.size} products.")
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launches the official Google Play purchase sheet.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = _productDetailsMap.value[productId]
        if (productDetails == null) {
            Log.e(TAG, "Product details not found for: $productId")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Processes a successful purchase. VERY IMPORTANT: Must Acknowledge!
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully.")

                        // REWARD THE USER
                        val coinsToAdd = when (purchase.products.firstOrNull()) {
                            "coins_small" -> 1000
                            "coins_medium" -> 7500
                            "coins_large" -> 15000
                            "coins_mega" -> 75000
                            else -> 0
                        }

                        if (coinsToAdd > 0) {
                            coroutineScope.launch(Dispatchers.Main) {
                                onCoinsPurchased(coinsToAdd)
                            }
                        }
                    }
                }
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}