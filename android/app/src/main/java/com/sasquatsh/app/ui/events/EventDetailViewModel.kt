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
    val canEdit: Boolean = false,
    val currentUserId: String? = null,
    val error: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
    private val authRepository: com.sasquatsh.app.data.repository.AuthRepository,
    private val groupsRepository: com.sasquatsh.app.data.repository.GroupsRepository,
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

            // Wait briefly for auth sync to complete if currentUser is null
            if (authRepository.currentUser.value == null) {
                authRepository.syncExistingUser()
            }

            when (val result = eventsRepository.getEvent(eventId)) {
                is ApiResult.Success -> {
                    val event = result.data
                    val currentUser = authRepository.currentUser.value
                    val currentUserId = currentUser?.id
                    val hostUserId = event.hostUserId

                    val isHost = currentUserId != null && currentUserId == hostUserId
                    val isSiteAdmin = currentUser?.isAdmin == true

                    // Check if user is a group admin/owner for this event's group
                    var isGroupAdmin = false
                    if (!isHost && !isSiteAdmin && event.groupId != null && currentUserId != null) {
                        val groupResult = groupsRepository.getGroupById(event.groupId)
                        if (groupResult is ApiResult.Success) {
                            val membership = groupResult.data.userMembership
                            isGroupAdmin = membership != null &&
                                (membership.role == "owner" || membership.role == "admin")
                        }
                    }

                    _uiState.update { it.copy(
                        isLoading = false,
                        event = event,
                        canEdit = isHost || isSiteAdmin || isGroupAdmin,
                        currentUserId = currentUserId,
                    ) }
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
