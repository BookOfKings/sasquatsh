import Foundation

struct PlanningSession: Codable, Identifiable {
    let id: String
    let groupId: String
    let createdByUserId: String
    let title: String
    let description: String?
    let responseDeadline: String
    let status: PlanningStatus
    let finalizedDate: String?
    let finalizedGameId: String?
    let createdEventId: String?
    let createdAt: String
    var inviteeCount: Int?
    let group: PlanningGroupInfo?
    let createdBy: UserSummary?
    var invitees: [PlanningInvitee]?
    var dates: [PlanningDate]?
    var gameSuggestions: [GameSuggestion]?
    var items: [PlanningItem]?
}

struct PlanningGroupInfo: Codable {
    let id: String
    let name: String
    let slug: String
}

struct PlanningInvitee: Codable, Identifiable {
    let id: String
    let userId: String
    var hasResponded: Bool
    var respondedAt: String?
    var cannotAttendAny: Bool
    let user: UserSummary?
}

struct PlanningDate: Codable, Identifiable {
    let id: String
    let proposedDate: String
    let startTime: String?
    var availableCount: Int?
    var votes: [DateVote]?
}

struct DateVote: Codable {
    let userId: String
    let isAvailable: Bool
    let user: DateVoteUser?
}

struct DateVoteUser: Codable {
    let displayName: String?
    let avatarUrl: String?
}

struct GameSuggestion: Codable, Identifiable {
    let id: String
    let suggestedByUserId: String
    let bggId: Int?
    let gameName: String
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let playingTime: Int?
    let createdAt: String
    var voteCount: Int
    var hasVoted: Bool
    let suggestedBy: UserSummary?
}

struct CreatePlanningSessionInput: Codable {
    var groupId: String
    var title: String
    var description: String?
    var responseDeadline: String
    var inviteeUserIds: [String]
    var proposedDates: [ProposedDateInput]
}

struct ProposedDateInput: Codable {
    var date: String
    var startTime: String?
}

struct PlanningResponseInput: Codable {
    var cannotAttendAny: Bool
    var dateAvailability: [DateAvailabilityInput]
}

struct DateAvailabilityInput: Codable {
    var dateId: String
    var isAvailable: Bool
}

struct PlanningItem: Codable, Identifiable {
    let id: String
    let sessionId: String
    let name: String
    let category: ItemCategory
    let quantity: Int?
    let addedByUserId: String
    let claimedByUserId: String?
    let createdAt: String
    let addedBy: UserSummary?
    let claimedBy: UserSummary?
}

struct AddPlanningItemInput: Codable {
    var name: String
    var category: ItemCategory
    var quantity: Int?
}

struct SuggestGameInput: Codable {
    var gameName: String
    var bggId: Int?
    var thumbnailUrl: String?
    var minPlayers: Int?
    var maxPlayers: Int?
    var playingTime: Int?
}
