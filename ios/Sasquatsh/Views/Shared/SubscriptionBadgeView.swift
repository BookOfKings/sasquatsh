import SwiftUI

struct SubscriptionBadgeView: View {
    let tier: SubscriptionTier

    private var badgeColor: Color {
        switch tier {
        case .free: return .md3SurfaceContainerHigh
        case .basic: return .md3PrimaryContainer
        case .pro: return .md3TertiaryContainer
        case .premium: return .md3SecondaryContainer
        }
    }

    var body: some View {
        BadgeView(text: tier.displayName, color: badgeColor)
    }
}
