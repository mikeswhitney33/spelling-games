import SwiftUI

struct EndingMachineView: View {
    @AppStorage("spellit.grade") private var gradeRaw = GradeBand.g23.rawValue
    @State private var engine = RoundEngine()
    @State private var tasks: [EndingTask] = []

    private var grade: Binding<GradeBand> {
        Binding(
            get: { GradeBand(rawValue: gradeRaw) ?? .g23 },
            set: { gradeRaw = $0.rawValue },
        )
    }

    private func startRound() {
        tasks = pickRandom(WordData.endings[grade.wrappedValue] ?? [], RoundEngine.roundLength)
        engine.start(fixedWords: tasks.map {
            WordEntry(word: $0.word, hint: $0.hint, sentence: "")
        })
        // Keep the tasks aligned with the engine's shuffled order.
        tasks = engine.words.compactMap { word in
            (WordData.endings[grade.wrappedValue] ?? []).first { $0.word == word.word }
        }
    }

    var body: some View {
        GameScaffold(
            game: .endingMachine,
            grade: grade,
            engine: engine,
            onRestart: startRound,
        ) {
            if engine.current != nil, engine.index < tasks.count {
                EndingTaskView(
                    task: tasks[engine.index],
                    isLast: engine.isLastWord,
                    onJudged: { engine.record(correct: $0) },
                    onNext: { engine.advance() },
                )
                .id("\(engine.roundId)-\(engine.index)")
            }
        }
        .onAppear { if engine.words.isEmpty { startRound() } }
        .onChange(of: gradeRaw) { startRound() }
    }
}

struct EndingTaskView: View {
    let task: EndingTask
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var typed = ""
    @State private var outcome: Bool?
    @State private var retrying = false
    @FocusState private var inputFocused: Bool

    private var size: TileSize { TileSize.forWord(task.base + task.suffix) }

    var body: some View {
        VStack(spacing: 14) {
            FlowLayout(spacing: 8) {
                ForEach(Array(task.base.enumerated()), id: \.offset) { _, letter in
                    TileView(letter: String(letter), size: size, fill: .secondaryBg)
                }
                Text("+")
                    .font(.heading(24, weight: .semibold))
                    .foregroundStyle(Color.mutedInk)
                    .frame(height: size.side)
                ForEach(Array(task.suffix.enumerated()), id: \.offset) { _, letter in
                    TileView(letter: String(letter), size: size, fill: .sunSoft)
                }
                Text("=")
                    .font(.heading(24, weight: .semibold))
                    .foregroundStyle(Color.mutedInk)
                    .frame(height: size.side)
                TileView(letter: "?", size: size, fill: .sunSoft)
            }

            if outcome == nil {
                SpellingField(
                    placeholder: "What comes out?",
                    text: $typed,
                    onSubmit: submit,
                    focused: $inputFocused,
                )

                if retrying {
                    Text("Not quite! Hint: \(task.hint)")
                        .font(.heading(14, weight: .medium))
                        .foregroundStyle(Color.coral)
                        .multilineTextAlignment(.center)
                }

                Button("Crank the machine", action: submit)
                    .buttonStyle(ChunkyButtonStyle())
                    .disabled(typed.trimmingCharacters(in: .whitespaces).isEmpty)
            }

            if let outcome {
                Text("Rule: \(task.hint)")
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.ink)
                    .multilineTextAlignment(.center)
                    .padding(12)
                    .frame(maxWidth: .infinity)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color.sunSoft))
                FeedbackPanel(correct: outcome, word: task.word, isLast: isLast, onNext: onNext)
            }
        }
        .onAppear { inputFocused = true }
    }

    private func submit() {
        guard outcome == nil else { return }
        let attempt = typed.trimmingCharacters(in: .whitespaces).lowercased()
        guard !attempt.isEmpty else { return }
        if attempt == task.word.lowercased() || task.also.contains(attempt) {
            outcome = true
            onJudged(true)
        } else if !retrying {
            retrying = true
            typed = ""
        } else {
            outcome = false
            onJudged(false)
        }
    }
}
