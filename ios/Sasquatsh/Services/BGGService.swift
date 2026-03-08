import Foundation

protocol BGGServiceProtocol: Sendable {
    func searchGames(query: String) async throws -> [BggSearchResult]
    func getGameDetails(bggId: Int) async throws -> BggGame
}

final class BGGService: BGGServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func searchGames(query: String) async throws -> [BggSearchResult] {
        try await api.get("bgg", queryItems: [.init(name: "search", value: query)], authenticated: true)
    }

    func getGameDetails(bggId: Int) async throws -> BggGame {
        try await api.get("bgg", queryItems: [.init(name: "id", value: String(bggId))], authenticated: true)
    }
}
