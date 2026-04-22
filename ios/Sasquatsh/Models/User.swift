import Foundation

struct User: Codable, Identifiable, Equatable {
    let id: String
    let email: String
    let username: String
    var displayName: String?
    var avatarUrl: String?
    let subscriptionTier: SubscriptionTier?
    let subscriptionExpiresAt: String?
    let subscriptionOverrideTier: SubscriptionTier?
    let isAdmin: Bool
    var blockedUserIds: [String]
    let createdAt: String?

    /// Effective tier: override takes precedence over base tier (matches website logic)
    var effectiveTier: SubscriptionTier {
        subscriptionOverrideTier ?? subscriptionTier ?? .free
    }

    static func == (lhs: User, rhs: User) -> Bool {
        lhs.id == rhs.id
    }
}

struct UserSummary: Codable, Identifiable, Equatable, Hashable {
    let id: String
    var displayName: String?
    var avatarUrl: String?
    var username: String?
    var isFoundingMember: Bool?
    var isAdmin: Bool?
    var subscriptionTier: SubscriptionTier?
    var subscriptionOverrideTier: SubscriptionTier?

    /// Effective tier: override takes precedence (matches website getEffectiveTier)
    var effectiveTier: SubscriptionTier {
        subscriptionOverrideTier ?? subscriptionTier ?? .free
    }

    static func == (lhs: UserSummary, rhs: UserSummary) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
