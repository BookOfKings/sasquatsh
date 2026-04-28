package com.sasquatsh.app.views.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.GameGroup
import com.sasquatsh.app.models.GroupInvitation
import com.sasquatsh.app.models.GroupMember
import com.sasquatsh.app.models.JoinPolicy
import com.sasquatsh.app.models.JoinRequest
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.models.PlanningStatus
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.GroupDetailViewModel
import com.sasquatsh.app.views.events.formatEventDate
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.UserAvatarView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupDetailView(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEvent: (String) -> Unit,
    onNavigateToPlanning: (String) -> Unit,
    onNavigateToCreatePlanning: (String) -> Unit,
    onNavigateToEditGroup: (String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val currentUserId = authState.user?.id

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showJoinRequestDialog by remember { mutableStateOf(false) }
    var joinRequestMessage by remember { mutableStateOf("") }
    var showTransferConfirm by remember { mutableStateOf(false) }
    var transferTargetId by remember { mutableStateOf<String?>(null) }
    var transferTargetName by remember { mutableStateOf("") }
    var showChat by remember { mutableStateOf(false) }
    var showRecurringGameForm by remember { mutableStateOf(false) }
    var editingRecurringGame by remember { mutableStateOf<com.sasquatsh.app.models.RecurringGame?>(null) }

    val isMember = currentUserId != null && viewModel.userRole(currentUserId) != null
    val isAdmin = currentUserId != null && viewModel.isAdmin(currentUserId)
    val isOwner = currentUserId != null && viewModel.userRole(currentUserId) == MemberRole.OWNER

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadGroup(groupId) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading && uiState.group == null) {
            LoadingView()
        } else if (uiState.group != null) {
            val group = uiState.group!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header card
                HeaderSection(
                    group = group,
                    members = uiState.members,
                    currentUserId = currentUserId,
                    isMember = isMember,
                    isAdmin = isAdmin,
                    userRole = currentUserId?.let { viewModel.userRole(it) },
                    onEditClick = { onNavigateToEditGroup(groupId) },
                    onShareClick = { /* share intent */ }
                )

                // Error / action messages
                uiState.error?.let { error ->
                    ErrorBannerView(message = error)
                }
                uiState.actionMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Join/Leave actions
                if (!isMember) {
                    JoinActions(
                        group = group,
                        onJoin = { viewModel.join() },
                        onRequestToJoin = { showJoinRequestDialog = true }
                    )
                }

                // 2. Upcoming Games (members only)
                if (isMember) {
                    UpcomingGamesSection(
                        events = uiState.groupEvents,
                        onEventClick = onNavigateToEvent
                    )
                }

                // 3. Game Planning (members only)
                if (isMember) {
                    PlanningSection(
                        sessions = uiState.planningSessions,
                        isAdmin = isAdmin,
                        onCreatePlanning = { onNavigateToCreatePlanning(groupId) },
                        onPlanningClick = onNavigateToPlanning
                    )
                }

                // 4. Group Chat (members only, collapsible)
                if (isMember) {
                    GroupChatSection(
                        showChat = showChat,
                        onToggle = { showChat = !showChat },
                        groupId = groupId
                    )
                }

                // 5. Recurring Games
                RecurringGamesSection(
                    games = emptyList(), // Will be loaded separately
                    isAdmin = isAdmin,
                    onCreateGame = {
                        editingRecurringGame = null
                        showRecurringGameForm = true
                    },
                    onEditGame = { game ->
                        editingRecurringGame = game
                        showRecurringGameForm = true
                    }
                )

                // 6. Admin Panel or Members list
                if (isAdmin) {
                    AdminPanelSection(
                        members = uiState.members,
                        joinRequests = uiState.joinRequests,
                        invitations = uiState.invitations,
                        currentUserId = currentUserId,
                        isOwner = isOwner,
                        onChangeRole = { userId, role -> viewModel.changeRole(userId, role) },
                        onRemoveMember = { userId -> viewModel.removeMember(userId) },
                        onTransferOwnership = { userId, name ->
                            transferTargetId = userId
                            transferTargetName = name
                            showTransferConfirm = true
                        },
                        onApproveRequest = { userId -> viewModel.approveRequest(userId) },
                        onRejectRequest = { userId -> viewModel.rejectRequest(userId) },
                        onCreateInvitation = { maxUses, expiresInDays, onSuccess ->
                            viewModel.createInvitation(maxUses, expiresInDays, onSuccess)
                        },
                        onRevokeInvitation = { id -> viewModel.revokeInvitation(id) },
                        onLoadRequests = { viewModel.loadJoinRequests() },
                        onLoadInvitations = { viewModel.loadInvitations() }
                    )
                } else if (isMember) {
                    MembersSection(
                        members = uiState.members,
                        currentUserId = currentUserId
                    )
                }

                // 7. Leave Group (non-owners only)
                if (isMember && !isOwner) {
                    OutlinedButton(
                        onClick = { viewModel.leave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Group")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup { onNavigateBack() }
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showJoinRequestDialog) {
        AlertDialog(
            onDismissRequest = { showJoinRequestDialog = false },
            title = { Text("Request to Join") },
            text = {
                TextField(
                    value = joinRequestMessage,
                    onValueChange = { joinRequestMessage = it },
                    label = { Text("Message (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.requestToJoin(joinRequestMessage.ifEmpty { null })
                    showJoinRequestDialog = false
                    joinRequestMessage = ""
                }) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJoinRequestDialog = false
                    joinRequestMessage = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTransferConfirm) {
        AlertDialog(
            onDismissRequest = { showTransferConfirm = false },
            title = { Text("Transfer Ownership") },
            text = { Text("Transfer ownership to $transferTargetName? You will be demoted to admin.") },
            confirmButton = {
                TextButton(onClick = {
                    transferTargetId?.let { viewModel.transferOwnership(it) }
                    showTransferConfirm = false
                }) {
                    Text("Transfer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recurring game form
    if (showRecurringGameForm) {
        RecurringGameFormSheet(
            groupId = groupId,
            game = editingRecurringGame,
            onDismiss = { showRecurringGameForm = false },
            onSave = { _, _ ->
                showRecurringGameForm = false
                viewModel.loadGroup(groupId)
            }
        )
    }
}

// ---- Header Section ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderSection(
    group: GameGroup,
    members: List<GroupMember>,
    currentUserId: String?,
    isMember: Boolean,
    isAdmin: Boolean,
    userRole: MemberRole?,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: logo + name + buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Logo
                val logoUrl = group.logoUrl
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = group.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isMember) {
                            IconButton(
                                onClick = onShareClick,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Type + creator
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = group.groupType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        group.creator?.let { creator ->
                            Text(
                                text = " \u00b7 by ${creator.displayName ?: creator.username ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Badge chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Member count
                ChipBadge(
                    icon = Icons.Filled.Groups,
                    text = "${group.memberCount ?: members.size} ${if ((group.memberCount ?: members.size) == 1) "member" else "members"}",
                    bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )

                // Join policy
                when (group.joinPolicy) {
                    JoinPolicy.OPEN -> ChipBadge(
                        icon = Icons.Filled.Public,
                        text = "Open",
                        bgColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        fgColor = Color(0xFF4CAF50)
                    )
                    JoinPolicy.REQUEST -> ChipBadge(
                        icon = Icons.Filled.PanTool,
                        text = "Request to Join",
                        bgColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                        fgColor = Color(0xFFFF9800)
                    )
                    JoinPolicy.INVITE_ONLY -> ChipBadge(
                        icon = Icons.Filled.Lock,
                        text = "Invite Only",
                        bgColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                        fgColor = Color(0xFFFF9800)
                    )
                }

                // User role
                when (userRole) {
                    MemberRole.OWNER -> ChipBadge(
                        icon = Icons.Filled.Star,
                        text = "Owner",
                        bgColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                        fgColor = Color(0xFF9C27B0)
                    )
                    MemberRole.ADMIN -> ChipBadge(
                        icon = Icons.Filled.Shield,
                        text = "Admin",
                        bgColor = Color(0xFF2196F3).copy(alpha = 0.15f),
                        fgColor = Color(0xFF2196F3)
                    )
                    MemberRole.MEMBER -> ChipBadge(
                        icon = Icons.Filled.Groups,
                        text = "Member",
                        bgColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        fgColor = Color(0xFF4CAF50)
                    )
                    null -> {}
                }
            }

            // Location
            val city = group.locationCity
            if (!city.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    val parts = listOfNotNull(city, group.locationState).filter { it.isNotEmpty() }
                    Text(
                        text = parts.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val radius = group.locationRadiusMiles
                    if (radius != null && radius > 0) {
                        Text(
                            text = " \u00b7 $radius mi radius",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Description
            val description = group.description
            if (!description.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    bgColor: Color,
    fgColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor
        )
    }
}

// ---- Join Actions ----

@Composable
private fun JoinActions(
    group: GameGroup,
    onJoin: () -> Unit,
    onRequestToJoin: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (group.joinPolicy) {
            JoinPolicy.OPEN -> {
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Group")
                }
            }
            JoinPolicy.REQUEST -> {
                Button(
                    onClick = onRequestToJoin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Email, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request to Join")
                }
            }
            JoinPolicy.INVITE_ONLY -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Invitation required to join this group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---- Upcoming Games ----

@Composable
private fun UpcomingGamesSection(
    events: List<com.sasquatsh.app.models.EventSummary>,
    onEventClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            SectionHeader(icon = Icons.Filled.Casino, title = "Upcoming Games")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (events.isEmpty()) {
                Text(
                    text = "No upcoming games scheduled for this group yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                )
            } else {
                events.take(5).forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEventClick(event.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        val thumbUrl = event.primaryGameThumbnail
                        if (thumbUrl != null) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Casino, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatEventDate(event.eventDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        event.maxPlayers?.let { max ->
                            Text(
                                text = "${event.confirmedCount}/$max",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Filled.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---- Planning Section ----

@Composable
private fun PlanningSection(
    sessions: List<com.sasquatsh.app.models.PlanningSession>,
    isAdmin: Boolean,
    onCreatePlanning: () -> Unit,
    onPlanningClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header with "Host a Game" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderContent(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Game Planning"
                )
                if (isAdmin) {
                    Button(
                        onClick = onCreatePlanning,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Host a Game", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val openSessions = sessions.filter { it.status == PlanningStatus.OPEN }
            if (openSessions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No active planning sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        Text(
                            text = "Click \"Host a Game\" to start coordinating your next game night.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                openSessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlanningClick(session.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Deadline: ${formatEventDate(session.responseDeadline)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        BadgeView(
                            text = "Open",
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        )

                        Icon(
                            Icons.Filled.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---- Group Chat Section ----

@Composable
private fun GroupChatSection(
    showChat: Boolean,
    onToggle: () -> Unit,
    groupId: String
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (showChat) 180f else 0f,
        label = "chevron_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderContent(
                    icon = Icons.Filled.Chat,
                    title = "Group Chat"
                )
                Icon(
                    Icons.Filled.ExpandMore, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(visible = showChat) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Chat placeholder - will connect to ChatPanelView
                    Text(
                        text = "Chat coming soon...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ---- Recurring Games Section ----

@Composable
private fun RecurringGamesSection(
    games: List<com.sasquatsh.app.models.RecurringGame>,
    isAdmin: Boolean,
    onCreateGame: () -> Unit,
    onEditGame: (com.sasquatsh.app.models.RecurringGame) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recurring Games",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onCreateGame) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add recurring game",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (games.isEmpty()) {
            Text(
                text = "No recurring games scheduled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
        } else {
            games.forEach { game ->
                RecurringGameCard(
                    game = game,
                    isAdmin = isAdmin,
                    onEdit = { onEditGame(game) },
                    onToggleActive = {},
                    onDelete = {}
                )
            }
        }
    }
}

// ---- Members Section (non-admin) ----

@Composable
private fun MembersSection(
    members: List<GroupMember>,
    currentUserId: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            SectionHeader(
                icon = Icons.Filled.Groups,
                title = "Members (${members.size})"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            members.forEachIndexed { index, member ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                MemberRow(member = member, isAdmin = false, isOwner = false, currentUserId = currentUserId)
            }
        }
    }
}

// ---- Admin Panel Section ----

@Composable
private fun AdminPanelSection(
    members: List<GroupMember>,
    joinRequests: List<JoinRequest>,
    invitations: List<GroupInvitation>,
    currentUserId: String?,
    isOwner: Boolean,
    onChangeRole: (String, MemberRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onTransferOwnership: (String, String) -> Unit,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onCreateInvitation: (Int?, Int?, (GroupInvitation) -> Unit) -> Unit,
    onRevokeInvitation: (String) -> Unit,
    onLoadRequests: () -> Unit,
    onLoadInvitations: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        onLoadRequests()
        onLoadInvitations()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Tab bar
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Members (${members.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            if (joinRequests.isEmpty()) "Requests"
                            else "Requests (${joinRequests.size})"
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Invitations") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Members tab
                    members.forEachIndexed { index, member ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        MemberRow(
                            member = member,
                            isAdmin = true,
                            isOwner = isOwner,
                            currentUserId = currentUserId,
                            onChangeRole = onChangeRole,
                            onRemoveMember = onRemoveMember,
                            onTransferOwnership = onTransferOwnership
                        )
                    }
                }
                1 -> {
                    // Requests tab
                    if (joinRequests.isEmpty()) {
                        Text(
                            text = "No pending requests",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .padding(horizontal = 16.dp)
                        )
                    } else {
                        joinRequests.forEach { request ->
                            JoinRequestRow(
                                request = request,
                                onApprove = { onApproveRequest(request.userId) },
                                onReject = { onRejectRequest(request.userId) }
                            )
                        }
                    }
                }
                2 -> {
                    // Invitations tab
                    InvitationsTab(
                        invitations = invitations,
                        onCreateInvitation = onCreateInvitation,
                        onRevokeInvitation = onRevokeInvitation
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    isAdmin: Boolean,
    isOwner: Boolean,
    currentUserId: String?,
    onChangeRole: ((String, MemberRole) -> Unit)? = null,
    onRemoveMember: ((String) -> Unit)? = null,
    onTransferOwnership: ((String, String) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarView(
            url = member.avatarUrl,
            name = member.displayName,
            size = 40.dp,
            userId = member.userId
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Joined ${formatEventDate(member.joinedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Role badge
        RoleBadge(role = member.role)

        // Admin menu
        if (isAdmin && member.userId != currentUserId && member.role != MemberRole.OWNER) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (member.role == MemberRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Make Admin") },
                            onClick = {
                                onChangeRole?.invoke(member.userId, MemberRole.ADMIN)
                                showMenu = false
                            }
                        )
                    } else if (member.role == MemberRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Remove Admin") },
                            onClick = {
                                onChangeRole?.invoke(member.userId, MemberRole.MEMBER)
                                showMenu = false
                            }
                        )
                    }
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Transfer Ownership") },
                            leadingIcon = { Icon(Icons.Filled.SwapHoriz, null) },
                            onClick = {
                                onTransferOwnership?.invoke(
                                    member.userId,
                                    member.displayName ?: "this member"
                                )
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onRemoveMember?.invoke(member.userId)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: MemberRole) {
    val (bgColor, fgColor) = when (role) {
        MemberRole.OWNER -> Color(0xFF9C27B0).copy(alpha = 0.20f) to Color(0xFFCE93D8)
        MemberRole.ADMIN -> Color(0xFF2196F3).copy(alpha = 0.20f) to Color(0xFF64B5F6)
        MemberRole.MEMBER -> Color(0xFF4CAF50).copy(alpha = 0.20f) to Color(0xFF81C784)
    }

    Text(
        text = role.displayName,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fgColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun JoinRequestRow(
    request: JoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarView(
                url = request.avatarUrl,
                name = request.displayName,
                size = 40.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.displayName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Requested ${formatEventDate(request.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onApprove,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Approve", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onReject) {
                Text("Reject", style = MaterialTheme.typography.labelMedium)
            }
        }

        val message = request.message
        if (!message.isNullOrEmpty()) {
            Text(
                text = "\"$message\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun InvitationsTab(
    invitations: List<GroupInvitation>,
    onCreateInvitation: (Int?, Int?, (GroupInvitation) -> Unit) -> Unit,
    onRevokeInvitation: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showCreateForm by remember { mutableStateOf(false) }
    var selectedMaxUses by remember { mutableIntStateOf(-1) } // -1 = unlimited
    var selectedExpiration by remember { mutableIntStateOf(7) } // days, -1 = never
    var showMaxUsesMenu by remember { mutableStateOf(false) }
    var showExpirationMenu by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var createdLinkUrl by remember { mutableStateOf<String?>(null) }
    var linkCopiedMessage by remember { mutableStateOf<String?>(null) }

    // Clear copied message after delay
    LaunchedEffect(linkCopiedMessage) {
        if (linkCopiedMessage != null) {
            kotlinx.coroutines.delay(2000)
            linkCopiedMessage = null
        }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create Invite Link button
        if (!showCreateForm && createdLinkUrl == null) {
            Button(
                onClick = { showCreateForm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Invitation Link")
            }
        }

        // Creation form
        AnimatedVisibility(visible = showCreateForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Invitation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Max Uses picker
                    Text(
                        text = "Max Uses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        OutlinedButton(
                            onClick = { showMaxUsesMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedMaxUses == -1) "Unlimited" else "$selectedMaxUses use${if (selectedMaxUses != 1) "s" else ""}",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMaxUsesMenu,
                            onDismissRequest = { showMaxUsesMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unlimited") },
                                onClick = { selectedMaxUses = -1; showMaxUsesMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("1 use") },
                                onClick = { selectedMaxUses = 1; showMaxUsesMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("5 uses") },
                                onClick = { selectedMaxUses = 5; showMaxUsesMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("10 uses") },
                                onClick = { selectedMaxUses = 10; showMaxUsesMenu = false }
                            )
                        }
                    }

                    // Expiration picker
                    Text(
                        text = "Expires In",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        OutlinedButton(
                            onClick = { showExpirationMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (selectedExpiration) {
                                    -1 -> "Never"
                                    1 -> "1 day"
                                    else -> "$selectedExpiration days"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showExpirationMenu,
                            onDismissRequest = { showExpirationMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Never") },
                                onClick = { selectedExpiration = -1; showExpirationMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("1 day") },
                                onClick = { selectedExpiration = 1; showExpirationMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("7 days") },
                                onClick = { selectedExpiration = 7; showExpirationMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("30 days") },
                                onClick = { selectedExpiration = 30; showExpirationMenu = false }
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showCreateForm = false
                                selectedMaxUses = -1
                                selectedExpiration = 7
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                isCreating = true
                                val maxUses = if (selectedMaxUses == -1) null else selectedMaxUses
                                val expiresInDays = if (selectedExpiration == -1) null else selectedExpiration
                                onCreateInvitation(maxUses, expiresInDays) { invitation ->
                                    isCreating = false
                                    showCreateForm = false
                                    selectedMaxUses = -1
                                    selectedExpiration = 7
                                    createdLinkUrl = "https://sasquatsh.com/groups/invite/${invitation.inviteCode}"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isCreating
                        ) {
                            if (isCreating) {
                                Text("Creating...")
                            } else {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Link")
                            }
                        }
                    }
                }
            }
        }

        // Created link banner
        AnimatedVisibility(visible = createdLinkUrl != null) {
            createdLinkUrl?.let { url ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Invite link created!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(url))
                                    linkCopiedMessage = "Link copied!"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (linkCopiedMessage != null) "Copied!" else "Copy")
                            }
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Join our group on Sasquatsh!\n$url")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Invite Link"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                        TextButton(
                            onClick = { createdLinkUrl = null }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Existing invitations list
        if (invitations.isEmpty()) {
            Text(
                text = "No active invitations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else {
            invitations.forEach { invitation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = invitation.inviteCode,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "Uses: ${invitation.usesCount}${invitation.maxUses?.let { "/$it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        invitation.expiresAt?.let { expires ->
                            Text(
                                text = "Expires: ${formatEventDate(expires)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Share button
                    IconButton(onClick = {
                        val url = "https://sasquatsh.com/groups/invite/${invitation.inviteCode}"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Join our group on Sasquatsh!\n$url")
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Invite Link"))
                    }) {
                        Icon(
                            Icons.Filled.Share, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Copy button
                    IconButton(onClick = {
                        val url = "https://sasquatsh.com/groups/invite/${invitation.inviteCode}"
                        clipboardManager.setText(AnnotatedString(url))
                        linkCopiedMessage = "Link copied!"
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy, null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Revoke button
                    IconButton(onClick = { onRevokeInvitation(invitation.id) }) {
                        Icon(
                            Icons.Filled.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Copied feedback
        AnimatedVisibility(visible = linkCopiedMessage != null) {
            Text(
                text = linkCopiedMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---- Shared Helpers ----

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    SectionHeaderContent(icon = icon, title = title, modifier = Modifier.padding(16.dp))
}

@Composable
private fun SectionHeaderContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
