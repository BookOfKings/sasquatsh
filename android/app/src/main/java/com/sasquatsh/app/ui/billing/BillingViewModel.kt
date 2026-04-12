package com.sasquatsh.app.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.BillingInfoDto
import com.sasquatsh.app.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val billingInfo: BillingInfoDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val checkoutUrl: String? = null,
    val portalUrl: String? = null,
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState

    init {
        loadBillingInfo()
    }

    fun loadBillingInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getBillingInfo()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, billingInfo = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun subscribe(priceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.createCheckoutSession(priceId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, checkoutUrl = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun openBillingPortal() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.createPortalSession()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, portalUrl = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun clearUrls() {
        _uiState.value = _uiState.value.copy(checkoutUrl = null, portalUrl = null)
    }
}
