import SwiftUI

struct WordScrambleView: View {
    @AppStorage("spellit.grade") private var gradeRaw = GradeBand.g23.rawValue
    @State private var engine = RoundEngine()

    private var grade: Binding<GradeBand> {
        Binding(
            get: { GradeBand(rawValue: gradeRaw) ?? .g23 },
            set: { gradeRaw = $0.rawValue },
        )
    }

    private var pool: [WordEntry] { WordData.words[grade.wrappedValue] ?? [] }

    var body: some View {
        GameScaffold(
            game: .wordScramble,
            grade: grade,
            engine: engine,
            onRestart: { engine.start(pool: pool) },
        ) {
            if let entry = engine.current {
                ScrambleWordView(
                    entry: entry,
                    isLast: engine.isLastWord,
                    onJudged: { engine.record(correct: $0) },
                    onNext: { engine.advance() },
                )
                .id("\(engine.roundId)-\(engine.index)")
            }
        }
        .onAppear { if engine.words.isEmpty { engine.start(pool: pool) } }
        .onChange(of: gradeRaw) { engine.start(pool: pool) }
    }
}

/// Scramble a word's letters, guaranteed different when possible.
func scrambleLetters(_ word: String) -> [Character] {
    let original = Array(word)
    for _ in 0..<20 {
        let mixed = original.shuffled()
        if String(mixed) != word { return mixed }
    }
    return original.reversed()
}

struct ScrambleWordView: View {
    let entry: WordEntry
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var letters: [Character] = []
    @State private var picked: [Int] = []
    @State private var outcome: Bool?
    @State private var retrying = false
    @State private var shaking = false
    @State private var shakeTrigger = 0

    private var size: TileSize { TileSize.forWord(entry.word) }

    var body: some View {
        VStack(spacing: 14) {
            Text("Clue: \(entry.hint)")
                .font(.system(size: 14))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)

            // Answer slots
            FlowLayout(spacing: 6) {
                ForEach(0..<letters.count, id: \.self) { i in
                    if i < picked.count {
                        TileButton(
                            letter: String(letters[picked[i]]),
                            size: size,
                            fill: outcome == true ? .leafSoft : shaking ? .coralSoft : .white,
                        ) {
                            unpick(at: i)
                        }
                    } else {
                        TileView(letter: "", size: size, dashed: true)
                    }
                }
            }
            .shake(trigger: shakeTrigger)

            if retrying, outcome == nil {
                Text("Not quite — try again!")
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.coral)
            }

            if outcome == nil {
                FlowLayout(spacing: 6) {
                    ForEach(letters.indices, id: \.self) { i in
                        TileButton(
                            letter: String(letters[i]),
                            size: size,
                            fill: .coralSoft,
                            disabled: picked.contains(i) || shaking,
                        ) {
                            pick(i)
                        }
                    }
                }

                HStack(spacing: 10) {
                    Button {
                        Speaker.shared.speak(entry.word)
                    } label: {
                        Label("Hear it", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(ChunkyButtonStyle(bordered: true))
                    Button("Clear") { picked = [] }
                        .buttonStyle(ChunkyButtonStyle(bordered: true))
                        .disabled(picked.isEmpty || shaking)
                }
            }

            if let outcome {
                FeedbackPanel(correct: outcome, word: entry.word, isLast: isLast, onNext: onNext)
            }
        }
        .onAppear { if letters.isEmpty { letters = scrambleLetters(entry.word) } }
    }

    private func pick(_ index: Int) {
        guard outcome == nil, !picked.contains(index) else { return }
        var next = picked
        next.append(index)
        if next.count < letters.count {
            picked = next
            return
        }
        let attempt = String(next.map { letters[$0] })
        picked = next
        if attempt == entry.word {
            outcome = true
            onJudged(true)
        } else if !retrying {
            shaking = true
            shakeTrigger += 1
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
                shaking = false
                picked = []
                retrying = true
            }
        } else {
            outcome = false
            onJudged(false)
        }
    }

    private func unpick(at position: Int) {
        guard outcome == nil, !shaking else { return }
        picked.remove(at: position)
    }
}
