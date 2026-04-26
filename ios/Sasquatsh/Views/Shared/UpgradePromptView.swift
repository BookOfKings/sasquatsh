import SwiftUI

enum LimitType {
    case games
    case groups
    case items

    var title: String {
        switch self {
        case .games: return "Game Limit Reached"
        case .groups: return "Group Limit Reached"
        case .items: return "Items to Bring"
        }
    }

    var message: String {
        switch self {
        case .games: return "You've reached the maximum number of games for your current plan."
        case .groups: return "You've reached the maximum number of groups for your current plan."
        case .items: return "Track items to bring to game night with a Pro plan or higher."
        }
    }

    var icon: String {
        switch self {
        case .games: return "gamecontroller"
        case .groups: return "person.3"
        case .items: return "bag"
        }
    }
}

struct UpgradePromptView: View {
    let limitType: LimitType
    let currentTier: SubscriptionTier
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    private var recommendedTier: SubscriptionTier? {
        TierConfig.recommendedUpgrade(from: currentTier)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                // Icon
                Image(systemName: limitType.icon)
                    .font(.system(size: 48))
                    .foregroundStyle(Color.md3Tertiary)

                // Title & Message
                VStack(spacing: 8) {
                    Text(limitType.title)
                        .font(.md3HeadlineMedium)
                        .multilineTextAlignment(.center)

                    Text(limitType.message)
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)
                }

                // Recommended tier card
                if let tier = recommendedTier {
                    VStack(spacing: 12) {
                        HStack {
                            SubscriptionBadgeView(tier: tier)
                            Text(tier.priceLabel)
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3Primary)
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(TierConfig.tierFeatureDescriptions(for: tier), id: \.self) { feature in
                                HStack(spacing: 6) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3Primary)
                                    Text(feature)
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                        }
                    }
                    .padding()
                    .cardStyle()
                }

                Spacer()

                // Buttons
                VStack(spacing: 12) {
                    NavigationLink {
                        PricingView()
                    } label: {
                        Text("View Plans")
                            .primaryButtonStyle()
                    }

                    Button {
                        dismiss()
                    } label: {
                        Text("Maybe Later")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                    }
                }
            }
            .padding()
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Upgrade")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
        }
    }
}
