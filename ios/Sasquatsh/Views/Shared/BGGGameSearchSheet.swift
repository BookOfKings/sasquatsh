import SwiftUI

enum GameSearchSource {
    case bgg
    case myCollection
    case hostCollection(userId: String, hostName: String)
}

struct GameSearchSourcePicker: View {
    var hostUserId: String?
    var hostName: String?
    var onSelect: (GameSearchSource) -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Suggest a Game")
                .font(.md3TitleLarge)
                .foregroundStyle(Color.md3OnSurface)

            Text("Where do you want to search?")
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)

            VStack(spacing: 12) {
                sourceButton(
                    icon: "person.fill",
                    title: "My Collection",
                    subtitle: "Pick from games you own",
                    color: Color.md3Primary
                ) {
                    onSelect(.myCollection)
                }

                if let hostUserId, let hostName {
                    sourceButton(
                        icon: "crown.fill",
                        title: "\(hostName)'s Collection",
                        subtitle: "Pick from the host's games",
                        color: .purple
                    ) {
                        onSelect(.hostCollection(userId: hostUserId, hostName: hostName))
                    }
                }

                Button { onSelect(.bgg) } label: {
                    HStack(spacing: 12) {
                        Image("bgg-logo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(height: 28)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Search BoardGameGeek")
                                .font(.md3BodyMedium)
                                .fontWeight(.medium)
                                .foregroundStyle(Color.md3OnSurface)
                            Text("Search the entire BGG database")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 14))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    .padding()
                    .background(Color.md3SurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                }
                .buttonStyle(.plain)
            }
        }
        .padding()
    }

    private func sourceButton(icon: String, title: String, subtitle: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundStyle(color)
                    .frame(width: 36, height: 36)
                    .background(color.opacity(0.12))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.md3BodyMedium)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.md3OnSurface)
                    Text(subtitle)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .padding()
            .background(Color.md3SurfaceContainerHigh)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Unified Game Search Sheet (Multi-Select)

struct GameSuggestSheet: View {
    var hostUserId: String?
    var hostName: String?
    var alreadySuggestedBggIds: Set<Int> = []
    var onSelect: ([BggSearchResult]) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var source: GameSearchSource?
    @State private var selected: [Int: BggSearchResult] = [:] // keyed by bggId

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                Group {
                    if let source {
                        switch source {
                        case .bgg:
                            BGGSearchView(selected: $selected, disabledBggIds: alreadySuggestedBggIds)
                        case .myCollection:
                            CollectionPickerView(source: .mine, selected: $selected, disabledBggIds: alreadySuggestedBggIds)
                        case .hostCollection(let userId, _):
                            CollectionPickerView(source: .user(userId), selected: $selected, disabledBggIds: alreadySuggestedBggIds)
                        }
                    } else {
                        GameSearchSourcePicker(
                            hostUserId: hostUserId,
                            hostName: hostName
                        ) { picked in
                            withAnimation { source = picked }
                        }
                    }
                }

                // Floating add button
                if !selected.isEmpty {
                    Button {
                        onSelect(Array(selected.values))
                        dismiss()
                    } label: {
                        Text("Add \(selected.count) Game\(selected.count == 1 ? "" : "s")")
                            .font(.md3LabelLarge)
                            .fontWeight(.semibold)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.md3Primary)
                            .clipShape(Capsule())
                            .shadow(color: Color.md3Primary.opacity(0.3), radius: 8, y: 4)
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.2), value: selected.count)
            .navigationTitle(source == nil ? "Suggest Games" : navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                if source != nil {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            withAnimation { source = nil }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                Text("Sources")
                            }
                            .font(.md3LabelMedium)
                        }
                    }
                }
                if !selected.isEmpty {
                    ToolbarItem(placement: .primaryAction) {
                        Text("\(selected.count)")
                            .font(.md3LabelMedium)
                            .foregroundStyle(.white)
                            .frame(width: 24, height: 24)
                            .background(Color.md3Primary)
                            .clipShape(Circle())
                    }
                }
            }
        }
    }

    private var navigationTitle: String {
        guard let source else { return "Suggest Games" }
        switch source {
        case .bgg: return "BoardGameGeek"
        case .myCollection: return "My Collection"
        case .hostCollection(_, let name): return "\(name)'s Games"
        }
    }
}

// MARK: - BGG Search View

