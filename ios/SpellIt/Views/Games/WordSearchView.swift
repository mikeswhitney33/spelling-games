import SwiftUI

struct WordSearchView: View {
    @State private var store = BankStore.shared

    private var pool: [WordEntry] {
        store.activeBank.entries.filter { $0.word.count >= 3 && $0.word.count <= 12 }
    }

    @State private var puzzle: WordSearchPuzzle?
    @State private var found: Set<String> = []
    @State private var firstTap: GridCell?
    @State private var flashCells: Set<GridCell> = []
    @State private var flashTask: Task<Void, Never>?

    private var finished: Bool {
        guard let puzzle else { return false }
        return found.count == puzzle.placements.count
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header
                BankPickerView()

                VStack(spacing: 14) {
                    if pool.count < 5 {
                        NotEnoughWordsView(need: 5, requirement: "words of 3\u{2013}12 letters")
                    } else if let puzzle {
                        if finished {
                            RoundSummaryView(
                                score: puzzle.placements.count,
                                total: puzzle.placements.count,
                                bestStreak: 0,
                                missed: [],
                                summaryText: "You found every hidden word — great hunting!",
                                onRestart: startRound,
                            )
                        } else {
                            boardView(puzzle)
                        }
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white)
                        .shadow(color: Color.ink.opacity(0.08), radius: 6, y: 3)
                )
            }
            .padding(16)
        }
        .background(Color.paper)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if puzzle == nil { startRound() } }
        .onChange(of: store.activeId) { startRound() }
        .onDisappear { flashTask?.cancel() }
    }

    private var header: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14).fill(Color.ink).offset(y: 4)
                RoundedRectangle(cornerRadius: 14)
                    .fill(Game.wordSearch.accentSoft)
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                Image(systemName: Game.wordSearch.symbol)
                    .font(.system(size: 24))
                    .foregroundStyle(Color.ink)
            }
            .frame(width: 54, height: 54)
            VStack(alignment: .leading, spacing: 2) {
                Text(Game.wordSearch.title)
                    .font(.heading(26, weight: .semibold))
                    .foregroundStyle(Color.ink)
                Text(Game.wordSearch.instructions)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.mutedInk)
            }
        }
    }

    private var foundCells: Set<GridCell> {
        guard let puzzle else { return [] }
        return Set(puzzle.placements.filter { found.contains($0.word) }.flatMap(\.cells))
    }

    private func boardView(_ puzzle: WordSearchPuzzle) -> some View {
        VStack(spacing: 12) {
            GeometryReader { geo in
                let cellSide = geo.size.width / CGFloat(puzzle.size)
                VStack(spacing: 0) {
                    ForEach(0..<puzzle.size, id: \.self) { r in
                        HStack(spacing: 0) {
                            ForEach(0..<puzzle.size, id: \.self) { c in
                                let cell = GridCell(row: r, col: c)
                                cellView(letter: puzzle.grid[r][c], cell: cell)
                                    .frame(width: cellSide, height: cellSide)
                            }
                        }
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)

            Text(firstTap == nil ? "Tap the FIRST letter of a word you spot." : "Now tap the LAST letter of that word.")
                .font(.heading(13, weight: .medium))
                .foregroundStyle(Color.mutedInk)

            FlowLayout(spacing: 8) {
                ForEach(puzzle.placements) { p in
                    HStack(spacing: 4) {
                        if found.contains(p.word) {
                            Image(systemName: "checkmark").font(.system(size: 11)).foregroundStyle(Color.leaf)
                        }
                        Text(p.word)
                            .font(.heading(14, weight: .medium))
                            .strikethrough(found.contains(p.word))
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(found.contains(p.word) ? Color.leafSoft : Color.white)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .strokeBorder(found.contains(p.word) ? Color.leaf : Color.ink, lineWidth: 2)
                    )
                    .foregroundStyle(found.contains(p.word) ? Color.mutedInk : Color.ink)
                }
            }
        }
    }

    private func cellView(letter: Character, cell: GridCell) -> some View {
        let isFound = foundCells.contains(cell)
        let isFirst = firstTap == cell
        let isFlash = flashCells.contains(cell)
        return Button {
            tap(cell)
        } label: {
            Text(String(letter).uppercased())
                .font(.heading(15, weight: .medium))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 6)
                        .fill(isFlash ? Color.coralSoft : isFirst ? Color.skySoft : isFound ? Color.leafSoft : Color.clear)
                        .padding(1)
                )
                .foregroundStyle(Color.ink)
        }
        .accessibilityLabel("\(String(letter)), row \(cell.row + 1), column \(cell.col + 1)")
    }

    private func startRound() {
        flashTask?.cancel()
        guard pool.count >= 5 else {
            puzzle = nil
            return
        }
        puzzle = WordSearchGenerator.generate(pool: pool)
        found = []
        firstTap = nil
        flashCells = []
    }

    private func tap(_ cell: GridCell) {
        guard let puzzle else { return }
        guard let start = firstTap else {
            firstTap = cell
            return
        }
        if start == cell {
            firstTap = nil
            return
        }
        firstTap = nil
        guard let line = lineBetween(start, cell) else {
            flash([start, cell])
            return
        }
        if let hit = puzzle.placements.first(where: { $0.cells == line || $0.cells == line.reversed() }) {
            if !found.contains(hit.word) {
                found.insert(hit.word)
            }
        } else {
            flash(line)
        }
    }

    private func lineBetween(_ a: GridCell, _ b: GridCell) -> [GridCell]? {
        let dr = (b.row - a.row).signum()
        let dc = (b.col - a.col).signum()
        let steps = max(abs(b.row - a.row), abs(b.col - a.col))
        if dr != 0, dc != 0, abs(b.row - a.row) != abs(b.col - a.col) { return nil }
        return (0...steps).map { GridCell(row: a.row + dr * $0, col: a.col + dc * $0) }
    }

    private func flash(_ cells: [GridCell]) {
        flashTask?.cancel()
        flashCells = Set(cells)
        flashTask = Task {
            try? await Task.sleep(for: .seconds(0.5))
            guard !Task.isCancelled else { return }
            flashCells = []
        }
    }
}
