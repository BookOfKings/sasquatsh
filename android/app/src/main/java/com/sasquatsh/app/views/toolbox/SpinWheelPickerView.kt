package com.sasquatsh.app.views.toolbox

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// Player colors matching iOS version (up to 20)
private val playerColors = listOf(
    Color(0xFFFF3B30), // Red
    Color(0xFF007AFF), // Blue
    Color(0xFF34C759), // Green
    Color(0xFFFF9500), // Orange
    Color(0xFFAF52DE), // Purple
    Color(0xFF5AC8FA), // Cyan
    Color(0xFFFF2D55), // Pink
    Color(0xFFFFCC00), // Yellow
    Color(0xFF00C7BE), // Mint
    Color(0xFF5856D6), // Indigo
    Color(0xFF30B0C7), // Teal
    Color(0xFFA2845E), // Brown
    Color(0xFFFF6482), // Coral-pink
    Color(0xFF64D2FF), // Light cyan
    Color(0xFFBF5AF2), // Violet
    Color(0xFFFFD60A), // Bright yellow
    Color(0xFF32D74B), // Bright green
    Color(0xFFFF453A), // Bright red
    Color(0xFF0A84FF), // Bright blue
    Color(0xFFAC8E68), // Tan
)

private enum class WheelPhase { SETUP, SPINNING, RESULT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpinWheelPickerView(onBack: () -> Unit = {}) {
    var playerCount by remember { mutableIntStateOf(4) }
    var phase by remember { mutableStateOf(WheelPhase.SETUP) }
    var winnerIndex by remember { mutableIntStateOf(-1) }
    val rotationAngle = remember { Animatable(0f) }
    var lastTickSegment by remember { mutableIntStateOf(-1) }
    var currentRotation by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ToneGenerator for tick sounds
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }

