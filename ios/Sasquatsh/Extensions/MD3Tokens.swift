import SwiftUI

// MARK: - MD3 Color Roles

extension Color {
    // Primary
    static let md3Primary = Color.primaryBrand       // #2D5A3D Forest Green
    static let md3OnPrimary = Color.white
    static let md3PrimaryContainer = Color.primary100
    static let md3OnPrimaryContainer = Color.primary900

    // Secondary
    static let md3Secondary = Color.secondary         // #6B4423 Leather Brown
    static let md3OnSecondary = Color.white
    static let md3SecondaryContainer = Color.secondary100
    static let md3OnSecondaryContainer = Color.secondary900

    // Tertiary (Gold)
    static let md3Tertiary = Color.accent             // #D4920A Warm Gold
    static let md3OnTertiary = Color.white
    static let md3TertiaryContainer = Color.accent100
    static let md3OnTertiaryContainer = Color.accent900

    // Surface / Background
    static let md3Surface = Color.white
    static let md3SurfaceVariant = Color(red: 0.906, green: 0.878, blue: 0.925)       // #E7E0EC
    static let md3OnSurface = Color(red: 0.110, green: 0.106, blue: 0.122)             // #1C1B1F
    static let md3OnSurfaceVariant = Color(red: 0.286, green: 0.271, blue: 0.310)      // #49454F
    static let md3SurfaceContainerLowest = Color.white
    static let md3SurfaceContainerLow = Color(red: 0.969, green: 0.949, blue: 0.980)   // #F7F2FA
    static let md3SurfaceContainer = Color.appBackground                                // #f5f5f0
    static let md3SurfaceContainerHigh = Color(red: 0.925, green: 0.902, blue: 0.941)  // #ECE6F0

    // Error
    static let md3Error = Color(red: 0.702, green: 0.149, blue: 0.118)                 // #B3261E
    static let md3OnError = Color.white
    static let md3ErrorContainer = Color(red: 0.976, green: 0.871, blue: 0.863)         // #F9DEDC

    // Outline
    static let md3Outline = Color(red: 0.475, green: 0.455, blue: 0.494)               // #79747E
    static let md3OutlineVariant = Color(red: 0.792, green: 0.769, blue: 0.816)         // #CAC4D0
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
