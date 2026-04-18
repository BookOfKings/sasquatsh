import SwiftUI
import AudioToolbox

// MARK: - Models

struct ScoringPlayer: Identifiable, Codable {
    let id: UUID
    var name: String
    var score: Int

    init(name: String, score: Int = 0) {
        self.id = UUID()
        self.name = name
        self.score = score
    }
}

struct ScoreAction: Identifiable {
    let id = UUID()
    let playerId: UUID
    let playerName: String
    let amount: Int
    let timestamp: Date
}

enum ScoringPhase {
    case setup, playing, finished
}

// MARK: - Persistence

actor ScoreKeeperPersistence {
    static let shared = ScoreKeeperPersistence()

    private var docsDir: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }

    private var namesURL: URL { docsDir.appendingPathComponent("scorekeeper_names.json") }
    private var gamesURL: URL { docsDir.appendingPathComponent("scorekeeper_games.json") }

    func loadSavedNames() -> [String] {
        guard let data = try? Data(contentsOf: namesURL),
              let names = try? JSONDecoder().decode([String].self, from: data) else { return [] }
        return names
    }

    func saveName(_ name: String) {
        var names = loadSavedNames()
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty && !names.contains(where: { $0.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            names.append(trimmed)
            names.sort()
            try? JSONEncoder().encode(names).write(to: namesURL, options: .atomic)
        }
    }

    func loadSavedGames() -> [String] {
        guard let data = try? Data(contentsOf: gamesURL),
              let games = try? JSONDecoder().decode([String].self, from: data) else { return [] }
        return games
    }

    func saveGameName(_ name: String) {
        var games = loadSavedGames()
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty && !games.contains(where: { $0.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            games.insert(trimmed, at: 0) // most recent first
            if games.count > 20 { games = Array(games.prefix(20)) }
            try? JSONEncoder().encode(games).write(to: gamesURL, options: .atomic)
        }
    }
}

// MARK: - View

struct ScoreKeeperView: View {
    @State private var phase: ScoringPhase = .setup
    @State private var players: [ScoringPlayer] = []
    @State private var gameName = ""
    @State private var newPlayerName = ""
    @State private var savedNames: [String] = []
    @State private var savedGames: [String] = []
    @State private var filteredSuggestions: [String] = []
    @State private var filteredGameSuggestions: [String] = []
    @State private var scoreInput: [UUID: String] = [:]
    @State private var highestWins = true
    @State private var history: [ScoreAction] = []
    @State private var showHistory = false
    @State private var showEndConfirm = false
    @State private var showPlayerHistory = false
    @State private var playerHistoryId: UUID?

    var body: some View {
        VStack(spacing: 0) {
            switch phase {
            case .setup:
                setupView
            case .playing:
                scoringView
            case .finished:
                resultsView
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Score Keeper")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            savedNames = await ScoreKeeperPersistence.shared.loadSavedNames()
            savedGames = await ScoreKeeperPersistence.shared.loadSavedGames()
        }
    }

    // MARK: - Setup

    private var setupView: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 16) {
                    // Game name with autocomplete
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Game Name")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("e.g. Flip 7, Catan, Uno...", text: $gameName)
                            .padding(10)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .onChange(of: gameName) { _, value in
                                if value.isEmpty {
                                    filteredGameSuggestions = []
                                } else {
                                    filteredGameSuggestions = savedGames.filter {
                                        $0.localizedCaseInsensitiveContains(value)
                                    }.prefix(5).map { $0 }
                                }
                            }

                        // Game suggestions
                        if !filteredGameSuggestions.isEmpty {
                            VStack(spacing: 0) {
                                ForEach(filteredGameSuggestions, id: \.self) { name in
                                    Button {
                                        gameName = name
                                        filteredGameSuggestions = []
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

                        // Recent games quick select
                        if gameName.isEmpty && !savedGames.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 6) {
                                    ForEach(savedGames.prefix(8), id: \.self) { game in
                                        Button {
                                            gameName = game
                                        } label: {
                                            Text(game)
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
                    .padding(.top, 12)

                    // Scoring mode
                    HStack {
                        Text("Winner")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Spacer()
                        Picker("", selection: $highestWins) {
                            Text("Highest Score").tag(true)
                            Text("Lowest Score").tag(false)
                        }
                        .pickerStyle(.segmented)
                        .frame(width: 240)
                    }
                    .padding(.horizontal, 16)

                    // Add player
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Add Players")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)

                        HStack(spacing: 8) {
                            TextField("Player name", text: $newPlayerName)
                                .padding(10)
                                .background(Color.md3Surface)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                                .onChange(of: newPlayerName) { _, value in
                                    if value.isEmpty {
                                        filteredSuggestions = []
                                    } else {
                                        filteredSuggestions = savedNames.filter { savedName in
                                            savedName.localizedCaseInsensitiveContains(value) &&
                                            !players.contains(where: { p in p.name.caseInsensitiveCompare(savedName) == .orderedSame })
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

                        // Name suggestions
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
                    if !players.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Players (\(players.count))")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3OnSurfaceVariant)

                            ForEach(players) { player in
                                HStack {
                                    Circle()
                                        .fill(playerColor(for: player))
                                        .frame(width: 8, height: 8)
                                    Text(player.name)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                    Button {
                                        players.removeAll { $0.id == player.id }
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
                .padding(.bottom, 16)
            }

            // Start button
            Button {
                if !gameName.trimmingCharacters(in: .whitespaces).isEmpty {
                    Task { await ScoreKeeperPersistence.shared.saveGameName(gameName) }
                }
                history = []
                withAnimation { phase = .playing }
            } label: {
                Text("Start Game")
                    .primaryButtonStyle()
            }
            .disabled(players.count < 2)
            .opacity(players.count < 2 ? 0.4 : 1)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.md3SurfaceContainer)
        }
    }

    // MARK: - Scoring

    private var scoringView: some View {
        VStack(spacing: 0) {
            // Game header with undo
            HStack {
                if !gameName.isEmpty {
                    Text(gameName)
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                }
                Spacer()

                if !history.isEmpty {
                    Button {
                        showHistory = true
                    } label: {
                        Text("\(history.count)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color.md3OnSecondaryContainer)
                            .frame(width: 20, height: 20)
                            .background(Color.md3SecondaryContainer)
                            .clipShape(Circle())
                            .overlay(alignment: .topTrailing) {}
                    }

                    Button {
                        undoLast()
                    } label: {
                        Label("Undo", systemImage: "arrow.uturn.backward")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach($players) { $player in
                        VStack(spacing: 8) {
                            // Top row: name + score
                            HStack {
                                Circle()
                                    .fill(playerColor(for: player))
                                    .frame(width: 12, height: 12)

                                Text(player.name)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundStyle(Color.md3OnSurface)
                                    .lineLimit(1)

                                Spacer()

                                Button {
                                    playerHistoryId = player.id
                                    showPlayerHistory = true
                                } label: {
                                    HStack(spacing: 4) {
                                        Text("\(player.score)")
                                            .font(.system(size: 32, weight: .bold, design: .rounded))
                                            .foregroundStyle(Color.md3OnSurface)
                                        Image(systemName: "clock.arrow.circlepath")
                                            .font(.system(size: 12))
                                            .foregroundStyle(Color.md3OnSurfaceVariant)
                                    }
                                }
                            }

                            // Bottom row: buttons + custom input
                            HStack(spacing: 6) {
                                scoreButton("-5", amount: -5, player: $player)
                                scoreButton("-1", amount: -1, player: $player)
                                scoreButton("+1", amount: 1, player: $player)
                                scoreButton("+5", amount: 5, player: $player)

                                Spacer()

                                TextField("±", text: Binding(
                                    get: { scoreInput[player.id] ?? "" },
                                    set: { scoreInput[player.id] = $0 }
                                ))
                                .keyboardType(.numbersAndPunctuation)
                                .frame(width: 56)
                                .multilineTextAlignment(.center)
                                .padding(8)
                                .background(Color.md3SurfaceContainerHigh)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                .onSubmit {
                                    if let val = Int(scoreInput[player.id] ?? "") {
                                        addScore(to: &player, amount: val)
                                        scoreInput[player.id] = ""
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        .contextMenu {
                            Button(role: .destructive) {
                                withAnimation {
                                    players.removeAll { $0.id == player.id }
                                }
                            } label: {
                                Label("Remove \(player.name)", systemImage: "trash")
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            Button {
                showEndConfirm = true
            } label: {
                Text("End Game")
                    .primaryButtonStyle()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.md3SurfaceContainer)
        }
        .alert("End Game?", isPresented: $showEndConfirm) {
            Button("Cancel", role: .cancel) {}
            Button("End Game") {
                withAnimation { phase = .finished }
            }
        } message: {
            Text("Are you sure? You can go back to scoring if you hit it by mistake.")
        }
        .sheet(isPresented: $showHistory) {
            historySheet
        }
        .sheet(isPresented: $showPlayerHistory) {
            if let playerId = playerHistoryId {
                playerHistorySheet(playerId: playerId)
            }
        }
    }

    private func scoreButton(_ label: String, amount: Int, player: Binding<ScoringPlayer>) -> some View {
        let isNegative = amount < 0
        return Button {
            addScore(to: &player.wrappedValue, amount: amount)
        } label: {
            Text(label)
                .font(.system(size: 16, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(isNegative ? Color.md3ErrorContainer.opacity(amount == -5 ? 1 : 0.5) : Color.md3PrimaryContainer.opacity(amount == 5 ? 1 : 0.5))
                .foregroundStyle(isNegative ? Color.md3Error : Color.md3Primary)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        }
    }

    private func addScore(to player: inout ScoringPlayer, amount: Int) {
        history.append(ScoreAction(
            playerId: player.id,
            playerName: player.name,
            amount: amount,
            timestamp: Date()
        ))
        player.score += amount
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    private func undoLast() {
        guard let last = history.popLast() else { return }
        if let idx = players.firstIndex(where: { $0.id == last.playerId }) {
            players[idx].score -= last.amount
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        }
    }

    // MARK: - History Sheet

    private var historySheet: some View {
        NavigationStack {
            List {
                ForEach(history.reversed()) { action in
                    HStack {
                        Circle()
                            .fill(playerColorById(action.playerId))
                            .frame(width: 8, height: 8)
                        Text(action.playerName)
                            .font(.md3BodyMedium)
                        Spacer()
                        Text(action.amount > 0 ? "+\(action.amount)" : "\(action.amount)")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                            .foregroundStyle(action.amount > 0 ? Color.md3Primary : Color.md3Error)
                        Text(action.timestamp.formatted(date: .omitted, time: .shortened))
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
            .navigationTitle("Score History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showHistory = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func playerHistorySheet(playerId: UUID) -> some View {
        let playerName = players.first(where: { $0.id == playerId })?.name ?? "Player"
        let playerActions = history.filter { $0.playerId == playerId }

        return NavigationStack {
            Group {
                if playerActions.isEmpty {
                    VStack {
                        Spacer()
                        Text("No score changes yet")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Spacer()
                    }
                } else {
                    ScrollView {
                        VStack(spacing: 6) {
                            // Running total computed per entry
                            let reversed = Array(playerActions.enumerated()).reversed()
                            ForEach(Array(reversed), id: \.element.id) { index, action in
                                let runningTotal = playerActions.prefix(index + 1).reduce(0) { $0 + $1.amount }
                                HStack(spacing: 12) {
                                    Text(action.amount > 0 ? "+\(action.amount)" : "\(action.amount)")
                                        .font(.system(size: 20, weight: .bold, design: .rounded))
                                        .foregroundStyle(action.amount > 0 ? Color.md3Primary : Color.md3Error)
                                        .frame(width: 55, alignment: .leading)

                                    Text("= \(runningTotal)")
                                        .font(.system(size: 16, weight: .medium, design: .rounded))
                                        .foregroundStyle(Color.md3OnSurfaceVariant)

                                    Spacer()

                                    Text(action.timestamp.formatted(date: .omitted, time: .shortened))
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.6))

                                    Button {
                                        removeHistoryEntry(action)
                                    } label: {
                                        Image(systemName: "xmark.circle.fill")
                                            .font(.system(size: 20))
                                            .foregroundStyle(Color.md3Error.opacity(0.6))
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color.md3Surface)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }
                }
            }
            .navigationTitle("\(playerName)'s History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showPlayerHistory = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func removeHistoryEntry(_ action: ScoreAction) {
        // Reverse the score change
        if let idx = players.firstIndex(where: { $0.id == action.playerId }) {
            players[idx].score -= action.amount
        }
        // Remove from history
        history.removeAll { $0.id == action.id }
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
    }

    // MARK: - Results

    private var resultsView: some View {
        let sorted = players.sorted { highestWins ? $0.score > $1.score : $0.score < $1.score }
        let winner = sorted.first

        return VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 20) {
                    if !gameName.isEmpty {
                        Text(gameName)
                            .font(.md3TitleMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .padding(.top, 8)
                    }

                    if let winner {
                        VStack(spacing: 8) {
                            Image(systemName: "crown.fill")
                                .font(.system(size: 40))
                                .foregroundStyle(.yellow)

                            Text(winner.name)
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(Color.md3OnSurface)

                            Text("\(winner.score) points")
                                .font(.md3TitleLarge)
                                .foregroundStyle(Color.md3Primary)

                            Text("Winner!")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3OnTertiaryContainer)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 4)
                                .background(Color.md3TertiaryContainer)
                                .clipShape(Capsule())
                        }
                        .padding(.top, 16)
                    }

                    VStack(spacing: 6) {
                        ForEach(Array(sorted.enumerated()), id: \.element.id) { index, player in
                            HStack {
                                Text("#\(index + 1)")
                                    .font(.system(size: 16, weight: .bold, design: .rounded))
                                    .foregroundStyle(index == 0 ? Color.md3Tertiary : Color.md3OnSurfaceVariant)
                                    .frame(width: 30)

                                Circle()
                                    .fill(playerColor(for: player))
                                    .frame(width: 8, height: 8)

                                Text(player.name)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)

                                Spacer()

                                Text("\(player.score)")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundStyle(index == 0 ? Color.md3Primary : Color.md3OnSurface)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(index == 0 ? Color.md3PrimaryContainer.opacity(0.3) : Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.bottom, 16)
            }

            VStack(spacing: 8) {
                // Go back to scoring (undo end game)
                Button {
                    withAnimation { phase = .playing }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.uturn.backward")
                        Text("Back to Scoring")
                    }
                    .secondaryButtonStyle()
                }

                Button {
                    for i in players.indices { players[i].score = 0 }
                    scoreInput = [:]
                    history = []
                    withAnimation { phase = .playing }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                        Text("Play Again (Same Players)")
                    }
                    .primaryButtonStyle()
                }

                Button {
                    players = []
                    scoreInput = [:]
                    gameName = ""
                    history = []
                    withAnimation { phase = .setup }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "plus.circle")
                        Text("New Game")
                    }
                    .outlinedButtonStyle()
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.md3SurfaceContainer)
        }
    }

    // MARK: - Helpers

    private func addPlayer() {
        let name = newPlayerName.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty else { return }
        guard !players.contains(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) else {
            newPlayerName = ""
            filteredSuggestions = []
            return
        }
        players.append(ScoringPlayer(name: name))
        Task { await ScoreKeeperPersistence.shared.saveName(name) }
        newPlayerName = ""
        filteredSuggestions = []
    }

    private let playerColors: [Color] = [
        .red, .blue, .green, .orange, .purple, .cyan, .pink, .yellow, .mint, .indigo
    ]

    private func playerColor(for player: ScoringPlayer) -> Color {
        guard let index = players.firstIndex(where: { $0.id == player.id }) else { return .gray }
        return playerColors[index % playerColors.count]
    }

    private func playerColorById(_ id: UUID) -> Color {
        guard let index = players.firstIndex(where: { $0.id == id }) else { return .gray }
        return playerColors[index % playerColors.count]
    }
}
