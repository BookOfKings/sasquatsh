package com.sasquatsh.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.CreateInvitationInput
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GameGroup
import com.sasquatsh.app.models.GroupInvitation
import com.sasquatsh.app.models.GroupMember
import com.sasquatsh.app.models.JoinRequest
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.services.EventsService
import com.sasquatsh.app.services.GroupsService
import com.sasquatsh.app.services.PlanningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val group: GameGroup? = null,
    val members: List<GroupMember> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
    val invitations: List<GroupInvitation> = emptyList(),
    val planningSessions: List<PlanningSession> = emptyList(),
    val groupEvents: List<EventSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupsService: GroupsService,
    private val planningService: PlanningService,
    private val eventsService: EventsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val groupId: String? = savedStateHandle["groupId"]

    init {
        groupId?.let { loadGroup(it) }
    }

    fun loadGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val group = groupsService.getGroup(id)
                val members = groupsService.getMembers(id)
                val planning = try {
                    planningService.getGroupSessions(id)
                } catch (_: Exception) {
                    emptyList()
                }
                val events = try {
                    eventsService.getGroupEvents(id)
                } catch (_: Exception) {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        group = group,
                        members = members,
                        planningSessions = planning,
                        groupEvents = events,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun loadJoinRequests() {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                val requests = groupsService.getJoinRequests(group.id)
                _uiState.update { it.copy(joinRequests = requests) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun loadInvitations() {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                val invitations = groupsService.getInvitations(group.id)
                _uiState.update { it.copy(invitations = invitations) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun join() {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                groupsService.joinGroup(group.id)
                _uiState.update { it.copy(actionMessage = "Joined group!") }
                loadGroup(group.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun requestToJoin(message: String?) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                groupsService.requestToJoin(group.id, message)
                _uiState.update { it.copy(actionMessage = "Join request sent!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun leave() {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                groupsService.leaveGroup(group.id)
                _uiState.update { it.copy(actionMessage = "Left group") }
                loadGroup(group.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun removeMember(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                groupsService.removeMember(group.id, userId)
                _uiState.update { state ->
                    state.copy(members = state.members.filter { it.userId != userId })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun changeRole(userId: String, role: MemberRole) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                groupsService.changeRole(group.id, userId, role)
                _uiState.update { state ->
                    state.copy(
                        members = state.members.map {
                            if (it.userId == userId) it.copy(role = role) else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun approveRequest(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                groupsService.approveRequest(group.id, userId)
                _uiState.update { state ->
                    state.copy(joinRequests = state.joinRequests.filter { it.userId != userId })
                }
                loadGroup(group.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun rejectRequest(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                groupsService.rejectRequest(group.id, userId)
                _uiState.update { state ->
                    state.copy(joinRequests = state.joinRequests.filter { it.userId != userId })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun createInvitation(
        maxUses: Int? = null,
        expiresInDays: Int? = null,
        onSuccess: (GroupInvitation) -> Unit
    ) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                val input = if (maxUses != null || expiresInDays != null) {
                    CreateInvitationInput(maxUses = maxUses, expiresInDays = expiresInDays)
                } else null
                val invitation = groupsService.createInvitation(group.id, input)
                _uiState.update { state ->
                    state.copy(invitations = listOf(invitation) + state.invitations)
                }
                onSuccess(invitation)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun revokeInvitation(inviteId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                groupsService.revokeInvitation(group.id, inviteId)
                _uiState.update { state ->
                    state.copy(invitations = state.invitations.filter { it.id != inviteId })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteGroup(onSuccess: () -> Unit) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                groupsService.deleteGroup(group.id)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun transferOwnership(userId: String) {
        val group = _uiState.value.group ?: return
        viewModelScope.launch {
            try {
                groupsService.transferOwnership(group.id, userId)
                loadGroup(group.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun userRole(userId: String): MemberRole? {
        return _uiState.value.members.firstOrNull { it.userId == userId }?.role
    }

    fun isAdmin(userId: String): Boolean {
        val role = userRole(userId)
        return role == MemberRole.OWNER || role == MemberRole.ADMIN
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
