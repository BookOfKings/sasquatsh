package com.sasquatsh.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.ChatMessage
import com.sasquatsh.app.services.ChatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatService: ChatService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var contextType: String = savedStateHandle["contextType"] ?: ""
    private var contextId: String = savedStateHandle["contextId"] ?: ""
    private var pollJob: Job? = null

    fun configure(contextType: String, contextId: String) {
        this.contextType = contextType
        this.contextId = contextId
    }

    fun updateMessageText(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun loadMessages() {
        if (contextId.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val fetched = chatService.getMessages(
                    contextType = contextType,
                    contextId = contextId,
                    limit = 50,
                    before = null
                )
                _uiState.update {
                    it.copy(
                        messages = fetched.reversed(),
                        hasMore = fetched.size >= 50,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun loadEarlierMessages() {
        val oldest = _uiState.value.messages.firstOrNull() ?: return
        viewModelScope.launch {
            try {
                val fetched = chatService.getMessages(
                    contextType = contextType,
                    contextId = contextId,
                    limit = 50,
                    before = oldest.createdAt
                )
                val earlier = fetched.reversed()
                _uiState.update { state ->
                    state.copy(
                        messages = earlier + state.messages,
                        hasMore = fetched.size >= 50
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun sendMessage() {
        val content = _uiState.value.messageText.trim()
        if (content.isEmpty() || content.length > 1000) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val originalText = _uiState.value.messageText
            _uiState.update { it.copy(messageText = "") }

            try {
                val message = chatService.sendMessage(
                    contextType = contextType,
                    contextId = contextId,
                    content = content
                )
                _uiState.update { state ->
                    val existingIds = state.messages.map { it.id }.toSet()
                    if (message.id !in existingIds) {
                        state.copy(
                            messages = state.messages + message,
                            isSending = false
                        )
                    } else {
                        state.copy(isSending = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messageText = originalText,
                        error = e.localizedMessage,
                        isSending = false
                    )
                }
            }
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            try {
                chatService.deleteMessage(
                    contextType = contextType,
                    contextId = contextId,
                    messageId = id
                )
                _uiState.update { state ->
                    state.copy(messages = state.messages.filter { it.id != id })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun reportMessage(id: String, reason: String, details: String?) {
        viewModelScope.launch {
            try {
                chatService.reportMessage(
                    contextType = contextType,
                    contextId = contextId,
                    messageId = id,
                    reason = reason,
                    details = details
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000) // 10 seconds
                pollNewMessages()
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun pollNewMessages() {
        if (contextId.isEmpty()) return
        try {
            val fetched = chatService.getMessages(
                contextType = contextType,
                contextId = contextId,
                limit = 50,
                before = null
            )
            val reversed = fetched.reversed()
            _uiState.update { state ->
                val existingIds = state.messages.map { it.id }.toSet()
                val newMessages = reversed.filter { it.id !in existingIds }
                if (newMessages.isNotEmpty()) {
                    state.copy(messages = state.messages + newMessages)
                } else state
            }
        } catch (_: Exception) {
            // Polling errors are non-critical
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
