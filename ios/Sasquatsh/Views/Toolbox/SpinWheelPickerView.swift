import SwiftUI
import AVFoundation

struct SpinWheelPickerView: View {
    @State private var playerCount: Int = 4
    @State private var players: [WheelPlayer] = []
    @State private var phase: WheelPhase = .setup
    @State private var rotation: Double = 0
    @State private var winnerIndex: Int?

    private let wheelColors: [Color] = [
        .red, .blue, .green, .orange, .purple, .cyan, .pink, .yellow, .mint, .indigo,
        .teal, .brown, Color(red: 0.8, green: 0.2, blue: 0.4), Color(red: 0.4, green: 0.7, blue: 0.2),
        Color(red: 0.9, green: 0.5, blue: 0.1), Color(red: 0.3, green: 0.3, blue: 0.8),
        Color(red: 0.7, green: 0.1, blue: 0.6), Color(red: 0.1, green: 0.6, blue: 0.5),
        Color(red: 0.9, green: 0.3, blue: 0.3), Color(red: 0.2, green: 0.5, blue: 0.7)
    ]

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "Spin the Wheel") { dismiss() }
            ZStack {
                Color.md3SurfaceContainer

                switch phase {
                case .setup:
                    setupView
                case .spinning, .result:
                    wheelView
                }
            }
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - Setup

    private var setupView: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                Image(systemName: "circle.dotted.and.circle")
                    .font(.system(size: 50))
                    .foregroundStyle(Color.md3Primary.opacity(0.4))

                Text("How many players?")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color.md3OnSurface)

                HStack(spacing: 20) {
                    Button {
                        if playerCount > 2 { playerCount -= 1 }
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(playerCount > 2 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(playerCount <= 2)

                    Text("\(playerCount)")
                        .font(.system(size: 44, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.md3OnSurface)
                        .frame(width: 70)

                    Button {
                        if playerCount < 20 { playerCount += 1 }
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(playerCount < 20 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(playerCount >= 20)
                }

                HStack(spacing: 6) {
                    ForEach(0..<playerCount, id: \.self) { i in
                        VStack(spacing: 3) {
                            Circle()
                                .fill(wheelColors[i % wheelColors.count])
                                .frame(width: 24, height: 24)
                            Text("P\(i + 1)")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }
            }

            Spacer()

            Button {
                startGame()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.trianglehead.2.clockwise.rotate.90")
                    Text("Spin the Wheel!")
                }
                .primaryButtonStyle()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }

    // MARK: - Wheel

    private var wheelView: some View {
        GeometryReader { geo in
            let available = geo.size.height - 100 // reserve space for winner text + buttons
            let wheelSize = min(geo.size.width - 60, available * 0.75, 280)

            VStack(spacing: 8) {
                ZStack {
                    SpinWheel(players: players, rotation: rotation)
                        .frame(width: wheelSize, height: wheelSize)

                    Circle()
                        .fill(Color.md3Surface)
                        .frame(width: 36, height: 36)
                        .shadow(color: .black.opacity(0.2), radius: 4)
                        .overlay {
                            Circle()
                                .stroke(Color.md3OnSurfaceVariant.opacity(0.3), lineWidth: 2)
                        }

                    VStack(spacing: 0) {
                        Triangle()
                            .fill(Color.md3Error)
                            .frame(width: 22, height: 18)
                            .shadow(color: .black.opacity(0.3), radius: 2, y: 2)
                        Spacer()
                    }
                    .frame(height: wheelSize)
                    .offset(y: -9)
                }
                .padding(.top, 4)

                if let winnerIndex, phase == .result {
                    HStack(spacing: 10) {
                        Circle()
                            .fill(players[winnerIndex].color)
                            .frame(width: 28, height: 28)
                            .overlay {
                                Text("\(winnerIndex + 1)")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        Text("Player \(winnerIndex + 1) goes first!")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(Color.md3OnSurface)
                    }
                    .transition(.opacity.combined(with: .scale(scale: 0.8)))
                    .padding(.top, 4)
                }

                Spacer()

                if phase == .result {
                    HStack(spacing: 12) {
                        Button {
                            spin()
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "arrow.clockwise")
                                Text("Spin Again")
                            }
                            .primaryButtonStyle()
                        }

                        Button {
                            withAnimation {
                                phase = .setup
                                rotation = 0
                                winnerIndex = nil
                                players = []
                            }
                        } label: {
                            Image(systemName: "gear")
                                .font(.md3LabelLarge)
                                .frame(width: 48, height: 40)
                                .background(Color.md3SecondaryContainer)
                                .foregroundStyle(Color.md3OnSecondaryContainer)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }
            .padding(.bottom, 12)
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Logic

    private func startGame() {
        players = (0..<playerCount).map { i in
            WheelPlayer(index: i, color: wheelColors[i % wheelColors.count])
        }
        phase = .spinning
        spin()
    }

    private func spin() {
        winnerIndex = nil
        phase = .spinning

        // Random final rotation: 3-5 full spins + random offset
        let extraSpins = Double.random(in: 3...5) * 360
        let randomOffset = Double.random(in: 0..<360)
        let totalRotation = rotation + extraSpins + randomOffset
        let spinDuration = 7.0

        WheelSoundEngine.shared.startTicking(
            segmentCount: players.count,
            startRotation: rotation,
            endRotation: totalRotation,
            duration: spinDuration
        )

        // Dramatic deceleration — spends most of the time crawling at the end
        withAnimation(.timingCurve(0.0, 0.6, 0.05, 1.0, duration: spinDuration)) {
            rotation = totalRotation
        }

        // Determine winner after spin
        DispatchQueue.main.asyncAfter(deadline: .now() + spinDuration + 0.1) {
            let segmentAngle = 360.0 / Double(players.count)
            // Pointer is at top (0°), wheel rotates clockwise
            // Normalize to find which segment is at top
            let normalizedAngle = rotation.truncatingRemainder(dividingBy: 360)
            let pointerAngle = (360 - normalizedAngle).truncatingRemainder(dividingBy: 360)
            let winner = Int(pointerAngle / segmentAngle) % players.count

            withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
                winnerIndex = winner
                phase = .result
            }

            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(.success)

            // Winner sound
            AudioServicesPlaySystemSound(1025)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                AudioServicesPlaySystemSound(1025)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                AudioServicesPlaySystemSound(1016)
            }
        }
    }
}

// MARK: - Types

struct WheelPlayer {
    let index: Int
    let color: Color
}

enum WheelPhase {
    case setup, spinning, result
}

// MARK: - Wheel Drawing

struct SpinWheel: View {
    let players: [WheelPlayer]
    let rotation: Double

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let center = CGPoint(x: size / 2, y: size / 2)
            let radius = size / 2
            let segmentAngle = 360.0 / Double(players.count)

            ZStack {
                // Segments
                ForEach(players, id: \.index) { player in
                    WheelSegment(
                        center: center,
                        radius: radius,
                        startAngle: Double(player.index) * segmentAngle - 90,
                        endAngle: Double(player.index + 1) * segmentAngle - 90,
                        color: player.color
                    )
                }

                // Segment dividers
                ForEach(0..<players.count, id: \.self) { i in
                    let angle = Angle(degrees: Double(i) * segmentAngle - 90)
                    let x = center.x + radius * cos(angle.radians)
                    let y = center.y + radius * sin(angle.radians)
                    Path { path in
                        path.move(to: center)
                        path.addLine(to: CGPoint(x: x, y: y))
                    }
                    .stroke(Color.white, lineWidth: 2)
                }

                // Player labels
                ForEach(players, id: \.index) { player in
                    let midAngle = (Double(player.index) + 0.5) * segmentAngle - 90
                    let labelRadius = radius * 0.65
                    let angle = Angle(degrees: midAngle)
                    let x = center.x + labelRadius * cos(angle.radians)
                    let y = center.y + labelRadius * sin(angle.radians)

                    Text("P\(player.index + 1)")
                        .font(.system(size: players.count > 12 ? 10 : (players.count > 6 ? 12 : 16), weight: .bold))
                        .foregroundStyle(.white)
                        .shadow(color: .black.opacity(0.5), radius: 1)
                        .position(x: x, y: y)
                }

                // Outer ring
                Circle()
                    .stroke(Color.md3OnSurfaceVariant.opacity(0.3), lineWidth: 3)
                    .frame(width: size, height: size)

                // Pegs around the edge
                ForEach(0..<players.count * 2, id: \.self) { i in
                    let pegAngle = Angle(degrees: Double(i) * (360.0 / Double(players.count * 2)) - 90)
                    let pegRadius = radius - 6
                    let x = center.x + pegRadius * cos(pegAngle.radians)
                    let y = center.y + pegRadius * sin(pegAngle.radians)

                    Circle()
                        .fill(Color.white.opacity(0.6))
                        .frame(width: 6, height: 6)
                        .position(x: x, y: y)
                }
            }
        }
        .rotationEffect(.degrees(rotation))
        .aspectRatio(1, contentMode: .fit)
    }
}

struct WheelSegment: View {
    let center: CGPoint
    let radius: CGFloat
    let startAngle: Double
    let endAngle: Double
    let color: Color

    var body: some View {
        Path { path in
            path.move(to: center)
            path.addArc(
                center: center,
                radius: radius,
                startAngle: .degrees(startAngle),
                endAngle: .degrees(endAngle),
                clockwise: false
            )
            path.closeSubpath()
        }
        .fill(color)
    }
}

// MARK: - Pointer Triangle

struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.midX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            path.closeSubpath()
        }
    }
}

// MARK: - Wheel Click Sound Engine

class WheelSoundEngine {
    static let shared = WheelSoundEngine()

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let format: AVAudioFormat
    private let clickBuffer: AVAudioPCMBuffer
    private var tickTask: Task<Void, Never>?

    private init() {
        format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1)!
        clickBuffer = WheelSoundEngine.makeClick(format: format)

        engine.attach(playerNode)
        engine.connect(playerNode, to: engine.mainMixerNode, format: format)

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: .mixWithOthers)
            try engine.start()
        } catch {
            print("WheelSoundEngine: \(error)")
        }
    }

    private static func makeClick(format: AVAudioFormat) -> AVAudioPCMBuffer {
        let sr = format.sampleRate
        let duration = 0.025
        let count = AVAudioFrameCount(sr * duration)
        let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count)!
        buf.frameLength = count
        let out = buf.floatChannelData![0]

        for i in 0..<Int(count) {
            let t = Double(i) / sr
            // Sharp metallic click — like a peg hitting the pointer
            let click = sin(2.0 * .pi * 800 * t) * exp(-t * 200)
            let knock = sin(2.0 * .pi * 2200 * t) * exp(-t * 300) * 0.4
            out[i] = Float((click + knock) * 0.6)
        }
        return buf
    }

    func playClick() {
        if !engine.isRunning { try? engine.start() }
        playerNode.scheduleBuffer(clickBuffer, at: nil, options: [], completionHandler: nil)
        if !playerNode.isPlaying { playerNode.play() }
    }

    /// Simulates the clicking sound of a wheel spinning past pegs
    func startTicking(segmentCount: Int, startRotation: Double, endRotation: Double, duration: Double) {
        tickTask?.cancel()

        let totalDegrees = endRotation - startRotation
        let pegCount = segmentCount * 2 // pegs between and at each segment
        let degreesPerPeg = 360.0 / Double(pegCount)

        tickTask = Task { @MainActor in
            let startTime = Date()

            // Use the same easing curve as the animation to sync clicks
            var lastPegIndex = Int(startRotation / degreesPerPeg)

            while !Task.isCancelled {
                let elapsed = Date().timeIntervalSince(startTime)
                if elapsed >= duration { break }

                let progress = elapsed / duration
                // Match the timingCurve(0.0, 0.6, 0.05, 1.0) — dramatic slow finish
                let eased = cubicBezierEase(t: progress, x1: 0.0, y1: 0.6, x2: 0.05, y2: 1.0)
                let currentRotation = startRotation + totalDegrees * eased
                let currentPegIndex = Int(currentRotation / degreesPerPeg)

                if currentPegIndex > lastPegIndex {
                    playClick()
                    lastPegIndex = currentPegIndex
                }

                try? await Task.sleep(nanoseconds: 8_000_000) // ~120fps check
            }
        }
    }

    /// Approximate cubic bezier easing
    private func cubicBezierEase(t: Double, x1: Double, y1: Double, x2: Double, y2: Double) -> Double {
        // Simple approximation using iterative method
        var low = 0.0, high = 1.0
        for _ in 0..<20 {
            let mid = (low + high) / 2
            let x = cubicBezier(t: mid, p1: x1, p2: x2)
            if x < t { low = mid } else { high = mid }
        }
        let bezierT = (low + high) / 2
        return cubicBezier(t: bezierT, p1: y1, p2: y2)
    }

    private func cubicBezier(t: Double, p1: Double, p2: Double) -> Double {
        let t2 = t * t
        let t3 = t2 * t
        return 3 * (1 - t) * (1 - t) * t * p1 + 3 * (1 - t) * t2 * p2 + t3
    }
}
