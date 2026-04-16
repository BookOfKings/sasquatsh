import Foundation

protocol BadgesServiceProtocol: Sendable {
    func getAllBadges() async throws -> [Badge]
    func getMyBadges() async throws -> BadgesResponse
    func computeBadges() async throws -> BadgesResponse
    func togglePin(badgeId: Int) async throws -> PinResponse
}

struct Badge: Decodable, Identifiable {
    let id: Int
    let slug: String
    let name: String
    let description: String
    let iconSvg: String?
    let category: String
    let tier: String
    let requirementType: String
    let requirementCount: Int
    let sortOrder: Int

    // snake_case support
    enum CodingKeys: String, CodingKey {
        case id, slug, name, description, category, tier, sortOrder
        case iconSvg = "iconSvg"
        case requirementType = "requirementType"
        case requirementCount = "requirementCount"
        case icon_svg, requirement_type, requirement_count, sort_order
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        slug = try c.decode(String.self, forKey: .slug)
        name = try c.decode(String.self, forKey: .name)
        description = try c.decode(String.self, forKey: .description)
        category = try c.decode(String.self, forKey: .category)
        tier = try c.decode(String.self, forKey: .tier)
        iconSvg = (try? c.decode(String.self, forKey: .iconSvg)) ?? (try? c.decode(String.self, forKey: .icon_svg))
        requirementType = (try? c.decode(String.self, forKey: .requirementType)) ?? (try? c.decode(String.self, forKey: .requirement_type)) ?? ""
        requirementCount = (try? c.decode(Int.self, forKey: .requirementCount)) ?? (try? c.decode(Int.self, forKey: .requirement_count)) ?? 0
        sortOrder = (try? c.decode(Int.self, forKey: .sortOrder)) ?? (try? c.decode(Int.self, forKey: .sort_order)) ?? 0
    }
}

struct UserBadge: Decodable, Identifiable {
    let id: String
    let badgeId: Int
    let earnedAt: String
    let isPinned: Bool
    let badge: Badge

    enum CodingKeys: String, CodingKey {
        case id, badge
        case badgeId = "badgeId"
        case earnedAt = "earnedAt"
        case isPinned = "isPinned"
        case badge_id, earned_at, is_pinned
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        badgeId = (try? c.decode(Int.self, forKey: .badgeId)) ?? (try? c.decode(Int.self, forKey: .badge_id)) ?? 0
        earnedAt = (try? c.decode(String.self, forKey: .earnedAt)) ?? (try? c.decode(String.self, forKey: .earned_at)) ?? ""
        isPinned = (try? c.decode(Bool.self, forKey: .isPinned)) ?? (try? c.decode(Bool.self, forKey: .is_pinned)) ?? false
        badge = try c.decode(Badge.self, forKey: .badge)
    }
}

struct BadgesResponse: Decodable {
    let badges: [UserBadge]
    let newlyEarned: Int?
}

struct AllBadgesResponse: Decodable {
    let badges: [Badge]
}

struct PinResponse: Decodable {
    let pinned: Bool
}

struct BadgesService: BadgesServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getAllBadges() async throws -> [Badge] {
        let response: AllBadgesResponse = try await api.get("badges", authenticated: false)
        return response.badges
    }

    func getMyBadges() async throws -> BadgesResponse {
        try await api.get("badges", queryItems: [.init(name: "action", value: "my-badges")], authenticated: true)
    }

    func computeBadges() async throws -> BadgesResponse {
        try await api.post("badges", queryItems: [.init(name: "action", value: "compute")])
    }

    func togglePin(badgeId: Int) async throws -> PinResponse {
        try await api.put("badges", queryItems: [
            .init(name: "action", value: "pin"),
            .init(name: "badgeId", value: String(badgeId))
        ])
    }
}
