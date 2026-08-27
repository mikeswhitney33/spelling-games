import SwiftUI

struct MissingLettersView: View {
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
            game: .missingLetters,
            grade: grade,
            engine: engine,
            onRestart: { engine.start(pool: pool) },
        ) {
            if let entry = engine.current {
                MissingLettersWordView(
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

struct MissingLettersWordView: View {
    let entry: WordEntry
    let grade: GradeBand
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var positions: [Int] = []
    @State private var bank: [Character] = []
    /// For each blank, the bank index placed there.
    @State private var placed: [Int?] = []
    @State private var outcome: Bool?
    @State private var retrying = false
    @State private var shaking = false
    @State private var shakeTrigger = 0

    private static let blanksPerGrade: [GradeBand: Int] = [
        .k1: 1, .g23: 2, .g45: 3, .g6plus: 4,
    ]

    private var size: TileSize { TileSize.forWord(entry.word) }
    private var chars: [Character] { Array(entry.word) }

    var body: some View {
        VStack(spacing: 14) {
            Text("Clue: \(entry.hint)")
                .font(.system(size: 14))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)

            FlowLayout(spacing: 6) {
                ForEach(chars.indices, id: \.self) { pos in
                    if let blankIndex = positions.firstIndex(of: pos) {
                        if let bankIndex = placed.indices.contains(blankIndex) ? placed[blankIndex] : nil {
                            TileButton(
                                letter: String(bank[bankIndex]),
                                size: size,
                                fill: outcome == true ? .leafSoft : shaking ? .coralSoft : .sunSoft,
                            ) {
                                clearBlank(blankIndex)
                            }
                        } else {
                            TileView(letter: "", size: size, fill: .sunSoft, dashed: true)
                        }
                    } else {
                        TileView(letter: String(chars[pos]), size: size, fill: .secondaryBg)
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
                    ForEach(bank.indices, id: \.self) { i in
                        TileButton(
                            letter: String(bank[i]),
                            size: .md,
                            disabled: placed.contains(i) || shaking,
                        ) {
                            pickFromBank(i)
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
        .onAppear(perform: setup)
    }

    private func setup() {
        guard positions.isEmpty else { return }
        let blanks = min(Self.blanksPerGrade[grade] ?? 2, chars.count)
        positions = Array(chars.indices.shuffled().prefix(blanks)).sorted()
        let needed = positions.map { chars[$0] }
        // Compare lowercased so a needed capital ("F" in February) can't draw
        // its lowercase twin as a distractor.
        let neededLower = Set(needed.flatMap { $0.lowercased() })
        let distractors = "abcdefghijklmnopqrstuvwxyz".filter { !neededLower.contains($0) }
            .shuffled().prefix(3)
        bank = (needed + distractors).shuffled()
        placed = Array(repeating: nil, count: positions.count)
    }

    private func pickFromBank(_ bankIndex: Int) {
        guard outcome == nil, !shaking, !placed.contains(bankIndex) else { return }
        guard let firstEmpty = placed.firstIndex(of: nil) else { return }
        var next = placed
        next[firstEmpty] = bankIndex
        placed = next
        guard !next.contains(where: { $0 == nil }) else { return }

        let correct = positions.enumerated().allSatisfy { i, pos in
            bank[next[i]!] == chars[pos]
        }
        if correct {
            outcome = true
            onJudged(true)
        } else if !retrying {
            shaking = true
            shakeTrigger += 1
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
                shaking = false
                placed = Array(repeating: nil, count: positions.count)
                retrying = true
            }
        } else {
            outcome = false
            onJudged(false)
        }
    }

    private func clearBlank(_ blankIndex: Int) {
        guard outcome == nil, !shaking else { return }
        placed[blankIndex] = nil
    }
}
