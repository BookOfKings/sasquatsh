import SwiftUI

@Observable
@MainActor
final class InvitationViewModel {
    var gameInvitation: GameInvitation?
    var groupInvitePreview: InvitationPreview?
    var isLoading = false
    var error: String?
    var successMessage: String?
    var acceptedEventId: String?
    var acceptedGroupId: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadGameInvitation(code: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            gameInvitation = try await services.social.getGameInvitation(code: code)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func acceptGameInvitation(code: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            let result = try await services.social.acceptGameInvitation(code: code)
            successMessage = result.message
            acceptedEventId = result.eventId
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadGroupInvitePreview(code: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            groupInvitePreview = try await services.groups.previewInvite(code: code)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func acceptGroupInvite(code: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            let result = try await services.groups.acceptInvite(code: code)
            successMessage = result.message
            acceptedGroupId = result.groupId
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
