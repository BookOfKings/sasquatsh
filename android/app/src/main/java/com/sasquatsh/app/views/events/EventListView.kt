package com.sasquatsh.app.views.events

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.DifficultyLevel
import com.sasquatsh.app.models.EventSummary
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.viewmodels.EventListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListView(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadUserPostalCode()
        viewModel.loadEvents()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading && uiState.events.isNotEmpty(),
        onRefresh = { viewModel.loadEvents() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Search bar
            item {
                SearchBar(
                    text = uiState.searchText,
                    onTextChange = { text ->
                        viewModel.updateSearchText(text)
                        viewModel.loadEvents()
                    },
                    placeholder = "Search games...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter chips row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Host a Game chip
                    AssistChip(
                        onClick = { onNavigateToCreate() },
                        label = { Text("Host a Game") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // Filters chip
                    FilterChip(
                        selected = false,
                        onClick = { showFilters = true },
                        label = { Text("Filters") }
                    )

                    // Active filter badges
                    uiState.selectedGameSystem?.let { system ->
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text(system.shortName) }
                        )
                    }

                    if (uiState.nearbyEnabled) {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("Nearby ${uiState.radiusMiles}mi") }
                        )
                    }

                    uiState.filterCity?.takeIf { it.isNotEmpty() }?.let { city ->
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text(city) }
                        )
                    }

                    uiState.filterState?.takeIf { it.isNotEmpty() }?.let { state ->
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text(state) }
                        )
                    }

                    // Clear filters chip
                    if (uiState.hasActiveFilters) {
                        AssistChip(
                            onClick = {
                                viewModel.clearFilters()
                                viewModel.loadEvents()
                            },
                            label = { Text("Clear") },
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Error banner
            uiState.error?.let { error ->
                item {
                    ErrorBanner(
                        message = error,
                        onDismiss = { viewModel.loadEvents() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Content
            if (uiState.isLoading && uiState.events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                }
            } else if (uiState.events.isEmpty()) {
                item {
                    EmptyStateContent(
                        title = "No Games Found",
                        message = "No game nights scheduled yet",
                        buttonTitle = "Create Game",
                        onAction = { onNavigateToCreate() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(uiState.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onNavigateToDetail(event.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilters) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState
        ) {
            EventFilterSheet(
                selectedGameSystem = uiState.selectedGameSystem,
                onGameSystemChange = { viewModel.updateGameSystem(it) },
                selectedCategory = uiState.selectedCategory,
                onCategoryChange = { viewModel.updateCategory(it) },
                selectedDifficulty = uiState.selectedDifficulty,
                onDifficultyChange = { viewModel.updateDifficulty(it) },
                filterCity = uiState.filterCity,
                onCityChange = { viewModel.updateFilterCity(it) },
                filterState = uiState.filterState,
                onStateChange = { viewModel.updateFilterState(it) },
                nearbyEnabled = uiState.nearbyEnabled,
                onNearbyChange = { viewModel.updateNearbyEnabled(it) },
                radiusMiles = uiState.radiusMiles,
                onRadiusChange = { viewModel.updateRadiusMiles(it) },
                userPostalCode = uiState.userPostalCode,
                onApply = {
                    showFilters = false
                    viewModel.loadEvents()
                },
                onCancel = { showFilters = false }
            )
        }
    }
}

// ─── Search Bar ───

@Composable
private fun SearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        trailingIcon = {
            if (text.isNotEmpty()) {
                TextButton(onClick = { onTextChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}

// ─── Error Banner ───

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

// ─── Empty State ───

@Composable
private fun EmptyStateContent(
    title: String,
    message: String,
    buttonTitle: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = onAction) {
            Text(buttonTitle)
        }
    }
}

// ─── Event Card ───

@Composable
fun EventCard(
    event: EventSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game thumbnail
            event.primaryGameThumbnail?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Game system badge + date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    event.gameSystem?.let { system ->
                        if (system != GameSystem.BOARD_GAME) {
                            Text(
                                text = system.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            append(formatEventDate(event.eventDate))
                            event.startTime?.let { append("  ${formatStartTime(it)}") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Location
                val locationParts = listOfNotNull(event.city, event.state).joinToString(", ")
                if (locationParts.isNotEmpty()) {
                    Text(
                        text = locationParts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Player count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val maxPlayers = event.maxPlayers ?: 0
                    val progress = if (maxPlayers > 0) {
                        event.confirmedCount.toFloat() / maxPlayers
                    } else 0f

                    Text(
                        text = "${event.confirmedCount}/${maxPlayers} players",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Filter Sheet ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFilterSheet(
    selectedGameSystem: GameSystem?,
    onGameSystemChange: (GameSystem?) -> Unit,
    selectedCategory: GameCategory?,
    onCategoryChange: (GameCategory?) -> Unit,
    selectedDifficulty: DifficultyLevel?,
    onDifficultyChange: (DifficultyLevel?) -> Unit,
    filterCity: String?,
    onCityChange: (String?) -> Unit,
    filterState: String?,
    onStateChange: (String?) -> Unit,
    nearbyEnabled: Boolean,
    onNearbyChange: (Boolean) -> Unit,
    radiusMiles: Int,
    onRadiusChange: (Int) -> Unit,
    userPostalCode: String?,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onApply) {
                Text("Apply")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game System
        Text("Game System", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        GameSystemDropdown(
            selected = selectedGameSystem,
            onSelect = onGameSystemChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category (only for board games)
        if (selectedGameSystem == null || selectedGameSystem == GameSystem.BOARD_GAME) {
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryDropdown(
                selected = selectedCategory,
                onSelect = onCategoryChange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Difficulty
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        DifficultyDropdown(
            selected = selectedDifficulty,
            onSelect = onDifficultyChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Location
        Text("Location", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = filterCity ?: "",
            onValueChange = { onCityChange(it.ifEmpty { null }) },
            label = { Text("City") },
            singleLine = true,
            enabled = !nearbyEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        USStateDropdown(
            selected = filterState ?: "",
            onSelect = { onStateChange(it.ifEmpty { null }) },
            enabled = !nearbyEnabled
        )

        // Nearby
        if (userPostalCode != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nearby", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Search Nearby", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = nearbyEnabled, onCheckedChange = onNearbyChange)
            }

            if (nearbyEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50, 100).forEach { radius ->
                        FilterChip(
                            selected = radiusMiles == radius,
                            onClick = { onRadiusChange(radius) },
                            label = { Text("${radius} mi") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Dropdown Helpers ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSystemDropdown(
    selected: GameSystem?,
    onSelect: (GameSystem?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "Any",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            GameSystem.entries.forEach { system ->
                DropdownMenuItem(
                    text = { Text(system.displayName) },
                    onClick = {
                        onSelect(system)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: GameCategory?,
    onSelect: (GameCategory?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "Any",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            GameCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.displayName) },
                    onClick = {
                        onSelect(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyDropdown(
    selected: DifficultyLevel?,
    onSelect: (DifficultyLevel?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "Any",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            DifficultyLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.displayName) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun USStateDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    val states = listOf(
        "" to "Any",
        "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas",
        "CA" to "California", "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware",
        "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii", "ID" to "Idaho",
        "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa", "KS" to "Kansas",
        "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine", "MD" to "Maryland",
        "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota", "MS" to "Mississippi",
        "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada",
        "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico", "NY" to "New York",
        "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio", "OK" to "Oklahoma",
        "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island", "SC" to "South Carolina",
        "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas", "UT" to "Utah",
        "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington", "WV" to "West Virginia",
        "WI" to "Wisconsin", "WY" to "Wyoming", "DC" to "D.C."
    )

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = states.find { it.first == selected }?.second ?: "Any",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("State") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            states.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Date formatting helper ───

fun formatEventDate(dateString: String): String {
    return try {
        // Try date-only format first (yyyy-MM-dd)
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
        // Take only the first 10 chars to handle ISO timestamps like "2026-05-08T19:00:00"
        val dateStr = if (dateString.length > 10) dateString.substring(0, 10) else dateString
        val date = inputFormat.parse(dateStr) ?: return dateString
        outputFormat.format(date)
    } catch (_: Exception) {
        dateString
    }
}

fun formatStartTime(timeString: String?): String {
    if (timeString == null) return "TBD"
    return try {
        val inputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
        val time = inputFormat.parse(timeString) ?: return timeString
        outputFormat.format(time)
    } catch (_: Exception) {
        timeString
    }
}
