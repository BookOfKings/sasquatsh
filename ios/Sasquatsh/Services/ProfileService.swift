import Foundation

protocol ProfileServiceProtocol: Sendable {
    func getMyProfile() async throws -> UserProfile
    func getPublicProfile(userId: String) async throws -> PublicProfile
    func updateProfile(input: UpdateProfileInput) async throws -> UserProfile
    func blockUser(userId: String) async throws -> BlockActionResponse
    func unblockUser(userId: String) async throws -> BlockActionResponse
    func checkUsername(username: String) async throws -> UsernameCheckResponse
    func uploadAvatar(imageData: Data, fileName: String, mimeType: String) async throws -> AvatarUploadResponse
    func deleteAvatar() async throws -> AvatarDeleteResponse
}

struct BlockActionResponse: Codable {
    let message: String
    let blockedUserIds: [String]
}

final class ProfileService: ProfileServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getMyProfile() async throws -> UserProfile {
        try await api.get("profile", authenticated: true)
    }

    func getPublicProfile(userId: String) async throws -> PublicProfile {
        try await api.get("profile", queryItems: [.init(name: "id", value: userId)], authenticated: true)
    }

    func updateProfile(input: UpdateProfileInput) async throws -> UserProfile {
        try await api.put("profile", body: input)
    }

    func blockUser(userId: String) async throws -> BlockActionResponse {
        try await api.post("profile", queryItems: [
            .init(name: "action", value: "block"),
            .init(name: "userId", value: userId)
        ])
    }

    func unblockUser(userId: String) async throws -> BlockActionResponse {
        try await api.post("profile", queryItems: [
            .init(name: "action", value: "unblock"),
            .init(name: "userId", value: userId)
        ])
    }

    func checkUsername(username: String) async throws -> UsernameCheckResponse {
        try await api.get("check-username", queryItems: [.init(name: "username", value: username)], authenticated: true)
    }

    func uploadAvatar(imageData: Data, fileName: String, mimeType: String) async throws -> AvatarUploadResponse {
        try await api.postMultipart(
            "profile",
            fileData: imageData,
            fileName: fileName,
            mimeType: mimeType,
            fieldName: "avatar",
            queryItems: [.init(name: "action", value: "upload-avatar")]
        )
    }

    func deleteAvatar() async throws -> AvatarDeleteResponse {
        try await api.post("profile", queryItems: [.init(name: "action", value: "delete-avatar")])
    }
}
