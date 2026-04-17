import SwiftUI

/// Drop-in replacement for ProgressView() using the spinning d20
struct D20ProgressView: View {
    var size: CGFloat = 40
    var message: String? = nil

    var body: some View {
        VStack(spacing: 8) {
            D20SpinnerView(size: size)
                .frame(width: size, height: size)
            if let message {
                Text(message)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
    }
}
