import Foundation

struct ChatUser: Codable {
    let id: String
    let displayName: String?
    let avatarUrl: String?
    let isFoundingMember: Bool?
    let isAdmin: Bool?
}

struct ChatMessage: Codable, Identifiable {
    let id: String
    let contextType: String
    let contextId: String
    let userId: String
    let content: String
    let createdAt: String
    let user: ChatUser?
}

struct SendMessageInput: Codable {
    let content: String
}

enum ChatReportReason: String, CaseIterable, Identifiable {
    case harassment
    case spam
    case hate_speech
    case inappropriate
    case threats
    case other

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .harassment: return "Harassment"
        case .spam: return "Spam"
        case .hate_speech: return "Hate Speech"
        case .inappropriate: return "Inappropriate"
        case .threats: return "Threats"
        case .other: return "Other"
        }
    }
}

struct ReportMessageInput: Codable {
    let reason: String
    let details: String?
}

struct ChatMessagesResponse: Codable {
    let messages: [ChatMessage]
}

struct SendMessageResponse: Codable {
    let message: ChatMessage
}

struct ReportMessageResponse: Codable {
    let success: Bool
    let reportId: String?
}
