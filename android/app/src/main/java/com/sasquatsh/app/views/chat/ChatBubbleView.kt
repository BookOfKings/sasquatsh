package com.sasquatsh.app.views.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatBubbleView(
    message: ChatMessage,
    isOwnMessage: Boolean,
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar (other users only)
        if (!isOwnMessage) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = message.user?.avatarUrl
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (message.user?.displayName ?: "?").take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // Display name (other users only)
            if (!isOwnMessage) {
                Text(
                    text = message.user?.displayName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Message bubble
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Context menu on long press
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isOwnMessage) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete?.invoke()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = {
                                showMenu = false
                                onReport?.invoke()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            // Timestamp
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spacer for own messages (push left)
        if (isOwnMessage) {
            // No trailing spacer needed; the Row's Arrangement.End handles alignment
        } else {
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
}

private fun formattedTime(createdAt: String): String {
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = try {
            isoFormat.parse(createdAt)
        } catch (_: Exception) {
            val isoNoFrac = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            isoNoFrac.parse(createdAt)
        }
        if (date == null) return ""

        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        calendar.time = date

        val displayFormat = when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.US)
            }
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
                SimpleDateFormat("'Yesterday' h:mm a", Locale.US)
            }
            else -> {
                SimpleDateFormat("MMM d, h:mm a", Locale.US)
            }
        }
        displayFormat.format(date)
    } catch (_: Exception) {
        ""
    }
}
