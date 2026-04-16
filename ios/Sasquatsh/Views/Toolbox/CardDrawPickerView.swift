import SwiftUI
import AudioToolbox

// MARK: - Card Models

enum CardSuit: String, CaseIterable {
    case spades = "♠", hearts = "♥", diamonds = "♦", clubs = "♣"

    var color: Color {
        switch self {
        case .hearts, .diamonds: return .red
        case .spades, .clubs: return .black
        }
    }
}

enum CardRank: Int, CaseIterable, Comparable {
    case two = 2, three, four, five, six, seven, eight, nine, ten
    case jack, queen, king, ace

    var display: String {
        switch self {
        case .ace: return "A"
        case .king: return "K"
        case .queen: return "Q"
        case .jack: return "J"
        default: return "\(rawValue)"
        }
    }

    static func < (lhs: CardRank, rhs: CardRank) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

struct PlayingCard: Identifiable {
    let id = UUID()
    let rank: CardRank
    let suit: CardSuit

    var display: String { "\(rank.display)\(suit.rawValue)" }
}

// MARK: - View

struct CardDrawPickerView: View {
    @State private var playerCount: Int = 4
    @State private var phase: CardPhase = .setup
    @State private var deck: [PlayingCard] = []
    @State private var drawnCards: [Int: PlayingCard] = [:] // playerIndex -> card
    @State private var currentPlayer: Int = 0
    @State private var showCard = false
    @State private var winnerIndices: [Int] = []
    @State private var tiedPlayers: [Int] = []
    @State private var isTiebreaker = false

    private let playerColors: [Color] = [
        .red, .blue, .green, .orange, .purple, .cyan, .pink, .yellow, .mint, .indigo,
        .teal, .brown, Color(red: 0.8, green: 0.2, blue: 0.4), Color(red: 0.4, green: 0.7, blue: 0.2),
        Color(red: 0.9, green: 0.5, blue: 0.1), Color(red: 0.3, green: 0.3, blue: 0.8),
        Color(red: 0.7, green: 0.1, blue: 0.6), Color(red: 0.1, green: 0.6, blue: 0.5),
        Color(red: 0.9, green: 0.3, blue: 0.3), Color(red: 0.2, green: 0.5, blue: 0.7)
    ]

    private var activePlayers: [Int] {
        isTiebreaker ? tiedPlayers : Array(0..<playerCount)
    }

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "High Card Draw") { dismiss() }
            Group {
                switch phase {
                case .setup:
                    setupView
                case .drawing:
                    drawingView
                case .result:
                    resultView
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
    }

    // MARK: - Setup

    private var setupView: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                Image(systemName: "suit.spade.fill")
                    .font(.system(size: 50))
                    .foregroundStyle(Color.md3Primary.opacity(0.4))

                Text("How many players?")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color.md3OnSurface)

