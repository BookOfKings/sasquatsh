import Foundation

struct TierFeatures {
    let tableInfo: Bool
    let planning: Bool
    let items: Bool
    let showAds: Bool
}

struct TierLimits {
    let gamesPerEvent: Int?  // nil = unlimited
    let maxGroups: Int?      // nil = unlimited
    let features: TierFeatures
}

enum TierConfig {
    static func getLimits(for tier: SubscriptionTier) -> TierLimits {
        switch tier {
        case .free:
            return TierLimits(
                gamesPerEvent: 1,
                maxGroups: 1,
                features: TierFeatures(tableInfo: false, planning: false, items: false, showAds: true)
            )
        case .basic:
            return TierLimits(
                gamesPerEvent: 5,
                maxGroups: 5,
                features: TierFeatures(tableInfo: true, planning: true, items: false, showAds: false)
            )
        case .pro:
            return TierLimits(
                gamesPerEvent: 10,
                maxGroups: 10,
                features: TierFeatures(tableInfo: true, planning: true, items: true, showAds: false)
            )
        case .premium:
            return TierLimits(
                gamesPerEvent: nil,
                maxGroups: nil,
                features: TierFeatures(tableInfo: true, planning: true, items: true, showAds: false)
            )
        }
    }

    static func hasFeature(_ tier: SubscriptionTier, feature: KeyPath<TierFeatures, Bool>) -> Bool {
        getLimits(for: tier).features[keyPath: feature]
    }

    static func canCreateGroup(_ tier: SubscriptionTier, currentCount: Int) -> Bool {
        guard let max = getLimits(for: tier).maxGroups else { return true }
        return currentCount < max
    }

    static func canAddGame(_ tier: SubscriptionTier, currentCount: Int) -> Bool {
        guard let max = getLimits(for: tier).gamesPerEvent else { return true }
        return currentCount < max
    }

    static func recommendedUpgrade(from tier: SubscriptionTier) -> SubscriptionTier? {
        switch tier {
        case .free: return .basic
        case .basic: return .pro
        case .pro: return .premium
        case .premium: return nil
        }
    }

    static func tierFeatureDescriptions(for tier: SubscriptionTier) -> [String] {
        let limits = getLimits(for: tier)
        var features: [String] = []

        if let max = limits.maxGroups {
            features.append("Up to \(max) group\(max == 1 ? "" : "s")")
        } else {
            features.append("Unlimited groups")
        }

        if let max = limits.gamesPerEvent {
            features.append("Up to \(max) game\(max == 1 ? "" : "s") per event")
        } else {
            features.append("Unlimited games per event")
        }

        if limits.features.tableInfo {
            features.append("Table/room/hall info")
        }

        if limits.features.planning {
            features.append("Planning sessions")
        }

        if limits.features.items {
            features.append("Event items tracking")
        }

        if !limits.features.showAds {
            features.append("No ads")
        }

        return features
    }
}
