package com.sasquatsh.app.views.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.Badge
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.viewmodels.BadgesViewModel
import com.sasquatsh.app.views.shared.LoadingView

private val categories = listOf(
    "hosting" to "Hosting",
    "attendance" to "Attendance",
    "planning" to "Planning",
    "social" to "Social",
    "collection" to "Collection",
    "game_system" to "Game Systems",
    "items" to "Items",
    "special" to "Special"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesView(
    viewModel: BadgesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBadges()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Badges") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats bar
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.earnedBadges.size}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Earned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.allBadges.size}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.computeBadges() },
                        enabled = !uiState.isComputing
                    ) {
                        if (uiState.isComputing) {
                            D20SpinnerView(size = 18.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check")
                    }
                }
            }

            // Newly earned banner
            if (uiState.newlyEarned > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "You earned ${uiState.newlyEarned} new badge${if (uiState.newlyEarned == 1) "" else "s"}!",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Category filter
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All") },
                    leadingIcon = if (uiState.selectedCategory == null) ({
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    }) else null
                )
                categories.forEach { (key, label) ->
                    FilterChip(
                        selected = uiState.selectedCategory == key,
                        onClick = { viewModel.selectCategory(key) },
                        label = { Text(label) },
                        leadingIcon = if (uiState.selectedCategory == key) ({
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        }) else null
                    )
                }
            }

            if (uiState.isLoading) {
                LoadingView()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredBadges, key = { it.id }) { badge ->
                        val earned = uiState.earnedBadgeIds.contains(badge.id)
                        val userBadge = uiState.earnedBadges.find { it.badgeId == badge.id }
                        val pinned = userBadge?.isPinned ?: false

                        BadgeCard(
                            badge = badge,
                            earned = earned,
                            pinned = pinned,
                            onClick = {
                                if (earned) viewModel.togglePin(badge.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(
    badge: Badge,
    earned: Boolean,
    pinned: Boolean,
    onClick: () -> Unit
) {
    val tierColor = tierColor(badge.tier)
    val iconColor = if (earned) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (earned) 1f else 0.6f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (earned) tierColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        categoryIcon(badge.category),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                badge.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (earned) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (earned) 0.8f else 0.4f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tier badge
            Text(
                badge.tier.replaceFirstChar { it.uppercase() },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (earned) tierColor
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .background(
                        if (earned) tierColor.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .border(
                        0.5.dp,
                        if (earned) tierColor.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun tierColor(tier: String): Color = when (tier) {
    "bronze" -> Color(0xFFCC8033)
    "silver" -> Color(0xFFA1A1A1)
    "gold" -> Color(0xFFFFD700)
    "platinum" -> Color(0xFF6750A4)
    else -> Color(0xFF79747E)
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "hosting" -> Icons.Default.Home
    "attendance" -> Icons.Default.Person
    "planning" -> Icons.Default.CalendarMonth
    "social" -> Icons.Default.Groups
    "collection" -> Icons.Default.Casino
    "game_system" -> Icons.Default.Gamepad
    "items" -> Icons.Default.Inventory2
    "special" -> Icons.Default.Star
    else -> Icons.Default.QuestionMark
}
