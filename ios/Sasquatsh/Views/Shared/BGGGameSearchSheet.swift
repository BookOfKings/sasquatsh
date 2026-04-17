import SwiftUI

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
                        HStack {
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