struct BGGSearchView: View {
    @Binding var selected: [Int: BggSearchResult]
    var disabledBggIds: Set<Int> = []

    @Environment(\.services) private var services
    @State private var searchText = ""
    @State private var results: [BggSearchResult] = []
    @State private var isSearching = false
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 0) {
            Image("bgg-logo")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(height: 32)
                .padding(.top, 8)
                .padding(.bottom, 4)

            SearchBarView(text: $searchText, placeholder: "Search BoardGameGeek...")
                .padding(.horizontal)
                .padding(.bottom, 8)
                .onChange(of: searchText) { _, newValue in
                    searchTask?.cancel()
                    guard newValue.count >= 2 else {
                        results = []
                        return
                    }
                    searchTask = Task {
                        try? await Task.sleep(for: .milliseconds(300))
                        guard !Task.isCancelled else { return }
                        await search(query: newValue)
                    }
                }

            if isSearching {
                D20ProgressView(size: 32)
                    .padding()
            }

            gameList(results.map { ($0.bggId, $0) })
                .padding(.bottom, selected.isEmpty ? 0 : 60)
        }
    }

    private func gameList(_ items: [(Int, BggSearchResult)]) -> some View {
        List(items, id: \.0) { bggId, game in
            gameRow(game)
        }
        .listStyle(.plain)
    }

    private func gameRow(_ game: BggSearchResult) -> some View {
        let isSelected = selected[game.bggId] != nil
        let isDisabled = disabledBggIds.contains(game.bggId)
        return Button {
            guard !isDisabled else { return }
            if selected[game.bggId] != nil {
                selected.removeValue(forKey: game.bggId)
            } else {
                selected[game.bggId] = game
            }
        } label: {
            HStack(spacing: 12) {
                if isDisabled {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.3))
                } else {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundStyle(isSelected ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.4))
                }

                if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                    AsyncImage(url: imageURL) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.md3SurfaceVariant
                    }
                    .frame(width: 40, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                VStack(alignment: .leading) {
                    Text(game.name)
                        .font(.md3BodyMedium)
                        .foregroundStyle(isDisabled ? Color.md3OnSurfaceVariant.opacity(0.5) : Color.md3OnSurface)
                    if isDisabled {
                        Text("Already suggested")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.5))
                    } else if let year = game.yearPublished {
                        Text("(\(year))")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
                Spacer()
            }
            .opacity(isDisabled ? 0.6 : 1.0)
        }
        .disabled(isDisabled)
        .listRowBackground(isSelected ? Color.md3Primary.opacity(0.06) : Color.clear)
    }

    private func search(query: String) async {
        isSearching = true
        do {
            results = try await services.bgg.searchGames(query: query)
        } catch {
            results = []
        }
        isSearching = false
    }
}

// MARK: - Collection Picker View

enum CollectionSource {
    case mine
    case user(String)
}

struct CollectionPickerView: View {
    let source: CollectionSource
    @Binding var selected: [Int: BggSearchResult]
    var disabledBggIds: Set<Int> = []
    var onTap: ((BggSearchResult) -> Void)?

    @Environment(\.services) private var services
    @State private var games: [CollectionGame] = []
    @State private var isLoading = false
    @State private var error: String?
    @State private var searchText = ""
    @State private var addedBggIds: Set<Int> = [] // tracks tapped games in onTap mode

