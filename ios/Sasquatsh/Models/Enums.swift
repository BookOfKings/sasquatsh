import Foundation

enum DifficultyLevel: String, Codable, CaseIterable, Identifiable {
    case beginner
    case intermediate
    case advanced

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum EventStatus: String, Codable, CaseIterable, Identifiable {
    case draft
    case published
    case cancelled
    case completed

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum ItemCategory: String, Codable, CaseIterable, Identifiable {
    case food
    case drinks
    case supplies
    case other

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum GameCategory: String, Codable, CaseIterable, Identifiable {
    case strategy
    case party
    case cooperative
    case deckbuilding
    case workerplacement
    case areacontrol
    case dice
    case trivia
    case roleplaying
    case miniatures
    case card
    case family
    case abstract
    case other

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .deckbuilding: return "Deck Building"
        case .workerplacement: return "Worker Placement"
        case .areacontrol: return "Area Control"
        case .roleplaying: return "Roleplaying"
        default: return rawValue.capitalized
        }
    }
}

enum GroupType: String, Codable, CaseIterable, Identifiable {
    case geographic
    case interest
    case both

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum MemberRole: String, Codable, CaseIterable, Identifiable {
    case owner
    case admin
    case member

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum JoinPolicy: String, Codable, CaseIterable, Identifiable {
    case open
    case request
    case invite_only

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .open: return "Open"
        case .request: return "Request to Join"
        case .invite_only: return "Invite Only"
        }
    }
}

enum PlanningStatus: String, Codable, CaseIterable, Identifiable {
    case open
    case finalized
    case cancelled

    var id: String { rawValue }

    var displayName: String {
        rawValue.capitalized
    }
}

enum JoinRequestStatus: String, Codable {
    case pending
    case approved
    case rejected
}

enum SubscriptionTier: String, Codable {
    case free
    case pro
    case premium
}

enum EventLocationStatus: String, Codable {
    case pending
    case approved
    case rejected
}

enum AppTimezone: String, CaseIterable, Identifiable {
    case eastern = "America/New_York"
    case central = "America/Chicago"
    case mountain = "America/Denver"
    case arizona = "America/Phoenix"
    case pacific = "America/Los_Angeles"
    case alaska = "America/Anchorage"
    case hawaii = "Pacific/Honolulu"
    case uk = "Europe/London"
    case centralEurope = "Europe/Paris"
    case japan = "Asia/Tokyo"
    case sydney = "Australia/Sydney"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .eastern: return "Eastern (ET)"
        case .central: return "Central (CT)"
        case .mountain: return "Mountain (MT)"
        case .arizona: return "Arizona (MST)"
        case .pacific: return "Pacific (PT)"
        case .alaska: return "Alaska (AKT)"
        case .hawaii: return "Hawaii (HST)"
        case .uk: return "UK (GMT/BST)"
        case .centralEurope: return "Central Europe (CET)"
        case .japan: return "Japan (JST)"
        case .sydney: return "Sydney (AEST)"
        }
    }
}
