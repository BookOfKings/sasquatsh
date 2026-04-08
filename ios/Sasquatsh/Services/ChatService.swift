import Foundation

protocol ChatServiceProtocol {
    func getMessages(contextType: String, contextId: String, limit: Int, before: String?) async throws -> [ChatMessage]
    func sendMessage(contextType: String, contextId: String, content: String) async throws -> ChatMessage
    func deleteMessage(contextType: String, contextId: String, messageId: String) async throws
    func reportMessage(contextType: String, contextId: String, messageId: String, reason: String, details: String?) async throws
}

final class ChatService: ChatServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getMessages(contextType: String, contextId: String, limit: Int = 50, before: String? = nil) async throws -> [ChatMessage] {
        var queryItems = [
            URLQueryItem(name: "contextType", value: contextType),
            URLQueryItem(name: "contextId", value: contextId),
            URLQueryItem(name: "limit", value: String(limit))
        ]
        if let before {
            queryItems.append(URLQueryItem(name: "before", value: before))
        }
        let response: ChatMessagesResponse = try await api.get("chat", queryItems: queryItems, authenticated: true)
        return response.messages
    }

    func sendMessage(contextType: String, contextId: String, content: String) async throws -> ChatMessage {
        let queryItems = [
            URLQueryItem(name: "contextType", value: contextType),
            URLQueryItem(name: "contextId", value: contextId)
        ]
        let input = SendMessageInput(content: content)
        let response: SendMessageResponse = try await api.post("chat", body: input, queryItems: queryItems, authenticated: true)
        return response.message
    }

    func deleteMessage(contextType: String, contextId: String, messageId: String) async throws {
        let queryItems = [
            URLQueryItem(name: "contextType", value: contextType),
            URLQueryItem(name: "contextId", value: contextId),
            URLQueryItem(name: "messageId", value: messageId)
        ]
        try await api.deleteVoid("chat", queryItems: queryItems, authenticated: true)
    }

    func reportMessage(contextType: String, contextId: String, messageId: String, reason: String, details: String?) async throws {
        let queryItems = [
            URLQueryItem(name: "contextType", value: contextType),
            URLQueryItem(name: "contextId", value: contextId),
            URLQueryItem(name: "messageId", value: messageId),
            URLQueryItem(name: "action", value: "report")
        ]
        let input = ReportMessageInput(reason: reason, details: details)
        let _: ReportMessageResponse = try await api.post("chat", body: input, queryItems: queryItems, authenticated: true)
    }
}
