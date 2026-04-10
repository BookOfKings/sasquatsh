import SwiftUI

struct MyDecksView: View {
    @Environment(\.services) private var services
    @State private var vm = DeckListViewModel()
    @State private var showCreateDeck = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.decks.isEmpty {
                LoadingView()
            } else if vm.decks.isEmpty {
                EmptyStateView(
                    icon: "rectangle.stack",
                    title: "No Decks Yet",
                    message: "Build your first MTG deck",
                    buttonTitle: "Create Deck",
                    action: { showCreateDeck = true }
                )
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(vm.filteredDecks) { deck in
                        NavigationLink {
                            DeckBuilderView(deck: deck)
                        } label: {
                            deckCard(deck)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("My Decks")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showCreateDeck = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(Color.md3Primary)
                }
            }
        }
        .sheet(isPresented: $showCreateDeck) {
            NavigationStack {
                DeckBuilderView(deck: nil)
            }
        }
        .refreshable { await vm.loadDecks() }
        .task {
            vm.configure(services: services)
            await vm.loadDecks()
        }
    }

    private func deckCard(_ deck: MtgDeck) -> some View {
        HStack(spacing: 12) {
            // Commander or first card image
            if let commander = deck.commander, let url = commander.smallImageUrl.flatMap({ URL(string: $0) }) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 50, height: 70)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            } else {
                RoundedRectangle(cornerRadius: MD3Shape.small)
                    .fill(Color.md3SurfaceVariant)
                    .frame(width: 50, height: 70)
                    .overlay {
                        Image(systemName: "rectangle.stack")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(deck.name)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                    .lineLimit(1)

                HStack(spacing: 8) {
                    if let format = deck.formatId {
                        BadgeView(text: format.capitalized, color: .md3SecondaryContainer)
                    }
                    Text("\(deck.cardCount ?? 0) cards")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    if let pl = deck.powerLevel {
                        Text("PL \(pl)")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .padding()
        .cardStyle()
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                Task { await vm.deleteDeck(deck.id) }
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }
}
