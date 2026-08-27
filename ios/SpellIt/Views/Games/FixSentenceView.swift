import SwiftUI

struct FixSentenceView: View {
    @State private var store = BankStore.shared
    @State private var engine = RoundEngine()

    private var pool: [WordEntry] {
        store.activeBank.entries.filter { $0.sentence != nil }
    }

    private func startRound() {
        if pool.count >= 4 {
            engine.start(pool: pool)
        }
    }

    var body: some View {
        GameScaffold(
            game: .fixTheSentence,
            engine: engine,
            onRestart: { startRound() },
        ) {
            BankPickerView()
        } content: {
            if pool.count < 4 {
                NotEnoughWordsView(need: 4, requirement: "words with sentences")
            } else if let entry = engine.current {
                FixSentenceWordView(
                    entry: entry,
                    isLast: engine.isLastWord,
                    onJudged: { engine.record(correct: $0) },
                    onNext: { engine.advance() },
                )
                .id("\(engine.roundId)-\(engine.index)")
            }
        }
        .onAppear { if engine.words.isEmpty { startRound() } }
        .onChange(of: store.activeId) { startRound() }
    }
}

struct SentenceToken: Identifiable {
    let id: Int
    let prefix: String
    let core: String
    let suffix: String
    let isTarget: Bool
    /// The core as displayed — the planted misspelling for the target.
    var shown: String
}

func tokenizeSentence(_ sentence: String, target: String) -> [SentenceToken] {
    var targetFound = false
    return sentence.split(separator: " ").enumerated().map { index, raw in
        let text = String(raw)
        let isWordChar: (Character) -> Bool = { $0.isLetter || $0 == "'" }
        let prefix = String(text.prefix { !isWordChar($0) })
        let suffix = String(text.reversed().prefix { !isWordChar($0) }.reversed())
        let core = String(text.dropFirst(prefix.count).dropLast(suffix.count))
        let isTarget = !targetFound && core.lowercased() == target.lowercased()
        if isTarget { targetFound = true }
        return SentenceToken(
            id: index, prefix: prefix, core: core, suffix: suffix,
            isTarget: isTarget, shown: core,
        )
    }
}

struct FixSentenceWordView: View {
    let entry: WordEntry
    let isLast: Bool
    let onJudged: (Bool) -> Void
    let onNext: () -> Void

    private enum Stage {
        case find
        case fix
    }

    @State private var tokens: [SentenceToken] = []
    @State private var stage = Stage.find
    @State private var typed = ""
    @State private var outcome: Bool?
    @State private var retrying = false
    @State private var findMisses = 0
    @State private var lastMissWord: String?
    @State private var shakeTrigger = 0
    @FocusState private var inputFocused: Bool

    var body: some View {
        VStack(spacing: 14) {
            FlowLayout(spacing: 4) {
                ForEach(tokens) { token in
                    tokenView(token)
                }
            }

            if stage == .find {
                Text(findStatus)
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.mutedInk)
                    .multilineTextAlignment(.center)
            }

            if stage == .fix, outcome == nil {
                Text("You found it! Now type it the right way.")
                    .font(.heading(14, weight: .medium))
                    .foregroundStyle(Color.mutedInk)
                SpellingField(
                    placeholder: "Type the fix…",
                    text: $typed,
                    onSubmit: submit,
                    focused: $inputFocused,
                )
                if retrying {
                    Text("Not quite — look at the clue and try again!")
                        .font(.heading(14, weight: .medium))
                        .foregroundStyle(Color.coral)
                }
                if let hint = entry.hint {
                    Text("Clue: \(hint)")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.mutedInk)
                        .multilineTextAlignment(.center)
                }
                Button("Fix it", action: submit)
                    .buttonStyle(ChunkyButtonStyle())
                    .disabled(typed.trimmingCharacters(in: .whitespaces).isEmpty)
            }

            if let outcome {
                FeedbackPanel(correct: outcome, word: entry.word, isLast: isLast, onNext: onNext)
            }
        }
        .onAppear(perform: setup)
    }

    private var findStatus: String {
        if findMisses == 0 { return "Tap the word that's spelled wrong." }
        let missed = lastMissWord.map { "\"\($0)\" is spelled fine" } ?? "That one's spelled fine"
        if findMisses == 1 { return "\(missed) — keep hunting!" }
        let hint = tokens.first(where: \.isTarget)?.shown.first.map(String.init) ?? "?"
        return "\(missed). Psst — the wrong word starts with \"\(hint)\"."
    }

    @ViewBuilder
    private func tokenView(_ token: SentenceToken) -> some View {
        let highlighted = token.isTarget && findMisses >= 2 && stage == .find
        let struck = token.isTarget && stage == .fix && outcome == nil
        let fixed = token.isTarget && outcome != nil
        let text = fixed ? matchCase(model: token.shown, text: entry.word.lowercased()) : token.shown

        Button {
            tap(token)
        } label: {
            Text(token.prefix + text + token.suffix)
                .font(.system(size: 19, weight: token.isTarget && stage != .find ? .semibold : .regular))
                .strikethrough(struck, color: .coral)
                .padding(.horizontal, 4)
                .padding(.vertical, 3)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(
                            fixed ? Color.leafSoft
                                : struck ? Color.coralSoft
                                : highlighted ? Color.skySoft
                                : Color.clear
                        )
                )
                .foregroundStyle(Color.ink)
        }
        .disabled(stage != .find || outcome != nil)
        .modifier(
            ShakeEffect(animatableData: CGFloat(lastMissWord == token.shown ? shakeTrigger : 0))
        )
    }

    private func setup() {
        guard tokens.isEmpty else { return }
        var parsed = tokenizeSentence(entry.sentence ?? "", target: entry.word)
        if let targetIndex = parsed.firstIndex(where: \.isTarget) {
            let fake = Misspell.make(for: entry.word, count: 1).first
                ?? entry.word.lowercased() + String(entry.word.lowercased().last!)
            parsed[targetIndex].shown = matchCase(model: parsed[targetIndex].core, text: fake)
            tokens = parsed
        } else {
            // Data safety net: sentence lacks its own word — skip with credit.
            tokens = parsed
            onJudged(true)
            onNext()
        }
    }

    private func tap(_ token: SentenceToken) {
        guard stage == .find else { return }
        if token.isTarget {
            stage = .fix
            inputFocused = true
        } else {
            findMisses += 1
            lastMissWord = token.shown
            withAnimation { shakeTrigger += 1 }
        }
    }

    private func submit() {
        guard outcome == nil, stage == .fix else { return }
        let attempt = typed.trimmingCharacters(in: .whitespaces).lowercased()
        guard !attempt.isEmpty else { return }
        if attempt == entry.word.lowercased() {
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
