package com.sasquatsh.app.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.Invoice
import com.sasquatsh.app.models.SubscriptionInfo
import com.sasquatsh.app.models.SubscriptionStatus
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.services.BillingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class BillingUiState(
    val subscriptionInfo: SubscriptionInfo? = null,
    val invoices: List<Invoice> = emptyList(),
    val hasMoreInvoices: Boolean = false,
    val isLoading: Boolean = false,
    val actionLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
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
    private val billingService: BillingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private var currentPage = 1

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
