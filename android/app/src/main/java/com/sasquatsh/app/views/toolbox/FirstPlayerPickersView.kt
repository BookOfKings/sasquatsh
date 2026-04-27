package com.sasquatsh.app.views.toolbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstPlayerPickersView(
    onBack: () -> Unit,
    onNavigateToFingerPicker: () -> Unit,
    onNavigateToSpinWheel: () -> Unit,
    onNavigateToCardDraw: () -> Unit,
    onNavigateToStatement: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("First Player Pickers") },
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
            // Finger Picker
            item {
                PickerCard(
                    emoji = "\u270B",
                    title = "Finger Picker",
                    description = "Everyone puts a finger on the screen \u2014 one gets picked to go first!",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToFingerPicker
                )
            }

            // Spin the Wheel
            item {
                PickerCard(
                    emoji = "\uD83C\uDFB0",
                    title = "Spin the Wheel",
                    description = "Price is Right style! Spin the wheel to pick who goes first.",
                    color = MaterialTheme.colorScheme.error,
                    onClick = onNavigateToSpinWheel
                )
            }

            // High Card Draw
            item {
                PickerCard(
                    emoji = "\u2660",
                    title = "High Card Draw",
                    description = "Draw from a deck of cards \u2014 highest card goes first! Ties get a redraw.",
                    color = Color(0xFF1A3399),
                    onClick = onNavigateToCardDraw
                )
            }

            // Whoever Last Picker
            item {
                PickerCard(
                    emoji = "\u201C\u201C",
                    title = "Whoever Last Picker",
                    description = "3 random prompts to decide who goes first \u2014 no luck needed!",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToStatement
                )
            }
        }
    }
}

@Composable
private fun PickerCard(
    emoji: String,
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
                    Text(
                        emoji,
                        fontSize = 24.sp
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
