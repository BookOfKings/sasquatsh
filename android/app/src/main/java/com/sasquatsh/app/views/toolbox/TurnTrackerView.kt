package com.sasquatsh.app.views.toolbox

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

// --- Player Colors ---
private val PlayerColors = listOf(
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1), // Cyan
    Color(0xFFD81B60), // Pink
    Color(0xFFFDD835), // Yellow
    Color(0xFF98FB98), // Mint
    Color(0xFF3949AB), // Indigo
)

private fun playerColor(index: Int): Color = PlayerColors[index % PlayerColors.size]

// --- Data classes ---
private data class TurnRecord(
    val playerIndex: Int,
    val durationMs: Long
)

private data class RoundRecord(
    val roundNumber: Int,
    val durationMs: Long
)

// --- Phases ---
private enum class Phase { Setup, PickFirst, Playing }
private enum class SetupMode { QuickStart, Named }

// --- SharedPreferences helpers ---
private const val PREFS_NAME = "turn_tracker"
private const val KEY_SAVED_NAMES = "saved_names"

private fun loadSavedNames(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_NAMES, "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
}

private fun saveName(context: Context, name: String) {
    val existing = loadSavedNames(context).toMutableList()
    existing.remove(name)
    existing.add(0, name)
    val trimmed = existing.take(50)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SAVED_NAMES, trimmed.joinToString("|||")).apply()
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02d".format(minutes, seconds) else "%ds".format(seconds)
}

private fun formatTimePrecise(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return if (minutes > 0) "%d:%02d.%d".format(minutes, seconds, tenths)
    else "%d.%d".format(seconds, tenths)
}

private fun hapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    } catch (_: Exception) { /* ignore */ }
}

