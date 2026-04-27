package com.sasquatsh.app.views.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.services.CollectionsService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// -- Search source enum --

sealed class GameSearchSource {
    data object Bgg : GameSearchSource()
    data object MyCollection : GameSearchSource()
    data class HostCollection(val userId: String, val hostName: String) : GameSearchSource()
}

// -- Source picker --

@Composable
fun GameSearchSourcePicker(
    hostUserId: String? = null,
    hostName: String? = null,
    onSelect: (GameSearchSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Suggest a Game",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Where do you want to search?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SourceButton(
                icon = Icons.Default.Person,
                title = "My Collection",
                subtitle = "Pick from games you own",
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { onSelect(GameSearchSource.MyCollection) }
            )

            if (hostUserId != null && hostName != null) {
                SourceButton(
                    icon = Icons.Default.Person,
                    title = "$hostName's Collection",
                    subtitle = "Pick from the host's games",
                    iconColor = Color(0xFF9C27B0),
                    onClick = { onSelect(GameSearchSource.HostCollection(hostUserId, hostName)) }
                )
            }

            // BGG search button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(GameSearchSource.Bgg) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BGG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search BoardGameGeek",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Search the entire BGG database",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

// -- Multi-select Game Suggest Sheet --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSuggestSheet(
    bggService: BggService,
    collectionsService: CollectionsService,
    hostUserId: String? = null,
    hostName: String? = null,
    alreadySuggestedBggIds: Set<Int> = emptySet(),
    onSelect: (List<BggSearchResult>) -> Unit,
    onDismiss: () -> Unit
) {
    var source by remember { mutableStateOf<GameSearchSource?>(null) }
    val selected = remember { mutableStateMapOf<Int, BggSearchResult>() }

    val navigationTitle = when (val s = source) {
        null -> "Suggest Games"
        is GameSearchSource.Bgg -> "BoardGameGeek"
        is GameSearchSource.MyCollection -> "My Collection"
        is GameSearchSource.HostCollection -> "${s.hostName}'s Games"
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(navigationTitle) },
                navigationIcon = {
                    if (source != null) {
                        TextButton(onClick = { source = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sources", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                actions = {
                    if (source == null) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    if (selected.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${selected.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = source) {
                null -> GameSearchSourcePicker(
                    hostUserId = hostUserId,
                    hostName = hostName,
                    onSelect = { source = it }
                )
                is GameSearchSource.Bgg -> BGGSearchView(
                    bggService = bggService,
                    selected = selected,
                    disabledBggIds = alreadySuggestedBggIds,
                    onToggle = { game ->
                        if (selected.containsKey(game.bggId)) {
                            selected.remove(game.bggId)
                        } else {
                            selected[game.bggId] = game
                        }
                    },
                    bottomPadding = if (selected.isNotEmpty()) 72.dp else 0.dp
                )
                is GameSearchSource.MyCollection -> CollectionPickerView(
                    collectionsService = collectionsService,
                    userId = null,
                    selected = selected,
                    disabledBggIds = alreadySuggestedBggIds,
                    onToggle = { game ->
                        if (selected.containsKey(game.bggId)) {
                            selected.remove(game.bggId)
                        } else {
                            selected[game.bggId] = game
                        }
                    },
                    bottomPadding = if (selected.isNotEmpty()) 72.dp else 0.dp
                )
                is GameSearchSource.HostCollection -> CollectionPickerView(
                    collectionsService = collectionsService,
                    userId = s.userId,
                    selected = selected,
                    disabledBggIds = alreadySuggestedBggIds,
                    onToggle = { game ->
                        if (selected.containsKey(game.bggId)) {
                            selected.remove(game.bggId)
                        } else {
                            selected[game.bggId] = game
                        }
                    },
                    bottomPadding = if (selected.isNotEmpty()) 72.dp else 0.dp
                )
            }

            // Floating add button
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Button(
                    onClick = {
                        onSelect(selected.values.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "Add ${selected.size} Game${if (selected.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// -- BGG Search View --

@OptIn(FlowPreview::class)
@Composable
fun BGGSearchView(
    bggService: BggService,
    selected: Map<Int, BggSearchResult>,
    disabledBggIds: Set<Int> = emptySet(),
    onToggle: (BggSearchResult) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BggSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .collect { query ->
                isSearching = true
                try {
                    results = bggService.searchGames(query)
                } catch (_: Exception) {
                    results = emptyList()
                }
                isSearching = false
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // BGG logo placeholder
        Text(
            text = "BoardGameGeek",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 4.dp)
        )

        SearchBarView(
            text = searchText,
            onTextChange = {
                searchText = it
                if (it.length < 2) results = emptyList()
            },
            placeholder = "Search BoardGameGeek...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isSearching) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomPadding)
        ) {
            items(results, key = { it.bggId }) { game ->
                GameSearchRow(
                    name = game.name,
                    yearPublished = game.yearPublished,
                    thumbnailUrl = game.thumbnailUrl,
                    isSelected = selected.containsKey(game.bggId),
                    isDisabled = disabledBggIds.contains(game.bggId),
                    onClick = { onToggle(game) }
                )
            }
        }
    }
}

// -- Collection Picker View --

@Composable
fun CollectionPickerView(
    collectionsService: CollectionsService,
    userId: String?,
    selected: Map<Int, BggSearchResult>,
    disabledBggIds: Set<Int> = emptySet(),
    onToggle: (BggSearchResult) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var games by remember { mutableStateOf<List<CollectionGame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    val filteredGames = remember(games, searchText) {
        if (searchText.isEmpty()) games
        else games.filter { it.gameName.contains(searchText, ignoreCase = true) }
    }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            games = if (userId == null) {
                collectionsService.getMyCollection()
            } else {
                // getUserCollection not yet in service, call API directly
                collectionsService.getMyCollection() // fallback to own collection
            }
        } catch (e: Exception) {
            error = "Could not load collection. It may be set to private."
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (games.isNotEmpty()) {
            SearchBarView(
                text = searchText,
                onTextChange = { searchText = it },
                placeholder = "Filter collection...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingView()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            filteredGames.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchText.isEmpty()) "No games in this collection" else "No matching games",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomPadding)
                ) {
                    items(filteredGames, key = { it.id }) { game ->
                        val bggId = game.bggId ?: 0
                        val isSelected = selected.containsKey(bggId)
                        val isDisabled = disabledBggIds.contains(bggId)

                        CollectionGameRow(
                            game = game,
                            isSelected = isSelected,
                            isDisabled = isDisabled,
                            onClick = {
                                if (!isDisabled) {
                                    onToggle(
                                        BggSearchResult(
                                            bggId = bggId,
                                            name = game.gameName,
                                            yearPublished = game.yearPublished,
                                            thumbnailUrl = game.thumbnailUrl
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionGameRow(
    game: CollectionGame,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.6f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox icon
        val bggId = game.bggId ?: 0
        if (isDisabled) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Thumbnail
        if (game.thumbnailUrl != null) {
            AsyncImage(
                model = game.thumbnailUrl,
                contentDescription = game.gameName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
            Text(
                text = game.gameName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDisabled) {
                Text(
                    text = "Already suggested",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.minPlayers != null && game.maxPlayers != null) {
                        Text(
                            text = "${game.minPlayers}-${game.maxPlayers} players",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (game.playingTime != null && game.playingTime > 0) {
                        Text(
                            text = "${game.playingTime} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (game.yearPublished != null) {
                        Text(
                            text = "(${game.yearPublished})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -- Shared game row composable --

@Composable
private fun GameSearchRow(
    name: String,
    yearPublished: Int?,
    thumbnailUrl: String?,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (isDisabled) 0.6f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDisabled) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDisabled) {
                Text(
                    text = "Already suggested",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else if (yearPublished != null) {
                Text(
                    text = "($yearPublished)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -- Legacy single-select BGGGameSearchSheet --

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun BGGGameSearchSheet(
    bggService: BggService,
    onSelect: (BggSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BggSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .collect { query ->
                isSearching = true
                try {
                    results = bggService.searchGames(query)
                } catch (_: Exception) {
                    results = emptyList()
                }
                isSearching = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Games") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // BGG logo placeholder
            Text(
                text = "BoardGameGeek",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp)
            )

            SearchBarView(
                text = searchText,
                onTextChange = {
                    searchText = it
                    if (it.length < 2) results = emptyList()
                },
                placeholder = "Search BoardGameGeek...",
                modifier = Modifier.padding(16.dp)
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.bggId }) { game ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(game)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (game.thumbnailUrl != null) {
                            AsyncImage(
                                model = game.thumbnailUrl,
                                contentDescription = game.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (game.yearPublished != null) {
                                Text(
                                    text = "(${game.yearPublished})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

