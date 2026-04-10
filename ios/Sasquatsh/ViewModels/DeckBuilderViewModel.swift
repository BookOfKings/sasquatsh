import SwiftUI

@Observable
@MainActor
final class DeckBuilderViewModel {
    // Deck metadata
    var deckName = ""
    var formatId: String? = nil
    var description = ""
    var powerLevel: Int? = nil
    var isPublic = false
    var commanderScryfallId: String? = nil

    // Card search
    var searchQuery = ""
    var searchResults: [ScryfallCard] = []
    var isSearching = false
    private var searchTask: Task<Void, Never>?

    // Deck cards
    var cards: [MtgDeckCard] = []

    // State
    var isLoading = false
    var isSaving = false
    var error: String?
    var isEditing = false

    private var deckId: String?
    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    // MARK: - Card Search

    func searchCards(query: String) {
        searchTask?.cancel()
        guard query.count >= 2, let services else {
            searchResults = []
            return
        }
        searchTask = Task {
            try? await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled else { return }
            isSearching = true
            do {
                searchResults = try await services.scryfall.searchCards(query: query)
            } catch {
                if !Task.isCancelled { searchResults = [] }
            }
            isSearching = false
        }
    }

    func clearSearch() {
        searchQuery = ""
        searchResults = []
        searchTask?.cancel()
    }

    // MARK: - Deck Management

    func addCard(_ card: ScryfallCard, board: String = "main") {
        if let index = cards.firstIndex(where: { $0.scryfallId == card.scryfallId && $0.board == board }) {
            // Increment quantity
            let existing = cards[index]
            cards[index] = MtgDeckCard(
                id: existing.id, deckId: existing.deckId,
                scryfallId: existing.scryfallId,
                quantity: existing.quantity + 1,
                board: existing.board, card: existing.card
            )
        } else {
            cards.append(MtgDeckCard(
                id: UUID().uuidString, deckId: deckId,
                scryfallId: card.scryfallId,
                quantity: 1, board: board, card: card
            ))
        }
    }

    func removeCard(at index: Int) {
        guard cards.indices.contains(index) else { return }
        cards.remove(at: index)
    }

    func updateQuantity(at index: Int, quantity: Int) {
        guard cards.indices.contains(index) else { return }
        if quantity <= 0 {
            cards.remove(at: index)
        } else {
            let card = cards[index]
            cards[index] = MtgDeckCard(
                id: card.id, deckId: card.deckId,
                scryfallId: card.scryfallId,
                quantity: quantity, board: card.board, card: card.card
            )
        }
    }

    // MARK: - Stats

    var totalCards: Int { cards.filter { $0.board == "main" }.reduce(0) { $0 + $1.quantity } }
    var creatureCount: Int { cards.filter { $0.board == "main" && $0.card?.isCreature == true }.reduce(0) { $0 + $1.quantity } }
    var landCount: Int { cards.filter { $0.board == "main" && $0.card?.isLand == true }.reduce(0) { $0 + $1.quantity } }
    var spellCount: Int { totalCards - creatureCount - landCount }

    var avgCMC: Double {
        let nonLands = cards.filter { $0.board == "main" && $0.card?.isLand != true }
        let total = nonLands.reduce(0.0) { $0 + (($1.card?.cmc ?? 0) * Double($1.quantity)) }
        let count = nonLands.reduce(0) { $0 + $1.quantity }
        return count > 0 ? total / Double(count) : 0
    }

    var mainDeckCards: [MtgDeckCard] { cards.filter { $0.board == "main" } }
    var sideboardCards: [MtgDeckCard] { cards.filter { $0.board == "sideboard" } }

    var cardsByType: [(type: String, cards: [MtgDeckCard])] {
        let types = ["Creatures", "Instants", "Sorceries", "Enchantments", "Artifacts", "Planeswalkers", "Lands", "Other"]
        return types.compactMap { type in
            let matching = mainDeckCards.filter { $0.card?.typeCategory == type }
            return matching.isEmpty ? nil : (type, matching)
        }
    }

    // MARK: - Load / Save

    func loadForEdit(deck: MtgDeck) {
        isEditing = true
        deckId = deck.id
        deckName = deck.name
        formatId = deck.formatId
        description = deck.description ?? ""
        powerLevel = deck.powerLevel
        isPublic = deck.isPublic ?? false
        commanderScryfallId = deck.commanderScryfallId
        cards = deck.cards ?? []
    }

    func saveDeck() async -> MtgDeck? {
        guard let services else { return nil }
        isSaving = true
        error = nil
        do {
            if isEditing, let deckId {
                let input = UpdateDeckInput(
                    name: deckName,
                    formatId: formatId,
                    description: description.isEmpty ? nil : description,
                    powerLevel: powerLevel,
                    isPublic: isPublic,
                    commanderScryfallId: commanderScryfallId
                )
                let deck = try await services.mtgDecks.updateDeck(id: deckId, input: input)
                isSaving = false
                return deck
            } else {
                let input = CreateDeckInput(
                    name: deckName,
                    formatId: formatId,
                    description: description.isEmpty ? nil : description,
                    powerLevel: powerLevel,
                    isPublic: isPublic
                )
                let deck = try await services.mtgDecks.createDeck(input: input)
                deckId = deck.id
                isEditing = true

                // Add cards
                for card in cards {
                    let cardInput = DeckCardInput(
                        scryfallId: card.scryfallId,
                        quantity: card.quantity,
                        board: card.board
                    )
                    try await services.mtgDecks.addCard(deckId: deck.id, card: cardInput)
                }

                isSaving = false
                return deck
            }
        } catch {
            self.error = error.localizedDescription
            isSaving = false
            return nil
        }
    }

    var isValid: Bool {
        !deckName.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
