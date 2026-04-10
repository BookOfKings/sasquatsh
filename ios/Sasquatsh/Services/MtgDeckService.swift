import Foundation

protocol MtgDeckServiceProtocol {
    func getMyDecks() async throws -> [MtgDeck]
    func getDeck(id: String) async throws -> MtgDeck
    func createDeck(input: CreateDeckInput) async throws -> MtgDeck
    func updateDeck(id: String, input: UpdateDeckInput) async throws -> MtgDeck
    func deleteDeck(id: String) async throws
    func addCard(deckId: String, card: DeckCardInput) async throws
    func removeCard(deckId: String, cardId: String) async throws
    func updateCard(deckId: String, cardId: String, quantity: Int, board: String) async throws
    func importDeck(input: ImportDeckInput) async throws -> MtgDeck
}

final class MtgDeckService: MtgDeckServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getMyDecks() async throws -> [MtgDeck] {
        try await api.get("mtg-decks", authenticated: true)
    }

    func getDeck(id: String) async throws -> MtgDeck {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        return try await api.get("mtg-decks", queryItems: queryItems, authenticated: true)
    }

    func createDeck(input: CreateDeckInput) async throws -> MtgDeck {
        try await api.post("mtg-decks", body: input, authenticated: true)
    }

    func updateDeck(id: String, input: UpdateDeckInput) async throws -> MtgDeck {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        return try await api.put("mtg-decks", body: input, queryItems: queryItems, authenticated: true)
    }

    func deleteDeck(id: String) async throws {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        try await api.deleteVoid("mtg-decks", queryItems: queryItems, authenticated: true)
    }

    func addCard(deckId: String, card: DeckCardInput) async throws {
        let queryItems = [URLQueryItem(name: "id", value: deckId)]
        try await api.postVoid("mtg-decks", body: card, queryItems: queryItems, authenticated: true)
    }

    func removeCard(deckId: String, cardId: String) async throws {
        let queryItems = [
            URLQueryItem(name: "id", value: deckId),
            URLQueryItem(name: "cardId", value: cardId)
        ]
        try await api.deleteVoid("mtg-decks", queryItems: queryItems, authenticated: true)
    }

    func updateCard(deckId: String, cardId: String, quantity: Int, board: String) async throws {
        struct CardUpdate: Codable { let quantity: Int; let board: String }
        let queryItems = [
            URLQueryItem(name: "id", value: deckId),
            URLQueryItem(name: "cardId", value: cardId)
        ]
        try await api.putVoid("mtg-decks", body: CardUpdate(quantity: quantity, board: board), queryItems: queryItems, authenticated: true)
    }

    func importDeck(input: ImportDeckInput) async throws -> MtgDeck {
        let queryItems = [URLQueryItem(name: "action", value: "import")]
        return try await api.post("mtg-decks", body: input, queryItems: queryItems, authenticated: true)
    }
}
