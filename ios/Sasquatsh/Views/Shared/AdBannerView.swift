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

    private var shouldShow: Bool {
        let tier = authVM.user?.effectiveTier ?? .free
        return TierConfig.hasFeature(tier, feature: \.showAds)
    }

    var body: some View {
        VStack(spacing: 0) {
            if shouldShow, let ad {
                adContent(ad)
            }
        }
        .onAppear {
            Task { await loadAd() }
        }
    }

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
                // Ad image
                if let imageUrlStr = ad.imageUrl, let imageURL = URL(string: imageUrlStr) {
                    AsyncImage(url: imageURL) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.md3SurfaceContainerHigh
                            .frame(height: 100)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 100)
                    .clipped()
                }

                // Ad content
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

    private func loadAd() async {
        isLoading = true
        do {
            let response: Ad = try await services.api.get("ads", queryItems: [
                .init(name: "placement", value: placement)
            ])
            ad = response
            trackImpression(response)
        } catch {
            // No ad available for this placement
        }
        isLoading = false
    }

    private func trackImpression(_ ad: Ad) {
        Task {
            try? await services.api.postVoid("ads", body: ["adId": ad.id] as [String: String], queryItems: [
                .init(name: "action", value: "impression"),
                .init(name: "adId", value: ad.id),
                .init(name: "pageUrl", value: "ios://\(placement)")
            ])
        }
    }

    private func trackClick(_ ad: Ad) {
        Task {
            try? await services.api.postVoid("ads", body: ["adId": ad.id] as [String: String], queryItems: [
                .init(name: "action", value: "click"),
                .init(name: "adId", value: ad.id),
                .init(name: "pageUrl", value: "ios://\(placement)")
            ])
        }
    }
}
