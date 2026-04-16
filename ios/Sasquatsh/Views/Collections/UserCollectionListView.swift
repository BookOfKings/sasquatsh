import SwiftUI

enum CollectionViewMode: String {
    case cards, list
}

struct UserCollectionListView: View {
    let games: [CollectionGame]
    let userName: String
    @State private var searchText = ""
    @State private var viewMode: CollectionViewMode = .cards

    private var filteredGames: [CollectionGame] {
        if searchText.isEmpty { return games }
        return games.filter { $0.gameName.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Search + view toggle
            HStack(spacing: 10) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    TextField("Search \(games.count) games...", text: $searchText)
                    if !searchText.isEmpty {
                        Button { searchText = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }
                .padding(10)
                .background(Color.md3Surface)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))

                // View toggle
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        viewMode = viewMode == .cards ? .list : .cards
                    }
                } label: {
                    Image(systemName: viewMode == .cards ? "list.bullet" : "square.grid.2x2")
                        .font(.system(size: 18))
                        .frame(width: 40, height: 40)
                        .foregroundStyle(Color.md3OnSecondaryContainer)
                        .background(Color.md3SecondaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if filteredGames.isEmpty {
                Spacer()
                Text(searchText.isEmpty ? "No games" : "No matches")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Spacer()
            } else {
                ScrollView {
                    switch viewMode {
                    case .cards:
                        cardGrid
                    case .list:
                        listView
                    }
                }
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("\(userName)'s Games")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Card Grid

    private var cardGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
            ForEach(filteredGames) { game in
                VStack(spacing: 0) {
                    // Thumbnail
                    if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                        AsyncImage(url: url) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Color.md3SurfaceVariant
                        }
                        .frame(height: 100)
                        .clipped()
                    } else {
                        Color.md3SurfaceVariant
                            .frame(height: 100)
                            .overlay {
                                Image(systemName: "dice")
                                    .font(.system(size: 24))
                                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.3))
                            }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(game.gameName)
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurface)
                            .lineLimit(2)

                        HStack(spacing: 6) {
                            if let min = game.minPlayers, let max = game.maxPlayers {
                                Label(min == max ? "\(min)p" : "\(min)-\(max)p", systemImage: "person.2")
                                    .font(.system(size: 10))
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            if let time = game.playingTime, time > 0 {
                                Label("\(time)m", systemImage: "clock")
                                    .font(.system(size: 10))
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }

                        if let rank = game.bggRank, rank > 0 {
                            Text("#\(rank) BGG")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(Color.md3Tertiary)
                        }
                    }
                    .padding(8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .background(Color.md3Surface)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    // MARK: - List View

    private var listView: some View {
        LazyVStack(spacing: 4) {
            ForEach(filteredGames) { game in
                HStack(spacing: 12) {
                    if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                        AsyncImage(url: url) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Color.md3SurfaceVariant
                        }
                        .frame(width: 40, height: 40)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                    } else {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.md3SurfaceVariant)
                            .frame(width: 40, height: 40)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(game.gameName)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurface)
                            .lineLimit(1)

                        HStack(spacing: 8) {
                            if let year = game.yearPublished {
                                Text(String(year))
                                    .font(.md3BodySmall)
                            }
                            if let min = game.minPlayers, let max = game.maxPlayers {
                                Text(min == max ? "\(min)p" : "\(min)-\(max)p")
                                    .font(.md3BodySmall)
                            }
                            if let time = game.playingTime, time > 0 {
                                Text("\(time) min")
                                    .font(.md3BodySmall)
                            }
                        }
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    }

                    Spacer()

                    if let rank = game.bggRank, rank > 0 {
                        Text("#\(rank)")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Tertiary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .padding(.bottom, 16)
        }
    }
}
