package com.sasquatsh.app.views.planning

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.GroupMember
import com.sasquatsh.app.viewmodels.CreatePlanningViewModel
import com.sasquatsh.app.views.events.formatEventDate
import com.sasquatsh.app.views.shared.UserAvatarView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanningView(
    groupId: String,
    members: List<GroupMember>,
    onDismiss: () -> Unit,
    viewModel: CreatePlanningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Planning Session") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save(groupId) { onDismiss() }
                        },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text("Create")
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
            // Session Info
            SectionLabel("Session Info")

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Response deadline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = uiState.responseDeadline
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = java.util.Calendar.getInstance()
                                newCal.set(year, month, day)
                                viewModel.updateResponseDeadline(newCal.time)
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Response Deadline", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    val fmt = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                    Text(
                        text = fmt.format(uiState.responseDeadline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Proposed Dates
            SectionLabel("Proposed Dates")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Date", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = {
                    val today = java.util.Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            viewModel.addDate(LocalDate.of(year, month + 1, day))
                        },
                        today.get(java.util.Calendar.YEAR),
                        today.get(java.util.Calendar.MONTH),
                        today.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Icon(
                        Icons.Filled.Add, "Add date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uiState.proposedDates.forEach { date ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { viewModel.removeDate(date) }) {
                        Icon(
                            Icons.Filled.RemoveCircle, "Remove date",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            // Options
            SectionLabel("Options")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open to entire group")
                Switch(
                    checked = uiState.openToGroup,
                    onCheckedChange = { viewModel.updateOpenToGroup(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Max Participants")
                OutlinedTextField(
                    value = uiState.maxParticipants?.toString() ?: "",
                    onValueChange = { viewModel.updateMaxParticipants(it.toIntOrNull()) },
                    placeholder = { Text("Unlimited") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tables")
                OutlinedTextField(
                    value = uiState.tableCount?.toString() ?: "",
                    onValueChange = { viewModel.updateTableCount(it.toIntOrNull()) },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }

            HorizontalDivider()

            // Invite Members
            SectionLabel("Invite Members")

            members.forEach { member ->
                val isSelected = uiState.selectedMemberIds.contains(member.userId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleMember(member.userId) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarView(
                        url = member.avatarUrl,
                        name = member.displayName,
                        size = 28.dp
                    )
                    Text(
                        text = member.displayName ?: "Member",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            TextButton(
                onClick = {
                    members.forEach { member ->
                        if (!uiState.selectedMemberIds.contains(member.userId)) {
                            viewModel.toggleMember(member.userId)
                        }
                    }
                }
            ) {
                Text("Select All", style = MaterialTheme.typography.labelSmall)
            }

            // Validation issues
            if (uiState.validationIssues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Required",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    uiState.validationIssues.forEach { issue ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning, null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Error
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
