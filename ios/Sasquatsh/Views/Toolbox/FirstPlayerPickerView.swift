import SwiftUI
import AVFoundation
import AudioToolbox

// MARK: - Snare Drum Engine

class SnareDrumEngine {
    static let shared = SnareDrumEngine()

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let format: AVAudioFormat
    private let hitBuffer: AVAudioPCMBuffer
    private let softHitBuffer: AVAudioPCMBuffer

    private init() {
        format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1)!
        hitBuffer = SnareDrumEngine.makeSnare(format: format, volume: 0.7, duration: 0.18, noiseDecayRate: 18, bodyLevel: 0.5)
        softHitBuffer = SnareDrumEngine.makeSnare(format: format, volume: 0.35, duration: 0.10, noiseDecayRate: 25, bodyLevel: 0.4)

        engine.attach(playerNode)
        engine.connect(playerNode, to: engine.mainMixerNode, format: format)

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: .mixWithOthers)
            try engine.start()
        } catch {
            print("SnareDrumEngine: \(error)")
        }
    }

    private static func makeSnare(format: AVAudioFormat, volume: Double, duration: Double, noiseDecayRate: Double, bodyLevel: Double) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        var lpState = 0.0
        var hpState = 0.0

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            var sample = 0.0

            // 1) Stick click — sharp transient in first ~1ms
            if t < 0.001 {
                sample += (1.0 - t / 0.001) * 0.9
            }

            // 2) Drum body — 185Hz sine, decays in ~25ms
            let body = sin(2.0 * .pi * 185.0 * t) * exp(-t * 38.0)
            sample += body * bodyLevel

            // 3) Snare wire rattle — bandpass filtered noise
            let raw = Double.random(in: -1...1)
            lpState += 0.71 * (raw - lpState)       // LP ~5kHz
            hpState += 0.05 * (lpState - hpState)   // HP ~350Hz
            let bandpassed = lpState - hpState
            sample += bandpassed * exp(-t * noiseDecayRate) * (1.0 - bodyLevel)

            out[i] = Float(sample * volume)
        }
        return buf
    }

    func playHit() {
        if !engine.isRunning { try? engine.start() }
        playerNode.scheduleBuffer(hitBuffer, at: nil, options: [], completionHandler: nil)
        if !playerNode.isPlaying { playerNode.play() }
    }

    func playSoftHit() {
        if !engine.isRunning { try? engine.start() }
        playerNode.scheduleBuffer(softHitBuffer, at: nil, options: [], completionHandler: nil)
        if !playerNode.isPlaying { playerNode.play() }
    }
}

