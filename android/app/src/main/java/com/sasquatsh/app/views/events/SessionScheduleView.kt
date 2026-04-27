package com.sasquatsh.app.views.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.EventTable
import com.sasquatsh.app.models.GameSession
import com.sasquatsh.app.models.SessionRegistration
import com.sasquatsh.app.services.SessionsService
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.UserAvatarView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScheduleView(
    eventId: String,
    sessionsService: SessionsService,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var tables by remember { mutableStateOf<List<EventTable>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<GameSession?>(null) }

    // Load sessions
    LaunchedEffect(eventId) {
        isLoading = true
        try {
            val response = sessionsService.getEventSessions(eventId)
            tables = response.tables
            sessions = response.sessions
        } catch (e: Exception) {
            error = e.message ?: "Failed to load sessions"
        }
        isLoading = false
    }

    fun sessionsForTable(tableId: String): List<GameSession> {
        return sessions.filter { it.tableId == tableId }
    }

    fun hasConflict(session: GameSession): Boolean {
        return sessions.any { s ->
            s.id != session.id &&
            s.isUserRegistered &&
            s.slotIndex == session.slotIndex &&
            s.tableId != session.tableId
        }
    }

    suspend fun refreshSessions() {
        try {
            val response = sessionsService.getEventSessions(eventId)
            tables = response.tables
            sessions = response.sessions
        } catch (_: Exception) { }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Game Sessions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            if (isLoading && tables.isEmpty()) {
                LoadingView()
            } else if (tables.isEmpty()) {
                Text(
                    "No sessions scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                error?.let { msg ->
                    ErrorBannerView(message = msg, onDismiss = { error = null })
                    Spacer(Modifier.height(8.dp))
                }

                tables.forEach { table ->
                    TableSection(
                        table = table,
                        sessions = sessionsForTable(table.id),
                        onSessionClick = { selectedSession = it }
                    )
                }
            }
        }
    }

    // Session detail bottom sheet
    selectedSession?.let { session ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = sheetState
        ) {
            SessionDetailContent(
                session = session,
                isRegistering = isRegistering,
                hasConflict = hasConflict(session),
                onJoin = {
                    scope.launch {
                        isRegistering = true
                        try {
                            sessionsService.registerForSession(session.id)
                            refreshSessions()
                            selectedSession = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                        isRegistering = false
                    }
                },
                onLeave = {
                    scope.launch {
                        isRegistering = true
                        try {
                            sessionsService.unregisterFromSession(session.id)
                            refreshSessions()
                            selectedSession = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                        isRegistering = false
                    }
                },
                onClose = { selectedSession = null }
            )
        }
    }
}

@Composable
private fun TableSection(
    table: EventTable,
    sessions: List<GameSession>,
    onSessionClick: (GameSession) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = table.tableName ?: "Table ${table.tableNumber}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        if (sessions.isEmpty()) {
            Text(
                "No games at this table",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sessions.forEach { session ->
                SessionCard(session = session, onClick = { onSessionClick(session) })
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SessionCard(
    session: GameSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isUserRegistered)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            session.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = session.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.gameName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${session.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    session.maxPlayers?.let { max ->
                        Text(
                            "${session.registeredCount}/$max",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (session.isFull) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (session.isUserRegistered) {
                BadgeView(
                    text = "Joined",
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(Modifier.width(4.dp))
            } else if (session.isFull) {
                BadgeView(
                    text = "Full",
                    color = MaterialTheme.colorScheme.errorContainer
                )
                Spacer(Modifier.width(4.dp))
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SessionDetailContent(
    session: GameSession,
    isRegistering: Boolean,
    hasConflict: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Game info header
        Row(verticalAlignment = Alignment.CenterVertically) {
            session.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = session.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column {
                Text(session.gameName, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${session.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val min = session.minPlayers
                    val max = session.maxPlayers
                    if (min != null && max != null) {
                        Text(
                            "$min-$max players",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Capacity
        session.maxPlayers?.let { max ->
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spots", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${session.registeredCount}/$max",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (session.isFull) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action button
        when {
            session.isUserRegistered -> {
                Button(
                    onClick = onLeave,
                    enabled = !isRegistering,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Leave Session", style = MaterialTheme.typography.labelLarge)
                }
            }
            session.isFull -> {
                Text(
                    "Session is full",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
            hasConflict -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "You're already in a session at this time slot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Button(
                    onClick = onJoin,
                    enabled = !isRegistering,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        if (isRegistering) "Joining..." else "Join Session",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Player roster
        session.registrations?.takeIf { it.isNotEmpty() }?.let { regs ->
            Spacer(Modifier.height(16.dp))
            Text("Players", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            regs.forEach { reg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    UserAvatarView(
                        url = reg.avatarUrl,
                        name = reg.displayName ?: "Player",
                        size = 28.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        reg.displayName ?: "Player",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (reg.isHostReserved == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Host reserved",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
