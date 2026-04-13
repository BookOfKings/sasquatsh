package com.sasquatsh.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.EventSummaryDto
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import com.sasquatsh.app.data.repository.EventsRepository
import com.sasquatsh.app.data.repository.GroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val myGames: List<EventSummaryDto> = emptyList(),
    val hostedGames: List<EventSummaryDto> = emptyList(),
    val myGroups: List<GroupSummaryDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val groupsRepository: GroupsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load all in parallel
            val myGamesResult = eventsRepository.getRegisteredEvents()
            val hostedResult = eventsRepository.getHostedEvents()
            val groupsResult = groupsRepository.getMyGroups()

            val today = java.time.LocalDate.now().toString()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                myGames = when (myGamesResult) {
                    is ApiResult.Success -> myGamesResult.data.filter { (it.eventDate) >= today }
                    is ApiResult.Error -> emptyList()
                },
                hostedGames = when (hostedResult) {
                    is ApiResult.Success -> hostedResult.data.filter { (it.eventDate) >= today }
                    is ApiResult.Error -> emptyList()
                },
                myGroups = when (groupsResult) {
                    is ApiResult.Success -> groupsResult.data
                    is ApiResult.Error -> emptyList()
                },
            )
        }
    }
}
