package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.DifficultyLevel
import com.sasquatsh.app.models.EventSearchFilter
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.services.EventsService
import com.sasquatsh.app.services.ProfileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventListUiState(
    val events: List<EventSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val selectedCategory: GameCategory? = null,
    val selectedGameSystem: GameSystem? = null,
    val selectedDifficulty: DifficultyLevel? = null,
    val nearbyEnabled: Boolean = false,
    val radiusMiles: Int = 25,
    val userPostalCode: String? = null,
    val filterCity: String? = null,
    val filterState: String? = null
) {
    val hasActiveFilters: Boolean
        get() = selectedCategory != null || selectedGameSystem != null ||
                selectedDifficulty != null || nearbyEnabled ||
                filterCity != null || filterState != null

    val filter: EventSearchFilter
        get() = if (nearbyEnabled && userPostalCode != null) {
            EventSearchFilter(
                search = searchText.ifEmpty { null },
                gameCategory = selectedCategory,
                gameSystem = selectedGameSystem,
                difficulty = selectedDifficulty,
                nearbyZip = userPostalCode,
                radiusMiles = radiusMiles
            )
        } else {
            EventSearchFilter(
                city = filterCity?.ifEmpty { null },
                state = filterState?.ifEmpty { null },
                search = searchText.ifEmpty { null },
                gameCategory = selectedCategory,
                gameSystem = selectedGameSystem,
                difficulty = selectedDifficulty
            )
        }
}

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val eventsService: EventsService,
    private val profileService: ProfileService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    fun updateSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun updateCategory(category: GameCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateGameSystem(gameSystem: GameSystem?) {
        _uiState.update { it.copy(selectedGameSystem = gameSystem) }
    }

    fun updateDifficulty(difficulty: DifficultyLevel?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun updateNearbyEnabled(enabled: Boolean) {
        _uiState.update { it.copy(nearbyEnabled = enabled) }
    }

    fun updateRadiusMiles(radius: Int) {
        _uiState.update { it.copy(radiusMiles = radius) }
    }

    fun updateFilterCity(city: String?) {
        _uiState.update { it.copy(filterCity = city) }
    }

    fun updateFilterState(state: String?) {
        _uiState.update { it.copy(filterState = state) }
    }

    fun loadUserPostalCode() {
        viewModelScope.launch {
            try {
                val profile = profileService.getMyProfile()
                _uiState.update { it.copy(userPostalCode = profile.homePostalCode) }
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val events = eventsService.getPublicEvents(_uiState.value.filter)
                _uiState.update { it.copy(events = events, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchText = "",
                selectedCategory = null,
                selectedGameSystem = null,
                selectedDifficulty = null,
                nearbyEnabled = false,
                filterCity = null,
                filterState = null
            )
        }
    }
}
