package com.sasquatsh.app.views.toolbox

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Data classes ---

private data class ScoringPlayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val score: Int = 0
)

private data class ScoreAction(
    val id: String = UUID.randomUUID().toString(),
    val playerId: String,
    val playerName: String,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private enum class ScoringPhase { SETUP, PLAYING, FINISHED }

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

// --- SharedPreferences helpers ---

private const val PREFS_NAME = "score_keeper"
private const val KEY_SAVED_NAMES = "scorekeeper_names"
private const val KEY_SAVED_GAMES = "scorekeeper_games"
private const val KEY_SESSION = "scorekeeper_session"
private const val TURN_TRACKER_PREFS = "turn_tracker"
private const val TURN_TRACKER_KEY_NAMES = "saved_names"

private fun loadSavedNames(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_NAMES, "") ?: ""
    val skNames = if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
    // Also load from turn tracker for shared autocomplete
    val ttPrefs = context.getSharedPreferences(TURN_TRACKER_PREFS, Context.MODE_PRIVATE)
    val ttRaw = ttPrefs.getString(TURN_TRACKER_KEY_NAMES, "") ?: ""
    val ttNames = if (ttRaw.isBlank()) emptyList() else ttRaw.split("|||").filter { it.isNotBlank() }
    return (skNames + ttNames).distinct().sorted()
}

private fun saveName(context: Context, name: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_NAMES, "") ?: ""
    val existing = if (raw.isBlank()) mutableListOf() else raw.split("|||").filter { it.isNotBlank() }.toMutableList()
    existing.remove(name)
    existing.add(0, name)
    val trimmed = existing.take(50)
    prefs.edit().putString(KEY_SAVED_NAMES, trimmed.joinToString("|||")).apply()
    // Also save to turn tracker prefs for sharing
    val ttPrefs = context.getSharedPreferences(TURN_TRACKER_PREFS, Context.MODE_PRIVATE)
    val ttRaw = ttPrefs.getString(TURN_TRACKER_KEY_NAMES, "") ?: ""
    val ttExisting = if (ttRaw.isBlank()) mutableListOf() else ttRaw.split("|||").filter { it.isNotBlank() }.toMutableList()
    if (!ttExisting.contains(name)) {
        ttExisting.add(0, name)
        ttPrefs.edit().putString(TURN_TRACKER_KEY_NAMES, ttExisting.take(50).joinToString("|||")).apply()
    }
}

private fun loadSavedGames(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_GAMES, "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("|||").filter { it.isNotBlank() }
}

private fun saveGameName(context: Context, name: String) {
    if (name.isBlank()) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SAVED_GAMES, "") ?: ""
    val existing = if (raw.isBlank()) mutableListOf() else raw.split("|||").filter { it.isNotBlank() }.toMutableList()
    existing.remove(name)
    existing.add(0, name)
    val trimmed = existing.take(20)
    prefs.edit().putString(KEY_SAVED_GAMES, trimmed.joinToString("|||")).apply()
}

// --- Session persistence ---

private fun saveSession(
    context: Context,
    phase: ScoringPhase,
    players: List<ScoringPlayer>,
    gameName: String,
    highestWins: Boolean,
    history: List<ScoreAction>
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = JSONObject().apply {
        put("phase", phase.name)
        put("gameName", gameName)
        put("highestWins", highestWins)
        put("players", JSONArray().apply {
            players.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("score", p.score)
                })
            }
        })
        put("history", JSONArray().apply {
            history.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("playerId", a.playerId)
                    put("playerName", a.playerName)
                    put("amount", a.amount)
                    put("timestamp", a.timestamp)
                })
            }
        })
    }
    prefs.edit().putString(KEY_SESSION, json.toString()).apply()
}

private data class SessionData(
    val phase: ScoringPhase,
    val players: List<ScoringPlayer>,
    val gameName: String,
    val highestWins: Boolean,
    val history: List<ScoreAction>
)

private fun loadSession(context: Context): SessionData? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SESSION, null) ?: return null
    return try {
        val json = JSONObject(raw)
        val phase = ScoringPhase.valueOf(json.getString("phase"))
        val gameName = json.optString("gameName", "")
        val highestWins = json.optBoolean("highestWins", true)
        val playersArr = json.getJSONArray("players")
        val players = (0 until playersArr.length()).map { i ->
            val p = playersArr.getJSONObject(i)
            ScoringPlayer(
                id = p.getString("id"),
                name = p.getString("name"),
                score = p.getInt("score")
            )
        }
        val historyArr = json.getJSONArray("history")
        val history = (0 until historyArr.length()).map { i ->
            val a = historyArr.getJSONObject(i)
            ScoreAction(
                id = a.getString("id"),
                playerId = a.getString("playerId"),
                playerName = a.getString("playerName"),
                amount = a.getInt("amount"),
                timestamp = a.getLong("timestamp")
            )
        }
        SessionData(phase, players, gameName, highestWins, history)
    } catch (_: Exception) {
        null
    }
}

