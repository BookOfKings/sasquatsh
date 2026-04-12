package com.sasquatsh.app.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import com.sasquatsh.app.data.repository.GroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GroupTab { DISCOVER, MY_GROUPS }

data class GroupListUiState(
    val activeTab: GroupTab = GroupTab.DISCOVER,
    val publicGroups: List<GroupSummaryDto> = emptyList(),
    val myGroups: List<GroupSummaryDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupsRepository: GroupsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState

    init {
        loadGroups()
    }

    fun setActiveTab(tab: GroupTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == GroupTab.DISCOVER && _uiState.value.publicGroups.isEmpty()) {
            loadPublicGroups()
        } else if (tab == GroupTab.MY_GROUPS && _uiState.value.myGroups.isEmpty()) {
            loadMyGroups()
        }
    }

    fun loadGroups() {
        loadPublicGroups()
        loadMyGroups()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadPublicGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = groupsRepository.getPublicGroups()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, publicGroups = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadMyGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = groupsRepository.getMyGroups()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, myGroups = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
