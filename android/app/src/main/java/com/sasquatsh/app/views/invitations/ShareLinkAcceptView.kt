package com.sasquatsh.app.views.invitations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.services.ShareLinkAcceptResult
import com.sasquatsh.app.services.ShareLinkPreview
import com.sasquatsh.app.services.ShareLinksService
import com.sasquatsh.app.views.shared.LoadingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLinkAcceptView(
    code: String,
    shareLinksService: ShareLinksService,
    onDismiss: () -> Unit
) {
    var preview by remember { mutableStateOf<ShareLinkPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAccepting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var accepted by remember { mutableStateOf(false) }
    var acceptResult by remember { mutableStateOf<ShareLinkAcceptResult?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(code) {
        isLoading = true
        try {
            preview = shareLinksService.previewLink(code)
        } catch (_: Exception) {
            error = "This invite link is invalid or has expired."
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (accepted) "Done" else "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> {
                    LoadingView(message = "Loading invite...")
                }

                error != null -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                accepted -> {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You're in!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    preview?.targetName?.let { name ->
                        Text(
                            text = "Joined: $name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                preview != null -> {
                    Spacer(modifier = Modifier.weight(1f))

                    // Icon
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Target name
                    preview?.targetName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Type label
                    preview?.type?.let { type ->
                        Spacer(modifier = Modifier.height(4.dp))
                        val label = when (type) {
                            "planning_session" -> "Planning Session"
                            "event" -> "Game Event"
                            "group" -> "Group"
                            else -> type.replaceFirstChar { it.uppercase() }
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Invited by
                    preview?.createdBy?.let { name ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Invited by $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Accept button
                    Button(
                        onClick = {
                            scope.launch {
                                isAccepting = true
                                try {
                                    acceptResult = shareLinksService.acceptLink(code)
                                    accepted = true
                                    delay(2000)
                                    onDismiss()
                                } catch (e: Exception) {
                                    error = e.localizedMessage ?: "Failed to accept invite"
                                }
                                isAccepting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAccepting
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Join")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
