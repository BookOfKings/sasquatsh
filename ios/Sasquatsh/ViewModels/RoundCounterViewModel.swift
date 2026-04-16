import SwiftUI
import AudioToolbox

@Observable
final class RoundCounterViewModel {
    var state: RoundCounterState
    var history: [RoundCounterEvent] = []
    var showResetConfirmation = false
    var showEditRound = false
    var editRoundText = ""

    // Timer
    var isTimerRunning = false
    var roundElapsed: TimeInterval = 0
    var sessionElapsed: TimeInterval = 0
    private var timerTask: Task<Void, Never>?
    private var roundStartTime: Date?
    private var sessionStartTime: Date?
    private var accumulatedSessionTime: TimeInterval = 0
    private var accumulatedRoundTime: TimeInterval = 0

    // Countdown mode
    var isCountdownMode = false
    var roundDurationSeconds: Int = 120 // 2 minutes default
    var maxRounds: Int = 10
    var countdownRemaining: TimeInterval = 0
    var reachedMaxRound = false

    var formattedCountdown: String { formatTime(max(0, countdownRemaining)) }

    private let persistence = RoundCounterPersistence.shared

    init() {
        state = .new()
    }

    func loadSaved() async {
        if let saved = await persistence.load() {
            state = saved
        }
    }

    // MARK: - Timer

    func toggleTimer() {
        if isTimerRunning {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    func startTimer() {
        guard !isTimerRunning else { return }
        isTimerRunning = true
        reachedMaxRound = false
        roundStartTime = Date()
        if sessionStartTime == nil {
            sessionStartTime = Date()
        }
        if isCountdownMode {
            countdownRemaining = Double(roundDurationSeconds) - accumulatedRoundTime
        }

        timerTask?.cancel()
        timerTask = Task { @MainActor [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 100_000_000) // 100ms
                guard let self, self.isTimerRunning else { continue }

                if let roundStart = self.roundStartTime {
                    self.roundElapsed = self.accumulatedRoundTime + Date().timeIntervalSince(roundStart)
                }
                if let sessionStart = self.sessionStartTime {
                    self.sessionElapsed = self.accumulatedSessionTime + Date().timeIntervalSince(sessionStart)
                }

                // Countdown logic
                if self.isCountdownMode {
                    self.countdownRemaining = Double(self.roundDurationSeconds) - self.roundElapsed
                    if self.countdownRemaining <= 0 {
                        self.autoAdvanceRound()
                    }
                }
            }
        }
    }

    func pauseTimer() {
        isTimerRunning = false
        if let roundStart = roundStartTime {
            accumulatedRoundTime += Date().timeIntervalSince(roundStart)
        }
        if let sessionStart = sessionStartTime {
            accumulatedSessionTime += Date().timeIntervalSince(sessionStart)
        }
        roundStartTime = nil
        sessionStartTime = nil
    }

    private func autoAdvanceRound() {
        if state.roundNumber >= maxRounds {
            // Hit max — special sound + stop
            reachedMaxRound = true
            pauseTimer()
            playMaxRoundSound()
            haptic(.heavy)
            logEvent("maxRoundReached")
            return
        }

        // Auto-increment
        state.roundNumber += 1
        state.updatedAt = Date()
        logEvent("autoIncrement")
        resetRoundTimer()
        countdownRemaining = Double(roundDurationSeconds)
        playRoundAdvanceSound()
        haptic(.medium)
        save()
    }

    private func playRoundAdvanceSound() {
        AudioServicesPlaySystemSound(1057) // tock
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            AudioServicesPlaySystemSound(1057)
        }
    }

    private func playMaxRoundSound() {
        AudioServicesPlaySystemSound(1025)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            AudioServicesPlaySystemSound(1025)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            AudioServicesPlaySystemSound(1016)
        }
    }

    private func resetRoundTimer() {
        if isTimerRunning {
            accumulatedRoundTime = 0
            roundStartTime = Date()
            roundElapsed = 0
        } else {
            accumulatedRoundTime = 0
            roundStartTime = nil
            roundElapsed = 0
        }
    }

    private func resetAllTimers() {
        timerTask?.cancel()
        isTimerRunning = false
        roundElapsed = 0
        sessionElapsed = 0
        countdownRemaining = 0
        accumulatedRoundTime = 0
        accumulatedSessionTime = 0
        roundStartTime = nil
        sessionStartTime = nil
        reachedMaxRound = false
    }

    var formattedRoundTime: String { formatTime(roundElapsed) }
    var formattedSessionTime: String { formatTime(sessionElapsed) }

    private func formatTime(_ interval: TimeInterval) -> String {
        let total = Int(interval)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    // MARK: - Actions

    func increment() {
        state.roundNumber += 1
        state.updatedAt = Date()
        logEvent("incrementRound")
        resetRoundTimer()
        if isCountdownMode {
            countdownRemaining = Double(roundDurationSeconds)
        }
        haptic(.light)
        save()
    }

    func decrement() {
        guard state.roundNumber > state.minimumRound else { return }
        state.roundNumber -= 1
        state.updatedAt = Date()
        logEvent("decrementRound")
        resetRoundTimer()
        if isCountdownMode {
            countdownRemaining = Double(roundDurationSeconds)
        }
        haptic(.light)
        save()
    }

    func setRound(_ round: Int) {
        let clamped = max(round, state.minimumRound)
        state.roundNumber = clamped
        state.updatedAt = Date()
        logEvent("setRound")
        resetRoundTimer()
        if isCountdownMode {
            countdownRemaining = Double(roundDurationSeconds)
        }
        save()
    }

    func reset() {
        state.roundNumber = state.startingRound
        state.turnNumber = nil
        state.phaseKey = nil
        state.activePlayerId = nil
        state.updatedAt = Date()
        logEvent("resetRound")
        resetAllTimers()
        haptic(.medium)
        save()
    }

    func updateStartingRound(_ value: Int) {
        state.startingRound = max(value, 0)
        state.minimumRound = state.startingRound
        save()
    }

    func newSession(startingRound: Int = 1) {
        state = .new(startingRound: startingRound)
        history = []
        resetAllTimers()
        save()
    }

    func beginEditRound() {
        editRoundText = "\(state.roundNumber)"
        showEditRound = true
    }

    func commitEditRound() {
        if let value = Int(editRoundText) {
            setRound(value)
        }
        showEditRound = false
    }

    // MARK: - Private

    private func logEvent(_ type: String) {
        let event = RoundCounterEvent.log(state: state, eventType: type)
        history.append(event)
    }

    private func save() {
        Task { await persistence.save(state) }
    }

    private func haptic(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        UIImpactFeedbackGenerator(style: style).impactOccurred()
    }
}
