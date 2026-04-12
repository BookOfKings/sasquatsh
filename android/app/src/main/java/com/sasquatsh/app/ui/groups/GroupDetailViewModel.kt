package com.sasquatsh.app.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.GroupDetailDto
import com.sasquatsh.app.data.repository.GroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val group: GroupDetailDto? = null,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupsRepository: GroupsRepository,
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState

    init {
        loadGroup()
    }

    fun loadGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = groupsRepository.getGroup(slug)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, group = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun joinGroup() {
        val groupId = _uiState.value.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }
            when (val result = groupsRepository.joinGroup(groupId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isActionLoading = false, actionSuccess = "Joined group!") }
                    loadGroup()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, error = result.message) }
                }
            }
        }
    }

    fun leaveGroup() {
        val groupId = _uiState.value.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }
            when (val result = groupsRepository.leaveGroup(groupId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isActionLoading = false, actionSuccess = "Left group") }
                    loadGroup()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, error = result.message) }
                }
            }
        }
    }

    fun requestToJoin() {
        val groupId = _uiState.value.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null) }
            when (val result = groupsRepository.requestToJoin(groupId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isActionLoading = false, actionSuccess = "Request sent!") }
                    loadGroup()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isActionLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearActionSuccess() {
        _uiState.update { it.copy(actionSuccess = null) }
    }
}
