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
    var isUploadingLogo = false
    var error: String?
    var isEditing = false
    var currentLogoUrl: String?

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
        currentLogoUrl = group.logoUrl
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

    func uploadLogo(imageData: Data) async {
        guard let services else {
            error = "Services not configured"
            return
        }
        guard let groupId else {
            error = "Group ID not set"
            return
        }
        guard !imageData.isEmpty else {
            error = "No image data"
            return
        }
        isUploadingLogo = true
        error = nil
        do {
            let url = try await services.groups.uploadLogo(
                groupId: groupId,
                imageData: imageData,
                fileName: "logo.jpg",
                mimeType: "image/jpeg"
            )
            currentLogoUrl = url
        } catch {
            self.error = "Upload failed: \(error.localizedDescription)"
        }
        isUploadingLogo = false
    }

    func removeLogo() async {
        guard let services, let groupId else { return }
        error = nil
        do {
            let input = UpdateGroupInput(logoUrl: "")
            _ = try await services.groups.updateGroup(id: groupId, input: input)
            currentLogoUrl = nil
        } catch {
            self.error = error.localizedDescription
        }
    }

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
