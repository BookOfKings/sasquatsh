import SwiftUI

struct Ad: Decodable {
    let id: String
    let title: String
    let adDescription: String?
    let imageUrl: String?
    let linkUrl: String?
    let adType: String?

    enum CodingKeys: String, CodingKey {
        case id, title
        case adDescription = "description"
        case imageUrl, linkUrl, adType
    }
}

struct AdBannerView: View {
    let placement: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @Environment(\.openURL) private var openURL
    @State private var ad: Ad?
    @State private var isLoading = false
    @State private var refreshTimer: Timer?

    private var shouldShow: Bool {
        let tier = authVM.user?.effectiveTier ?? .free
        return TierConfig.hasFeature(tier, feature: \.showAds)
    }

    var body: some View {
        VStack(spacing: 0) {
            if shouldShow {
                if let ad {
                    adContent(ad)
                } else if !isLoading {
                    // Offline / no ad fallback
                    offlineFallback
                }
            }
        }
        .onAppear {
            Task { await loadAd() }
            startRefreshTimer()
        }
        .onDisappear {
            stopRefreshTimer()
        }
    }

    // MARK: - Ad Content

    private func adContent(_ ad: Ad) -> some View {
        Button {
            trackClick(ad)
            if let urlStr = ad.linkUrl {
                let fullUrl = urlStr.hasPrefix("http") ? urlStr : "https://\(AppConfig.webDomain)\(urlStr)"
                if let url = URL(string: fullUrl) {
                    openURL(url)
                }
            }
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                if let imageUrlStr = ad.imageUrl, let imageURL = URL(string: imageUrlStr),
                   !imageUrlStr.hasSuffix(".svg") {
                    AsyncImage(url: imageURL) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                                .frame(maxWidth: .infinity)
                                .frame(height: 140)
                                .clipped()
                        default:
                            EmptyView()
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text("Sponsored")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.6))
                            .textCase(.uppercase)
                        Spacer()
                    }

                    Text(ad.title)
                        .font(.md3BodyMedium)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)

                    if let desc = ad.adDescription, !desc.isEmpty {
                        Text(desc)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .lineLimit(2)
                    }
                }
                .padding(10)
            }
            .background(Color.md3SurfaceContainerHigh)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.medium)
                    .stroke(Color.md3OutlineVariant.opacity(0.5), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }

    // MARK: - Offline Fallback

    private var offlineFallback: some View {
        NavigationLink {
            PricingView()
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "sparkles")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.md3Tertiary)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Upgrade Your Experience")
                        .font(.md3BodyMedium)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.md3OnSurface)
                    Text("Get chat, planning, more groups, and no ads")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .padding(10)
            .background(Color.md3SurfaceContainerHigh)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.medium)
                    .stroke(Color.md3OutlineVariant.opacity(0.5), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }

    // MARK: - Loading & Tracking

    private func loadAd() async {
        isLoading = true
        do {
            let response: Ad = try await services.api.get("ads", queryItems: [
                .init(name: "placement", value: placement)
            ])
            ad = response
            trackImpression(response)
        } catch {
            // No connection or no ad — fallback will show
        }
        isLoading = false
    }

    private func trackImpression(_ ad: Ad) {
        Task {
            try? await services.api.postVoid("ads", queryItems: [
                .init(name: "action", value: "impression"),
                .init(name: "id", value: ad.id),
                .init(name: "page", value: "ios://\(placement)")
            ])
        }
    }

    private func trackClick(_ ad: Ad) {
        Task {
            try? await services.api.postVoid("ads", queryItems: [
                .init(name: "action", value: "click"),
                .init(name: "id", value: ad.id),
                .init(name: "page", value: "ios://\(placement)")
            ])
        }
    }

    // MARK: - Refresh Timer

    private func startRefreshTimer() {
        stopRefreshTimer()
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { _ in
            Task { @MainActor in
                await loadAd()
            }
        }
    }

    private func stopRefreshTimer() {
        refreshTimer?.invalidate()
        refreshTimer = nil
    }
}
