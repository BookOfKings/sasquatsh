import Foundation

protocol GameUpcServiceProtocol: Sendable {
    func lookupUpc(upc: String, search: String?) async throws -> UpcLookupResult
    func voteMatch(upc: String, bggId: Int) async throws -> UpcLookupResult
}

struct UpcLookupResult: Decodable {
    let upc: String?
    let name: String?
    let searchedFor: String?
    let bggInfoStatus: String?
    let bggInfo: [UpcBggInfo]?

    enum CodingKeys: String, CodingKey {
        case upc, name
        case searchedFor = "searched_for"
        case bggInfoStatus = "bgg_info_status"
        case bggInfo = "bgg_info"
    }
}

struct UpcBggInfo: Decodable, Identifiable {
    let id: Int
    let name: String
    let published: String?
    let thumbnailUrl: String?
    let imageUrl: String?
    let confidence: Double?

    enum CodingKeys: String, CodingKey {
        case id, name, published, confidence
        case thumbnailUrl = "thumbnail_url"
        case imageUrl = "image_url"
    }
}

struct GameUpcService: GameUpcServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func lookupUpc(upc: String, search: String? = nil) async throws -> UpcLookupResult {
        var queryItems = [URLQueryItem(name: "upc", value: upc)]
        if let search {
            queryItems.append(.init(name: "search", value: search))
        }
        return try await api.get("game-upc", queryItems: queryItems, authenticated: true)
    }

    func voteMatch(upc: String, bggId: Int) async throws -> UpcLookupResult {
        try await api.post("game-upc", queryItems: [
            .init(name: "upc", value: upc),
            .init(name: "bggId", value: String(bggId))
        ])
    }
}
