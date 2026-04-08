import SwiftUI

extension GameSystem {
    var badgeColor: Color {
        switch self {
        case .boardGame: return .md3PrimaryContainer
        case .mtg: return .md3SecondaryContainer
        case .pokemonTcg: return .md3TertiaryContainer
        case .yugioh: return Color(red: 0.3, green: 0.2, blue: 0.5).opacity(0.3)
        case .warhammer40k: return Color(red: 0.5, green: 0.1, blue: 0.1).opacity(0.3)
        }
    }

    var badgeTextColor: Color {
        switch self {
        case .boardGame: return .md3OnPrimaryContainer
        case .mtg: return .md3OnSecondaryContainer
        case .pokemonTcg: return .md3OnTertiaryContainer
        case .yugioh: return .md3OnSurface
        case .warhammer40k: return .md3OnSurface
        }
    }
}
