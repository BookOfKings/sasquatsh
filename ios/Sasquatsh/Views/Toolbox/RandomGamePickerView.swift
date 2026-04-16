import SwiftUI

struct PickedGame {
    let name: String
    let yearPublished: Int?
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let bggRank: Int?

    init(cached: BggCachedGame) {
        name = cached.name
        yearPublished = cached.yearPublished
        thumbnailUrl = cached.thumbnailUrl
        minPlayers = cached.minPlayers
        maxPlayers = cached.maxPlayers
        bggRank = cached.bggRank
    }

    init(collection: CollectionGame) {
        name = collection.gameName
        yearPublished = collection.yearPublished
        thumbnailUrl = collection.thumbnailUrl
        minPlayers = collection.minPlayers
        maxPlayers = collection.maxPlayers
        bggRank = collection.bggRank
    }
}

enum GamePickerMode: String, CaseIterable {
    case customPick = "Custom"
    case myCollection = "My Collection"
    case top50 = "BGG Top 50"
    case random = "Random"
}

struct RandomGamePickerView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.services) private var services
    @State private var mode: GamePickerMode = .customPick
    @State private var selectedGameIds: Set<String> = []
    @State private var playerCount: Int = 4
    @State private var allGames: [BggCachedGame] = []
    @State private var myCollection: [CollectionGame] = []
    @State private var pickedGame: PickedGame?
    @State private var isLoading = false
    @State private var isLoadingGames = false
    @State private var error: String?
    @State private var showResult = false
    @State private var hasLoadedCache = false
    @State private var hasLoadedCollection = false

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "Random Game Picker") { dismiss() }
            ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 16) {
                    // Player count — always visible
                    HStack(spacing: 14) {
                        Image(systemName: "person.2")
                            .font(.system(size: 16))
                            .foregroundStyle(Color.md3Primary)

                        Text("Min. Players")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)

                        Spacer()

                        HStack(spacing: 12) {
                            Button {
                                if playerCount > 1 { playerCount -= 1 }
                            } label: {
                                Image(systemName: "minus.circle.fill")
                                    .font(.system(size: 26))
                                    .foregroundStyle(playerCount > 1 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                            }
                            .disabled(playerCount <= 1)

                            Text("\(playerCount)")
                                .font(.system(size: 28, weight: .bold, design: .rounded))
                                .foregroundStyle(Color.md3OnSurface)
                                .frame(width: 40)

                            Button {
                                if playerCount < 20 { playerCount += 1 }
                            } label: {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 26))
                                    .foregroundStyle(playerCount < 20 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                            }
                            .disabled(playerCount >= 20)
                        }
                    }
                    .padding(14)
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                    // Mode picker
                    VStack(alignment: .leading, spacing: 6) {
                        Text("From:")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(GamePickerMode.allCases, id: \.self) { m in
                                    Button {
                                        withAnimation { mode = m }
                                    } label: {
                                        HStack(spacing: 4) {
                                            if mode == m {
                                                Image(systemName: "checkmark")
                                                    .font(.system(size: 11, weight: .semibold))
                                            }
                                            Text(m.rawValue)
                                                .font(.md3LabelLarge)
                                        }
                                        .padding(.horizontal, 16)
                                        .frame(height: 32)
                                        .background(mode == m ? Color.md3SecondaryContainer : Color.clear)
                                        .foregroundStyle(mode == m ? Color.md3OnSecondaryContainer : Color.md3OnSurfaceVariant)
                                        .clipShape(Capsule())
                                        .overlay(
                                            Capsule().stroke(mode == m ? Color.clear : Color.md3Outline, lineWidth: 1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 20)

                    // Result — show above the game list
                    if showResult, let game = pickedGame {
                        gameResultCard(game)
                            .transition(.opacity.combined(with: .scale(scale: 0.9)))
                            .padding(.horizontal, 20)
                            .id("result")
                    } else if showResult && pickedGame == nil {
                        VStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle")
                                .font(.system(size: 32))
                                .foregroundStyle(Color.md3Error.opacity(0.6))
                            Text("No games found matching your criteria")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.top, 16)
                    }

                    // Custom pick — selectable game list
                    if mode == .customPick && hasLoadedCollection && !myCollection.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Select games to pick from")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                Spacer()
                                if !selectedGameIds.isEmpty {
                                    Text("\(selectedGameIds.count) selected")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundStyle(Color.md3Primary)
                                }
                            }

                            ForEach(myCollection) { game in
                                let isSelected = selectedGameIds.contains(game.id)
                                Button {
                                    if isSelected {
                                        selectedGameIds.remove(game.id)
                                    } else {
                                        selectedGameIds.insert(game.id)
                                    }
                                } label: {
                                    HStack(spacing: 10) {
                                        Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                            .font(.system(size: 20))
                                            .foregroundStyle(isSelected ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.4))

                                        if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                                            AsyncImage(url: url) { image in
                                                image.resizable().aspectRatio(contentMode: .fill)
                                            } placeholder: {
                                                Color.md3SurfaceVariant
                                            }
                                            .frame(width: 36, height: 36)
                                            .clipShape(RoundedRectangle(cornerRadius: 4))
                                        }

                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(game.gameName)
                                                .font(.system(size: 14, weight: .medium))
                                                .foregroundStyle(Color.md3OnSurface)
                                                .lineLimit(1)
                                            if let min = game.minPlayers, let max = game.maxPlayers {
                                                Text(min == max ? "\(min) players" : "\(min)–\(max) players")
                                                    .font(.system(size: 11))
                                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                            }
                                        }

                                        Spacer()
                                    }
                                    .padding(.vertical, 6)
                                    .padding(.horizontal, 10)
                                    .background(isSelected ? Color.md3PrimaryContainer.opacity(0.3) : Color.clear)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 20)
                    } else if mode == .customPick && hasLoadedCollection && myCollection.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "tray")
                                .font(.system(size: 32))
                                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.4))
                            Text("Your collection is empty")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Add games to your collection on the website")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                        }
                        .padding(.top, 16)
                    }

                    // Loading cache indicator
                    if isLoadingGames {
                        HStack(spacing: 8) {
                            ProgressView()
                                .tint(Color.md3Primary)
                            Text("Loading game library...")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding(.top, 8)
                    }

                    // Empty collection states for non-custom modes
                    if mode == .myCollection && hasLoadedCollection && myCollection.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "tray")
                                .font(.system(size: 32))
                                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.4))
                            Text("Your collection is empty")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Add games to your collection on the website")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                        }
                        .padding(.top, 16)
                    }

                    if isLoading {
                        ProgressView()
                            .tint(Color.md3Primary)
                            .scaleEffect(1.3)
                            .padding(.top, 16)
                    }

                    if let error {
                        Text(error)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Error)
                            .padding(.horizontal, 20)
                    }
                }
                .padding(.bottom, 16)
            }
            .onChange(of: showResult) { _, showing in
                if showing {
                    withAnimation {
                        proxy.scrollTo("result", anchor: .top)
                    }
                }
            }
            } // ScrollViewReader

            // Action button
            let isDisabled = isLoading || isLoadingGames || (mode == .customPick && selectedGameIds.isEmpty)
            Button {
                pickGame()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: showResult ? "arrow.clockwise" : "dice.fill")
                    Text(showResult ? "Pick Another" : "Pick a Game!")
                }
                .primaryButtonStyle()
                .opacity(isDisabled ? 0.4 : 1.0)
            }
            .disabled(isDisabled)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.md3SurfaceContainer)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await loadData()
        }
    }

    // MARK: - Game Result Card

    private func gameResultCard(_ game: PickedGame) -> some View {
        VStack(spacing: 0) {
            // Thumbnail
            if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fit)
                } placeholder: {
                    Color.md3SurfaceVariant
                        .frame(height: 160)
                        .overlay {
                            ProgressView()
                                .tint(Color.md3Primary)
                        }
                }
                .frame(maxHeight: 200)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium, style: .continuous))
            }

            VStack(spacing: 10) {
                Text(game.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(Color.md3OnSurface)
                    .multilineTextAlignment(.center)

                if let year = game.yearPublished {
                    Text("(\(String(year)))")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                HStack(spacing: 16) {
                    if let min = game.minPlayers, let max = game.maxPlayers {
                        Label {
                            Text(min == max ? "\(min) players" : "\(min)–\(max) players")
                                .font(.md3BodySmall)
                        } icon: {
                            Image(systemName: "person.2")
                                .foregroundStyle(Color.md3Primary)
                        }
                    }

                    if let rank = game.bggRank, rank > 0 {
                        Label {
                            Text("#\(rank) on BGG")
                                .font(.md3BodySmall)
                        } icon: {
                            Image(systemName: "trophy")
                                .foregroundStyle(Color.md3Tertiary)
                        }
                    }
                }
                .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .padding(16)
        }
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))
        .shadow(color: .black.opacity(0.08), radius: 6, y: 3)
    }

    // MARK: - Logic

    private func loadData() async {
        isLoadingGames = true

        // Load collection and cache in parallel
        async let collectionTask: () = loadCollection()
        async let cacheTask: () = loadGameCache()
        _ = await (collectionTask, cacheTask)

        isLoadingGames = false
    }

    private func loadCollection() async {
        guard !hasLoadedCollection else { return }
        do {
            myCollection = try await services.collections.getMyCollection()
            hasLoadedCollection = true
        } catch {
            print("Collection load error: \(error)")
            hasLoadedCollection = true // Mark loaded so we show empty state
        }
    }

    private func loadGameCache() async {
        guard !hasLoadedCache else { return }
        do {
            var page = 1
            var allFetched: [BggCachedGame] = []
            var hasMore = true
            while hasMore {
                let response = try await services.bgg.listCachedGames(page: page, limit: 100)
                allFetched.append(contentsOf: response.games)
                hasMore = page < response.totalPages
                page += 1
                if page > 10 { break }
            }
            allGames = allFetched
            hasLoadedCache = true
        } catch {
            self.error = "Failed to load game library"
        }
    }

    private func pickGame() {
        isLoading = true
        error = nil
        withAnimation {
            showResult = false
            pickedGame = nil
        }

        Task {
            try? await Task.sleep(for: .milliseconds(400))

            let picked: PickedGame?

            switch mode {
            case .customPick:
                let selected = myCollection.filter { selectedGameIds.contains($0.id) }
                picked = selected.randomElement().map { PickedGame(collection: $0) }

            case .myCollection:
                let filtered = myCollection.filter { game in
                    guard let max = game.maxPlayers else { return true }
                    return playerCount <= max
                }
                picked = filtered.randomElement().map { PickedGame(collection: $0) }

            case .top50:
                let filtered = allGames
                    .filter { game in
                        guard let max = game.maxPlayers else { return true }
                        return playerCount <= max
                    }
                    .filter { $0.bggRank != nil && $0.bggRank! > 0 }
                    .sorted { ($0.bggRank ?? Int.max) < ($1.bggRank ?? Int.max) }
                    .prefix(50)
                picked = Array(filtered).randomElement().map { PickedGame(cached: $0) }

            case .random:
                let filtered = allGames.filter { game in
                    guard let max = game.maxPlayers else { return true }
                    return playerCount <= max
                }
                picked = filtered.randomElement().map { PickedGame(cached: $0) }
            }

            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                pickedGame = picked
                showResult = true
                isLoading = false
            }
        }
    }

}
