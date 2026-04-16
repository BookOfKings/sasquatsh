import SwiftUI
import UIKit

// MARK: - MD3 Color Roles (Light + Dark)

extension Color {
    // Helper: create adaptive color from light/dark hex
    private static func adaptive(light: UIColor, dark: UIColor) -> Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark ? dark : light
        })
    }

    // Primary
    static let md3Primary = adaptive(
        light: UIColor(red: 0.173, green: 0.353, blue: 0.235, alpha: 1),   // #2C5A3C
        dark: UIColor(red: 0.478, green: 0.804, blue: 0.557, alpha: 1)     // #7ACD8E bright green
    )
    static let md3OnPrimary = adaptive(
        light: .white,
        dark: UIColor(red: 0.09, green: 0.17, blue: 0.12, alpha: 1)        // #172C1F dark on bright
    )
    static let md3PrimaryContainer = adaptive(
        light: UIColor(red: 0.863, green: 0.925, blue: 0.871, alpha: 1),   // #DCECDE
        dark: UIColor(red: 0.157, green: 0.263, blue: 0.180, alpha: 1)     // #28432E
    )
    static let md3OnPrimaryContainer = adaptive(
        light: UIColor(red: 0.090, green: 0.173, blue: 0.122, alpha: 1),   // #172C1F
        dark: UIColor(red: 0.863, green: 0.925, blue: 0.871, alpha: 1)     // #DCECDE
    )

    // Secondary
    static let md3Secondary = adaptive(
        light: UIColor(red: 0.420, green: 0.267, blue: 0.133, alpha: 1),   // #6B4422
        dark: UIColor(red: 0.824, green: 0.620, blue: 0.447, alpha: 1)     // #D29E72
    )
    static let md3OnSecondary = adaptive(
        light: .white,
        dark: UIColor(red: 0.212, green: 0.129, blue: 0.075, alpha: 1)     // #362113
    )
    static let md3SecondaryContainer = adaptive(
        light: UIColor(red: 0.945, green: 0.910, blue: 0.875, alpha: 1),   // #F1E8DF
        dark: UIColor(red: 0.290, green: 0.200, blue: 0.130, alpha: 1)     // #4A3321
    )
    static let md3OnSecondaryContainer = adaptive(
        light: UIColor(red: 0.212, green: 0.129, blue: 0.075, alpha: 1),   // #362113
        dark: UIColor(red: 0.945, green: 0.910, blue: 0.875, alpha: 1)     // #F1E8DF
    )

    // Tertiary (Gold)
    static let md3Tertiary = adaptive(
        light: UIColor(red: 0.827, green: 0.573, blue: 0.035, alpha: 1),   // #D39209
        dark: UIColor(red: 0.976, green: 0.780, blue: 0.310, alpha: 1)     // #F9C74F
    )
    static let md3OnTertiary = adaptive(
        light: .white,
        dark: UIColor(red: 0.400, green: 0.231, blue: 0.071, alpha: 1)     // #663B12
    )
    static let md3TertiaryContainer = adaptive(
        light: UIColor(red: 0.988, green: 0.937, blue: 0.780, alpha: 1),   // #FCEFC7
        dark: UIColor(red: 0.400, green: 0.290, blue: 0.050, alpha: 1)     // #664A0D
    )
    static let md3OnTertiaryContainer = adaptive(
        light: UIColor(red: 0.400, green: 0.231, blue: 0.071, alpha: 1),   // #663B12
        dark: UIColor(red: 0.988, green: 0.937, blue: 0.780, alpha: 1)     // #FCEFC7
    )

    // Surface / Background
    static let md3Surface = adaptive(
        light: .white,
        dark: UIColor(red: 0.071, green: 0.071, blue: 0.078, alpha: 1)     // #121214
    )
    static let md3SurfaceVariant = adaptive(
        light: UIColor(red: 0.906, green: 0.878, blue: 0.925, alpha: 1),   // #E7E0EC
        dark: UIColor(red: 0.180, green: 0.176, blue: 0.196, alpha: 1)     // #2E2D32
    )
    static let md3OnSurface = adaptive(
        light: UIColor(red: 0.110, green: 0.106, blue: 0.122, alpha: 1),   // #1C1B1F
        dark: UIColor(red: 0.906, green: 0.894, blue: 0.918, alpha: 1)     // #E7E4EA
    )
    static let md3OnSurfaceVariant = adaptive(
        light: UIColor(red: 0.286, green: 0.271, blue: 0.310, alpha: 1),   // #49454F
        dark: UIColor(red: 0.792, green: 0.769, blue: 0.816, alpha: 1)     // #CAC4D0
    )
    static let md3SurfaceContainerLowest = adaptive(
        light: .white,
        dark: UIColor(red: 0.055, green: 0.055, blue: 0.063, alpha: 1)     // #0E0E10
    )
    static let md3SurfaceContainerLow = adaptive(
        light: UIColor(red: 0.969, green: 0.949, blue: 0.980, alpha: 1),   // #F7F2FA
        dark: UIColor(red: 0.110, green: 0.106, blue: 0.122, alpha: 1)     // #1C1B1F
    )
    static let md3SurfaceContainer = adaptive(
        light: UIColor(red: 0.961, green: 0.961, blue: 0.937, alpha: 1),   // #F5F5EF
        dark: UIColor(red: 0.129, green: 0.125, blue: 0.141, alpha: 1)     // #212024
    )
    static let md3SurfaceContainerHigh = adaptive(
        light: UIColor(red: 0.925, green: 0.902, blue: 0.941, alpha: 1),   // #ECE6F0
        dark: UIColor(red: 0.176, green: 0.173, blue: 0.192, alpha: 1)     // #2D2C31
    )

    // Error
    static let md3Error = adaptive(
        light: UIColor(red: 0.702, green: 0.149, blue: 0.118, alpha: 1),   // #B3261E
        dark: UIColor(red: 0.976, green: 0.710, blue: 0.694, alpha: 1)     // #F9B5B1
    )
    static let md3OnError = adaptive(
        light: .white,
        dark: UIColor(red: 0.376, green: 0.078, blue: 0.059, alpha: 1)     // #60140F
    )
    static let md3ErrorContainer = adaptive(
        light: UIColor(red: 0.976, green: 0.871, blue: 0.863, alpha: 1),   // #F9DEDC
        dark: UIColor(red: 0.557, green: 0.114, blue: 0.090, alpha: 1)     // #8E1D17
    )

    // Outline
    static let md3Outline = adaptive(
        light: UIColor(red: 0.475, green: 0.455, blue: 0.494, alpha: 1),   // #79747E
        dark: UIColor(red: 0.573, green: 0.557, blue: 0.592, alpha: 1)     // #928E97
    )
    static let md3OutlineVariant = adaptive(
        light: UIColor(red: 0.792, green: 0.769, blue: 0.816, alpha: 1),   // #CAC4D0
        dark: UIColor(red: 0.286, green: 0.271, blue: 0.310, alpha: 1)     // #49454F
    )
}

// MARK: - MD3 Shape System

enum MD3Shape {
    static let extraSmall: CGFloat = 4
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let large: CGFloat = 16
    static let extraLarge: CGFloat = 28
    static let full: CGFloat = .infinity
}

// MARK: - MD3 Elevation

enum MD3Elevation {
    case level0
    case level1
    case level2
    case level3

    var shadowRadius: CGFloat {
        switch self {
        case .level0: return 0
        case .level1: return 3
        case .level2: return 6
        case .level3: return 8
        }
    }

    var shadowY: CGFloat {
        switch self {
        case .level0: return 0
        case .level1: return 1
        case .level2: return 2
        case .level3: return 4
        }
    }

    var shadowOpacity: Double {
        switch self {
        case .level0: return 0
        case .level1: return 0.10
        case .level2: return 0.12
        case .level3: return 0.14
        }
    }
}
