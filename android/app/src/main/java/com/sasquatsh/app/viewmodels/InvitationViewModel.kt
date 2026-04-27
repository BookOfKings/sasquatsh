package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.GameInvitation
import com.sasquatsh.app.models.InvitationPreview
import com.sasquatsh.app.services.InvitationsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvitationUiState(
    val gameInvitation: GameInvitation? = null,
    val groupInvitePreview: InvitationPreview? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val acceptedEventId: String? = null,
    val acceptedGroupId: String? = null
)

@HiltViewModel
class InvitationViewModel @Inject constructor(
    private val invitationsService: InvitationsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationUiState())
    val uiState: StateFlow<InvitationUiState> = _uiState.asStateFlow()

    fun loadGameInvitation(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val invitation = invitationsService.getGameInvitation(code)
                _uiState.update { it.copy(gameInvitation = invitation, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun acceptGameInvitation(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = invitationsService.acceptGameInvitation(code)
                _uiState.update {
                    it.copy(
                        successMessage = result.message,
                        acceptedEventId = result.eventId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun loadGroupInvitePreview(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val preview = invitationsService.getGroupInvitePreview(code)
                _uiState.update { it.copy(groupInvitePreview = preview, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun acceptGroupInvite(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = invitationsService.acceptGroupInvite(code)
                _uiState.update {
                    it.copy(
                        successMessage = result.message,
                        acceptedGroupId = result.groupId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }
}
