import SwiftUI

struct MyCollectionView: View {
    @Environment(\.services) private var services
    @State private var myGames: [CollectionGame] = []
    @State private var topGames: [CollectionGame] = []
    @State private var searchResults: [BggSearchResult] = []
    @State private var searchQuery = ""
    @State private var isLoading = true
    @State private var isSearching = false
    @State private var error: String?
    @State private var activeTab = 0 // 0=My Games, 1=Top 50, 2=Search
    @State private var filterText = ""
    @State private var pendingAdds: Set<Int> = []
    @State private var pendingRemoves: Set<Int> = []

    private var ownedBggIds: Set<Int> {
        Set(myGames.compactMap(\.bggId))
    }

    private var filteredGames: [CollectionGame] {
        if filterText.isEmpty { return myGames }
        return myGames.filter { $0.gameName.localizedCaseInsensitiveContains(filterText) }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Tab picker
            Picker("", selection: $activeTab) {
                Text("My Games (\(myGames.count))").tag(0)
                Text("Top 50").tag(1)
                Text("Search BGG").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            switch activeTab {
            case 0:
                myGamesTab
            case 1:
                topGamesTab
            case 2:
                searchTab
            default:
                EmptyView()
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("My Collection")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadCollection()
        }
    }

    // MARK: - My Games Tab

    private var myGamesTab: some View {
        VStack(spacing: 0) {
            // Filter
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                TextField("Filter games...", text: $filterText)
            }
            .padding(10)
            .background(Color.md3Surface)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if isLoading {
                Spacer()
                    D20ProgressView(size: 32)
                Spacer()
            } else if myGames.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "tray")
                        .font(.system(size: 40))
                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.3))
                    Text("Your collection is empty")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Search BGG to add games")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                    Button {
                        activeTab = 2
                    } label: {
                        Text("Search Games")
                            .primaryButtonStyle()
                    }
                    .frame(width: 200)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(filteredGames) { game in
                            gameRow(game: game, isOwned: true, isPending: pendingRemoves.contains(game.bggId ?? 0))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
            }
        }
    }

    // MARK: - Top 50 Tab

    private var topGamesTab: some View {
        Group {
            if topGames.isEmpty && isLoading {
                VStack {
                    Spacer()
                    D20ProgressView(size: 32)
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(topGames) { game in
                            let bggId = game.bggId ?? 0
                            gameRow(
                                game: game,
                                isOwned: ownedBggIds.contains(bggId),
                                isPending: pendingAdds.contains(bggId) || pendingRemoves.contains(bggId),
                                showRank: true
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                    .padding(.top, 8)
                }
            }
        }
    }

    // MARK: - Search Tab

    private var searchTab: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                TextField("Search BoardGameGeek...", text: $searchQuery)
                    .onSubmit { searchBGG() }
                if isSearching {
                    D20ProgressView(size: 20)
                }
                if !searchQuery.isEmpty {
                    Button { searchQuery = ""; searchResults = [] } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
            .padding(10)
            .background(Color.md3Surface)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if searchResults.isEmpty && !searchQuery.isEmpty && !isSearching {
                Spacer()
                Text("No results")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(searchResults) { result in
                            searchResultRow(result)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
            }
        }
        .onChange(of: searchQuery) { _, newValue in
            guard !newValue.isEmpty else {
                searchResults = []
                return
            }
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                guard searchQuery == newValue else { return }
                searchBGG()
            }
        }
    }

    // MARK: - Game Row

    private func gameRow(game: CollectionGame, isOwned: Bool, isPending: Bool, showRank: Bool = false) -> some View {
        HStack(spacing: 12) {
            // Thumbnail
            if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 50, height: 50)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            } else {
                RoundedRectangle(cornerRadius: MD3Shape.small)
                    .fill(Color.md3SurfaceVariant)
                    .frame(width: 50, height: 50)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(game.gameName)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                    .lineLimit(1)

                HStack(spacing: 12) {
                    if let year = game.yearPublished {
                        Text(String(year))
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    if let min = game.minPlayers, let max = game.maxPlayers {
                        Text(min == max ? "\(min)p" : "\(min)-\(max)p")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    if showRank, let rank = game.bggRank, rank > 0 {
                        Text("#\(rank)")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Tertiary)
                    }
                }
            }

            Spacer()

            // Add/Remove button
            if isPending {
                D20ProgressView(size: 20)
            } else {
                Button {
                    Task { await toggleGame(bggId: game.bggId ?? 0, game: game, isOwned: isOwned) }
                } label: {
                    Image(systemName: isOwned ? "checkmark.circle.fill" : "plus.circle")
                        .font(.system(size: 24))
                        .foregroundStyle(isOwned ? Color.md3Primary : Color.md3OnSurfaceVariant)
                }
            }
        }
        .padding(10)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
    }

    private func searchResultRow(_ result: BggSearchResult) -> some View {
        let isOwned = ownedBggIds.contains(result.bggId)
        let isPending = pendingAdds.contains(result.bggId) || pendingRemoves.contains(result.bggId)

        return HStack(spacing: 12) {
            if let urlStr = result.thumbnailUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 50, height: 50)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            } else {
                RoundedRectangle(cornerRadius: MD3Shape.small)
                    .fill(Color.md3SurfaceVariant)
                    .frame(width: 50, height: 50)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(result.name)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                    .lineLimit(1)
                if let year = result.yearPublished {
                    Text(String(year))
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }

            Spacer()

            if isPending {
                D20ProgressView(size: 20)
            } else {
                Button {
                    Task { await addFromSearch(result) }
                } label: {
                    Image(systemName: isOwned ? "checkmark.circle.fill" : "plus.circle")
                        .font(.system(size: 24))
                        .foregroundStyle(isOwned ? Color.md3Primary : Color.md3OnSurfaceVariant)
                }
                .disabled(isOwned)
            }
        }
        .padding(10)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
    }

    // MARK: - Actions

    private func loadCollection() async {
        isLoading = true
        do {
            async let myTask = services.collections.getMyCollection()
            async let topTask = services.collections.getTopGames()
            let (my, top) = try await (myTask, topTask)
            myGames = my.sorted { $0.gameName < $1.gameName }
            topGames = top
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func searchBGG() {
        guard !searchQuery.isEmpty else { return }
        isSearching = true
        Task {
            do {
                searchResults = try await services.bgg.searchGames(query: searchQuery)
            } catch {
                self.error = error.localizedDescription
            }
            isSearching = false
        }
    }

    private func toggleGame(bggId: Int, game: CollectionGame, isOwned: Bool) async {
        if isOwned {
            pendingRemoves.insert(bggId)
            do {
                try await services.collections.removeGame(bggId: bggId)
                myGames.removeAll { $0.bggId == bggId }
            } catch {
                self.error = error.localizedDescription
            }
            pendingRemoves.remove(bggId)
        } else {
            pendingAdds.insert(bggId)
            do {
                let input = AddCollectionGameInput(
                    bggId: bggId,
                    name: game.gameName,
                    thumbnailUrl: game.thumbnailUrl,
                    imageUrl: nil,
                    minPlayers: game.minPlayers,
                    maxPlayers: game.maxPlayers,
                    playingTime: game.playingTime,
                    yearPublished: game.yearPublished,
                    bggRank: game.bggRank,
                    averageRating: game.averageRating
                )
                let added = try await services.collections.addGame(input)
                myGames.append(contentsOf: added)
                myGames.sort { $0.gameName < $1.gameName }
            } catch {
                self.error = error.localizedDescription
            }
            pendingAdds.remove(bggId)
        }
    }

    private func addFromSearch(_ result: BggSearchResult) async {
        let bggId = result.bggId
        pendingAdds.insert(bggId)
        do {
            let input = AddCollectionGameInput(
                bggId: bggId,
                name: result.name,
                thumbnailUrl: result.thumbnailUrl,
                imageUrl: nil,
                minPlayers: nil,
                maxPlayers: nil,
                playingTime: nil,
                yearPublished: result.yearPublished,
                bggRank: nil,
                averageRating: nil
            )
            let added = try await services.collections.addGame(input)
            myGames.append(contentsOf: added)
            myGames.sort { $0.gameName < $1.gameName }
        } catch {
            self.error = error.localizedDescription
        }
        pendingAdds.remove(bggId)
    }
}
