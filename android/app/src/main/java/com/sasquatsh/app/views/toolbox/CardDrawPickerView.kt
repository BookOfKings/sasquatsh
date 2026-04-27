package com.sasquatsh.app.views.toolbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Card Models ---

private enum class Suit(val symbol: String, val color: Color) {
    SPADES("\u2660", Color.Black),
    HEARTS("\u2665", Color.Red),
    DIAMONDS("\u2666", Color.Red),
    CLUBS("\u2663", Color.Black)
}

private enum class Rank(val display: String, val value: Int) {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14)
}

private data class PlayingCard(val rank: Rank, val suit: Suit) {
    val label: String get() = "${rank.display}${suit.symbol}"
}

private data class PlayerDraw(
    val playerNumber: Int,
    val card: PlayingCard,
    val isTiebreaker: Boolean = false
)

private fun buildDeck(): MutableList<PlayingCard> {
    val deck = mutableListOf<PlayingCard>()
    for (suit in Suit.entries) {
        for (rank in Rank.entries) {
            deck.add(PlayingCard(rank, suit))
        }
    }
    deck.shuffle()
    return deck
}

// --- Game Phases ---

private enum class GamePhase { SETUP, DRAWING, RESULT }

// --- Composables ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDrawPickerView(onBack: () -> Unit = {}) {
    var phase by remember { mutableStateOf(GamePhase.SETUP) }
    var playerCount by remember { mutableIntStateOf(4) }
    var deck by remember { mutableStateOf(buildDeck()) }
    var currentPlayerIndex by remember { mutableIntStateOf(0) }
    var cardRevealed by remember { mutableStateOf(false) }
    val draws = remember { mutableStateListOf<PlayerDraw>() }
    var tiebreakerRound by remember { mutableIntStateOf(0) }
    var tiebreakerPlayers by remember { mutableStateOf(listOf<Int>()) }
    var winner by remember { mutableStateOf<PlayerDraw?>(null) }

    // Which players are drawing in the current round
    val activePlayers: List<Int> = if (tiebreakerRound > 0) {
        tiebreakerPlayers
    } else {
        (1..playerCount).toList()
    }

    // Draws for the current round only
    val currentRoundDraws = draws.filter {
        if (tiebreakerRound > 0) it.isTiebreaker else !it.isTiebreaker
    }

    fun startGame() {
        deck = buildDeck()
        draws.clear()
        currentPlayerIndex = 0
        cardRevealed = false
        tiebreakerRound = 0
        tiebreakerPlayers = emptyList()
        winner = null
        phase = GamePhase.DRAWING
    }

    fun drawCard() {
        if (cardRevealed) return
        val card = deck.removeFirst()
        val playerNum = activePlayers[currentPlayerIndex]
        draws.add(PlayerDraw(playerNum, card, isTiebreaker = tiebreakerRound > 0))
        cardRevealed = true
    }

    fun advanceToNext() {
        cardRevealed = false
        if (currentPlayerIndex + 1 < activePlayers.size) {
            currentPlayerIndex++
        } else {
            // All players in this round have drawn -- check for winner
            val roundDraws = if (tiebreakerRound > 0) {
                draws.filter { it.isTiebreaker && it.playerNumber in tiebreakerPlayers }
                    .groupBy { it.playerNumber }
                    .mapValues { it.value.last() }
                    .values.toList()
            } else {
                draws.filter { !it.isTiebreaker }
            }

            val maxVal = roundDraws.maxOf { it.card.rank.value }
            val topPlayers = roundDraws.filter { it.card.rank.value == maxVal }

            if (topPlayers.size == 1) {
                winner = topPlayers.first()
                phase = GamePhase.RESULT
            } else {
                // Tiebreaker needed
                tiebreakerRound++
                tiebreakerPlayers = topPlayers.map { it.playerNumber }
                currentPlayerIndex = 0
            }
        }
    }

    fun resetGame() {
        phase = GamePhase.SETUP
        draws.clear()
        currentPlayerIndex = 0
        cardRevealed = false
        tiebreakerRound = 0
        tiebreakerPlayers = emptyList()
        winner = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("High Card Draw") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (phase) {
                GamePhase.SETUP -> SetupPhase(
                    playerCount = playerCount,
                    onPlayerCountChange = { playerCount = it },
                    onDeal = { startGame() }
                )

                GamePhase.DRAWING -> DrawingPhase(
                    activePlayers = activePlayers,
                    currentPlayerIndex = currentPlayerIndex,
                    cardRevealed = cardRevealed,
                    currentRoundDraws = currentRoundDraws,
                    allDraws = draws,
                    tiebreakerRound = tiebreakerRound,
                    onTapCard = { drawCard() },
                    onNext = { advanceToNext() },
                    isLastPlayer = currentPlayerIndex + 1 >= activePlayers.size
                )

                GamePhase.RESULT -> ResultPhase(
                    winner = winner,
                    allDraws = draws,
                    onPlayAgain = { resetGame() },
                    onSettings = { resetGame() }
                )
            }
        }
    }
}

