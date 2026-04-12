package com.sasquatsh.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.ChatMessageDto
import com.sasquatsh.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var pollingJob: Job? = null
    private var contextType: String = ""
    private var contextId: String = ""

    fun initialize(contextType: String, contextId: String) {
        this.contextType = contextType
        this.contextId = contextId
        loadMessages()
    }

    fun loadMessages() {
        if (contextType.isBlank() || contextId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.messages.isEmpty())
            when (val result = chatRepository.getMessages(contextType, contextId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        messages = result.data,
                        isLoading = false,
                        error = null,
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

    fun sendMessage(content: String) {
        if (content.isBlank() || contextType.isBlank() || contextId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            when (val result = chatRepository.sendMessage(contextType, contextId, content)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + result.data,
                        isSending = false,
                        error = null,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                loadMessages()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 10_000L
    }
}