    // Track rotation to play tick sounds during spin
    LaunchedEffect(currentRotation, phase) {
        if (phase == WheelPhase.SPINNING) {
            val segmentAngle = 360f / playerCount
            val normalizedAngle = ((currentRotation % 360f) + 360f) % 360f
            val currentSegment = floor(normalizedAngle / segmentAngle).toInt()
            if (currentSegment != lastTickSegment) {
                lastTickSegment = currentSegment
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                } catch (_: Exception) {}
            }
        }
    }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun spinWheel() {
        phase = WheelPhase.SPINNING
        lastTickSegment = -1
        scope.launch {
            // Spin 5-10 full rotations plus a random offset
            val extraRotations = (5..10).random() * 360f
            val randomOffset = (0 until 360).random().toFloat()
            val totalSpin = extraRotations + randomOffset
            val startAngle = rotationAngle.value

            rotationAngle.animateTo(
                targetValue = startAngle + totalSpin,
                animationSpec = tween(
                    durationMillis = 7000,
                    easing = { t ->
                        // Dramatic deceleration: cubic ease-out with extra slowdown at end
                        val p = 1f - t
                        1f - (p * p * p * p)
                    }
                )
            ) {
                currentRotation = this.value
            }

            // Determine winner from final angle
            val finalAngle = rotationAngle.value % 360f
            // The pointer is at top (270 degrees in standard math, but we rotate clockwise)
            // Normalize: segment 0 starts at top-center going clockwise
            val segmentAngle = 360f / playerCount
            // The pointer is at top. Wheel rotates clockwise.
            // At angle 0, segment 0 is at right (3 o'clock), so pointer (top/12 o'clock) points to segment at 90 degrees offset
            val pointerAngle = (90f - finalAngle % 360f + 360f) % 360f
            val winner = (floor(pointerAngle / segmentAngle).toInt()) % playerCount

            winnerIndex = winner
            phase = WheelPhase.RESULT
            triggerHaptic()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spin the Wheel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (phase == WheelPhase.RESULT) {
                        IconButton(onClick = {
                            phase = WheelPhase.SETUP
                            winnerIndex = -1
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (phase) {
                WheelPhase.SETUP -> SetupScreen(
                    playerCount = playerCount,
                    onPlayerCountChange = { playerCount = it },
                    onSpin = { spinWheel() }
                )

                WheelPhase.SPINNING -> SpinningScreen(
                    playerCount = playerCount,
                    rotationAngle = currentRotation
                )

                WheelPhase.RESULT -> ResultScreen(
                    playerCount = playerCount,
                    winnerIndex = winnerIndex,
                    rotationAngle = currentRotation,
                    onSpinAgain = { spinWheel() },
                    onSetup = {
                        phase = WheelPhase.SETUP
                        winnerIndex = -1
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupScreen(
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    onSpin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Wheel icon
        Text(
            "\uD83C\uDFB0",
            fontSize = 72.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "How many players?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player count controls: - [count] +
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
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (playerCount > 2) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                "$playerCount",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(32.dp))

            IconButton(
                onClick = { if (playerCount < 20) onPlayerCountChange(playerCount + 1) },
                enabled = playerCount < 20,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (playerCount < 20) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Colored dots with labels
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until playerCount) {
                PlayerDot(index = i)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Spin button
        Button(
            onClick = onSpin,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF34C759)
            )
        ) {
            Text(
                "Spin the Wheel!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PlayerDot(index: Int) {
    val color = playerColors[index % playerColors.size]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "P${index + 1}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpinningScreen(
    playerCount: Int,
    rotationAngle: Float
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WheelCanvas(
            playerCount = playerCount,
            rotationAngle = rotationAngle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1f)
        )
    }
}

@Composable
private fun ResultScreen(
    playerCount: Int,
    winnerIndex: Int,
    rotationAngle: Float,
    onSpinAgain: () -> Unit,
    onSetup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Wheel (slightly smaller to fit result below)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            WheelCanvas(
                playerCount = playerCount,
                rotationAngle = rotationAngle,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        // Winner announcement
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Colored badge
            val winnerColor = playerColors[winnerIndex % playerColors.size]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(winnerColor, CircleShape)
            ) {
                Text(
                    "P${winnerIndex + 1}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Player ${winnerIndex + 1} goes first!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Spin Again button
            Button(
                onClick = onSpinAgain,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Spin Again",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WheelCanvas(
    playerCount: Int,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY) * 0.88f
        val segmentAngle = 360f / playerCount
        val pegRadius = radius + 12f
        val pegCount = playerCount * 2 // Two pegs per segment for more ticks

        // Draw the wheel (rotated)
        rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
            // Draw segments
            for (i in 0 until playerCount) {
                val startAngle = i * segmentAngle - 90f // Start from top
                val color = playerColors[i % playerColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = segmentAngle,
                    useCenter = true,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            // Draw white divider lines between segments
            for (i in 0 until playerCount) {
                val angle = Math.toRadians((i * segmentAngle - 90f).toDouble())
                val endX = centerX + radius * cos(angle).toFloat()
                val endY = centerY + radius * sin(angle).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f
                )
            }

            // Draw player labels inside segments
            drawPlayerLabels(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                playerCount = playerCount,
                segmentAngle = segmentAngle
            )

            // Draw pegs around the edge
            for (i in 0 until pegCount) {
                val angle = Math.toRadians((i * (360f / pegCount) - 90f).toDouble())
                val pegX = centerX + pegRadius * cos(angle).toFloat()
                val pegY = centerY + pegRadius * sin(angle).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(pegX, pegY)
                )
            }

            // Outer ring
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )
        }

        // Center dot (not rotated)
        drawCircle(
            color = Color(0xFF1C1C1E),
            radius = 18f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = 18f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Triangle pointer at top (not rotated)
        val pointerPath = Path().apply {
            val pointerTop = centerY - radius - 28f
            val pointerBottom = centerY - radius + 16f
            val pointerHalfWidth = 14f
            moveTo(centerX, pointerBottom)
            lineTo(centerX - pointerHalfWidth, pointerTop)
            lineTo(centerX + pointerHalfWidth, pointerTop)
            close()
        }
        drawPath(pointerPath, color = Color(0xFFFF3B30))
        drawPath(pointerPath, color = Color.Black.copy(alpha = 0.2f), style = Stroke(width = 1.5f))
    }
}

private fun DrawScope.drawPlayerLabels(
    centerX: Float,
    centerY: Float,
    radius: Float,
    playerCount: Int,
    segmentAngle: Float
) {
    val labelRadius = radius * 0.62f
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, android.graphics.Color.argb(100, 0, 0, 0))
    }

    // Scale text size based on player count
    paint.textSize = when {
        playerCount <= 4 -> 42f
        playerCount <= 6 -> 36f
        playerCount <= 10 -> 28f
        playerCount <= 14 -> 22f
        else -> 18f
    }

    for (i in 0 until playerCount) {
        val midAngle = Math.toRadians(((i * segmentAngle) + (segmentAngle / 2f) - 90f).toDouble())
        val labelX = centerX + labelRadius * cos(midAngle).toFloat()
        val labelY = centerY + labelRadius * sin(midAngle).toFloat()

        drawContext.canvas.nativeCanvas.drawText(
            "P${i + 1}",
            labelX,
            labelY + paint.textSize / 3f, // Vertical centering adjustment
            paint
        )
    }
}
