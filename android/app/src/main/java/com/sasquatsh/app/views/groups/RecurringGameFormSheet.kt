package com.sasquatsh.app.views.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.CreateRecurringGameInput
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.models.RecurringGame
import com.sasquatsh.app.models.UpdateRecurringGameInput
import com.sasquatsh.app.views.events.USStateDropdown
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringGameFormSheet(
    groupId: String,
    game: RecurringGame?,
    onDismiss: () -> Unit,
    onSave: (CreateRecurringGameInput?, UpdateRecurringGameInput?) -> Unit
) {
    val isEditing = game != null

    var title by remember { mutableStateOf(game?.title ?: "") }
    var description by remember { mutableStateOf(game?.description ?: "") }
    var frequency by remember { mutableStateOf(game?.frequency ?: "weekly") }
    var dayOfWeek by remember { mutableIntStateOf(game?.dayOfWeek ?: 5) } // Friday
    var monthlyWeek by remember { mutableStateOf(game?.monthlyWeek) }
    var startTimeHour by remember { mutableIntStateOf(19) }
    var startTimeMinute by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableIntStateOf(game?.durationMinutes ?: 120) }
    var gameSystem by remember { mutableStateOf(game?.gameSystem?.let { GameSystem.fromValue(it) } ?: GameSystem.BOARD_GAME) }
    var gameTitle by remember { mutableStateOf(game?.gameTitle ?: "") }
    var maxPlayers by remember { mutableIntStateOf(game?.maxPlayers ?: 8) }
    var hostIsPlaying by remember { mutableStateOf(game?.hostIsPlaying ?: true) }
    var isPublic by remember { mutableStateOf(game?.isPublic ?: true) }
    var addressLine1 by remember { mutableStateOf(game?.addressLine1 ?: "") }
    var city by remember { mutableStateOf(game?.city ?: "") }
    var state by remember { mutableStateOf(game?.state ?: "") }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Parse start time from existing game
    LaunchedEffect(game) {
        if (game != null) {
            try {
                val parts = game.startTime.split(":")
                startTimeHour = parts[0].toInt()
                startTimeMinute = parts[1].toInt()
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (isEditing) "Edit Recurring Game" else "New Recurring Game")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val timeString = String.format(
                                    Locale.US, "%02d:%02d", startTimeHour, startTimeMinute
                                )
                                if (isEditing) {
                                    val input = UpdateRecurringGameInput(
                                        title = title,
                                        description = description.ifEmpty { null },
                                        frequency = frequency,
                                        dayOfWeek = dayOfWeek,
                                        monthlyWeek = if (frequency == "monthly") monthlyWeek else null,
                                        startTime = timeString,
                                        durationMinutes = durationMinutes,
                                        maxPlayers = maxPlayers,
                                        hostIsPlaying = hostIsPlaying,
                                        addressLine1 = addressLine1.ifEmpty { null },
                                        city = city.ifEmpty { null },
                                        state = state.ifEmpty { null },
                                        gameSystem = gameSystem.value,
                                        gameTitle = gameTitle.ifEmpty { null },
                                        isPublic = isPublic
                                    )
                                    onSave(null, input)
                                } else {
                                    val input = CreateRecurringGameInput(
                                        groupId = groupId,
                                        title = title,
                                        description = description.ifEmpty { null },
                                        frequency = frequency,
                                        dayOfWeek = dayOfWeek,
                                        monthlyWeek = if (frequency == "monthly") monthlyWeek else null,
                                        startTime = timeString,
                                        durationMinutes = durationMinutes,
                                        maxPlayers = maxPlayers,
                                        hostIsPlaying = hostIsPlaying,
                                        addressLine1 = addressLine1.ifEmpty { null },
                                        city = city.ifEmpty { null },
                                        state = state.ifEmpty { null },
                                        gameSystem = gameSystem.value,
                                        gameTitle = gameTitle.ifEmpty { null },
                                        isPublic = isPublic
                                    )
                                    onSave(input, null)
                                }
                            },
                            enabled = title.trim().isNotEmpty()
                        ) {
                            Text(if (isEditing) "Save" else "Create")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Details
                SectionLabel("Details")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Schedule
                SectionLabel("Schedule")

                // Frequency
                val frequencies = listOf("weekly" to "Weekly", "biweekly" to "Every 2 Weeks", "monthly" to "Monthly")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    frequencies.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            shape = SegmentedButtonDefaults.itemShape(index, frequencies.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Day of Week
                Text(
                    "Day of Week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayNames.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (dayOfWeek == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { dayOfWeek = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dayOfWeek == index) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Monthly week picker
                if (frequency == "monthly") {
                    val weekOptions = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", -1 to "Last")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        weekOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = (monthlyWeek ?: 1) == value,
                                onClick = { monthlyWeek = value },
                                shape = SegmentedButtonDefaults.itemShape(index, weekOptions.size)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Start time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start Time", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d", startTimeHour),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let { if (it in 0..23) startTimeHour = it }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d", startTimeMinute),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let { if (it in 0..59) startTimeMinute = it }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }

                // Duration
                Text(
                    "Duration",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60, 120, 180, 240).forEach { mins ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (durationMinutes == mins) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { durationMinutes = mins }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${mins / 60}h",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (durationMinutes == mins) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Text(
                    text = "Duration: $durationMinutes min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = it.toInt() },
                    valueRange = 30f..480f,
                    steps = ((480 - 30) / 15) - 1
                )

                // Game
                SectionLabel("Game")
                GameSystemPicker(
                    selected = gameSystem,
                    onSelect = { gameSystem = it }
                )
                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text("Game Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Settings
                SectionLabel("Settings")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max Players: $maxPlayers")
                    Row {
                        TextButton(onClick = { if (maxPlayers > 2) maxPlayers-- }) {
                            Text("-")
                        }
                        Text(maxPlayers.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                        TextButton(onClick = { if (maxPlayers < 100) maxPlayers++ }) {
                            Text("+")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Host Is Playing")
                    Switch(checked = hostIsPlaying, onCheckedChange = { hostIsPlaying = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public")
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }

                // Location
                SectionLabel("Location")
                OutlinedTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = it },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                USStateDropdown(selected = state, onSelect = { state = it })

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSystemPicker(
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
