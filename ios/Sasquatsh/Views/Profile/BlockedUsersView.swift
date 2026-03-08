import SwiftUI

struct BlockedUsersView: View {
    @Bindable var vm: ProfileViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if let profile = vm.profile, !profile.blockedUserIds.isEmpty {
                    List {
                        ForEach(profile.blockedUserIds, id: \.self) { userId in
                            HStack {
                                Text(userId)
                                    .font(.md3BodyMedium)
                                Spacer()
                                Button("Unblock") {
                                    Task { await vm.unblockUser(userId: userId) }
                                }
                                .foregroundStyle(Color.md3Error)
                                .font(.md3LabelLarge)
                            }
                        }
                    }
                } else {
                    EmptyStateView(
                        icon: "person.crop.circle.badge.checkmark",
                        title: "No Blocked Users",
                        message: "You haven't blocked anyone"
                    )
                }
            }
            .navigationTitle("Blocked Users")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
