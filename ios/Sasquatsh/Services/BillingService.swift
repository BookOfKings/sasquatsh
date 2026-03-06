import Foundation

protocol BillingServiceProtocol: Sendable {
    func getSubscriptionInfo() async throws -> SubscriptionInfo
    func getInvoices(page: Int) async throws -> InvoicesResponse
    func getInvoice(id: String) async throws -> Invoice
    func cancelSubscription() async throws -> CancelResponse
    func reactivateSubscription() async throws -> ReactivateResponse
}

final class BillingService: BillingServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getSubscriptionInfo() async throws -> SubscriptionInfo {
        try await api.get("billing", authenticated: true)
    }

    func getInvoices(page: Int) async throws -> InvoicesResponse {
        try await api.get("billing", queryItems: [
            .init(name: "include", value: "invoices"),
            .init(name: "page", value: String(page))
        ], authenticated: true)
    }

    func getInvoice(id: String) async throws -> Invoice {
        try await api.get("billing", queryItems: [
            .init(name: "invoiceId", value: id)
        ], authenticated: true)
    }

    func cancelSubscription() async throws -> CancelResponse {
        try await api.post("billing", queryItems: [
            .init(name: "action", value: "cancel")
        ])
    }

    func reactivateSubscription() async throws -> ReactivateResponse {
        try await api.post("billing", queryItems: [
            .init(name: "action", value: "reactivate")
        ])
    }
}
