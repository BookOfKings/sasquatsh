package com.sasquatsh.app.views.collections

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.UpcBggInfo
import com.sasquatsh.app.models.UpcLookupResult
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.services.GameUpcService
import com.sasquatsh.app.views.shared.LoadingView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Barcode scanner sheet for looking up game UPCs.
 * Uses manual UPC entry (no camera dependency required).
 * Supports verified matches, suggestions, and manual BGG search fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerView(
    gameUpcService: GameUpcService,
    bggService: BggService,
    initialOwnedBggIds: Set<Int> = emptySet(),
    onGameFound: (AddCollectionGameInput) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var manualCode by remember { mutableStateOf("") }
    var isLooking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpcLookupResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showManualBggSearch by remember { mutableStateOf(false) }
    var bggSearchQuery by remember { mutableStateOf("") }
    var bggSearchResults by remember { mutableStateOf<List<BggSearchResult>>(emptyList()) }
    var isSearchingBgg by remember { mutableStateOf(false) }
    var addedBggIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val ownedBggIds = initialOwnedBggIds + addedBggIds

    val currentUpc: String = result?.upc ?: manualCode

    fun resetToScanner() {
        result = null
        error = null
        showManualBggSearch = false
        bggSearchQuery = ""
        bggSearchResults = emptyList()
        manualCode = ""
    }

    fun lookupBarcode(code: String) {
        val clean = code.replace(Regex("[^0-9]"), "")
        if (clean.length < 8) {
            error = "Barcode must be at least 8 digits"
            return
        }
        isLooking = true
        error = null
        showManualBggSearch = false
        scope.launch {
            try {
                result = gameUpcService.lookupUpc(clean)
            } catch (e: Exception) {
                error = "Lookup failed: ${e.localizedMessage}"
            }
            isLooking = false
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

    fun selectFromBggSearch(searchResult: BggSearchResult) {
        val input = AddCollectionGameInput(
            bggId = searchResult.bggId,
            name = searchResult.name,
            thumbnailUrl = searchResult.thumbnailUrl,
            yearPublished = searchResult.yearPublished
        )
        onGameFound(input)
        addedBggIds = addedBggIds + searchResult.bggId
        // Vote match in background
        scope.launch {
            try { gameUpcService.voteMatch(currentUpc, searchResult.bggId) } catch (_: Exception) {}
        }
    }

    fun addToCollection(match: UpcBggInfo, upc: String) {
        val input = AddCollectionGameInput(
            bggId = match.id,
            name = match.name,
            thumbnailUrl = match.thumbnailUrl,
            imageUrl = match.imageUrl,
            yearPublished = match.published?.toIntOrNull()
        )
        onGameFound(input)
        addedBggIds = addedBggIds + match.id
        // Vote to confirm in background
        scope.launch {
            try { gameUpcService.voteMatch(upc, match.id) } catch (_: Exception) {}
        }
    }

    // Debounced BGG search
    LaunchedEffect(bggSearchQuery) {
        if (bggSearchQuery.isEmpty()) {
            bggSearchResults = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        searchBgg()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
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
            val currentResult = result
            val status = currentResult?.bggInfoStatus ?: ""
            val matches = currentResult?.bggInfo ?: emptyList()

            when {
                currentResult != null && status == "verified" && matches.isNotEmpty() -> {
                    // Use Case 1: Verified match
                    VerifiedView(
                        match = matches[0],
                        upc = currentUpc,
                        isOwned = ownedBggIds.contains(matches[0].id),
                        onAdd = { addToCollection(matches[0], currentUpc) },
                        onScanAnother = { resetToScanner() }
                    )
                }
                currentResult != null && matches.isNotEmpty() -> {
                    // Use Case 2: Suggestions
                    SuggestionsView(
                        matches = matches,
                        upc = currentUpc,
                        ownedBggIds = ownedBggIds,
                        onAdd = { match -> addToCollection(match, currentUpc) },
                        onSearchBgg = { showManualBggSearch = true },
                        onScanAnother = { resetToScanner() }
                    )
                }
                currentResult != null && showManualBggSearch -> {
                    // Use Case 3: Manual BGG search
                    ManualBggSearchView(
                        bggSearchQuery = bggSearchQuery,
                        onQueryChange = { bggSearchQuery = it },
                        bggSearchResults = bggSearchResults,
                        isSearching = isSearchingBgg,
                        ownedBggIds = ownedBggIds,
                        onSelect = { selectFromBggSearch(it) },
                        onScanAnother = { resetToScanner() }
                    )
                }
                currentResult != null -> {
                    // Use Case 3: No data prompt
                    NoDataView(
                        onSearchBgg = { showManualBggSearch = true },
                        onScanAnother = { resetToScanner() }
                    )
                }
                isLooking -> {
                    LoadingView(message = "Looking up barcode...")
                }
                else -> {
                    // Manual entry view
                    ManualEntryView(
                        manualCode = manualCode,
                        onCodeChange = { manualCode = it },
                        onLookup = { lookupBarcode(manualCode) }
                    )
                }
            }

            // Error display
            error?.let { errMsg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        error = null
                        result = null
                        manualCode = ""
                    }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

// -- Manual Entry --

@Composable
private fun ManualEntryView(
    manualCode: String,
    onCodeChange: (String) -> Unit,
    onLookup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Enter UPC/EAN barcode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualCode,
                onValueChange = onCodeChange,
                placeholder = { Text("e.g. 681706711003") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { onLookup() }),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLookup,
                enabled = manualCode.length >= 8
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }
    }
}

// -- Verified Match View --

@Composable
private fun VerifiedView(
    match: UpcBggInfo,
    upc: String,
    isOwned: Boolean,
    onAdd: () -> Unit,
    onScanAnother: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScanAnotherHeader(onScanAnother)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    "Verified Match!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                GameCard(
                    match = match,
                    isOwned = isOwned,
                    onAdd = onAdd
                )
            }
        }
    }
}