// ==============================
// MAIN COMPOSABLE
// ==============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnTrackerView(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- State ---
    var phase by rememberSaveable { mutableStateOf(Phase.Setup) }
    var setupMode by rememberSaveable { mutableStateOf(SetupMode.QuickStart) }

    // Settings
    var trackTime by rememberSaveable { mutableStateOf(false) }
    var trashTalk by rememberSaveable { mutableStateOf(false) }
    var trashTalkThreshold by rememberSaveable { mutableIntStateOf(60) } // seconds
    var rotateFirstPlayer by rememberSaveable { mutableStateOf(false) }

    // Quick start
    var quickPlayerCount by rememberSaveable { mutableIntStateOf(4) }
    var quickFirstName by rememberSaveable { mutableStateOf("") }

    // Named players
    val namedPlayers = remember { mutableStateListOf<String>() }
    var newPlayerName by rememberSaveable { mutableStateOf("") }

    // Playing state
    val playerNames = remember { mutableStateListOf<String>() }
    var currentPlayerIndex by rememberSaveable { mutableIntStateOf(0) }
    var firstPlayerIndex by rememberSaveable { mutableIntStateOf(0) }
    var roundNumber by rememberSaveable { mutableIntStateOf(1) }

    // Timer
    var turnStartTime by rememberSaveable { mutableLongStateOf(0L) }
    var currentElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    val turnRecords = remember { mutableStateListOf<TurnRecord>() }
    val roundRecords = remember { mutableStateListOf<RoundRecord>() }
    var roundStartTime by rememberSaveable { mutableLongStateOf(0L) }
    var trashTalkSpoken by rememberSaveable { mutableStateOf(false) }

    // Stats sheet
    var showStats by rememberSaveable { mutableStateOf(false) }

    // Saved names
    var savedNames by remember { mutableStateOf(loadSavedNames(context)) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS if trash talk enabled
    DisposableEffect(trashTalk) {
        if (trashTalk) {
            val engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                }
            }
            tts = engine
            onDispose { engine.shutdown() }
        } else {
            onDispose { }
        }
    }

    // Keep screen on during playing
    DisposableEffect(phase) {
        if (phase == Phase.Playing) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Timer tick
    LaunchedEffect(phase, trackTime) {
        if (phase == Phase.Playing && trackTime) {
            while (true) {
                currentElapsedMs = System.currentTimeMillis() - turnStartTime
                // Trash talk check
                if (trashTalk && !trashTalkSpoken &&
                    currentElapsedMs >= trashTalkThreshold * 1000L
                ) {
                    trashTalkSpoken = true
                    tts?.speak("You should just pass!", TextToSpeech.QUEUE_FLUSH, null, "trash")
                }
                delay(100)
            }
        }
    }

    // --- Helper values & lambdas ---
    val playerCount = playerNames.size

    val currentPlayerName =
        if (playerNames.isNotEmpty()) playerNames[currentPlayerIndex % playerNames.size] else ""

    val nextPlayerName =
        if (playerNames.isNotEmpty()) playerNames[(currentPlayerIndex + 1) % playerNames.size] else ""

    val doStartGame: (List<String>, Int) -> Unit = { names, firstIndex ->
        playerNames.clear()
        playerNames.addAll(names)
        currentPlayerIndex = firstIndex
        firstPlayerIndex = firstIndex
        roundNumber = 1
        turnStartTime = System.currentTimeMillis()
        roundStartTime = System.currentTimeMillis()
        currentElapsedMs = 0L
        trashTalkSpoken = false
        turnRecords.clear()
        roundRecords.clear()
        phase = Phase.Playing
        names.forEach { n -> if (n.isNotBlank()) saveName(context, n) }
        savedNames = loadSavedNames(context)
    }

    val doNextTurn: () -> Unit = {
        hapticFeedback(context)
        if (trackTime) {
            turnRecords.add(TurnRecord(currentPlayerIndex, currentElapsedMs))
        }

        val nextIndex = (currentPlayerIndex + 1) % playerNames.size
        if (nextIndex == firstPlayerIndex) {
            if (trackTime) {
                roundRecords.add(
                    RoundRecord(roundNumber, System.currentTimeMillis() - roundStartTime)
                )
            }
            roundNumber++
            if (rotateFirstPlayer) {
                firstPlayerIndex = (firstPlayerIndex + 1) % playerNames.size
            }
            roundStartTime = System.currentTimeMillis()
        }

        currentPlayerIndex = nextIndex
        turnStartTime = System.currentTimeMillis()
        currentElapsedMs = 0L
        trashTalkSpoken = false
    }

    val doPreviousTurn: () -> Unit = {
        hapticFeedback(context)
        val prevIndex = (currentPlayerIndex - 1 + playerNames.size) % playerNames.size

        if (currentPlayerIndex == firstPlayerIndex && roundNumber > 1) {
            roundNumber--
            if (rotateFirstPlayer) {
                firstPlayerIndex = (firstPlayerIndex - 1 + playerNames.size) % playerNames.size
            }
            if (roundRecords.isNotEmpty()) roundRecords.removeAt(roundRecords.lastIndex)
        }

        if (turnRecords.isNotEmpty()) turnRecords.removeAt(turnRecords.lastIndex)

        currentPlayerIndex = prevIndex
        turnStartTime = System.currentTimeMillis()
        currentElapsedMs = 0L
        trashTalkSpoken = false
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Turn Tracker") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (phase == Phase.Playing) {
                            // could confirm, but for now just go back
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (phase == Phase.Playing && trackTime && turnRecords.isNotEmpty()) {
                        IconButton(onClick = { showStats = true }) {
                            Icon(Icons.Default.BarChart, contentDescription = "Stats")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (phase) {
            Phase.Setup -> SetupPhaseContent(
                modifier = Modifier.padding(padding),
                setupMode = setupMode,
                onSetupModeChange = { setupMode = it },
                trackTime = trackTime,
                onTrackTimeChange = { trackTime = it },
                trashTalk = trashTalk,
                onTrashTalkChange = { trashTalk = it },
                trashTalkThreshold = trashTalkThreshold,
                onThresholdChange = { trashTalkThreshold = it },
                rotateFirstPlayer = rotateFirstPlayer,
                onRotateChange = { rotateFirstPlayer = it },
                quickPlayerCount = quickPlayerCount,
                onQuickCountChange = { quickPlayerCount = it },
                quickFirstName = quickFirstName,
                onQuickFirstNameChange = { quickFirstName = it },
                namedPlayers = namedPlayers,
                newPlayerName = newPlayerName,
                onNewPlayerNameChange = { newPlayerName = it },
                savedNames = savedNames,
                onAddPlayer = { name ->
                    if (name.isNotBlank() && namedPlayers.size < 20) {
                        namedPlayers.add(name.trim())
                        newPlayerName = ""
                    }
                },
                onRemovePlayer = { index -> namedPlayers.removeAt(index) },
                onStartQuick = {
                    val names = (1..quickPlayerCount).map { i ->
                        if (i == 1 && quickFirstName.isNotBlank()) quickFirstName.trim()
                        else "P$i"
                    }
                    if (quickFirstName.isNotBlank()) saveName(context, quickFirstName.trim())
                    savedNames = loadSavedNames(context)
                    doStartGame(names, 0)
                },
                onPickFirst = {
                    phase = Phase.PickFirst
                }
            )

            Phase.PickFirst -> PickFirstPhaseContent(
                modifier = Modifier.padding(padding),
                players = namedPlayers.toList(),
                onPickPlayer = { index -> doStartGame(namedPlayers.toList(), index) },
                onBackToSetup = { phase = Phase.Setup }
            )

            Phase.Playing -> PlayingPhaseContent(
                modifier = Modifier.padding(padding),
                playerNames = playerNames.toList(),
                currentPlayerIndex = currentPlayerIndex,
                firstPlayerIndex = firstPlayerIndex,
                roundNumber = roundNumber,
                rotateFirstPlayer = rotateFirstPlayer,
                trackTime = trackTime,
                elapsedMs = currentElapsedMs,
                turnRecords = turnRecords.toList(),
                roundRecords = roundRecords.toList(),
                onNextTurn = { doNextTurn() },
                onUndo = { doPreviousTurn() },
                onEnd = { phase = Phase.Setup }
            )
        }
    }

    // Stats bottom sheet
    if (showStats) {
        StatsSheet(
            playerNames = playerNames.toList(),
            turnRecords = turnRecords.toList(),
            roundRecords = roundRecords.toList(),
            onDismiss = { showStats = false }
        )
    }
}

// ==============================
// SETUP PHASE
// ==============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupPhaseContent(
    modifier: Modifier = Modifier,
    setupMode: SetupMode,
    onSetupModeChange: (SetupMode) -> Unit,
    trackTime: Boolean,
    onTrackTimeChange: (Boolean) -> Unit,
    trashTalk: Boolean,
    onTrashTalkChange: (Boolean) -> Unit,
    trashTalkThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    rotateFirstPlayer: Boolean,
    onRotateChange: (Boolean) -> Unit,
    quickPlayerCount: Int,
    onQuickCountChange: (Int) -> Unit,
    quickFirstName: String,
    onQuickFirstNameChange: (String) -> Unit,
    namedPlayers: List<String>,
    newPlayerName: String,
    onNewPlayerNameChange: (String) -> Unit,
    savedNames: List<String>,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (Int) -> Unit,
    onStartQuick: () -> Unit,
    onPickFirst: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Segmented toggle: Quick Start / Named Players
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = setupMode == SetupMode.QuickStart,
                onClick = { onSetupModeChange(SetupMode.QuickStart) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Quick Start")
            }
            SegmentedButton(
                selected = setupMode == SetupMode.Named,
                onClick = { onSetupModeChange(SetupMode.Named) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Named Players")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Track turn times toggle
        SettingToggle(
            label = "Track turn times",
            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF43A047)) },
            checked = trackTime,
            onCheckedChange = onTrackTimeChange,
            activeColor = Color(0xFF43A047)
        )

        // Trash talk (only if timer enabled)
        AnimatedVisibility(visible = trackTime) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggle(
                    label = "Trash talk slow players",
                    icon = {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFE53935)
                        )
                    },
                    checked = trashTalk,
                    onCheckedChange = onTrashTalkChange,
                    activeColor = Color(0xFFE53935)
                )
                AnimatedVisibility(visible = trashTalk) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        val thresholds = listOf(30 to "30s", 60 to "1m", 120 to "2m", 180 to "3m", 300 to "5m")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            thresholds.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = trashTalkThreshold == value,
                                    onClick = { onThresholdChange(value) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = thresholds.size
                                    )
                                ) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rotate first player
        SettingToggle(
            label = "Rotate first player each round",
            icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF43A047)) },
            checked = rotateFirstPlayer,
            onCheckedChange = onRotateChange,
            activeColor = Color(0xFF43A047)
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        when (setupMode) {
            SetupMode.QuickStart -> QuickStartSetup(
                playerCount = quickPlayerCount,
                onCountChange = onQuickCountChange,
                firstName = quickFirstName,
                onFirstNameChange = onQuickFirstNameChange,
                savedNames = savedNames,
                onStart = onStartQuick
            )
            SetupMode.Named -> NamedPlayersSetup(
                players = namedPlayers,
                newName = newPlayerName,
                onNewNameChange = onNewPlayerNameChange,
                savedNames = savedNames,
                onAddPlayer = onAddPlayer,
                onRemovePlayer = onRemovePlayer,
                onNext = onPickFirst
            )
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor
            )
        )
    }
}

