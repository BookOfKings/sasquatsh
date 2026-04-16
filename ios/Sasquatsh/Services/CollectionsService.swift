import Foundation
import os

protocol CollectionsServiceProtocol: Sendable {
    func getMyCollection() async throws -> [CollectionGame]
    func getTopGames() async throws -> [CollectionGame]
    func addGame(_ game: AddCollectionGameInput) async throws -> [CollectionGame]
    func removeGame(bggId: Int) async throws
}

struct CollectionGame: Decodable, Identifiable {
    let id: String
    let bggId: Int?
    let gameName: String
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let yearPublished: Int?
    let bggRank: Int?
    let averageRating: Double?
    let playingTime: Int?

    enum CodingKeys: String, CodingKey {
        case id, bggId, gameName, thumbnailUrl, minPlayers, maxPlayers
        case yearPublished, bggRank, averageRating, playingTime
        // snake_case fallbacks
        case bgg_id, game_name, thumbnail_url, min_players, max_players
        case year_published, bgg_rank, average_rating, playing_time
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = (try? c.decode(String.self, forKey: .id)) ?? UUID().uuidString
        bggId = (try? c.decode(Int.self, forKey: .bggId)) ?? (try? c.decode(Int.self, forKey: .bgg_id))
        gameName = (try? c.decode(String.self, forKey: .gameName)) ?? (try? c.decode(String.self, forKey: .game_name)) ?? ""
        thumbnailUrl = (try? c.decode(String.self, forKey: .thumbnailUrl)) ?? (try? c.decode(String.self, forKey: .thumbnail_url))
        minPlayers = (try? c.decode(Int.self, forKey: .minPlayers)) ?? (try? c.decode(Int.self, forKey: .min_players))
        maxPlayers = (try? c.decode(Int.self, forKey: .maxPlayers)) ?? (try? c.decode(Int.self, forKey: .max_players))
        yearPublished = (try? c.decode(Int.self, forKey: .yearPublished)) ?? (try? c.decode(Int.self, forKey: .year_published))
        bggRank = (try? c.decode(Int.self, forKey: .bggRank)) ?? (try? c.decode(Int.self, forKey: .bgg_rank))
        averageRating = (try? c.decode(Double.self, forKey: .averageRating)) ?? (try? c.decode(Double.self, forKey: .average_rating))
        playingTime = (try? c.decode(Int.self, forKey: .playingTime)) ?? (try? c.decode(Int.self, forKey: .playing_time))
    }

    // For creating from BGG search results
    init(id: String = UUID().uuidString, bggId: Int?, gameName: String, thumbnailUrl: String? = nil, minPlayers: Int? = nil, maxPlayers: Int? = nil, yearPublished: Int? = nil, bggRank: Int? = nil, averageRating: Double? = nil, playingTime: Int? = nil) {
        self.id = id
        self.bggId = bggId
        self.gameName = gameName
        self.thumbnailUrl = thumbnailUrl
        self.minPlayers = minPlayers
        self.maxPlayers = maxPlayers
        self.yearPublished = yearPublished
        self.bggRank = bggRank
        self.averageRating = averageRating
        self.playingTime = playingTime
    }
}

struct AddCollectionGameInput: Encodable {
    let bggId: Int
    let name: String
    let thumbnailUrl: String?
    let imageUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let playingTime: Int?
    let yearPublished: Int?
    let bggRank: Int?
    let averageRating: Double?

    // API expects snake_case
    enum CodingKeys: String, CodingKey {
        case bggId = "bgg_id"
        case name
        case thumbnailUrl = "thumbnail_url"
        case imageUrl = "image_url"
        case minPlayers = "min_players"
        case maxPlayers = "max_players"
        case playingTime = "playing_time"
        case yearPublished = "year_published"
        case bggRank = "bgg_rank"
        case averageRating = "average_rating"
    }
}

private struct CollectionResponse: Decodable {
    let games: [CollectionGame]?
}

private struct AddGamesRequest: Encodable {
    let games: [AddCollectionGameInput]
}

struct CollectionsService: CollectionsServiceProtocol {
    private let api: APIClient
    private let logger = Logger(subsystem: "com.sasquatsh", category: "Collections")

    init(api: APIClient) {
        self.api = api
    }

    func getMyCollection() async throws -> [CollectionGame] {
        let response: CollectionResponse = try await api.get("collections", authenticated: true)
        return response.games ?? []
    }

    func getTopGames() async throws -> [CollectionGame] {
        let response: CollectionResponse = try await api.get("collections", queryItems: [
            .init(name: "action", value: "top-games")
        ], authenticated: false)
        return response.games ?? []
    }

    func addGame(_ game: AddCollectionGameInput) async throws -> [CollectionGame] {
        let request = AddGamesRequest(games: [game])
        let response: CollectionResponse = try await api.post("collections", body: request)
        return response.games ?? []
    }

    func removeGame(bggId: Int) async throws {
        try await api.deleteVoid("collections", queryItems: [
            .init(name: "bggId", value: String(bggId))
        ])
    }
}
