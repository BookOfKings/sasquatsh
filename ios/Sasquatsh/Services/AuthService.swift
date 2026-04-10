import Foundation
import FirebaseAuth
import GoogleSignIn
import FirebaseCore

protocol AuthServiceProtocol: Sendable {
    func login(email: String, password: String) async throws -> FirebaseAuth.User
    func signup(email: String, password: String) async throws -> FirebaseAuth.User
    func signInWithGoogle() async throws -> FirebaseAuth.User
    func logout() throws
    func getIdToken() async -> String?
    func getCurrentUser() -> FirebaseAuth.User?
    func addStateListener(_ callback: @escaping (FirebaseAuth.User?) -> Void) -> AuthStateDidChangeListenerHandle
    func removeStateListener(_ handle: AuthStateDidChangeListenerHandle)
    func resetPassword(email: String) async throws
}

final class AuthService: AuthServiceProtocol {
    static let shared = AuthService()
    private init() {}

    func login(email: String, password: String) async throws -> FirebaseAuth.User {
        let result = try await Auth.auth().signIn(withEmail: email, password: password)
        return result.user
    }

    func signup(email: String, password: String) async throws -> FirebaseAuth.User {
        let result = try await Auth.auth().createUser(withEmail: email, password: password)
        return result.user
    }

    func signInWithGoogle() async throws -> FirebaseAuth.User {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw APIError.serverError("Missing Firebase client ID")
        }

        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        guard let windowScene = await MainActor.run(body: {
            UIApplication.shared.connectedScenes.first as? UIWindowScene
        }),
        let rootViewController = await MainActor.run(body: {
            windowScene.windows.first?.rootViewController
        }) else {
            throw APIError.serverError("No root view controller found")
        }

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        guard let idToken = result.user.idToken?.tokenString else {
            throw APIError.serverError("No ID token from Google")
        }

        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: result.user.accessToken.tokenString
        )
        let authResult = try await Auth.auth().signIn(with: credential)
        return authResult.user
    }

    func logout() throws {
        try Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
    }

    func getIdToken() async -> String? {
        try? await Auth.auth().currentUser?.getIDToken()
    }

    func getCurrentUser() -> FirebaseAuth.User? {
        Auth.auth().currentUser
    }

    func addStateListener(_ callback: @escaping (FirebaseAuth.User?) -> Void) -> AuthStateDidChangeListenerHandle {
        Auth.auth().addStateDidChangeListener { _, user in
            callback(user)
        }
    }

    func removeStateListener(_ handle: AuthStateDidChangeListenerHandle) {
        Auth.auth().removeStateDidChangeListener(handle)
    }

    func resetPassword(email: String) async throws {
        try await Auth.auth().sendPasswordReset(withEmail: email)
    }
}
