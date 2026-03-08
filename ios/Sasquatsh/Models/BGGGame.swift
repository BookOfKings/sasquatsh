import Foundation

struct BggSearchResult: Codable, Identifiable {
    let bggId: Int
    let name: String
    let yearPublished: Int?

    var id: Int { bggId }
}

struct BggGame: Codable, Identifiable {
    let bggId: Int
    let name: String
    let yearPublished: Int?
    let thumbnailUrl: String?
    let imageUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let minPlaytime: Int?
    let maxPlaytime: Int?
    let playingTime: Int?
    let weight: Double?
    let description: String?
    let categories: [String]
    let mechanics: [String]

    var id: Int { bggId }
}

struct EventGame: Codable, Identifiable {
    let id: String
    let eventId: String
    let bggId: Int?
    let gameName: String
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let playingTime: Int?
    let isPrimary: Bool
    let isAlternative: Bool
    let addedByUserId: String?
    let createdAt: String
}

struct AddEventGameInput: Codable {
    var eventId: String
    var bggId: Int?
    var gameName: String
    var thumbnailUrl: String?
    var minPlayers: Int?
    var maxPlayers: Int?
    var playingTime: Int?
    var isPrimary: Bool?
    var isAlternative: Bool?
}
