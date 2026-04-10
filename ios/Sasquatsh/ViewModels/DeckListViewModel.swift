import SwiftUI

@Observable
@MainActor
final class DeckListViewModel {
    var decks: [MtgDeck] = []
    var isLoading = false
    var error: String?
    var filterFormat: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadDecks() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            decks = try await services.mtgDecks.getMyDecks()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func deleteDeck(_ id: String) async {
        guard let services else { return }
        do {
            try await services.mtgDecks.deleteDeck(id: id)
            decks.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }

    var filteredDecks: [MtgDeck] {
        guard let filterFormat else { return decks }
        return decks.filter { $0.formatId == filterFormat }
    }
}
