import SwiftUI

struct UpgradeAdBanner: View {
    var body: some View {
        NavigationLink {
            PricingView()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "sparkles")
                    .foregroundStyle(Color.md3Tertiary)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Unlock more features")
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3OnSurface)
                    Text("Upgrade to Basic for $4.99/mo")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                Spacer()

                Text("Upgrade")
                    .font(.md3LabelMedium)
                    .foregroundStyle(Color.md3OnPrimary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.md3Primary)
                    .clipShape(Capsule())
            }
            .padding()
            .background(Color.md3SurfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.medium)
                    .stroke(Color.md3OutlineVariant, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
