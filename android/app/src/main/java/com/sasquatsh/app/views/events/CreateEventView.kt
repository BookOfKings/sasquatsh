package com.sasquatsh.app.views.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.AppTimezone
import com.sasquatsh.app.models.DifficultyLevel
import com.sasquatsh.app.models.EventStatus
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.viewmodels.CreateEditEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventView(
    groupId: String? = null,
    onDismiss: () -> Unit,
    viewModel: CreateEditEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        groupId?.let { viewModel.updateGroupId(it) }
        viewModel.loadAvailableGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Game") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save { onDismiss() }
                        },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text(
                            "Create",
                            color = if (uiState.isValid && !uiState.isLoading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Basic Information ──
            EventFormBasicInfo(
                viewModel = viewModel,
                uiState = uiState
            )

            // ── Date & Time ──
            EventFormDateTime(
                viewModel = viewModel,
                uiState = uiState
            )

            // ── Location ──
            EventFormLocation(
                viewModel = viewModel,
                uiState = uiState
            )

            // ── Game Search (Board Games only) ──
            if (uiState.isBoardGame) {
                EventFormGameSearch(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }

            // ── Game Settings ──
            EventFormGameSettings(
                viewModel = viewModel,
                uiState = uiState
            )

            // ── Validation Issues ──
            if (uiState.validationIssues.isNotEmpty()) {
                SectionCard {
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.validationIssues.forEach { issue ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\u26A0",
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // ── Error ──
            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Basic Information Section ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormBasicInfo(
    viewModel: CreateEditEventViewModel,
    uiState: com.sasquatsh.app.viewmodels.CreateEditEventUiState
) {
    SectionCard {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Group picker (if groups available)
        if (uiState.availableGroups.isNotEmpty()) {
            GroupDropdown(
                selectedGroupId = uiState.groupId,
                groups = uiState.availableGroups,
                onSelect = { viewModel.updateGroupId(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Game System
        GameSystemSelector(
            selected = uiState.gameSystem,
            onSelect = { viewModel.updateGameSystem(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.updateTitle(it) },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Date & Time Section ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDateTime(
    viewModel: CreateEditEventViewModel,
    uiState: com.sasquatsh.app.viewmodels.CreateEditEventUiState
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }

    SectionCard {
        Text(
            text = "Date & Time",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date
        OutlinedTextField(
            value = dateFormat.format(uiState.eventDate),
            onValueChange = {},
            label = { Text("Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            enabled = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Start Time
        OutlinedTextField(
            value = timeFormat.format(uiState.startTime),
            onValueChange = {},
            label = { Text("Start Time") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true },
            enabled = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration stepper
        StepperRow(
            label = "Duration",
            value = uiState.durationMinutes,
            unit = "min",
            range = 15..480,
            step = 15,
            onValueChange = { viewModel.updateDurationMinutes(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Setup time stepper
        StepperRow(
            label = "Setup",
            value = uiState.setupMinutes,
            unit = "min",
            range = 0..120,
            step = 5,
            onValueChange = { viewModel.updateSetupMinutes(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Timezone
        TimezoneSelector(
            selected = uiState.timezone,
            onSelect = { viewModel.updateTimezone(it) }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.eventDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.updateEventDate(Date(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { time = uiState.startTime }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false }
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            val newCal = Calendar.getInstance().apply {
                                time = uiState.startTime
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }
                            viewModel.updateStartTime(newCal.time)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

// ─── Location Section ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormLocation(
    viewModel: CreateEditEventViewModel,
    uiState: com.sasquatsh.app.viewmodels.CreateEditEventUiState
) {
    SectionCard {
        Text(
            text = "Location",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location type toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = uiState.useVenueMode,
                onClick = {
                    viewModel.selectVenue(uiState.selectedVenue ?: return@SegmentedButton)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Select Venue")
            }
            SegmentedButton(
                selected = !uiState.useVenueMode,
                onClick = { viewModel.switchToCustomAddress() },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Custom Address")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.useVenueMode) {
            // Venue selection
            if (uiState.selectedVenue != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = uiState.selectedVenue.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.selectedVenue.city}, ${uiState.selectedVenue.state}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hall / Room / Table fields
                OutlinedTextField(
                    value = uiState.venueHall ?: "",
                    onValueChange = { viewModel.updateVenueHall(it.ifEmpty { null }) },
                    label = { Text("Hall") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.venueRoom ?: "",
                    onValueChange = { viewModel.updateVenueRoom(it.ifEmpty { null }) },
                    label = { Text("Room") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.venueTable ?: "",
                    onValueChange = { viewModel.updateVenueTable(it.ifEmpty { null }) },
                    label = { Text("Table") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TextButton(onClick = { /* Navigate to venue selector */ }) {
                    Text("Choose a Venue")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.locationDetails,
                onValueChange = { viewModel.updateLocationDetails(it) },
                label = { Text("Location Details") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Custom address fields
            OutlinedTextField(
                value = uiState.addressLine1,
                onValueChange = { viewModel.updateAddressLine1(it) },
                label = { Text("Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.city,
                onValueChange = { viewModel.updateCity(it) },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            USStateDropdown(
                selected = uiState.state,
                onSelect = { viewModel.updateState(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.postalCode,
                onValueChange = { viewModel.updatePostalCode(it) },
                label = { Text("Postal Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.locationDetails,
                onValueChange = { viewModel.updateLocationDetails(it) },
                label = { Text("Location Details") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Game Search Section (Board Games Only) ───

@Composable
fun EventFormGameSearch(
    viewModel: CreateEditEventViewModel,
    uiState: com.sasquatsh.app.viewmodels.CreateEditEventUiState
) {
    var bggSearchText by remember { mutableStateOf("") }

    SectionCard {
        // BGG search field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = bggSearchText,
                onValueChange = { text ->
                    bggSearchText = text
                    viewModel.searchBGG(text)
                },
                placeholder = { Text("Search BoardGameGeek...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    if (uiState.isSearchingBGG) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else if (bggSearchText.isNotEmpty()) {
                        IconButton(onClick = {
                            bggSearchText = ""
                            viewModel.clearBGGSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
        }

        // Search results
        if (uiState.bggSearchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            uiState.bggSearchResults.take(8).forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.addGame(result)
                            bggSearchText = ""
                            viewModel.clearBGGSearch()
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    if (result.thumbnailUrl != null) {
                        AsyncImage(
                            model = result.thumbnailUrl,
                            contentDescription = result.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Card(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        result.yearPublished?.let { year ->
                            Text(
                                text = "$year",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Loading game details
        if (uiState.isFetchingGameDetails) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading game details...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selected games
        if (uiState.selectedGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            uiState.selectedGames.forEachIndexed { index, game ->
                SelectedGameRow(
                    gameName = game.name,
                    thumbnailUrl = game.thumbnailUrl,
                    isPrimary = index == 0,
                    onSetPrimary = { viewModel.setPrimaryGame(index) },
                    onRemove = { viewModel.removeGame(index) }
                )
                if (index < uiState.selectedGames.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        // Category picker (board games)
        if (uiState.isBoardGame) {
            Spacer(modifier = Modifier.height(12.dp))
            CategoryDropdown(
                selected = uiState.gameCategory,
                onSelect = { viewModel.updateGameCategory(it) }
            )
        }
    }
}

// ─── Game Settings Section ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormGameSettings(
    viewModel: CreateEditEventViewModel,
    uiState: com.sasquatsh.app.viewmodels.CreateEditEventUiState,
    showStatusPicker: Boolean = false
) {
    SectionCard {
        Text(
            text = "Game Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Max Players stepper
        StepperRow(
            label = "Max Players",
            value = uiState.maxPlayers,
            unit = "",
            range = 2..100,
            step = 1,
            onValueChange = { viewModel.updateMaxPlayers(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Host playing toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("I Am Playing", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.hostIsPlaying,
                onCheckedChange = { viewModel.updateHostIsPlaying(it) }
            )
        }

        Text(
            text = if (uiState.hostIsPlaying)
                "${uiState.maxPlayers - 1} spots for others"
            else
                "${uiState.maxPlayers} spots (you're not playing)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Min Age
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Min Age", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = uiState.minAge?.toString() ?: "",
                onValueChange = { text ->
                    viewModel.updateMinAge(text.toIntOrNull())
                },
                placeholder = { Text("None") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Difficulty
        DifficultyDropdown(
            selected = uiState.difficultyLevel,
            onSelect = { viewModel.updateDifficultyLevel(it) }
        )

        // Status picker (only in edit mode)
        if (showStatusPicker) {
            Spacer(modifier = Modifier.height(12.dp))
            StatusDropdown(
                selected = uiState.status,
                onSelect = { viewModel.updateStatus(it) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Public toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Public Game", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.isPublic,
                onCheckedChange = { viewModel.updateIsPublic(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Charity toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Charity Game", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = uiState.isCharityEvent,
                onCheckedChange = { viewModel.updateIsCharityEvent(it) }
            )
        }
    }
}

// ─── Reusable Form Components ───

@Composable
private fun SelectedGameRow(
    gameName: String,
    thumbnailUrl: String?,
    isPrimary: Boolean,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = gameName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gameName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPrimary) {
                BadgeChip(text = "Primary", isPrimary = false)
            } else {
                TextButton(onClick = onSetPrimary) {
                    Text("Set Primary", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StepperRow(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (unit.isNotEmpty()) "$label: $value $unit" else "$label: $value",
            style = MaterialTheme.typography.bodyMedium
        )
        Row {
            TextButton(
                onClick = {
                    val newVal = (value - step).coerceIn(range)
                    onValueChange(newVal)
                },
                enabled = value > range.first
            ) {
                Text("-")
            }
            TextButton(
                onClick = {
                    val newVal = (value + step).coerceIn(range)
                    onValueChange(newVal)
                },
                enabled = value < range.last
            ) {
                Text("+")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSystemSelector(
    selected: GameSystem,
    onSelect: (GameSystem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Game System") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GameSystem.entries.forEach { system ->
                DropdownMenuItem(
                    text = { Text(system.displayName) },
                    onClick = {
                        onSelect(system)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneSelector(
    selected: AppTimezone,
    onSelect: (AppTimezone) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Timezone") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppTimezone.entries.forEach { tz ->
                DropdownMenuItem(
                    text = { Text(tz.displayName) },
                    onClick = {
                        onSelect(tz)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDropdown(
    selectedGroupId: String?,
    groups: List<com.sasquatsh.app.models.GroupSummary>,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = groups.find { it.id == selectedGroupId }?.name ?: "Personal Event",
            onValueChange = {},
            readOnly = true,
            label = { Text("Host for Group") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Personal Event") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                        onSelect(group.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyDropdown(
    selected: DifficultyLevel?,
    onSelect: (DifficultyLevel?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "None",
            onValueChange = {},
            readOnly = true,
            label = { Text("Difficulty") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            DifficultyLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.displayName) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: GameCategory?,
    onSelect: (GameCategory?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "None",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            GameCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.displayName) },
                    onClick = {
                        onSelect(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selected: EventStatus,
    onSelect: (EventStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName) },
                    onClick = {
                        onSelect(status)
                        expanded = false
                    }
                )
            }
        }
    }
}