                HStack(spacing: 20) {
                    Button {
                        if playerCount > 2 { playerCount -= 1 }
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(playerCount > 2 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(playerCount <= 2)

                    Text("\(playerCount)")
                        .font(.system(size: 44, weight: .bold, design: .rounded))
                        .foregroundStyle(Color.md3OnSurface)
                        .frame(width: 70)

                    Button {
                        if playerCount < 20 { playerCount += 1 }
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(playerCount < 20 ? Color.md3Primary : Color.md3OnSurfaceVariant.opacity(0.3))
                    }
                    .disabled(playerCount >= 20)
                }
            }

            Spacer()

            Button {
                startGame()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "suit.spade.fill")
                    Text("Deal Cards")
                }
                .primaryButtonStyle()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }

    // MARK: - Drawing

    private var drawingView: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 12) {
                    if isTiebreaker {
                        Text("TIEBREAKER!")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(Color.md3Error)
                            .padding(.top, 4)
                    }

                    // Previously drawn cards (compact)
                    if !drawnCards.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(activePlayers.filter({ drawnCards[$0] != nil }), id: \.self) { idx in
                                    if let card = drawnCards[idx] {
                                        VStack(spacing: 2) {
                                            MiniCardView(card: card)
                                            Text("P\(idx + 1)")
                                                .font(.system(size: 10, weight: .medium))
                                                .foregroundStyle(playerColors[idx % playerColors.count])
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                        .frame(height: 70)
                    }

                    if showCard, let card = drawnCards[currentPlayer] {
                        CardFaceView(card: card, playerIndex: currentPlayer, playerColor: playerColors[currentPlayer % playerColors.count])
                            .transition(.asymmetric(
                                insertion: .scale(scale: 0.5).combined(with: .opacity),
                                removal: .opacity
                            ))
                            .padding(.top, 16)
                    } else {
                        VStack(spacing: 16) {
                            Text("Player \(currentPlayer + 1)")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(playerColors[currentPlayer % playerColors.count])

                            Button {
                                drawCard()
                            } label: {
                                CardBackView()
                            }

                            Text("Tap the card to draw")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding(.top, 16)
                    }
                }
                .padding(.bottom, 16)
            }

            if showCard {
                let remaining = activePlayers.filter { drawnCards[$0] == nil }
                if remaining.isEmpty {
                    Button {
                        evaluateRound()
                    } label: {
                        Text("See Results")
                            .primaryButtonStyle()
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
                } else {
                    Button {
                        nextPlayer()
                    } label: {
                        Text("Next: Player \(remaining.first! + 1)")
                            .primaryButtonStyle()
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
                }
            }
        }
    }

    // MARK: - Result

    private var resultView: some View {
        VStack(spacing: 16) {
            if winnerIndices.count == 1 {
                let winner = winnerIndices[0]
                VStack(spacing: 8) {
                    Image(systemName: "crown.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(.yellow)
                    Text("Player \(winner + 1) goes first!")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Color.md3OnSurface)
                }
                .padding(.top, 16)
            }

            ScrollView {
                VStack(spacing: 8) {
                    ForEach(sortedResults, id: \.0) { idx, card in
                        let isWinner = winnerIndices.contains(idx)
                        HStack(spacing: 12) {
                            if isWinner {
                                Image(systemName: "crown.fill")
                                    .font(.system(size: 14))
                                    .foregroundStyle(.yellow)
                            }

                            Circle()
                                .fill(playerColors[idx % playerColors.count])
                                .frame(width: 24, height: 24)
                                .overlay {
                                    Text("\(idx + 1)")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(.white)
                                }

                            Text("Player \(idx + 1)")
                                .font(.system(size: 14, weight: isWinner ? .bold : .medium))
                                .foregroundStyle(Color.md3OnSurface)

                            Spacer()

                            Text(card.display)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(card.suit.color)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(isWinner ? Color.md3PrimaryContainer.opacity(0.5) : Color.md3Surface)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    }
                }
                .padding(.horizontal, 20)
            }

            HStack(spacing: 12) {
                Button {
                    resetFull()
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "arrow.clockwise")
                        Text("Play Again")
                    }
                    .primaryButtonStyle()
                }

                Button {
                    phase = .setup
                    resetFull()
                } label: {
                    Image(systemName: "gear")
                        .font(.md3LabelLarge)
                        .frame(width: 48, height: 40)
                        .background(Color.md3SecondaryContainer)
                        .foregroundStyle(Color.md3OnSecondaryContainer)
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
        }
    }

    private var sortedResults: [(Int, PlayingCard)] {
        drawnCards.sorted { a, b in
            a.value.rank > b.value.rank
        }.map { ($0.key, $0.value) }
    }

    // MARK: - Logic

    private func startGame() {
        deck = buildShuffledDeck()
        drawnCards = [:]
        currentPlayer = 0
        showCard = false
        winnerIndices = []
        tiedPlayers = []
        isTiebreaker = false
        phase = .drawing
    }

    private func buildShuffledDeck() -> [PlayingCard] {
        var cards: [PlayingCard] = []
        for suit in CardSuit.allCases {
            for rank in CardRank.allCases {
                cards.append(PlayingCard(rank: rank, suit: suit))
            }
        }
        return cards.shuffled()
    }

    private func drawCard() {
        guard let card = deck.popLast() else { return }
        withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
            drawnCards[currentPlayer] = card
            showCard = true
        }
        // Card flip sound
        AudioServicesPlaySystemSound(1104)
    }

    private func nextPlayer() {
        showCard = false
        let remaining = activePlayers.filter { drawnCards[$0] == nil }
        if let next = remaining.first {
            currentPlayer = next
        }
    }

    private func evaluateRound() {
        guard let maxRank = drawnCards.values.map(\.rank).max() else { return }
        let winners = drawnCards.filter { $0.value.rank == maxRank }.map(\.key)

        if winners.count == 1 {
            winnerIndices = winners
            withAnimation {
                phase = .result
            }
            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(.success)
            AudioServicesPlaySystemSound(1025)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                AudioServicesPlaySystemSound(1016)
            }
        } else {
            // Tie — start tiebreaker
            tiedPlayers = winners.sorted()
            isTiebreaker = true
            drawnCards = [:]
            showCard = false
            deck = buildShuffledDeck()
            currentPlayer = tiedPlayers.first!

            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(.warning)
        }
    }

    private func resetFull() {
        deck = buildShuffledDeck()
        drawnCards = [:]
        currentPlayer = 0
        showCard = false
        winnerIndices = []
        tiedPlayers = []
        isTiebreaker = false
        if phase != .setup {
            phase = .drawing
        }
    }
}

