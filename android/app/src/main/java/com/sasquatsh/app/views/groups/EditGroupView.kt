package com.sasquatsh.app.views.groups

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.viewmodels.CreateEditGroupViewModel
import com.sasquatsh.app.viewmodels.GroupDetailViewModel
import com.sasquatsh.app.views.events.USStateDropdown
import com.sasquatsh.app.views.shared.LoadingView
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupView(
    groupId: String,
    onDismiss: () -> Unit,
    viewModel: CreateEditGroupViewModel = hiltViewModel(),
    detailViewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    var showRemoveLogoConfirm by remember { mutableStateOf(false) }
    var groupLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Photo picker
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    // Resize to max 512px and compress
                    val maxDim = 512
                    val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
                    val resized = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap
                    val baos = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    viewModel.uploadLogo(baos.toByteArray())
                }
            } catch (e: Exception) {
                // Error handled in viewModel
            }
        }
    }

    LaunchedEffect(groupId) {
        detailViewModel.loadGroup(groupId)
    }

    LaunchedEffect(detailState.group) {
        detailState.group?.let { group ->
            if (!groupLoaded) {
                viewModel.loadForEdit(group)
                groupLoaded = true
            }
        }
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
                        onClick = { viewModel.save { onDismiss() } },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            D20SpinnerView(size = 20.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                "Save",
                                color = if (uiState.isValid) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (detailState.isLoading && !groupLoaded) {
            LoadingView(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error banner at top
                uiState.error?.let { error ->
                    ErrorBannerView(
                        message = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }

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
                            OutlinedButton(onClick = { photoLauncher.launch("image/*") }) {
                                Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uiState.currentLogoUrl != null) "Change" else "Upload")
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.height(32.dp))
            }
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
