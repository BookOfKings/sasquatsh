package com.sasquatsh.app.ui.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.PlanningSessionDto
import com.sasquatsh.app.data.repository.PlanningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanningSessionUiState(
    val session: PlanningSessionDto? = null,
    val isLoading: Boolean = false,
    val isActioning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PlanningSessionViewModel @Inject constructor(
    private val planningRepository: PlanningRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _uiState = MutableStateFlow(PlanningSessionUiState())
    val uiState: StateFlow<PlanningSessionUiState> = _uiState

    init {
        if (sessionId.isNotBlank()) {
            loadSession()
        }
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = planningRepository.getSession(sessionId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        session = result.data,
                        isLoading = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun voteDates(votes: List<Map<String, Any>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActioning = true, error = null)
            when (val result = planningRepository.voteDates(sessionId, votes)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        session = result.data,
                        isActioning = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActioning = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun suggestGame(gameName: String, bggId: Int? = null, thumbnailUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActioning = true, error = null)
            val game = mutableMapOf<String, Any?>(
                "gameName" to gameName,
            )
            if (bggId != null) game["bggId"] = bggId
            if (thumbnailUrl != null) game["thumbnailUrl"] = thumbnailUrl

            when (val result = planningRepository.suggestGame(sessionId, game)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        session = result.data,
                        isActioning = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActioning = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun voteGame(gameId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActioning = true, error = null)
            when (val result = planningRepository.voteGame(sessionId, gameId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        session = result.data,
                        isActioning = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActioning = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun claimItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActioning = true, error = null)
            val body = mapOf<String, Any?>("itemId" to itemId)
            when (val result = planningRepository.suggestGame(sessionId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        session = result.data,
                        isActioning = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isActioning = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
