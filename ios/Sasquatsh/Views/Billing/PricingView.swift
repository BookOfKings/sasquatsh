import SwiftUI

struct PricingView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var billingPeriod: SubscriptionPeriod = .monthly
    @Environment(\.dismiss) private var dismiss
    @State private var purchaseError: String?
    @State private var showError = false
    @State private var showSuccess = false
    @State private var purchasedTierName = ""

    private var currentTier: SubscriptionTier {
        authVM.user?.effectiveTier ?? .free
    }

    private var storeKit: StoreKitService {
        services.storeKit
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

                // Monthly / Annual toggle
                Picker("Billing Period", selection: $billingPeriod) {
                    Text("Monthly").tag(SubscriptionPeriod.monthly)
                    Text("Annual (Save ~17%)").tag(SubscriptionPeriod.annual)
                }
                .pickerStyle(.segmented)
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
                let basicProduct = storeKit.product(for: .basic, period: billingPeriod)
                TierCardView(
                    tierName: "Basic",
                    price: basicProduct?.product.displayPrice ?? (billingPeriod == .monthly ? "$4.99" : "$49.99"),
                    priceSubtitle: billingPeriod == .monthly ? "/month" : "/year",
                    features: [
                        "Up to 5 games per event",
                        "Create up to 5 groups",
                        "1 recurring game per group",
                        "Table/room/hall locations",
                        "Game night planning",
                        "Event chat",
                        "No ads",
                    ],
                    isPopular: true,
                    isCurrent: currentTier == .basic,
                    buttonTitle: buttonTitle(for: .basic),
                    isDisabled: currentTier == .basic || storeKit.purchaseInProgress
                ) {
                    Task { await purchaseTier(.basic) }
                }
                .padding(.horizontal)

                // Pro Tier
                let proProduct = storeKit.product(for: .pro, period: billingPeriod)
                TierCardView(
                    tierName: "Pro",
                    price: proProduct?.product.displayPrice ?? (billingPeriod == .monthly ? "$7.99" : "$79.99"),
                    priceSubtitle: billingPeriod == .monthly ? "/month" : "/year",
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
                    buttonTitle: buttonTitle(for: .pro),
                    isDisabled: currentTier == .pro || storeKit.purchaseInProgress
                ) {
                    Task { await purchaseTier(.pro) }
                }
                .padding(.horizontal)

                // Restore purchases
                Button {
                    Task {
                        try? await storeKit.restorePurchases()
                    }
                } label: {
                    Text("Restore Purchases")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3Primary)
                }
                .padding(.top, 4)

                // Legal links (Apple requirement)
                VStack(spacing: 8) {
                    Link("Terms of Service", destination: URL(string: "https://sasquatsh.com/terms")!)
                        .font(.md3BodySmall)
                    Link("Privacy Policy", destination: URL(string: "https://sasquatsh.com/privacy")!)
                        .font(.md3BodySmall)
                }
                .foregroundStyle(Color.md3OnSurfaceVariant)
                .padding(.bottom, 20)
            }
            .padding(.vertical)
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Pricing")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Purchase Error", isPresented: $showError) {
            Button("OK") {}
        } message: {
            Text(purchaseError ?? "An error occurred")
        }
        .alert("Welcome to \(purchasedTierName)!", isPresented: $showSuccess) {
            Button("Let's Go") {
                dismiss()
            }
        } message: {
            Text("Your subscription is now active. Enjoy your upgraded features!")
        }
        .overlay {
            if storeKit.purchaseInProgress {
                Color.black.opacity(0.3).ignoresSafeArea()
                D20ProgressView(size: 40, message: "Processing...")
                    .padding(24)
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            }
        }
    }

    private func buttonTitle(for tier: SubscriptionTier) -> String {
        if currentTier == tier { return "Current Plan" }
        if currentTier.rank < tier.rank { return "Upgrade" }
        return "Downgrade"
    }

    private func purchaseTier(_ tier: SubscriptionTier) async {
        guard let product = storeKit.product(for: tier, period: billingPeriod) else {
            purchaseError = "Product not available"
            showError = true
            return
        }

        do {
            try await storeKit.purchase(product)
            purchasedTierName = tier.displayName
            showSuccess = true
            // Refresh user data to pick up new tier
            await authVM.refreshUser()
        } catch StoreKitError.userCancelled {
            // Silently ignore
        } catch {
            purchaseError = error.localizedDescription
            showError = true
        }
    }
}
