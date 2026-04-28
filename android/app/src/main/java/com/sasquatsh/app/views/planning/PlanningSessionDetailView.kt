package com.sasquatsh.app.views.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.DateAvailabilityInput
import com.sasquatsh.app.models.GameSuggestion
import com.sasquatsh.app.models.ItemCategory
import com.sasquatsh.app.models.PlanningDate
import com.sasquatsh.app.models.PlanningInvitee
import com.sasquatsh.app.models.PlanningItem
import com.sasquatsh.app.models.PlanningSession
import com.sasquatsh.app.models.PlanningStatus
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.PlanningSessionViewModel
import com.sasquatsh.app.views.events.formatEventDate
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.ErrorBannerView
import com.sasquatsh.app.views.shared.LoadingView
import com.sasquatsh.app.views.shared.UserAvatarView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanningSessionDetailView(
    sessionId: String,
    onNavigateToEvent: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlanningSessionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val currentUserId = authState.user?.id

    var currentStep by remember { mutableIntStateOf(0) }
    val selectedAvailability = remember { mutableStateMapOf<String, Boolean>() }
    var cannotAttendAny by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    // Finalize state
    var selectedDateId by remember { mutableStateOf<String?>(null) }
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    val tableGameAssignments = remember { mutableStateMapOf<Int, String>() }
    var tableCountInput by remember { mutableIntStateOf(2) }

    val session = uiState.session
    val isCreator = session?.createdByUserId == currentUserId
    val stepCount = if (isCreator) 5 else 4

    fun isInvitee(): Boolean {
        return session?.invitees?.any { it.userId == currentUserId } == true
    }

    fun hasResponded(): Boolean {
        return session?.invitees?.firstOrNull { it.userId == currentUserId }?.hasResponded == true
    }

    val respondedCount = session?.invitees?.count { it.hasResponded } ?: 0
    val totalInvitees = session?.invitees?.size ?: 0

    fun prefillAvailability() {
        session?.dates?.forEach { date ->
            date.votes?.firstOrNull { it.userId == currentUserId }?.let { vote ->
                selectedAvailability[date.id] = vote.isAvailable
            }
        }
        session?.invitees?.firstOrNull { it.userId == currentUserId }?.let {
            cannotAttendAny = it.cannotAttendAny
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    LaunchedEffect(session) {
        if (session != null) {
            prefillAvailability()
            if (hasResponded()) currentStep = 1
            session.tableCount?.let { if (it >= 2) tableCountInput = it }
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadSession(sessionId) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading && session == null) {
            LoadingView()
        } else if (session == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                uiState.error?.let { ErrorBannerView(message = it) }
                    ?: Text(
                        "Session not found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                TextButton(onClick = { viewModel.loadSession(sessionId) }) {
                    Text("Retry")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                HeaderCard(session = session, respondedCount = respondedCount, totalInvitees = totalInvitees, hasResponded = hasResponded())

                // Error/Action messages
                uiState.error?.let { ErrorBannerView(message = it) }
                uiState.actionMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Status banners
                when (session.status) {
                    PlanningStatus.FINALIZED -> FinalizedBanner(session, onNavigateToEvent)
                    PlanningStatus.CANCELLED -> CancelledBanner()
                    PlanningStatus.OPEN -> {}
                }

                // Step navigation (when open)
                if (session.status == PlanningStatus.OPEN) {
                    StepperView(
                        currentStep = currentStep,
                        stepCount = stepCount,
                        isCreator = isCreator,
                        onStepClick = { currentStep = it }
                    )
                }

                // Step content
                when (currentStep) {
                    0 -> DatesStep(
                        session = session,
                        selectedAvailability = selectedAvailability,
                        cannotAttendAny = cannotAttendAny,
                        onCannotAttendChange = { cannotAttendAny = it },
                        onAvailabilityChange = { dateId, avail -> selectedAvailability[dateId] = avail },
                        isInvitee = isInvitee(),
                        hasResponded = hasResponded(),
                        onSubmitResponse = {
                            val availability = (session.dates ?: emptyList()).map { date ->
                                DateAvailabilityInput(
                                    dateId = date.id,
                                    isAvailable = if (cannotAttendAny) false else (selectedAvailability[date.id] ?: false)
                                )
                            }
                            viewModel.submitResponse(cannotAttendAny, availability)
                            currentStep = 1
                        },
                        onNext = { currentStep = 1 }
                    )
                    1 -> GamesStep(
                        session = session,
                        currentUserId = currentUserId,
                        isCreator = isCreator,
                        onVote = { viewModel.voteForGame(it) },
                        onUnvote = { viewModel.unvoteGame(it) },
                        onRemoveSuggestion = { viewModel.removeSuggestion(it) },
                        onSuggestGame = { /* open game suggest sheet */ },
                        onBack = { currentStep = 0 },
                        onNext = { currentStep = 2 }
                    )
                    2 -> ItemsStep(
                        session = session,
                        currentUserId = currentUserId,
                        isCreator = isCreator,
                        onAddItem = { showAddItem = true },
                        onClaimItem = { viewModel.claimItem(it) },
                        onUnclaimItem = { viewModel.unclaimItem(it) },
                        onRemoveItem = { viewModel.removeItem(it) },
                        onBack = { currentStep = 1 },
                        onNext = { currentStep = 3 }
                    )
                    3 -> PeopleStep(
                        session = session,
                        isCreator = isCreator,
                        stepCount = stepCount,
                        onBack = { currentStep = 2 },
                        onNext = { if (isCreator) currentStep = 4 }
                    )
                    4 -> if (isCreator) {
                        FinalizeStep(
                            session = session,
                            selectedDateId = selectedDateId,
                            selectedGameId = selectedGameId,
                            tableGameAssignments = tableGameAssignments,
                            tableCountInput = tableCountInput,
                            onSelectDate = { selectedDateId = it },
                            onSelectGame = { selectedGameId = it },
                            onAssignTable = { table, gameId -> tableGameAssignments[table] = gameId },
                            onRemoveTableAssignment = { tableGameAssignments.remove(it) },
                            onUpdateTableCount = { tableCountInput = it },
                            onToggleMultiTable = { enabled ->
                                if (enabled) {
                                    viewModel.updateSettings(tableCountInput)
                                } else {
                                    viewModel.updateSettings(null)
                                }
                            },
                            onFinalize = {
                                val isMultiTable = (session.tableCount ?: 0) >= 2
                                if (isMultiTable && tableGameAssignments.isNotEmpty()) {
                                    viewModel.scheduleSessions(tableGameAssignments.toMap())
                                }
                                viewModel.finalize(selectedDateId, if (isMultiTable) null else selectedGameId) { eventId ->
                                    onNavigateToEvent(eventId)
                                }
                            },
                            onBack = { currentStep = 3 }
                        )
                    }
                }

                // Chat section (collapsible)
                PlanningChatSection(showChat = showChat, onToggle = { showChat = !showChat })

                // Creator bottom actions
                if (session.status == PlanningStatus.OPEN && isCreator) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { /* invite more */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite More")
                            }
                        }

                        TextButton(
                            onClick = { showCancelConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Session", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItem) {
        AddItemDialog(
            onDismiss = { showAddItem = false },
            onAdd = { name, category, quantity ->
                viewModel.addItem(name, category, quantity)
                showAddItem = false
            }
        )
    }

    // Cancel confirm dialog
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Session") },
            text = { Text("Are you sure you want to cancel this planning session?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancel()
                    showCancelConfirm = false
                }) {
                    Text("Cancel Session", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Keep Open")
                }
            }
        )
    }
}

// ---- Header Card ----

@Composable
private fun HeaderCard(
    session: PlanningSession,
    respondedCount: Int,
    totalInvitees: Int,
    hasResponded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = session.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            session.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeView(
                    text = session.status.displayName,
                    color = when (session.status) {
                        PlanningStatus.OPEN -> MaterialTheme.colorScheme.tertiaryContainer
                        PlanningStatus.FINALIZED -> Color(0xFF2196F3).copy(alpha = 0.15f)
                        PlanningStatus.CANCELLED -> Color(0xFFF44336).copy(alpha = 0.15f)
                    }
                )
                if (session.openToGroup == true) {
                    BadgeView(text = "Open to Group", color = MaterialTheme.colorScheme.tertiaryContainer)
                }
            }

            // Creator info
            session.createdBy?.let { creator ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarView(
                        url = creator.avatarUrl,
                        name = creator.displayName,
                        size = 28.dp,
                        userId = creator.id
                    )
                    Text(
                        text = "Hosted by ${creator.displayName ?: creator.username ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Deadline
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Deadline: ${formatEventDate(session.responseDeadline)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Response progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$respondedCount of $totalInvitees responded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasResponded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "You've responded",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { if (totalInvitees > 0) respondedCount.toFloat() / totalInvitees else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }

            // Max participants
            session.maxParticipants?.let { maxP ->
                if (maxP > 0) {
                    val slotsUsed = session.invitees?.count { it.hasSlot == true } ?: 0
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Groups, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$slotsUsed of $maxP slots filled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---- Status Banners ----

@Composable
private fun FinalizedBanner(session: PlanningSession, onNavigateToEvent: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFF2196F3).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Verified, null, tint = Color(0xFF2196F3))
            Text(
                "This session has been finalized",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        session.finalizedDate?.let {
            Text(
                "Event date: ${formatEventDate(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        session.createdEventId?.let { eventId ->
            Button(onClick = { onNavigateToEvent(eventId) }) {
                Text("View Event")
            }
        }
    }
}

@Composable
private fun CancelledBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error)
        Text(
            "This session has been cancelled",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// ---- Stepper View ----

@Composable
private fun StepperView(
    currentStep: Int,
    stepCount: Int,
    isCreator: Boolean,
    onStepClick: (Int) -> Unit
) {
    val labels = if (isCreator) {
        listOf("Dates", "Games", "Items", "People", "Finalize")
    } else {
        listOf("Dates", "Games", "Items", "People")
    }
    val icons = if (isCreator) {
        listOf(Icons.Filled.CalendarMonth, Icons.Filled.Casino, Icons.Filled.Backpack, Icons.Filled.Groups, Icons.Filled.Verified)
    } else {
        listOf(Icons.Filled.CalendarMonth, Icons.Filled.Casino, Icons.Filled.Backpack, Icons.Filled.Groups)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        for (step in 0 until stepCount) {
            if (step > 0) {
                // Connector line
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 18.dp)
                        .height(2.dp)
                        .background(
                            if (step <= currentStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStepClick(step) }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            when {
                                step == currentStep -> MaterialTheme.colorScheme.primary
                                step < currentStep -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step < currentStep) {
                        Icon(
                            Icons.Filled.Check, null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            icons[step], null,
                            tint = if (step == currentStep) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = labels[step],
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (step == currentStep) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (step == currentStep) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---- Step 1: Dates ----

@Composable
private fun DatesStep(
    session: PlanningSession,
    selectedAvailability: Map<String, Boolean>,
    cannotAttendAny: Boolean,
    onCannotAttendChange: (Boolean) -> Unit,
    onAvailabilityChange: (String, Boolean) -> Unit,
    isInvitee: Boolean,
    hasResponded: Boolean,
    onSubmitResponse: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Date availability grid
        val dates = session.dates
        if (!dates.isNullOrEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Date Availability", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                    dates.forEach { date ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatEventDate(date.proposedDate),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${date.availableCount ?: 0} available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Response form
        if (session.status == PlanningStatus.OPEN && isInvitee) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (hasResponded) "Update Response" else "Your Availability",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cannot attend any dates")
                        Switch(checked = cannotAttendAny, onCheckedChange = onCannotAttendChange)
                    }

                    if (!cannotAttendAny) {
                        session.dates?.forEach { date ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        formatEventDate(date.proposedDate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    date.startTime?.let {
                                        Text(
                                            "at ${formatPlanningTime(it)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = selectedAvailability[date.id] ?: false,
                                    onCheckedChange = { onAvailabilityChange(date.id, it) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onSubmitResponse,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasResponded) "Update Response" else "Submit Response")
                    }
                }
            }
        }

        // Nav
        StepNav(back = null, forward = 1, stepCount = 5, isCreator = true, onStep = { onNext() })
    }
}

// ---- Step 2: Games ----

@Composable
private fun GamesStep(
    session: PlanningSession,
    currentUserId: String?,
    isCreator: Boolean,
    onVote: (String) -> Unit,
    onUnvote: (String) -> Unit,
    onRemoveSuggestion: (String) -> Unit,
    onSuggestGame: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Suggestions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (session.status == PlanningStatus.OPEN) {
                        Button(
                            onClick = onSuggestGame,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suggest", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                val suggestions = session.gameSuggestions
                if (!suggestions.isNullOrEmpty()) {
                    Text(
                        "Vote for games you'd like to play. Games with 2+ votes are prioritized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    suggestions.sortedByDescending { it.voteCount }.forEach { game ->
                        GameSuggestionRow(
                            game = game,
                            isOpen = session.status == PlanningStatus.OPEN,
                            isCreator = isCreator,
                            currentUserId = currentUserId,
                            onVote = { onVote(game.id) },
                            onUnvote = { onUnvote(game.id) },
                            onRemove = { onRemoveSuggestion(game.id) }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Casino, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Text("No game suggestions yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap \"Suggest\" to search BoardGameGeek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }

        StepNavRow(onBack = onBack, backLabel = "Dates", onNext = onNext, nextLabel = "Items")
    }
}

@Composable
private fun GameSuggestionRow(
    game: GameSuggestion,
    isOpen: Boolean,
    isCreator: Boolean,
    currentUserId: String?,
    onVote: () -> Unit,
    onUnvote: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (game.hasVoted) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        val thumbUrl = game.thumbnailUrl
        if (thumbUrl != null) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Casino, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(game.gameName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val min = game.minPlayers
                val max = game.maxPlayers
                if (min != null && max != null) {
                    Text("$min-$max players", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val time = game.playingTime
                if (time != null && time > 0) {
                    Text("$time min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            game.suggestedBy?.let {
                Text(
                    "by ${it.displayName ?: it.username ?: "someone"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Vote button
        FilledTonalButton(
            onClick = { if (game.hasVoted) onUnvote() else onVote() },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (game.hasVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (game.hasVoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${game.voteCount}")
        }

        // Remove button
        if (isOpen && (isCreator || game.suggestedByUserId == currentUserId)) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
            }
        }
    }
}

// ---- Step 3: Items ----

@Composable
private fun ItemsStep(
    session: PlanningSession,
    currentUserId: String?,
    isCreator: Boolean,
    onAddItem: () -> Unit,
    onClaimItem: (String) -> Unit,
    onUnclaimItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Backpack, null, tint = Color(0xFF4CAF50))
                        Text("Items to Bring", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (session.status == PlanningStatus.OPEN) {
                        IconButton(onClick = onAddItem) {
                            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                val items = session.items
                if (!items.isNullOrEmpty()) {
                    items.forEach { item ->
                        ItemRow(
                            item = item,
                            isOpen = session.status == PlanningStatus.OPEN,
                            isCreator = isCreator,
                            currentUserId = currentUserId,
                            onClaim = { onClaimItem(item.id) },
                            onUnclaim = { onUnclaimItem(item.id) },
                            onRemove = { onRemoveItem(item.id) }
                        )
                    }
                } else {
                    Text(
                        "No items yet -- add things for people to bring!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        StepNavRow(onBack = onBack, backLabel = "Games", onNext = onNext, nextLabel = "People")
    }
}

@Composable
private fun ItemRow(
    item: PlanningItem,
    isOpen: Boolean,
    isCreator: Boolean,
    currentUserId: String?,
    onClaim: () -> Unit,
    onUnclaim: () -> Unit,
    onRemove: () -> Unit
) {
    val categoryIcon = when (item.itemCategory) {
        ItemCategory.FOOD -> Icons.Filled.Backpack
        ItemCategory.DRINKS -> Icons.Filled.Backpack
        ItemCategory.SUPPLIES -> Icons.Filled.Backpack
        ItemCategory.OTHER -> Icons.Filled.Backpack
    }
    val categoryColor = when (item.itemCategory) {
        ItemCategory.FOOD -> Color(0xFFFF9800)
        ItemCategory.DRINKS -> Color(0xFF2196F3)
        ItemCategory.SUPPLIES -> Color(0xFF9C27B0)
        ItemCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.claimedByUserId != null) Color(0xFF4CAF50).copy(alpha = 0.06f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(categoryIcon, null, tint = categoryColor, modifier = Modifier.size(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.itemName, style = MaterialTheme.typography.bodyMedium)
                val qty = item.quantityNeeded
                if (qty != null && qty > 1) {
                    Text("x$qty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val claimedBy = item.claimedBy
            if (claimedBy != null) {
                Text(
                    "Claimed by ${claimedBy.displayName ?: claimedBy.username ?: "someone"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text("Unclaimed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (isOpen) {
            if (item.claimedByUserId == currentUserId) {
                TextButton(onClick = onUnclaim) {
                    Text("Drop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            } else if (item.claimedByUserId == null) {
                TextButton(onClick = onClaim) {
                    Text("I'll bring this", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (isCreator || item.addedByUserId == currentUserId) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

// ---- Step 4: People ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleStep(
    session: PlanningSession,
    isCreator: Boolean,
    stepCount: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invited Members", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    session.maxParticipants?.let { max ->
                        if (max > 0) {
                            BadgeView(text = "Limited to $max")
                        }
                    }
                }

                val invitees = session.invitees
                if (!invitees.isNullOrEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        invitees.forEach { invitee ->
                            InviteeCell(invitee = invitee, session = session)
                        }
                    }
                } else {
                    Text("No one has been invited yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isCreator) {
            StepNavRow(onBack = onBack, backLabel = "Items", onNext = onNext, nextLabel = "Finalize")
        } else {
            StepNavRow(onBack = onBack, backLabel = "Items", onNext = null, nextLabel = null)
        }
    }
}

@Composable
private fun InviteeCell(invitee: PlanningInvitee, session: PlanningSession) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            UserAvatarView(
                url = invitee.user?.avatarUrl,
                name = invitee.user?.displayName,
                size = 48.dp,
                userId = invitee.userId
            )

            // Status badge
            if (invitee.userId != session.createdByUserId && invitee.hasResponded) {
                val canAttend = !invitee.cannotAttendAny
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(16.dp)
                        .background(
                            if (canAttend) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (canAttend) Icons.Filled.Check else Icons.Filled.Close,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Text(
            text = invitee.user?.displayName ?: "Unknown",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Status label
        val (statusText, statusColor) = when {
            invitee.userId == session.createdByUserId -> "Organizer" to MaterialTheme.colorScheme.primary
            invitee.hasSlot == true -> "Has Slot" to Color(0xFF4CAF50)
            invitee.hasResponded && invitee.cannotAttendAny -> "Unavailable" to MaterialTheme.colorScheme.error
            invitee.hasResponded -> "Responded" to MaterialTheme.colorScheme.onSurfaceVariant
            else -> "Pending" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
            color = statusColor
        )
    }
}

// ---- Step 5: Finalize ----

@Composable
private fun FinalizeStep(
    session: PlanningSession,
    selectedDateId: String?,
    selectedGameId: String?,
    tableGameAssignments: Map<Int, String>,
    tableCountInput: Int,
    onSelectDate: (String) -> Unit,
    onSelectGame: (String) -> Unit,
    onAssignTable: (Int, String) -> Unit,
    onRemoveTableAssignment: (Int) -> Unit,
    onUpdateTableCount: (Int) -> Unit,
    onToggleMultiTable: (Boolean) -> Unit,
    onFinalize: () -> Unit,
    onBack: () -> Unit
) {
    val isMultiTable = (session.tableCount ?: 0) >= 2

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Event mode toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Event Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EventModeButton(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Single Event",
                        subtitle = "One game session",
                        isActive = !isMultiTable,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onToggleMultiTable(false) },
                        modifier = Modifier.weight(1f)
                    )
                    EventModeButton(
                        icon = Icons.Filled.GridView,
                        title = "Multi-Table",
                        subtitle = "Multiple games at once",
                        isActive = isMultiTable,
                        color = Color(0xFF9C27B0),
                        onClick = { onToggleMultiTable(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Finalize form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Finalize Session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Finalizing will create a game event with the selected date and game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Date selection
                Text("Select Date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                session.dates?.sortedByDescending { it.availableCount ?: 0 }?.forEach { date ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedDateId == date.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectDate(date.id) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selectedDateId == date.id) Icons.Filled.CheckCircle else Icons.Filled.CalendarMonth,
                            null,
                            tint = if (selectedDateId == date.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(formatEventDate(date.proposedDate), style = MaterialTheme.typography.bodyMedium)
                        date.startTime?.let {
                            Text("at ${formatPlanningTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${date.availableCount ?: 0} available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Game selection (single table)
                if (!isMultiTable) {
                    Text("Select Game", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    val games = session.gameSuggestions
                    if (!games.isNullOrEmpty()) {
                        games.sortedByDescending { it.voteCount }.forEach { game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedGameId == game.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectGame(game.id) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (selectedGameId == game.id) Icons.Filled.CheckCircle else Icons.Filled.Casino,
                                    null,
                                    tint = if (selectedGameId == game.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(game.gameName, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${game.voteCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        Text("No games suggested", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Validation
                val issues = mutableListOf<String>()
                if (selectedDateId == null) issues.add("Select a date above")
                if (isMultiTable && tableGameAssignments.isEmpty()) issues.add("Assign at least one game to a table")

                if (issues.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF9800).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        issues.forEach { issue ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("!", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                Text(issue, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF9800))
                            }
                        }
                    }
                }

                // Finalize button
                Button(
                    onClick = onFinalize,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedDateId != null && (!isMultiTable || tableGameAssignments.isNotEmpty())
                ) {
                    Text("Create Game Event")
                }
            }
        }

        StepNavRow(onBack = onBack, backLabel = "People", onNext = null, nextLabel = null)
    }
}

@Composable
private fun EventModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .background(
                if (isActive) color.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(12.dp)
            )
            .then(
                if (isActive) Modifier.border(2.dp, color, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, modifier = Modifier.size(22.dp), tint = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = if (isActive) color else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isActive) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp), tint = color)
            }
        }
    }
}

// ---- Chat Section ----

@Composable
private fun PlanningChatSection(showChat: Boolean, onToggle: () -> Unit) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (showChat) 180f else 0f,
        label = "chat_chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Planning Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(
                    Icons.Filled.ExpandMore, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(visible = showChat) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Chat coming soon...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ---- Navigation Helpers ----

@Composable
private fun StepNav(back: Int?, forward: Int?, stepCount: Int, isCreator: Boolean, onStep: (Int) -> Unit) {
    // Simplified - just use StepNavRow
}

@Composable
private fun StepNavRow(
    onBack: (() -> Unit)?,
    backLabel: String?,
    onNext: (() -> Unit)?,
    nextLabel: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null && backLabel != null) {
            TextButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (onNext != null && nextLabel != null) {
            TextButton(onClick = onNext) {
                Text("Next: $nextLabel", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ---- Add Item Dialog ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, ItemCategory, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.FOOD) }
    var quantity by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Add an item for someone to bring to game night.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category picker
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ItemCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (optional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, category, quantity.toIntOrNull()) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Time formatting helper ───

private fun formatPlanningTime(timeString: String): String {
    return try {
        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$hour12:$minute $amPm"
    } catch (_: Exception) {
        timeString
    }
}
