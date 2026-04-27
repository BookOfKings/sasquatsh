package com.sasquatsh.app.views.toolbox

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private data class ScoringPlayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var score: Int = 0
)

private data class ScoreAction(
    val id: String = UUID.randomUUID().toString(),
    val playerId: String,
    val playerName: String,
    val amount: Int
)

private enum class ScoringPhase { SETUP, PLAYING, FINISHED }

private val playerColors = listOf(
    Color.Red, Color.Blue, Color.Green, Color(0xFFFFA500),
    Color(0xFF800080), Color.Cyan, Color(0xFFFF69B4),
    Color.Yellow, Color(0xFF00CED1), Color(0xFF4B0082)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreKeeperView() {
    var phase by rememberSaveable { mutableStateOf(ScoringPhase.SETUP) }
    val players = remember { mutableStateListOf<ScoringPlayer>() }
    var gameName by rememberSaveable { mutableStateOf("") }
    var newPlayerName by rememberSaveable { mutableStateOf("") }
    var highestWins by rememberSaveable { mutableStateOf(true) }
    val history = remember { mutableStateListOf<ScoreAction>() }
    var showEndConfirm by remember { mutableStateOf(false) }
    val scoreInputs = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Score Keeper") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (phase) {
                ScoringPhase.SETUP -> {
                    // Setup view
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text("Game Name", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = gameName,
                                onValueChange = { gameName = it },
                                placeholder = { Text("e.g. Flip 7, Catan, Uno...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

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

                        item {
                            Text("Add Players", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerName,
                                    onValueChange = { newPlayerName = it },
                                    placeholder = { Text("Player name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (newPlayerName.trim().isNotEmpty()) {
                                            players.add(ScoringPlayer(name = newPlayerName.trim()))
                                            newPlayerName = ""
                                        }
                                    }),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                )
                                IconButton(
                                    onClick = {
                                        if (newPlayerName.trim().isNotEmpty()) {
                                            players.add(ScoringPlayer(name = newPlayerName.trim()))
                                            newPlayerName = ""
                                        }
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
                        }

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
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(playerColors[index % playerColors.size])
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
                            history.clear()
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

                ScoringPhase.PLAYING -> {
                    // Game header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (gameName.isNotEmpty()) {
                            Text(gameName, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (history.isNotEmpty()) {
                            IconButton(onClick = {
                                val last = history.removeLastOrNull() ?: return@IconButton
                                val idx = players.indexOfFirst { it.id == last.playerId }
                                if (idx >= 0) {
                                    val p = players[idx]
                                    players[idx] = p.copy(score = p.score - last.amount)
                                }
                            }) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo")
                            }
                        }
                    }

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
                                    // Name + score
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(playerColors[index % playerColors.size])
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
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Score buttons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(-5, -1, 1, 5).forEach { amount ->
                                            val isNeg = amount < 0
                                            TextButton(
                                                onClick = {
                                                    val p = players[index]
                                                    players[index] = p.copy(score = p.score + amount)
                                                    history.add(
                                                        ScoreAction(
                                                            playerId = p.id,
                                                            playerName = p.name,
                                                            amount = amount
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                    containerColor = if (isNeg)
                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = if (amount == -5) 1f else 0.5f)
                                                    else
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (amount == 5) 1f else 0.5f)
                                                )
                                            ) {
                                                Text(
                                                    if (amount > 0) "+$amount" else "$amount",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isNeg) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Custom input
                                        OutlinedTextField(
                                            value = scoreInputs[player.id] ?: "",
                                            onValueChange = { scoreInputs[player.id] = it },
                                            modifier = Modifier.width(56.dp),
                                            placeholder = { Text("\u00b1") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            keyboardActions = KeyboardActions(onDone = {
                                                val v = scoreInputs[player.id]?.toIntOrNull()
                                                if (v != null) {
                                                    val p = players[index]
                                                    players[index] = p.copy(score = p.score + v)
                                                    history.add(
                                                        ScoreAction(
                                                            playerId = p.id,
                                                            playerName = p.name,
                                                            amount = v
                                                        )
                                                    )
                                                    scoreInputs[player.id] = ""
                                                }
                                            })
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showEndConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("End Game")
                    }

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
                }

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
                                        modifier = Modifier.size(40.dp),
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
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(playerColors[originalIndex.coerceAtLeast(0) % playerColors.size])
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