    private var filteredGames: [CollectionGame] {
        if searchText.isEmpty { return games }
        return games.filter { $0.gameName.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        VStack(spacing: 0) {
            if !games.isEmpty {
                SearchBarView(text: $searchText, placeholder: "Filter collection...")
                    .padding(.horizontal)
                    .padding(.vertical, 8)
            }

            if isLoading {
                Spacer()
                D20ProgressView(size: 40)
                Spacer()
            } else if let error {
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.title)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text(error)
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                .padding()
                Spacer()
            } else if filteredGames.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "tray")
                        .font(.title)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text(searchText.isEmpty ? "No games in this collection" : "No matching games")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                .padding()
                Spacer()
            } else {
                List(filteredGames) { game in
                    collectionRow(game)
                }
                .listStyle(.plain)
                .padding(.bottom, selected.isEmpty ? 0 : 60)
            }
        }
        .task { await loadCollection() }
    }

    private func collectionRow(_ game: CollectionGame) -> some View {
        let bggId = game.bggId ?? 0
        let isSelected = selected[bggId] != nil
        let isDisabled = onTap == nil && disabledBggIds.contains(bggId)
        let wasAdded = addedBggIds.contains(bggId)
        return Button {
            guard !isDisabled else { return }
            let result = BggSearchResult(
                bggId: bggId,
                name: game.gameName,
                yearPublished: game.yearPublished,
                thumbnailUrl: game.thumbnailUrl
            )
            if let onTap {
                onTap(result)
                withAnimation { addedBggIds.insert(bggId) }
                return
            }
            if isSelected {
                selected.removeValue(forKey: bggId)
            } else {
                selected[bggId] = BggSearchResult(
                    bggId: bggId,
                    name: game.gameName,
                    yearPublished: game.yearPublished,
                    thumbnailUrl: game.thumbnailUrl
                )
            }
        } label: {
            HStack(spacing: 12) {
                if onTap != nil {
                    if wasAdded {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundStyle(.green)
                    } else {
                        Image(systemName: "plus.circle")
                            .font(.system(size: 22))
                            .foregroundStyle(Color.md3Primary)
                    }
                } else if isDisabled {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.3))
                } else {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundStyle(isSelected ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.4))
                }

                if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                    AsyncImage(url: imageURL) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.md3SurfaceVariant
                    }
                    .frame(width: 40, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(game.gameName)
                        .font(.md3BodyMedium)
                        .foregroundStyle(isDisabled ? Color.md3OnSurfaceVariant.opacity(0.5) : Color.md3OnSurface)
                    if onTap != nil && wasAdded {
                        Text("Added")
                            .font(.md3LabelSmall)
                            .foregroundStyle(.green)
                    } else if isDisabled {
                        Text("Already suggested")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.5))
                    } else {
                        HStack(spacing: 8) {
                            if let min = game.minPlayers, let max = game.maxPlayers {
                                Label("\(min)-\(max)", systemImage: "person.2")
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            if let time = game.playingTime, time > 0 {
                                Label("\(time) min", systemImage: "clock")
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            if let year = game.yearPublished {
                                Text("(\(year))")
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer()
            }
            .opacity(isDisabled ? 0.6 : 1.0)
        }
        .disabled(isDisabled)
        .listRowBackground(
            wasAdded ? Color.green.opacity(0.06) :
            isSelected ? Color.md3Primary.opacity(0.06) :
            Color.clear
        )
    }

    private func loadCollection() async {
        isLoading = true
        error = nil
        do {
            switch source {
            case .mine:
                games = try await services.collections.getMyCollection()
            case .user(let userId):
                games = try await services.collections.getUserCollection(userId: userId)
            }
        } catch {
            self.error = "Could not load collection. It may be set to private."
        }
        isLoading = false
    }
}

// MARK: - Legacy wrapper (used elsewhere in the app)

struct BGGGameSearchSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    var onSelect: (BggSearchResult) -> Void

    @State private var searchText = ""
    @State private var results: [BggSearchResult] = []
    @State private var isSearching = false
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // BGG logo
                Image("bgg-logo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(height: 32)
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                SearchBarView(text: $searchText, placeholder: "Search BoardGameGeek...")
                    .padding()
                    .onChange(of: searchText) { _, newValue in
                        searchTask?.cancel()
                        guard newValue.count >= 2 else {
                            results = []
                            return
                        }
                        searchTask = Task {
                            try? await Task.sleep(for: .milliseconds(300))
                            guard !Task.isCancelled else { return }
                            await search(query: newValue)
                        }
                    }

                if isSearching {
                    D20ProgressView(size: 32)
                        .tint(Color.md3Primary)
                        .padding()
                }

                List(results) { game in
                    Button {
                        onSelect(game)
                        dismiss()
                    } label: {
                        HStack(spacing: 12) {
                            if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                                AsyncImage(url: imageURL) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.md3SurfaceVariant
                                }
                                .frame(width: 40, height: 40)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                            }
                            VStack(alignment: .leading) {
                                Text(game.name)
                                    .font(.md3TitleMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                                if let year = game.yearPublished {
                                    Text("(\(year))")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                            Spacer()
                            Image(systemName: "plus.circle")
                                .foregroundStyle(Color.md3Primary)
                        }
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("Search Games")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func search(query: String) async {
        isSearching = true
        do {
            results = try await services.bgg.searchGames(query: query)
        } catch {
            results = []
        }
        isSearching = false
    }
}
