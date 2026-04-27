package com.sasquatsh.app.views.toolbox

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.viewmodels.RoundCounterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundCounterView(
    onBack: () -> Unit = {},
    viewModel: RoundCounterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Round Counter") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Timer bar
            TimerBar(uiState)

            Spacer(modifier = Modifier.weight(1f))

            // Center: round number + controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round label
                if (uiState.isCountdownMode) {
                    Text(
                        "ROUND ${uiState.state.roundNumber} OF ${uiState.maxRounds}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                } else {
                    Text(
                        "ROUND",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big round number with progress ring
                val ringColor = when {
                    uiState.reachedMaxRound -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    uiState.isTimerRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                }
                val progressColor = if (uiState.countdownRemaining <= 10000 && uiState.isTimerRunning)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .drawBehind {
                            // Background ring
                            drawCircle(
                                color = ringColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 6.dp.toPx())
                            )
                            // Progress ring (countdown mode)
                            if (uiState.isCountdownMode && uiState.roundDurationSeconds > 0) {
                                val progress =
                                    (uiState.countdownRemaining.toFloat() / (uiState.roundDurationSeconds * 1000f)).coerceIn(0f, 1f)
                                drawArc(
                                    color = progressColor,
                                    startAngle = -90f,
                                    sweepAngle = progress * 360f,
                                    useCenter = false,
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, size.height),
                                    style = Stroke(
                                        width = 6.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                        .clickable { viewModel.beginEditRound() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.state.roundNumber}",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.reachedMaxRound) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.isCountdownMode) {
                            Text(
                                uiState.formattedCountdown,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.countdownRemaining <= 10000 && uiState.isTimerRunning)
                                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (uiState.reachedMaxRound) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Final round!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // +/- controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus
                    FloatingActionButton(
                        onClick = { viewModel.decrement() },
                        modifier = Modifier.size(64.dp),
                        containerColor = if (uiState.state.roundNumber <= uiState.state.minimumRound)
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(26.dp),
                            tint = if (uiState.state.roundNumber <= uiState.state.minimumRound)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Play/Pause
                    FloatingActionButton(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(
                            if (uiState.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isTimerRunning) "Pause" else "Play",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Plus
                    FloatingActionButton(
                        onClick = { viewModel.increment() },
                        modifier = Modifier.size(64.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.showResetConfirmation(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                IconButton(
                    onClick = { showSettings = true }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }

                Button(
                    onClick = { viewModel.newSession() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New")
                }
            }
        }
    }

    // Reset confirmation dialog
    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetConfirmation(false) },
            title = { Text("Reset Round?") },
            text = { Text("Reset to round ${uiState.state.startingRound} and clear timers?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    viewModel.showResetConfirmation(false)
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showResetConfirmation(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit round dialog
    if (uiState.showEditRound) {
        AlertDialog(
            onDismissRequest = { viewModel.commitEditRound() },
            title = { Text("Set Round") },
            text = {
                OutlinedTextField(
                    value = uiState.editRoundText,
                    onValueChange = { viewModel.updateEditRoundText(it) },
                    label = { Text("Round") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.commitEditRound() }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.commitEditRound() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings sheet
    if (showSettings) {
        SettingsSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun TimerBar(uiState: com.sasquatsh.app.viewmodels.RoundCounterUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (uiState.isCountdownMode) "Left" else "Round",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (uiState.isCountdownMode) uiState.formattedCountdown else uiState.formattedRoundTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.isCountdownMode && uiState.countdownRemaining <= 10000 && uiState.isTimerRunning)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .drawBehind {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height)
                        )
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Session",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    uiState.formattedSessionTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.isTimerRunning) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .drawBehind {
                            drawCircle(color = Color.Red)
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    uiState: com.sasquatsh.app.viewmodels.RoundCounterUiState,
    viewModel: RoundCounterViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Countdown mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Countdown Mode", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.isCountdownMode,
                    onCheckedChange = { viewModel.updateCountdownMode(it) }
                )
            }

            if (uiState.isCountdownMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Round Duration", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val durationOptions = listOf(30, 60, 90, 120, 180, 300, 600)
                durationOptions.forEach { secs ->
                    val label = if (secs < 60) "$secs seconds"
                    else "${secs / 60} minute${if (secs / 60 == 1) "" else "s"}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateRoundDurationSeconds(secs) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (uiState.roundDurationSeconds == secs) {
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Max Rounds", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rounds", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (uiState.maxRounds > 1) viewModel.updateMaxRounds(uiState.maxRounds - 1) },
                            enabled = uiState.maxRounds > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            "${uiState.maxRounds}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { if (uiState.maxRounds < 99) viewModel.updateMaxRounds(uiState.maxRounds + 1) },
                            enabled = uiState.maxRounds < 99
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
