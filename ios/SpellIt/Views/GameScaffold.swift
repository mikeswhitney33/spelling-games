import SwiftUI

// MARK: - Grade picker

struct GradePicker: View {
    @Binding var grade: GradeBand

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Pick your level")
                .font(.heading(13, weight: .medium))
                .foregroundStyle(Color.mutedInk)
            HStack(spacing: 8) {
                ForEach(GradeBand.allCases) { band in
                    Button {
                        grade = band
                    } label: {
                        Text(band.label)
                            .font(.heading(13, weight: .medium))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(
                                Capsule().fill(grade == band ? Color.ink : Color.white)
                            )
                            .overlay(Capsule().strokeBorder(Color.ink, lineWidth: 2.5))
                            .foregroundStyle(grade == band ? Color.white : Color.ink)
                    }
                    .accessibilityAddTraits(grade == band ? .isSelected : [])
                }
            }
            Text(grade.blurb)
                .font(.system(size: 13))
                .foregroundStyle(Color.mutedInk)
        }
    }
}

// MARK: - Score bar

struct ScoreBar: View {
    var unit = "Word"
    var index: Int
    var total: Int
    var score: Int
    var streak: Int

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text("\(unit) \(min(index + 1, total)) of \(total)")
                Spacer()
                if streak >= 2 {
                    Label("\(streak) in a row!", systemImage: "flame.fill")
                        .foregroundStyle(Color.coral)
                }
                Label("\(score)", systemImage: "star.fill")
                    .foregroundStyle(Color.ink)
                    .symbolRenderingMode(.multicolor)
            }
            .font(.heading(13, weight: .medium))
            .foregroundStyle(Color.mutedInk)
            ProgressView(value: Double(index), total: Double(max(total, 1)))
                .tint(Color.ink)
        }
    }
}

// MARK: - Round summary

struct RoundSummaryView: View {
    var score: Int
    var total: Int
    var bestStreak: Int
    var missed: [WordEntry]
    var summaryText: String?
    var onRestart: () -> Void

    private var stars: Int { RoundEngine.stars(score: score, total: total) }

    private var headline: String {
        switch stars {
        case 3: "Wow! Spelling superstar!"
        case 2: "Nice spelling! One more round?"
        case 1: "Good effort — try for more stars!"
        default: "Keep practicing — you'll get there!"
        }
    }

    var body: some View {
        VStack(spacing: 16) {
            HStack(spacing: 10) {
                ForEach(0..<3, id: \.self) { i in
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.ink)
                            .offset(y: 4)
                        RoundedRectangle(cornerRadius: 14)
                            .fill(i < stars ? Color.sunSoft : Color.secondaryBg)
                            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                        Image(systemName: i < stars ? "star.fill" : "star")
                            .font(.system(size: 26))
                            .foregroundStyle(i < stars ? Color.sun : Color.mutedInk)
                    }
                    .frame(width: 56, height: 56)
                    .rotationEffect(.degrees(i == 0 ? -6 : i == 2 ? 6 : 0))
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(stars) of 3 stars")

            Text(headline)
                .font(.heading(22, weight: .semibold))
                .foregroundStyle(Color.ink)
                .multilineTextAlignment(.center)

            Text(summaryText ?? defaultSummary)
                .font(.system(size: 15))
                .foregroundStyle(Color.mutedInk)
                .multilineTextAlignment(.center)

            if !missed.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Words to practice")
                        .font(.heading(13, weight: .medium))
                        .foregroundStyle(Color.mutedInk)
                    FlowLayout(spacing: 8) {
                        ForEach(missed) { entry in
                            Text(entry.word)
                                .font(.heading(14, weight: .medium))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(RoundedRectangle(cornerRadius: 10).fill(Color.white))
                                .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color.ink, lineWidth: 2))
                        }
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 14).fill(Color.secondaryBg))
            }

            Button("Play again", action: onRestart)
                .buttonStyle(ChunkyButtonStyle())
        }
        .padding(.vertical, 8)
    }

    private var defaultSummary: String {
        var text = "You spelled \(score) of \(total) words right"
        if bestStreak >= 3 { text += " — best streak: \(bestStreak) in a row" }
        return text + "."
    }
}

// MARK: - Feedback panel (correct / reveal)

struct FeedbackPanel: View {
    var correct: Bool
    var word: String
    var isLast: Bool
    var onNext: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            if correct {
                Text("Nailed it! ⭐️")
                    .font(.heading(19, weight: .semibold))
                    .foregroundStyle(Color.ink)
            } else {
                Text("Almost! It's spelled:")
                    .font(.heading(17, weight: .semibold))
                    .foregroundStyle(Color.ink)
                WordTilesView(word: word)
            }
            Button(isLast ? "See my score" : "Next word", action: onNext)
                .buttonStyle(ChunkyButtonStyle())
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(correct ? Color.leafSoft : Color.coralSoft)
        )
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Fitting a word onto one line

