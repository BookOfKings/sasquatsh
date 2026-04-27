package com.sasquatsh.app.views.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.ChatViewModel
import com.sasquatsh.app.views.shared.ErrorBannerView
import kotlinx.coroutines.launch

@Composable
fun ChatPanelView(
    contextType: String,
    contextId: String,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val currentUserId = authState.user?.id
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(contextType, contextId) {
        chatViewModel.configure(contextType, contextId)
        chatViewModel.loadMessages()
    }

    // Start/stop polling based on visibility
    DisposableEffect(isExpanded) {
        if (isExpanded) {
            chatViewModel.startPolling()
        } else {
            chatViewModel.stopPolling()
        }
        onDispose {
            chatViewModel.stopPolling()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && isExpanded) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            // Collapsible header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.messages.isNotEmpty()) {
                        Text(
                            text = "${uiState.messages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chat content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.height(350.dp)
                ) {
                    if (uiState.isLoading && uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No messages yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Start the conversation!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Messages list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.hasMore) {
                                item {
                                    TextButton(
                                        onClick = { chatViewModel.loadEarlierMessages() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Load earlier messages",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            items(uiState.messages, key = { it.id }) { message ->
                                ChatBubbleView(
                                    message = message,
                                    isOwnMessage = message.userId == currentUserId,
                                    onDelete = { chatViewModel.deleteMessage(message.id) },
                                    onReport = {
                                        chatViewModel.reportMessage(
                                            message.id,
                                            "inappropriate",
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Error banner
                    uiState.error?.let { error ->
                        ErrorBannerView(message = error)
                    }

                    // Input bar
                    ChatInputBar(
                        messageText = uiState.messageText,
                        onMessageChange = { chatViewModel.updateMessageText(it) },
                        onSend = { chatViewModel.sendMessage() },
                        isSending = uiState.isSending
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    val canSend = messageText.trim().isNotEmpty() && !isSending

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { newValue ->
                    if (newValue.length <= 1000) {
                        onMessageChange(newValue)
                    }
                },
                placeholder = { Text("Message...") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium,
                enabled = !isSending,
                shape = RoundedCornerShape(20.dp)
            )

            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
