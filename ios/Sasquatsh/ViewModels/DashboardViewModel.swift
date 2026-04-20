import SwiftUI

@Observable
@MainActor
final class DashboardViewModel {
    var registeredEvents: [EventSummary] = []
    var hostedEvents: [EventSummary] = []
    var myGroups: [GroupSummary] = []
    var pendingInvitations: [PendingGroupInvitation] = []
    var isLoading = false
    var error: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadDashboard() async {
        guard let services else { return }
        isLoading = true
        error = nil

        do {
            async let registered = services.events.getRegisteredEvents()
            async let hosted = services.events.getHostedEvents()
            async let groups = services.groups.getMyGroups()
            async let invitations = services.groups.getMyPendingInvitations()

            let startOfToday = Calendar.current.startOfDay(for: Date())
            registeredEvents = try await registered.filter { $0.eventDate.toDate ?? .distantPast >= startOfToday }
            hostedEvents = try await hosted.filter { $0.eventDate.toDate ?? .distantPast >= startOfToday }
            myGroups = try await groups
            pendingInvitations = (try? await invitations) ?? []
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func respondToInvitation(_ invitation: PendingGroupInvitation, accept: Bool) async {
        guard let services else { return }
        do {
            try await services.groups.respondToInvitation(invitationId: invitation.id, accept: accept)
            pendingInvitations.removeAll { $0.id == invitation.id }
            if accept {
                await loadDashboard()
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    var managedGroups: [GroupSummary] {
        myGroups.filter { $0.userRole == .owner || $0.userRole == .admin }
    }

    var memberGroups: [GroupSummary] {
        myGroups.filter { $0.userRole == .member }
    }
}
