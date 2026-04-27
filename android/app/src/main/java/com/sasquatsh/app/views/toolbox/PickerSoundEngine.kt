package com.sasquatsh.app.views.toolbox

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Bell-based sound engine for the First Player Picker.
 *
 * Pre-generates PCM buffers for tap, transition, countdown, and select-chime
 * sounds, then plays them via [AudioTrack] on a background thread.
 *
 * Thread-safe: every public method can be called from any thread.
 */
object PickerSoundEngine {

    // ── constants ───────────────────────────────────────────────────────
    private const val SAMPLE_RATE = 44100
    private const val TWO_PI = 2.0 * PI

    /** C-major scale frequencies used for tap and countdown pitch mapping. */
    private val C_MAJOR_SCALE = doubleArrayOf(
        261.63, 293.66, 329.63, 349.23,
        392.00, 440.00, 493.88, 523.25
    )

    // ── pre-generated buffers ───────────────────────────────────────────
    private val tapBuffers: Array<ShortArray>
    private val transitionBuffer: ShortArray
    private val countdownBuffers: Array<ShortArray>
    private val selectChimeBuffer: ShortArray

    // ── audio format shared by all tracks ───────────────────────────────
    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val audioFormat: AudioFormat = AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    // ── playback lock (serialises writes to AudioTrack) ─────────────────
    private val playbackLock = Any()

    init {
        // Pre-generate all buffers at load time so playback is instant.
        tapBuffers = Array(C_MAJOR_SCALE.size) { i -> generateTapBuffer(i) }

        transitionBuffer = generateTransitionBuffer()

        // 11 steps (progress 0.0 .. 1.0 in 0.1 increments) is plenty;
        // at runtime we pick the nearest bucket.
        countdownBuffers = Array(11) { i -> generateCountdownBuffer(i / 10.0) }

        selectChimeBuffer = generateSelectChimeBuffer()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════════════════

    /** Short muted bell mapped to a C-major scale note. */
    fun playTap(fingerIndex: Int) {
        val idx = fingerIndex.coerceIn(0, C_MAJOR_SCALE.lastIndex)
        playBuffer(tapBuffers[idx])
    }

    /** Reverse bell swell – G major chord shimmer. */
    fun playTransition() {
        playBuffer(transitionBuffer)
    }

    /** Damped bell tick whose pitch rises with [progress] (0..1). */
    fun playCountdownTick(progress: Double) {
        val p = progress.coerceIn(0.0, 1.0)
        val idx = (p * 10).toInt().coerceIn(0, 10)
        playBuffer(countdownBuffers[idx])
    }

    /** Full resonant bell with rich harmonics. */
    fun playSelectChime() {
        playBuffer(selectChimeBuffer)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUFFER GENERATION
    // ══════════════════════════════════════════════════════════════════════

    /** ~80 ms muted bell for finger tap. */
    private fun generateTapBuffer(scaleIndex: Int): ShortArray {
        val durationSec = 0.08
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        val freq = C_MAJOR_SCALE[scaleIndex]
        val partialFreq = freq * 2.2
        val volume = 0.4
        val attackSec = 0.003

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Envelope: soft 3 ms attack, fast exponential decay
            val attack = if (t < attackSec) t / attackSec else 1.0
            val decay = exp(-t * 35.0)
            val envelope = attack * decay

            // Fundamental + inharmonic partial
            val sample = sin(TWO_PI * freq * t) +
                    0.3 * sin(TWO_PI * partialFreq * t)

            // Slight random volume variation is baked per-buffer (deterministic
            // per finger index keeps it lightweight).
            val variation = 1.0 + (scaleIndex % 3 - 1) * 0.03
            buffer[i] = toShort(sample * envelope * volume * variation)
        }
        return buffer
    }

    /** ~350 ms reverse bell swell – G major chord shimmer. */
    private fun generateTransitionBuffer(): ShortArray {
        val durationSec = 0.35
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        val volume = 0.45

        // G major chord: G4, B4, D5
        val freqs = doubleArrayOf(392.0, 493.88, 587.33)
        val amps = doubleArrayOf(1.0, 0.6, 0.3)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val norm = t / durationSec // 0..1

            // Envelope: quadratic rise then gentle fall at the tail
            val rise = norm * norm
            val fall = if (norm > 0.85) 1.0 - ((norm - 0.85) / 0.15) else 1.0
            val envelope = rise * fall

            var sample = 0.0
            for (j in freqs.indices) {
                sample += amps[j] * sin(TWO_PI * freqs[j] * t)
            }

            buffer[i] = toShort(sample * envelope * volume)
        }
        return buffer
    }

    /** ~60 ms damped bell tick whose pitch ascends with [progress]. */
    private fun generateCountdownBuffer(progress: Double): ShortArray {
        val durationSec = 0.06
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        // Map progress to scale
        val scaleIdx = (progress * (C_MAJOR_SCALE.size - 1)).toInt()
            .coerceIn(0, C_MAJOR_SCALE.lastIndex)
        val freq = C_MAJOR_SCALE[scaleIdx]
        val partialFreq = freq * 3.0
        val volume = 0.3 + progress * 0.15
        val attackSec = 0.002

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            val attack = if (t < attackSec) t / attackSec else 1.0
            val decay = exp(-t * 45.0)
            val envelope = attack * decay

            val sample = sin(TWO_PI * freq * t) +
                    0.15 * sin(TWO_PI * partialFreq * t)

            buffer[i] = toShort(sample * envelope * volume)
        }
        return buffer
    }

    /** ~500 ms full resonant bell with rich harmonics. */
    private fun generateSelectChimeBuffer(): ShortArray {
        val durationSec = 0.5
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        val volume = 0.35
        val onsetSec = 0.005

        // Partials: frequency, amplitude, decay rate
        val partials = arrayOf(
            Triple(523.25, 1.00, 4.0),   // C5
            Triple(659.25, 0.60, 5.0),   // E5
            Triple(1046.50, 0.40, 6.0),  // C6
            Triple(1174.70, 0.25, 8.0),  // ~D6
            Triple(1568.00, 0.15, 10.0), // G6
            Triple(2093.00, 0.08, 12.0)  // C7
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // 5 ms soft onset
            val onset = if (t < onsetSec) t / onsetSec else 1.0

            var sample = 0.0
            for ((freq, amp, decayRate) in partials) {
                sample += amp * exp(-t * decayRate) * sin(TWO_PI * freq * t)
            }

            buffer[i] = toShort(sample * onset * volume)
        }
        return buffer
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PLAYBACK
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Plays the given PCM [buffer] on a fire-and-forget background thread.
     *
     * A short-lived [AudioTrack] in static mode is used so that each sound
     * can overlap with others without waiting for the previous one to finish.
     */
    private fun playBuffer(buffer: ShortArray) {
        Thread({
            try {
                val bufferBytes = buffer.size * 2 // 16-bit = 2 bytes per sample
                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()

                // Wait for playback to finish then release resources.
                val durationMs = (buffer.size * 1000L) / SAMPLE_RATE + 50
                Thread.sleep(durationMs)

                track.stop()
                track.release()
            } catch (_: Exception) {
                // Silently swallow audio errors – sound is non-critical UI feedback.
            }
        }, "PickerSound").start()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Clamp a [-1, 1] double to a 16-bit PCM short. */
    private fun toShort(value: Double): Short {
        val clamped = value.coerceIn(-1.0, 1.0)
        return (clamped * Short.MAX_VALUE).toInt().toShort()
    }
}
