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
            engine: engine,
            onRestart: startRound,
        ) {
            GradePicker(grade: grade)
        } content: {
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

private struct OperatorText: View {
    var glyph: String

    init(_ glyph: String) { self.glyph = glyph }

    var body: some View {
        Text(glyph)
            .font(.heading(20, weight: .semibold))
            .foregroundStyle(Color.mutedInk)
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

    private var baseLetters: [Character] { Array(task.base) }
    private var suffixLetters: [Character] { Array(task.suffix) }

    var body: some View {
        VStack(spacing: 14) {
            // Shrink the equation to keep it on one line; once even the
            // smallest readable tiles overflow, stack it rather than let the
            // words break wherever the row runs out of room.
            ViewThatFits(in: .horizontal) {
                inlineEquation(side: TileLadder.sides[0])
                inlineEquation(side: TileLadder.sides[1])
                inlineEquation(side: TileLadder.sides[2])
                inlineEquation(side: TileLadder.sides[3])
                stackedEquation()
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

    @ViewBuilder
    private func inlineEquation(side: CGFloat) -> some View {
        let spacing = TileLadder.spacing(for: side)
        let size = TileSize(side: side)
        HStack(spacing: spacing * 2) {
            HStack(spacing: spacing) {
                ForEach(baseLetters.indices, id: \.self) { i in
                    TileView(letter: String(baseLetters[i]), size: size, fill: .secondaryBg)
                }
            }
            OperatorText("+")
            HStack(spacing: spacing) {
                ForEach(suffixLetters.indices, id: \.self) { i in
                    TileView(letter: String(suffixLetters[i]), size: size, fill: .sunSoft)
                }
            }
            OperatorText("=")
            TileView(letter: "?", size: size, fill: .sunSoft)
        }
    }

    @ViewBuilder
    private func stackedEquation() -> some View {
        VStack(spacing: 6) {
            TileRow(count: baseLetters.count) { i, size in
                TileView(letter: String(baseLetters[i]), size: size, fill: .secondaryBg)
            }
            OperatorText("+")
            TileRow(count: suffixLetters.count) { i, size in
                TileView(letter: String(suffixLetters[i]), size: size, fill: .sunSoft)
            }
            OperatorText("=")
            TileView(letter: "?", size: .md, fill: .sunSoft)
        }
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