struct FirstPlayerPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var touches: [TouchPoint] = []
    @State private var phase: PickerPhase = .waiting
    @State private var winnerIndex: Int?
    @State private var pulseScale: CGFloat = 1.0
    @State private var countdown = 3
    @State private var touchIdsAtStart: Set<Int> = []
    @State private var countdownTaskId = UUID()
    @State private var pulseTickTask: Task<Void, Never>?

    private let sounds = PickerSoundEngine.shared

    private let fingerColors: [Color] = [
        .red, .blue, .green, .orange, .purple
    ]

    var body: some View {
        VStack(spacing: 0) {
        // Dark nav bar for black background
        HStack(spacing: 8) {
            Button { dismiss() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.7))
                    .frame(width: 28, height: 28)
            }
            Text("Finger Picker")
                .font(.md3TitleMedium)
                .foregroundStyle(.white.opacity(0.8))
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.black)
        ZStack {
            Color.black

            // Instructions
            if phase == .waiting && touches.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "hand.raised.fingers.spread.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(.white.opacity(0.3))
                    Text("Everyone place a finger\non the screen")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundStyle(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                    Text("Supports up to 5 players")
                        .font(.system(size: 14))
                        .foregroundStyle(.white.opacity(0.3))
                }
            }

            // Countdown
            if phase == .countdown {
                Text("\(countdown)")
                    .font(.system(size: 80, weight: .bold))
                    .foregroundStyle(.white.opacity(0.3))
            }

            // Touch circles
            ForEach(Array(touches.enumerated()), id: \.element.id) { index, touch in
                let color = fingerColors[index % fingerColors.count]
                let isWinner = winnerIndex == index

                Circle()
                    .fill(
                        isWinner
                            ? color
                            : (phase == .selected ? color.opacity(0.2) : color.opacity(0.6))
                    )
                    .frame(
                        width: isWinner ? 140 : (phase == .pulsing ? 100 : 80),
                        height: isWinner ? 140 : (phase == .pulsing ? 100 : 80)
                    )
                    .scaleEffect(phase == .pulsing ? pulseScale : 1.0)
                    .overlay {
                        if isWinner {
                            VStack(spacing: 4) {
                                Image(systemName: "crown.fill")
                                    .font(.system(size: 28))
                                    .foregroundStyle(.white)
                                Text("FIRST!")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    .shadow(color: isWinner ? color : .clear, radius: isWinner ? 20 : 0)
                    .position(touch.location)
                    .animation(.easeInOut(duration: 0.3), value: phase)
                    .animation(.easeInOut(duration: 0.3), value: isWinner)
            }
        }
        .overlay {
            if phase == .selected {
                // Reset overlay - catches taps when game is over
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        reset()
                    }
                    .overlay(alignment: .bottom) {
                        Text("Tap anywhere to reset")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(.white.opacity(0.2))
                            .clipShape(Capsule())
                            .padding(.bottom, 60)
                    }
            } else {
                // Multi-touch view - only active when not in selected state
                MultiTouchView { updatedTouches in
                    handleTouches(updatedTouches)
                }
            }
        } // .overlay on ZStack
        } // VStack end
        .background(Color.black)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - Sound Effects

    private func playTapSound(fingerIndex: Int = 0) {
        sounds.playTap(fingerIndex: fingerIndex)
    }

    private func playCountdownTick() {
        sounds.playCountdownTick()
    }

    private func startDrumRoll() {
        pulseTickTask?.cancel()
        pulseTickTask = Task { @MainActor in
            // Accelerating countdown ticks
            let intervals: [UInt64] = [
                500_000_000, 400_000_000, 300_000_000, 200_000_000, 120_000_000, 80_000_000
            ]
            var step = 0
            var totalTicks = 0
            while !Task.isCancelled && (phase == .countdown || phase == .pulsing) {
                let progress = min(Double(totalTicks) / 30.0, 1.0)
                sounds.playCountdownTick(progress: progress)
                let interval = intervals[min(step, intervals.count - 1)]
                try? await Task.sleep(nanoseconds: interval)
                totalTicks += 1
                if phase == .pulsing && step < intervals.count - 1 {
                    step += 1
                }
            }
        }
    }

    private func stopPulseTicks() {
        pulseTickTask?.cancel()
        pulseTickTask = nil
    }

    private func playWinnerSound() {
        stopPulseTicks()
        sounds.playSelectChime()
    }

    private func handleTouches(_ newTouches: [TouchPoint]) {
        // Play tap sound for new fingers
        let currentIds = Set(newTouches.map(\.id))
        let previousIds = Set(touches.map(\.id))
        let newFingers = currentIds.subtracting(previousIds)
        if !newFingers.isEmpty {
            playTapSound(fingerIndex: newTouches.count - 1)
        }

        touches = newTouches

        switch phase {
        case .waiting:
            if newTouches.count >= 2 {
                touchIdsAtStart = currentIds
                phase = .countdown
                countdown = 3
                sounds.playTransition()
                startCountdown()
            }

        case .countdown, .pulsing:
            // Reset if ANY finger was added or removed
            if currentIds != touchIdsAtStart {
                if phase == .pulsing {
                    pulseScale = 1.0
                    stopPulseTicks()
                }

                if newTouches.count < 2 {
                    phase = .waiting
                    countdown = 3
                    countdownTaskId = UUID()
                } else {
                    // Finger set changed — restart countdown
                    touchIdsAtStart = currentIds
                    pulseScale = 1.0
                    phase = .countdown
                    countdown = 3
                    countdownTaskId = UUID()
                    startCountdown()
                }
            }

        case .selected:
            break
        }
    }

    private func startCountdown() {
        let taskId = countdownTaskId
        Task { @MainActor in
            startDrumRoll()

            for i in stride(from: 3, through: 1, by: -1) {
                guard taskId == countdownTaskId else { return }
                countdown = i
                try? await Task.sleep(for: .seconds(1))
                guard taskId == countdownTaskId else { return }
                if phase != .countdown { return }
            }

            // Start pulsing — drum roll continues and accelerates
            phase = .pulsing
            startPulsing()

            // Wait then select
            try? await Task.sleep(for: .seconds(2))
            guard taskId == countdownTaskId else { return }
            if phase == .pulsing {
                selectWinner()
            }
        }
    }

    private func startPulsing() {
        withAnimation(.easeInOut(duration: 0.4).repeatForever(autoreverses: true)) {
            pulseScale = 1.2
        }
    }

    private func selectWinner() {
        guard !touches.isEmpty else { return }
        pulseScale = 1.0
        withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
            winnerIndex = Int.random(in: 0..<touches.count)
            phase = .selected
        }

        playWinnerSound()

        // Haptic feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }

    private func reset() {
        stopPulseTicks()
        withAnimation {
            phase = .waiting
            winnerIndex = nil
            touches = []
            pulseScale = 1.0
            countdown = 3
            touchIdsAtStart = []
            countdownTaskId = UUID()
        }
    }
}

// MARK: - Types

enum PickerPhase {
    case waiting, countdown, pulsing, selected
}

struct TouchPoint: Identifiable {
    let id: Int
    var location: CGPoint
}

// MARK: - Multi-Touch UIKit Bridge

struct MultiTouchView: UIViewRepresentable {
    var onTouchesChanged: ([TouchPoint]) -> Void

    func makeUIView(context: Context) -> MultiTouchUIView {
        let view = MultiTouchUIView()
        view.isMultipleTouchEnabled = true
        view.backgroundColor = .clear
        view.onTouchesChanged = onTouchesChanged
        return view
    }

    func updateUIView(_ uiView: MultiTouchUIView, context: Context) {
        uiView.onTouchesChanged = onTouchesChanged
    }
}

class MultiTouchUIView: UIView {
    var onTouchesChanged: (([TouchPoint]) -> Void)?
    private var activeTouches: [UITouch: Int] = [:]
    private var nextId = 0

    private func reportTouches() {
        let points = activeTouches.map { (touch, id) in
            TouchPoint(id: id, location: touch.location(in: self))
        }.sorted { $0.id < $1.id }
        onTouchesChanged?(points)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            if activeTouches[touch] == nil {
                activeTouches[touch] = nextId
                nextId += 1
            }
        }
        reportTouches()
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        reportTouches()
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            activeTouches.removeValue(forKey: touch)
        }
        reportTouches()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            activeTouches.removeValue(forKey: touch)
        }
        reportTouches()
    }
}
