package com.sasquatsh.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.EventSummaryDto
import com.sasquatsh.app.data.repository.EventsRepository
import com.sasquatsh.app.domain.model.GameSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventListUiState(
    val events: List<EventSummaryDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedGameSystem: GameSystem? = null,
    val city: String = "",
    val state: String = "",
    val nearbyZip: String = "",
    val radiusMiles: Int? = null,
)

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val filters = buildFilters()
            when (val result = eventsRepository.browseEvents(filters)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, events = result.data, isRefreshing = false) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message, isRefreshing = false) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val filters = buildFilters()
            when (val result = eventsRepository.browseEvents(filters)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, events = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun submitSearch() {
        loadEvents()
    }

    fun selectGameSystem(gameSystem: GameSystem?) {
        _uiState.update { it.copy(selectedGameSystem = gameSystem) }
        loadEvents()
    }

    fun applyFilter(
        city: String = _uiState.value.city,
        state: String = _uiState.value.state,
        nearbyZip: String = _uiState.value.nearbyZip,
        radiusMiles: Int? = _uiState.value.radiusMiles,
    ) {
        _uiState.update {
            it.copy(
                city = city,
                state = state,
                nearbyZip = nearbyZip,
                radiusMiles = radiusMiles,
            )
        }
        loadEvents()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun buildFilters(): Map<String, String> {
        val state = _uiState.value
        return buildMap {
            if (state.searchQuery.isNotBlank()) put("search", state.searchQuery)
            state.selectedGameSystem?.let { put("gameCategory", it.value) }
            if (state.city.isNotBlank()) put("city", state.city)
            if (state.state.isNotBlank()) put("state", state.state)
            if (state.nearbyZip.isNotBlank()) put("nearbyZip", state.nearbyZip)
            state.radiusMiles?.let { put("radiusMiles", it.toString()) }
        }
    }
}
