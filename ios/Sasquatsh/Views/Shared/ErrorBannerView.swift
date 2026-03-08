import SwiftUI

struct ErrorBannerView: View {
    let message: String
    var onDismiss: (() -> Void)?

    var body: some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color.md3Error)

            Text(message)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3Error)

            Spacer()

            if let onDismiss {
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundStyle(Color.md3Error)
                }
            }
        }
        .padding()
        .background(Color.md3ErrorContainer)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .padding(.horizontal)
    }
}
