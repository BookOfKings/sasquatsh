package com.sasquatsh.app.ui.lfp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.PlayerRequestDto
import com.sasquatsh.app.data.repository.PlayerRequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerRequestListUiState(
    val requests: List<PlayerRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val createSuccess: Boolean = false,
)

@HiltViewModel
class PlayerRequestListViewModel @Inject constructor(
    private val playerRequestsRepository: PlayerRequestsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerRequestListUiState())
    val uiState: StateFlow<PlayerRequestListUiState> = _uiState

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = playerRequestsRepository.getAll()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        requests = result.data,
                        isLoading = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun createRequest(body: Map<String, Any?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null, createSuccess = false)
            when (val result = playerRequestsRepository.create(body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        requests = listOf(result.data) + _uiState.value.requests,
                        isCreating = false,
                        createSuccess = true,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun deleteRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            when (val result = playerRequestsRepository.delete(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        requests = _uiState.value.requests.filter { it.id != id },
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCreateSuccess() {
        _uiState.value = _uiState.value.copy(createSuccess = false)
    }
}
