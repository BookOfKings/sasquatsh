package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.Badge
import com.sasquatsh.app.models.UserBadge
import com.sasquatsh.app.services.BadgesService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BadgesUiState(
    val allBadges: List<Badge> = emptyList(),
    val earnedBadges: List<UserBadge> = emptyList(),
    val isLoading: Boolean = true,
    val isComputing: Boolean = false,
    val newlyEarned: Int = 0,
    val error: String? = null,
    val selectedCategory: String? = null
) {
    val earnedBadgeIds: Set<Int>
        get() = earnedBadges.map { it.badgeId }.toSet()

    val filteredBadges: List<Badge>
        get() = if (selectedCategory != null) {
            allBadges.filter { it.category == selectedCategory }
        } else {
            allBadges
        }
}

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val badgesService: BadgesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    fun loadBadges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allBadges = badgesService.getAllBadges()
                val myBadges = badgesService.getMyBadges()
                _uiState.update {
                    it.copy(
                        allBadges = allBadges.sortedBy { b -> b.sortOrder },
                        earnedBadges = myBadges.badges,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun computeBadges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isComputing = true) }
            try {
                val result = badgesService.computeBadges()
                _uiState.update {
                    it.copy(
                        earnedBadges = result.badges,
                        newlyEarned = result.newlyEarned ?: 0,
                        isComputing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isComputing = false) }
            }
        }
    }

    fun togglePin(badgeId: Int) {
        viewModelScope.launch {
            try {
                badgesService.togglePin(badgeId)
                val myBadges = badgesService.getMyBadges()
                _uiState.update { it.copy(earnedBadges = myBadges.badges) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
