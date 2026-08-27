import Foundation

struct WordSearchPlacement: Identifiable {
    let word: String
    let hint: String
    let row: Int
    let col: Int
    let dRow: Int
    let dCol: Int

    var id: String { word }

    var cells: [GridCell] {
        (0..<word.count).map { i in
            GridCell(row: row + dRow * i, col: col + dCol * i)
        }
    }
}

struct WordSearchPuzzle {
    let grid: [[Character]]
    let placements: [WordSearchPlacement]
    let size: Int
}

/// Port of the site's word-search generator, including the placement-aware
/// safety scan: no blocked word may touch a fill cell, and no stray copy of
/// an answer word may appear off its real placement.
enum WordSearchGenerator {
    struct Config {
        let size: Int
        let count: Int
        let diagonals: Bool
    }

    /// Grid size scales with the pool's word lengths instead of a grade band.
    static func configForPool(_ pool: [WordEntry]) -> Config {
        let lengths = pool.map { $0.word.count }.filter { $0 <= 12 }
        let maxLen = lengths.max() ?? 5
        let size = min(12, max(7, maxLen + 2))
        return Config(size: size, count: size <= 8 ? 5 : 6, diagonals: size >= 10)
    }

    private static let alphabet = Array("abcdefghijklmnopqrstuvwxyz")
    private static let consonants = Array("bcdfghjklmnpqrstvwxz")

    static func generate(pool: [WordEntry]) -> WordSearchPuzzle {
        let config = configForPool(pool)
        let usable = pool.filter { $0.word.count <= config.size }
        let directions: [(Int, Int)] = config.diagonals ? [(0, 1), (1, 0), (1, 1)] : [(0, 1), (1, 0)]

        var best: (placements: [WordSearchPlacement], cells: [GridCell: Character]) = ([], [:])

        for _ in 0..<10 where best.placements.count < config.count {
            let entries = pickRandom(usable, min(usable.count, config.count + 4))
            var cells: [GridCell: Character] = [:]
            var placements: [WordSearchPlacement] = []

            for entry in entries {
                if placements.count >= config.count { break }
                let word = entry.word.lowercased()
                let reversed = String(word.reversed())
                if placements.contains(where: { $0.word == reversed }) { continue }
                let chars = Array(word)

                for _ in 0..<60 {
                    let (dRow, dCol) = directions.randomElement()!
                    let maxRow = config.size - (dRow != 0 ? chars.count : 1)
                    let maxCol = config.size - (dCol != 0 ? chars.count : 1)
                    guard maxRow >= 0, maxCol >= 0 else { break }
                    let row = Int.random(in: 0...maxRow)
                    let col = Int.random(in: 0...maxCol)
                    let candidate = WordSearchPlacement(
                        word: word, hint: entry.hint ?? "", row: row, col: col, dRow: dRow, dCol: dCol,
                    )
                    var ok = true
                    for (i, cell) in candidate.cells.enumerated() {
                        if let existing = cells[cell], existing != chars[i] {
                            ok = false
                            break
                        }
                    }
                    guard ok else { continue }
                    for (i, cell) in candidate.cells.enumerated() { cells[cell] = chars[i] }
                    placements.append(candidate)
                    break
                }
            }
            if placements.count > best.placements.count { best = (placements, cells) }
        }

        let wordLetters = best.placements.flatMap { Array($0.word) }

        func buildGrid(fill: () -> Character) -> [[Character]] {
            (0..<config.size).map { r in
                (0..<config.size).map { c in
                    best.cells[GridCell(row: r, col: c)] ?? fill()
                }
            }
        }

        for _ in 0..<20 {
            let grid = buildGrid {
                Bool.random() && !wordLetters.isEmpty
                    ? wordLetters.randomElement()!
                    : alphabet.randomElement()!
            }
            if !hasProblem(grid: grid, placements: best.placements) {
                return WordSearchPuzzle(grid: grid, placements: best.placements.shuffled(), size: config.size)
            }
        }
        for attempt in 0...20 {
            let grid = buildGrid { alphabet.randomElement()! }
            if attempt == 20 || !hasProblem(grid: grid, placements: best.placements) {
                return WordSearchPuzzle(grid: grid, placements: best.placements.shuffled(), size: config.size)
            }
        }
        fatalError("unreachable")
    }

    private static func lines(size: Int) -> [[GridCell]] {
        var lines: [[GridCell]] = []
        for r in 0..<size { lines.append((0..<size).map { GridCell(row: r, col: $0) }) }
        for c in 0..<size { lines.append((0..<size).map { GridCell(row: $0, col: c) }) }
        for d in (-size + 1)..<size {
            var diag: [GridCell] = []
            var antiDiag: [GridCell] = []
            for r in 0..<size {
                let c = r + d
                if (0..<size).contains(c) { diag.append(GridCell(row: r, col: c)) }
                let ac = size - 1 - r + d
                if (0..<size).contains(ac) { antiDiag.append(GridCell(row: r, col: ac)) }
            }
            if diag.count >= 3 { lines.append(diag) }
            if antiDiag.count >= 3 { lines.append(antiDiag) }
        }
        return lines
    }

    private static func hasProblem(grid: [[Character]], placements: [WordSearchPlacement]) -> Bool {
        var placedCells: Set<GridCell> = []
        var paths: [String: Set<[GridCell]>] = [:]
        for p in placements {
            let cells = p.cells
            placedCells.formUnion(cells)
            paths[p.word, default: []].insert(cells)
            paths[p.word, default: []].insert(cells.reversed())
        }

        for line in lines(size: grid.count) {
            for oriented in [line, line.reversed()] {
                let text = String(oriented.map { grid[$0.row][$0.col] })
                for bad in WordData.blockedWords {
                    var search = text.startIndex
                    while let range = text.range(of: bad, range: search..<text.endIndex) {
                        let start = text.distance(from: text.startIndex, to: range.lowerBound)
                        let window = Array(oriented[start..<(start + bad.count)])
                        if window.contains(where: { !placedCells.contains($0) }) { return true }
                        search = text.index(after: range.lowerBound)
                    }
                }
                for word in paths.keys {
                    var search = text.startIndex
                    while let range = text.range(of: word, range: search..<text.endIndex) {
                        let start = text.distance(from: text.startIndex, to: range.lowerBound)
                        let path = Array(oriented[start..<(start + word.count)])
                        if paths[word]?.contains(path) != true { return true }
                        search = text.index(after: range.lowerBound)
                    }
                }
            }
        }
        return false
    }
}
