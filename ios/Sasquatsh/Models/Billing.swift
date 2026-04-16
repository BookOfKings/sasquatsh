import Foundation

struct SubscriptionInfo: Codable {
    let subscription: SubscriptionDetail
    let paymentMethod: PaymentMethod?
    let hasStripeAccount: Bool
    let hasActiveSubscription: Bool
    let subscriptionSource: String?
    let hasAppleSubscription: Bool?
}

struct SubscriptionDetail: Codable {
    let tier: SubscriptionTier
    let effectiveTier: SubscriptionTier
    let status: SubscriptionStatus?
    let expiresAt: String?
    let hasOverride: Bool?
}

struct PaymentMethod: Codable {
    let brand: String
    let last4: String
    let expMonth: Int
    let expYear: Int
}

struct Invoice: Codable, Identifiable {
    let id: String
    let stripeInvoiceId: String?
    let amountCents: Int
    let taxCents: Int?
    let currency: String
    let status: InvoiceStatus
    let invoiceDate: String
    let periodStart: String?
    let periodEnd: String?
    let hostedInvoiceUrl: String?
    let invoicePdfUrl: String?
    let paymentMethodBrand: String?
    let paymentMethodLast4: String?
}

struct InvoicesResponse: Codable {
    let invoices: [Invoice]
    let total: Int
    let page: Int
    let pageSize: Int
    let hasMore: Bool
}

struct CancelResponse: Codable {
    let message: String
    let cancelAt: String?
    let manageUrl: String?
    let source: String?
}

struct ReactivateResponse: Codable {
    let message: String
}
