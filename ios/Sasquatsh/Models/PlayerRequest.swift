import Foundation

// MARK: - Player Request (Host needs fill-in players)

struct PlayerRequest: Codable, Identifiable {
    let id: String
    let userId: String
    let eventId: String
    var description: String?
    var playerCountNeeded: Int
    var status: String
    var isActive: Bool
    let createdAt: String
    var expiresAt: String
    let event: PlayerRequestEvent?
    let host: UserSummary?
}

struct PlayerRequestEvent: Codable {
    let id: String
    let title: String
    let gameTitle: String?
    let eventDate: String
    let startTime: String
    let city: String?
    let state: String?
    let addressLine1: String?
    let locationDetails: String?
}

struct CreatePlayerRequestInput: Codable {
    var eventId: String
    var description: String?
    var playerCountNeeded: Int?
}

struct UpdatePlayerRequestInput: Codable {
    var description: String?
    var playerCountNeeded: Int?
}

struct PlayerRequestFilters {
    var eventId: String?
    var city: String?
    var state: String?
    var eventLocationId: String?

    var queryItems: [URLQueryItem] {
        var items: [URLQueryItem] = []
        if let eventId { items.append(.init(name: "eventId", value: eventId)) }
        if let city { items.append(.init(name: "city", value: city)) }
        if let state { items.append(.init(name: "state", value: state)) }
        if let eventLocationId { items.append(.init(name: "eventLocationId", value: eventLocationId)) }
        return items
    }
}

struct EventLocation: Codable, Identifiable {
    let id: String
    let name: String
    let city: String
    let state: String
    let venue: String?
    let timezone: String?
    let startDate: String?
    let endDate: String?
    let isPermanent: Bool?
    let recurringDays: [Int]?
    let status: EventLocationStatus
    let eventCount: Int?
    let userCount: Int?
    let createdByUserId: String?
    let createdAt: String
    let updatedAt: String?
    let createdBy: UserSummary?
}

struct CreateEventLocationInput: Codable {
    var name: String
    var city: String
    var state: String
    var venue: String?
    var isPermanent: Bool?
    var recurringDays: [Int]?
    var startDate: String?
    var endDate: String?
}

struct GameInvitation: Codable, Identifiable {
    let id: String
    let eventId: String
    let inviteCode: String
    let invitedByUserId: String
    let invitedEmail: String?
    let channel: String?
    let status: String
    let acceptedByUserId: String?
    let createdAt: String
    let acceptedAt: String?
    let expiresAt: String?
    let event: GameInvitationEvent?
}

struct GameInvitationEvent: Codable {
    let id: String
    let title: String
    let eventDate: String
    let startTime: String
    let city: String?
    let state: String?
    let maxPlayers: Int
    let host: UserSummary?
}

struct CreateGameInvitationInput: Codable {
    var eventId: String
    var email: String?
    var channel: String?
    var expiresInDays: Int?
}
