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

struct PendingGroupInvitation: Codable, Identifiable {
    let id: String
    let inviteCode: String
    let status: String
    let createdAt: String
    let expiresAt: String?
    let invitedBy: UserSummary?
    let group: InvitationGroupInfo?
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
    let frequency: String?
    let dayOfWeek: Int
    let monthlyWeek: Int?
    let startTime: String
    let durationMinutes: Int
    let maxPlayers: Int
    let hostIsPlaying: Bool?
    let locationDetails: String?
    let eventLocationId: String?
    let addressLine1: String?
    let city: String?
    let state: String?
    let postalCode: String?
    let timezone: String?
    let gameSystem: String?
    let gameTitle: String?
    let isPublic: Bool?
    let isActive: Bool
    let nextOccurrenceDate: String?
    let lastGeneratedDate: String?
    let hostUserId: String?
    let createdByUserId: String?
    let createdAt: String

    var frequencyDisplayName: String {
        switch frequency {
        case "weekly": return "Weekly"
        case "biweekly": return "Every 2 Weeks"
        case "monthly": return "Monthly"
        default: return "Weekly"
        }
    }

    var dayOfWeekName: String {
        let days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
        guard dayOfWeek >= 0, dayOfWeek < 7 else { return "" }
        return days[dayOfWeek]
    }

    var monthlyWeekName: String? {
        guard frequency == "monthly", let week = monthlyWeek else { return nil }
        switch week {
        case 1: return "1st"
        case 2: return "2nd"
        case 3: return "3rd"
        case 4: return "4th"
        case -1: return "Last"
        default: return nil
        }
    }

    var scheduleDescription: String {
        let time = startTime
        switch frequency {
        case "monthly":
            if let weekName = monthlyWeekName {
                return "\(weekName) \(dayOfWeekName) at \(time)"
            }
            return "\(dayOfWeekName) at \(time) (monthly)"
        case "biweekly":
            return "Every other \(dayOfWeekName) at \(time)"
        default:
            return "Every \(dayOfWeekName) at \(time)"
        }
    }
}

struct CreateRecurringGameInput: Codable {
    var groupId: String
    var title: String
    var description: String?
    var frequency: String
    var dayOfWeek: Int
    var monthlyWeek: Int?
    var startTime: String
    var durationMinutes: Int?
    var maxPlayers: Int?
    var hostIsPlaying: Bool?
    var locationDetails: String?
    var eventLocationId: String?
    var addressLine1: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var timezone: String?
    var gameSystem: String?
    var gameTitle: String?
    var isPublic: Bool?
}

struct UpdateRecurringGameInput: Codable {
    var title: String?
    var description: String?
    var frequency: String?
    var dayOfWeek: Int?
    var monthlyWeek: Int?
    var startTime: String?
    var durationMinutes: Int?
    var maxPlayers: Int?
    var hostIsPlaying: Bool?
    var locationDetails: String?
    var eventLocationId: String?
    var addressLine1: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var timezone: String?
    var gameSystem: String?
    var gameTitle: String?
    var isPublic: Bool?
    var isActive: Bool?
}
