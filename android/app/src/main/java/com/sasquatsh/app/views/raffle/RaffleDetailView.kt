package com.sasquatsh.app.views.raffle

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.Raffle
import com.sasquatsh.app.models.RaffleEntryType
import com.sasquatsh.app.viewmodels.RaffleViewModel
import com.sasquatsh.app.views.shared.EmptyStateView
import com.sasquatsh.app.views.shared.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaffleDetailView(
    viewModel: RaffleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadActiveRaffle()
    }

    LaunchedEffect(uiState.mailInSuccess) {
        if (uiState.mailInSuccess) {
            snackbarHostState.showSnackbar("Your mail-in entry has been submitted.")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Monthly Raffle") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadActiveRaffle() },
            modifier = Modifier.padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.raffle == null -> LoadingView()
                uiState.raffle != null -> {
                    val raffle = uiState.raffle!!
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item { PrizeSection(raffle) }
                        item { StatsSection(raffle) }
                        item {
                            EntriesSection(
                                raffle = raffle,
                                entriesByType = uiState.entriesByType
                            )
                        }
                        item { HowToEarnSection() }
                        if (raffle.mailInInstructions != null && raffle.isActive) {
                            item {
                                MailInSection(
                                    raffle = raffle,
                                    name = uiState.mailInName,
                                    address = uiState.mailInAddress,
                                    isSubmitting = uiState.isSubmittingMailIn,
                                    error = uiState.error,
                                    onNameChange = { viewModel.updateMailInName(it) },
                                    onAddressChange = { viewModel.updateMailInAddress(it) },
                                    onSubmit = { viewModel.submitMailInEntry() }
                                )
                            }
                        }
                        if (raffle.isEnded && raffle.hasWinner) {
                            item { WinnerSection(raffle) }
                        }
                        if (raffle.termsConditions != null) {
                            item { TermsSection(raffle) }
                        }
                    }
                }
                else -> {
                    EmptyStateView(
                        icon = Icons.Default.EmojiEvents,
                        title = "No Active Raffle",
                        message = "Check back soon for the next raffle!"
                    )
                }
            }
        }
    }
}

private fun resolveImageUrl(urlStr: String?): String? {
    if (urlStr.isNullOrEmpty()) return null
    return if (urlStr.startsWith("http")) urlStr
    else "https://sasquatsh.com$urlStr"
}

@Composable
private fun PrizeSection(raffle: Raffle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val imageUrl = resolveImageUrl(raffle.prizeImageUrl)
                ?: resolveImageUrl(raffle.bannerImageUrl)
            imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = raffle.prizeName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                raffle.prizeName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            raffle.prizeDescription?.let { desc ->
                if (desc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                raffle.prizeValueFormatted?.let { value ->
                    Text(
                        value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(raffle: Raffle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = "${raffle.stats?.displayTotalEntries ?: 0}",
                label = "Total Entries",
                icon = Icons.Default.ConfirmationNumber
            )
            StatItem(
                value = "${raffle.stats?.displayParticipants ?: 0}",
                label = "Participants",
                icon = Icons.Default.Group
            )
            StatItem(
                value = "${raffle.userTotalEntries ?: 0}",
                label = "Your Entries",
                icon = Icons.Default.Star
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EntriesSection(
    raffle: Raffle,
    entriesByType: List<Pair<RaffleEntryType, Int>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Your Entries",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (entriesByType.isEmpty()) {
                Text(
                    "No entries yet. Host or attend events to earn entries!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entriesByType.forEach { (type, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            type.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$count",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToEarnSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "How to Earn Entries",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            RaffleEntryType.values().forEach { type ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            type.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MailInSection(
    raffle: Raffle,
    name: String,
    address: String,
    isSubmitting: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mail-In Entry",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle"
                    )
                }
            }

            raffle.mailInInstructions?.let { instructions ->
                Text(
                    instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("Mailing Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting && name.isNotEmpty() && address.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSubmitting) "Submitting..." else "Submit Entry")
                }
            }
        }
    }
}

@Composable
private fun WinnerSection(raffle: Raffle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Winner!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            raffle.winner?.let { winner ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    winner.displayName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun TermsSection(raffle: Raffle) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Terms & Conditions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                raffle.termsConditions?.let { terms ->
                    Text(
                        terms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
