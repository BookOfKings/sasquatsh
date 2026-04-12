package com.sasquatsh.app.ui.events

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.data.remote.dto.EventDetailDto
import com.sasquatsh.app.data.remote.dto.EventGameDto
import com.sasquatsh.app.data.remote.dto.EventItemDto
import com.sasquatsh.app.data.remote.dto.RegistrationDto
import com.sasquatsh.app.domain.model.GameSystem
import com.sasquatsh.app.ui.components.GameSystemChip
import com.sasquatsh.app.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.event?.title ?: "Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading && uiState.event == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.event == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Failed to load event",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadEvent() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.event != null -> {
                EventDetailContent(
                    event = uiState.event!!,
                    isRegistering = uiState.isRegistering,
                    onRegister = { viewModel.register() },
                    onUnregister = { viewModel.unregister() },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    event: EventDetailDto,
    isRegistering: Boolean,
    onRegister: () -> Unit,
    onUnregister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmedCount = event.registrations?.count { it.status == "confirmed" } ?: 0
    val isRegistered = event.registrations?.any { it.status == "confirmed" } == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Game thumbnail
        val primaryGame = event.games?.firstOrNull { it.isPrimary == true }
        val thumbnailUrl = primaryGame?.thumbnailUrl
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = event.gameTitle ?: event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Game system chip
            event.gameCategory?.let { category ->
                val gameSystem = try {
                    GameSystem.fromValue(category)
                } catch (_: Exception) {
                    null
                }
                if (gameSystem != null) {
                    GameSystemChip(gameSystem = gameSystem)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Date & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.eventDate,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (!event.startTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val timeText = buildString {
                        append(event.startTime)
                        event.durationMinutes?.let { mins ->
                            append(" (${mins}min)")
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Location
            val locationParts = listOfNotNull(event.address, event.city, event.state).filter { it.isNotBlank() }
            if (locationParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = locationParts.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Host info
            event.host?.let { host ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        avatarUrl = host.avatarUrl,
                        displayName = host.displayName,
                        size = 36.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hosted by",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = host.displayName ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player count with progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$confirmedCount / ${event.maxPlayers} players",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = {
                    if (event.maxPlayers > 0) confirmedCount.toFloat() / event.maxPlayers else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = if (confirmedCount >= event.maxPlayers) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Register / Unregister button
            if (isRegistered) {
                OutlinedButton(
                    onClick = onUnregister,
                    enabled = !isRegistering,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Cancel Registration")
                    }
                }
            } else {
                Button(
                    onClick = onRegister,
                    enabled = !isRegistering && confirmedCount < event.maxPlayers,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            if (confirmedCount >= event.maxPlayers) "Event Full" else "Register",
                        )
                    }
                }
            }

            // Description
            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Items to bring
            if (!event.items.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Items to Bring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                event.items.forEach { item ->
                    ItemRow(item = item)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Games
            if (!event.games.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Games",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                event.games.forEach { game ->
                    GameRow(game = game)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Registrations
            if (!event.registrations.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Players",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                event.registrations.forEach { registration ->
                    RegistrationRow(registration = registration)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Bottom spacing for FAB clearance
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ItemRow(
    item: EventItemDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyMedium,
            )
            item.itemCategory?.let { category ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (item.claimedBy != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Claimed",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.claimedBy.displayName ?: "Claimed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            OutlinedButton(
                onClick = { /* Claim action - would need API extension */ },
            ) {
                Text("Claim", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GameRow(
    game: EventGameDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!game.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = game.thumbnailUrl,
                contentDescription = game.gameName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.gameName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (game.isPrimary == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val details = buildList {
                if (game.minPlayers != null && game.maxPlayers != null) {
                    add("${game.minPlayers}-${game.maxPlayers} players")
                }
                game.playingTime?.let { add("${it}min") }
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RegistrationRow(
    registration: RegistrationDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            avatarUrl = registration.user?.avatarUrl,
            displayName = registration.user?.displayName,
            size = 32.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = registration.user?.displayName ?: "Unknown Player",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = registration.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = when (registration.status) {
                    "confirmed" -> MaterialTheme.colorScheme.primary
                    "pending" -> MaterialTheme.colorScheme.tertiary
                    "cancelled" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
