import SwiftUI

enum AppearanceMode: String, CaseIterable {
    case system, light, dark

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

@Observable
final class AppearanceManager {
    static let shared = AppearanceManager()

    @ObservationIgnored
    @AppStorage("appearanceMode") var mode: AppearanceMode = .system

    var colorScheme: ColorScheme? {
        mode.colorScheme
    }
}
