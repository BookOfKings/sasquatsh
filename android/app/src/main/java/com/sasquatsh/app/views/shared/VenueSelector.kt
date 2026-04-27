package com.sasquatsh.app.views.shared

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.EventLocation
import com.sasquatsh.app.services.EventLocationsService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueSelector(
    eventLocationsService: EventLocationsService,
    onSelect: (EventLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var locations by remember { mutableStateOf<List<EventLocation>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showSubmitVenue by remember { mutableStateOf(false) }

    val filteredLocations = remember(locations, searchText) {
        if (searchText.isEmpty()) locations
        else {
            val query = searchText.lowercase()
            locations.filter {
                it.name.lowercase().contains(query) ||
                        it.city.lowercase().contains(query) ||
                        it.state.lowercase().contains(query)
            }
        }
    }

    suspend fun loadLocations() {
        isLoading = true
        try {
            locations = eventLocationsService.getEventLocations()
        } catch (_: Exception) {
            locations = emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadLocations()
    }

    if (showSubmitVenue) {
        SubmitVenueSheet(
            eventLocationsService = eventLocationsService,
            onSubmitted = {
                showSubmitVenue = false
                // Reload after submission
            },
            onDismiss = { showSubmitVenue = false }
        )
        // Also reload locations when returning
        LaunchedEffect(showSubmitVenue) {
            if (!showSubmitVenue) loadLocations()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Venue") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchBarView(
                text = searchText,
                onTextChange = { searchText = it },
                placeholder = "Search venues...",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingView()
                    }
                }
                filteredLocations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.LocationCity,
                            title = "No Venues Found",
                            message = if (searchText.isEmpty()) "No venues available yet"
                            else "No venues match your search"
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredLocations, key = { it.id }) { location ->
                            VenueRow(
                                location = location,
                                onClick = {
                                    onSelect(location)
                                    onDismiss()
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Submit venue button
            Button(
                onClick = { showSubmitVenue = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Submit a Venue")
            }
        }
    }
}

@Composable
private fun VenueRow(
    location: EventLocation,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            val eventCount = location.eventCount
            if (eventCount != null && eventCount > 0) {
                BadgeView(text = "$eventCount games")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${location.city}, ${location.state}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val schedule = scheduleLabel(location)
        if (schedule.isNotEmpty()) {
            Text(
                text = schedule,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun scheduleLabel(location: EventLocation): String {
    if (location.isPermanent == true) {
        return "Permanent"
    }
    val days = location.recurringDays
    if (!days.isNullOrEmpty()) {
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val labels = days.filter { it in 0..6 }.map { dayNames[it] }
        return "Recurring: ${labels.joinToString(", ")}"
    }
    val start = location.startDate
    val end = location.endDate
    if (start != null && end != null) {
        return "$start - $end"
    }
    if (start != null) {
        return "From $start"
    }
    return ""
}
