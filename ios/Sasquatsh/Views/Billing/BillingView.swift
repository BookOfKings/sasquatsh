import SwiftUI

struct BillingView: View {
    @Environment(\.services) private var services
    @Environment(\.openURL) private var openURL
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = BillingViewModel()
    @State private var showCancelConfirm = false
    @State private var selectedInvoice: Invoice?

    private var storeKit: StoreKitService {
        services.storeKit
    }

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.subscriptionInfo == nil {
                LoadingView()
            } else {
                VStack(spacing: 20) {
                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    if let msg = vm.successMessage {
                        Text(msg)
                            .font(.md3BodyMedium)
                            .foregroundStyle(.green)
                            .padding(.horizontal)
                    }

                    currentPlanCard

                    NavigationLink {
                        PricingView()
                    } label: {
                        HStack {
                            Image(systemName: "sparkles")
                                .foregroundStyle(Color.md3Primary)
                            Text("View All Plans")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3Primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding()
                        .cardStyle()
                    }

                    // Only show Stripe payment/invoice sections for Stripe subscriptions
                    if vm.isStripeSubscription || (!vm.isAppleSubscription && !storeKit.hasActiveSubscription) {
                        paymentMethodCard
                        invoiceHistoryCard
                    }
                }
                .padding()
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Billing")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable {
            await vm.loadBillingInfo()
            await storeKit.updatePurchasedProducts()
        }
        .confirmationDialog("Cancel Subscription", isPresented: $showCancelConfirm) {
            Button("Cancel Subscription", role: .destructive) {
                Task { await vm.cancelSubscription() }
            }
        } message: {
            Text("Your subscription will remain active until the end of the current billing period.")
        }
        .sheet(item: $selectedInvoice) { invoice in
            InvoiceDetailSheet(invoice: invoice, viewModel: vm)
        }
        .task {
            vm.configure(services: services)
            await vm.loadBillingInfo()
            await storeKit.updatePurchasedProducts()
        }
    }

    // MARK: - Current Plan Card

    private var effectiveTier: SubscriptionTier {
        // Check StoreKit entitlements first, then server tier
        storeKit.currentTier ?? authVM.user?.effectiveTier ?? vm.currentTier
    }

    private var isApple: Bool {
        storeKit.hasActiveSubscription || vm.isAppleSubscription
    }

    private var currentPlanCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Current Plan")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                SubscriptionBadgeView(tier: effectiveTier)
            }

            // Price
            if isApple, let productId = storeKit.purchasedProductIDs.first,
               let product = storeKit.products.first(where: { $0.product.id == productId }) {
                Text("\(product.product.displayPrice)/\(product.period == .annual ? "year" : "mo")")
                    .font(.md3HeadlineSmall)
                    .foregroundStyle(Color.md3Primary)
            } else {
                Text(effectiveTier.priceLabel)
                    .font(.md3HeadlineSmall)
                    .foregroundStyle(Color.md3Primary)
            }

            // Subscription source
            if isApple {
                HStack(spacing: 6) {
                    Image(systemName: "apple.logo")
                        .font(.md3BodySmall)
                    Text("Managed by Apple")
                        .font(.md3BodySmall)
                }
                .foregroundStyle(Color.md3OnSurfaceVariant)
            } else if vm.isStripeSubscription {
                HStack(spacing: 6) {
                    Image(systemName: "creditcard.fill")
                        .font(.md3BodySmall)
                    Text("Managed by Stripe")
                        .font(.md3BodySmall)
                }
                .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            if vm.hasOverride {
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3Tertiary)
                    Text("Admin override active")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }

            // Features checklist
            VStack(alignment: .leading, spacing: 6) {
                ForEach(TierConfig.tierFeatureDescriptions(for: effectiveTier), id: \.self) { feature in
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

            Divider()

            // Action buttons
            if isApple {
                // Apple subscription management
                Button {
                    openURL(vm.manageAppleSubscriptionURL)
                } label: {
                    HStack {
                        Image(systemName: "apple.logo")
                        Text("Manage in App Store")
                    }
                    .outlinedButtonStyle()
                }

                Button {
                    Task {
                        try? await storeKit.restorePurchases()
                        await authVM.refreshUser()
                    }
                } label: {
                    Text("Restore Purchases")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3Primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 36)
                }
            } else if effectiveTier == .free && !vm.hasOverride {
                NavigationLink {
                    PricingView()
                } label: {
                    Text("Upgrade Plan")
                        .primaryButtonStyle()
                }
            } else if vm.isCancelled {
                Button {
                    Task { await vm.reactivateSubscription() }
                } label: {
                    Text("Reactivate Subscription")
                        .primaryButtonStyle()
                }
                .disabled(vm.actionLoading)

                if let expiresAt = vm.subscriptionInfo?.subscription.expiresAt {
                    Text("Access until \(vm.formattedDate(expiresAt))")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            } else if vm.hasActiveSubscription && vm.isStripeSubscription {
                HStack(spacing: 12) {
                    NavigationLink {
                        PricingView()
                    } label: {
                        Text("Change Plan")
                            .outlinedButtonStyle()
                    }

                    Button {
                        showCancelConfirm = true
                    } label: {
                        Text("Cancel")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Error)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                    }
                    .disabled(vm.actionLoading)
                }
            }

            if vm.isPastDue {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(Color.md3Error)
                    Text("Payment past due — please update your payment method")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3Error)
                }
            }
        }
        .padding()
        .cardStyle()
    }

    // MARK: - Payment Method Card (Stripe only)

    private var paymentMethodCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Payment Method")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if let pm = vm.subscriptionInfo?.paymentMethod {
                HStack {
                    Image(systemName: "creditcard.fill")
                        .foregroundStyle(Color.md3Primary)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(vm.formatCardBrand(pm.brand)) ••••\(pm.last4)")
                            .font(.md3BodyMedium)
                        Text("Expires \(pm.expMonth)/\(pm.expYear)")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    Spacer()
                }

                Button {
                    openURL(AppConfig.billingURL)
                } label: {
                    Text("Manage")
                        .outlinedButtonStyle()
                }
            } else {
                Text("No payment method on file")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
    }

    // MARK: - Invoice History Card (Stripe only)

    private var invoiceHistoryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Invoice History")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if vm.invoices.isEmpty {
                Text("No invoices yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(.vertical, 8)
            } else {
                ForEach(vm.invoices) { invoice in
                    Button {
                        selectedInvoice = invoice
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(vm.formattedDate(invoice.invoiceDate))
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                                Text(vm.formattedAmount(invoice.amountCents))
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            Spacer()
                            invoiceStatusBadge(invoice.status)
                            Image(systemName: "chevron.right")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }

                if vm.hasMoreInvoices {
                    Button {
                        Task { await vm.loadMoreInvoices() }
                    } label: {
                        Text("Load More")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                }
            }
        }
        .padding()
        .cardStyle()
    }

    private func invoiceStatusBadge(_ status: InvoiceStatus) -> some View {
        let (text, color): (String, Color) = switch status {
        case .paid: ("Paid", .md3PrimaryContainer)
        case .open: ("Open", .md3TertiaryContainer)
        case .draft: ("Draft", .md3SurfaceContainerHigh)
        case .void: ("Void", .md3ErrorContainer)
        case .uncollectible: ("Uncollectible", .md3ErrorContainer)
        }
        return BadgeView(text: text, color: color)
    }
}