// ==============================
// QUICK START SETUP
// ==============================
@Composable
private fun QuickStartSetup(
    playerCount: Int,
    onCountChange: (Int) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    savedNames: List<String>,
    onStart: () -> Unit
) {
    // Who goes first
    Text(
        "Who goes first?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = firstName,
        onValueChange = onFirstNameChange,
        label = { Text("First player name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )

    // Autocomplete suggestions
    if (firstName.isNotBlank()) {
        val suggestions = savedNames.filter {
            it.contains(firstName, ignoreCase = true) && it != firstName
        }.take(5)
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { name ->
                    FilledTonalButton(
                        onClick = { onFirstNameChange(name) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(name, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Quick pick chips from recently saved names
    if (firstName.isBlank() && savedNames.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            savedNames.take(8).forEach { name ->
                FilledTonalButton(
                    onClick = { onFirstNameChange(name) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(name, fontSize = 13.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // How many players
    Text(
        "How many players?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (playerCount > 2) onCountChange(playerCount - 1) },
            enabled = playerCount > 2
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            "$playerCount",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = { if (playerCount < 20) onCountChange(playerCount + 1) },
            enabled = playerCount < 20
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }

    // Colored dots preview
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until playerCount).forEach { i ->
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(playerColor(i))
                            )
                            // Star overlay for P1
                            if (i == 0) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "First player",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (i == 0 && firstName.isNotBlank()) firstName.take(6) else "P${i + 1}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
    ) {
        Text("Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==============================
// NAMED PLAYERS SETUP
// ==============================
@Composable
private fun NamedPlayersSetup(
    players: List<String>,
    newName: String,
    onNewNameChange: (String) -> Unit,
    savedNames: List<String>,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (Int) -> Unit,
    onNext: () -> Unit
) {
    Text(
        "Add players in seating order",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = newName,
            onValueChange = onNewNameChange,
            label = { Text("Player name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddPlayer(newName) })
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { onAddPlayer(newName) },
            enabled = newName.isNotBlank() && players.size < 20
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add player")
        }
    }

    // Autocomplete suggestions
    if (newName.isNotBlank()) {
        val suggestions = savedNames.filter {
            it.contains(newName, ignoreCase = true) &&
                    !players.contains(it) &&
                    it != newName
        }.take(5)
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { name ->
                    FilledTonalButton(
                        onClick = { onAddPlayer(name) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(name, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Player list
    players.forEachIndexed { index, name ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Numbered circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(playerColor(index)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                IconButton(
                    onClick = { onRemovePlayer(index) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        enabled = players.size >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
    ) {
        Text("Next: Pick First Player", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==============================
// PICK FIRST PHASE
// ==============================
@Composable
private fun PickFirstPhaseContent(
    modifier: Modifier = Modifier,
    players: List<String>,
    onPickPlayer: (Int) -> Unit,
    onBackToSetup: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Who goes first?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(players) { index, name ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickPlayer(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(playerColor(index)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBackToSetup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

// ==============================
// PLAYING PHASE
// ==============================
@Composable
private fun PlayingPhaseContent(
    modifier: Modifier = Modifier,
    playerNames: List<String>,
    currentPlayerIndex: Int,
    firstPlayerIndex: Int,
    roundNumber: Int,
    rotateFirstPlayer: Boolean,
    trackTime: Boolean,
    elapsedMs: Long,
    turnRecords: List<TurnRecord>,
    roundRecords: List<RoundRecord>,
    onNextTurn: () -> Unit,
    onUndo: () -> Unit,
    onEnd: () -> Unit
) {
    val playerCount = playerNames.size
    val currentName = playerNames.getOrElse(currentPlayerIndex % playerCount) { "?" }
    val nextName = playerNames.getOrElse((currentPlayerIndex + 1) % playerCount) { "?" }
    val currentColor = playerColor(currentPlayerIndex % playerCount)

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar: Round info + player dots
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Round $roundNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (rotateFirstPlayer) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${playerNames.getOrElse(firstPlayerIndex) { "" }} starts)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until playerCount).forEach { i ->
                            val isCurrent = i == currentPlayerIndex
                            val dotSize = if (isCurrent) 16.dp else 10.dp
                            val dotAlpha = if (isCurrent) 1f else 0.4f
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(playerColor(i).copy(alpha = dotAlpha))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center: Large tappable circle
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBehind {
                    // Pulsing glow ring
                    drawCircle(
                        color = currentColor.copy(alpha = glowAlpha),
                        radius = size.minDimension / 2 + 12.dp.toPx(),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                .clip(CircleShape)
                .background(currentColor)
                .clickable { onNextTurn() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    currentName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (trackTime) {
                    val timeColor = if (elapsedMs > 60_000) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.9f)
                    Text(
                        formatTimePrecise(elapsedMs),
                        color = timeColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        "Tap when done",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Up next
        Text(
            "Up next: $nextName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Timer stats bar
        if (trackTime && turnRecords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TimerStatsBar(playerNames = playerNames, turnRecords = turnRecords)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Undo")
            }
            Button(
                onClick = onEnd,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("End")
            }
        }
    }
}

// ==============================
// TIMER STATS BAR
// ==============================
@Composable
private fun TimerStatsBar(
    playerNames: List<String>,
    turnRecords: List<TurnRecord>
) {
    // Find slowest player
    val avgByPlayer = turnRecords.groupBy { it.playerIndex }
        .mapValues { (_, records) -> records.map { it.durationMs }.average().toLong() }
    val slowestEntry = avgByPlayer.maxByOrNull { it.value }
    val slowestName = slowestEntry?.let { playerNames.getOrElse(it.key) { "?" } } ?: ""
    val slowestTime = slowestEntry?.value ?: 0L

    // Avg round time (from all turns in a round)
    val totalTime = turnRecords.sumOf { it.durationMs }
    val avgTurnTime = totalTime / turnRecords.size

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Slowest
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDC22", fontSize = 16.sp) // Tortoise emoji
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$slowestName ${formatTime(slowestTime)}",
                    fontSize = 13.sp,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Medium
                )
            }
            // Avg
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Avg: ${formatTime(avgTurnTime)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==============================
// STATS BOTTOM SHEET
// ==============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsSheet(
    playerNames: List<String>,
    turnRecords: List<TurnRecord>,
    roundRecords: List<RoundRecord>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Player stats: avg time per player, sorted slowest first
    data class PlayerStat(
        val index: Int,
        val name: String,
        val avgMs: Long,
        val turnCount: Int,
        val totalMs: Long
    )

    val playerStats = turnRecords.groupBy { it.playerIndex }
        .map { (idx, records) ->
            PlayerStat(
                index = idx,
                name = playerNames.getOrElse(idx) { "P${idx + 1}" },
                avgMs = records.map { it.durationMs }.average().toLong(),
                turnCount = records.size,
                totalMs = records.sumOf { it.durationMs }
            )
        }
        .sortedByDescending { it.avgMs }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Turn Times",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slowest to Fastest
            Text(
                "Slowest to Fastest",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            playerStats.forEach { stat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(playerColor(stat.index))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stat.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatTime(stat.avgMs),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stat.avgMs > 60_000) Color(0xFFE53935)
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${stat.turnCount} turns \u2022 ${formatTime(stat.totalMs)} total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Round Times
            if (roundRecords.isNotEmpty()) {
                Text(
                    "Round Times",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val roundDurations = roundRecords.map { it.durationMs }
                val avgRound = roundDurations.average().toLong()
                val fastestRound = roundDurations.min()
                val slowestRound = roundDurations.max()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        RoundStatRow("Rounds completed", "${roundRecords.size}")
                        RoundStatRow("Average", formatTime(avgRound))
                        RoundStatRow("Fastest", formatTime(fastestRound))
                        RoundStatRow("Slowest", formatTime(slowestRound))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoundStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
