import SwiftUI
import AVFoundation

struct StatementPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.services) private var services
    @State private var statements: [String] = []
    @State private var isLoading = false
    @State private var showStatements = false
    @State private var error: String?
    @State private var isSpeaking = false

    private let synthesizer = AVSpeechSynthesizer()

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "Whoever Last Picker") { dismiss() }
            ScrollView {
                VStack(spacing: 16) {
                    if showStatements, !statements.isEmpty {
                        ForEach(Array(statements.enumerated()), id: \.offset) { index, statement in
                            HStack(alignment: .top, spacing: 12) {
                                Text("\(index + 1)")
                                    .font(.system(size: 16, weight: .bold, design: .rounded))
                                    .foregroundStyle(Color.md3Primary)
                                    .frame(width: 26, height: 26)
                                    .background(Color.md3Primary.opacity(0.12))
                                    .clipShape(Circle())

                                Text(statement)
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundStyle(Color.md3OnSurface)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(14)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }
                    } else if isLoading {
                        Spacer(minLength: 120)
                    D20ProgressView(size: 32)
                            .tint(Color.md3Primary)
                            .scaleEffect(1.5)
                    } else {
                        Spacer(minLength: 80)
                        VStack(spacing: 16) {
                            Image(systemName: "questionmark.circle.fill")
                                .font(.system(size: 60))
                                .foregroundStyle(Color.md3Primary.opacity(0.3))
                            Text("Who goes first?")
                                .font(.system(size: 22, weight: .medium))
                                .foregroundStyle(Color.md3OnSurface.opacity(0.6))
                            Text("Loading 3 random prompts...")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }

                    if let error {
                        Text(error)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Error)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
            }

            // Pinned buttons at bottom
            HStack(spacing: 12) {
                Button {
                    fetchStatements()
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "arrow.clockwise")
                        Text("Pick Again")
                    }
                    .primaryButtonStyle()
                }
                .disabled(isLoading)

                if showStatements {
                    Button {
                        speakStatements()
                    } label: {
                        Image(systemName: isSpeaking ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .font(.md3LabelLarge)
                            .frame(width: 48, height: 40)
                            .background(Color.md3SecondaryContainer)
                            .foregroundStyle(Color.md3OnSecondaryContainer)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.md3SurfaceContainer)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            if statements.isEmpty {
                fetchStatements()
            }
        }
        .onDisappear {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }

    private func speakStatements() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
            isSpeaking = false
            return
        }

        let text = statements.enumerated().map { index, s in
            "Number \(index + 1). \(s)"
        }.joined(separator: ". . . ")

        let utterance = AVSpeechUtterance(string: text)
        // Pick the highest quality downloaded voice for the user's language
        let preferredLang = Locale.preferredLanguages.first ?? "en-US"
        let langPrefix = String(preferredLang.prefix(2)) // "en" from "en-US"
        let bestVoice = AVSpeechSynthesisVoice.speechVoices()
            .filter { $0.language.hasPrefix(langPrefix) }
            .sorted { $0.quality.rawValue > $1.quality.rawValue }
            .first
        if let bestVoice {
            utterance.voice = bestVoice
        }
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9
        utterance.pitchMultiplier = 1.05
        utterance.preUtteranceDelay = 0.3

        isSpeaking = true
        synthesizer.speak(utterance)

        // Reset speaking state when done
        Task {
            while synthesizer.isSpeaking {
                try? await Task.sleep(for: .milliseconds(200))
            }
            isSpeaking = false
        }
    }

    private func fetchStatements() {
        isLoading = true
        error = nil
        withAnimation {
            showStatements = false
        }

        Task {
            do {
                var fetched: [String] = []
                for _ in 0..<3 {
                    let result = try await services.firstPlayer.getRandomStatement()
                    if !fetched.contains(result.statement) {
                        fetched.append(result.statement)
                    } else {
                        // Try once more to avoid duplicate
                        let retry = try await services.firstPlayer.getRandomStatement()
                        fetched.append(retry.statement)
                    }
                }
                try? await Task.sleep(for: .milliseconds(300))
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                    statements = fetched
                    showStatements = true
                    isLoading = false
                }
            } catch {
                self.error = "Couldn't load statements. Try again!"
                isLoading = false
            }
        }
    }
}
