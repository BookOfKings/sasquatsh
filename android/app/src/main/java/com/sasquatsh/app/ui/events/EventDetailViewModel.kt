package com.sasquatsh.app.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.EventDetailDto
import com.sasquatsh.app.data.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val event: EventDetailDto? = null,
    val isLoading: Boolean = false,
    val isRegistering: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    init {
        loadEvent()
    }

    fun loadEvent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = eventsRepository.getEvent(eventId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, event = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true) }
            when (val result = eventsRepository.registerForEvent(eventId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRegistering = false, actionMessage = "Registered successfully") }
                    loadEvent()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRegistering = false, actionMessage = result.message) }
                }
            }
        }
    }

    fun unregister() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true) }
            when (val result = eventsRepository.cancelRegistration(eventId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRegistering = false, actionMessage = "Registration cancelled") }
                    loadEvent()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRegistering = false, actionMessage = result.message) }
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
