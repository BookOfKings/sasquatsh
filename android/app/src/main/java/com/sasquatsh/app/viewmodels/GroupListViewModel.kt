package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.GroupSearchFilter
import com.sasquatsh.app.models.GroupSummary
import com.sasquatsh.app.models.GroupType
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.services.GroupsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupListUiState(
    val groups: List<GroupSummary> = emptyList(),
    val myGroupCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val selectedType: GroupType? = null,
    val filterCity: String = "",
    val filterState: String = ""
) {
    val hasActiveFilters: Boolean
        get() = selectedType != null || filterCity.isNotEmpty() || filterState.isNotEmpty()

    val filter: GroupSearchFilter
        get() = GroupSearchFilter(
            search = searchText.ifEmpty { null },
            groupType = selectedType,
            city = filterCity.ifEmpty { null },
            state = filterState.ifEmpty { null }
        )
}

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupsService: GroupsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    fun updateSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun updateSelectedType(type: GroupType?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun updateFilterCity(city: String) {
        _uiState.update { it.copy(filterCity = city) }
    }

    fun updateFilterState(state: String) {
        _uiState.update { it.copy(filterState = state) }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val publicGroupsDeferred = async { groupsService.getPublicGroups(_uiState.value.filter) }
                val myGroupsDeferred = async {
                    try {
                        groupsService.getMyGroups()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val publicGroups = publicGroupsDeferred.await()
                val myGroups = myGroupsDeferred.await()

                // Build a map of group ID -> user's role from my groups
                val roleMap = myGroups.associate { it.id to it.userRole }

                // Annotate public groups with user's role, dedup against my groups
                val myGroupIds = myGroups.map { it.id }.toSet()
                val annotatedPublic = publicGroups
                    .filter { it.id !in myGroupIds }
                    .map { group ->
                        val role = roleMap[group.id]
                        if (role != null) group.copy(userRole = role) else group
                    }

                // Merge: my groups first (sorted by role priority), then other groups alphabetically
                val rolePriority = mapOf(
                    MemberRole.OWNER to 0,
                    MemberRole.ADMIN to 1,
                    MemberRole.MEMBER to 2
                )
                val sortedMyGroups = myGroups.sortedWith(
                    compareBy<GroupSummary> { rolePriority[it.userRole] ?: 3 }
                        .thenBy { it.name.lowercase() }
                )
                val sortedOtherGroups = annotatedPublic.sortedBy { it.name.lowercase() }
                val merged = sortedMyGroups + sortedOtherGroups

                _uiState.update {
                    it.copy(
                        groups = merged,
                        myGroupCount = myGroups.size,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchText = "",
                selectedType = null,
                filterCity = "",
                filterState = ""
            )
        }
    }
}
