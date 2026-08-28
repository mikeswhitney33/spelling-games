import SwiftUI

struct WordScrambleView: View {
    @State private var store = BankStore.shared
    @State private var engine = RoundEngine()

    private var pool: [WordEntry] {
        store.activeBank.entries.filter { $0.word.count >= 3 }
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
            game: .wordScramble,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words of three or more letters")
            } else if let entry = engine.current {
                ScrambleWordView(
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

    var body: some View {
        VStack(spacing: 14) {
            if let hint = entry.hint {
                Text("Clue: \(hint)")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.mutedInk)
                    .multilineTextAlignment(.center)
            }

            // Answer slots
            TileRow(count: letters.count) { i, size in
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
            .shake(trigger: shakeTrigger)

            if retrying, outcome == nil {
                Text("Not quite — try again!")
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.coral)
            }

            if outcome == nil {
                TileRow(count: letters.count) { i, size in
                    TileButton(
                        letter: String(letters[i]),
                        size: size,
                        fill: .coralSoft,
                        disabled: picked.contains(i) || shaking,
                    ) {
                        pick(i)
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
