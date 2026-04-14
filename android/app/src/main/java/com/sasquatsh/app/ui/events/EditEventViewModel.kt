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

data class EditEventUiState(
    val event: EventDetailDto? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    // Editable fields
    val title: String = "",
    val description: String = "",
    val eventDate: String = "",
    val startTime: String = "",
    val durationMinutes: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val address: String = "",
    val maxPlayers: String = "",
    val difficultyLevel: String = "",
    val status: String = "draft",
    val isPublic: Boolean = true,
    val hostIsPlaying: Boolean = true,
    val minAge: String = "",
)

@HiltViewModel
class EditEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(EditEventUiState())
    val uiState: StateFlow<EditEventUiState> = _uiState

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = eventsRepository.getEvent(eventId)) {
                is ApiResult.Success -> {
                    val e = result.data
                    _uiState.update { it.copy(
                        isLoading = false,
                        event = e,
                        title = e.title,
                        description = e.description ?: "",
                        eventDate = e.eventDate,
                        startTime = e.startTime ?: "",
                        durationMinutes = e.durationMinutes?.toString() ?: "",
                        city = e.city ?: "",
                        state = e.state ?: "",
                        postalCode = e.postalCode ?: "",
                        address = e.address ?: "",
                        maxPlayers = e.maxPlayers.toString(),
                        difficultyLevel = e.difficultyLevel ?: "",
                        status = e.status ?: "draft",
                        isPublic = e.isPublic != false,
                        hostIsPlaying = e.hostIsPlaying != false,
                        minAge = e.minAge?.toString() ?: "",
                    ) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun updateTitle(value: String) { _uiState.update { it.copy(title = value) } }
    fun updateDescription(value: String) { _uiState.update { it.copy(description = value) } }
    fun updateEventDate(value: String) { _uiState.update { it.copy(eventDate = value) } }
    fun updateStartTime(value: String) { _uiState.update { it.copy(startTime = value) } }
    fun updateDurationMinutes(value: String) { _uiState.update { it.copy(durationMinutes = value) } }
    fun updateCity(value: String) { _uiState.update { it.copy(city = value) } }
    fun updateState(value: String) { _uiState.update { it.copy(state = value) } }
    fun updatePostalCode(value: String) { _uiState.update { it.copy(postalCode = value) } }
    fun updateAddress(value: String) { _uiState.update { it.copy(address = value) } }
    fun updateMaxPlayers(value: String) { _uiState.update { it.copy(maxPlayers = value) } }
    fun updateDifficultyLevel(value: String) { _uiState.update { it.copy(difficultyLevel = value) } }
    fun updateStatus(value: String) { _uiState.update { it.copy(status = value) } }
    fun updateIsPublic(value: Boolean) { _uiState.update { it.copy(isPublic = value) } }
    fun updateHostIsPlaying(value: Boolean) { _uiState.update { it.copy(hostIsPlaying = value) } }
    fun updateMinAge(value: String) { _uiState.update { it.copy(minAge = value) } }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank() || state.eventDate.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val body = mutableMapOf<String, Any?>(
                "title" to state.title.trim(),
                "description" to state.description.trim().ifEmpty { null },
                "eventDate" to state.eventDate,
                "startTime" to state.startTime.ifEmpty { null },
                "durationMinutes" to state.durationMinutes.toIntOrNull(),
                "city" to state.city.trim().ifEmpty { null },
                "state" to state.state.trim().ifEmpty { null },
                "postalCode" to state.postalCode.trim().ifEmpty { null },
                "addressLine1" to state.address.trim().ifEmpty { null },
                "maxPlayers" to (state.maxPlayers.toIntOrNull() ?: 8),
                "difficultyLevel" to state.difficultyLevel.ifEmpty { null },
                "status" to state.status,
                "isPublic" to state.isPublic,
                "hostIsPlaying" to state.hostIsPlaying,
                "minAge" to state.minAge.toIntOrNull(),
            )

            when (val result = eventsRepository.updateEvent(eventId, body)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
}
