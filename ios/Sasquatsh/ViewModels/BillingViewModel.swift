import SwiftUI

@Observable
@MainActor
final class BillingViewModel {
    var subscriptionInfo: SubscriptionInfo?
    var invoices: [Invoice] = []
    var hasMoreInvoices = false
    var isLoading = false
    var actionLoading = false
    var error: String?
    var successMessage: String?

    private var services: ServiceContainer?
    private var currentPage = 1

    func configure(services: ServiceContainer) {
        self.services = services
    }

    // MARK: - Derived Properties

    var currentTier: SubscriptionTier {
        subscriptionInfo?.subscription.effectiveTier ?? .free
    }

    var isCancelled: Bool {
        subscriptionInfo?.subscription.status == .canceled
    }

    var isPastDue: Bool {
        subscriptionInfo?.subscription.status == .pastDue
    }

    var hasActiveSubscription: Bool {
        subscriptionInfo?.hasActiveSubscription ?? false
    }

    var hasOverride: Bool {
        subscriptionInfo?.subscription.hasOverride ?? false
    }

    // MARK: - Data Loading

    func loadBillingInfo() async {
        guard let services else { return }
        isLoading = true
        error = nil

        async let subTask = services.billing.getSubscriptionInfo()
        async let invTask = services.billing.getInvoices(page: 1)

        do {
            let (sub, inv) = try await (subTask, invTask)
            subscriptionInfo = sub
            invoices = inv.invoices
            hasMoreInvoices = inv.hasMore
            currentPage = 1
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    func loadMoreInvoices() async {
        guard let services, hasMoreInvoices else { return }
        let nextPage = currentPage + 1
        do {
            let response = try await services.billing.getInvoices(page: nextPage)
            invoices.append(contentsOf: response.invoices)
            hasMoreInvoices = response.hasMore
            currentPage = nextPage
        } catch {
            self.error = error.localizedDescription
        }
    }

    // MARK: - Actions

    func cancelSubscription() async {
        guard let services else { return }
        actionLoading = true
        error = nil
        do {
            let result = try await services.billing.cancelSubscription()
            successMessage = result.message
            await loadBillingInfo()
        } catch {
            self.error = error.localizedDescription
        }
        actionLoading = false
    }

    func reactivateSubscription() async {
        guard let services else { return }
        actionLoading = true
        error = nil
        do {
            let result = try await services.billing.reactivateSubscription()
            successMessage = result.message
            await loadBillingInfo()
        } catch {
            self.error = error.localizedDescription
        }
        actionLoading = false
    }

    // MARK: - Formatting Helpers

    func formattedAmount(_ cents: Int) -> String {
        let dollars = Double(cents) / 100.0
        return String(format: "$%.2f", dollars)
    }

    func formattedDate(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let date = isoFormatter.date(from: dateString) else {
            // Try without fractional seconds
            isoFormatter.formatOptions = [.withInternetDateTime]
            guard let date = isoFormatter.date(from: dateString) else { return dateString }
            return formatDate(date)
        }
        return formatDate(date)
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }

    func formatCardBrand(_ brand: String) -> String {
        switch brand.lowercased() {
        case "visa": return "Visa"
        case "mastercard": return "Mastercard"
        case "amex", "american_express": return "Amex"
        case "discover": return "Discover"
        default: return brand.capitalized
        }
    }
}
