package com.sasquatsh.app.views.toolbox

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val fingerColors = listOf(
    Color.Red, Color.Blue, Color.Green, Color(0xFFFFA500), Color(0xFF800080)
)

private enum class PickerPhase { WAITING, COUNTDOWN, PULSING, SELECTED }

private data class TouchPoint(val id: Int, val x: Float, val y: Float)

@Composable
fun FirstPlayerPickerView(
    onBack: (() -> Unit)? = null
) {
    val touches = remember { mutableStateListOf<TouchPoint>() }
    var phase by remember { mutableStateOf(PickerPhase.WAITING) }
    var winnerIndex by remember { mutableIntStateOf(-1) }
    var countdown by remember { mutableIntStateOf(3) }
    var touchIdsAtStart by remember { mutableStateOf(setOf<Int>()) }
    val context = LocalContext.current

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "pulseScale"
    )

    // Countdown + selection logic
    LaunchedEffect(phase) {
        if (phase == PickerPhase.COUNTDOWN) {
            countdown = 3
            for (i in 3 downTo 1) {
                countdown = i
                val progress = (3 - i).toDouble() / 2.0 // 0.0, 0.5, 1.0
                PickerSoundEngine.playCountdownTick(progress)
                delay(1000)
                if (phase != PickerPhase.COUNTDOWN) return@LaunchedEffect
            }
            phase = PickerPhase.PULSING
            delay(2000)
            if (phase == PickerPhase.PULSING && touches.isNotEmpty()) {
                winnerIndex = (0 until touches.size).random()
                phase = PickerPhase.SELECTED
                PickerSoundEngine.playSelectChime()
                // Haptic feedback
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = context.getSystemService(VibratorManager::class.java)
                        vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        val v = context.getSystemService(Vibrator::class.java)
                        v?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Multi-touch capture via AndroidView
        AndroidView(
            factory = { ctx ->
                object : View(ctx) {
                    private val activeTouches = mutableMapOf<Int, TouchPoint>()

                    init {
                        isClickable = true
                        isFocusable = true
                    }

                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                val idx = event.actionIndex
                                val id = event.getPointerId(idx)
                                activeTouches[id] = TouchPoint(id, event.getX(idx), event.getY(idx))
                                PickerSoundEngine.playTap(activeTouches.size - 1)
                                updateTouches()
                            }
                            MotionEvent.ACTION_MOVE -> {
                                for (i in 0 until event.pointerCount) {
                                    val id = event.getPointerId(i)
                                    activeTouches[id] = TouchPoint(id, event.getX(i), event.getY(i))
                                }
                                updateTouches()
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                                val idx = event.actionIndex
                                val id = event.getPointerId(idx)
                                activeTouches.remove(id)
                                updateTouches()
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                activeTouches.clear()
                                updateTouches()
                            }
                        }
                        return true
                    }

                    private fun updateTouches() {
                        val sorted = activeTouches.values.sortedBy { it.id }
                        touches.clear()
                        touches.addAll(sorted)

                        val currentIds = sorted.map { it.id }.toSet()

                        when (phase) {
                            PickerPhase.WAITING -> {
                                if (sorted.size >= 2) {
                                    touchIdsAtStart = currentIds
                                    PickerSoundEngine.playTransition()
                                    phase = PickerPhase.COUNTDOWN
                                }
                            }
                            PickerPhase.COUNTDOWN, PickerPhase.PULSING -> {
                                if (currentIds != touchIdsAtStart) {
                                    if (sorted.size < 2) {
                                        phase = PickerPhase.WAITING
                                    } else {
                                        touchIdsAtStart = currentIds
                                        PickerSoundEngine.playTransition()
                                        phase = PickerPhase.COUNTDOWN
                                    }
                                }
                            }
                            PickerPhase.SELECTED -> {
                                // Tap to reset
                                if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                                    phase = PickerPhase.WAITING
                                    winnerIndex = -1
                                    activeTouches.clear()
                                    touches.clear()
                                }
                            }
                        }
                    }

                    // Need access to event in updateTouches for SELECTED tap detection
                    private var event: MotionEvent? = null

                    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                        event = ev
                        val result = super.dispatchTouchEvent(ev)
                        event = null
                        return result
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw circles on canvas overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            touches.forEachIndexed { index, touch ->
                val color = fingerColors[index % fingerColors.size]
                val isWinner = phase == PickerPhase.SELECTED && index == winnerIndex
                val baseRadius = if (isWinner) 180f else 110f
                val radius = if (phase == PickerPhase.PULSING) baseRadius * pulseScale else baseRadius
                val alpha = when {
                    isWinner -> 1f
                    phase == PickerPhase.SELECTED -> 0.2f
                    else -> 0.6f
                }

                // Glow for winner
                if (isWinner) {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = radius + 40f,
                        center = Offset(touch.x, touch.y)
                    )
                }

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(touch.x, touch.y)
                )

                // Crown + FIRST! text for winner
                if (isWinner) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 60f
                            isFakeBoldText = true
                        }
                        drawText("👑", touch.x, touch.y - 20f, paint)
                        paint.textSize = 36f
                        drawText("FIRST!", touch.x, touch.y + 40f, paint)
                    }
                }
            }
        }

        // Header bar
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Finger Picker",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Instructions / Countdown overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (phase) {
                PickerPhase.WAITING -> {
                    if (touches.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "🖐️",
                                fontSize = 60.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Everyone place a finger\non the screen",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 30.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Supports up to 5 players",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                PickerPhase.COUNTDOWN -> {
                    Text(
                        "$countdown",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }
                PickerPhase.PULSING -> {
                    // No text during pulsing — just the circles animate
                }
                PickerPhase.SELECTED -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 60.dp)
                    ) {
                        Text(
                            "Tap anywhere to reset",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
