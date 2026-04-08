import Foundation

struct Raffle: Codable, Identifiable {
    let id: String
    let title: String
    let description: String?
    let prizeName: String
    let prizeDescription: String?
    let prizeImageUrl: String?
    let prizeBggId: Int?
    let prizeValueCents: Int?
    let startDate: String
    let endDate: String
    let termsConditions: String?
    let mailInInstructions: String?
    let status: String
    let winnerUserId: String?
    let winnerSelectedAt: String?
    let winnerNotifiedAt: String?
    let winnerClaimedAt: String?
    let bannerImageUrl: String?
    let createdAt: String?
    let stats: RaffleStats?
    let userEntries: [RaffleEntry]?
    let userTotalEntries: Int?
    let winner: RaffleUser?

    var isActive: Bool { status == "active" }
    var isEnded: Bool { status == "ended" }
    var hasWinner: Bool { winnerUserId != nil }

    var prizeValueFormatted: String? {
        guard let cents = prizeValueCents, cents > 0 else { return nil }
        let dollars = Double(cents) / 100.0
        return String(format: "$%.2f", dollars)
    }

    var timeRemaining: String? {
        guard let endDate = endDate.toDate else { return nil }
        let now = Date()
        guard endDate > now else { return "Ended" }

        let components = Calendar.current.dateComponents([.day, .hour, .minute], from: now, to: endDate)
        if let days = components.day, days > 0 {
            return "\(days)d \(components.hour ?? 0)h remaining"
        } else if let hours = components.hour, hours > 0 {
            return "\(hours)h \(components.minute ?? 0)m remaining"
        } else if let minutes = components.minute {
            return "\(minutes)m remaining"
        }
        return nil
    }
}

struct RaffleStats: Codable {
    let totalEntries: Int?
    let uniqueParticipants: Int?
    let entries: Int?
    let users: Int?

    var displayTotalEntries: Int { totalEntries ?? entries ?? 0 }
    var displayParticipants: Int { uniqueParticipants ?? users ?? 0 }
}

struct RaffleUser: Codable {
    let id: String
    let displayName: String?
    let avatarUrl: String?
}

struct RaffleEntry: Codable, Identifiable {
    let id: String
    let raffleId: String
    let userId: String
    let entryType: String
    let sourceId: String?
    let entryCount: Int
    let createdAt: String
}

enum RaffleEntryType: String, CaseIterable, Identifiable {
    case hostEvent = "host_event"
    case planSession = "plan_session"
    case attendEvent = "attend_event"
    case mailIn = "mail_in"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .hostEvent: return "Host Event"
        case .planSession: return "Plan Session"
        case .attendEvent: return "Attend Event"
        case .mailIn: return "Mail-In"
        }
    }

    var iconName: String {
        switch self {
        case .hostEvent: return "star.fill"
        case .planSession: return "calendar.badge.plus"
        case .attendEvent: return "person.badge.clock"
        case .mailIn: return "envelope.fill"
        }
    }

    var description: String {
        switch self {
        case .hostEvent: return "Host a game event (2x for paid)"
        case .planSession: return "Create a planning session (2x for paid)"
        case .attendEvent: return "Attend a game event"
        case .mailIn: return "No-purchase-necessary entry"
        }
    }
}

struct MailInEntryInput: Codable {
    let raffleId: String
    let name: String
    let address: String
}
