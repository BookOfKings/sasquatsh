import SwiftUI

struct TierCardView: View {
    let tierName: String
    let price: String
    let priceSubtitle: String
    let features: [String]
    let isPopular: Bool
    let isCurrent: Bool
    let buttonTitle: String
    let isDisabled: Bool
    var onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header
            HStack {
                Text(tierName)
                    .font(.md3TitleLarge)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if isPopular {
                    Text("Most Popular")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnPrimary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.md3Primary)
                        .clipShape(Capsule())
                }
                if isCurrent {
                    Text("Current")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnTertiaryContainer)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.md3TertiaryContainer)
                        .clipShape(Capsule())
                }
            }

            // Price
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(price)
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(Color.md3OnSurface)
                Text(priceSubtitle)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            Divider()

            // Features
            VStack(alignment: .leading, spacing: 8) {
                ForEach(features, id: \.self) { feature in
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundStyle(Color.md3Primary)
                            .font(.md3BodySmall)
                            .padding(.top, 2)
                        Text(feature)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurface)
                    }
                }
            }

            Spacer(minLength: 8)

            // CTA Button
            Button {
                onTap()
            } label: {
                Text(buttonTitle)
                    .font(.md3LabelLarge)
                    .foregroundStyle(isDisabled ? Color.md3OnSurfaceVariant : Color.md3OnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(isDisabled ? Color.md3SurfaceContainerHigh : Color.md3Primary)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            }
            .disabled(isDisabled)
        }
        .padding(20)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))
        .overlay(
            RoundedRectangle(cornerRadius: MD3Shape.large)
                .stroke(isCurrent ? Color.md3Primary : (isPopular ? Color.md3Primary.opacity(0.5) : Color.md3OutlineVariant), lineWidth: isCurrent || isPopular ? 2 : 1)
        )
    }
}
