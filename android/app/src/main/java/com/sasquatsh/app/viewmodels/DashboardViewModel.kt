package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GroupSummary
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.models.PendingGroupInvitation
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.models.PlanningStatus
import com.sasquatsh.app.models.UserBadge
import com.sasquatsh.app.services.BadgesService
import com.sasquatsh.app.services.EventsService
import com.sasquatsh.app.services.GroupsService
import com.sasquatsh.app.services.PlanningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val registeredEvents: List<EventSummary> = emptyList(),
    val hostedEvents: List<EventSummary> = emptyList(),
    val planningSessions: List<PlanningSession> = emptyList(),
    val myGroups: List<GroupSummary> = emptyList(),
    val pendingInvitations: List<PendingGroupInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val managedGroups: List<GroupSummary>
        get() = myGroups.filter { it.userRole == MemberRole.OWNER || it.userRole == MemberRole.ADMIN }

    val memberGroups: List<GroupSummary>
        get() = myGroups.filter { it.userRole == MemberRole.MEMBER }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val eventsService: EventsService,
    private val groupsService: GroupsService,
    private val planningService: PlanningService,
    private val badgesService: BadgesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Newly earned badges to show in the popup */
    private val _newBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    val newBadges: StateFlow<List<UserBadge>> = _newBadges.asStateFlow()

    /** Whether the badge-earned popup should be shown */
    private val _showBadgePopup = MutableStateFlow(false)
    val showBadgePopup: StateFlow<Boolean> = _showBadgePopup.asStateFlow()

    /** Timestamp of last badge computation for 60-second throttle */
    private var lastBadgeComputeMillis: Long = 0L

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val registeredDeferred = async { eventsService.getRegisteredEvents() }
                val hostedDeferred = async { eventsService.getHostedEvents() }
                val planningDeferred = async {
                    try {
                        planningService.getMySessions()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val groupsDeferred = async { groupsService.getMyGroups() }
                val invitationsDeferred = async {
                    try {
                        groupsService.getMyPendingInvitations()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val today = LocalDate.now().toString()
                val registered = registeredDeferred.await().filter { it.eventDate >= today }
                val hosted = hostedDeferred.await().filter { it.eventDate >= today }
                val planning = planningDeferred.await().filter { it.status == PlanningStatus.OPEN }
                val groups = groupsDeferred.await()
                val invitations = invitationsDeferred.await()

                _uiState.update {
                    it.copy(
                        registeredEvents = registered,
                        hostedEvents = hosted,
                        planningSessions = planning,
                        myGroups = groups,
                        pendingInvitations = invitations,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun respondToInvitation(invitation: PendingGroupInvitation, accept: Boolean) {
        viewModelScope.launch {
            try {
                groupsService.respondToInvitation(invitation.id, accept)
                _uiState.update { state ->
                    state.copy(
                        pendingInvitations = state.pendingInvitations.filter { it.id != invitation.id }
                    )
                }
                if (accept) {
                    loadDashboard()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /**
     * Compute badges on the server and show a popup if any new badges were earned.
     * Throttled to at most once every 60 seconds unless [force] is true.
     */
    fun computeBadges(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastBadgeComputeMillis) < 60_000L) return
        lastBadgeComputeMillis = now

        viewModelScope.launch {
            try {
                android.util.Log.d("Badges", "Computing badges...")
                val response = badgesService.computeBadges()
                val earned = response.newlyEarned ?: 0
                android.util.Log.d("Badges", "Result: $earned newly earned, ${response.badges.size} total")
                if (earned > 0) {
                    _newBadges.value = response.badges.take(earned)
                    _showBadgePopup.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("Badges", "Error computing badges", e)
            }
        }
    }

    /** Dismiss the badge-earned popup */
    fun dismissBadgePopup() {
        _showBadgePopup.value = false
        _newBadges.value = emptyList()
    }
}
