import Foundation
import FirebaseAuth
import GoogleSignIn
import FirebaseCore
import AuthenticationServices
import CryptoKit

protocol AuthServiceProtocol: Sendable {
    func login(email: String, password: String) async throws -> FirebaseAuth.User
    func signup(email: String, password: String) async throws -> FirebaseAuth.User
    func signInWithGoogle() async throws -> FirebaseAuth.User
    func signInWithApple() async throws -> FirebaseAuth.User
    func logout() throws
    func getIdToken() async -> String?
    func getCurrentUser() -> FirebaseAuth.User?
    func addStateListener(_ callback: @escaping (FirebaseAuth.User?) -> Void) -> AuthStateDidChangeListenerHandle
    func removeStateListener(_ handle: AuthStateDidChangeListenerHandle)
    func resetPassword(email: String) async throws
    func changePassword(newPassword: String) async throws
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

    // Apple Sign-In nonce for Firebase
    private var currentNonce: String?

    func signInWithApple() async throws -> FirebaseAuth.User {
        let nonce = randomNonceString()
        currentNonce = nonce
        let hashedNonce = sha256(nonce)

        let appleResult = try await performAppleSignIn(hashedNonce: hashedNonce)

        guard let appleIDCredential = appleResult.credential as? ASAuthorizationAppleIDCredential,
              let appleIDToken = appleIDCredential.identityToken,
              let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
            throw APIError.serverError("Unable to get Apple ID token")
        }

        let credential = OAuthProvider.appleCredential(
            withIDToken: idTokenString,
            rawNonce: nonce,
            fullName: appleIDCredential.fullName
        )
        let authResult = try await Auth.auth().signIn(with: credential)
        return authResult.user
    }

    private func performAppleSignIn(hashedNonce: String) async throws -> ASAuthorization {
        try await withCheckedThrowingContinuation { continuation in
            let delegate = AppleSignInDelegate(continuation: continuation)
            // Retain the delegate
            objc_setAssociatedObject(self, "appleDelegate", delegate, .OBJC_ASSOCIATION_RETAIN)

            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = hashedNonce

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = delegate
            controller.performRequests()
        }
    }

    private func randomNonceString(length: Int = 32) -> String {
        var randomBytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    private func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
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

    func changePassword(newPassword: String) async throws {
        guard let user = Auth.auth().currentUser else {
            throw NSError(domain: "AuthService", code: 0, userInfo: [NSLocalizedDescriptionKey: "Not signed in"])
        }
        try await user.updatePassword(to: newPassword)
    }
}

// MARK: - Apple Sign-In Delegate

private class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate {
    let continuation: CheckedContinuation<ASAuthorization, Error>

    init(continuation: CheckedContinuation<ASAuthorization, Error>) {
        self.continuation = continuation
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        continuation.resume(returning: authorization)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        continuation.resume(throwing: error)
    }
}
