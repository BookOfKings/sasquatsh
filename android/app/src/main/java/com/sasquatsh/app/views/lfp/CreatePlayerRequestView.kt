package com.sasquatsh.app.views.lfp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.viewmodels.PlayerRequestListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlayerRequestView(
    viewModel: PlayerRequestListViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEventId by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var playerCountNeeded by rememberSaveable { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var eventDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadHostedEvents()
    }

    // Filter to upcoming events
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val upcomingEvents = uiState.hostedEvents.filter {
        it.eventDate >= todayStr && it.status != "cancelled"
    }

    // Auto-select first event
    LaunchedEffect(upcomingEvents) {
        if (selectedEventId.isEmpty() && upcomingEvents.isNotEmpty()) {
            selectedEventId = upcomingEvents.first().id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Text(
                    text = "Need Players",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        if (selectedEventId.isNotEmpty()) {
                            isLoading = true
                            viewModel.createRequest(
                                eventId = selectedEventId,
                                description = description.ifEmpty { null },
                                playerCount = playerCountNeeded
                            )
                            isLoading = false
                            if (uiState.error == null) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = selectedEventId.isNotEmpty() && !isLoading
                ) {
                    Text("Post")
                }
            }

            // Info text
            Text(
                text = "Post an urgent request for fill-in players. Your request will be visible for 15 minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Select Event
            Text(
                text = "Select Event",
                style = MaterialTheme.typography.titleSmall
            )

            if (upcomingEvents.isEmpty()) {
                Text(
                    text = "You need to create an event first before requesting players.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = eventDropdownExpanded,
                    onExpandedChange = { eventDropdownExpanded = it }
                ) {
                    val selectedEvent = upcomingEvents.find { it.id == selectedEventId }
                    OutlinedTextField(
                        value = selectedEvent?.let { "${it.title} - ${formatDropdownDate(it.eventDate)}" }
                            ?: "Choose an event...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = eventDropdownExpanded,
                        onDismissRequest = { eventDropdownExpanded = false }
                    ) {
                        upcomingEvents.forEach { event ->
                            DropdownMenuItem(
                                text = { Text("${event.title} - ${formatDropdownDate(event.eventDate)}") },
                                onClick = {
                                    selectedEventId = event.id
                                    eventDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Details
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleSmall
            )

            // Player count stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Players Needed: $playerCountNeeded",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = {
                            if (playerCountNeeded > 1) playerCountNeeded--
                        },
                        enabled = playerCountNeeded > 1
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalIconButton(
                        onClick = {
                            if (playerCountNeeded < 20) playerCountNeeded++
                        },
                        enabled = playerCountNeeded < 20
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Message (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Error
            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatDropdownDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
        val date = inputFormat.parse(dateString) ?: return dateString
        outputFormat.format(date)
    } catch (_: Exception) {
        dateString
    }
}
