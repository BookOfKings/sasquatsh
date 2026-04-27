package com.sasquatsh.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.AddEventGameInput
import com.sasquatsh.app.models.CreateEventItemInput
import com.sasquatsh.app.models.Event
import com.sasquatsh.app.services.AuthService
import com.sasquatsh.app.services.EventsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventsService: EventsService,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private val eventId: String? = savedStateHandle["eventId"]

    init {
        eventId?.let { loadEvent(it) }
    }

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val event = eventsService.getEvent(id)
                _uiState.update { it.copy(event = event, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun register() {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.registerForEvent(event.id)
                _uiState.update { it.copy(actionMessage = "Registered successfully!") }
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancelRegistration() {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.cancelRegistration(event.id)
                _uiState.update { it.copy(actionMessage = "Registration cancelled") }
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun claimItem(itemId: String) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.claimItem(itemId)
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun unclaimItem(itemId: String) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.unclaimItem(itemId)
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun addItem(name: String, category: String?) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val input = CreateEventItemInput(itemName = name, itemCategory = category)
                eventsService.addItem(event.id, input)
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun addGame(input: AddEventGameInput) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val gameInput = input.copy(eventId = event.id)
                eventsService.addGame(gameInput)
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun removeGame(gameId: String) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.removeGame(gameId)
                loadEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteEvent(onSuccess: () -> Unit) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                eventsService.deleteEvent(event.id)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun isRegistered(userId: String): Boolean {
        return _uiState.value.event?.registrations?.any { it.userId == userId } == true
    }

    fun isHost(userId: String): Boolean {
        return _uiState.value.event?.hostUserId == userId
    }

    fun getCurrentUserId(): String? {
        return authService.getCurrentUserId()
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