// -- Suggestions View --

@Composable
private fun SuggestionsView(
    matches: List<UpcBggInfo>,
    upc: String,
    ownedBggIds: Set<Int>,
    onAdd: (UpcBggInfo) -> Unit,
    onSearchBgg: () -> Unit,
    onScanAnother: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Which game is this?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            ScanAnotherButton(onScanAnother)
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Select the correct match to help improve results for everyone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(matches, key = { it.id }) { match ->
                GameCard(
                    match = match,
                    isOwned = ownedBggIds.contains(match.id),
                    onAdd = { onAdd(match) }
                )
            }
            item {
                OutlinedButton(
                    onClick = onSearchBgg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("None of these -- search BGG")
                }
            }
        }
    }
}

// -- No Data View --

@Composable
private fun NoDataView(
    onSearchBgg: () -> Unit,
    onScanAnother: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScanAnotherHeader(onScanAnother)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.QuestionMark,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No match found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This barcode isn't in the database yet.\nSearch BGG to find the game and help others!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSearchBgg) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search BoardGameGeek")
            }
        }
    }
}

// -- Manual BGG Search View --

@Composable
private fun ManualBggSearchView(
    bggSearchQuery: String,
    onQueryChange: (String) -> Unit,
    bggSearchResults: List<BggSearchResult>,
    isSearching: Boolean,
    ownedBggIds: Set<Int>,
    onSelect: (BggSearchResult) -> Unit,
    onScanAnother: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Find the game on BGG",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            ScanAnotherButton(onScanAnother)
        }

        OutlinedTextField(
            value = bggSearchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by game name...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (isSearching) {
                    D20SpinnerView(size = 20.dp, modifier = Modifier.size(20.dp))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bggSearchResults, key = { it.bggId }) { searchResult ->
                val isOwned = ownedBggIds.contains(searchResult.bggId)
                BggSearchResultRow(
                    result = searchResult,
                    isOwned = isOwned,
                    onSelect = { onSelect(searchResult) }
                )
            }
        }
    }
}

// -- Reusable Components --

@Composable
private fun ScanAnotherHeader(onScanAnother: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        ScanAnotherButton(onScanAnother)
    }
}

@Composable
private fun ScanAnotherButton(onScanAnother: () -> Unit) {
    FilledTonalButton(onClick = onScanAnother) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Scan Another", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GameCard(
    match: UpcBggInfo,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        match.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = match.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                match.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            match.published?.let { year ->
                Text(
                    year,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            match.confidence?.let { conf ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (conf > 0.7) Icons.Default.Verified else Icons.Default.QuestionMark,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (conf > 0.7) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${(conf * 100).toInt()}% match",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conf > 0.7) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary
                    )
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
                Text(
                    "Owned",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Button(
                onClick = onAdd,
                shape = CircleShape
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun BggSearchResultRow(
    result: BggSearchResult,
    isOwned: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
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
            Spacer(modifier = Modifier.width(12.dp))
        }

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
            Button(
                onClick = onSelect,
                shape = CircleShape
            ) {
                Text("This one")
            }
        }
    }
}