enum CardPhase {
    case setup, drawing, result
}

// MARK: - Card Views

struct CardFaceView: View {
    let card: PlayingCard
    let playerIndex: Int
    let playerColor: Color

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    Text(card.rank.display)
                        .font(.system(size: 28, weight: .bold))
                    Text(card.suit.rawValue)
                        .font(.system(size: 22))
                }
                .foregroundStyle(card.suit.color)
                Spacer()
            }

            Spacer()

            Text(card.suit.rawValue)
                .font(.system(size: 60))
                .foregroundStyle(card.suit.color)

            Spacer()

            HStack {
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text(card.suit.rawValue)
                        .font(.system(size: 22))
                    Text(card.rank.display)
                        .font(.system(size: 28, weight: .bold))
                }
                .foregroundStyle(card.suit.color)
                .rotationEffect(.degrees(180))
            }
        }
        .padding(12)
        .frame(width: 160, height: 220)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay {
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
        }
        .shadow(color: .black.opacity(0.15), radius: 6, y: 3)
    }
}

struct CardBackView: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(red: 0.1, green: 0.2, blue: 0.6))
                .frame(width: 160, height: 220)

            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.white.opacity(0.3), lineWidth: 2)
                .frame(width: 140, height: 200)

            // Diamond pattern
            VStack(spacing: 8) {
                ForEach(0..<5, id: \.self) { row in
                    HStack(spacing: 8) {
                        ForEach(0..<3, id: \.self) { col in
                            Image(systemName: "diamond.fill")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.white.opacity(0.15))
                        }
                    }
                }
            }
        }
        .shadow(color: .black.opacity(0.15), radius: 6, y: 3)
    }
}

struct MiniCardView: View {
    let card: PlayingCard

    var body: some View {
        VStack(spacing: 0) {
            Text(card.rank.display)
                .font(.system(size: 14, weight: .bold))
            Text(card.suit.rawValue)
                .font(.system(size: 12))
        }
        .foregroundStyle(card.suit.color)
        .frame(width: 36, height: 48)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 4))
        .overlay {
            RoundedRectangle(cornerRadius: 4)
                .stroke(Color.gray.opacity(0.3), lineWidth: 0.5)
        }
    }
}
