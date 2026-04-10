import Foundation

struct EventTable: Codable, Identifiable {
    let id: String
    let tableNumber: Int
    let tableName: String?
}

struct SessionRegistration: Codable {
    let userId: String
    let displayName: String?
    let avatarUrl: String?
    let isHostReserved: Bool?
}

struct GameSession: Codable, Identifiable {
    let id: String
    let tableId: String
    let tableNumber: Int
    let bggId: Int?
    let gameName: String
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let slotIndex: Int
    let startTime: String?
    let durationMinutes: Int
    let status: String
    let registeredCount: Int
    let isFull: Bool
    let isUserRegistered: Bool
    let registrations: [SessionRegistration]?
}

struct EventSessionsResponse: Codable {
    let tables: [EventTable]
    let sessions: [GameSession]
}
