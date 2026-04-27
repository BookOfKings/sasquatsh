package com.sasquatsh.app.views.mtg

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.MtgDeck
import com.sasquatsh.app.viewmodels.DeckListViewModel
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.EmptyStateView
import com.sasquatsh.app.views.shared.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDecksView(
    onNavigateToDeck: (MtgDeck?) -> Unit,
    viewModel: DeckListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deckToDelete by remember { mutableStateOf<MtgDeck?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDecks()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Decks") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDeck(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Deck")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadDecks() },
            modifier = Modifier.padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.decks.isEmpty() -> {
                    LoadingView()
                }
                uiState.decks.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        title = "No Decks Yet",
                        message = "Build your first MTG deck",
                        buttonTitle = "Create Deck",
                        onAction = { onNavigateToDeck(null) }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredDecks, key = { it.id }) { deck ->
                            DeckCard(
                                deck = deck,
                                onClick = { onNavigateToDeck(deck) },
                                onDelete = { deckToDelete = deck }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    deckToDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete Deck") },
            text = { Text("Are you sure you want to delete \"${deck.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeck(deck.id)
                    deckToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeckCard(
    deck: MtgDeck,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Commander / card image
            val imageUrl = deck.commander?.smallImageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = deck.name,
                    modifier = Modifier
                        .size(width = 50.dp, height = 70.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 70.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    deck.formatId?.let { format ->
                        BadgeView(
                            text = format.replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                    Text(
                        text = "${deck.cardCount ?: 0} cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    deck.powerLevel?.let { pl ->
                        Text(
                            text = "PL $pl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
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
