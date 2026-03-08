import SwiftUI

@Observable
@MainActor
final class DashboardViewModel {
    var registeredEvents: [EventSummary] = []
    var hostedEvents: [EventSummary] = []
    var myGroups: [GroupSummary] = []
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

            registeredEvents = try await registered
            hostedEvents = try await hosted
            myGroups = try await groups
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    var managedGroups: [GroupSummary] {
        myGroups.filter { $0.userRole == .owner || $0.userRole == .admin }
    }

    var memberGroups: [GroupSummary] {
        myGroups.filter { $0.userRole == .member }
    }
}
