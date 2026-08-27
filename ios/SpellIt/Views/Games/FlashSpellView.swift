import SwiftUI

struct FlashSpellView: View {
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
            game: .flashSpell,
            grade: grade,
            engine: engine,
            onRestart: { engine.start(pool: pool) },
        ) {
            if let entry = engine.current {
                FlashWordView(
                    entry: entry,
                    grade: grade.wrappedValue,
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

struct FlashWordView: View {
    let entry: WordEntry
    let grade: GradeBand
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

    private static let showSeconds: [GradeBand: Double] = [
        .k1: 4, .g23: 3.5, .g45: 3, .g6plus: 3,
    ]

    private var size: TileSize { TileSize.forWord(entry.word) }

    var body: some View {
        VStack(spacing: 14) {
            Text("Clue: \(entry.hint)")
                .font(.system(size: 14))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)

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
        .onAppear { scheduleHide(after: Self.showSeconds[grade] ?? 3) }
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
