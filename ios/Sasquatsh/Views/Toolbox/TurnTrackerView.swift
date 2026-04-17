import SwiftUI
import AVFoundation

struct TurnTrackerView: View {
    @State private var phase: TurnPhase = .setup
    @State private var playerNames: [String] = []
    @State private var newPlayerName = ""
    @State private var savedNames: [String] = []
    @State private var filteredSuggestions: [String] = []
    @State private var currentTurnIndex = 0
    @State private var roundNumber = 1
    @State private var firstPlayerIndex = 0
    @State private var showPickFirst = false

    // Timer
    @State private var timerEnabled = false
    @State private var turnStartTime: Date?
    @State private var currentTurnElapsed: TimeInterval = 0
    @State private var totalTurnTimes: [Int: TimeInterval] = [:] // playerIndex -> total seconds
    @State private var turnCounts: [Int: Int] = [:] // playerIndex -> number of turns
    @State private var timerTask: Task<Void, Never>?
    @State private var showStats = false
    @State private var roundStartTime: Date?
    @State private var roundTimes: [TimeInterval] = []
    @FocusState private var playerNameFocused: Bool
    @State private var quickMode = true
    @State private var quickPlayerCount = 4
    @State private var firstPlayerName = ""
    @State private var rotateFirstPlayer = false
    @State private var pulseScale: CGFloat = 1.0
    @State private var glowOpacity: Double = 0.3
    @State private var trashTalkEnabled = false
    @State private var trashTalkThreshold: Int = 60 // seconds
    @State private var hasTrashedThisTurn = false
    private let synthesizer = AVSpeechSynthesizer()

    private let playerColors: [Color] = [
        .red, .blue, .green, .orange, .purple, .cyan, .pink, .yellow, .mint, .indigo
    ]

