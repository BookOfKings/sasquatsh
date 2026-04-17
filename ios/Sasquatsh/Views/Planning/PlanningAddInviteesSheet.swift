import SwiftUI

struct PlanningAddInviteesSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    let sessionId: String
    let groupId: String
    var onAdded: (() -> Void)?

    @State private var members: [GroupMember] = []
    @State private var selectedIds: Set<String> = []
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    D20ProgressView(size: 32)
                } else if members.isEmpty {
                    Text("No group members available")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                } else {
                    List(members) { member in
                        let selected = selectedIds.contains(member.userId)
                        Button {
                            if selected {
                                selectedIds.remove(member.userId)
                            } else {
                                selectedIds.insert(member.userId)
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                                    .foregroundStyle(selected ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.4))

                                UserAvatarView(url: member.avatarUrl, name: member.displayName, size: 32)

                                Text(member.displayName ?? "Member")
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)

                                Spacer()
                            }
                        }
                        .listRowBackground(Color.md3SurfaceContainer)
                    }
                    .listStyle(.plain)
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Add Invitees")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add (\(selectedIds.count))") {
                        Task { await addInvitees() }
                    }
                    .disabled(selectedIds.isEmpty || isSaving)
                }
            }
            .task {
                await loadMembers()
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func loadMembers() async {
        isLoading = true
        do {
            members = try await services.groups.getMembers(groupId: groupId)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func addInvitees() async {
        isSaving = true
        do {
            try await services.planning.addInvitees(sessionId: sessionId, userIds: Array(selectedIds))
            onAdded?()
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        isSaving = false
    }
}
