import SwiftUI

struct SpotWordView: View {
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
            game: .spotTheWord,
            grade: grade,
            engine: engine,
            onRestart: { engine.start(pool: pool) },
        ) {
            if let entry = engine.current {
                SpotWordChallengeView(
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

struct SpotWordChallengeView: View {
    let entry: WordEntry
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    @State private var options: [String] = []
    @State private var chosen: String?
    @State private var shakeTrigger = 0

    var body: some View {
        VStack(spacing: 14) {
            Text("Clue: \(entry.hint)")
                .font(.system(size: 14))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(options, id: \.self) { option in
                    let isReal = option == entry.word
                    let isPicked = option == chosen
                    let revealed = chosen != nil
                    Button {
                        choose(option)
                    } label: {
                        HStack(spacing: 6) {
                            if revealed, isReal {
                                Image(systemName: "checkmark").foregroundStyle(Color.leaf)
                            }
                            if revealed, isPicked, !isReal {
                                Image(systemName: "xmark").foregroundStyle(Color.coral)
                            }
                            Text(option)
                                .font(.heading(19, weight: .medium))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            ZStack {
                                if !revealed {
                                    RoundedRectangle(cornerRadius: 14)
                                        .fill(Color.ink)
                                        .offset(y: 4)
                                }
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(
                                        revealed && isReal
                                            ? Color.leafSoft
                                            : revealed && isPicked ? Color.coralSoft : Color.white
                                    )
                                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                            }
                        )
                        .foregroundStyle(Color.ink)
                        .opacity(revealed && !isReal && !isPicked ? 0.4 : 1)
                    }
                    .buttonStyle(PressStyle())
                    .disabled(revealed)
                    .modifier(ShakeEffect(animatableData: CGFloat(revealed && isPicked && !isReal ? shakeTrigger : 0)))
                }
            }

            if let chosen {
                FeedbackPanel(
                    correct: chosen == entry.word,
                    word: entry.word,
                    isLast: isLast,
                    onNext: onNext,
                )
            }
        }
        .onAppear {
            guard options.isEmpty else { return }
            options = (Misspell.make(for: entry.word, count: 3) + [entry.word]).shuffled()
        }
    }

    private func choose(_ option: String) {
        guard chosen == nil else { return }
        chosen = option
        if option != entry.word {
            withAnimation { shakeTrigger += 1 }
        }
        onJudged(option == entry.word)
    }
}
