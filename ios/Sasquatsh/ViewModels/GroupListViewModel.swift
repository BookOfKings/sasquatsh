import SwiftUI

@Observable
@MainActor
final class GroupListViewModel {
    var groups: [GroupSummary] = []
    var myGroupCount = 0
    var isLoading = false
    var error: String?
    var searchText = ""
    var selectedType: GroupType?
    var filterCity = ""
    var filterState = ""

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    var filter: GroupSearchFilter {
        GroupSearchFilter(
            search: searchText.isEmpty ? nil : searchText,
            groupType: selectedType,
            city: filterCity.isEmpty ? nil : filterCity,
            state: filterState.isEmpty ? nil : filterState
        )
    }

    var hasActiveFilters: Bool {
        selectedType != nil || !filterCity.isEmpty || !filterState.isEmpty
    }

    func loadGroups() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            async let publicGroups = services.groups.getPublicGroups(filter: filter)
            async let myGroupsList = services.groups.getMyGroups()

            var allGroups = try await publicGroups
            let myGroups = (try? await myGroupsList) ?? []
            myGroupCount = myGroups.count

            // Build a map of group ID → user role from my groups
            let roleMap = Dictionary(uniqueKeysWithValues: myGroups.compactMap { g -> (String, MemberRole)? in
                guard let role = g.userRole else { return nil }
                return (g.id, role)
            })

            // Annotate public groups with user's role
            for i in allGroups.indices {
                if let role = roleMap[allGroups[i].id] {
                    allGroups[i].userRole = role
                }
            }

            // Sort: user's groups first (by role: owner > admin > member), then others
            groups = allGroups.sorted { a, b in
                let aRole = a.userRole
                let bRole = b.userRole
                if aRole != nil && bRole == nil { return true }
                if aRole == nil && bRole != nil { return false }
                if let ar = aRole, let br = bRole {
                    let order: [MemberRole: Int] = [.owner: 0, .admin: 1, .member: 2]
                    return (order[ar] ?? 3) < (order[br] ?? 3)
                }
                return a.name < b.name
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func clearFilters() {
        searchText = ""
        selectedType = nil
        filterCity = ""
        filterState = ""
    }
}
