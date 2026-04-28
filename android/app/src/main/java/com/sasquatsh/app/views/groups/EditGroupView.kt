package com.sasquatsh.app.views.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.GameGroup
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.viewmodels.CreateEditGroupViewModel
import com.sasquatsh.app.views.events.USStateDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupView(
    group: GameGroup,
    onDismiss: () -> Unit,
    viewModel: CreateEditGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRemoveLogoConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(group) {
        viewModel.loadForEdit(group)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Group") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.save { onDismiss() }
                        },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text("Save")
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
            // Group Logo
            Text(
                text = "Group Logo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val logoUrl = uiState.currentLogoUrl
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Group logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (uiState.isUploadingLogo) {
                    D20SpinnerView(size = 24.dp, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Uploading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Upload button placeholder - photo picker requires Activity context
                        TextButton(onClick = { /* photo picker */ }) {
                            Text(
                                text = if (uiState.currentLogoUrl != null) "Change" else "Upload",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (uiState.currentLogoUrl != null) {
                            TextButton(onClick = { showRemoveLogoConfirm = true }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Group Info
            Text(
                text = "Group Info",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // Group Type
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            GroupTypePicker(
                selected = uiState.groupType,
                onSelect = { viewModel.updateGroupType(it) }
            )

            // Location
            Text(
                text = "Location",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.locationCity,
                onValueChange = { viewModel.updateLocationCity(it) },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            USStateDropdown(
                selected = uiState.locationState,
                onSelect = { viewModel.updateLocationState(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Radius (miles)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.locationRadiusMiles?.toString() ?: "",
                    onValueChange = {
                        viewModel.updateLocationRadiusMiles(it.toIntOrNull())
                    },
                    placeholder = { Text("25") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }

            // Join Policy
            Text(
                text = "Join Policy",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            JoinPolicyPicker(
                selected = uiState.joinPolicy,
                onSelect = { viewModel.updateJoinPolicy(it) }
            )

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

    // Remove logo confirmation
    if (showRemoveLogoConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveLogoConfirm = false },
            title = { Text("Remove Logo") },
            text = { Text("Are you sure you want to remove the group logo?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeLogo()
                    showRemoveLogoConfirm = false
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveLogoConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
