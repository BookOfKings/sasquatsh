package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.CreatePlayerRequestInput
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.PlayerRequest
import com.sasquatsh.app.models.PlayerRequestFilters
import com.sasquatsh.app.services.EventsService
import com.sasquatsh.app.services.SocialService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerRequestListUiState(
    val requests: List<PlayerRequest> = emptyList(),
    val myRequests: List<PlayerRequest> = emptyList(),
    val hostedEvents: List<EventSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class PlayerRequestListViewModel @Inject constructor(
    private val socialService: SocialService,
    private val eventsService: EventsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerRequestListUiState())
    val uiState: StateFlow<PlayerRequestListUiState> = _uiState.asStateFlow()

    fun loadRequests(filters: PlayerRequestFilters = PlayerRequestFilters()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val requests = socialService.getPlayerRequests(filters)
                _uiState.update { it.copy(requests = requests, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun loadMyRequests() {
        viewModelScope.launch {
            try {
                val myRequests = socialService.getMyPlayerRequests()
                _uiState.update { it.copy(myRequests = myRequests) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun loadHostedEvents() {
        viewModelScope.launch {
            try {
                val events = eventsService.getHostedEvents()
                _uiState.update { it.copy(hostedEvents = events) }
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }

    fun createRequest(eventId: String, description: String?, playerCount: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val input = CreatePlayerRequestInput(
                eventId = eventId,
                description = description,
                playerCountNeeded = playerCount
            )
            try {
                val newRequest = socialService.createPlayerRequest(input)
                _uiState.update { state ->
                    state.copy(
                        myRequests = listOf(newRequest) + state.myRequests,
                        requests = listOf(newRequest) + state.requests,
                        actionMessage = "Request posted! Expires in 15 minutes."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun fillRequest(id: String) {
        viewModelScope.launch {
            try {
                val updated = socialService.fillPlayerRequest(id)
                _uiState.update { state ->
                    state.copy(
                        myRequests = state.myRequests.map { if (it.id == id) updated else it },
                        requests = state.requests.filter { it.id != id },
                        actionMessage = "Marked as filled!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancelRequest(id: String) {
        viewModelScope.launch {
            try {
                val updated = socialService.cancelPlayerRequest(id)
                _uiState.update { state ->
                    state.copy(
                        myRequests = state.myRequests.map { if (it.id == id) updated else it },
                        requests = state.requests.filter { it.id != id },
                        actionMessage = "Request cancelled"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteRequest(id: String) {
        viewModelScope.launch {
            try {
                socialService.deletePlayerRequest(id)
                _uiState.update { state ->
                    state.copy(
                        requests = state.requests.filter { it.id != id },
                        myRequests = state.myRequests.filter { it.id != id }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