/// Fixed tile sizes wrap a word wherever the row happens to run out of room,
/// which leaves orphan letters on the next line and is hard to read. These
/// candidates are offered to `ViewThatFits` largest first: shrink the tiles to
/// keep the whole word on one line, and only once they would be too small for a
/// young reader split into even rows, so "pronunciation" reads as 7 + 6 rather
/// than 9 + 4.
enum TileLadder {
    /// Full-size tile, matching `.md`, down to the smallest a child can still
    /// read comfortably.
    static let sides: [CGFloat] = [48, 40, 34, 28]
    /// Last resort, when even three rows of the smallest readable tile overflow.
    static let hardMinSide: CGFloat = 22

    /// Gaps tighten alongside the tiles so narrow screens buy back some room.
    static func spacing(for side: CGFloat) -> CGFloat { side < 36 ? 4 : 6 }

    /// Even rows: 13 tiles over 2 rows is 7 + 6, never 7 + 6 reordered.
    static func rows(count: Int, over rowCount: Int) -> [Range<Int>] {
        guard count > 0, rowCount > 0 else { return [] }
        let perRow = Int((Double(count) / Double(rowCount)).rounded(.up))
        return stride(from: 0, to: count, by: perRow).map {
            $0..<min($0 + perRow, count)
        }
    }
}

/// A run of tiles sized to fit the width it is given, split into even rows only
/// when the tiles would otherwise be too small to read.
struct TileRow<Tile: View>: View {
    var count: Int
    @ViewBuilder var tile: (Int, TileSize) -> Tile

    var body: some View {
        ViewThatFits(in: .horizontal) {
            candidate(rows: 1, side: TileLadder.sides[0])
            candidate(rows: 1, side: TileLadder.sides[1])
            candidate(rows: 1, side: TileLadder.sides[2])
            candidate(rows: 1, side: TileLadder.sides[3])
            candidate(rows: 2, side: TileLadder.sides[0])
            candidate(rows: 2, side: TileLadder.sides[1])
            candidate(rows: 2, side: TileLadder.sides[2])
            candidate(rows: 2, side: TileLadder.sides[3])
            candidate(rows: 3, side: TileLadder.sides[2])
            candidate(rows: 3, side: TileLadder.hardMinSide)
        }
    }

    @ViewBuilder
    private func candidate(rows rowCount: Int, side: CGFloat) -> some View {
        let spacing = TileLadder.spacing(for: side)
        let size = TileSize(side: side)
        VStack(spacing: spacing) {
            ForEach(TileLadder.rows(count: count, over: rowCount), id: \.lowerBound) { range in
                HStack(spacing: spacing) {
                    ForEach(range, id: \.self) { index in
                        tile(index, size)
                    }
                }
            }
        }
    }
}

/// A word rendered as a row of tiles that fits the space it is given.
struct WordTilesView: View {
    var word: String
    var fill: Color = .white

    var body: some View {
        let letters = Array(word)
        TileRow(count: letters.count) { index, size in
            TileView(letter: String(letters[index]), size: size, fill: fill)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(word.map(String.init).joined(separator: " "))
    }
}

// MARK: - Game scaffold

struct GameScaffold<Content: View, Picker: View>: View {
    var game: Game
    var engine: RoundEngine
    var unit = "Word"
    var summaryText: String? = nil
    var onRestart: () -> Void
    @ViewBuilder var picker: () -> Picker
    @ViewBuilder var content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.ink)
                            .offset(y: 4)
                        RoundedRectangle(cornerRadius: 14)
                            .fill(game.accentSoft)
                            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                        Image(systemName: game.symbol)
                            .font(.system(size: 24))
                            .foregroundStyle(Color.ink)
                    }
                    .frame(width: 54, height: 54)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(game.title)
                            .font(.heading(26, weight: .semibold))
                            .foregroundStyle(Color.ink)
                        Text(game.instructions)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.mutedInk)
                    }
                }

                picker()

                if engine.phase == .playing, !engine.words.isEmpty {
                    ScoreBar(
                        unit: unit,
                        index: engine.index,
                        total: engine.words.count,
                        score: engine.score,
                        streak: engine.streak,
                    )
                }

                VStack {
                    if engine.phase == .done {
                        RoundSummaryView(
                            score: engine.score,
                            total: engine.words.count,
                            bestStreak: engine.bestStreak,
                            missed: engine.missedWords,
                            summaryText: summaryText,
                            onRestart: onRestart,
                        )
                    } else {
                        content()
                    }
                }
                .padding(18)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white)
                        .shadow(color: Color.ink.opacity(0.08), radius: 6, y: 3)
                )
                .overlay(alignment: .top) {
                    UnevenRoundedRectangle(
                        topLeadingRadius: 18,
                        topTrailingRadius: 18,
                    )
                    .fill(game.accent)
                    .frame(height: 7)
                }
            }
            .padding(16)
        }
        .background(Color.paper)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Simple flow (wrap) layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(proposal: proposal, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let arrangement = arrange(proposal: proposal, subviews: subviews)
        for (subview, position) in zip(subviews, arrangement.positions) {
            subview.place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified,
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            totalWidth = max(totalWidth, x - spacing)
        }
        return (CGSize(width: totalWidth, height: y + rowHeight), positions)
    }
}
