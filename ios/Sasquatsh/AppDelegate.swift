import UIKit
import FirebaseCore
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {
    static var firebaseConfigured = false

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let dict = NSDictionary(contentsOfFile: path),
           let appId = dict["GOOGLE_APP_ID"] as? String,
           !appId.contains("placeholder") {
            FirebaseApp.configure()
            Self.firebaseConfigured = true
        } else {
            print("[Sasquatsh] GoogleService-Info.plist not configured. Download it from Firebase Console.")
            print("[Sasquatsh] Running in preview mode — auth features disabled.")
        }

        configureAppearance()
        return true
    }

    private func configureAppearance() {
        // MD3 Nav Bar
        let largeTitleFont = UIFont(name: "Inter-Bold", size: 26) ?? .boldSystemFont(ofSize: 26)
        let titleFont = UIFont(name: "Inter-Medium", size: 16) ?? .systemFont(ofSize: 16, weight: .medium)
        let navTitleColor = UIColor(red: 0.110, green: 0.106, blue: 0.122, alpha: 1) // md3OnSurface

        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithDefaultBackground()
        navAppearance.backgroundColor = .white // md3Surface
        navAppearance.largeTitleTextAttributes = [.font: largeTitleFont, .foregroundColor: navTitleColor]
        navAppearance.titleTextAttributes = [.font: titleFont, .foregroundColor: navTitleColor]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance

        // MD3 Tab Bar
        let tabFont = UIFont(name: "Inter-Medium", size: 10) ?? .systemFont(ofSize: 10)
        let tabAppearance = UITabBarAppearance()
        tabAppearance.configureWithDefaultBackground()
        // md3SurfaceContainer background
        tabAppearance.backgroundColor = UIColor(red: 0.961, green: 0.961, blue: 0.941, alpha: 1)

        let itemAppearance = UITabBarItemAppearance()
        let unselectedColor = UIColor(red: 0.286, green: 0.271, blue: 0.310, alpha: 1) // md3OnSurfaceVariant
        itemAppearance.normal.titleTextAttributes = [.font: tabFont, .foregroundColor: unselectedColor]
        itemAppearance.normal.iconColor = unselectedColor
        itemAppearance.selected.titleTextAttributes = [.font: tabFont]
        tabAppearance.stackedLayoutAppearance = itemAppearance
        UITabBar.appearance().standardAppearance = tabAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabAppearance
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        guard Self.firebaseConfigured else { return false }
        return GIDSignIn.sharedInstance.handle(url)
    }
}
