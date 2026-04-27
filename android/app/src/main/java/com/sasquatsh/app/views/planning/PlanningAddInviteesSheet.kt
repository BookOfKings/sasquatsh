package com.sasquatsh.app.views.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.GroupMember
import com.sasquatsh.app.services.GroupsService
import com.sasquatsh.app.services.PlanningService
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.UserAvatarView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningAddInviteesSheet(
    sessionId: String,
    groupId: String,
    groupsService: GroupsService,
    planningService: PlanningService,
    onAdded: () -> Unit,
    onDismiss: () -> Unit
) {
    var members by remember { mutableStateOf<List<GroupMember>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        isLoading = true
        try {
            members = groupsService.getMembers(groupId)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load members"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Invitees") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                try {
                                    planningService.addInvitees(
                                        sessionId = sessionId,
                                        userIds = selectedIds.toList()
                                    )
                                    onAdded()
                                    onDismiss()
                                } catch (e: Exception) {
                                    error = e.localizedMessage ?: "Failed to add invitees"
                                }
                                isSaving = false
                            }
                        },
                        enabled = selectedIds.isNotEmpty() && !isSaving
                    ) {
                        Text("Add (${selectedIds.size})")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingView(message = "Loading members...")
                }
                members.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No group members available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn {
                        items(members, key = { it.userId }) { member ->
                            val selected = selectedIds.contains(member.userId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (selected) {
                                            selectedIds - member.userId
                                        } else {
                                            selectedIds + member.userId
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (selected) Icons.Default.CheckCircle
                                        else Icons.Outlined.Circle,
                                    contentDescription = if (selected) "Selected" else "Not selected",
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )

                                UserAvatarView(
                                    url = member.avatarUrl,
                                    name = member.displayName,
                                    size = 32.dp
                                )

                                Text(
                                    text = member.displayName ?: "Member",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
