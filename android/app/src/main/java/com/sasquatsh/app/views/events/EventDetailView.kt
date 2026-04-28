package com.sasquatsh.app.views.events

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.Event
import com.sasquatsh.app.models.EventGameSummary
import com.sasquatsh.app.models.EventItem
import com.sasquatsh.app.models.EventRegistration
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.viewmodels.EventDetailViewModel
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailView(
    eventId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var chatExpanded by remember { mutableStateOf(false) }

    val currentUserId = viewModel.getCurrentUserId()
    val event = uiState.event
    val isHost = currentUserId != null && event != null && viewModel.isHost(currentUserId)
    val isRegistered = currentUserId != null && event != null && viewModel.isRegistered(currentUserId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = event?.title ?: "Game",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    if (event != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Calendar") },
                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        addToCalendar(context, event)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        shareEvent(context, event)
                                    }
                                )
                                if (isHost) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToEdit(event.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.event != null,
            onRefresh = { eventId.let { viewModel.loadEvent(it) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.event == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            } else if (event != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Header Section
                    item {
                        HeaderSection(event = event)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Error banner
                    uiState.error?.let { error ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Action message
                    uiState.actionMessage?.let { msg ->
                        item {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Action Buttons (Register / Cancel Registration)
                    item {
                        ActionButtons(
                            event = event,
                            currentUserId = currentUserId,
                            isHost = isHost,
                            isRegistered = isRegistered,
                            onRegister = { viewModel.register() },
                            onCancelRegistration = { viewModel.cancelRegistration() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Details Section
                    item {
                        DetailsSection(event = event, context = context)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Description
                    if (!event.description.isNullOrEmpty()) {
                        item {
                            SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = "About This Game",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Games Section
                    item {
                        GamesSection(
                            games = event.games,
                            isHost = isHost,
                            onRemoveGame = { gameId -> viewModel.removeGame(gameId) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Players and Items Section
                    item {
                        PlayersAndItemsSection(
                            event = event,
                            isHost = isHost,
                            currentUserId = currentUserId,
                            onClaimItem = { viewModel.claimItem(it) },
                            onUnclaimItem = { viewModel.unclaimItem(it) },
                            onShowAddItem = { showAddItemDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Chat Section (collapsible)
                    item {
                        SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chatExpanded = !chatExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (chatExpanded)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (chatExpanded) "Collapse" else "Expand"
                                )
                            }
                            AnimatedVisibility(visible = chatExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        text = "Chat is available for registered players",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Game") },
            text = { Text("Are you sure you want to delete this game? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteEvent { onNavigateBack() }
                    }
                ) {
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

    // Add Item dialog
    if (showAddItemDialog) {
        var itemName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (itemName.isNotBlank()) {
                            viewModel.addItem(itemName.trim(), "other")
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Header Section ───

@Composable
private fun HeaderSection(event: Event) {
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Title
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Game title
        if (!event.gameTitle.isNullOrEmpty()) {
            Text(
                text = event.gameTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            event.gameSystem?.let { system ->
                if (system != GameSystem.BOARD_GAME) {
                    BadgeChip(text = system.shortName, isPrimary = true)
                }
            }
            BadgeChip(
                text = event.status.replaceFirstChar { it.uppercase() },
                isPrimary = false
            )
            if (event.isCharityEvent) {
                BadgeChip(text = "Charity", isPrimary = false)
            }
            if (event.hostIsPlaying == false) {
                BadgeChip(text = "Not Playing", isPrimary = false)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Host info
        event.host?.let { host ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder
                AsyncImage(
                    model = host.avatarUrl,
                    contentDescription = host.displayName ?: "Host",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hosted by",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = host.displayName ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─── Details Section ───

@Composable
private fun DetailsSection(event: Event, context: android.content.Context) {
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date & Time row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailRow(
                icon = Icons.Default.DateRange,
                text = formatEventDate(event.eventDate),
                modifier = Modifier
                    .clickable { addToCalendar(context, event) }
            )
            DetailRow(
                icon = Icons.Default.DateRange,
                text = formatStartTime(event.startTime)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Duration & Players row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DetailRow(
                icon = Icons.Default.DateRange,
                text = "${event.durationMinutes ?: 0} min"
            )
            DetailRow(
                icon = Icons.Default.Person,
                text = "${event.confirmedCount}/${event.maxPlayers ?: 0} players"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Player progress bar
        val maxPlayers = event.maxPlayers ?: 0
        if (maxPlayers > 0) {
            LinearProgressIndicator(
                progress = { event.confirmedCount.toFloat() / maxPlayers },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Address
        val city = event.city
        val state = event.state
        if (city != null && state != null) {
            val address = buildString {
                event.addressLine1?.let { append("$it, ") }
                append("$city, $state")
            }
            DetailRow(
                icon = Icons.Default.LocationOn,
                text = address,
                modifier = Modifier.clickable {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(mapIntent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Venue details (hall/room/table)
        val venueParts = listOfNotNull(
            event.venueHall?.let { "Hall: $it" },
            event.venueRoom?.let { "Room: $it" },
            event.venueTable?.let { "Table: $it" }
        )
        if (venueParts.isNotEmpty()) {
            DetailRow(
                icon = Icons.Default.LocationOn,
                text = venueParts.joinToString(" \u00B7 ")
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Timezone
        event.timezone?.let { tz ->
            com.sasquatsh.app.models.AppTimezone.fromValue(tz)?.let { appTz ->
                DetailRow(
                    icon = Icons.Default.DateRange,
                    text = appTz.displayName
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Difficulty
        event.difficultyLevel?.let { difficulty ->
            DetailRow(
                icon = Icons.Default.Person,
                text = difficulty.replaceFirstChar { it.uppercase() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Min age
        event.minAge?.let { minAge ->
            DetailRow(
                icon = Icons.Default.Person,
                text = "Ages $minAge+"
            )
        }
    }
}

// ─── Action Buttons ───

@Composable
private fun ActionButtons(
    event: Event,
    currentUserId: String?,
    isHost: Boolean,
    isRegistered: Boolean,
    onRegister: () -> Unit,
    onCancelRegistration: () -> Unit
) {
    if (currentUserId == null || isHost) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (isRegistered) {
            Button(
                onClick = onCancelRegistration,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text("Cancel Registration")
            }
        } else if (event.confirmedCount < (event.maxPlayers ?: Int.MAX_VALUE)) {
            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Text("Register")
            }
        } else {
            Text(
                text = "Game is full",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ─── Games Section ───

@Composable
private fun GamesSection(
    games: List<EventGameSummary>?,
    isHost: Boolean,
    onRemoveGame: (String) -> Unit
) {
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Games",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (games.isNullOrEmpty()) {
            Text(
                text = "No games added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            games.forEach { game ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    game.thumbnailUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = game.gameName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.gameName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val minP = game.minPlayers
                        val maxP = game.maxPlayers
                        if (minP != null && maxP != null) {
                            Text(
                                text = "$minP-$maxP players",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (game.isPrimary) {
                        BadgeChip(text = "Primary", isPrimary = false)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (isHost) {
                        IconButton(
                            onClick = { onRemoveGame(game.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove game",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Players and Items Section ───

@Composable
private fun PlayersAndItemsSection(
    event: Event,
    isHost: Boolean,
    currentUserId: String?,
    onClaimItem: (String) -> Unit,
    onUnclaimItem: (String) -> Unit,
    onShowAddItem: () -> Unit
) {
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Players (${event.confirmedCount}/${event.maxPlayers ?: 0})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isHost) {
                TextButton(onClick = onShowAddItem) {
                    Text("+ Add Item", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Player list
        val registrations = event.registrations
        if (registrations.isNullOrEmpty()) {
            Text(
                text = "No players registered yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            registrations.forEach { reg ->
                PlayerRow(
                    registration = reg,
                    items = event.items?.filter { it.claimedByUserId == reg.userId }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Unclaimed items
        val unclaimedItems = event.items?.filter { it.claimedByUserId == null } ?: emptyList()
        if (unclaimedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Still Needed",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            unclaimedItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "(${item.itemCategory.replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.claimedByUserId == currentUserId) {
                        TextButton(onClick = { onUnclaimItem(item.id) }) {
                            Text("Unclaim", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        TextButton(onClick = { onClaimItem(item.id) }) {
                            Text(
                                "I'll bring this",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Player Row ───

@Composable
private fun PlayerRow(
    registration: EventRegistration,
    items: List<EventItem>?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = registration.user?.avatarUrl,
            contentDescription = registration.user?.displayName ?: "Player",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = registration.user?.displayName ?: "Player",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Items this player is bringing
            items?.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.quantityNeeded > 1) {
                        Text(
                            text = " x${item.quantityNeeded}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        BadgeChip(
            text = registration.status.replaceFirstChar { it.uppercase() },
            isPrimary = registration.status == "confirmed"
        )
    }
}

// ─── Shared Components ───

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BadgeChip(
    text: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPrimary)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isPrimary)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// ─── Calendar & Share helpers ───

private fun addToCalendar(context: android.content.Context, event: Event) {
    try {
        val startMillis = parseEventToMillis(event)
        val durationMillis = (event.durationMinutes ?: 60) * 60 * 1000L

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + durationMillis)
            event.description?.let {
                putExtra(CalendarContract.Events.DESCRIPTION, it)
            }
            val location = buildString {
                event.addressLine1?.let { append("$it, ") }
                event.city?.let { append("$it, ") }
                event.state?.let { append(it) }
            }
            if (location.isNotEmpty()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Calendar app not available
    }
}

private fun shareEvent(context: android.content.Context, event: Event) {
    val shareText = buildString {
        append("${event.title}\n")
        append("Date: ${formatEventDate(event.eventDate)}\n")
        event.startTime?.let { append("Time: ${formatStartTime(it)}\n") }
        val location = listOfNotNull(event.city, event.state).joinToString(", ")
        if (location.isNotEmpty()) append("Location: $location\n")
        append("\nhttps://sasquatsh.com/events/${event.id}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Game"))
}

private fun parseEventToMillis(event: Event): Long {
    return try {
        val dateTimeStr = "${event.eventDate} ${event.startTime ?: "12:00"}"
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        event.timezone?.let { tz ->
            format.timeZone = java.util.TimeZone.getTimeZone(tz)
        }
        format.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}
