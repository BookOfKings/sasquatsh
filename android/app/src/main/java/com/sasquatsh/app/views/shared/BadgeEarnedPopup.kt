package com.sasquatsh.app.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sasquatsh.app.models.UserBadge

/**
 * Celebratory popup shown when a user earns one or more badges.
 * Port of the iOS BadgeEarnedPopup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeEarnedPopup(
    badges: List<UserBadge>,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Celebration header
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\uD83C\uDF89", // party popper emoji
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (badges.size == 1) "Badge Earned!" else "${badges.size} Badges Earned!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Congratulations on your achievement!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Badge list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { userBadge ->
                    val badge = userBadge.badge
                    val catColor = categoryColor(badge.category)
                    val shape = RoundedCornerShape(12.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(catColor.copy(alpha = 0.05f))
                            .border(1.dp, catColor.copy(alpha = 0.2f), shape)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category emoji circle
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = categoryEmoji(badge.category),
                                fontSize = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = badge.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor
                            )
                        }
                    }
                }
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Awesome!")
            }
        }
    }
}

private fun categoryColor(category: String): Color {
    return when (category) {
        "social" -> Color(0xFF2196F3)       // blue
        "hosting" -> Color(0xFFFF9800)      // orange
        "gaming" -> Color(0xFF9C27B0)       // purple
        "collection" -> Color(0xFF4CAF50)   // green
        "community" -> Color(0xFFE91E63)    // pink
        "planning" -> Color(0xFF009688)     // teal
        "special" -> Color(0xFFFFEB3B)      // yellow
        "veteran" -> Color(0xFFF44336)      // red
        else -> Color(0xFF6366F1)           // primary / indigo
    }
}

private fun categoryEmoji(category: String): String {
    return when (category) {
        "social" -> "\uD83E\uDD1D"      // handshake
        "hosting" -> "\uD83C\uDFB2"     // dice
        "gaming" -> "\uD83C\uDFC6"      // trophy
        "collection" -> "\uD83D\uDCDA"  // books
        "community" -> "\uD83D\uDC65"   // busts in silhouette
        "planning" -> "\uD83D\uDCC5"    // calendar
        "special" -> "\u2B50"           // star
        "veteran" -> "\uD83C\uDF96\uFE0F" // military medal
        else -> "\uD83C\uDFC5"          // sports medal
    }
}
