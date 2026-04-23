import SwiftUI
import FirebaseAuth

@main
struct SasquatshApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    @Environment(\.scenePhase) private var scenePhase
    @State private var services = ServiceContainer()
    @State private var authVM = AuthViewModel()
    @State private var deepLinkHandler = DeepLinkHandler()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.services, services)
                .environment(authVM)
                .environment(deepLinkHandler)
                .onOpenURL { url in
                    deepLinkHandler.handle(url: url)
                }
                .task {
                    await services.api.setTokenProvider {
                        await services.auth.getIdToken()
                    }
                    authVM.configure(services: services)
                    await authVM.initialize()

                    // UI Testing: auto-login with test credentials
                    if ProcessInfo.processInfo.arguments.contains("--uitesting"),
                       let email = ProcessInfo.processInfo.environment["TEST_EMAIL"],
                       let password = ProcessInfo.processInfo.environment["TEST_PASSWORD"],
                       !authVM.isAuthenticated {
                        await authVM.login(email: email, password: password)
                    }

                    services.storeKit.configure(api: services.api)
                    await services.storeKit.loadProducts()
                }
                .onChange(of: scenePhase) {
                    if scenePhase == .active {
                        // Safety net: re-enable idle timer when app becomes active.
                        // Toolbox views (Turn Tracker, Round Counter) will re-disable
                        // it in their own onAppear if they're still showing.
                        UIApplication.shared.isIdleTimerDisabled = false
                    }
                }
        }
    }
}
