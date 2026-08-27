import SwiftUI

struct MiniCrosswordView: View {
    @AppStorage("spellit.grade") private var gradeRaw = GradeBand.g23.rawValue

    private var grade: Binding<GradeBand> {
        Binding(
            get: { GradeBand(rawValue: gradeRaw) ?? .g23 },
            set: { gradeRaw = $0.rawValue },
        )
    }

    @State private var puzzle: CrosswordPuzzle?
    @State private var letters: [GridCell: Character] = [:]
    @State private var solved: Set<Int> = []
    /// First-fill correctness per placement index (nil until attempted).
    @State private var results: [Int: Bool] = [:]
    @State private var selected = 0
    @State private var activeCell: GridCell?
    @State private var shakeTrigger = 0
    @State private var shakingIndex: Int?
    @State private var shakeTask: Task<Void, Never>?
    @FocusState private var keyboardFocused: Bool
    @State private var keyboardBuffer = " "
    /// Set right before programmatic buffer writes so onChange can tell them
    /// apart from real typing.
    @State private var programmaticBuffer: String?

    private var finished: Bool {
        guard let puzzle else { return false }
        return solved.count == puzzle.placements.count
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header
                GradePicker(grade: grade)

                VStack(spacing: 14) {
                    if let puzzle {
                        if finished {
                            RoundSummaryView(
                                score: results.values.filter { $0 }.count,
                                total: puzzle.placements.count,
                                bestStreak: 0,
                                missed: puzzle.placements.enumerated()
                                    .filter { results[$0.offset] == false }
                                    .map { WordEntry(word: $0.element.word, hint: $0.element.hint, sentence: "") },
                                summaryText: nil,
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
        .safeAreaInset(edge: .bottom) {
            // Pinned above the keyboard so the active clue is always readable
            // while typing — no scrolling back and forth.
            if let puzzle, !finished {
                clueBar(puzzle)
            }
        }
        .onAppear { if puzzle == nil { startRound() } }
        .onChange(of: gradeRaw) { startRound() }
        .onDisappear { shakeTask?.cancel() }
    }

    // MARK: Active clue bar

    /// Clue order for the prev/next chevrons: across by number, then down.
    private func orderedClueIndices(_ puzzle: CrosswordPuzzle) -> [Int] {
        let indexed = puzzle.placements.enumerated()
        let across = indexed.filter { $0.element.dir == .across }.sorted { $0.element.number < $1.element.number }
        let down = indexed.filter { $0.element.dir == .down }.sorted { $0.element.number < $1.element.number }
        return (across + down).map(\.offset)
    }

    /// The next clue after `index` in cycle order, preferring unsolved ones.
    private func nextClueIndex(after index: Int, step: Int, in puzzle: CrosswordPuzzle) -> Int {
        let order = orderedClueIndices(puzzle)
        guard let at = order.firstIndex(of: index), order.count > 1 else { return index }
        for offset in 1..<order.count {
            let wrapped = ((at + step * offset) % order.count + order.count) % order.count
            let candidate = order[wrapped]
            if !solved.contains(candidate) { return candidate }
        }
        return index
    }

    private func jumpToClue(_ index: Int, in puzzle: CrosswordPuzzle) {
        selectClue(index, in: puzzle)
    }

    private func clueBar(_ puzzle: CrosswordPuzzle) -> some View {
        let placement = puzzle.placements[safe: selected]
        let crossing = activeCell.map { cellInfo($0, in: puzzle).indices.count > 1 } ?? false
        return HStack(spacing: 10) {
            Button {
                jumpToClue(nextClueIndex(after: selected, step: -1, in: puzzle), in: puzzle)
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.ink)
                    .frame(width: 36, height: 44)
            }
            .accessibilityLabel("Previous clue")

            VStack(spacing: 2) {
                if let placement {
                    Text("\(placement.number) \(placement.dir == .across ? "ACROSS" : "DOWN") · \(placement.word.count) letters")
                        .font(.heading(11, weight: .semibold))
                        .foregroundStyle(Color.mutedInk)
                    Text(placement.hint)
                        .font(.heading(14, weight: .medium))
                        .foregroundStyle(Color.ink)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)
                }
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
            .onTapGesture {
                // Tapping the clue re-focuses the word's first empty cell.
                jumpToClue(selected, in: puzzle)
            }

            if crossing {
                Button {
                    if let cell = activeCell { tapCell(cell, in: puzzle) }
                } label: {
                    Image(systemName: "arrow.triangle.swap")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Color.ink)
                        .frame(width: 36, height: 44)
                }
                .accessibilityLabel("Switch to the crossing clue")
            }

            Button {
                jumpToClue(nextClueIndex(after: selected, step: 1, in: puzzle), in: puzzle)
            } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Color.ink)
                    .frame(width: 36, height: 44)
            }
            .accessibilityLabel("Next clue")
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.skySoft)
                .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Color.ink, lineWidth: 2.5))
        )
        .padding(.horizontal, 12)
        .padding(.bottom, 6)
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14).fill(Color.ink).offset(y: 4)
                RoundedRectangle(cornerRadius: 14)
                    .fill(Game.miniCrossword.accentSoft)
                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.ink, lineWidth: 3))
                Image(systemName: Game.miniCrossword.symbol)
                    .font(.system(size: 24))
                    .foregroundStyle(Color.ink)
            }
            .frame(width: 54, height: 54)
            VStack(alignment: .leading, spacing: 2) {
                Text(Game.miniCrossword.title)
                    .font(.heading(26, weight: .semibold))
                    .foregroundStyle(Color.ink)
                Text(Game.miniCrossword.instructions)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.mutedInk)
            }
        }
    }

    // MARK: Board

    private func cellInfo(_ cell: GridCell, in puzzle: CrosswordPuzzle) -> (indices: [Int], number: Int?) {
        var indices: [Int] = []
        var number: Int?
        for (i, p) in puzzle.placements.enumerated() where p.cells.contains(cell) {
            indices.append(i)
            if p.cells.first == cell { number = min(number ?? p.number, p.number) }
        }
        return (indices, number)
    }

    private func isLocked(_ cell: GridCell, in puzzle: CrosswordPuzzle) -> Bool {
        cellInfo(cell, in: puzzle).indices.contains { solved.contains($0) }
    }

    private func boardView(_ puzzle: CrosswordPuzzle) -> some View {
        let selectedCells = Set(puzzle.placements[safe: selected]?.cells ?? [])
        let shakeCells: Set<GridCell> = shakingIndex.flatMap { idx in
            puzzle.placements[safe: idx].map { Set($0.cells) }
        } ?? []

        return VStack(spacing: 14) {
            // Hidden field that drives the system keyboard for cell entry.
            TextField("", text: $keyboardBuffer)
                .focused($keyboardFocused)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.asciiCapable)
                .frame(width: 1, height: 1)
                .opacity(0.02)
                .onChange(of: keyboardBuffer) { _, newValue in
                    handleTyped(newValue, in: puzzle)
                }

            GeometryReader { geo in
                let side = geo.size.width / CGFloat(max(puzzle.width, 1))
                VStack(spacing: 0) {
                    ForEach(0..<puzzle.height, id: \.self) { r in
                        HStack(spacing: 0) {
                            ForEach(0..<puzzle.width, id: \.self) { c in
                                let cell = GridCell(row: r, col: c)
                                let info = cellInfo(cell, in: puzzle)
                                Group {
                                    if info.indices.isEmpty {
                                        Color.clear
                                    } else {
                                        crosswordCell(
                                            cell,
                                            info: info,
                                            locked: isLocked(cell, in: puzzle),
                                            selected: selectedCells.contains(cell),
                                            shaking: shakeCells.contains(cell),
                                            in: puzzle,
                                        )
                                    }
                                }
                                .frame(width: side, height: side)
                            }
                        }
                    }
                }
                .modifier(ShakeEffect(animatableData: CGFloat(shakeTrigger)))
            }
            .aspectRatio(
                CGFloat(max(puzzle.width, 1)) / CGFloat(max(puzzle.height, 1)),
                contentMode: .fit,
            )

            cluesView(puzzle)
        }
    }

    private func crosswordCell(
        _ cell: GridCell,
        info: (indices: [Int], number: Int?),
        locked: Bool,
        selected isSelected: Bool,
        shaking: Bool,
        in puzzle: CrosswordPuzzle,
    ) -> some View {
        Button {
            tapCell(cell, in: puzzle)
        } label: {
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 5)
                    .fill(
                        shaking ? Color.coralSoft
                            : locked ? Color.leafSoft
                            : isSelected ? Color.skySoft
                            : Color.white
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 5)
                            .strokeBorder(
                                activeCell == cell && !locked ? Color.sky : Color.ink,
                                lineWidth: activeCell == cell && !locked ? 2.5 : 1.5,
                            )
                    )
                    .padding(1)
                if let number = info.number {
                    Text("\(number)")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(Color.mutedInk)
                        .padding(.top, 2)
                        .padding(.leading, 3)
                }
                Text((letters[cell].map(String.init) ?? "").uppercased())
                    .font(.heading(16, weight: .semibold))
                    .foregroundStyle(Color.ink)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
        }
        .accessibilityLabel(cellAccessibilityLabel(cell, info: info, in: puzzle))
    }

    private func cellAccessibilityLabel(
        _ cell: GridCell,
        info: (indices: [Int], number: Int?),
        in puzzle: CrosswordPuzzle,
    ) -> String {
        let primary = info.indices.first { $0 == selected }
            ?? info.indices.first { puzzle.placements[$0].dir == .across }
            ?? info.indices.first
        guard let primary, let p = puzzle.placements[safe: primary] else { return "empty" }
        let position = (p.cells.firstIndex(of: cell) ?? 0) + 1
        let dir = p.dir == .across ? "across" : "down"
        return "Clue \(p.number) \(dir), letter \(position) of \(p.word.count). \(p.hint)"
    }

    private func cluesView(_ puzzle: CrosswordPuzzle) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach([CrosswordDirection.across, .down], id: \.self) { dir in
                let clues = puzzle.placements.enumerated()
                    .filter { $0.element.dir == dir }
                    .sorted { $0.element.number < $1.element.number }
                if !clues.isEmpty {
                    Text(dir == .across ? "ACROSS" : "DOWN")
                        .font(.heading(12, weight: .semibold))
                        .foregroundStyle(Color.mutedInk)
                    ForEach(clues, id: \.offset) { index, p in
                        Button {
                            selectClue(index, in: puzzle)
                        } label: {
                            HStack(alignment: .firstTextBaseline, spacing: 6) {
                                Text("\(p.number).")
                                    .font(.heading(14, weight: .semibold))
                                Text("\(p.hint) (\(p.word.count) letters)")
                                    .font(.system(size: 14))
                                    .multilineTextAlignment(.leading)
                                Spacer(minLength: 0)
                                if solved.contains(index) {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 12))
                                        .foregroundStyle(Color.leaf)
                                }
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(selected == index && !solved.contains(index) ? Color.skySoft : Color.clear)
                            )
                            .foregroundStyle(solved.contains(index) ? Color.mutedInk : Color.ink)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Interaction

    private func startRound() {
        shakeTask?.cancel()
        puzzle = CrosswordGenerator.generate(pool: WordData.words[grade.wrappedValue] ?? [])
        letters = [:]
        solved = []
        results = [:]
        selected = 0
        activeCell = puzzle?.placements.first?.cells.first
        shakingIndex = nil
        resetBuffer()
    }

    private func tapCell(_ cell: GridCell, in puzzle: CrosswordPuzzle) {
        let info = cellInfo(cell, in: puzzle)
        guard !info.indices.isEmpty else { return }
        if activeCell == cell, info.indices.count > 1 {
            // Re-tapping a crossing flips to the other word.
            if let other = info.indices.first(where: { $0 != selected }) {
                selected = other
            }
        } else if !info.indices.contains(selected) {
            selected = info.indices.first { puzzle.placements[$0].dir == .across } ?? info.indices[0]
        }
        activeCell = cell
        keyboardFocused = true
    }

    private func selectClue(_ index: Int, in puzzle: CrosswordPuzzle) {
        selected = index
        let cells = puzzle.placements[index].cells
        activeCell = cells.first { letters[$0] == nil && !isLocked($0, in: puzzle) } ?? cells.first
        keyboardFocused = true
    }

    /// Reset the hidden field to a single sentinel space, so a real backspace
    /// (which deletes the space) is observable as a change to "".
    private func resetBuffer() {
        programmaticBuffer = " "
        keyboardBuffer = " "
    }

    private func handleTyped(_ value: String, in puzzle: CrosswordPuzzle) {
        if value == programmaticBuffer {
            programmaticBuffer = nil
            return
        }
        defer { resetBuffer() }
        guard let cell = activeCell else { return }
        if value.isEmpty {
            // The sentinel space was deleted: a real backspace.
            if !isLocked(cell, in: puzzle), letters[cell] != nil {
                letters[cell] = nil
            } else {
                moveActive(from: cell, delta: -1, in: puzzle)
                if let prev = activeCell, !isLocked(prev, in: puzzle) {
                    letters[prev] = nil
                }
            }
            return
        }
        guard let ch = value.lowercased().last, ch.isLetter else { return }
        if !isLocked(cell, in: puzzle) {
            letters[cell] = ch
            checkWords(changed: cell, in: puzzle)
        }
        moveActive(from: cell, delta: 1, in: puzzle)
    }

    private func moveActive(from cell: GridCell, delta: Int, in puzzle: CrosswordPuzzle) {
        guard let placement = puzzle.placements[safe: selected] else { return }
        let cells = placement.cells
        guard var at = cells.firstIndex(of: cell) else { return }
        at += delta
        while cells.indices.contains(at) {
            if !isLocked(cells[at], in: puzzle) {
                activeCell = cells[at]
                return
            }
            at += delta
        }
    }

    private func checkWords(changed: GridCell, in puzzle: CrosswordPuzzle) {
        for (index, p) in puzzle.placements.enumerated() where !solved.contains(index) {
            let cells = p.cells
            let filled = cells.compactMap { letters[$0] }
            guard filled.count == cells.count else { continue }
            let attempt = String(filled)
            if attempt == p.word.lowercased() {
                solved.insert(index)
                if results[index] == nil { results[index] = true }
                if solved.count == puzzle.placements.count {
                    keyboardFocused = false
                } else if index == selected {
                    // Let the green lock land, then hop to the next unsolved clue.
                    let next = nextClueIndex(after: index, step: 1, in: puzzle)
                    Task {
                        try? await Task.sleep(for: .seconds(0.6))
                        guard !Task.isCancelled, solved.contains(index), selected == index else { return }
                        jumpToClue(next, in: puzzle)
                    }
                }
            } else if index == selected, cells.contains(changed), results[index] == nil {
                results[index] = false
                shakingIndex = index
                withAnimation { shakeTrigger += 1 }
                shakeTask?.cancel()
                shakeTask = Task {
                    try? await Task.sleep(for: .seconds(0.5))
                    guard !Task.isCancelled else { return }
                    shakingIndex = nil
                }
            }
        }
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