    var body: some View {
        VStack(spacing: 0) {
            switch phase {
            case .setup:
                setupView
            case .pickFirst:
                pickFirstView
            case .playing:
                playingView
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Turn Tracker")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
            Task { savedNames = await ScoreKeeperPersistence.shared.loadSavedNames() }
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }

    // MARK: - Setup

    private var setupView: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 16) {
                    // Mode toggle
                    Picker("Mode", selection: $quickMode) {
                        Text("Quick Start").tag(true)
                        Text("Named Players").tag(false)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                    // Timer toggle
                    Toggle(isOn: $timerEnabled) {
                        HStack(spacing: 8) {
                            Image(systemName: "timer")
                                .foregroundStyle(Color.md3Primary)
                            Text("Track turn times")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3OnSurface)
                        }
                    }
                    .tint(Color.md3Primary)
                    .padding(.horizontal, 16)

                    if timerEnabled {
                        VStack(spacing: 8) {
                            Toggle(isOn: $trashTalkEnabled) {
                                HStack(spacing: 8) {
                                    Image(systemName: "megaphone.fill")
                                        .foregroundStyle(Color.md3Error)
                                    Text("Trash talk slow players")
                                        .font(.md3LabelLarge)
                                        .foregroundStyle(Color.md3OnSurface)
                                }
                            }
                            .tint(Color.md3Error)

                            if trashTalkEnabled {
                                HStack {
                                    Text("After")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Picker("", selection: $trashTalkThreshold) {
                                        Text("30s").tag(30)
                                        Text("1 min").tag(60)
                                        Text("2 min").tag(120)
                                        Text("3 min").tag(180)
                                        Text("5 min").tag(300)
                                    }
                                    .pickerStyle(.segmented)
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    Toggle(isOn: $rotateFirstPlayer) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.triangle.2.circlepath.circle")
                                .foregroundStyle(Color.md3Primary)
                            Text("Rotate first player each round")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3OnSurface)
                        }
                    }
                    .tint(Color.md3Primary)
                    .padding(.horizontal, 16)

                    if quickMode {
                        quickSetupSection
                    } else {
                        namedSetupSection
                    }
                }
                .padding(.bottom, 16)
            }

            if quickMode {
                Button {
                    startQuickMode()
                } label: {
                    Text("Start")
                        .primaryButtonStyle()
                }
                .disabled(quickPlayerCount < 2)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color.md3SurfaceContainer)
            } else {
                Button {
                    withAnimation { phase = .pickFirst }
                } label: {
                    Text("Next: Pick First Player")
                        .primaryButtonStyle()
                }
                .disabled(playerNames.count < 2)
                .opacity(playerNames.count < 2 ? 0.4 : 1)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color.md3SurfaceContainer)
            }
        }
    }

    // MARK: - Quick Setup

    @State private var firstPlayerSuggestions: [String] = []

    private var quickSetupSection: some View {
        VStack(spacing: 20) {
            // First player name with autocomplete
            VStack(alignment: .leading, spacing: 6) {
                Text("Who goes first?")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                TextField("First player name", text: $firstPlayerName)
                    .padding(10)
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    .onChange(of: firstPlayerName) { _, value in
                        if value.isEmpty {
                            firstPlayerSuggestions = []
                        } else {
                            firstPlayerSuggestions = savedNames.filter {
                                $0.localizedCaseInsensitiveContains(value)
                            }.prefix(5).map { $0 }
                        }
                    }

                if !firstPlayerSuggestions.isEmpty {
                    VStack(spacing: 0) {
                        ForEach(firstPlayerSuggestions, id: \.self) { name in
                            Button {
                                firstPlayerName = name
                                firstPlayerSuggestions = []
                            } label: {
                                HStack {
                                    Image(systemName: "clock.arrow.circlepath")
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Text(name)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                            }
                            Divider()
                        }
                    }
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }

                // Quick pick from recent names
                if firstPlayerName.isEmpty && !savedNames.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(savedNames.prefix(8), id: \.self) { name in
                                Button {
                                    firstPlayerName = name
                                } label: {
                                    Text(name)
                                        .font(.md3LabelLarge)
                                        .padding(.horizontal, 12)
                                        .frame(height: 30)
                                        .background(Color.md3SecondaryContainer)
                                        .foregroundStyle(Color.md3OnSecondaryContainer)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)

            // Player count
            VStack(spacing: 8) {
                Text("How many players?")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3OnSurfaceVariant)

                HStack(spacing: 20) {
                    Button {
                        if quickPlayerCount > 2 { quickPlayerCount -= 1 }
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(quickPlayerCount > 2 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(quickPlayerCount <= 2)

                    Text("\(quickPlayerCount)")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.md3OnSurface)
                        .frame(width: 60)

                    Button {
                        if quickPlayerCount < 20 { quickPlayerCount += 1 }
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(quickPlayerCount < 20 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(quickPlayerCount >= 20)
                }
            }
            .padding(.horizontal, 16)

            // Preview
            HStack(spacing: 6) {
                ForEach(0..<quickPlayerCount, id: \.self) { i in
                    VStack(spacing: 3) {
                        Circle()
                            .fill(playerColors[i % playerColors.count])
                            .frame(width: 20, height: 20)
                            .overlay {
                                if i == 0 {
                                    Image(systemName: "star.fill")
                                        .font(.system(size: 8))
                                        .foregroundStyle(.white)
                                }
                            }
                        Text(i == 0 ? (firstPlayerName.isEmpty ? "P1" : String(firstPlayerName.prefix(3))) : "P\(i + 1)")
                            .font(.system(size: 9))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Named Setup

    private var namedSetupSection: some View {
        VStack(spacing: 16) {
            // Add player
            VStack(alignment: .leading, spacing: 6) {
                Text("Add players in seating order")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3OnSurfaceVariant)

                HStack(spacing: 8) {
                    TextField("Player name", text: $newPlayerName)
                        .focused($playerNameFocused)
                        .padding(10)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        .onChange(of: newPlayerName) { _, value in
                            if value.isEmpty {
                                filteredSuggestions = []
                            } else {
                                filteredSuggestions = savedNames.filter { savedName in
                                    savedName.localizedCaseInsensitiveContains(value) &&
                                    !playerNames.contains(where: { $0.caseInsensitiveCompare(savedName) == .orderedSame })
                                }.prefix(5).map { $0 }
                            }
                        }
                        .onSubmit { addPlayer() }

                    Button { addPlayer() } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 28))
                            .foregroundStyle(newPlayerName.trimmingCharacters(in: .whitespaces).isEmpty ? Color.md3OnSurfaceVariant.opacity(0.3) : Color.md3Primary)
                    }
                    .disabled(newPlayerName.trimmingCharacters(in: .whitespaces).isEmpty)
                }

                if !filteredSuggestions.isEmpty {
                    VStack(spacing: 0) {
                        ForEach(filteredSuggestions, id: \.self) { name in
                            Button {
                                newPlayerName = name
                                addPlayer()
                            } label: {
                                HStack {
                                    Image(systemName: "clock.arrow.circlepath")
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Text(name)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                            }
                            Divider()
                        }
                    }
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            }
            .padding(.horizontal, 16)

            // Player list
            if !playerNames.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Players (\(playerNames.count))")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3OnSurfaceVariant)

                    ForEach(Array(playerNames.enumerated()), id: \.element) { index, name in
                        HStack {
                            Text("\(index + 1)")
                                .font(.system(size: 14, weight: .bold, design: .rounded))
                                .foregroundStyle(Color.md3OnSecondaryContainer)
                                .frame(width: 24, height: 24)
                                .background(playerColors[index % playerColors.count].opacity(0.2))
                                .clipShape(Circle())

                            Text(name)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurface)

                            Spacer()

                            Button {
                                playerNames.remove(at: index)
                            } label: {
                                Image(systemName: "xmark.circle")
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    // MARK: - Pick First Player

    private var pickFirstView: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 16) {
                Text("Who goes first?")
                    .font(.md3HeadlineSmall)
                    .foregroundStyle(Color.md3OnSurface)

                VStack(spacing: 8) {
                    ForEach(Array(playerNames.enumerated()), id: \.offset) { index, name in
                        Button {
                            firstPlayerIndex = index
                            currentTurnIndex = index
                            roundNumber = 1
                            totalTurnTimes = [:]
                            turnCounts = [:]
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                phase = .playing
                            }
                            if timerEnabled {
                                roundStartTime = Date()
                                startTurnTimer()
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Circle()
                                    .fill(playerColors[index % playerColors.count])
                                    .frame(width: 12, height: 12)
                                Text(name)
                                    .font(.md3TitleMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        }
                    }
                }
                .padding(.horizontal, 24)
            }

            Spacer()

            Button {
                withAnimation { phase = .setup }
            } label: {
                Text("Back")
                    .outlinedButtonStyle()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
    }

    // MARK: - Playing

    private var playingView: some View {
        let safeIndex = playerNames.isEmpty ? 0 : currentTurnIndex % playerNames.count
        let currentName = playerNames.isEmpty ? "" : playerNames[safeIndex]
        let currentColor = playerColors[safeIndex % playerColors.count]
        let nextIndex = playerNames.isEmpty ? 0 : (safeIndex + 1) % playerNames.count
        let nextName = playerNames.isEmpty ? "" : playerNames[nextIndex]

        return GeometryReader { geo in
            VStack(spacing: 0) {
                // Round indicator + stats
                HStack {
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Round \(roundNumber)")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        if rotateFirstPlayer && !playerNames.isEmpty {
                            let firstName = playerNames[firstPlayerIndex % playerNames.count]
                            Text("\(firstName) starts")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.6))
                        }
                    }

                    Spacer()

                    if timerEnabled {
                        Button { showStats = true } label: {
                            Image(systemName: "chart.bar.fill")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.md3Primary)
                        }
                    }

                    HStack(spacing: 4) {
                        ForEach(0..<playerNames.count, id: \.self) { i in
                            ZStack {
                                Circle()
                                    .fill(i == currentTurnIndex ? playerColors[i % playerColors.count] : playerColors[i % playerColors.count].opacity(0.2))
                                    .frame(width: i == currentTurnIndex ? 12 : 8, height: i == currentTurnIndex ? 12 : 8)
                                if i == firstPlayerIndex {
                                    Circle()
                                        .stroke(playerColors[i % playerColors.count], lineWidth: 1.5)
                                        .frame(width: 16, height: 16)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)

                Spacer()

                // Current player — tap circle to advance
                VStack(spacing: 12) {
                    let circleSize = min(geo.size.width * 0.55, 200)
                    Button {
                        nextTurn()
                    } label: {
                        ZStack {
                            // Pulsing glow ring
                            Circle()
                                .stroke(currentColor.opacity(glowOpacity), lineWidth: 8)
                                .frame(width: circleSize + 16, height: circleSize + 16)
                                .scaleEffect(pulseScale)

                            // Main circle
                            Circle()
                                .fill(currentColor.opacity(0.15))
                                .frame(width: circleSize, height: circleSize)
                                .overlay {
                                    VStack(spacing: 4) {
                                        Text(currentName)
                                            .font(.system(size: 28, weight: .bold))
                                            .foregroundStyle(Color.md3OnSurface)
                                            .lineLimit(1)
                                            .minimumScaleFactor(0.5)

                                        if timerEnabled {
                                            Text(formatTime(currentTurnElapsed))
                                                .font(.system(size: 36, weight: .semibold, design: .monospaced))
                                                .foregroundStyle(currentTurnElapsed > 60 ? Color.md3Error : currentColor)
                                                .contentTransition(.numericText())
                                        } else {
                                            Text("Tap when done")
                                                .font(.md3BodySmall)
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                        }
                                    }
                                    .padding(16)
                                }
                                .overlay {
                                    Circle()
                                        .stroke(currentColor, lineWidth: 4)
                                }
                        }
                    }
                    .onAppear { startPulse() }
                    .onChange(of: currentTurnIndex) { _, _ in
                        // Bounce on turn change
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
                            pulseScale = 1.15
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
                                pulseScale = 1.0
                            }
                            startPulse()
                        }
                    }

                    Text("Up next: \(nextName)")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)

                    // Timer stats bar
                    if timerEnabled && !totalTurnTimes.isEmpty {
                        VStack(spacing: 6) {
                            // Slowest player
                            if let slowest = slowestPlayerInfo {
                                HStack(spacing: 4) {
                                    Image(systemName: "tortoise.fill")
                                        .font(.system(size: 11))
                                        .foregroundStyle(Color.md3Error)
                                    Text("\(slowest.name) avg \(formatTime(slowest.avg))")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3Error)
                                }
                            }

                            // Round stats
                            if !roundTimes.isEmpty {
                                let avgRound = roundTimes.reduce(0, +) / Double(roundTimes.count)
                                HStack(spacing: 4) {
                                    Image(systemName: "clock")
                                        .font(.system(size: 11))
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Text("Avg round: \(formatTime(avgRound))")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                        }
                        .padding(.top, 4)
                    }
                }

                Spacer()

                // Controls
                HStack(spacing: 12) {
                    Button {
                        previousTurn()
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "arrow.uturn.backward")
                            Text("Undo")
                        }
                        .outlinedButtonStyle()
                    }

                    Button {
                        stopTimer()
                        playerNames = []
                        withAnimation { phase = .setup }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "stop.fill")
                            Text("End")
                        }
                        .font(.md3LabelLarge)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(Color.md3ErrorContainer)
                        .foregroundStyle(Color.md3Error)
                        .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
            }
        }
        .sheet(isPresented: $showStats) {
            turnStatsSheet
        }
    }

    // MARK: - Turn Stats Sheet

    private var turnStatsSheet: some View {
        NavigationStack {
            List {
                // Sort by average turn time descending (slowest first)
                let stats = (0..<playerNames.count).map { i in
                    let total = totalTurnTimes[i] ?? 0
                    let count = turnCounts[i] ?? 0
                    let avg = count > 0 ? total / Double(count) : 0
                    return (index: i, name: playerNames[i], total: total, count: count, avg: avg)
                }.sorted { $0.avg > $1.avg }

                Section("Slowest to Fastest") {
                    ForEach(stats, id: \.index) { stat in
                        HStack {
                            Circle()
                                .fill(playerColors[stat.index % playerColors.count])
                                .frame(width: 10, height: 10)

                            Text(stat.name)
                                .font(.md3BodyMedium)

                            Spacer()

                            VStack(alignment: .trailing, spacing: 2) {
                                Text("Avg: \(formatTime(stat.avg))")
                                    .font(.system(size: 14, weight: .semibold, design: .monospaced))
                                    .foregroundStyle(stat.avg > 60 ? Color.md3Error : Color.md3Primary)
                                Text("\(stat.count) turns · \(formatTime(stat.total)) total")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                    }
                }

                if !roundTimes.isEmpty {
                    Section("Round Times") {
                        let avgRound = roundTimes.reduce(0, +) / Double(roundTimes.count)
                        let fastest = roundTimes.min() ?? 0
                        let slowest = roundTimes.max() ?? 0

                        HStack {
                            Text("Rounds Completed")
                            Spacer()
                            Text("\(roundTimes.count)")
                                .font(.system(size: 16, weight: .bold, design: .rounded))
                        }
                        HStack {
                            Text("Average")
                            Spacer()
                            Text(formatTime(avgRound))
                                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.md3Primary)
                        }
                        HStack {
                            Text("Fastest Round")
                            Spacer()
                            Text(formatTime(fastest))
                                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.md3Primary)
                        }
                        HStack {
                            Text("Slowest Round")
                            Spacer()
                            Text(formatTime(slowest))
                                .font(.system(size: 14, weight: .semibold, design: .monospaced))
                                .foregroundStyle(Color.md3Error)
                        }
                    }
                }
            }
            .navigationTitle("Turn Times")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showStats = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Logic

    private func startQuickMode() {
        let first = firstPlayerName.trimmingCharacters(in: .whitespaces)
        playerNames = (0..<quickPlayerCount).map { i in
            if i == 0 && !first.isEmpty { return first }
            return "Player \(i + 1)"
        }
        if !first.isEmpty {
            Task { await ScoreKeeperPersistence.shared.saveName(first) }
        }
        firstPlayerIndex = 0
        currentTurnIndex = 0
        roundNumber = 1
        totalTurnTimes = [:]
        turnCounts = [:]
        roundTimes = []
        withAnimation { phase = .playing }
        if timerEnabled {
            roundStartTime = Date()
            startTurnTimer()
        }
    }

    private func addPlayer() {
        let name = newPlayerName.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty else { return }
        guard !playerNames.contains(where: { $0.caseInsensitiveCompare(name) == .orderedSame }) else {
            newPlayerName = ""
            filteredSuggestions = []
            return
        }
        playerNames.append(name)
        Task { await ScoreKeeperPersistence.shared.saveName(name) }
        newPlayerName = ""
        filteredSuggestions = []
        // Keep keyboard open for next player
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            playerNameFocused = true
        }
    }

    private func nextTurn() {
        guard !playerNames.isEmpty else { return }
        if timerEnabled { recordTurnTime() }
        withAnimation(.easeInOut(duration: 0.3)) {
            let nextIndex = (currentTurnIndex + 1) % playerNames.count
            if nextIndex == firstPlayerIndex {
                // Round complete — record round time
                if let roundStart = roundStartTime {
                    roundTimes.append(Date().timeIntervalSince(roundStart))
                }
                roundStartTime = Date()
                roundNumber += 1
                if rotateFirstPlayer {
                    // Advance the first player marker
                    firstPlayerIndex = (firstPlayerIndex + 1) % playerNames.count
                    // Start the new round from the new first player
                    currentTurnIndex = firstPlayerIndex
                } else {
                    currentTurnIndex = nextIndex
                }
            } else {
                currentTurnIndex = nextIndex
            }
        }
        if timerEnabled { startTurnTimer() }
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    private func previousTurn() {
        guard !playerNames.isEmpty else { return }
        if timerEnabled { stopTimer() }
        withAnimation(.easeInOut(duration: 0.3)) {
            currentTurnIndex = (currentTurnIndex - 1 + playerNames.count) % playerNames.count
            if currentTurnIndex == (firstPlayerIndex - 1 + playerNames.count) % playerNames.count {
                roundNumber = max(1, roundNumber - 1)
            }
        }
        if timerEnabled { startTurnTimer() }
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    private var slowestPlayerInfo: (name: String, avg: TimeInterval)? {
        guard !playerNames.isEmpty else { return nil }
        var slowest: (index: Int, avg: TimeInterval)?
        for i in 0..<playerNames.count {
            let total = totalTurnTimes[i] ?? 0
            let count = turnCounts[i] ?? 0
            guard count > 0 else { continue }
            let avg = total / Double(count)
            if slowest == nil || avg > slowest!.avg {
                slowest = (i, avg)
            }
        }
        guard let s = slowest else { return nil }
        return (playerNames[s.index], s.avg)
    }

    private func startPulse() {
        withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
            glowOpacity = 0.6
            pulseScale = 1.05
        }
    }

    // MARK: - Timer

    private func startTurnTimer() {
        turnStartTime = Date()
        currentTurnElapsed = 0
        hasTrashedThisTurn = false
        timerTask?.cancel()
        timerTask = Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 100_000_000)
                if let start = turnStartTime {
                    currentTurnElapsed = Date().timeIntervalSince(start)

                    // Trash talk trigger
                    if trashTalkEnabled && !hasTrashedThisTurn && currentTurnElapsed >= Double(trashTalkThreshold) {
                        hasTrashedThisTurn = true
                        speakTrashTalk()
                    }
                }
            }
        }
    }

    private func speakTrashTalk() {
        let utterance = AVSpeechUtterance(string: "You should just pass!")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95
        utterance.pitchMultiplier = 1.1
        utterance.volume = 1.0

        // Use best available voice
        let langPrefix = String(Locale.preferredLanguages.first?.prefix(2) ?? "en")
        if let voice = AVSpeechSynthesisVoice.speechVoices()
            .filter({ $0.language.hasPrefix(langPrefix) })
            .sorted(by: { $0.quality.rawValue > $1.quality.rawValue })
            .first {
            utterance.voice = voice
        }

        synthesizer.speak(utterance)
    }

    private func recordTurnTime() {
        if let start = turnStartTime {
            let elapsed = Date().timeIntervalSince(start)
            totalTurnTimes[currentTurnIndex, default: 0] += elapsed
            turnCounts[currentTurnIndex, default: 0] += 1
        }
    }

    private func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
        turnStartTime = nil
        currentTurnElapsed = 0
    }

    private func formatTime(_ interval: TimeInterval) -> String {
        let total = Int(interval)
        let mins = total / 60
        let secs = total % 60
        if mins > 0 {
            return String(format: "%d:%02d", mins, secs)
        }
        return "\(secs)s"
    }
}

enum TurnPhase {
    case setup, pickFirst, playing
}
