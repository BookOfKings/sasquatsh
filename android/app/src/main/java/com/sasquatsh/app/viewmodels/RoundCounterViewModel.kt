package com.sasquatsh.app.viewmodels

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.RoundCounterEvent
import com.sasquatsh.app.models.RoundCounterState
import com.sasquatsh.app.services.RoundCounterPersistence
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class RoundCounterUiState(
    val state: RoundCounterState = RoundCounterState.new(),
    val history: List<RoundCounterEvent> = emptyList(),
    val showResetConfirmation: Boolean = false,
    val showEditRound: Boolean = false,
    val editRoundText: String = "",
    val isTimerRunning: Boolean = false,
    val roundElapsed: Long = 0L, // millis
    val sessionElapsed: Long = 0L, // millis
    val isCountdownMode: Boolean = false,
    val roundDurationSeconds: Int = 120,
    val maxRounds: Int = 10,
    val countdownRemaining: Long = 0L, // millis
    val reachedMaxRound: Boolean = false
) {
    val formattedRoundTime: String get() = formatTime(roundElapsed)
    val formattedSessionTime: String get() = formatTime(sessionElapsed)
    val formattedCountdown: String get() = formatTime(maxOf(0, countdownRemaining))

    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

@HiltViewModel
class RoundCounterViewModel @Inject constructor(
    private val persistence: RoundCounterPersistence,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundCounterUiState())
    val uiState: StateFlow<RoundCounterUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var roundStartTime: Long? = null
    private var sessionStartTime: Long? = null
    private var accumulatedRoundTime: Long = 0L
    private var accumulatedSessionTime: Long = 0L

    init {
        loadSaved()
    }

    private fun loadSaved() {
        viewModelScope.launch {
            val saved = persistence.load()
            if (saved != null) {
                _uiState.update { it.copy(state = saved) }
            }
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isTimerRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (_uiState.value.isTimerRunning) return
        _uiState.update { it.copy(isTimerRunning = true, reachedMaxRound = false) }
        roundStartTime = System.currentTimeMillis()
        if (sessionStartTime == null) {
            sessionStartTime = System.currentTimeMillis()
        }
        if (_uiState.value.isCountdownMode) {
            _uiState.update {
                it.copy(countdownRemaining = (_uiState.value.roundDurationSeconds * 1000L) - accumulatedRoundTime)
            }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                if (!_uiState.value.isTimerRunning) continue

                val now = System.currentTimeMillis()
                roundStartTime?.let { start ->
                    val elapsed = accumulatedRoundTime + (now - start)
                    _uiState.update { it.copy(roundElapsed = elapsed) }
                }
                sessionStartTime?.let { start ->
                    val elapsed = accumulatedSessionTime + (now - start)
                    _uiState.update { it.copy(sessionElapsed = elapsed) }
                }

                if (_uiState.value.isCountdownMode) {
                    val remaining = (_uiState.value.roundDurationSeconds * 1000L) - _uiState.value.roundElapsed
                    _uiState.update { it.copy(countdownRemaining = remaining) }
                    if (remaining <= 0) {
                        autoAdvanceRound()
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isTimerRunning = false) }
        roundStartTime?.let { start ->
            accumulatedRoundTime += System.currentTimeMillis() - start
        }
        sessionStartTime?.let { start ->
            accumulatedSessionTime += System.currentTimeMillis() - start
        }
        roundStartTime = null
        sessionStartTime = null
    }

    private fun autoAdvanceRound() {
        val currentState = _uiState.value.state
        if (currentState.roundNumber >= _uiState.value.maxRounds) {
            _uiState.update { it.copy(reachedMaxRound = true) }
            pauseTimer()
            playMaxRoundSound()
            haptic(HapticStyle.HEAVY)
            logEvent("maxRoundReached")
            return
        }

        val updatedState = currentState.copy(
            roundNumber = currentState.roundNumber + 1,
            updatedAt = Date().toString()
        )
        _uiState.update { it.copy(state = updatedState) }
        logEvent("autoIncrement")
        resetRoundTimer()
        _uiState.update { it.copy(countdownRemaining = _uiState.value.roundDurationSeconds * 1000L) }
        playRoundAdvanceSound()
        haptic(HapticStyle.MEDIUM)
        save()
    }

    private fun resetRoundTimer() {
        if (_uiState.value.isTimerRunning) {
            accumulatedRoundTime = 0
            roundStartTime = System.currentTimeMillis()
            _uiState.update { it.copy(roundElapsed = 0) }
        } else {
            accumulatedRoundTime = 0
            roundStartTime = null
            _uiState.update { it.copy(roundElapsed = 0) }
        }
    }

    private fun resetAllTimers() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isTimerRunning = false,
                roundElapsed = 0,
                sessionElapsed = 0,
                countdownRemaining = 0,
                reachedMaxRound = false
            )
        }
        accumulatedRoundTime = 0
        accumulatedSessionTime = 0
        roundStartTime = null
        sessionStartTime = null
    }

    // Actions

    fun increment() {
        _uiState.update {
            val updated = it.state.copy(
                roundNumber = it.state.roundNumber + 1,
                updatedAt = Date().toString()
            )
            it.copy(state = updated)
        }
        logEvent("incrementRound")
        resetRoundTimer()
        if (_uiState.value.isCountdownMode) {
            _uiState.update { it.copy(countdownRemaining = it.roundDurationSeconds * 1000L) }
        }
        haptic(HapticStyle.LIGHT)
        save()
    }

    fun decrement() {
        val currentState = _uiState.value.state
        if (currentState.roundNumber <= currentState.minimumRound) return
        _uiState.update {
            val updated = it.state.copy(
                roundNumber = it.state.roundNumber - 1,
                updatedAt = Date().toString()
            )
            it.copy(state = updated)
        }
        logEvent("decrementRound")
        resetRoundTimer()
        if (_uiState.value.isCountdownMode) {
            _uiState.update { it.copy(countdownRemaining = it.roundDurationSeconds * 1000L) }
        }
        haptic(HapticStyle.LIGHT)
        save()
    }

    fun setRound(round: Int) {
        val clamped = maxOf(round, _uiState.value.state.minimumRound)
        _uiState.update {
            val updated = it.state.copy(roundNumber = clamped, updatedAt = Date().toString())
            it.copy(state = updated)
        }
        logEvent("setRound")
        resetRoundTimer()
        if (_uiState.value.isCountdownMode) {
            _uiState.update { it.copy(countdownRemaining = it.roundDurationSeconds * 1000L) }
        }
        save()
    }

    fun reset() {
        _uiState.update {
            val updated = it.state.copy(
                roundNumber = it.state.startingRound,
                turnNumber = null,
                phaseKey = null,
                activePlayerId = null,
                updatedAt = Date().toString()
            )
            it.copy(state = updated)
        }
        logEvent("resetRound")
        resetAllTimers()
        haptic(HapticStyle.MEDIUM)
        save()
    }

    fun updateStartingRound(value: Int) {
        _uiState.update {
            val clamped = maxOf(value, 0)
            val updated = it.state.copy(startingRound = clamped, minimumRound = clamped)
            it.copy(state = updated)
        }
        save()
    }

    fun newSession(startingRound: Int = 1) {
        _uiState.update {
            it.copy(
                state = RoundCounterState.new(startingRound),
                history = emptyList()
            )
        }
        resetAllTimers()
        save()
    }

    fun updateCountdownMode(enabled: Boolean) {
        _uiState.update { it.copy(isCountdownMode = enabled) }
    }

    fun updateRoundDurationSeconds(seconds: Int) {
        _uiState.update { it.copy(roundDurationSeconds = seconds) }
    }

    fun updateMaxRounds(max: Int) {
        _uiState.update { it.copy(maxRounds = max) }
    }

    fun beginEditRound() {
        _uiState.update {
            it.copy(editRoundText = it.state.roundNumber.toString(), showEditRound = true)
        }
    }

    fun updateEditRoundText(text: String) {
        _uiState.update { it.copy(editRoundText = text) }
    }

    fun commitEditRound() {
        val value = _uiState.value.editRoundText.toIntOrNull()
        if (value != null) {
            setRound(value)
        }
        _uiState.update { it.copy(showEditRound = false) }
    }

    fun showResetConfirmation(show: Boolean) {
        _uiState.update { it.copy(showResetConfirmation = show) }
    }

    // Private helpers

    private fun logEvent(type: String) {
        val state = _uiState.value.state
        val event = RoundCounterEvent(
            id = UUID.randomUUID().toString(),
            sessionId = state.sessionId,
            roundNumber = state.roundNumber,
            turnNumber = state.turnNumber,
            phaseKey = state.phaseKey,
            eventType = type,
            createdAt = Date().toString()
        )
        _uiState.update { it.copy(history = it.history + event) }
    }

    private fun save() {
        viewModelScope.launch {
            persistence.save(_uiState.value.state)
        }
    }

    private enum class HapticStyle { LIGHT, MEDIUM, HEAVY }

    private fun haptic(style: HapticStyle) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val amplitude = when (style) {
                HapticStyle.LIGHT -> 50
                HapticStyle.MEDIUM -> 128
                HapticStyle.HEAVY -> 255
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(30, amplitude))
        } catch (_: Exception) {
            // Haptics not available
        }
    }

    private fun playRoundAdvanceSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            viewModelScope.launch {
                delay(150)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                delay(200)
                toneGen.release()
            }
        } catch (_: Exception) {}
    }

    private fun playMaxRoundSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            viewModelScope.launch {
                delay(200)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
                delay(200)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                delay(400)
                toneGen.release()
            }
        } catch (_: Exception) {}
    }
}
