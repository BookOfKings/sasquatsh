import AVFoundation

/// Bell-based sound engine for the First Player Picker.
/// All sounds derived from bell/chime tones with musical pitch variation.
class PickerSoundEngine {
    static let shared = PickerSoundEngine()

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let format: AVAudioFormat

    // C major scale frequencies (C4 through C5)
    private let scale: [Double] = [
        261.63, // C4
        293.66, // D4
        329.63, // E4
        349.23, // F4
        392.00, // G4
        440.00, // A4
        493.88, // B4
        523.25, // C5
    ]

    // Pre-generated buffers
    private var tapBuffers: [AVAudioPCMBuffer] = []   // one per scale note
    private let transitionBuffer: AVAudioPCMBuffer     // reverse bell swell
    private var tickBuffers: [AVAudioPCMBuffer] = []   // damped bells, rising pitch
    private let chimeBuffer: AVAudioPCMBuffer           // full bell

    private init() {
        let fmt = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1)!
        format = fmt

        let scaleFreqs = scale

        // Tap: short muted bell per scale note
        tapBuffers = scaleFreqs.map { freq in
            PickerSoundEngine.makeMutedBell(format: fmt, frequency: freq, duration: 0.08, volume: 0.4)
        }

        // Transition: soft reverse bell swell
        transitionBuffer = PickerSoundEngine.makeReverseBell(format: fmt)

        // Countdown ticks: damped bells ascending the scale
        tickBuffers = scaleFreqs.map { freq in
            PickerSoundEngine.makeDampedBell(format: fmt, frequency: freq, duration: 0.06, volume: 0.35)
        }

        // Final: full resonant bell chime
        chimeBuffer = PickerSoundEngine.makeFullBell(format: fmt)

        engine.attach(playerNode)
        engine.connect(playerNode, to: engine.mainMixerNode, format: format)

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: .mixWithOthers)
            try engine.start()
        } catch {
            print("PickerSoundEngine: \(error)")
        }
    }

    private func play(_ buffer: AVAudioPCMBuffer) {
        if !engine.isRunning { try? engine.start() }
        playerNode.scheduleBuffer(buffer, at: nil, options: [], completionHandler: nil)
        if !playerNode.isPlaying { playerNode.play() }
    }

    // MARK: - Public API

    /// Short muted bell for finger placement. Pitch mapped to C major scale by finger index.
    func playTap(fingerIndex: Int = 0) {
        let idx = fingerIndex % tapBuffers.count
        playerNode.volume = Float(0.35 + Double.random(in: -0.03...0.03))
        play(tapBuffers[idx])
    }

    /// Soft reverse bell swell when entering countdown.
    func playTransition() {
        playerNode.volume = 0.4
        play(transitionBuffer)
    }

    /// Damped bell tick for countdown. Progress (0–1) selects ascending pitch.
    func playCountdownTick(progress: Double = 0) {
        let idx = min(Int(progress * Double(tickBuffers.count - 1)), tickBuffers.count - 1)
        playerNode.volume = Float(0.3 + progress * 0.15)
        play(tickBuffers[max(0, idx)])
    }

    /// Full resonant bell chime for final selection.
    func playSelectChime() {
        playerNode.volume = 0.6
        play(chimeBuffer)
    }

    // MARK: - Bell Synthesis

    /// Short muted bell: fundamental + 2nd partial, fast damped envelope (~80ms)
    private static func makeMutedBell(format: AVAudioFormat, frequency: Double, duration: Double, volume: Double) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            // Soft attack (no click): use sine-shaped onset ~3ms
            let attack = min(t / 0.003, 1.0)
            // Fast exponential decay
            let decay = exp(-t * 35)
            let env = attack * decay

            let f1 = sin(2.0 * .pi * frequency * t)
            let f2 = sin(2.0 * .pi * frequency * 2.2 * t) * 0.3 // inharmonic partial (bell-like)
            out[i] = Float((f1 + f2) * env * volume)
        }
        return buf
    }

    /// Reverse bell swell: amplitude rises then cuts, ~350ms
    private static func makeReverseBell(format: AVAudioFormat) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let duration = 0.35
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            let norm = t / duration

            // Reverse envelope: slow rise, soft peak, gentle fall at end
            let env = pow(norm, 2.0) * (1.0 - pow(max(0, norm - 0.85) / 0.15, 2.0)) * 0.45

            // Bell tone cluster (G4 + B4 + D5) — G major chord shimmer
            let f1 = sin(2.0 * .pi * 392.0 * t)         // G4
            let f2 = sin(2.0 * .pi * 493.88 * t) * 0.6  // B4
            let f3 = sin(2.0 * .pi * 587.33 * t) * 0.3  // D5
            out[i] = Float((f1 + f2 + f3) * env)
        }
        return buf
    }

    /// Damped bell tick: like a muted bell strike, very short (~60ms)
    private static func makeDampedBell(format: AVAudioFormat, frequency: Double, duration: Double, volume: Double) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            // Soft onset + fast damp
            let attack = min(t / 0.002, 1.0)
            let decay = exp(-t * 45)
            let env = attack * decay

            let f1 = sin(2.0 * .pi * frequency * t)
            let f2 = sin(2.0 * .pi * frequency * 3.0 * t) * 0.15 // bell partial
            out[i] = Float((f1 + f2) * env * volume)
        }
        return buf
    }

    /// Full resonant bell: rich harmonics, slow decay (~500ms)
    private static func makeFullBell(format: AVAudioFormat) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let duration = 0.5
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        // C5 bell with inharmonic partials (characteristic bell sound)
        let partials: [(freq: Double, amp: Double, decay: Double)] = [
            (523.25, 1.0,  4.0),   // C5 fundamental
            (659.25, 0.6,  5.0),   // E5 (major third)
            (1046.5, 0.4,  6.0),   // C6 octave
            (1174.7, 0.25, 8.0),   // ~D6 (inharmonic, bell character)
            (1568.0, 0.15, 10.0),  // G6 shimmer
            (2093.0, 0.08, 12.0),  // C7 sparkle
        ]

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            let attack = min(t / 0.005, 1.0) // 5ms soft onset
            var sample = 0.0

            for p in partials {
                sample += sin(2.0 * .pi * p.freq * t) * p.amp * exp(-t * p.decay)
            }

            out[i] = Float(sample * attack * 0.35)
        }
        return buf
    }
}