private fun clearSession(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_SESSION).apply()
}

// --- Timestamp formatting ---

private fun formatTimestamp(timestamp: Long): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(timestamp))
}

// --- Main Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreKeeperView(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // State
    var phase by rememberSaveable { mutableStateOf(ScoringPhase.SETUP) }
    val players = remember { mutableStateListOf<ScoringPlayer>() }
    var gameName by rememberSaveable { mutableStateOf("") }
    var newPlayerName by rememberSaveable { mutableStateOf("") }
    var highestWins by rememberSaveable { mutableStateOf(true) }
    val history = remember { mutableStateListOf<ScoreAction>() }
    val scoreInputs = remember { mutableStateMapOf<String, String>() }

    // UI state
    var showEndConfirm by remember { mutableStateOf(false) }
    var showFullHistory by remember { mutableStateOf(false) }
    var playerHistoryId by remember { mutableStateOf<String?>(null) }
    var showGameDropdown by remember { mutableStateOf(false) }
    var showNameDropdown by remember { mutableStateOf(false) }

    // Saved data
    var savedNames by remember { mutableStateOf(loadSavedNames(context)) }
    var savedGames by remember { mutableStateOf(loadSavedGames(context)) }
    var sessionRestored by rememberSaveable { mutableStateOf(false) }

    // Restore session on first launch
    LaunchedEffect(sessionRestored) {
        if (!sessionRestored) {
            sessionRestored = true
            val session = loadSession(context)
            if (session != null && session.phase != ScoringPhase.SETUP) {
                phase = session.phase
                gameName = session.gameName
                highestWins = session.highestWins
                players.clear()
                players.addAll(session.players)
                history.clear()
                history.addAll(session.history)
            }
        }
    }

    // Auto-save session on state changes
    LaunchedEffect(phase, players.toList(), history.toList(), gameName, highestWins) {
        if (sessionRestored) {
            if (phase == ScoringPhase.SETUP) {
                clearSession(context)
            } else {
                saveSession(context, phase, players.toList(), gameName, highestWins, history.toList())
            }
        }
    }

    // Helper to add score
    fun addScore(playerIndex: Int, amount: Int) {
        val p = players[playerIndex]
        players[playerIndex] = p.copy(score = p.score + amount)
        history.add(
            ScoreAction(
                playerId = p.id,
                playerName = p.name,
                amount = amount
            )
        )
    }

    // Helper to add player
    fun addPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (players.any { it.name.equals(trimmed, ignoreCase = true) }) return
        players.add(ScoringPlayer(name = trimmed))
        saveName(context, trimmed)
        savedNames = loadSavedNames(context)
        newPlayerName = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Score Keeper") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (phase == ScoringPhase.SETUP) clearSession(context)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (phase) {
                // ===================== SETUP =====================
                ScoringPhase.SETUP -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Game Name
                        item {
                            Text("Game Name", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box {
                                OutlinedTextField(
                                    value = gameName,
                                    onValueChange = { v ->
                                        gameName = v
                                        showGameDropdown = v.isNotEmpty() &&
                                                savedGames.any { it.contains(v, ignoreCase = true) && !it.equals(v, ignoreCase = true) }
                                    },
                                    placeholder = { Text("e.g. Flip 7, Catan, Uno...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        showGameDropdown = false
                                    })
                                )
                                // Autocomplete dropdown
                                val filteredGames = savedGames.filter {
                                    gameName.isNotEmpty() && it.contains(gameName, ignoreCase = true) && !it.equals(gameName, ignoreCase = true)
                                }
                                DropdownMenu(
                                    expanded = showGameDropdown && filteredGames.isNotEmpty(),
                                    onDismissRequest = { showGameDropdown = false }
                                ) {
                                    filteredGames.take(8).forEach { game ->
                                        DropdownMenuItem(
                                            text = { Text(game) },
                                            onClick = {
                                                gameName = game
                                                showGameDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            // Quick-pick chips for recent games
                            if (gameName.isEmpty() && savedGames.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    savedGames.take(10).forEach { game ->
                                        AssistChip(
                                            onClick = { gameName = game },
                                            label = { Text(game, fontSize = 13.sp) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Winner toggle
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Winner", style = MaterialTheme.typography.labelLarge)
                                SingleChoiceSegmentedButtonRow {
                                    SegmentedButton(
                                        selected = highestWins,
                                        onClick = { highestWins = true },
                                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                                    ) { Text("Highest") }
                                    SegmentedButton(
                                        selected = !highestWins,
                                        onClick = { highestWins = false },
                                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                                    ) { Text("Lowest") }
                                }
                            }
                        }

                        // Add Players
                        item {
                            Text("Add Players", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newPlayerName,
                                        onValueChange = { v ->
                                            newPlayerName = v
                                            val playerNameSet = players.map { it.name.lowercase() }.toSet()
                                            showNameDropdown = v.isNotEmpty() && savedNames.any {
                                                it.contains(v, ignoreCase = true) &&
                                                        !it.equals(v, ignoreCase = true) &&
                                                        it.lowercase() !in playerNameSet
                                            }
                                        },
                                        placeholder = { Text("Player name") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardActions = KeyboardActions(onDone = {
                                            addPlayer(newPlayerName)
                                            showNameDropdown = false
                                        }),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                    )
                                    IconButton(
                                        onClick = {
                                            addPlayer(newPlayerName)
                                            showNameDropdown = false
                                        },
                                        enabled = newPlayerName.trim().isNotEmpty()
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = "Add player",
                                            tint = if (newPlayerName.trim().isNotEmpty())
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                // Name autocomplete dropdown
                                val playerNameSet = players.map { it.name.lowercase() }.toSet()
                                val filteredNames = savedNames.filter {
                                    newPlayerName.isNotEmpty() &&
                                            it.contains(newPlayerName, ignoreCase = true) &&
                                            !it.equals(newPlayerName, ignoreCase = true) &&
                                            it.lowercase() !in playerNameSet
                                }
                                DropdownMenu(
                                    expanded = showNameDropdown && filteredNames.isNotEmpty(),
                                    onDismissRequest = { showNameDropdown = false }
                                ) {
                                    filteredNames.take(8).forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                newPlayerName = name
                                                addPlayer(name)
                                                showNameDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Player list
                        if (players.isNotEmpty()) {
                            item {
                                Text(
                                    "Players (${players.size})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            itemsIndexed(players.toList()) { index, player ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(playerColor(index))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            player.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { players.removeAt(index) }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Start button
                    Button(
                        onClick = {
                            saveGameName(context, gameName)
                            savedGames = loadSavedGames(context)
                            history.clear()
                            scoreInputs.clear()
                            phase = ScoringPhase.PLAYING
                        },
                        enabled = players.size >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Start Game")
                    }
                }

                // ===================== PLAYING =====================
                ScoringPhase.PLAYING -> {
                    // Game header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (gameName.isNotEmpty()) {
                                Text(gameName, style = MaterialTheme.typography.titleMedium)
                            }
                            // History count badge
                            if (history.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showFullHistory = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${history.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (history.isNotEmpty()) {
                            TextButton(onClick = {
                                val last = history.removeLastOrNull() ?: return@TextButton
                                val idx = players.indexOfFirst { it.id == last.playerId }
                                if (idx >= 0) {
                                    val p = players[idx]
                                    players[idx] = p.copy(score = p.score - last.amount)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo")
                            }
                        }
                    }

                    // Player scoring cards
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(players.toList()) { index, player ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    // Name + score + clock
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(playerColor(index))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            player.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${player.score}",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Per-player history clock icon
                                        val playerActionCount = history.count { it.playerId == player.id }
                                        if (playerActionCount > 0) {
                                            IconButton(
                                                onClick = { playerHistoryId = player.id },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.History,
                                                    contentDescription = "Player history",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Score buttons row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(-5, -1, 1, 5).forEach { amount ->
                                            val isNeg = amount < 0
                                            TextButton(
                                                onClick = { addScore(index, amount) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.textButtonColors(
                                                    containerColor = if (isNeg)
                                                        MaterialTheme.colorScheme.errorContainer.copy(
                                                            alpha = if (amount == -5) 1f else 0.5f
                                                        )
                                                    else
                                                        Color(0xFF2E7D32).copy(
                                                            alpha = if (amount == 5) 0.4f else 0.2f
                                                        )
                                                )
                                            ) {
                                                Text(
                                                    if (amount > 0) "+$amount" else "$amount",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isNeg) MaterialTheme.colorScheme.error
                                                    else Color(0xFF4CAF50)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Custom input
                                        OutlinedTextField(
                                            value = scoreInputs[player.id] ?: "",
                                            onValueChange = { v ->
                                                // Allow digits, plus, minus
                                                val filtered = v.filter { c -> c.isDigit() || c == '+' || c == '-' }
                                                scoreInputs[player.id] = filtered
                                            },
                                            modifier = Modifier.width(72.dp),
                                            placeholder = { Text("\u00b1", fontSize = 14.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(onDone = {
                                                val input = scoreInputs[player.id]?.trim() ?: ""
                                                val v = input.toIntOrNull()
                                                if (v != null && v != 0) {
                                                    addScore(index, v)
                                                    scoreInputs[player.id] = ""
                                                }
                                            })
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // End Game button
                    Button(
                        onClick = { showEndConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("End Game")
                    }

                    // End Game confirmation dialog
                    if (showEndConfirm) {
                        AlertDialog(
                            onDismissRequest = { showEndConfirm = false },
                            title = { Text("End Game?") },
                            text = { Text("Are you sure? You can go back to scoring if you hit it by mistake.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showEndConfirm = false
                                    phase = ScoringPhase.FINISHED
                                }) { Text("End Game") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEndConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }

                    // Full history bottom sheet
                    if (showFullHistory) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = { showFullHistory = false },
                            sheetState = sheetState
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    "Score History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                if (history.isEmpty()) {
                                    Text(
                                        "No score changes yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 24.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.height(400.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(history.reversed()) { action ->
                                            val pIndex = players.indexOfFirst { it.id == action.playerId }
                                            val color = if (pIndex >= 0) playerColor(pIndex)
                                            else MaterialTheme.colorScheme.onSurface
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    action.playerName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    if (action.amount > 0) "+${action.amount}" else "${action.amount}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (action.amount >= 0) Color(0xFF4CAF50)
                                                    else MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    formatTimestamp(action.timestamp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // Per-player history bottom sheet
                    if (playerHistoryId != null) {
                        val targetId = playerHistoryId!!
                        val pIndex = players.indexOfFirst { it.id == targetId }
                        val playerObj = players.getOrNull(pIndex)
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                        if (playerObj != null) {
                            ModalBottomSheet(
                                onDismissRequest = { playerHistoryId = null },
                                sheetState = sheetState
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(playerColor(pIndex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${playerObj.name} - History",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    val playerHistory = history.filter { it.playerId == targetId }
                                    if (playerHistory.isEmpty()) {
                                        Text(
                                            "No score changes yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 24.dp)
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.height(400.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Show in reverse chronological, with running total
                                            val reversed = playerHistory.reversed()
                                            itemsIndexed(reversed) { rIdx, action ->
                                                // Running total = sum of all actions up to this point
                                                // The action at reversed index rIdx corresponds to
                                                // original index (playerHistory.size - 1 - rIdx)
                                                val originalIdx = playerHistory.size - 1 - rIdx
                                                val runningTotal = playerHistory
                                                    .take(originalIdx + 1)
                                                    .sumOf { it.amount }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Amount
                                                    Text(
                                                        if (action.amount > 0) "+${action.amount}" else "${action.amount}",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (action.amount >= 0) Color(0xFF4CAF50)
                                                        else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.width(56.dp)
                                                    )
                                                    // Running total
                                                    Text(
                                                        "Total: $runningTotal",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    // Timestamp
                                                    Text(
                                                        formatTimestamp(action.timestamp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    // Delete button
                                                    IconButton(
                                                        onClick = {
                                                            // Reverse the score
                                                            val currentIdx = players.indexOfFirst { it.id == targetId }
                                                            if (currentIdx >= 0) {
                                                                val cp = players[currentIdx]
                                                                players[currentIdx] = cp.copy(score = cp.score - action.amount)
                                                            }
                                                            history.remove(action)
                                                            // Close sheet if no more history
                                                            if (history.none { it.playerId == targetId }) {
                                                                playerHistoryId = null
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove entry",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }

                // ===================== FINISHED =====================
                ScoringPhase.FINISHED -> {
                    val sorted = players.sortedWith(
                        if (highestWins) compareByDescending { it.score }
                        else compareBy { it.score }
                    )
                    val winner = sorted.firstOrNull()

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (gameName.isNotEmpty()) {
                            item {
                                Text(
                                    gameName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        winner?.let { w ->
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFFFFD700)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        w.name,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${w.score} points",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Winner!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        itemsIndexed(sorted) { index, player ->
                            val originalIndex = players.indexOf(player)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (index == 0)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "#${index + 1}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(30.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(playerColor(originalIndex.coerceAtLeast(0)))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        player.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${player.score}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Bottom action buttons
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { phase = ScoringPhase.PLAYING },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Scoring")
                        }
                        Button(
                            onClick = {
                                players.forEachIndexed { i, p -> players[i] = p.copy(score = 0) }
                                scoreInputs.clear()
                                history.clear()
                                phase = ScoringPhase.PLAYING
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Again (Same Players)")
                        }
                        OutlinedButton(
                            onClick = {
                                players.clear()
                                scoreInputs.clear()
                                gameName = ""
                                history.clear()
                                clearSession(context)
                                phase = ScoringPhase.SETUP
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Game")
                        }
                    }
                }
            }
        }
    }
}
