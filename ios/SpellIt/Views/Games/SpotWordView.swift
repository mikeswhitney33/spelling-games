import SwiftUI

struct SpotWordView: View {
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
            game: .spotTheWord,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words")
            } else if let entry = engine.current {
                SpotWordChallengeView(
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
            if let hint = entry.hint {
                Text("Clue: \(hint)")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.mutedInk)
                    .multilineTextAlignment(.center)
            }

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
