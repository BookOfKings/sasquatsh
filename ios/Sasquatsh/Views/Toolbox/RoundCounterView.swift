import SwiftUI

struct RoundCounterView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var vm = RoundCounterViewModel()
    @State private var showSettings = false

    private let durationOptions = [30, 60, 90, 120, 180, 300, 600]

    var body: some View {
        GeometryReader { geo in
            let compact = geo.size.height < 700

            VStack(spacing: 0) {
                // Compact nav + timer
                CompactNavBar(title: "Round Counter") { dismiss() }
                timerBar(compact: compact)

                Spacer(minLength: 0)

                // Center: round number + controls — always visible
                VStack(spacing: compact ? 12 : 20) {
                    // Round label
                    if vm.isCountdownMode {
                        Text("ROUND \(vm.state.roundNumber) OF \(vm.maxRounds)")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .tracking(3)
                    } else {
                        Text("ROUND")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .tracking(3)
                    }

                    // Big round number
                    Button {
                        vm.beginEditRound()
                    } label: {
                        ZStack {
                            // Glow ring
                            Circle()
                                .stroke(
                                    vm.reachedMaxRound ? Color.md3Error.opacity(0.3) :
                                    vm.isTimerRunning ? Color.md3Primary.opacity(0.15) :
                                    Color.md3OnSurfaceVariant.opacity(0.08),
                                    lineWidth: 6
                                )
                                .frame(width: compact ? 160 : 200, height: compact ? 160 : 200)

                            // Progress ring (countdown mode)
                            if vm.isCountdownMode && vm.roundDurationSeconds > 0 {
                                Circle()
                                    .trim(from: 0, to: max(0, vm.countdownRemaining / Double(vm.roundDurationSeconds)))
                                    .stroke(
                                        vm.countdownRemaining <= 10 && vm.isTimerRunning ? Color.md3Error : Color.md3Primary,
                                        style: StrokeStyle(lineWidth: 6, lineCap: .round)
                                    )
                                    .frame(width: compact ? 160 : 200, height: compact ? 160 : 200)
                                    .rotationEffect(.degrees(-90))
                                    .animation(.linear(duration: 0.1), value: vm.countdownRemaining)
                            }

                            VStack(spacing: 2) {
                                Text("\(vm.state.roundNumber)")
                                    .font(.system(size: compact ? 64 : 80, weight: .bold, design: .rounded))
                                    .foregroundStyle(vm.reachedMaxRound ? Color.md3Error : Color.md3OnSurface)
                                    .contentTransition(.numericText())

                                if vm.isCountdownMode {
                                    Text(vm.formattedCountdown)
                                        .font(.system(size: 16, weight: .medium, design: .monospaced))
                                        .foregroundStyle(vm.countdownRemaining <= 10 && vm.isTimerRunning ? Color.md3Error : Color.md3Primary)
                                }
                            }
                        }
                    }
                    .accessibilityLabel("Round \(vm.state.roundNumber)")

                    if vm.reachedMaxRound {
                        Text("Final round!")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Error)
                            .transition(.scale.combined(with: .opacity))
                    }

                    // +/- controls
                    HStack(spacing: 40) {
                        Button {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                                vm.decrement()
                            }
                        } label: {
                            Image(systemName: "minus")
                                .font(.system(size: 26, weight: .semibold))
                                .frame(width: 64, height: 64)
                                .foregroundStyle(vm.state.roundNumber <= vm.state.minimumRound ? Color.md3OnSurfaceVariant.opacity(0.3) : Color.md3OnSecondaryContainer)
                                .background(vm.state.roundNumber <= vm.state.minimumRound ? Color.md3SurfaceContainerHigh : Color.md3SecondaryContainer)
                                .clipShape(Circle())
                        }
                        .disabled(vm.state.roundNumber <= vm.state.minimumRound)

                        // Play/pause in the middle
                        Button {
                            vm.toggleTimer()
                        } label: {
                            Image(systemName: vm.isTimerRunning ? "pause.fill" : "play.fill")
                                .font(.system(size: 22))
                                .frame(width: 56, height: 56)
                                .foregroundStyle(Color.md3OnPrimary)
                                .background(Color.md3Primary)
                                .clipShape(Circle())
                                .shadow(color: Color.md3Primary.opacity(0.3), radius: 8, y: 4)
                        }

                        Button {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                                vm.increment()
                            }
                        } label: {
                            Image(systemName: "plus")
                                .font(.system(size: 26, weight: .semibold))
                                .frame(width: 64, height: 64)
                                .foregroundStyle(Color.md3OnPrimary)
                                .background(Color.md3Primary)
                                .clipShape(Circle())
                        }
                    }
                }

                Spacer(minLength: 0)

                // Bottom bar
                HStack(spacing: 12) {
                    Button {
                        vm.showResetConfirmation = true
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.counterclockwise")
                            Text("Reset")
                        }
                        .outlinedButtonStyle()
                    }

                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                            .font(.md3LabelLarge)
                            .frame(width: 48, height: 40)
                            .background(Color.md3SecondaryContainer)
                            .foregroundStyle(Color.md3OnSecondaryContainer)
                            .clipShape(Capsule())
                    }

                    Button {
                        vm.newSession()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "plus.circle")
                            Text("New")
                        }
                        .primaryButtonStyle()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.md3SurfaceContainer)
            }
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await vm.loadSaved()
        }
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .alert("Reset Round?", isPresented: $vm.showResetConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Reset", role: .destructive) {
                withAnimation { vm.reset() }
            }
        } message: {
            Text("Reset to round \(vm.state.startingRound) and clear timers?")
        }
        .alert("Set Round", isPresented: $vm.showEditRound) {
            TextField("Round", text: $vm.editRoundText)
                .keyboardType(.numberPad)
            Button("Cancel", role: .cancel) {}
            Button("Set") { vm.commitEditRound() }
        } message: {
            Text("Enter the round number.")
        }
        .sheet(isPresented: $showSettings) {
            settingsSheet
        }
    }

    // MARK: - Timer Bar

    private func timerBar(compact: Bool) -> some View {
        HStack(spacing: 12) {
            // Round / countdown time
            VStack(spacing: 1) {
                Text(vm.isCountdownMode ? "Left" : "Round")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text(vm.isCountdownMode ? vm.formattedCountdown : vm.formattedRoundTime)
                    .font(.system(size: 20, weight: .semibold, design: .monospaced))
                    .foregroundStyle(
                        vm.isCountdownMode && vm.countdownRemaining <= 10 && vm.isTimerRunning
                            ? Color.md3Error : Color.md3OnSurface
                    )
                    .contentTransition(.numericText())
            }

            Divider().frame(height: 28)

            // Session time
            VStack(spacing: 1) {
                Text("Session")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text(vm.formattedSessionTime)
                    .font(.system(size: 20, weight: .semibold, design: .monospaced))
                    .foregroundStyle(Color.md3OnSurface)
                    .contentTransition(.numericText())
            }

            if vm.isTimerRunning {
                Circle()
                    .fill(Color.md3Error)
                    .frame(width: 8, height: 8)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .padding(.horizontal, 20)
        .background(Color.md3Surface)
    }

    // MARK: - Settings Sheet

    private var settingsSheet: some View {
        NavigationStack {
            List {
                Section {
                    Toggle(isOn: $vm.isCountdownMode) {
                        Label("Countdown Mode", systemImage: "timer")
                    }
                    .tint(Color.md3Primary)
                }

                if vm.isCountdownMode {
                    Section("Round Duration") {
                        ForEach(durationOptions, id: \.self) { secs in
                            Button {
                                vm.roundDurationSeconds = secs
                            } label: {
                                HStack {
                                    Text(durationLabel(secs))
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                    if vm.roundDurationSeconds == secs {
                                        Image(systemName: "checkmark")
                                            .foregroundStyle(Color.md3Primary)
                                    }
                                }
                            }
                        }
                    }

                    Section("Max Rounds") {
                        HStack {
                            Text("Rounds")
                            Spacer()
                            HStack(spacing: 12) {
                                Button {
                                    if vm.maxRounds > 1 { vm.maxRounds -= 1 }
                                } label: {
                                    Image(systemName: "minus.circle.fill")
                                        .font(.system(size: 24))
                                        .foregroundStyle(Color.md3Primary)
                                }
                                .disabled(vm.maxRounds <= 1)

                                Text("\(vm.maxRounds)")
                                    .font(.system(size: 22, weight: .bold, design: .rounded))
                                    .frame(width: 40)

                                Button {
                                    if vm.maxRounds < 99 { vm.maxRounds += 1 }
                                } label: {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.system(size: 24))
                                        .foregroundStyle(Color.md3Primary)
                                }
                                .disabled(vm.maxRounds >= 99)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showSettings = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Helpers

    private func durationLabel(_ seconds: Int) -> String {
        if seconds < 60 { return "\(seconds) seconds" }
        let mins = seconds / 60
        return mins == 1 ? "1 minute" : "\(mins) minutes"
    }

    private func iconForEvent(_ type: String) -> String {
        switch type {
        case "incrementRound": return "arrow.up"
        case "decrementRound": return "arrow.down"
        case "setRound": return "pencil"
        case "resetRound": return "arrow.counterclockwise"
        case "autoIncrement": return "arrow.up.circle"
        case "maxRoundReached": return "flag.checkered"
        default: return "circle"
        }
    }

    private func labelForEvent(_ event: RoundCounterEvent) -> String {
        switch event.eventType {
        case "incrementRound": return "Round \(event.roundNumber)"
        case "decrementRound": return "Round \(event.roundNumber)"
        case "setRound": return "Set to round \(event.roundNumber)"
        case "resetRound": return "Reset to round \(event.roundNumber)"
        case "autoIncrement": return "Auto → Round \(event.roundNumber)"
        case "maxRoundReached": return "Max round \(event.roundNumber) reached"
        default: return event.eventType
        }
    }
}
