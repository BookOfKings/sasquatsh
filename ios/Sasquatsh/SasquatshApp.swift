import SwiftUI
import FirebaseAuth

@main
struct SasquatshApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

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
                    services.storeKit.configure(api: services.api)
                    await services.storeKit.loadProducts()
                }
        }
    }
}
