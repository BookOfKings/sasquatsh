import SwiftUI

struct PricingView: View {
    @Environment(AuthViewModel.self) private var authVM

    private var currentTier: SubscriptionTier {
        authVM.user?.subscriptionTier ?? .free
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    Text("Simple, Transparent Pricing")
                        .font(.md3HeadlineMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .multilineTextAlignment(.center)

                    Text("Choose the plan that fits your game nights")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 8)
                .padding(.horizontal)

                // Free Tier
                TierCardView(
                    tierName: "Free",
                    price: "$0",
                    priceSubtitle: "forever",
                    features: [
                        "Host 1 game per event",
                        "Create 1 group",
                        "Basic event management",
                        "Join unlimited events",
                    ],
                    isPopular: false,
                    isCurrent: currentTier == .free,
                    buttonTitle: currentTier == .free ? "Current Plan" : "Downgrade",
                    isDisabled: currentTier == .free
                ) {}
                .padding(.horizontal)

                // Basic Tier
                TierCardView(
                    tierName: "Basic",
                    price: "$4.99",
                    priceSubtitle: "/month",
                    features: [
                        "Up to 5 games per event",
                        "Create up to 5 groups",
                        "1 recurring game per group",
                        "Table/room/hall locations",
                        "Game night planning",
                        "Items to bring lists",
                        "Event chat",
                        "No ads",
                    ],
                    isPopular: true,
                    isCurrent: currentTier == .basic,
                    buttonTitle: currentTier == .basic ? "Current Plan" : (currentTier.rank < SubscriptionTier.basic.rank ? "Upgrade" : "Downgrade"),
                    isDisabled: currentTier == .basic
                ) {
                    openBillingPortal()
                }
                .padding(.horizontal)

                // Pro Tier
                TierCardView(
                    tierName: "Pro",
                    price: "$7.99",
                    priceSubtitle: "/month",
                    features: [
                        "Up to 10 games per event",
                        "Create up to 10 groups",
                        "Unlimited recurring games",
                        "Table/room/hall locations",
                        "Game night planning",
                        "Items to bring lists",
                        "Event chat",
                        "No ads",
                    ],
                    isPopular: false,
                    isCurrent: currentTier == .pro,
                    buttonTitle: currentTier == .pro ? "Current Plan" : (currentTier.rank < SubscriptionTier.pro.rank ? "Upgrade" : "Downgrade"),
                    isDisabled: currentTier == .pro
                ) {
                    openBillingPortal()
                }
                .padding(.horizontal)

                // Enterprise section
                VStack(spacing: 12) {
                    Image(systemName: "building.2.fill")
                        .font(.system(size: 32))
                        .foregroundStyle(Color.md3Primary)

                    Text("Need More?")
                        .font(.md3TitleLarge)
                        .foregroundStyle(Color.md3OnSurface)

                    Text("Contact us for custom plans with unlimited everything, custom branding, and API access.")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)

                    Link(destination: URL(string: "https://sasquatsh.com/contact")!) {
                        Text("Contact Us")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .overlay(
                                RoundedRectangle(cornerRadius: MD3Shape.medium)
                                    .stroke(Color.md3Primary, lineWidth: 1)
                            )
                    }
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(
                        colors: [Color.md3PrimaryContainer.opacity(0.3), Color.md3TertiaryContainer.opacity(0.3)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))
                .padding(.horizontal)

                Spacer(minLength: 20)
            }
            .padding(.vertical)
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Pricing")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func openBillingPortal() {
        if let url = URL(string: "https://sasquatsh.com/billing") {
            UIApplication.shared.open(url)
        }
    }
}
