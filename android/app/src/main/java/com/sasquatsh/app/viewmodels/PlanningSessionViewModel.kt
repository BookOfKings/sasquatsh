package com.sasquatsh.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.AddPlanningItemInput
import com.sasquatsh.app.models.DateAvailabilityInput
import com.sasquatsh.app.models.ItemCategory
import com.sasquatsh.app.models.PlanningResponseInput
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.models.ScheduleEntry
import com.sasquatsh.app.models.SuggestGameInput
import com.sasquatsh.app.services.PlanningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanningSessionUiState(
    val session: PlanningSession? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class PlanningSessionViewModel @Inject constructor(
    private val planningService: PlanningService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningSessionUiState())
    val uiState: StateFlow<PlanningSessionUiState> = _uiState.asStateFlow()

    private val sessionId: String? = savedStateHandle["sessionId"]

    init {
        sessionId?.let { loadSession(it) }
    }

    fun loadSession(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val session = planningService.getSession(id)
                _uiState.update { it.copy(session = session, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun submitResponse(cannotAttendAny: Boolean, dateAvailability: List<DateAvailabilityInput>) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val input = PlanningResponseInput(
                    cannotAttendAny = cannotAttendAny,
                    dateAvailability = dateAvailability
                )
                planningService.submitResponse(session.id, input)
                _uiState.update { it.copy(actionMessage = "Response submitted!") }
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun suggestGame(input: SuggestGameInput) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val suggestion = planningService.suggestGame(session.id, input)
                _uiState.update { state ->
                    val currentSession = state.session ?: return@update state
                    val suggestions = (currentSession.gameSuggestions ?: emptyList()) + suggestion
                    state.copy(session = currentSession.copy(gameSuggestions = suggestions))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun voteForGame(suggestionId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.voteForGame(session.id, suggestionId)
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun unvoteGame(suggestionId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.unvoteGame(session.id, suggestionId)
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun removeSuggestion(suggestionId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.removeSuggestion(session.id, suggestionId)
                _uiState.update { state ->
                    val currentSession = state.session ?: return@update state
                    val suggestions = currentSession.gameSuggestions?.filter { it.id != suggestionId }
                    state.copy(session = currentSession.copy(gameSuggestions = suggestions))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun finalize(selectedDateId: String?, selectedGameId: String?, onEventCreated: (String) -> Unit) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val result = planningService.finalizeSession(session.id, selectedDateId, selectedGameId)
                _uiState.update { it.copy(actionMessage = result.message) }
                onEventCreated(result.eventId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun addItem(name: String, category: ItemCategory, quantity: Int?) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val input = AddPlanningItemInput(
                    itemName = name,
                    itemCategory = category,
                    quantityNeeded = quantity
                )
                val item = planningService.addItem(session.id, input)
                _uiState.update { state ->
                    val currentSession = state.session ?: return@update state
                    val items = (currentSession.items ?: emptyList()) + item
                    state.copy(session = currentSession.copy(items = items))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun claimItem(itemId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.claimItem(session.id, itemId)
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun unclaimItem(itemId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.unclaimItem(session.id, itemId)
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun removeItem(itemId: String) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.removeItem(session.id, itemId)
                _uiState.update { state ->
                    val currentSession = state.session ?: return@update state
                    val items = currentSession.items?.filter { it.id != itemId }
                    state.copy(session = currentSession.copy(items = items))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun scheduleSessions(assignments: Map<Int, String>) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val schedule = assignments.map { (tableNum, gameId) ->
                    ScheduleEntry(
                        suggestionId = gameId,
                        tableNumber = tableNum,
                        slotIndex = 0
                    )
                }
                planningService.scheduleSessions(session.id, schedule)
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateSettings(tableCount: Int?) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.updateSettings(session.id, tableCount)
                _uiState.update { state ->
                    val currentSession = state.session ?: return@update state
                    val message = if (tableCount != null && tableCount >= 2) {
                        "Multi-table enabled with $tableCount tables"
                    } else {
                        "Multi-table disabled"
                    }
                    state.copy(
                        session = currentSession.copy(tableCount = tableCount),
                        actionMessage = message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancel() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                planningService.cancelSession(session.id)
                _uiState.update { it.copy(actionMessage = "Session cancelled") }
                loadSession(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
