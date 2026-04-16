import SwiftUI

struct InvitationAcceptView: View {
    let inviteCode: String
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = InvitationViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                if vm.isLoading && vm.gameInvitation == nil && vm.successMessage == nil {
                    LoadingView(message: "Loading invitation...")
                } else if let msg = vm.successMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 60))
                            .foregroundStyle(.green)
                        Text("Invitation Accepted!")
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
                } else if let invitation = vm.gameInvitation, let event = invitation.event {
                    VStack(spacing: 20) {
                        Image(systemName: "envelope.open.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(Color.md3Primary)

                        Text("You're Invited!")
                            .font(.md3HeadlineMedium)

                        VStack(spacing: 8) {
                            Text(event.title)
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)

                            if let host = event.host?.displayName {
                                Text("Hosted by \(host)")
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }

                            HStack(spacing: 12) {
                                Label(event.eventDate.toDate?.displayDate ?? event.eventDate, systemImage: "calendar")
                                Label(event.startTime.to12HourTime, systemImage: "clock")
                            }
                            .font(.md3BodySmall)

                            if let city = event.city, let state = event.state {
                                Label("\(city), \(state)", systemImage: "mappin")
                                    .font(.md3BodySmall)
                            }
                        }
                        .padding()
                        .cardStyle()

                        Button {
                            Task { await vm.acceptGameInvitation(code: inviteCode) }
                        } label: {
                            Text("Accept Invitation")
                                .primaryButtonStyle()
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Game Invitation")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                vm.configure(services: services)
                await vm.loadGameInvitation(code: inviteCode)
            }
        }
    }
}
