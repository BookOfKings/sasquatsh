package com.sasquatsh.app.views.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sasquatsh.app.models.DateVote
import com.sasquatsh.app.models.PlanningDate
import com.sasquatsh.app.models.PlanningInvitee
import com.sasquatsh.app.views.shared.UserAvatarView
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DateAvailabilityGrid(
    dates: List<PlanningDate>,
    invitees: List<PlanningInvitee>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val nameWidth: Dp = maxOf(90.dp, screenWidth * 0.28f)
    val dateColumnWidth: Dp = if (dates.isEmpty()) 60.dp
        else maxOf(50.dp, (screenWidth - nameWidth) / dates.size)

    val scrollState = rememberScrollState()
    val totalHeight = ((invitees.size + 2) * 44).dp

    Box(modifier = modifier.height(totalHeight)) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier.widthIn(min = screenWidth)
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(nameWidth))

                    dates.forEach { date ->
                        Column(
                            modifier = Modifier.width(dateColumnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatShortDate(date.proposedDate),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            if (!date.startTime.isNullOrEmpty()) {
                                Text(
                                    text = formatGridTime(date.startTime),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Invitee rows
                invitees.forEach { invitee ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(nameWidth),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UserAvatarView(
                                url = invitee.user?.avatarUrl,
                                name = invitee.user?.displayName,
                                size = 24.dp,
                                userId = invitee.userId
                            )
                            Text(
                                text = invitee.user?.displayName ?: "Player",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        dates.forEach { date ->
                            val vote = date.votes?.firstOrNull { it.userId == invitee.userId }
                            Box(
                                modifier = Modifier
                                    .width(dateColumnWidth)
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AvailabilityCellIcon(invitee = invitee, vote = vote)
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Total row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(nameWidth)
                    )

                    dates.forEach { date ->
                        val count = date.availableCount ?: countAvailable(date)
                        val total = invitees.size
                        Text(
                            text = "$count/$total",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (count > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(dateColumnWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityCellIcon(
    invitee: PlanningInvitee,
    vote: DateVote?
) {
    when {
        invitee.cannotAttendAny -> {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Cannot attend",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
        !invitee.hasResponded -> {
            Icon(
                Icons.Default.Help,
                contentDescription = "No response",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        vote != null -> {
            Icon(
                if (vote.isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (vote.isAvailable) "Available" else "Not available",
                tint = if (vote.isAvailable) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
        else -> {
            Icon(
                Icons.Default.RemoveCircle,
                contentDescription = "No vote",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatShortDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = inputFormat.parse(dateStr) ?: return dateStr
        val outputFormat = SimpleDateFormat("EEE\nMMM d", Locale.US)
        outputFormat.format(date)
    } catch (_: Exception) {
        dateStr
    }
}

private fun countAvailable(date: PlanningDate): Int {
    return date.votes?.count { it.isAvailable } ?: 0
}

private fun formatGridTime(timeString: String): String {
    return try {
        val parts = timeString.split(":")
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
        timeString
    }
}
