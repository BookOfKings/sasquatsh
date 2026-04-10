import SwiftUI

struct SessionScheduleView: View {
    let eventId: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = SessionsViewModel()
    @State private var selectedSession: GameSession?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Game Sessions")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if vm.isLoading && vm.tables.isEmpty {
                LoadingView()
            } else if vm.tables.isEmpty {
                Text("No sessions scheduled")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                if let error = vm.error {
                    ErrorBannerView(message: error) { vm.error = nil }
                }

                // Table tabs
                ForEach(vm.tables) { table in
                    tableSection(table)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
        .task {
            vm.configure(services: services, eventId: eventId)
            await vm.loadSessions()
        }
        .sheet(item: $selectedSession) { session in
            sessionDetailSheet(session)
        }
    }

    // MARK: - Table Section

    private func tableSection(_ table: EventTable) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(table.tableName ?? "Table \(table.tableNumber)")
                .font(.md3TitleSmall)
                .foregroundStyle(Color.md3Primary)

            let tableSessions = vm.sessionsForTable(table.id)
            if tableSessions.isEmpty {
                Text("No games at this table")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                ForEach(tableSessions) { session in
                    sessionCard(session)
                }
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Session Card

    private func sessionCard(_ session: GameSession) -> some View {
        Button {
            selectedSession = session
        } label: {
            HStack(spacing: 10) {
                if let urlStr = session.thumbnailUrl, let url = URL(string: urlStr) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.md3SurfaceVariant
                    }
                    .frame(width: 40, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(session.gameName)
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)
                    HStack(spacing: 8) {
                        Text("\(session.durationMinutes) min")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        if let max = session.maxPlayers {
                            Text("\(session.registeredCount)/\(max)")
                                .font(.md3BodySmall)
                                .foregroundStyle(session.isFull ? Color.md3Error : Color.md3OnSurfaceVariant)
                        }
                    }
                }

                Spacer()

                if session.isUserRegistered {
                    BadgeView(text: "Joined", color: .md3PrimaryContainer)
                } else if session.isFull {
                    BadgeView(text: "Full", color: .md3ErrorContainer)
                }

                Image(systemName: "chevron.right")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .padding(10)
            .background(session.isUserRegistered ? Color.md3PrimaryContainer.opacity(0.15) : Color.md3SurfaceContainerHigh)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Session Detail Sheet

    private func sessionDetailSheet(_ session: GameSession) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Game info
                    HStack(spacing: 12) {
                        if let urlStr = session.thumbnailUrl, let url = URL(string: urlStr) {
                            AsyncImage(url: url) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Color.md3SurfaceVariant
                            }
                            .frame(width: 60, height: 60)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text(session.gameName)
                                .font(.md3TitleMedium)
                            HStack(spacing: 12) {
                                Label("\(session.durationMinutes) min", systemImage: "clock")
                                if let min = session.minPlayers, let max = session.maxPlayers {
                                    Label("\(min)-\(max) players", systemImage: "person.2")
                                }
                            }
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                    .padding(.horizontal)

                    // Capacity
                    if let max = session.maxPlayers {
                        HStack {
                            Text("Spots")
                                .font(.md3TitleSmall)
                            Spacer()
                            Text("\(session.registeredCount)/\(max)")
                                .font(.md3TitleSmall)
                                .foregroundStyle(session.isFull ? Color.md3Error : Color.md3Primary)
                        }
                        .padding(.horizontal)
                    }

                    // Action button
                    VStack(spacing: 8) {
                        if session.isUserRegistered {
                            Button {
                                Task {
                                    await vm.cancelRegistration(session.id)
                                    selectedSession = nil
                                }
                            } label: {
                                Text("Leave Session")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3Error)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color.md3ErrorContainer)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            }
                        } else if session.isFull {
                            Text("Session is full")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        } else if vm.hasConflict(session: session) {
                            HStack(spacing: 6) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundStyle(Color.md3Error)
                                Text("You're already in a session at this time slot")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3Error)
                            }
                            .padding(.vertical, 8)
                        } else {
                            Button {
                                Task {
                                    await vm.registerForSession(session.id)
                                    selectedSession = nil
                                }
                            } label: {
                                Text(vm.isRegistering ? "Joining..." : "Join Session")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3OnPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color.md3Primary)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            }
                            .disabled(vm.isRegistering)
                        }
                    }
                    .padding(.horizontal)

                    // Player roster
                    if let regs = session.registrations, !regs.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Players")
                                .font(.md3TitleSmall)

                            ForEach(regs, id: \.userId) { reg in
                                HStack(spacing: 8) {
                                    UserAvatarView(url: reg.avatarUrl, name: reg.displayName, size: 28)
                                    Text(reg.displayName ?? "Player")
                                        .font(.md3BodyMedium)
                                    if reg.isHostReserved == true {
                                        Image(systemName: "star.fill")
                                            .font(.md3LabelSmall)
                                            .foregroundStyle(Color.md3Tertiary)
                                    }
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Session Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { selectedSession = nil }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
