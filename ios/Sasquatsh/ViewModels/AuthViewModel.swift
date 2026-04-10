import SwiftUI
import FirebaseAuth

@Observable
@MainActor
final class AuthViewModel {
    var user: User?
    var isLoading = false
    var isInitialized = false
    var error: String?
    var isAuthenticated: Bool { user != nil }

    private var services: ServiceContainer?
    private var authHandle: AuthStateDidChangeListenerHandle?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func initialize() async {
        guard let services else { return }

        guard AppDelegate.firebaseConfigured else {
            isInitialized = true
            isLoading = false
            error = "Firebase not configured. Add GoogleService-Info.plist from Firebase Console."
            return
        }

        isLoading = true

        authHandle = services.auth.addStateListener { [weak self] firebaseUser in
            Task { @MainActor in
                guard let self else { return }
                if firebaseUser != nil {
                    await self.syncUser()
                } else {
                    self.user = nil
                }
                self.isInitialized = true
                self.isLoading = false
            }
        }
    }

    func login(email: String, password: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            _ = try await services.auth.login(email: email, password: password)
            await syncUser()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func signup(email: String, password: String, displayName: String, username: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            let firebaseUser = try await services.auth.signup(email: email, password: password)
            let changeRequest = firebaseUser.createProfileChangeRequest()
            changeRequest.displayName = displayName
            try await changeRequest.commitChanges()

            struct SyncBody: Codable {
                let username: String
            }
            let _: User = try await services.api.post("auth-sync", body: SyncBody(username: username), authenticated: true)
            await syncUser()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func signInWithGoogle() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            _ = try await services.auth.signInWithGoogle()
            await syncUser()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func logout() {
        guard let services else { return }
        do {
            try services.auth.logout()
            user = nil
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func syncUser() async {
        guard let services else { return }
        do {
            // Include FCM token if available
            struct SyncInput: Codable {
                let fcmToken: String?
            }
            let input = SyncInput(fcmToken: AppDelegate.fcmToken)
            let syncedUser: User = try await services.api.post("auth-sync", body: input, authenticated: true)
            self.user = syncedUser
            self.error = nil
        } catch {
            self.error = error.localizedDescription
        }
    }

    // Auth listener persists for app lifetime — no cleanup needed
}
