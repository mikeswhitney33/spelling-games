import SwiftUI

struct ListenSpellView: View {
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
            game: .listenAndSpell,
            grade: grade,
            engine: engine,
            onRestart: { engine.start(pool: pool) },
        ) {
            if let entry = engine.current {
                ListenWordView(
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
                    Button {
                        showHint = true
                    } label: {
                        Label("Clue", systemImage: "lightbulb.fill")
                    }
                    .buttonStyle(ChunkyButtonStyle(bordered: true))
                    .disabled(showHint)
                }

                if showHint {
                    Text("Clue: \(entry.hint)")
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
