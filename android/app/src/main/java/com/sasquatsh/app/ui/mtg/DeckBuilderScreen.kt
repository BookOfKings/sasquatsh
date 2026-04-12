package com.sasquatsh.app.ui.mtg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.data.remote.dto.MtgDeckCardDto
import com.sasquatsh.app.data.remote.dto.ScryfallCardDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBuilderScreen(
    deckId: String?,
    viewModel: DeckBuilderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        if (deckId != null && deckId != "new") {
            viewModel.loadDeck(deckId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (deckId == "new") "New Deck" else uiState.deck?.name ?: "Deck") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Cards")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        "${uiState.deck?.cards?.sumOf { it.quantity } ?: 0} cards",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.deck?.cards ?: emptyList(), key = { it.scryfallId }) { card ->
                    DeckCardRow(card = card)
                }

                if (uiState.deck?.cards.isNullOrEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No cards yet", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap the search icon to add cards", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearchSheet) {
        CardSearchSheet(
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            onSearch = { viewModel.searchCards(it) },
            onAddCard = { viewModel.addCard(it) },
            onDismiss = { showSearchSheet = false },
        )
    }
}

@Composable
private fun DeckCardRow(card: MtgDeckCardDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${card.quantity}x",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(32.dp),
        )
        if (card.imageUrl != null) {
            AsyncImage(
                model = card.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp, 44.dp),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                card.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.manaCost != null) {
                Text(
                    card.manaCost,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardSearchSheet(
    searchResults: List<ScryfallCardDto>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onAddCard: (ScryfallCardDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; if (it.length >= 2) onSearch(it) },
                label = { Text("Search cards") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(searchResults, key = { it.id }) { card ->
                    ListItem(
                        headlineContent = { Text(card.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                listOfNotNull(card.typeLine, card.manaCost).joinToString(" - "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = {
                            if (card.imageUris?.small != null) {
                                AsyncImage(
                                    model = card.imageUris.small,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp, 56.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onAddCard(card) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
