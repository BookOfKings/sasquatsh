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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.BggCollectionGame
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.viewmodels.CollectionViewModel
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.PoweredByBggLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionView(
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBarcodeScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCollection()
    }

    // Barcode scanner as fullscreen dialog
    if (showBarcodeScanner) {
        BarcodeScannerView(
            gameUpcService = viewModel.gameUpcService,
            bggService = viewModel.bggService,
            initialOwnedBggIds = uiState.ownedBggIds,
            onGameFound = { input ->
                viewModel.addFromInput(input)
            },
            onDismiss = { showBarcodeScanner = false }
        )
        return
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
            // Tab picker + scan button (matching iOS layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.updateActiveTab(0) },
                        text = { Text("Games", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    )
                    Tab(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.updateActiveTab(1) },
                        text = { Text("Top 50", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    )
                    Tab(
                        selected = uiState.activeTab == 2,
                        onClick = { viewModel.updateActiveTab(2) },
                        text = { Text("Search", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    )
                    Tab(
                        selected = uiState.activeTab == 3,
                        onClick = { viewModel.updateActiveTab(3) },
                        text = { Text("Import", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    )
                }

                Button(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan barcode",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            when (uiState.activeTab) {
                0 -> MyGamesTab(uiState, viewModel)
                1 -> TopGamesTab(uiState, viewModel)
                2 -> SearchTab(uiState, viewModel)
                3 -> ImportBggTab(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun MyGamesTab(
    uiState: com.sasquatsh.app.viewmodels.CollectionUiState,
    viewModel: CollectionViewModel
) {
    var gameToRemove by remember { mutableStateOf<CollectionGame?>(null) }

    // Confirm removal dialog
    gameToRemove?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToRemove = null },
            title = { Text("Remove Game") },
            text = { Text("Remove \"${game.gameName}\" from your collection?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleGame(game.bggId ?: 0, game, true)
                    gameToRemove = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                            onToggle = { gameToRemove = game }
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
    var gameToRemove by remember { mutableStateOf<CollectionGame?>(null) }

    gameToRemove?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToRemove = null },
            title = { Text("Remove Game") },
            text = { Text("Remove \"${game.gameName}\" from your collection?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleGame(game.bggId ?: 0, game, true)
                    gameToRemove = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { gameToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.topGames.isEmpty() && uiState.isLoading) {
        LoadingView()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.topGames, key = { it.id }) { game ->
                val bggId = game.bggId ?: 0
                val isOwned = uiState.ownedBggIds.contains(bggId)
                GameRow(
                    game = game,
                    isOwned = isOwned,
                    isPending = uiState.pendingAdds.contains(bggId) || uiState.pendingRemoves.contains(bggId),
                    showRank = true,
                    onToggle = {
                        if (isOwned) {
                            gameToRemove = game
                        } else {
                            viewModel.toggleGame(bggId, game, false)
                        }
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
        PoweredByBggLogo(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            height = 28.dp
        )
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
private fun ImportBggTab(
    uiState: com.sasquatsh.app.viewmodels.CollectionUiState,
    viewModel: CollectionViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.prefillBggUsername()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        // BGG logo + description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PoweredByBggLogo(
                modifier = Modifier.padding(horizontal = 16.dp),
                height = 36.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Import your BoardGameGeek collection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username input
        OutlinedTextField(
            value = uiState.bggUsername,
            onValueChange = { viewModel.updateBggUsername(it) },
            placeholder = { Text("BGG Username") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            trailingIcon = {
                if (uiState.isFetchingBgg) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        // Hint about profile BGG username
        if (uiState.bggUsername.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Set your BGG username in your Profile to auto-fill this field.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fetch button
        Button(
            onClick = { viewModel.fetchBggCollection() },
            enabled = uiState.bggUsername.trim().isNotEmpty() && !uiState.isFetchingBgg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("Fetch Collection")
        }

        // Error message
        uiState.bggError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Import result
        uiState.bggImportResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                result,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Games list + sync buttons
        if (uiState.bggGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "${uiState.bggGames.size} games found on BGG",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sync buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.importAddNewOnly() },
                    enabled = !uiState.isImporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add New Only", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = { viewModel.importFullSync() },
                    enabled = !uiState.isImporting,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Full Sync", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Importing indicator
            if (uiState.isImporting) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Importing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description of sync modes
            Text(
                "Add New Only \u2014 adds games from BGG not already in your collection\n" +
                        "Full Sync \u2014 makes your collection match BGG exactly (removes games not on BGG)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preview list (show first 20)
            val previewGames = uiState.bggGames.take(20)
            previewGames.forEach { game ->
                BggCollectionGameRow(
                    game = game,
                    isOwned = uiState.ownedBggIds.contains(game.bggId)
                )
            }

            if (uiState.bggGames.size > 20) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "...and ${uiState.bggGames.size - 20} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BggCollectionGameRow(
    game: BggCollectionGame,
    isOwned: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        game.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = game.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } ?: Box(modifier = Modifier.size(40.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                game.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            game.yearPublished?.let {
                Text(
                    "$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isOwned) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Already owned",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
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
