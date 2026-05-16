package com.sasquatsh.app.services

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseResult(
    val purchase: Purchase,
    val productId: String
)

@Singleton
class GooglePlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "GooglePlayBilling"

        val PRODUCT_IDS = listOf(
            "com.sasquatsh.basic.monthly",
            "com.sasquatsh.basic.annual",
            "com.sasquatsh.pro.monthly",
            "com.sasquatsh.pro.annual",
        )
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    val purchaseResults: SharedFlow<PurchaseResult> = _purchaseResults.asSharedFlow()

    private val _purchaseErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val purchaseErrors: SharedFlow<String> = _purchaseErrors.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun startConnection() {
        if (billingClient.isReady) {
            _isConnected.value = true
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _isConnected.value = true
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, will retry on next operation")
                _isConnected.value = false
            }
        })
    }

    suspend fun queryProductDetails(): List<ProductDetails> {
        if (!ensureConnected()) return emptyList()

        val productList = PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val details = result.productDetailsList ?: emptyList()
            _products.value = details
            Log.d(TAG, "Loaded ${details.size} products")
            return details
        } else {
            Log.e(TAG, "Query products failed: ${result.billingResult.debugMessage}")
            return emptyList()
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        val productId = purchase.products.firstOrNull() ?: ""
                        Log.d(TAG, "Purchase successful: $productId")
                        _purchaseResults.tryEmit(PurchaseResult(purchase, productId))
                    }
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${result.responseCode} - ${result.debugMessage}")
                _purchaseErrors.tryEmit(result.debugMessage ?: "Purchase failed")
            }
        }
    }

    suspend fun queryExistingPurchases(): List<Purchase> {
        if (!ensureConnected()) return emptyList()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            return result.purchasesList
        }
        return emptyList()
    }

    fun getProductDetails(productId: String): ProductDetails? {
        return _products.value.find { it.productId == productId }
    }

    fun getFormattedPrice(productDetails: ProductDetails): String? {
        return productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        startConnection()
        // Give it a moment to connect
        repeat(10) {
            if (billingClient.isReady) return true
            kotlinx.coroutines.delay(200)
        }
        return billingClient.isReady
    }
}
