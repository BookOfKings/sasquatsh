package com.sasquatsh.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.sasquatsh.app.data.remote.dto.EventSummaryDto
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import com.sasquatsh.app.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToEvents: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToEvent: (String) -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sasquatsh") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateEvent,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Host a Game")
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // User Header
                item {
                    UserHeaderCard(
                        displayName = firebaseUser?.displayName,
                        email = firebaseUser?.email,
                        avatarUrl = firebaseUser?.photoUrl?.toString(),
                        onProfileClick = onNavigateToProfile,
                    )
                }

                // My Upcoming Games
                item {
                    SectionHeader(
                        title = "My Upcoming Games",
                        icon = Icons.Default.Casino,
                        iconTint = MaterialTheme.colorScheme.primary,
                    )
                }

                if (uiState.myGames.isEmpty()) {
                    item {
                        EmptyCard(
                            message = "You haven't signed up for any games yet.",
                            actionLabel = "Browse Games",
                            onAction = onNavigateToEvents,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        )
                    }
                } else {
                    items(uiState.myGames, key = { it.id }) { game ->
                        GameRow(
                            game = game,
                            onClick = { onNavigateToEvent(game.id) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        )
                    }
                }

                // Games I'm Hosting
                item {
                    SectionHeader(
                        title = "Games I'm Hosting",
                        icon = Icons.Default.Person,
                        iconTint = MaterialTheme.colorScheme.secondary,
                    )
                }

                if (uiState.hostedGames.isEmpty()) {
                    item {
                        EmptyCard(
                            message = "You haven't hosted any games yet.",
                            actionLabel = "Host Your First Game",
                            onAction = onNavigateToCreateEvent,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        )
                    }
                } else {
                    items(uiState.hostedGames, key = { "hosted-${it.id}" }) { game ->
                        GameRow(
                            game = game,
                            onClick = { onNavigateToEvent(game.id) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            showStatus = true,
                        )
                    }
                }

                // My Groups
                item {
                    SectionHeader(
                        title = "My Groups",
                        icon = Icons.Default.Groups,
                        iconTint = MaterialTheme.colorScheme.primary,
                    )
                }

                if (uiState.myGroups.isEmpty()) {
                    item {
                        EmptyCard(
                            message = "You haven't joined any groups yet.",
                            actionLabel = "Browse Groups",
                            onAction = onNavigateToGroups,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        )
                    }
                } else {
                    items(uiState.myGroups, key = { it.id }) { group ->
                        GroupRow(
                            group = group,
                            onClick = { onNavigateToGroup(group.slug ?: group.id) },
                        )
                    }
                }

                // Bottom actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToEvents,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Games")
                        }
                        OutlinedButton(
                            onClick = onNavigateToGroups,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Groups")
                        }
                    }
                }

                // Spacer for FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun UserHeaderCard(
    displayName: String?,
    email: String?,
    avatarUrl: String?,
    onProfileClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onProfileClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(avatarUrl = avatarUrl, displayName = displayName, size = 56.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName ?: "Welcome!",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (email != null) {
                    Text(
                        email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun GameRow(
    game: EventSummaryDto,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    showStatus: Boolean = false,
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            if (game.primaryGameThumbnail != null) {
                AsyncImage(
                    model = game.primaryGameThumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showStatus && game.status != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (game.status == "published")
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                game.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    buildString {
                        append(formatDate(game.eventDate))
                        if (game.startTime != null) append(" at ${formatTime(game.startTime)}")
                        if (game.city != null) {
                            append(" · ${game.city}")
                            if (game.state != null) append(", ${game.state}")
                        }
                        if (showStatus) {
                            append(" · ${game.confirmedCount ?: 0}/${game.maxPlayers} players")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GroupRow(
    group: GroupSummaryDto,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Group logo
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp),
            ) {
                if (group.logoUrl != null) {
                    AsyncImage(
                        model = group.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${group.memberCount ?: 0} member${if ((group.memberCount ?: 0) != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")
        date.format(formatter)
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatTime(timeStr: String): String {
    return try {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minutes = parts[1]
        val ampm = if (hour >= 12) "PM" else "AM"
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        "$hour12:$minutes $ampm"
    } catch (e: Exception) {
        timeStr
    }
}
