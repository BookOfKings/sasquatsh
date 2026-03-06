import SwiftUI

@Observable
@MainActor
final class ProfileViewModel {
    var profile: UserProfile?
    var blockedUsers: [BlockedUser] = []
    var isLoading = false
    var isUploadingAvatar = false
    var error: String?
    var successMessage: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadProfile() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            profile = try await services.profile.getMyProfile()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func updateProfile(input: UpdateProfileInput) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            profile = try await services.profile.updateProfile(input: input)
            successMessage = "Profile updated!"
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func blockUser(userId: String) async {
        guard let services else { return }
        error = nil
        do {
            let result = try await services.profile.blockUser(userId: userId)
            profile?.blockedUserIds = result.blockedUserIds
            successMessage = result.message
        } catch {
            self.error = error.localizedDescription
        }
    }

    func unblockUser(userId: String) async {
        guard let services else { return }
        error = nil
        do {
            let result = try await services.profile.unblockUser(userId: userId)
            profile?.blockedUserIds = result.blockedUserIds
            blockedUsers.removeAll { $0.id == userId }
            successMessage = result.message
        } catch {
            self.error = error.localizedDescription
        }
    }

    func uploadAvatar(imageData: Data) async {
        guard let services else { return }
        isUploadingAvatar = true
        error = nil
        do {
            let result = try await services.profile.uploadAvatar(
                imageData: imageData,
                fileName: "avatar.jpg",
                mimeType: "image/jpeg"
            )
            profile = result.user
            successMessage = result.message
        } catch {
            self.error = error.localizedDescription
        }
        isUploadingAvatar = false
    }

    func deleteAvatar() async {
        guard let services else { return }
        error = nil
        do {
            let result = try await services.profile.deleteAvatar()
            profile = result.user
            successMessage = result.message
        } catch {
            self.error = error.localizedDescription
        }
    }

    func checkUsername(_ username: String) async -> UsernameCheckResponse? {
        guard let services else { return nil }
        do {
            return try await services.profile.checkUsername(username: username)
        } catch {
            return nil
        }
    }
}
