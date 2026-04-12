package com.sasquatsh.app.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.data.remote.dto.GameSuggestionDto
import com.sasquatsh.app.data.remote.dto.PlanningDateDto
import com.sasquatsh.app.data.remote.dto.PlanningItemDto
import com.sasquatsh.app.data.remote.dto.PlanningSessionDto
import com.sasquatsh.app.ui.chat.ChatScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningSessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanningSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.title ?: "Planning Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.session == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(onClick = { viewModel.loadSession() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.session != null -> {
                PlanningSessionContent(
                    session = uiState.session!!,
                    isActioning = uiState.isActioning,
                    error = uiState.error,
                    onVoteDates = { votes -> viewModel.voteDates(votes) },
                    onSuggestGame = { name -> viewModel.suggestGame(name) },
                    onVoteGame = { gameId -> viewModel.voteGame(gameId) },
                    onClaimItem = { itemId -> viewModel.claimItem(itemId) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanningSessionContent(
    session: PlanningSessionDto,
    isActioning: Boolean,
    error: String?,
    onVoteDates: (List<Map<String, Any>>) -> Unit,
    onSuggestGame: (String) -> Unit,
    onVoteGame: (String) -> Unit,
    onClaimItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSuggestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header info
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(session.status.replaceFirstChar { it.uppercase() }) },
                    )
                    Text(
                        text = "Deadline: ${session.responseDeadline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Proposed dates
        if (!session.dates.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Proposed Dates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            session.dates.forEach { date ->
                DateVoteCard(
                    date = date,
                    isActioning = isActioning,
                    onVote = { available ->
                        val vote = mapOf<String, Any>(
                            "dateId" to date.id,
                            "available" to available,
                        )
                        onVoteDates(listOf(vote))
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Game suggestions
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Game Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FilledTonalButton(
                onClick = { showSuggestDialog = true },
                enabled = !isActioning,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Suggest")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (session.gameSuggestions.isNullOrEmpty()) {
            Text(
                text = "No games suggested yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            session.gameSuggestions.forEach { game ->
                GameSuggestionCard(
                    game = game,
                    isActioning = isActioning,
                    onVote = { onVoteGame(game.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Items
        if (!session.items.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Items Needed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            session.items.forEach { item ->
                ItemCard(
                    item = item,
                    isActioning = isActioning,
                    onClaim = { onClaimItem(item.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Chat
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
        ) {
            ChatScreen(
                contextType = "planning",
                contextId = session.id,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showSuggestDialog) {
        SuggestGameDialog(
            onDismiss = { showSuggestDialog = false },
            onConfirm = { gameName ->
                onSuggestGame(gameName)
                showSuggestDialog = false
            },
        )
    }
}

@Composable
private fun DateVoteCard(
    date: PlanningDateDto,
    isActioning: Boolean,
    onVote: (Boolean) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = date.date,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                date.startTime?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val availableCount = date.votes?.count { it.available } ?: 0
                val totalVotes = date.votes?.size ?: 0
                if (totalVotes > 0) {
                    Text(
                        text = "$availableCount/$totalVotes available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { onVote(true) },
                    enabled = !isActioning,
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Available", modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = { onVote(false) },
                    enabled = !isActioning,
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Unavailable", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun GameSuggestionCard(
    game: GameSuggestionDto,
    isActioning: Boolean,
    onVote: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            game.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.gameName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${game.votes?.size ?: 0} votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            FilledTonalButton(
                onClick = onVote,
                enabled = !isActioning,
            ) {
                Icon(Icons.Default.ThumbUp, contentDescription = "Vote", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vote")
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: PlanningItemDto,
    isActioning: Boolean,
    onClaim: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                item.itemCategory?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.claimedByDisplayName?.let {
                    Text(
                        text = "Claimed by $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (item.claimedByUserId == null) {
                FilledTonalButton(
                    onClick = onClaim,
                    enabled = !isActioning,
                ) {
                    Text("Claim")
                }
            }
        }
    }
}

@Composable
private fun SuggestGameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var gameName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest a Game") },
        text = {
            OutlinedTextField(
                value = gameName,
                onValueChange = { gameName = it },
                label = { Text("Game name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(gameName.trim()) },
                enabled = gameName.isNotBlank(),
            ) {
                Text("Suggest")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
