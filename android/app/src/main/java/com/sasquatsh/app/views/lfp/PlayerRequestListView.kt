package com.sasquatsh.app.views.lfp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.PlayerRequest
import com.sasquatsh.app.viewmodels.PlayerRequestListViewModel
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.ErrorBannerView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerRequestListView(
    onNavigateToEventDetail: (String) -> Unit,
    viewModel: PlayerRequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateRequest by rememberSaveable { mutableStateOf(false) }
    var filterCity by rememberSaveable { mutableStateOf("") }
    var filterState by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRequests()
        viewModel.loadMyRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // Segmented tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Requests") }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "If someone bails on your game, post a request here. Requests expire in 15 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Need Players button
            item {
                Button(
                    onClick = { showCreateRequest = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Need Players", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    ErrorBannerView(
                        message = error,
                        onDismiss = { /* clear */ }
                    )
                }
            }

            // Action message
            uiState.actionMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (selectedTab == 0) {
                // City/State filter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = filterCity,
                            onValueChange = { filterCity = it },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = filterState,
                            onValueChange = { filterState = it },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.loadRequests(
                                    com.sasquatsh.app.models.PlayerRequestFilters(
                                        city = filterCity.ifEmpty { null },
                                        state = filterState.ifEmpty { null }
                                    )
                                )
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Apply")
                        }
                        OutlinedButton(
                            onClick = {
                                filterCity = ""
                                filterState = ""
                                viewModel.loadRequests()
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Clear")
                        }
                    }
                }

                // Loading
                if (uiState.isLoading && uiState.requests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        }
                    }
                } else if (uiState.requests.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.CheckCircle,
                            title = "No Active Requests",
                            message = "No one needs players right now. Check back later!"
                        )
                    }
                } else {
                    items(uiState.requests, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            onViewEvent = {
                                request.event?.id?.let { onNavigateToEventDetail(it) }
                            }
                        )
                    }
                }
            } else {
                // My Requests tab
                if (uiState.myRequests.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Chat,
                            title = "No Requests",
                            message = "Need players for your game? Post a request!",
                            buttonTitle = "Need Players",
                            onButtonClick = { showCreateRequest = true }
                        )
                    }
                } else {
                    items(uiState.myRequests, key = { it.id }) { request ->
                        MyRequestCard(
                            request = request,
                            onFill = { viewModel.fillRequest(request.id) },
                            onCancel = { viewModel.cancelRequest(request.id) },
                            onDelete = { viewModel.deleteRequest(request.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Request Sheet
    if (showCreateRequest) {
        CreatePlayerRequestView(
            viewModel = viewModel,
            onDismiss = {
                showCreateRequest = false
                viewModel.loadRequests()
            }
        )
    }
}

@Composable
private fun RequestCard(
    request: PlayerRequest,
    onViewEvent: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Host avatar
                request.host?.let { host ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (host.avatarUrl != null) {
                            AsyncImage(
                                model = host.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (host.displayName ?: host.username ?: "?")
                                    .take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.event?.title ?: "Game Night",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hosted by ${request.host?.displayName ?: request.host?.username ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeRemaining(request.expiresAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BadgeView(
                        text = "${request.playerCountNeeded} needed",
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // Event details
            request.event?.let { event ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        event.gameTitle?.let { game ->
                            Text(
                                text = game,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = event.eventDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTime(event.startTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val location = event.locationDetails
                            ?: listOfNotNull(event.city, event.state)
                                .joinToString(", ")
                                .ifEmpty { null }
                        location?.let { loc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Description
            request.description?.takeIf { it.isNotEmpty() }?.let { desc ->
                Text(
                    text = "\"$desc\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }

            // View Event button
            if (request.event?.id != null) {
                Button(
                    onClick = onViewEvent,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("View Event & Join")
                }
            }
        }
    }
}

@Composable
private fun MyRequestCard(
    request: PlayerRequest,
    onFill: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha = if (request.status == "open") 1f else 0.6f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = request.event?.title ?: "Game Night",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    modifier = Modifier.weight(1f, fill = false)
                )
                BadgeView(
                    text = statusText(request.status),
                    color = statusColor(request.status)
                )
                if (request.status == "open") {
                    Text(
                        text = timeRemaining(request.expiresAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            val gameInfo = buildString {
                append(request.event?.gameTitle ?: "No game specified")
                append(" - Needs ${request.playerCountNeeded} player")
                if (request.playerCountNeeded > 1) append("s")
            }
            Text(
                text = gameInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )

            request.description?.takeIf { it.isNotEmpty() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }

            if (request.status == "open") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onFill,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Found Players")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            } else {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    buttonTitle: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (buttonTitle != null && onButtonClick != null) {
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(buttonTitle)
            }
        }
    }
}

@Composable
private fun statusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status) {
        "open" -> MaterialTheme.colorScheme.primaryContainer
        "filled" -> MaterialTheme.colorScheme.secondaryContainer
        "cancelled" -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

private fun statusText(status: String): String {
    return when (status) {
        "open" -> "Active"
        "filled" -> "Filled"
        "cancelled" -> "Cancelled"
        else -> status
    }
}

private fun timeRemaining(expiresAt: String): String {
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = try {
            isoFormat.parse(expiresAt)
        } catch (_: Exception) {
            val isoNoFrac = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            isoNoFrac.parse(expiresAt)
        }
        if (date == null) return ""
        val diff = (date.time - System.currentTimeMillis()) / 1000
        if (diff <= 0) return "Expired"
        val minutes = diff / 60
        val seconds = diff % 60
        if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    } catch (_: Exception) {
        ""
    }
}

private fun formatTime(time: String): String {
    return try {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$hour12:$minute $amPm"
    } catch (_: Exception) {
        time
    }
}
