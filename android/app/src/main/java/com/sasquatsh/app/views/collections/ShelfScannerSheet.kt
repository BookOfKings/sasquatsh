package com.sasquatsh.app.views.collections

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.ShelfScanGame
import com.sasquatsh.app.models.ShelfScanQuota
import com.sasquatsh.app.models.ShelfScanResult
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.services.ShelfScanService
import com.sasquatsh.app.views.shared.LoadingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Shelf Scanner sheet that takes a photo of a game shelf,
 * sends it to the backend for AI analysis, and shows identified games.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScannerSheet(
    shelfScanService: ShelfScanService,
    bggService: BggService,
    ownedBggIds: Set<Int> = emptySet(),
    onGamesAdded: (List<AddCollectionGameInput>) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ShelfScanResult?>(null) }
    var quota by remember { mutableStateOf<ShelfScanQuota?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var addedBggIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Manual BGG search state for unmatched titles
    var searchingTitle by remember { mutableStateOf<String?>(null) }
    var bggSearchQuery by remember { mutableStateOf("") }
    var bggSearchResults by remember { mutableStateOf<List<BggSearchResult>>(emptyList()) }
    var isSearchingBgg by remember { mutableStateOf(false) }

    val allOwnedIds = ownedBggIds + addedBggIds

    // -- Image processing and scanning --

    fun resizeAndEncode(bitmap: Bitmap): String {
        val maxDim = 1024f
        val scale = if (maxOf(bitmap.width, bitmap.height) > maxDim) {
            maxDim / maxOf(bitmap.width, bitmap.height)
        } else 1f

        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else bitmap

        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun scanShelf(bitmap: Bitmap) {
        isScanning = true
        error = null
        scope.launch {
            try {
                val base64 = resizeAndEncode(bitmap)
                scanResult = shelfScanService.scanImage(base64)
                // Refresh quota
                try { quota = shelfScanService.getRemainingScans() } catch (_: Exception) {}
            } catch (e: Exception) {
                error = e.localizedMessage
            }
            isScanning = false
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { scanShelf(it) }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    scanShelf(bitmap)
                } else {
                    error = "Failed to load image"
                }
            } catch (e: Exception) {
                error = "Failed to load image: ${e.localizedMessage}"
            }
        }
    }

    fun searchBgg() {
        if (bggSearchQuery.isEmpty()) return
        isSearchingBgg = true
        scope.launch {
            try {
                bggSearchResults = bggService.searchGames(bggSearchQuery)
            } catch (e: Exception) {
                error = e.localizedMessage
            }
            isSearchingBgg = false
        }
    }

    fun addSingleGame(game: ShelfScanGame) {
        val bggId = game.bggId ?: return
        val input = AddCollectionGameInput(
            bggId = bggId,
            name = game.name ?: game.detectedTitle,
            thumbnailUrl = game.thumbnailUrl,
            minPlayers = game.minPlayers,
            maxPlayers = game.maxPlayers,
            playingTime = game.playingTime,
            yearPublished = game.yearPublished
        )
        onGamesAdded(listOf(input))
        addedBggIds = addedBggIds + bggId
    }

    fun addAllMatched(games: List<ShelfScanGame>) {
        val inputs = games.mapNotNull { game ->
            val bggId = game.bggId ?: return@mapNotNull null
            addedBggIds = addedBggIds + bggId
            AddCollectionGameInput(
                bggId = bggId,
                name = game.name ?: game.detectedTitle,
                thumbnailUrl = game.thumbnailUrl,
                minPlayers = game.minPlayers,
                maxPlayers = game.maxPlayers,
                playingTime = game.playingTime,
                yearPublished = game.yearPublished
            )
        }
        onGamesAdded(inputs)
    }

    fun addFromSearch(result: BggSearchResult) {
        val input = AddCollectionGameInput(
            bggId = result.bggId,
            name = result.name,
            thumbnailUrl = result.thumbnailUrl,
            yearPublished = result.yearPublished
        )
        onGamesAdded(listOf(input))
        addedBggIds = addedBggIds + result.bggId
        searchingTitle = null
        bggSearchResults = emptyList()
    }

    // Load quota on launch
    LaunchedEffect(Unit) {
        try { quota = shelfScanService.getRemainingScans() } catch (_: Exception) {}
    }

    // Debounced BGG search
    LaunchedEffect(bggSearchQuery) {
        if (bggSearchQuery.isEmpty()) return@LaunchedEffect
        delay(400)
        searchBgg()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shelf Scanner") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            when {
                scanResult != null -> {
                    ShelfResultsView(
                        result = scanResult!!,
                        allOwnedIds = allOwnedIds,
                        onAddSingle = { addSingleGame(it) },
                        onAddAll = { addAllMatched(it) },
                        onSearchUnmatched = { game ->
                            bggSearchQuery = game.detectedTitle
                            searchingTitle = game.detectedTitle
                        },
                        onScanAgain = {
                            scanResult = null
                            error = null
                        }
                    )
                }
                isScanning -> {
                    LoadingView(message = "Scanning shelf...")
                    Text(
                        "Reading game titles with AI vision",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                searchingTitle != null -> {
                    ShelfManualSearchView(
                        title = searchingTitle!!,
                        bggSearchQuery = bggSearchQuery,
                        onQueryChange = { bggSearchQuery = it },
                        bggSearchResults = bggSearchResults,
                        isSearching = isSearchingBgg,
                        allOwnedIds = allOwnedIds,
                        onAdd = { addFromSearch(it) },
                        onBack = {
                            searchingTitle = null
                            bggSearchResults = emptyList()
                        }
                    )
                }
                else -> {
                    ShelfCaptureView(
                        quota = quota,
                        onTakePhoto = { cameraLauncher.launch(null) },
                        onChooseFromLibrary = { galleryLauncher.launch("image/*") }
                    )
                }
            }

            error?.let { errMsg ->
                Text(
                    text = errMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// -- Capture View --

@Composable
private fun ShelfCaptureView(
    quota: ShelfScanQuota?,
    onTakePhoto: () -> Unit,
    onChooseFromLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Scan Your Game Shelf",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Take a photo of your game shelf and we'll identify the titles using AI vision",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        quota?.let { q ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (q.isUnlimited || q.remaining > 0)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    q.limitDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (q.isUnlimited || q.remaining > 0)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Take Photo")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onChooseFromLibrary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose from Library")
        }
    }
}

// -- Results View --

@Composable
private fun ShelfResultsView(
    result: ShelfScanResult,
    allOwnedIds: Set<Int>,
    onAddSingle: (ShelfScanGame) -> Unit,
    onAddAll: (List<ShelfScanGame>) -> Unit,
    onSearchUnmatched: (ShelfScanGame) -> Unit,
    onScanAgain: () -> Unit
) {
    val matchedUnowned = result.games.filter {
        it.bggId != null && !allOwnedIds.contains(it.bggId)
    }
    val unmatched = result.games.filter { it.bggId == null }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Found ${result.totalDetected ?: result.games.size} titles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${result.matched ?: 0} matched to BGG",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onScanAgain) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Again", style = MaterialTheme.typography.labelLarge)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Matched games
            items(result.games.filter { it.bggId != null }, key = { it.id }) { game ->
                ShelfMatchedGameRow(
                    game = game,
                    isOwned = allOwnedIds.contains(game.bggId!!),
                    onAdd = { onAddSingle(game) }
                )
            }

            // Unmatched titles section
            if (unmatched.isNotEmpty()) {
                item {
                    Text(
                        "Unmatched Titles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(unmatched, key = { it.id }) { game ->
                    ShelfUnmatchedGameRow(
                        game = game,
                        onSearch = { onSearchUnmatched(game) }
                    )
                }
            }
        }

        // Add all button
        if (matchedUnowned.isNotEmpty()) {
            Button(
                onClick = { onAddAll(matchedUnowned) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add All ${matchedUnowned.size} Matched Games")
            }
        }
    }
}

@Composable
private fun ShelfMatchedGameRow(
    game: ShelfScanGame,
    isOwned: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isOwned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        game.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = game.name ?: game.detectedTitle,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    game.name ?: game.detectedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                game.confidence?.let { conf ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        if (conf == "high") Icons.Default.Verified else Icons.Default.QuestionMark,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (conf == "high") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.yearPublished?.let {
                    Text("$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (game.minPlayers != null && game.maxPlayers != null) {
                    val players = if (game.minPlayers == game.maxPlayers) "${game.minPlayers}p"
                    else "${game.minPlayers}-${game.maxPlayers}p"
                    Text(players, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isOwned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Owned",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Owned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Button(onClick = onAdd, shape = CircleShape) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun ShelfUnmatchedGameRow(
    game: ShelfScanGame,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.QuestionMark,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            game.detectedTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(onClick = onSearch) {
            Text("Search", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// -- Manual Search for Unmatched --

@Composable
private fun ShelfManualSearchView(
    title: String,
    bggSearchQuery: String,
    onQueryChange: (String) -> Unit,
    bggSearchResults: List<BggSearchResult>,
    isSearching: Boolean,
    allOwnedIds: Set<Int>,
    onAdd: (BggSearchResult) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Search for: $title",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = bggSearchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search BGG...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(bggSearchResults, key = { it.bggId }) { result ->
                val isOwned = allOwnedIds.contains(result.bggId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    result.thumbnailUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = result.name,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            result.name,
                            style = MaterialTheme.typography.bodyMedium,
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

                    if (isOwned) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Owned",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Owned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(onClick = { onAdd(result) }, shape = CircleShape) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
