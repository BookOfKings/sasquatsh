package com.sasquatsh.app.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditEventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving && uiState.title.isNotBlank() && uiState.eventDate.isNotBlank(),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Error
                if (uiState.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            uiState.error!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Basic Info Section
                SectionHeader("Basic Info")

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Date & Time Section
                SectionHeader("Date & Time")

                // Date picker
                var showDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = formatDisplayDate(uiState.eventDate),
                    onValueChange = {},
                    label = { Text("Event Date *") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = try {
                            java.time.LocalDate.parse(uiState.eventDate)
                                .atStartOfDay(java.time.ZoneOffset.UTC)
                                .toInstant().toEpochMilli()
                        } catch (e: Exception) { null }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = java.time.Instant.ofEpochMilli(millis)
                                        .atZone(java.time.ZoneOffset.UTC)
                                        .toLocalDate()
                                    viewModel.updateEventDate(date.toString())
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Time picker
                    var showTimePicker by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = formatDisplayTime(uiState.startTime),
                        onValueChange = {},
                        label = { Text("Start Time") },
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )

                    if (showTimePicker) {
                        val initialHour = try { uiState.startTime.split(":")[0].toInt() } catch (e: Exception) { 18 }
                        val initialMinute = try { uiState.startTime.split(":")[1].toInt() } catch (e: Exception) { 0 }
                        val timePickerState = rememberTimePickerState(
                            initialHour = initialHour,
                            initialMinute = initialMinute,
                            is24Hour = false,
                        )
                        AlertDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val h = timePickerState.hour.toString().padStart(2, '0')
                                    val m = timePickerState.minute.toString().padStart(2, '0')
                                    viewModel.updateStartTime("$h:$m:00")
                                    showTimePicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                            },
                            text = {
                                TimePicker(state = timePickerState)
                            },
                        )
                    }

                    OutlinedTextField(
                        value = uiState.durationMinutes,
                        onValueChange = { viewModel.updateDurationMinutes(it) },
                        label = { Text("Duration (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Location Section
                SectionHeader("Location")

                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = { viewModel.updateAddress(it) },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = { viewModel.updateCity(it) },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.state,
                        onValueChange = { viewModel.updateState(it) },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f),
                    )
                }

                OutlinedTextField(
                    value = uiState.postalCode,
                    onValueChange = { viewModel.updatePostalCode(it) },
                    label = { Text("Zip Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.5f),
                )

                // Player Settings Section
                SectionHeader("Player Settings")

                OutlinedTextField(
                    value = uiState.maxPlayers,
                    onValueChange = { viewModel.updateMaxPlayers(it) },
                    label = { Text("Max Players") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.5f),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = uiState.hostIsPlaying,
                        onCheckedChange = { viewModel.updateHostIsPlaying(it) },
                    )
                    Text("I'm playing too", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = uiState.minAge,
                    onValueChange = { viewModel.updateMinAge(it) },
                    label = { Text("Minimum Age (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.5f),
                )

                // Difficulty
                SectionHeader("Difficulty")

                val difficulties = listOf("" to "Any", "beginner" to "Beginner", "intermediate" to "Intermediate", "advanced" to "Advanced")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    difficulties.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = uiState.difficultyLevel == value,
                            onClick = { viewModel.updateDifficultyLevel(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, difficulties.size),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Status & Visibility
                SectionHeader("Status & Visibility")

                val statuses = listOf("draft" to "Draft", "published" to "Published", "cancelled" to "Cancelled")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    statuses.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = uiState.status == value,
                            onClick = { viewModel.updateStatus(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = uiState.isPublic,
                        onCheckedChange = { viewModel.updateIsPublic(it) },
                    )
                    Text("Public event (visible to everyone)", style = MaterialTheme.typography.bodyMedium)
                }

                // Save Button
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isSaving && uiState.title.isNotBlank() && uiState.eventDate.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Save Changes")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private fun formatDisplayDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatDisplayTime(timeStr: String): String {
    if (timeStr.isBlank()) return ""
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
