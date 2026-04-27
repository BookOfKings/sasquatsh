package com.sasquatsh.app.views.toolbox

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolboxView(
    onBack: () -> Unit = {},
    onNavigateToFirstPlayer: () -> Unit,
    onNavigateToRoundCounter: () -> Unit,
    onNavigateToTurnTracker: () -> Unit,
    onNavigateToScoreKeeper: () -> Unit,
    onNavigateToRandomGame: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamer Toolbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    "Tools for your game night",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // First Player Pickers
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToFirstPlayer),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "1ST" text badge
                        Card(
                            modifier = Modifier.size(50.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "1ST",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "First Player Pickers",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Multiple ways to pick who goes first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Round Counter
            item {
                ToolCard(
                    icon = Icons.Default.Looks3,
                    title = "Round Counter",
                    description = "Track game rounds with a tap. Autosaves between sessions.",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToRoundCounter
                )
            }

            // Turn Tracker
            item {
                ToolCard(
                    icon = Icons.Default.Sync,
                    title = "Turn Tracker",
                    description = "Keep track of whose turn it is. Never lose your place!",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToTurnTracker
                )
            }

            // Score Keeper
            item {
                ToolCard(
                    icon = Icons.Default.Looks3,
                    title = "Score Keeper",
                    description = "Track scores for any game. Remembers your players!",
                    color = Color(0xFF6366F1),
                    onClick = onNavigateToScoreKeeper
                )
            }

            // Random Game Picker
            item {
                ToolCard(
                    icon = Icons.Default.Casino,
                    title = "Random Game Picker",
                    description = "Can't decide what to play? Let fate choose for you!",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToRandomGame
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
