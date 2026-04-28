package com.sasquatsh.app.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.GameSystem

@Composable
fun EventCard(
    event: EventSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Game thumbnail or system logo placeholder
            val thumbnail = event.primaryGameThumbnail
            val system = event.gameSystem

            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else if (system != null && system != GameSystem.BOARD_GAME) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = system.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Title row with badges
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val gameTitle = event.gameTitle
                        if (!gameTitle.isNullOrEmpty()) {
                            Text(
                                text = gameTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (system != null && system != GameSystem.BOARD_GAME) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GameSystemChip(gameSystem = system)
                    }

                    if (event.isCharityEvent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        BadgeView(
                            text = "Charity",
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                }

                // Date and time row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatEventCardDate(event.eventDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val startTime = event.startTime
                    if (startTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formatTime(startTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Location and player count row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val city = event.city
                    val state = event.state
                    if (city != null && state != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$city, $state",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val maxPlayers = event.maxPlayers
                    if (maxPlayers != null) {
                        val spotsColor = spotsColor(event.confirmedCount, maxPlayers)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                tint = spotsColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${event.confirmedCount}/$maxPlayers",
                                style = MaterialTheme.typography.bodySmall,
                                color = spotsColor
                            )
                        }
                    }
                }

                // Difficulty and category badges
                val difficulty = event.difficultyLevel
                if (difficulty != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BadgeView(
                            text = difficulty.replaceFirstChar { it.uppercase() },
                            color = difficultyColor(difficulty)
                        )
                        val category = event.gameCategory
                        if (category != null) {
                            val categoryEnum = GameCategory.fromValue(category)
                            BadgeView(
                                text = categoryEnum?.displayName ?: category,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun spotsColor(confirmedCount: Int, maxPlayers: Int): Color {
    val remaining = maxPlayers - confirmedCount
    return when {
        remaining <= 0 -> MaterialTheme.colorScheme.error
        remaining <= 2 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun difficultyColor(level: String): Color {
    return when (level) {
        "beginner" -> MaterialTheme.colorScheme.primaryContainer
        "intermediate" -> MaterialTheme.colorScheme.tertiaryContainer
        "advanced" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

private fun formatTime(time: String): String {
    // Convert "HH:mm" or "HH:mm:ss" to 12-hour format
    return try {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$hour12:$minute $amPm"
    } catch (_: Exception) {
        time
    }
}

private fun formatEventCardDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
        val date = inputFormat.parse(dateString) ?: return dateString
        outputFormat.format(date)
    } catch (_: Exception) {
        dateString
    }
}
