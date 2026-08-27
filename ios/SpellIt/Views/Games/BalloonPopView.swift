import SwiftUI

struct BalloonPopView: View {
    @State private var store = BankStore.shared
    @State private var engine = RoundEngine()

    private var pool: [WordEntry] {
        store.activeBank.entries
    }

    private func startRound() {
        if pool.count >= 4 {
            engine.start(pool: pool)
        } else {
            engine.clear()
        }
    }

    var body: some View {
        GameScaffold(
            game: .balloonPop,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words")
            } else if let entry = engine.current {
                BalloonWordView(
                    entry: entry,
                    isLast: engine.isLastWord,
                    onJudged: { engine.record(correct: $0) },
                    onNext: { engine.advance() },
                )
                .id("\(engine.roundId)-\(engine.index)-\(store.activeId)")
            }
        }
        .onAppear { if engine.words.isEmpty { startRound() } }
        .onChange(of: store.activeId) { startRound() }
        .onChange(of: store.revision) { startRound() }
    }
}

private struct BalloonShape: View {
    var popped: Bool
    var color: Color

    var body: some View {
        ZStack {
            if popped {
                Image(systemName: "burst.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(Color.mutedInk.opacity(0.5))
            } else {
                VStack(spacing: -2) {
                    Ellipse()
                        .fill(color)
                        .overlay(Ellipse().strokeBorder(Color.ink, lineWidth: 2.5))
                        .frame(width: 30, height: 38)
                    Triangle()
                        .fill(color)
                        .frame(width: 10, height: 7)
                }
            }
        }
        .frame(width: 34, height: 52)
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

struct BalloonWordView: View {
    let entry: WordEntry
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var guessed: Set<Character> = []
    @State private var misses = 0
    @State private var outcome: Bool?
    @State private var shakeTrigger = 0

    private static let maxMisses = 6
    private static let balloonColors: [Color] = [.coral, .sun, .leaf, .sky, .grape, .coral]
    private static let alphabet = Array("abcdefghijklmnopqrstuvwxyz")

    private var size: TileSize { TileSize.forWord(entry.word) }
    private var balloonsLeft: Int { Self.maxMisses - misses }
    /// Guesses are lowercase a–z; compare against the lowercased word so
    /// capitalized entries like "February" stay winnable.
    private var word: String { entry.word.lowercased() }

    var body: some View {
        VStack(spacing: 14) {
            if let hint = entry.hint {
                Text("Clue: \(hint)")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.mutedInk)
                    .multilineTextAlignment(.center)
            }

            HStack(spacing: 2) {
                ForEach(0..<Self.maxMisses, id: \.self) { i in
                    BalloonShape(popped: i >= balloonsLeft, color: Self.balloonColors[i])
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(balloonsLeft) of \(Self.maxMisses) balloons left")

            FlowLayout(spacing: 6) {
                ForEach(Array(word.enumerated()), id: \.offset) { _, letter in
                    let revealed = guessed.contains(letter) || outcome != nil
                    let wasGuessed = guessed.contains(letter)
                    TileView(
                        letter: revealed ? String(letter) : "",
                        size: size,
                        fill: outcome == true ? .leafSoft
                            : revealed && wasGuessed ? .grapeSoft
                            : revealed ? .coralSoft
                            : .grapeSoft.opacity(0.5),
                        dashed: !revealed,
                    )
                }
            }
            .shake(trigger: shakeTrigger)

            if outcome == nil {
                FlowLayout(spacing: 6) {
                    ForEach(Self.alphabet, id: \.self) { letter in
                        let used = guessed.contains(letter)
                        let hit = used && word.contains(letter)
                        TileButton(
                            letter: String(letter),
                            size: .sm,
                            fill: used ? (hit ? .leafSoft : .coralSoft) : .white,
                            disabled: used,
                        ) {
                            guess(letter)
                        }
                    }
                }

                Button {
                    Speaker.shared.speak(entry.word)
                } label: {
                    Label("Hear it", systemImage: "speaker.wave.2.fill")
                }
                .buttonStyle(ChunkyButtonStyle(bordered: true))
            }

            if let outcome {
                FeedbackPanel(correct: outcome, word: entry.word, isLast: isLast, onNext: onNext)
            }
        }
    }

    private func guess(_ letter: Character) {
        guard outcome == nil, !guessed.contains(letter) else { return }
        guessed.insert(letter)
        if word.contains(letter) {
            if word.allSatisfy({ guessed.contains($0) }) {
                outcome = true
                onJudged(true)
            }
        } else {
            misses += 1
            withAnimation { shakeTrigger += 1 }
            if misses >= Self.maxMisses {
                outcome = false
                onJudged(false)
            }
        }
    }
}
