import SwiftUI

struct UserSearchInviteSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    let groupId: String

    @State private var searchQuery = ""
    @State private var results: [UserSearchResult] = []
    @State private var isSearching = false
    @State private var invitedIds: Set<String> = []
    @State private var error: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    TextField("Search by username...", text: $searchQuery)
                        .onSubmit { search() }
                    if isSearching {
                        D20ProgressView(size: 20)
                    }
                }
                .padding(10)
                .background(Color.md3Surface)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                if let error {
                    Text(error)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3Error)
                        .padding(.horizontal, 16)
                }

                if results.isEmpty && !searchQuery.isEmpty && !isSearching {
                    Spacer()
                    Text("No users found")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Spacer()
                } else {
                    List(results) { user in
                        HStack(spacing: 12) {
                            UserAvatarView(url: user.avatarUrl, name: user.displayName, size: 36)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(user.displayName ?? user.username)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                                Text("@\(user.username)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }

                            Spacer()

                            if invitedIds.contains(user.id) {
                                Label("Invited", systemImage: "checkmark")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3Primary)
                            } else {
                                Button {
                                    Task { await inviteUser(user) }
                                } label: {
                                    Text("Invite")
                                        .font(.md3LabelLarge)
                                        .padding(.horizontal, 14)
                                        .frame(height: 32)
                                        .background(Color.md3Primary)
                                        .foregroundStyle(Color.md3OnPrimary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                        .listRowBackground(Color.md3SurfaceContainer)
                    }
                    .listStyle(.plain)
                }
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Invite User")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onChange(of: searchQuery) { _, newValue in
            guard !newValue.isEmpty else {
                results = []
                return
            }
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                guard searchQuery == newValue else { return }
                search()
            }
        }
    }

    private func search() {
        guard !searchQuery.isEmpty else { return }
        isSearching = true
        Task {
            do {
                results = try await services.profile.searchUsers(query: searchQuery)
            } catch {
                self.error = error.localizedDescription
            }
            isSearching = false
        }
    }

    private func inviteUser(_ user: UserSearchResult) async {
        do {
            let input = CreateInvitationInput(userId: user.id, maxUses: 1, expiresInDays: 7)
            let _ = try await services.groups.createInvitation(groupId: groupId, input: input)
            invitedIds.insert(user.id)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
