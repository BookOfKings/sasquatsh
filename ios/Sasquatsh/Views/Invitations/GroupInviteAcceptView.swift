import SwiftUI

struct GroupInviteAcceptView: View {
    let inviteCode: String
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = InvitationViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                if vm.isLoading && vm.groupInvitePreview == nil && vm.successMessage == nil {
                    LoadingView(message: "Loading invitation...")
                } else if let msg = vm.successMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 60))
                            .foregroundStyle(.green)
                        Text("Joined Group!")
                            .font(.md3HeadlineMedium)
                        Text(msg)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .multilineTextAlignment(.center)
                        Button {
                            dismiss()
                        } label: {
                            Text("Done")
                                .primaryButtonStyle()
                        }
                        .padding(.horizontal, 40)
                    }
                } else if let error = vm.error {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 60))
                            .foregroundStyle(.orange)
                        Text("Could not load invitation")
                            .font(.md3HeadlineSmall)
                        Text(error)
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .multilineTextAlignment(.center)
                    }
                } else if let preview = vm.groupInvitePreview {
                    VStack(spacing: 20) {
                        Image(systemName: "person.3.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(Color.md3Primary)

                        Text("Group Invitation")
                            .font(.md3HeadlineMedium)

                        VStack(spacing: 8) {
                            Text(preview.group.name)
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)

                            if let description = preview.group.description {
                                Text(description)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                    .multilineTextAlignment(.center)
                            }

                            HStack(spacing: 8) {
                                BadgeView(text: preview.group.groupType.displayName, color: .md3TertiaryContainer)
                                BadgeView(text: preview.group.joinPolicy.displayName, color: .md3PrimaryContainer)
                            }

                            if let city = preview.group.locationCity, let state = preview.group.locationState {
                                Label("\(city), \(state)", systemImage: "mappin")
                                    .font(.md3BodySmall)
                            }

                            if let invitedBy = preview.invitedBy.displayName {
                                Text("Invited by \(invitedBy)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        .padding()
                        .cardStyle()

                        Button {
                            Task { await vm.acceptGroupInvite(code: inviteCode) }
                        } label: {
                            Text("Join Group")
                                .primaryButtonStyle()
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Group Invitation")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                vm.configure(services: services)
                await vm.loadGroupInvitePreview(code: inviteCode)
            }
        }
    }
}
