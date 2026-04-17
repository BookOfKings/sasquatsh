import SwiftUI

struct ShareLinkAcceptView: View {
    let code: String
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    @State private var preview: ShareLinkPreview?
    @State private var isLoading = true
    @State private var isAccepting = false
    @State private var error: String?
    @State private var accepted = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                if isLoading {
                    Spacer()
                    D20ProgressView(size: 40, message: "Loading invite...")
                    Spacer()
                } else if let error {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 36))
                            .foregroundStyle(Color.md3Error)
                        Text(error)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .multilineTextAlignment(.center)
                    }
                    Spacer()
                } else if accepted {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(Color.md3Primary)
                        Text("You're in!")
                            .font(.md3HeadlineSmall)
                            .foregroundStyle(Color.md3OnSurface)
                        if let target = preview?.target {
                            Text("Joined: \(target.title)")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                    Spacer()
                } else if let preview {
                    Spacer()

                    // Group info
                    if let group = preview.group {
                        VStack(spacing: 8) {
                            Image(systemName: "person.3.fill")
                                .font(.system(size: 36))
                                .foregroundStyle(Color.md3Primary)
                            Text(group.name)
                                .font(.md3HeadlineSmall)
                                .foregroundStyle(Color.md3OnSurface)
                        }
                    }

                    // Target info
                    if let target = preview.target {
                        VStack(spacing: 4) {
                            Text(target.type == "planning_session" ? "Planning Session" : "Game Event")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text(target.title)
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity)
                        .background(Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        .padding(.horizontal, 24)
                    }

                    // Invited by
                    if let inviter = preview.invitedBy, let name = inviter.displayName {
                        HStack(spacing: 8) {
                            UserAvatarView(url: inviter.avatarUrl, name: name, size: 28)
                            Text("Invited by \(name)")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }

                    Spacer()

                    // Accept button
                    Button {
                        Task { await acceptInvite() }
                    } label: {
                        HStack(spacing: 8) {
                            if isAccepting {
                                ProgressView().tint(.white)
                            }
                            Text("Join")
                        }
                        .primaryButtonStyle()
                    }
                    .disabled(isAccepting)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 16)
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Invite")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(accepted ? "Done" : "Cancel") { dismiss() }
                }
            }
        }
        .task {
            await loadPreview()
        }
    }

    private func loadPreview() async {
        isLoading = true
        do {
            preview = try await services.shareLinks.preview(code: code)
        } catch {
            self.error = "This invite link is invalid or has expired."
        }
        isLoading = false
    }

    private func acceptInvite() async {
        isAccepting = true
        do {
            let _ = try await services.shareLinks.accept(code: code)
            withAnimation {
                accepted = true
            }
            try? await Task.sleep(for: .seconds(2))
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        isAccepting = false
    }
}
