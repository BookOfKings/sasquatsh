import SwiftUI

@Observable
@MainActor
final class CreateEditGroupViewModel {
    var name = ""
    var description = ""
    var groupType: GroupType = .geographic
    var locationCity = ""
    var locationState = ""
    var locationRadiusMiles: Int?
    var joinPolicy: JoinPolicy = .open

    var isLoading = false
    var error: String?
    var isEditing = false

    private var groupId: String?
    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadForEdit(group: GameGroup) {
        isEditing = true
        groupId = group.id
        name = group.name
        description = group.description ?? ""
        groupType = group.groupType
        locationCity = group.locationCity ?? ""
        locationState = group.locationState ?? ""
        locationRadiusMiles = group.locationRadiusMiles
        joinPolicy = group.joinPolicy
    }

    func save() async -> GameGroup? {
        guard let services else { return nil }
        isLoading = true
        error = nil

        do {
            if isEditing, let groupId {
                let input = UpdateGroupInput(
                    name: name,
                    description: description.isEmpty ? nil : description,
                    groupType: groupType,
                    locationCity: locationCity.isEmpty ? nil : locationCity,
                    locationState: locationState.isEmpty ? nil : locationState,
                    locationRadiusMiles: locationRadiusMiles,
                    joinPolicy: joinPolicy
                )
                let group = try await services.groups.updateGroup(id: groupId, input: input)
                isLoading = false
                return group
            } else {
                let input = CreateGroupInput(
                    name: name,
                    description: description.isEmpty ? nil : description,
                    groupType: groupType,
                    locationCity: locationCity.isEmpty ? nil : locationCity,
                    locationState: locationState.isEmpty ? nil : locationState,
                    locationRadiusMiles: locationRadiusMiles,
                    joinPolicy: joinPolicy
                )
                let group = try await services.groups.createGroup(input: input)
                isLoading = false
                return group
            }
        } catch {
            self.error = error.localizedDescription
            isLoading = false
            return nil
        }
    }

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
