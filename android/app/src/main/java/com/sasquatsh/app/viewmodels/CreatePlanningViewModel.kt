package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.CreatePlanningSessionInput
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.models.ProposedDateInput
import com.sasquatsh.app.services.PlanningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CreatePlanningUiState(
    val title: String = "",
    val description: String = "",
    val responseDeadline: Date = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L),
    val selectedMemberIds: Set<String> = emptySet(),
    val proposedDates: List<LocalDate> = emptyList(),
    val openToGroup: Boolean = false,
    val maxParticipants: Int? = null,
    val tableCount: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean get() = validationIssues.isEmpty()

    val validationIssues: List<String>
        get() {
            val issues = mutableListOf<String>()
            if (title.trim().isEmpty()) {
                issues.add("Title is required")
            }
            if (proposedDates.isEmpty()) {
                issues.add("Add at least one proposed date")
            }
            if (!openToGroup && selectedMemberIds.isEmpty()) {
                issues.add("Invite at least one member (or enable Open to Group)")
            }
            return issues
        }
}

@HiltViewModel
class CreatePlanningViewModel @Inject constructor(
    private val planningService: PlanningService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlanningUiState())
    val uiState: StateFlow<CreatePlanningUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateResponseDeadline(deadline: Date) {
        _uiState.update { it.copy(responseDeadline = deadline) }
    }

    fun toggleMember(memberId: String) {
        _uiState.update { state ->
            val updated = state.selectedMemberIds.toMutableSet()
            if (updated.contains(memberId)) {
                updated.remove(memberId)
            } else {
                updated.add(memberId)
            }
            state.copy(selectedMemberIds = updated)
        }
    }

    fun updateOpenToGroup(open: Boolean) {
        _uiState.update { it.copy(openToGroup = open) }
    }

    fun updateMaxParticipants(max: Int?) {
        _uiState.update { it.copy(maxParticipants = max) }
    }

    fun updateTableCount(count: Int?) {
        _uiState.update { it.copy(tableCount = count) }
    }

    fun addDate(date: LocalDate) {
        _uiState.update { state ->
            if (state.proposedDates.none { it == date }) {
                state.copy(proposedDates = (state.proposedDates + date).sorted())
            } else state
        }
    }

    fun removeDate(date: LocalDate) {
        _uiState.update { state ->
            state.copy(proposedDates = state.proposedDates.filter { it != date })
        }
    }

    fun save(groupId: String, onSuccess: (PlanningSession) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            val dates = state.proposedDates.map {
                ProposedDateInput(date = it.format(isoFormatter))
            }

            val input = CreatePlanningSessionInput(
                groupId = groupId,
                title = state.title,
                description = state.description.ifEmpty { null },
                responseDeadline = dateFormatter.format(state.responseDeadline),
                inviteeUserIds = state.selectedMemberIds.toList(),
                proposedDates = dates,
                openToGroup = if (state.openToGroup) true else null,
                maxParticipants = state.maxParticipants,
                tableCount = state.tableCount
            )

            try {
                val session = planningService.createSession(input)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(session)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }
}
