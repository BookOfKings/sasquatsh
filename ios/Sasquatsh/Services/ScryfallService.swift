import Foundation

protocol ScryfallServiceProtocol {
    func searchCards(query: String) async throws -> [ScryfallCard]
    func autocomplete(query: String) async throws -> [String]
    func getCard(id: String) async throws -> ScryfallCard
}

private struct ScryfallSearchResponse: Decodable {
    let cards: [ScryfallCard]
}

final class ScryfallService: ScryfallServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func searchCards(query: String) async throws -> [ScryfallCard] {
        let queryItems = [URLQueryItem(name: "search", value: query)]
        let response: ScryfallSearchResponse = try await api.get("scryfall", queryItems: queryItems, authenticated: true)
        return response.cards
    }

    func autocomplete(query: String) async throws -> [String] {
        let queryItems = [URLQueryItem(name: "autocomplete", value: query)]
        return try await api.get("scryfall", queryItems: queryItems, authenticated: true)
    }

    func getCard(id: String) async throws -> ScryfallCard {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        return try await api.get("scryfall", queryItems: queryItems, authenticated: true)
    }
}
