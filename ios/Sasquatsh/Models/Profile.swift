import Foundation

struct UserProfile: Codable, Identifiable {
    let id: String
    let firebaseUid: String
    let email: String
    var username: String
    var displayName: String?
    var avatarUrl: String?
    var birthYear: Int?
    var maxTravelMiles: Int?
    var homeCity: String?
    var homeState: String?
    var homePostalCode: String?
    var activeCity: String?
    var activeState: String?
    var activeLocationExpiresAt: String?
    var activeEventLocationId: String?
    var activeLocationHall: String?
    var activeLocationRoom: String?
    var activeLocationTable: String?
    var timezone: String?
    var bio: String?
    var favoriteGames: [String]?
    var preferredGameTypes: [String]?
    var collectionVisibility: String?
    let isAdmin: Bool
    let subscriptionTier: SubscriptionTier?
    let subscriptionExpiresAt: String?
    var blockedUserIds: [String]
    let createdAt: String
    let updatedAt: String
    var groups: [UserGroupMembership]?
    var upcomingEvents: [UserUpcomingEvent]?
    var stats: UserStats?
}

struct UserGroupMembership: Codable, Identifiable {
    var id: String { group?.id ?? UUID().uuidString }
    let role: String
    let joinedAt: String
    let group: UserGroupInfo?
}

struct UserGroupInfo: Codable {
    let id: String
    let name: String
    let slug: String
    let logoUrl: String?
    let groupType: String
}

struct UserUpcomingEvent: Codable, Identifiable {
    var id: String { event?.id ?? UUID().uuidString }
    let status: String
    let event: UserEventInfo?
}

struct UserEventInfo: Codable {
    let id: String
    let title: String
    let eventDate: String
    let startTime: String
    let city: String?
    let state: String?
}

struct UserStats: Codable {
    let hostedCount: Int
    let attendedCount: Int
    let groupCount: Int
}

struct PublicProfile: Codable, Identifiable {
    let id: String
    let username: String
    let displayName: String?
    let avatarUrl: String?
    let homeCity: String?
    let homeState: String?
    let bio: String?
    let favoriteGames: [String]?
    let preferredGameTypes: [String]?
    let createdAt: String
}

struct BlockedUser: Codable, Identifiable {
    let id: String
    let username: String
    let displayName: String?
    let avatarUrl: String?
}

struct UpdateProfileInput: Codable {
    var username: String?
    var displayName: String?
    var avatarUrl: String?
    var birthYear: Int?
    var maxTravelMiles: Int?
    var homeCity: String?
    var homeState: String?
    var homePostalCode: String?
    var activeCity: String?
    var activeState: String?
    var activeLocationExpiresAt: String?
    var activeEventLocationId: String?
    var activeLocationHall: String?
    var activeLocationRoom: String?
    var activeLocationTable: String?
    var timezone: String?
    var bio: String?
    var favoriteGames: [String]?
    var preferredGameTypes: [String]?
    var collectionVisibility: String?
}

struct AvatarUploadResponse: Codable {
    let message: String
    let avatarUrl: String
    let user: UserProfile
}

struct AvatarDeleteResponse: Codable {
    let message: String
    let user: UserProfile
}

struct UsernameCheckResponse: Codable {
    let available: Bool
    let reason: String?
}
