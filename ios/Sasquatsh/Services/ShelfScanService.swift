import Foundation

protocol ShelfScanServiceProtocol: Sendable {
    func getRemainingScans() async throws -> ShelfScanQuota
    func scanImage(imageData: Data) async throws -> ShelfScanResult
}

struct ShelfScanQuota: Decodable {
    let used: Int
    let limit: QuotaLimit
    let remaining: Int

    // Backend sends Int for limited tiers, "unlimited" string for pro/premium
    enum QuotaLimit: Decodable {
        case count(Int)
        case unlimited

        init(from decoder: Decoder) throws {
            let container = try decoder.singleValueContainer()
            if let n = try? container.decode(Int.self) {
                self = .count(n)
            } else if let s = try? container.decode(String.self), s == "unlimited" {
                self = .unlimited
            } else {
                self = .count(0)
            }
        }

        var displayText: String {
            switch self {
            case .count(let n): return "\(n)"
            case .unlimited: return "unlimited"
            }
        }

        var isUnlimited: Bool {
            if case .unlimited = self { return true }
            return false
        }
    }
}

struct ShelfScanResult: Decodable {
    let games: [ShelfScanGame]
    let rawText: String?
    let totalDetected: Int?
    let matched: Int?
}

struct ShelfScanGame: Decodable, Identifiable {
    var id: String { detectedTitle + String(bggId ?? 0) }
    let detectedTitle: String
    let bggId: Int?
    let name: String?
    let yearPublished: Int?
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let playingTime: Int?
    let confidence: String? // "high", "medium", or "none"
    let imageUrl: String?
}

struct ShelfScanService: ShelfScanServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getRemainingScans() async throws -> ShelfScanQuota {
        try await api.get("shelf-scan", authenticated: true)
    }

    func scanImage(imageData: Data) async throws -> ShelfScanResult {
        // Send as base64 JSON — the edge function accepts { "image": "base64..." }
        let base64 = imageData.base64EncodedString()
        struct ScanBody: Encodable {
            let image: String
        }
        return try await api.post("shelf-scan", body: ScanBody(image: base64))
    }
}
