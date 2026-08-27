import SwiftUI

struct FlashSpellView: View {
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
            game: .flashSpell,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words")
            } else if let entry = engine.current {
                FlashWordView(
                    entry: entry,
                    showSeconds: GameHeuristics.flashSeconds(for: entry.word),
                    isLast: engine.isLastWord,
                    onJudged: { engine.record(correct: $0) },
                    onNext: { engine.advance() },
                )
                .id("\(engine.roundId)-\(engine.index)")
            }
        }
        .onAppear { if engine.words.isEmpty { startRound() } }
        .onChange(of: store.activeId) { startRound() }
        .onChange(of: store.revision) { startRound() }
    }
}

struct FlashWordView: View {
    let entry: WordEntry
    let showSeconds: Double
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    private enum Phase {
        case show
        case type
    }

    @State private var phase = Phase.show
    @State private var typed = ""
    @State private var outcome: Bool?
    @State private var retrying = false
    @State private var showTask: Task<Void, Never>?
    @FocusState private var inputFocused: Bool

    private var size: TileSize { TileSize.forWord(entry.word) }

    var body: some View {
        VStack(spacing: 14) {
            if let hint = entry.hint {
                Text("Clue: \(hint)")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.mutedInk)
                    .multilineTextAlignment(.center)
            }

            if phase == .show {
                WordTilesView(word: entry.word, fill: .coralSoft, size: size)
                Text(retrying ? "One more look — you've got this!" : "Look closely… it's about to hide!")
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.mutedInk)
                Button {
                    Speaker.shared.speak(entry.word)
                } label: {
                    Label("Hear it", systemImage: "speaker.wave.2.fill")
                }
                .buttonStyle(ChunkyButtonStyle(bordered: true))
            }

            if phase == .type, outcome == nil {
                FlowLayout(spacing: 6) {
                    ForEach(0..<entry.word.count, id: \.self) { _ in
                        TileView(letter: "", size: size, dashed: true)
                    }
                }

                SpellingField(
                    placeholder: "Type it from memory…",
                    text: $typed,
                    onSubmit: submit,
                    focused: $inputFocused,
                )

                if retrying {
                    Text("Not quite — try once more!")
                        .font(.heading(14, weight: .medium))
                        .foregroundStyle(Color.coral)
                }

                HStack(spacing: 10) {
                    Button("Check my spelling", action: submit)
                        .buttonStyle(ChunkyButtonStyle())
                        .disabled(typed.trimmingCharacters(in: .whitespaces).isEmpty)
                    Button {
                        Speaker.shared.speak(entry.word)
                    } label: {
                        Label("Hear it", systemImage: "speaker.wave.2.fill")
                    }
                    .buttonStyle(ChunkyButtonStyle(bordered: true))
                }
            }

            if let outcome {
                FeedbackPanel(correct: outcome, word: entry.word, isLast: isLast, onNext: onNext)
            }
        }
        .onAppear { scheduleHide(after: showSeconds) }
        .onDisappear { showTask?.cancel() }
    }

    private func scheduleHide(after seconds: Double) {
        showTask?.cancel()
        showTask = Task {
            try? await Task.sleep(for: .seconds(seconds))
            guard !Task.isCancelled else { return }
            phase = .type
            inputFocused = true
        }
    }

    private func submit() {
        guard outcome == nil, phase == .type else { return }
        let attempt = typed.trimmingCharacters(in: .whitespaces).lowercased()
        guard !attempt.isEmpty else { return }
        if attempt == entry.word.lowercased() {
            outcome = true
            onJudged(true)
        } else if !retrying {
            retrying = true
            typed = ""
            phase = .show
            scheduleHide(after: 2)
        } else {
            outcome = false
            onJudged(false)
        }
    }
}
