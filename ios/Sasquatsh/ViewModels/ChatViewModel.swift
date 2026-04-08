import SwiftUI

@Observable
@MainActor
final class ChatViewModel {
    var messages: [ChatMessage] = []
    var messageText = ""
    var isLoading = false
    var isSending = false
    var error: String?
    var hasMore = true

    private var contextType = ""
    private var contextId = ""
    private var services: ServiceContainer?
    private var pollTimer: Timer?

    func configure(services: ServiceContainer, contextType: String, contextId: String) {
        self.services = services
        self.contextType = contextType
        self.contextId = contextId
    }

    func loadMessages() async {
        guard let services, !contextId.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            let fetched = try await services.chat.getMessages(
                contextType: contextType,
                contextId: contextId,
                limit: 50,
                before: nil
            )
            messages = fetched.reversed()
            hasMore = fetched.count >= 50
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadEarlierMessages() async {
        guard let services, let oldest = messages.first else { return }
        do {
            let fetched = try await services.chat.getMessages(
                contextType: contextType,
                contextId: contextId,
                limit: 50,
                before: oldest.createdAt
            )
            let earlier = fetched.reversed()
            messages.insert(contentsOf: earlier, at: 0)
            hasMore = fetched.count >= 50
        } catch {
            self.error = error.localizedDescription
        }
    }

    func sendMessage() async {
        guard let services else { return }
        let content = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty, content.count <= 1000 else { return }
        isSending = true
        error = nil
        let text = messageText
        messageText = ""
        do {
            let message = try await services.chat.sendMessage(
                contextType: contextType,
                contextId: contextId,
                content: content
            )
            if !messages.contains(where: { $0.id == message.id }) {
                messages.append(message)
            }
        } catch {
            messageText = text
            self.error = error.localizedDescription
        }
        isSending = false
    }

    func deleteMessage(_ id: String) async {
        guard let services else { return }
        do {
            try await services.chat.deleteMessage(
                contextType: contextType,
                contextId: contextId,
                messageId: id
            )
            messages.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func reportMessage(_ id: String, reason: String, details: String?) async {
        guard let services else { return }
        do {
            try await services.chat.reportMessage(
                contextType: contextType,
                contextId: contextId,
                messageId: id,
                reason: reason,
                details: details
            )
        } catch {
            self.error = error.localizedDescription
        }
    }

    func startPolling() {
        stopPolling()
        pollTimer = Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                await self.pollNewMessages()
            }
        }
    }

    func stopPolling() {
        pollTimer?.invalidate()
        pollTimer = nil
    }

    private func pollNewMessages() async {
        guard let services, !contextId.isEmpty else { return }
        do {
            let fetched = try await services.chat.getMessages(
                contextType: contextType,
                contextId: contextId,
                limit: 50,
                before: nil
            )
            let reversed = fetched.reversed()
            let existingIds = Set(messages.map(\.id))
            let newMessages = reversed.filter { !existingIds.contains($0.id) }
            if !newMessages.isEmpty {
                messages.append(contentsOf: newMessages)
            }
        } catch {}
    }
}
