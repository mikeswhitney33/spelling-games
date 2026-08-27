import SwiftUI

struct MemoryMatchView: View {
    @State private var store = BankStore.shared

    private var pool: [WordEntry] {
        store.activeBank.entries.filter { $0.hint != nil }
    }

    private struct Card: Identifiable {
        let id: Int
        let pairId: Int
        let isWord: Bool
        let text: String
    }

    private static let pairCount = 6

    @State private var entries: [WordEntry] = []
    @State private var cards: [Card] = []
    @State private var faceUp: [Int] = []
    @State private var matched: Set<Int> = []
    @State private var attempts = 0
    @State private var finished = false
    @State private var resolveTask: Task<Void, Never>?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header
                BankPickerView()

                VStack(spacing: 14) {
                    if pool.count < Self.pairCount {
                        NotEnoughWordsView(need: Self.pairCount, requirement: "words with hints")
                    } else if finished {
                        RoundSummaryView(
                            score: scoreForAttempts,
                            total: Self.pairCount,
                            bestStreak: 0,
                            missed: [],
                            summaryText:
                                "You matched all \(Self.pairCount) pairs in \(attempts) \(attempts == 1 ? "try" : "tries")!",
                            onRestart: startRound,
                        )
                    } else {
                        board
                    }
                }
                .padding(18)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white)
                        .shadow(color: Color.ink.opacity(0.08), radius: 6, y: 3)
                )
            }
            .padding(16)
        }
        .background(Color.paper)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if cards.isEmpty { startRound() } }
        .onChange(of: store.activeId) { startRound() }
        .onDisappear { resolveTask?.cancel() }
    }

    private var header: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14).fill(Color.ink).offset(y: 4)
                RoundedRectangle(cornerRadius: 14)
                    .fill(Game.memoryMatch.accentSoft)
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                Image(systemName: Game.memoryMatch.symbol)
                    .font(.system(size: 24))
                    .foregroundStyle(Color.ink)
            }
            .frame(width: 54, height: 54)
            VStack(alignment: .leading, spacing: 2) {
                Text(Game.memoryMatch.title)
                    .font(.heading(26, weight: .semibold))
                    .foregroundStyle(Color.ink)
                Text(Game.memoryMatch.instructions)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.mutedInk)
            }
        }
    }

    private var board: some View {
        VStack(spacing: 12) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                ForEach(cards) { card in
                    cardView(card)
                }
            }
            Text(statusText)
                .font(.heading(13, weight: .medium))
                .foregroundStyle(Color.mutedInk)
        }
    }

    private var statusText: String {
        if attempts == 0, faceUp.isEmpty { return "Flip a card to start!" }
        if attempts == 0 { return "Now find its partner!" }
        return "\(matched.count) of \(Self.pairCount) pairs matched · \(attempts) \(attempts == 1 ? "try" : "tries")"
    }

    private func cardView(_ card: Card) -> some View {
        let isMatched = matched.contains(card.pairId)
        let isUp = isMatched || faceUp.contains(card.id)
        return Button {
            flip(card)
        } label: {
            ZStack {
                if !isUp {
                    RoundedRectangle(cornerRadius: 14).fill(Color.ink).offset(y: 4)
                }
                RoundedRectangle(cornerRadius: 14)
                    .fill(isMatched ? Color.leafSoft : isUp ? Color.skySoft : Color.ink)
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                if isUp {
                    Text(card.text)
                        .font(card.isWord ? .heading(17, weight: .semibold) : .system(size: 11))
                        .foregroundStyle(Color.ink)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.7)
                        .padding(6)
                } else {
                    Text("?")
                        .font(.heading(26, weight: .semibold))
                        .foregroundStyle(Color.white)
                }
            }
            .frame(minHeight: 92)
            .opacity(isMatched ? 0.8 : 1)
        }
        .buttonStyle(PressStyle())
        .accessibilityLabel(
            isUp ? "\(card.isWord ? "Word" : "Clue"): \(card.text)\(isMatched ? ", matched" : "")" : "Hidden card"
        )
    }

    private var scoreForAttempts: Int {
        let total = Self.pairCount
        if attempts <= total + 2 { return total }
        if attempts <= total + 6 { return Int(ceil(Double(total) * 0.75)) }
        if attempts <= total + 11 { return Int(ceil(Double(total) * 0.6)) }
        return Int(ceil(Double(total) * 0.5))
    }

    private func startRound() {
        resolveTask?.cancel()
        guard pool.count >= Self.pairCount else {
            entries = []
            cards = []
            return
        }
        entries = pickRandom(pool, Self.pairCount)
        cards = entries.enumerated().flatMap { pairId, entry in
            [
                Card(id: pairId * 2, pairId: pairId, isWord: true, text: entry.word),
                Card(id: pairId * 2 + 1, pairId: pairId, isWord: false, text: entry.hint ?? ""),
            ]
        }.shuffled()
        faceUp = []
        matched = []
        attempts = 0
        finished = false
    }

    private func flip(_ card: Card) {
        guard faceUp.count < 2, !faceUp.contains(card.id), !matched.contains(card.pairId) else { return }
        faceUp.append(card.id)
        guard faceUp.count == 2 else { return }

        attempts += 1
        let flipped = faceUp.compactMap { id in cards.first { $0.id == id } }
        if flipped.count == 2, flipped[0].pairId == flipped[1].pairId {
            matched.insert(flipped[0].pairId)
            faceUp = []
            if matched.count == Self.pairCount {
                finished = true
            }
        } else {
            resolveTask = Task {
                try? await Task.sleep(for: .seconds(1.1))
                guard !Task.isCancelled else { return }
                faceUp = []
            }
        }
    }
}
