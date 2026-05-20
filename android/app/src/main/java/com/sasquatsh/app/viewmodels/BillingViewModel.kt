package com.sasquatsh.app.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.sasquatsh.app.models.Invoice
import com.sasquatsh.app.models.SubscriptionInfo
import com.sasquatsh.app.models.SubscriptionStatus
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.services.BillingService
import com.sasquatsh.app.services.GooglePlayBillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class BillingUiState(
    val subscriptionInfo: SubscriptionInfo? = null,
    val invoices: List<Invoice> = emptyList(),
    val hasMoreInvoices: Boolean = false,
    val isLoading: Boolean = false,
    val actionLoading: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val googleProducts: List<ProductDetails> = emptyList()
) {
    val currentTier: SubscriptionTier
        get() = subscriptionInfo?.subscription?.effectiveTier ?: SubscriptionTier.FREE

    val isCancelled: Boolean
        get() = subscriptionInfo?.subscription?.status == SubscriptionStatus.CANCELED

    val isPastDue: Boolean
        get() = subscriptionInfo?.subscription?.status == SubscriptionStatus.PAST_DUE

    val hasActiveSubscription: Boolean
        get() = subscriptionInfo?.hasActiveSubscription ?: false

    val hasOverride: Boolean
        get() = subscriptionInfo?.subscription?.hasOverride ?: false

    val isGoogleSubscription: Boolean
        get() = subscriptionInfo?.subscriptionSource == "google"

    val isStripeSubscription: Boolean
        get() = subscriptionInfo?.subscriptionSource == "stripe"

    val manageGoogleSubscriptionUri: Uri
        get() = Uri.parse("https://play.google.com/store/account/subscriptions")
}

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingService: BillingService,
    private val googlePlayBillingManager: GooglePlayBillingManager
) : ViewModel() {

    companion object {
        private const val TAG = "BillingViewModel"
    }

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        // Listen for completed purchases
        viewModelScope.launch {
            googlePlayBillingManager.purchaseResults.collect { result ->
                Log.d(TAG, "Purchase completed: ${result.productId}")
                _uiState.update { it.copy(purchaseInProgress = true) }
                try {
                    val verifyResult = billingService.verifyGooglePlayPurchase(
                        purchaseToken = result.purchase.purchaseToken,
                        productId = result.productId,
                        orderId = result.purchase.orderId
                    )
                    if (verifyResult.success) {
                        _uiState.update {
                            it.copy(
                                successMessage = "Subscription activated! You now have ${verifyResult.tier ?: "premium"} access.",
                                purchaseInProgress = false
                            )
                        }
                        loadBillingInfo()
                    } else {
                        _uiState.update {
                            it.copy(error = "Purchase verification failed", purchaseInProgress = false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Verification failed", e)
                    _uiState.update {
                        it.copy(error = e.localizedMessage ?: "Verification failed", purchaseInProgress = false)
                    }
                }
            }
        }

        // Listen for purchase errors
        viewModelScope.launch {
            googlePlayBillingManager.purchaseErrors.collect { errorMsg ->
                _uiState.update { it.copy(error = errorMsg, purchaseInProgress = false) }
            }
        }

        // Connect billing client
        googlePlayBillingManager.startConnection()
    }

    fun loadGoogleProducts() {
        viewModelScope.launch {
            val products = googlePlayBillingManager.queryProductDetails()
            _uiState.update { it.copy(googleProducts = products) }
        }
    }

    fun purchaseSubscription(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        _uiState.update { it.copy(purchaseInProgress = true, error = null) }
        val result = googlePlayBillingManager.launchBillingFlow(activity, productDetails, offerToken)
        if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
            _uiState.update {
                it.copy(
                    error = "Failed to launch purchase flow: ${result.debugMessage}",
                    purchaseInProgress = false
                )
            }
        }
        // If OK, the PurchasesUpdatedListener callback will handle the result
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            try {
                val purchases = googlePlayBillingManager.queryExistingPurchases()
                val activePurchases = purchases.filter {
                    it.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED
                }
                if (activePurchases.isEmpty()) {
                    _uiState.update {
                        it.copy(successMessage = "No active subscriptions found", actionLoading = false)
                    }
                    return@launch
                }
                // Verify each active purchase with the server
                for (purchase in activePurchases) {
                    val productId = purchase.products.firstOrNull() ?: continue
                    try {
                        billingService.verifyGooglePlayPurchase(
                            purchaseToken = purchase.purchaseToken,
                            productId = productId,
                            orderId = purchase.orderId
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to verify restored purchase: $productId", e)
                    }
                }
                _uiState.update {
                    it.copy(successMessage = "Purchases restored successfully", actionLoading = false)
                }
                loadBillingInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, actionLoading = false) }
            }
        }
    }

    fun loadBillingInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val subDeferred = async { billingService.getSubscriptionInfo() }
                val invDeferred = async { billingService.getInvoices(page = 1) }

                val sub = subDeferred.await()
                val inv = invDeferred.await()

                _uiState.update {
                    it.copy(
                        subscriptionInfo = sub,
                        invoices = inv.invoices,
                        hasMoreInvoices = inv.hasMore,
                        isLoading = false
                    )
                }
                currentPage = 1
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun loadMoreInvoices() {
        if (!_uiState.value.hasMoreInvoices) return
        viewModelScope.launch {
            val nextPage = currentPage + 1
            try {
                val response = billingService.getInvoices(page = nextPage)
                _uiState.update { state ->
                    state.copy(
                        invoices = state.invoices + response.invoices,
                        hasMoreInvoices = response.hasMore
                    )
                }
                currentPage = nextPage
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancelSubscription(context: Context) {
        val state = _uiState.value
        // Google subscriptions can't be cancelled server-side
        if (state.isGoogleSubscription) {
            val intent = Intent(Intent.ACTION_VIEW, state.manageGoogleSubscriptionUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            try {
                val result = billingService.cancelSubscription()
                if (result.source == "google") {
                    result.manageUrl?.let { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                } else {
                    _uiState.update { it.copy(successMessage = result.message) }
                    loadBillingInfo()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
            _uiState.update { it.copy(actionLoading = false) }
        }
    }

    fun reactivateSubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            try {
                val result = billingService.reactivateSubscription()
                _uiState.update { it.copy(successMessage = result.message) }
                loadBillingInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
            _uiState.update { it.copy(actionLoading = false) }
        }
    }

    fun getProductForTier(tier: String, annual: Boolean): ProductDetails? {
        val productId = "com.sasquatsh.$tier"
        return googlePlayBillingManager.getProductDetails(productId)
    }

    fun getOfferTokenForPlan(productDetails: ProductDetails, annual: Boolean): String? {
        val basePlanId = if (annual) "annual" else "monthly"
        return productDetails.subscriptionOfferDetails
            ?.find { it.basePlanId == basePlanId }
            ?.offerToken
    }

    fun getFormattedPrice(productDetails: ProductDetails, annual: Boolean = false): String {
        val basePlanId = if (annual) "annual" else "monthly"
        return googlePlayBillingManager.getFormattedPrice(productDetails, basePlanId) ?: ""
    }

    fun formattedAmount(cents: Int): String {
        val dollars = cents / 100.0
        return String.format(Locale.US, "$%.2f", dollars)
    }

    fun formattedDate(dateString: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = try {
                isoFormat.parse(dateString)
            } catch (_: Exception) {
                val isoNoFrac = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                isoNoFrac.parse(dateString)
            }
            if (date != null) {
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
            } else {
                dateString
            }
        } catch (_: Exception) {
            dateString
        }
    }

    fun formatCardBrand(brand: String): String {
        return when (brand.lowercase()) {
            "visa" -> "Visa"
            "mastercard" -> "Mastercard"
            "amex", "american_express" -> "Amex"
            "discover" -> "Discover"
            else -> brand.replaceFirstChar { it.uppercase() }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
