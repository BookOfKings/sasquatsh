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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.sasquatsh.app.models.MtgDeckCard
import com.sasquatsh.app.viewmodels.DeckBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBuilderView(
    deck: MtgDeck?,
    onDismiss: () -> Unit,
    viewModel: DeckBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var formatExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(deck) {
        if (deck != null) {
            viewModel.loadForEdit(deck)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Deck" else "New Deck")
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: show import sheet */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Import")
                    }
                    TextButton(
                        onClick = {
                            viewModel.saveDeck { onDismiss() }
                        },
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Text(if (uiState.isSaving) "Saving..." else "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Deck Settings section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Deck Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.deckName,
                            onValueChange = { viewModel.updateDeckName(it) },
                            label = { Text("Deck Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Format picker
                        ExposedDropdownMenuBox(
                            expanded = formatExpanded,
                            onExpandedChange = { formatExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = uiState.formatId?.replaceFirstChar { it.uppercase() } ?: "None",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Format") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = formatExpanded,
                                onDismissRequest = { formatExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        viewModel.updateFormatId(null)
                                        formatExpanded = false
                                    }
                                )
                                listOf(
                                    "standard", "pioneer", "modern", "legacy", "vintage",
                                    "commander", "pauper", "historic", "alchemy", "brawl"
                                ).forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            viewModel.updateFormatId(format)
                                            formatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Power level
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Power Level: ${uiState.powerLevel ?: 5}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Slider(
                                value = (uiState.powerLevel ?: 5).toFloat(),
                                onValueChange = { viewModel.updatePowerLevel(it.toInt()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Public toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Public", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = uiState.isPublic,
                                onCheckedChange = { viewModel.updateIsPublic(it) }
                            )
                        }
                    }
                }
            }

            // Stats section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Deck Stats",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBadge("Total", uiState.totalCards)
                            StatBadge("Creatures", uiState.creatureCount)
                            StatBadge("Spells", uiState.spellCount)
                            StatBadge("Lands", uiState.landCount)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Avg CMC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format("%.2f", uiState.avgCMC),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Card search section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Add Cards",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search cards...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (uiState.isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Search results
            if (uiState.searchResults.isNotEmpty()) {
                items(uiState.searchResults.take(8)) { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addCard(card)
                                viewModel.clearSearch()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            card.smallImageUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = card.name,
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    card.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                card.typeLine?.let { type ->
                                    Text(
                                        type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            card.manaCost?.let { mana ->
                                if (mana.isNotEmpty()) {
                                    Text(
                                        mana,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.AddCircle,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Deck list by type
            uiState.cardsByType.forEach { (type, cards) ->
                item {
                    Text(
                        "$type (${cards.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(cards) { deckCard ->
                    DeckCardRow(
                        deckCard = deckCard,
                        onIncrement = {
                            val idx = uiState.cards.indexOfFirst { it.id == deckCard.id }
                            if (idx >= 0) viewModel.updateQuantity(idx, deckCard.quantity + 1)
                        },
                        onDecrement = {
                            val idx = uiState.cards.indexOfFirst { it.id == deckCard.id }
                            if (idx >= 0) viewModel.updateQuantity(idx, deckCard.quantity - 1)
                        }
                    )
                }
            }

            // Sideboard
            if (uiState.sideboardCards.isNotEmpty()) {
                item {
                    Text(
                        "Sideboard (${uiState.sideboardCards.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.sideboardCards) { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            card.card?.name ?: card.scryfallId,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "x${card.quantity}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeckCardRow(
    deckCard: MtgDeckCard,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            deckCard.card?.smallImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 28.dp, height = 40.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                deckCard.card?.name ?: deckCard.scryfallId,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = "Decrease",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${deckCard.quantity}",
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Increase",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
