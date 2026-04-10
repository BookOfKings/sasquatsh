import UIKit
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import GoogleSignIn
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    static var firebaseConfigured = false
    static var fcmToken: String?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let dict = NSDictionary(contentsOfFile: path),
           let appId = dict["GOOGLE_APP_ID"] as? String,
           !appId.contains("placeholder") {
            FirebaseApp.configure()
            // Use default keychain access (works on simulator without code signing)
            try? Auth.auth().useUserAccessGroup(nil)
            Self.firebaseConfigured = true

            // Set up push notifications
            UNUserNotificationCenter.current().delegate = self
            Messaging.messaging().delegate = self

            // Request notification permission
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
                if granted {
                    DispatchQueue.main.async {
                        application.registerForRemoteNotifications()
                    }
                }
                if let error {
                    print("[Sasquatsh] Notification permission error: \(error)")
                }
            }
        } else {
            print("[Sasquatsh] GoogleService-Info.plist not configured. Download it from Firebase Console.")
            print("[Sasquatsh] Running in preview mode — auth features disabled.")
        }

        configureAppearance()
        return true
    }

    // MARK: - Push Notification Token

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[Sasquatsh] Failed to register for remote notifications: \(error)")
    }

    // MARK: - FCM Delegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        print("[Sasquatsh] FCM token received: \(fcmToken.prefix(20))...")
        Self.fcmToken = fcmToken

        // Post notification so AuthViewModel can pick it up
        NotificationCenter.default.post(
            name: .fcmTokenReceived,
            object: nil,
            userInfo: ["token": fcmToken]
        )
    }

    // MARK: - Notification Display

    // Show notification even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }

    // Handle notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        // Could navigate to event detail using eventId from userInfo
        print("[Sasquatsh] Notification tapped: \(userInfo)")
        completionHandler()
    }

    // MARK: - Appearance

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

        // Hide system tab bar (using custom MD3 tab bar)
        UITabBar.appearance().isHidden = true
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

// MARK: - Notification Name

extension Notification.Name {
    static let fcmTokenReceived = Notification.Name("fcmTokenReceived")
}
