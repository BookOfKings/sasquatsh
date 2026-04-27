package com.sasquatsh.app.views.groups

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.GroupType
import com.sasquatsh.app.viewmodels.GroupListViewModel
import com.sasquatsh.app.views.events.USStateDropdown
import com.sasquatsh.app.views.shared.EmptyStateView
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.views.shared.GroupCard
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.SearchBarView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListView(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadGroups()
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadGroups() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Search bar
            item {
                SearchBarView(
                    text = uiState.searchText,
                    onTextChange = { viewModel.updateSearchText(it) },
                    placeholder = "Search groups...",
                    onSearch = { viewModel.loadGroups() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Filter chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // Create Group chip
                    AssistChip(
                        onClick = onNavigateToCreate,
                        label = { Text("Create Group") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // Filters chip
                    AssistChip(
                        onClick = { showFilters = true },
                        label = { Text("Filters") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    // Clear filters chip
                    if (uiState.hasActiveFilters) {
                        AssistChip(
                            onClick = {
                                viewModel.clearFilters()
                                viewModel.loadGroups()
                            },
                            label = { Text("Clear") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Error banner
            val error = uiState.error
            if (error != null) {
                item {
                    ErrorBannerView(message = error)
                }
            }

            // Content
            if (uiState.isLoading && uiState.groups.isEmpty()) {
                item {
                    LoadingView(modifier = Modifier.fillParentMaxSize())
                }
            } else if (uiState.groups.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.Groups,
                        title = "No Groups Found",
                        message = "Create a group to start planning game nights together",
                        buttonTitle = "Create Group",
                        onAction = onNavigateToCreate,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                items(uiState.groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onNavigateToDetail(group.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilters) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState
        ) {
            FilterSheetContent(
                selectedType = uiState.selectedType,
                filterCity = uiState.filterCity,
                filterState = uiState.filterState,
                onTypeChange = { viewModel.updateSelectedType(it) },
                onCityChange = { viewModel.updateFilterCity(it) },
                onStateChange = { viewModel.updateFilterState(it) },
                onApply = {
                    scope.launch { sheetState.hide() }
                    showFilters = false
                    viewModel.loadGroups()
                },
                onCancel = {
                    scope.launch { sheetState.hide() }
                    showFilters = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetContent(
    selectedType: GroupType?,
    filterCity: String,
    filterState: String,
    onTypeChange: (GroupType?) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            Button(onClick = onApply) {
                Text("Apply")
            }
        }

        // Group Type
        Text(
            text = "Group Type",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            // "Any" option
            AssistChip(
                onClick = { onTypeChange(null) },
                label = { Text("Any") },
                colors = if (selectedType == null) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                }
            )
            GroupType.entries.forEach { type ->
                AssistChip(
                    onClick = { onTypeChange(type) },
                    label = { Text(type.displayName) },
                    colors = if (selectedType == type) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }
        }

        // Location
        Text(
            text = "Location",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = filterCity,
            onValueChange = onCityChange,
            label = { Text("City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        USStateDropdown(
            selected = filterState,
            onSelect = onStateChange
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
