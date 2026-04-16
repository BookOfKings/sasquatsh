import Foundation

protocol BGGServiceProtocol: Sendable {
    func searchGames(query: String) async throws -> [BggSearchResult]
    func getGameDetails(bggId: Int) async throws -> BggGame
    func listCachedGames(page: Int, limit: Int) async throws -> BggCacheListResponse
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

    func listCachedGames(page: Int, limit: Int) async throws -> BggCacheListResponse {
        try await api.get("bgg-cache", queryItems: [
            .init(name: "action", value: "list"),
            .init(name: "page", value: String(page)),
            .init(name: "limit", value: String(limit))
        ], authenticated: true)
    }
}
