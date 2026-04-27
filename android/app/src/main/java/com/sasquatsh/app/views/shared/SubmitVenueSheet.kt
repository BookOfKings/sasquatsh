package com.sasquatsh.app.views.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.CreateEventLocationInput
import com.sasquatsh.app.services.EventLocationsService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LocationType(val label: String) {
    TEMPORARY("Temporary"),
    PERMANENT("Permanent"),
    RECURRING("Recurring")
}

private val DAY_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private val US_STATES = listOf(
    "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
    "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
    "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
    "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
    "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitVenueSheet(
    eventLocationsService: EventLocationsService,
    onSubmitted: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var locationType by remember { mutableStateOf(LocationType.TEMPORARY) }
    var startDate by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(System.currentTimeMillis() + 86400_000L * 3) }
    var recurringDays by remember { mutableStateOf(setOf<Int>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    val isValid = name.isNotBlank() && city.isNotBlank() && state.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit a Venue") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSubmitting = true
                                error = null

                                val input = CreateEventLocationInput(
                                    name = name.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    venue = venue.trim().ifEmpty { null },
                                    isPermanent = if (locationType == LocationType.PERMANENT) true else null,
                                    recurringDays = if (locationType == LocationType.RECURRING)
                                        recurringDays.sorted() else null,
                                    startDate = if (locationType == LocationType.TEMPORARY && startDate != null)
                                        dateFormat.format(Date(startDate!!)) else null,
                                    endDate = if (locationType == LocationType.TEMPORARY && endDate != null)
                                        dateFormat.format(Date(endDate!!)) else null
                                )

                                try {
                                    eventLocationsService.createEventLocation(input)
                                    showSuccess = true
                                    delay(1500)
                                    onSubmitted()
                                    onDismiss()
                                } catch (e: Exception) {
                                    error = e.localizedMessage ?: "Failed to submit venue"
                                }
                                isSubmitting = false
                            }
                        },
                        enabled = isValid && !isSubmitting
                    ) {
                        Text("Submit")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Venue Details section
            Text(
                text = "Venue Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // State picker - clickable disabled text field
            OutlinedTextField(
                value = state,
                onValueChange = {},
                label = { Text("State") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStatePicker = true },
                enabled = false,
                singleLine = true
            )

            OutlinedTextField(
                value = venue,
                onValueChange = { venue = it },
                label = { Text("Venue / Building (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location Type section
            Text(
                text = "Location Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                LocationType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = locationType == type,
                        onClick = { locationType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LocationType.entries.size
                        )
                    ) {
                        Text(type.label)
                    }
                }
            }

            when (locationType) {
                LocationType.TEMPORARY -> {
                    OutlinedTextField(
                        value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = {},
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        enabled = false,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = {},
                        label = { Text("End Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        enabled = false,
                        singleLine = true
                    )
                }
                LocationType.RECURRING -> {
                    Text(
                        text = "Recurring Days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DAY_NAMES.forEachIndexed { index, dayName ->
                            val isSelected = recurringDays.contains(index)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        recurringDays = if (isSelected) {
                                            recurringDays - index
                                        } else {
                                            recurringDays + index
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                LocationType.PERMANENT -> {
                    // No additional fields
                }
            }

            // Error display
            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Success display
            if (showSuccess) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Venue submitted for approval!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // State picker dialog
    if (showStatePicker) {
        AlertDialog(
            onDismissRequest = { showStatePicker = false },
            title = { Text("Select State") },
            text = {
                LazyColumn {
                    items(US_STATES.size) { index ->
                        Text(
                            text = US_STATES[index],
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    state = US_STATES[index]
                                    showStatePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (state == US_STATES[index]) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatePicker = false }) { Text("Cancel") }
            }
        )
    }
}
