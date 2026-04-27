package com.sasquatsh.app.views.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.Event
import com.sasquatsh.app.viewmodels.CreateEditEventViewModel
import com.sasquatsh.app.viewmodels.EventDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventView(
    eventId: String,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: CreateEditEventViewModel = hiltViewModel(),
    detailViewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var eventLoaded by remember { mutableStateOf(false) }

    // Load event data for editing
    LaunchedEffect(eventId) {
        detailViewModel.loadEvent(eventId)
    }

    // Once the event is loaded, populate the form
    LaunchedEffect(detailState.event) {
        detailState.event?.let { event ->
            if (!eventLoaded) {
                viewModel.loadForEdit(event)
                eventLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Game") },
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
                            "Save",
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
        if (detailState.isLoading && !eventLoaded) {
            // Loading state while fetching the event
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading event...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
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

                // ── Game System Config ──
                // (Game system-specific sections would go here, e.g. MtgConfigFormSections)

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

                // ── Game Settings (with status picker in edit mode) ──
                EventFormGameSettings(
                    viewModel = viewModel,
                    uiState = uiState,
                    showStatusPicker = true
                )

                // ── Error ──
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // ── Delete button ──
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Delete Event")
                }

                Spacer(modifier = Modifier.height(32.dp))
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
                        detailViewModel.deleteEvent { onDeleted() }
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
}
