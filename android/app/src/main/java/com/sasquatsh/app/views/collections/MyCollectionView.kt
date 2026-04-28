package com.sasquatsh.app.views.collections

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.viewmodels.CollectionViewModel
import com.sasquatsh.app.views.shared.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionView(
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCollection()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Collection") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab picker
            TabRow(selectedTabIndex = uiState.activeTab) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.updateActiveTab(0) },
                    text = { Text("My Games (${uiState.myGames.size})") }
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.updateActiveTab(1) },
                    text = { Text("Top 50") }
                )
                Tab(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.updateActiveTab(2) },
                    text = { Text("Search") }
                )
            }

            when (uiState.activeTab) {
                0 -> MyGamesTab(uiState, viewModel)
                1 -> TopGamesTab(uiState, viewModel)
                2 -> SearchTab(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun MyGamesTab(
    uiState: com.sasquatsh.app.viewmodels.CollectionUiState,
    viewModel: CollectionViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter
        OutlinedTextField(
            value = uiState.filterText,
            onValueChange = { viewModel.updateFilterText(it) },
            placeholder = { Text("Filter games...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        when {
            uiState.isLoading -> LoadingView()
            uiState.myGames.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Your collection is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Search BGG to add games",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.updateActiveTab(2) }) {
                            Text("Search Games")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredGames, key = { it.id }) { game ->
                        GameRow(
                            game = game,
                            isOwned = true,
                            isPending = uiState.pendingRemoves.contains(game.bggId ?: 0),
                            onToggle = {
                                viewModel.toggleGame(game.bggId ?: 0, game, true)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopGamesTab(
    uiState: com.sasquatsh.app.viewmodels.CollectionUiState,
    viewModel: CollectionViewModel
) {
    if (uiState.topGames.isEmpty() && uiState.isLoading) {
        LoadingView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.topGames, key = { it.id }) { game ->
                val bggId = game.bggId ?: 0
                GameRow(
                    game = game,
                    isOwned = uiState.ownedBggIds.contains(bggId),
                    isPending = uiState.pendingAdds.contains(bggId) || uiState.pendingRemoves.contains(bggId),
                    showRank = true,
                    onToggle = {
                        viewModel.toggleGame(bggId, game, uiState.ownedBggIds.contains(bggId))
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchTab(
    uiState: com.sasquatsh.app.viewmodels.CollectionUiState,
    viewModel: CollectionViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search BoardGameGeek...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.isSearching) {
                    D20SpinnerView(size = 20.dp, modifier = Modifier.size(20.dp))
                } else if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        if (uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty() && !uiState.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.searchResults, key = { it.bggId }) { result ->
                    SearchResultRow(
                        result = result,
                        isOwned = uiState.ownedBggIds.contains(result.bggId),
                        isPending = uiState.pendingAdds.contains(result.bggId),
                        onAdd = { viewModel.addFromSearch(result) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameRow(
    game: CollectionGame,
    isOwned: Boolean,
    isPending: Boolean,
    showRank: Boolean = false,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            game.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = game.gameName,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    game.gameName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    game.yearPublished?.let {
                        Text(
                            "$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (game.minPlayers != null && game.maxPlayers != null) {
                        val players = if (game.minPlayers == game.maxPlayers) "${game.minPlayers}p"
                        else "${game.minPlayers}-${game.maxPlayers}p"
                        Text(
                            players,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showRank && game.bggRank != null && game.bggRank > 0) {
                        Text(
                            "#${game.bggRank}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (isPending) {
                D20SpinnerView(size = 24.dp, modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isOwned) Icons.Default.CheckCircle
                        else Icons.Outlined.AddCircleOutline,
                        contentDescription = if (isOwned) "Remove" else "Add",
                        tint = if (isOwned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: BggSearchResult,
    isOwned: Boolean,
    isPending: Boolean,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            result.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = result.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(modifier = Modifier.size(50.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                result.yearPublished?.let {
                    Text(
                        "$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isPending) {
                D20SpinnerView(size = 24.dp, modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onAdd, enabled = !isOwned) {
                    Icon(
                        imageVector = if (isOwned) Icons.Default.CheckCircle
                        else Icons.Outlined.AddCircleOutline,
                        contentDescription = if (isOwned) "Owned" else "Add",
                        tint = if (isOwned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
