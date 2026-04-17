import SwiftUI
import CoreImage.CIFilterBuiltins

struct ShareInviteLinkSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    let groupId: String
    let linkType: String
    var planningSessionId: String?
    var eventId: String?
    var title: String = "Share Invite"

    @State private var shareLink: ShareLinkCreated?
    @State private var isCreating = true
    @State private var error: String?
    @State private var copied = false

    private var shareURL: String {
        shareLink?.url ?? "https://sasquatsh.com/join/\(shareLink?.inviteCode ?? "")"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                if isCreating {
                    Spacer()
                    D20ProgressView(size: 40, message: "Creating invite link...")
                    Spacer()
                } else if let error {
                    Spacer()
                    VStack(spacing: 8) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 32))
                            .foregroundStyle(Color.md3Error)
                        Text(error)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .multilineTextAlignment(.center)
                    }
                    Spacer()
                } else {
                    Spacer(minLength: 12)

                    // QR Code
                    if let qrImage = generateQRCode(from: shareURL) {
                        VStack(spacing: 8) {
                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 200, height: 200)
                                .padding(12)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))

                            Text("Scan to join")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }

                    // Link display
                    HStack {
                        Text(shareURL)
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .lineLimit(1)
                            .truncationMode(.middle)

                        Button {
                            UIPasteboard.general.string = shareURL
                            copied = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { copied = false }
                        } label: {
                            Image(systemName: copied ? "checkmark" : "doc.on.doc")
                                .font(.system(size: 14))
                                .foregroundStyle(copied ? Color.md3Primary : Color.md3OnSurfaceVariant)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.md3Surface)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    .padding(.horizontal, 20)

                    if copied {
                        Text("Copied!")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                    }

                    Spacer(minLength: 8)

                    // Action buttons
                    VStack(spacing: 10) {
                        // Share via system sheet (text message, etc.)
                        Button {
                            shareViaSystem()
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "square.and.arrow.up")
                                Text("Send via Text, Email, etc.")
                            }
                            .primaryButtonStyle()
                        }
                        .padding(.horizontal, 20)

                        // Share via Messages directly
                        Button {
                            shareViaMessages()
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "message.fill")
                                Text("Open in Messages")
                            }
                            .secondaryButtonStyle()
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.bottom, 16)
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .task {
            await createLink()
        }
    }

    private func createLink() async {
        isCreating = true
        do {
            var input = CreateShareLinkInput(
                groupId: groupId,
                linkType: linkType
            )
            input.planningSessionId = planningSessionId
            input.eventId = eventId
            shareLink = try await services.shareLinks.create(input: input)
        } catch {
            self.error = error.localizedDescription
        }
        isCreating = false
    }

    private func shareViaSystem() {
        let items: [Any] = ["Join my game on Sasquatsh! \(shareURL)"]
        let ac = UIActivityViewController(activityItems: items, applicationActivities: nil)
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = windowScene.windows.first?.rootViewController {
            root.present(ac, animated: true)
        }
    }

    private func shareViaMessages() {
        let body = "Join my game on Sasquatsh! \(shareURL)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "sms:&body=\(body)") {
            UIApplication.shared.open(url)
        }
    }

    private func generateQRCode(from string: String) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else { return nil }

        // Scale up for clarity
        let scale = 10.0
        let transformed = outputImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))

        guard let cgImage = context.createCGImage(transformed, from: transformed.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
