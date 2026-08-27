import SwiftUI

struct ListenSpellView: View {
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
            game: .listenAndSpell,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words")
            } else if let entry = engine.current {
                ListenWordView(
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

struct ListenWordView: View {
    let entry: WordEntry
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var typed = ""
    @State private var outcome: Bool?
    @State private var retrying = false
    @State private var showHint = false
    @FocusState private var inputFocused: Bool

    var body: some View {
        VStack(spacing: 14) {
            Button {
                Speaker.shared.speak(entry.word)
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.ink)
                        .offset(y: 5)
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.skySoft)
                        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Color.ink, lineWidth: 3.5))
                    Image(systemName: "speaker.wave.3.fill")
                        .font(.system(size: 38))
                        .foregroundStyle(Color.ink)
                }
                .frame(width: 96, height: 96)
            }
            .buttonStyle(PressStyle())
            .accessibilityLabel("Play the word out loud")

            Text("Tap the speaker to hear your word.")
                .font(.system(size: 13))
                .foregroundStyle(Color.mutedInk)

            if outcome == nil {
                SpellingField(
                    placeholder: "Type the word…",
                    text: $typed,
                    onSubmit: submit,
                    focused: $inputFocused,
                )

                if retrying {
                    Text("Not quite — listen again and give it one more try!")
                        .font(.heading(14, weight: .medium))
                        .foregroundStyle(Color.coral)
                        .multilineTextAlignment(.center)
                }

                HStack(spacing: 10) {
                    Button("Check my spelling", action: submit)
                        .buttonStyle(ChunkyButtonStyle())
                        .disabled(typed.trimmingCharacters(in: .whitespaces).isEmpty)
                    if entry.hint != nil {
                        Button {
                            showHint = true
                        } label: {
                            Label("Clue", systemImage: "lightbulb.fill")
                        }
                        .buttonStyle(ChunkyButtonStyle(bordered: true))
                        .disabled(showHint)
                    }
                }

                if showHint, let hint = entry.hint {
                    Text("Clue: \(hint)")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.mutedInk)
                        .multilineTextAlignment(.center)
                }
            }

            if let outcome {
                FeedbackPanel(correct: outcome, word: entry.word, isLast: isLast, onNext: onNext)
            }
        }
        .onAppear { Speaker.shared.speak(entry.word) }
    }

    private func submit() {
        guard outcome == nil else { return }
        let attempt = typed.trimmingCharacters(in: .whitespaces).lowercased()
        guard !attempt.isEmpty else { return }
        if attempt == entry.word.lowercased() {
            outcome = true
            onJudged(true)
        } else if !retrying {
            retrying = true
            typed = ""
            Speaker.shared.speak(entry.word)
        } else {
            outcome = false
            onJudged(false)
        }
    }
}
