import Foundation

struct GameGroup: Codable, Identifiable {
    let id: String
    var name: String
    let slug: String
    var description: String?
    var logoUrl: String?
    var coverImageUrl: String?
    var groupType: GroupType
    var locationCity: String?
    var locationState: String?
    var locationRadiusMiles: Int?
    var joinPolicy: JoinPolicy
    let createdByUserId: String
    let createdAt: String
    let updatedAt: String
    var memberCount: Int?
    let creator: UserSummary?
}

struct GroupSummary: Codable, Identifiable {
    let id: String
    let name: String
    let slug: String
    let description: String?
    let logoUrl: String?
    let groupType: GroupType
    let locationCity: String?
    let locationState: String?
    let joinPolicy: JoinPolicy
    let memberCount: Int
    var userRole: MemberRole?
}

struct GroupMember: Codable, Identifiable {
    let id: String
    let userId: String
    let displayName: String?
    let email: String?
    let avatarUrl: String?
    var role: MemberRole
    let joinedAt: String
}

struct JoinRequest: Codable, Identifiable {
    let id: String
    let userId: String
    let displayName: String?
    let email: String?
    let avatarUrl: String?
    let message: String?
    let status: JoinRequestStatus
    let createdAt: String
}

struct GroupInvitation: Codable, Identifiable {
    let id: String
    let inviteCode: String
    let invitedByDisplayName: String?
    let invitedEmail: String?
    let maxUses: Int?
    let usesCount: Int
    let expiresAt: String?
    let createdAt: String
}

struct CreateGroupInput: Codable {
    var name: String
    var description: String?
    var groupType: GroupType
    var locationCity: String?
    var locationState: String?
    var locationRadiusMiles: Int?
    var joinPolicy: JoinPolicy?
}

struct UpdateGroupInput: Codable {
    var name: String?
    var description: String?
    var groupType: GroupType?
    var locationCity: String?
    var locationState: String?
    var locationRadiusMiles: Int?
    var joinPolicy: JoinPolicy?
    var logoUrl: String?
}

struct GroupSearchFilter {
    var search: String?
    var groupType: GroupType?
    var city: String?
    var state: String?

    var queryItems: [URLQueryItem] {
        var items: [URLQueryItem] = []
        if let search, !search.isEmpty { items.append(.init(name: "search", value: search)) }
        if let groupType { items.append(.init(name: "type", value: groupType.rawValue)) }
        if let city { items.append(.init(name: "city", value: city)) }
        if let state { items.append(.init(name: "state", value: state)) }
        return items
    }
}

struct CreateInvitationInput: Codable {
    var email: String?
    var phone: String?
    var maxUses: Int?
    var expiresInDays: Int?
}

struct InvitationPreview: Codable {
    let inviteCode: String
    let invitedEmail: String?
    let group: InvitationGroupInfo
    let invitedBy: UserSummary
    let expiresAt: String?
}

struct InvitationGroupInfo: Codable {
    let id: String
    let name: String
    let slug: String
    let description: String?
    let logoUrl: String?
    let groupType: GroupType
    let locationCity: String?
    let locationState: String?
    let joinPolicy: JoinPolicy
}

struct GroupMembership: Codable, Identifiable {
    let id: String
    let groupId: String
    let userId: String
    var role: MemberRole
    let joinedAt: String
    let user: UserSummary?
}

struct RecurringGame: Codable, Identifiable {
    let id: String
    let groupId: String
    let title: String
    let description: String?
    let dayOfWeek: Int
    let startTime: String
    let durationMinutes: Int
    let maxPlayers: Int
    let locationDetails: String?
    let isActive: Bool
    let createdAt: String
}
