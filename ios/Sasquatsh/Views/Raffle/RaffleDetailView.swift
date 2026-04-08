import SwiftUI

struct RaffleDetailView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = RaffleViewModel()
    @State private var showTerms = false
    @State private var showMailInForm = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.raffle == nil {
                LoadingView()
            } else if let raffle = vm.raffle {
                VStack(alignment: .leading, spacing: 20) {
                    prizeSection(raffle)
                    statsSection(raffle)
                    entriesSection(raffle)
                    howToEarnSection
                    if raffle.mailInInstructions != nil && raffle.isActive {
                        mailInSection(raffle)
                    }
                    if raffle.isEnded && raffle.hasWinner {
                        winnerSection(raffle)
                    }
                    if raffle.termsConditions != nil {
                        termsSection(raffle)
                    }
                }
                .padding(.vertical)
            } else {
                EmptyStateView(
                    icon: "trophy",
                    title: "No Active Raffle",
                    message: "Check back soon for the next raffle!"
                )
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Monthly Raffle")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await vm.loadActiveRaffle() }
        .task {
            vm.configure(services: services)
            await vm.loadActiveRaffle()
        }
        .alert("Entry Submitted", isPresented: $vm.mailInSuccess) {
            Button("OK") {}
        } message: {
            Text("Your mail-in entry has been submitted.")
        }
    }

    // MARK: - Prize

    private func prizeSection(_ raffle: Raffle) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if let urlStr = raffle.prizeImageUrl, let url = URL(string: urlStr) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(height: 200)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            }

            Text(raffle.prizeName)
                .font(.md3HeadlineMedium)
                .foregroundStyle(Color.md3OnSurface)

            if let desc = raffle.prizeDescription, !desc.isEmpty {
                Text(desc)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            HStack(spacing: 16) {
                if let value = raffle.prizeValueFormatted {
                    Label(value, systemImage: "tag.fill")
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3Tertiary)
                }
                if let time = raffle.timeRemaining {
                    Label(time, systemImage: "clock")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Stats

    private func statsSection(_ raffle: Raffle) -> some View {
        HStack(spacing: 0) {
            statItem(
                value: "\(raffle.stats?.displayTotalEntries ?? 0)",
                label: "Total Entries",
                icon: "ticket.fill"
            )
            Divider().frame(height: 40)
            statItem(
                value: "\(raffle.stats?.displayParticipants ?? 0)",
                label: "Participants",
                icon: "person.2.fill"
            )
            Divider().frame(height: 40)
            statItem(
                value: "\(raffle.userTotalEntries ?? 0)",
                label: "Your Entries",
                icon: "star.fill"
            )
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func statItem(value: String, label: String, icon: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .foregroundStyle(Color.md3Primary)
            Text(value)
                .font(.md3TitleLarge)
                .foregroundStyle(Color.md3OnSurface)
            Text(label)
                .font(.md3LabelSmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - User Entries

    private func entriesSection(_ raffle: Raffle) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Your Entries")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if vm.entriesByType.isEmpty {
                Text("No entries yet. Host or attend events to earn entries!")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                RaffleEntryBreakdownView(
                    entries: vm.entriesByType,
                    totalEntries: raffle.userTotalEntries ?? 0
                )
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - How to Earn

    private var howToEarnSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("How to Earn Entries")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            ForEach(RaffleEntryType.allCases) { type in
                HStack(spacing: 12) {
                    Image(systemName: type.iconName)
                        .foregroundStyle(Color.md3Primary)
                        .frame(width: 24)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(type.displayName)
                            .font(.md3BodyMedium)
                        Text(type.description)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Mail-In

    private func mailInSection(_ raffle: Raffle) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Mail-In Entry")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                Button {
                    showMailInForm.toggle()
                } label: {
                    Image(systemName: showMailInForm ? "chevron.up" : "chevron.down")
                        .foregroundStyle(Color.md3Primary)
                }
            }

            if let instructions = raffle.mailInInstructions {
                Text(instructions)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            if showMailInForm {
                TextField("Full Name", text: $vm.mailInName)
                    .textFieldStyle(.roundedBorder)
                TextField("Mailing Address", text: $vm.mailInAddress, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(2...4)

                if let error = vm.error {
                    Text(error)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3Error)
                }

                Button {
                    Task { await vm.submitMailInEntry() }
                } label: {
                    Text(vm.isSubmittingMailIn ? "Submitting..." : "Submit Entry")
                        .primaryButtonStyle()
                }
                .disabled(vm.isSubmittingMailIn || vm.mailInName.isEmpty || vm.mailInAddress.isEmpty)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Winner

    private func winnerSection(_ raffle: Raffle) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "trophy.fill")
                .font(.system(size: 40))
                .foregroundStyle(.yellow)

            Text("Winner!")
                .font(.md3HeadlineSmall)
                .foregroundStyle(Color.md3OnSurface)

            if let winner = raffle.winner {
                HStack(spacing: 8) {
                    UserAvatarView(url: winner.avatarUrl, name: winner.displayName, size: 36)
                    Text(winner.displayName ?? "Unknown")
                        .font(.md3TitleMedium)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Terms

    private func termsSection(_ raffle: Raffle) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation { showTerms.toggle() }
            } label: {
                HStack {
                    Text("Terms & Conditions")
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3OnSurface)
                    Spacer()
                    Image(systemName: showTerms ? "chevron.up" : "chevron.down")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }

            if showTerms, let terms = raffle.termsConditions {
                Text(terms)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }
}
