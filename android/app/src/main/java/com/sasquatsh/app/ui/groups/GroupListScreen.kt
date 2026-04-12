package com.sasquatsh.app.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import com.sasquatsh.app.ui.components.GroupCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onNavigateToGroup: (slug: String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create Group",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
            ) {
                Tab(
                    selected = uiState.activeTab == GroupTab.DISCOVER,
                    onClick = { viewModel.setActiveTab(GroupTab.DISCOVER) },
                    text = { Text("Discover") },
                )
                Tab(
                    selected = uiState.activeTab == GroupTab.MY_GROUPS,
                    onClick = { viewModel.setActiveTab(GroupTab.MY_GROUPS) },
                    text = { Text("My Groups") },
                )
            }

            // Content
            val groups = when (uiState.activeTab) {
                GroupTab.DISCOVER -> uiState.publicGroups
                GroupTab.MY_GROUPS -> uiState.myGroups
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadGroups() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading && groups.isEmpty() -> {
                        LoadingState()
                    }
                    !uiState.isLoading && groups.isEmpty() -> {
                        EmptyState(
                            isMyGroups = uiState.activeTab == GroupTab.MY_GROUPS,
                            onCreateGroup = onNavigateToCreateGroup,
                        )
                    }
                    else -> {
                        GroupList(
                            groups = groups,
                            onGroupClick = { group ->
                                group.slug?.let { onNavigateToGroup(it) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: List<GroupSummaryDto>,
    onGroupClick: (GroupSummaryDto) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(groups, key = { it.id }) { group ->
            GroupCard(
                group = group,
                onClick = { onGroupClick(group) },
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    isMyGroups: Boolean,
    onCreateGroup: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.GroupOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isMyGroups) "You haven't joined any groups yet" else "No groups found",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isMyGroups) {
                    "Discover groups or create your own to get started"
                } else {
                    "Be the first to create a group in your area"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCreateGroup) {
                Text("Create a Group")
            }
        }
    }
}
