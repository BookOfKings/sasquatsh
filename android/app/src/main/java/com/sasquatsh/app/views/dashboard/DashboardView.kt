package com.sasquatsh.app.views.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.sasquatsh.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GroupSummary
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.models.PendingGroupInvitation
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.DashboardViewModel
import com.sasquatsh.app.viewmodels.RaffleViewModel
import com.sasquatsh.app.views.app.Routes
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.views.shared.SubscriptionBadgeView
import com.sasquatsh.app.views.shared.UserAvatarView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    authViewModel: AuthViewModel,
    navController: NavController,
    onSwitchTab: (Int) -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    raffleViewModel: RaffleViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val dashState by dashboardViewModel.uiState.collectAsState()
    val raffleState by raffleViewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
        raffleViewModel.loadActiveRaffle()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            dashboardViewModel.loadDashboard()
            raffleViewModel.loadActiveRaffle()
            isRefreshing = false
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (dashState.isLoading && dashState.registeredEvents.isEmpty() && dashState.myGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(0.dp))
                }

                // Gamer Toolbox link
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.TOOLBOX) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_full),
                            contentDescription = "Sasquatsh logo",
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gamer Toolbox",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // User header
                item {
                    UserHeaderCard(
                        displayName = authState.user?.displayName ?: authState.user?.username ?: "",
                        username = authState.user?.username,
                        avatarUrl = authState.user?.avatarUrl,
                        tier = authState.user?.effectiveTier ?: SubscriptionTier.FREE,
                        onHostGame = {
                            navController.navigate(Routes.CREATE_EVENT)
                        },
                        onTierClick = {
                            navController.navigate(Routes.PRICING)
                        }
                    )
                }

                // Error
                if (dashState.error != null) {
                    item {
                        ErrorBannerView(
                            message = dashState.error!!,
                            onDismiss = { /* ViewModel handles clearing */ },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Pending Group Invitations
                if (dashState.pendingInvitations.isNotEmpty()) {
                    item {
                        PendingInvitationsCard(
                            invitations = dashState.pendingInvitations,
                            onAccept = { dashboardViewModel.respondToInvitation(it, true) },
                            onDecline = { dashboardViewModel.respondToInvitation(it, false) }
                        )
                    }
                }

                // My Upcoming Games
                item {
                    DashboardSection(
                        title = "My Upcoming Games",
                        icon = Icons.Default.CalendarMonth,
                        isEmpty = dashState.registeredEvents.isEmpty(),
                        emptyMessage = "You haven't signed up for any games yet.",
                        emptyButtonTitle = "Browse Games",
                        onEmptyAction = { onSwitchTab(1) }
                    ) {
                        dashState.registeredEvents.forEach { event ->
                            CompactEventRow(
                                event = event,
                                onClick = {
                                    navController.navigate(Routes.eventDetail(event.id))
                                }
                            )
                        }
                    }
                }

                // Games I'm Hosting
                item {
                    DashboardSection(
                        title = "Games I'm Hosting",
                        icon = Icons.Default.Star,
                        isEmpty = dashState.hostedEvents.isEmpty(),
                        emptyMessage = "You haven't hosted any games yet.",
                        emptyButtonTitle = "Host Your First Game",
                        onEmptyAction = { navController.navigate(Routes.CREATE_EVENT) }
                    ) {
                        dashState.hostedEvents.forEach { event ->
                            CompactEventRow(
                                event = event,
                                onClick = {
                                    navController.navigate(Routes.eventDetail(event.id))
                                }
                            )
                        }
                    }
                }

                // Games Being Planned
                item {
                    DashboardSection(
                        title = "Games Being Planned",
                        icon = Icons.Default.PersonAdd,
                        isEmpty = dashState.planningSessions.isEmpty(),
                        emptyMessage = "No active planning sessions.",
                        emptyButtonTitle = "View Groups",
                        onEmptyAction = { onSwitchTab(2) }
                    ) {
                        dashState.planningSessions.forEach { session ->
                            CompactPlanningRow(
                                session = session,
                                onClick = {
                                    navController.navigate(Routes.planningDetail(session.id))
                                }
                            )
                        }
                    }
                }

                // Groups I Manage
                item {
                    DashboardSection(
                        title = "Groups I Manage",
                        icon = Icons.Default.Groups,
                        isEmpty = dashState.managedGroups.isEmpty(),
                        emptyMessage = "You're not managing any groups yet.",
                        emptyButtonTitle = "Create Group",
                        onEmptyAction = { navController.navigate(Routes.CREATE_GROUP) }
                    ) {
                        dashState.managedGroups.forEach { group ->
                            CompactGroupRow(
                                group = group,
                                onClick = {
                                    navController.navigate(Routes.groupDetail(group.id))
                                }
                            )
                        }
                    }
                }

                // Groups I'm In
                item {
                    DashboardSection(
                        title = "Groups I'm In",
                        icon = Icons.Default.People,
                        isEmpty = dashState.memberGroups.isEmpty(),
                        emptyMessage = "You haven't joined any groups yet.",
                        emptyButtonTitle = "Browse Groups",
                        onEmptyAction = { onSwitchTab(2) }
                    ) {
                        dashState.memberGroups.forEach { group ->
                            CompactGroupRow(
                                group = group,
                                onClick = {
                                    navController.navigate(Routes.groupDetail(group.id))
                                }
                            )
                        }
                    }
                }

                // Raffle banner
                if (raffleState.raffle != null) {
                    item {
                        RaffleBanner(
                            title = raffleState.raffle!!.title,
                            prizeName = raffleState.raffle!!.prizeName,
                            onClick = {
                                navController.navigate(Routes.RAFFLE_DETAIL)
                            }
                        )
                    }
                }

                // Upgrade banner for free tier
                if ((authState.user?.effectiveTier ?: SubscriptionTier.FREE) == SubscriptionTier.FREE) {
                    item {
                        UpgradeBanner(
                            onClick = {
                                navController.navigate(Routes.PRICING)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ---- User Header Card ----

@Composable
private fun UserHeaderCard(
    displayName: String,
    username: String?,
    avatarUrl: String?,
    tier: SubscriptionTier,
    onHostGame: () -> Unit,
    onTierClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarView(
            url = avatarUrl,
            name = displayName,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (username != null) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SubscriptionBadgeView(tier = tier)
            }
        }

        Button(
            onClick = onHostGame,
            shape = RoundedCornerShape(12.dp),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Host a Game",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ---- Pending Invitations Card ----

@Composable
private fun PendingInvitationsCard(
    invitations: List<PendingGroupInvitation>,
    onAccept: (PendingGroupInvitation) -> Unit,
    onDecline: (PendingGroupInvitation) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Group Invitations",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            invitations.forEach { invite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = invite.group?.name ?: "Group",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (invite.invitedBy != null) {
                            Text(
                                text = "Invited by ${invite.invitedBy.displayName ?: "someone"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { onDecline(invite) }
                    ) {
                        Text(
                            text = "Decline",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = { onAccept(invite) },
                        shape = RoundedCornerShape(50),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// ---- Dashboard Section ----

@Composable
private fun DashboardSection(
    title: String,
    icon: ImageVector,
    isEmpty: Boolean,
    emptyMessage: String,
    emptyButtonTitle: String,
    onEmptyAction: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onEmptyAction,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = emptyButtonTitle,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                content()
            }
        }
    }
}

// ---- Compact Row Views ----

@Composable
private fun CompactEventRow(
    event: EventSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = event.eventDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.startTime != null) {
                    Text(
                        text = event.startTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "${event.confirmedCount}/${event.maxPlayers ?: 0}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactPlanningRow(
    session: PlanningSession,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = session.responseDeadline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Open",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactGroupRow(
    group: GroupSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Group logo or placeholder
        if (group.logoUrl != null) {
            AsyncImage(
                model = group.logoUrl,
                contentDescription = group.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${group.memberCount} members",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (group.userRole != null) {
            Text(
                text = group.userRole.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---- Raffle Banner ----

@Composable
private fun RaffleBanner(
    title: String,
    prizeName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Prize: $prizeName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// ---- Upgrade Banner ----

@Composable
private fun UpgradeBanner(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock More Features",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Get planning sessions, event chat, more groups, and no ads with Basic \u2014 starting at \$4.99/mo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
