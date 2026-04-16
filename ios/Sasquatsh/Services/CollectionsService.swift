import Foundation
import os

protocol CollectionsServiceProtocol: Sendable {
    func getMyCollection() async throws -> [CollectionGame]
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

    // Edge functions convert to camelCase, but handle snake_case fallback
    enum CodingKeys: String, CodingKey {
        case id, bggId, gameName, thumbnailUrl, minPlayers, maxPlayers, yearPublished, bggRank
        // snake_case fallbacks
        case bgg_id, game_name, thumbnail_url, min_players, max_players, year_published, bgg_rank
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
    }
}

// Response wrapper in case the endpoint returns { games: [...] }
private struct CollectionResponse: Decodable {
    let games: [CollectionGame]?
}

struct CollectionsService: CollectionsServiceProtocol {
    private let api: APIClient
    private let logger = Logger(subsystem: "com.sasquatsh", category: "Collections")

    init(api: APIClient) {
        self.api = api
    }

    func getMyCollection() async throws -> [CollectionGame] {
        // Try as bare array first
        do {
            let games: [CollectionGame] = try await api.get("collections", authenticated: true)
            logger.info("Loaded \(games.count) collection games (array)")
            return games
        } catch {
            logger.debug("Array decode failed: \(error.localizedDescription)")
        }

        // Try as wrapped { games: [...] }
        let response: CollectionResponse = try await api.get("collections", authenticated: true)
        let games = response.games ?? []
        logger.info("Loaded \(games.count) collection games (wrapper)")
        return games
    }
}
