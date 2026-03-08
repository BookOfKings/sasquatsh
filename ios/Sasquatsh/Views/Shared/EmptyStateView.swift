import SwiftUI

struct EmptyStateView: View {
    let icon: String
    let title: String
    var message: String?
    var buttonTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 16) {
            Circle()
                .fill(Color.md3PrimaryContainer)
                .frame(width: 80, height: 80)
                .overlay {
                    Image(systemName: icon)
                        .font(.system(size: 32))
                        .foregroundStyle(Color.md3OnPrimaryContainer)
                }

            Text(title)
                .font(.md3HeadlineSmall)
                .foregroundStyle(Color.md3OnSurface)

            if let message {
                Text(message)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .multilineTextAlignment(.center)
            }

            if let buttonTitle, let action {
                Button(action: action) {
                    Text(buttonTitle)
                        .primaryButtonStyle()
                }
                .padding(.horizontal, 40)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