// --- Setup Phase ---

@Composable
private fun SetupPhase(
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    onDeal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "\u2660",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "How many players?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player count stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { if (playerCount > 2) onPlayerCountChange(playerCount - 1) },
                enabled = playerCount > 2,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                "$playerCount",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = { if (playerCount < 20) onPlayerCountChange(playerCount + 1) },
                enabled = playerCount < 20,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onDeal,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF34C759)
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp)
        ) {
            Text(
                "Deal Cards",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// --- Drawing Phase ---

@Composable
private fun DrawingPhase(
    activePlayers: List<Int>,
    currentPlayerIndex: Int,
    cardRevealed: Boolean,
    currentRoundDraws: List<PlayerDraw>,
    allDraws: List<PlayerDraw>,
    tiebreakerRound: Int,
    onTapCard: () -> Unit,
    onNext: () -> Unit,
    isLastPlayer: Boolean
) {
    val currentPlayer = activePlayers[currentPlayerIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tiebreaker banner
        if (tiebreakerRound > 0) {
            Text(
                "TIEBREAKER! (Round $tiebreakerRound)",
                color = Color(0xFFFF9500),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Players ${activePlayers.joinToString(", ") { "#$it" }} are tied!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Previously drawn mini cards (horizontal scroll)
        if (currentRoundDraws.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                currentRoundDraws.forEach { draw ->
                    MiniCard(draw)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Player label
        Text(
            "Player $currentPlayer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tap instruction
        if (!cardRevealed) {
            Text(
                "Tap card to draw",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Card area
        Box(
            modifier = Modifier
                .clickable(enabled = !cardRevealed) { onTapCard() },
            contentAlignment = Alignment.Center
        ) {
            val currentDraw = if (cardRevealed) {
                allDraws.lastOrNull { it.playerNumber == currentPlayer }
            } else null

            if (cardRevealed && currentDraw != null) {
                CardFace(currentDraw.card)
            } else {
                CardBack()
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom button
        if (cardRevealed) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    if (isLastPlayer) "See Results" else "Next: Player ${activePlayers.getOrNull(currentPlayerIndex + 1) ?: ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// --- Result Phase ---

@Composable
private fun ResultPhase(
    winner: PlayerDraw?,
    allDraws: List<PlayerDraw>,
    onPlayAgain: () -> Unit,
    onSettings: () -> Unit
) {
    // Sort by card value descending for display
    val sortedDraws = allDraws.sortedByDescending { it.card.rank.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Winner announcement
        if (winner != null) {
            Text(
                "\uD83D\uDC51",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Player ${winner.playerNumber} Wins!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Drew ${winner.card.rank.display}${winner.card.suit.symbol}",
                fontSize = 20.sp,
                color = winner.card.suit.color.let {
                    if (it == Color.Black) MaterialTheme.colorScheme.onSurface else it
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // All results list
        Text(
            "Results",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(sortedDraws) { index, draw ->
                val isWinner = draw.playerNumber == winner?.playerNumber
                        && draw.card == winner?.card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isWinner) Color(0xFF34C759).copy(alpha = 0.1f)
                            else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position
                    Text(
                        "${index + 1}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.width(32.dp)
                    )

                    // Player label
                    Text(
                        if (isWinner) "\uD83D\uDC51 Player ${draw.playerNumber}"
                        else "Player ${draw.playerNumber}",
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Tiebreaker badge
                    if (draw.isTiebreaker) {
                        Text(
                            "TB",
                            fontSize = 10.sp,
                            color = Color(0xFFFF9500),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    Color(0xFFFF9500).copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Mini card
                    MiniCard(draw)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    "Play Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Card Face ---

@Composable
private fun CardFace(card: PlayingCard, width: Dp = 160.dp, height: Dp = 220.dp) {
    val suitColor = card.suit.color.let {
        if (it == Color.Black) Color(0xFF1A1A1A) else it
    }

    Box(
        modifier = Modifier
            .size(width, height)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
    ) {
        // Top-left rank + suit
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                card.rank.display,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = suitColor,
                lineHeight = 22.sp
            )
            Text(
                card.suit.symbol,
                fontSize = 16.sp,
                color = suitColor,
                lineHeight = 18.sp
            )
        }

        // Center suit symbol
        Text(
            card.suit.symbol,
            fontSize = 56.sp,
            color = suitColor,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom-right rank + suit (rotated 180)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 8.dp)
                .graphicsLayer { rotationZ = 180f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                card.rank.display,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = suitColor,
                lineHeight = 22.sp
            )
            Text(
                card.suit.symbol,
                fontSize = 16.sp,
                color = suitColor,
                lineHeight = 18.sp
            )
        }
    }
}

// --- Card Back ---

private val CardBackBlue = Color(0xFF1A3399)

@Composable
private fun CardBack(width: Dp = 160.dp, height: Dp = 220.dp) {
    Box(
        modifier = Modifier
            .size(width, height)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(CardBackBlue, RoundedCornerShape(12.dp))
            .drawBehind {
                drawDiamondPattern(this)
            }
    ) {
        // White border inset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        )
    }
}

private fun drawDiamondPattern(drawScope: DrawScope) {
    val diamondSize = 10f
    val spacingX = 20f
    val spacingY = 20f
    val color = Color.White.copy(alpha = 0.15f)

    var y = spacingY / 2
    var rowIndex = 0
    while (y < drawScope.size.height) {
        val offsetX = if (rowIndex % 2 == 0) 0f else spacingX / 2
        var x = offsetX + spacingX / 2
        while (x < drawScope.size.width) {
            val path = Path().apply {
                moveTo(x, y - diamondSize)
                lineTo(x + diamondSize, y)
                lineTo(x, y + diamondSize)
                lineTo(x - diamondSize, y)
                close()
            }
            drawScope.drawPath(path, color)
            x += spacingX
        }
        y += spacingY
        rowIndex++
    }
}

// --- Mini Card ---

@Composable
private fun MiniCard(draw: PlayerDraw) {
    val suitColor = draw.card.suit.color.let {
        if (it == Color.Black) Color(0xFF1A1A1A) else it
    }

    Column(
        modifier = Modifier
            .size(36.dp, 48.dp)
            .shadow(2.dp, RoundedCornerShape(4.dp))
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0xFFDDDDDD), RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            draw.card.rank.display,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = suitColor,
            lineHeight = 14.sp
        )
        Text(
            draw.card.suit.symbol,
            fontSize = 10.sp,
            color = suitColor,
            lineHeight = 12.sp
        )
    }
}
