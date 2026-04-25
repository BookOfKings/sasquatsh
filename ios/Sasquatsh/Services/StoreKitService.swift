import StoreKit
import Foundation
import os

enum StoreKitError: LocalizedError {
    case productNotFound
    case purchaseFailed(String)
    case verificationFailed
    case userCancelled
    case serverSyncFailed(String)

    var errorDescription: String? {
        switch self {
        case .productNotFound: return "Product not found"
        case .purchaseFailed(let msg): return "Purchase failed: \(msg)"
        case .verificationFailed: return "Transaction verification failed"
        case .userCancelled: return nil
        case .serverSyncFailed(let msg): return "Sync failed: \(msg)"
        }
    }
}

struct IAPProduct: Identifiable {
    let product: Product
    let tier: SubscriptionTier
    let period: SubscriptionPeriod

    var id: String { product.id }
}

enum SubscriptionPeriod: String, Codable {
    case monthly, annual
}

@Observable
final class StoreKitService {
    var products: [IAPProduct] = []
    var purchasedProductIDs: Set<String> = []
    var isLoading = false
    var purchaseInProgress = false

    private var transactionListener: Task<Void, Never>?
    private let logger = Logger(subsystem: "com.sasquatsh", category: "StoreKit")

    private let productIDs: Set<String> = [
        "com.sasquatsh.basic.monthly",
        "com.sasquatsh.basic.annual",
        "com.sasquatsh.pro.monthly",
        "com.sasquatsh.pro.annual"
    ]

    private static let productTierMap: [String: SubscriptionTier] = [
        "com.sasquatsh.basic.monthly": .basic,
        "com.sasquatsh.basic.annual": .basic,
        "com.sasquatsh.pro.monthly": .pro,
        "com.sasquatsh.pro.annual": .pro,
    ]

    private var api: APIClient?

    func configure(api: APIClient) {
        self.api = api
        startTransactionListener()
    }

    // MARK: - Product Loading

    func loadProducts() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let storeProducts = try await Product.products(for: productIDs)
            products = storeProducts.compactMap { product in
                guard let tier = Self.productTierMap[product.id] else { return nil }
                let period: SubscriptionPeriod = product.id.contains("annual") ? .annual : .monthly
                return IAPProduct(product: product, tier: tier, period: period)
            }.sorted { $0.product.price < $1.product.price }
            logger.info("Loaded \(self.products.count) IAP products")
        } catch {
            logger.error("Failed to load products: \(error.localizedDescription)")
        }
    }

    // MARK: - Purchase

    func purchase(_ iapProduct: IAPProduct) async throws {
        purchaseInProgress = true
        defer { purchaseInProgress = false }

        let result = try await iapProduct.product.purchase()

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await syncTransactionWithServer(transaction)
            await transaction.finish()
            await updatePurchasedProducts()
            logger.info("Purchase complete: \(iapProduct.product.id)")

        case .userCancelled:
            throw StoreKitError.userCancelled

        case .pending:
            logger.info("Purchase pending approval")

        @unknown default:
            throw StoreKitError.purchaseFailed("Unknown result")
        }
    }

    // MARK: - Restore

    func restorePurchases() async throws {
        try await AppStore.sync()
        await updatePurchasedProducts()
        // Sync any active subscription with server
        if let transaction = await currentSubscription() {
            await syncTransactionWithServer(transaction)
        }
        logger.info("Purchases restored")
    }

    // MARK: - Transaction Listener

    private func startTransactionListener() {
        transactionListener = Task.detached { [weak self] in
            for await result in Transaction.updates {
                guard let self else { break }
                if let transaction = try? await self.checkVerified(result) {
                    await self.syncTransactionWithServer(transaction)
                    await transaction.finish()
                    await self.updatePurchasedProducts()
                }
            }
        }
    }

    // MARK: - Entitlement Check

    func updatePurchasedProducts() async {
        var purchased = Set<String>()
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                purchased.insert(transaction.productID)
            }
        }
        purchasedProductIDs = purchased
    }

    // MARK: - Helpers

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let value):
            return value
        case .unverified:
            throw StoreKitError.verificationFailed
        }
    }

    private func syncTransactionWithServer(_ transaction: Transaction) async {
        guard let api else {
            logger.error("syncTransaction: API not configured")
            return
        }

        struct SyncRequest: Encodable {
            let transactionId: UInt64
            let originalTransactionId: UInt64
            let productId: String
            let environment: String
        }

        struct SyncResponse: Decodable {
            let success: Bool
            let tier: String?
        }

        let env: String
        if #available(iOS 16.0, *) {
            env = transaction.environment == .sandbox ? "Sandbox" : "Production"
        } else {
            env = "Production"
        }

        logger.info("syncTransaction: id=\(transaction.id) product=\(transaction.productID) env=\(env)")

        let request = SyncRequest(
            transactionId: transaction.id,
            originalTransactionId: transaction.originalID,
            productId: transaction.productID,
            environment: env
        )

        do {
            let response: SyncResponse = try await api.post(
                "apple-iap-webhook",
                body: request,
                queryItems: [.init(name: "action", value: "verify")]
            )
            logger.info("syncTransaction: success=\(response.success) tier=\(response.tier ?? "nil")")
        } catch {
            logger.error("syncTransaction failed: \(error.localizedDescription)")
            // Don't throw — the Apple purchase succeeded, tier will sync on next app launch
            // via the transaction listener or restore purchases
        }
    }

    func product(for tier: SubscriptionTier, period: SubscriptionPeriod) -> IAPProduct? {
        products.first { $0.tier == tier && $0.period == period }
    }

    func currentSubscription() async -> Transaction? {
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result),
               transaction.productType == .autoRenewable {
                return transaction
            }
        }
        return nil
    }

    var hasActiveSubscription: Bool {
        !purchasedProductIDs.isEmpty
    }

    var currentTier: SubscriptionTier? {
        for id in purchasedProductIDs {
            if let tier = Self.productTierMap[id] {
                return tier
            }
        }
        return nil
    }
}
