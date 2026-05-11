import Foundation

struct ReferralStats: Decodable {
    let totalReferrals: Int
    let progressToNext: Int
    let nextMilestone: Int
    let referrals: [ReferralEntry]
    let rewards: [ReferralReward]
    let activeProReward: ActiveProReward?
}

struct ReferralEntry: Decodable, Identifiable {
    let id: String
    let status: String
    let createdAt: String
    let referred: ReferralUser?
}

struct ReferralUser: Decodable {
    let id: String
    let username: String
    let displayName: String?
    let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case id, username
        case displayName = "display_name"
        case avatarUrl = "avatar_url"
    }
}

struct ReferralReward: Decodable {
    let referralCountAtReward: Int
    let proExpiresAt: String
    let createdAt: String
}

struct ActiveProReward: Decodable {
    let expiresAt: String
}
