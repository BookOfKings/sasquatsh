import Foundation

struct User: Codable, Identifiable, Equatable {
    let id: String
    let email: String
    let username: String
    var displayName: String?
    var avatarUrl: String?
    let subscriptionTier: SubscriptionTier?
    let subscriptionExpiresAt: String?
    let isAdmin: Bool
    var blockedUserIds: [String]
    let createdAt: String?

    static func == (lhs: User, rhs: User) -> Bool {
        lhs.id == rhs.id
    }
}

struct UserSummary: Codable, Identifiable, Equatable, Hashable {
    let id: String
    var displayName: String?
    var avatarUrl: String?
    var username: String?

    static func == (lhs: UserSummary, rhs: UserSummary) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
