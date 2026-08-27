import Foundation

enum CrosswordDirection {
    case across
    case down
}

struct CrosswordPlacement: Identifiable {
    let word: String
    let hint: String
    var row: Int
    var col: Int
    let dir: CrosswordDirection
    var number = 0

    var id: String { "\(word)-\(row)-\(col)" }

    var cells: [GridCell] {
        (0..<word.count).map { i in
            GridCell(
                row: dir == .down ? row + i : row,
                col: dir == .across ? col + i : col,
            )
        }
    }
}

struct GridCell: Hashable {
    let row: Int
    let col: Int
}

struct CrosswordPuzzle {
    var placements: [CrosswordPlacement]
    var width: Int
    var height: Int
}

/// Port of the site's greedy crossword generator: perpendicular crossings
/// only, standard adjacency rules, compactness-scored retries.
enum CrosswordGenerator {
    static let maxWordLength = 9

    static func generate(pool: [WordEntry], target: Int = 5) -> CrosswordPuzzle {
        let usable = pool.filter { $0.word.count <= maxWordLength }
        var best: [CrosswordPlacement] = []
        var bestArea = Int.max
        let cozyArea = 120

        for _ in 0..<30 {
            let words = pickRandom(usable, min(usable.count, 12))
            guard let first = words.first else { break }
            var grid: [GridCell: Character] = [:]
            var dirs: [GridCell: Set<Bool>] = [:] // true = across
            var placed: [CrosswordPlacement] = []

            func setWord(_ entry: WordEntry, _ row: Int, _ col: Int, _ dir: CrosswordDirection) {
                let placement = CrosswordPlacement(word: entry.word, hint: entry.hint, row: row, col: col, dir: dir)
                let chars = Array(entry.word)
                for (i, cell) in placement.cells.enumerated() {
                    // Store lowercase so capitalized entries ("February") still
                    // cross lowercase words sharing the letter.
                    grid[cell] = Character(chars[i].lowercased())
                    dirs[cell, default: []].insert(dir == .across)
                }
                placed.append(placement)
            }

            func canPlace(_ word: [Character], _ row: Int, _ col: Int, _ dir: CrosswordDirection) -> Bool {
                let dr = dir == .down ? 1 : 0
                let dc = dir == .across ? 1 : 0
                if grid[GridCell(row: row - dr, col: col - dc)] != nil { return false }
                if grid[GridCell(row: row + dr * word.count, col: col + dc * word.count)] != nil { return false }
                var crossings = 0
                for i in word.indices {
                    let cell = GridCell(row: row + dr * i, col: col + dc * i)
                    if let existing = grid[cell] {
                        if existing != Character(word[i].lowercased()) { return false }
                        if dirs[cell]?.contains(dir == .across) == true { return false }
                        crossings += 1
                    } else {
                        if grid[GridCell(row: cell.row + dc, col: cell.col + dr)] != nil { return false }
                        if grid[GridCell(row: cell.row - dc, col: cell.col - dr)] != nil { return false }
                    }
                }
                return crossings > 0
            }

            setWord(first, 0, 0, .across)
            for entry in words.dropFirst() {
                if placed.count >= target { break }
                let wordChars = Array(entry.word)
                var options: [(Int, Int, CrosswordDirection)] = []
                for p in placed {
                    let pChars = Array(p.word)
                    for i in pChars.indices {
                        for j in wordChars.indices where pChars[i].lowercased() == wordChars[j].lowercased() {
                            let option: (Int, Int, CrosswordDirection) =
                                p.dir == .across
                                    ? (p.row - j, p.col + i, .down)
                                    : (p.row + i, p.col - j, .across)
                            if canPlace(wordChars, option.0, option.1, option.2) {
                                options.append(option)
                            }
                        }
                    }
                }
                if !options.isEmpty {
                    // Prefer compact placements.
                    func area(_ o: (Int, Int, CrosswordDirection)) -> Int {
                        var rows = placed.flatMap { [$0.row, $0.dir == .down ? $0.row + $0.word.count - 1 : $0.row] }
                        var cols = placed.flatMap { [$0.col, $0.dir == .across ? $0.col + $0.word.count - 1 : $0.col] }
                        rows += [o.0, o.2 == .down ? o.0 + wordChars.count - 1 : o.0]
                        cols += [o.1, o.2 == .across ? o.1 + wordChars.count - 1 : o.1]
                        return (rows.max()! - rows.min()! + 1) * (cols.max()! - cols.min()! + 1)
                    }
                    let sorted = options.sorted { area($0) < area($1) }
                    let pick = sorted.prefix(3).randomElement()!
                    setWord(entry, pick.0, pick.1, pick.2)
                }
            }

            let rows = placed.flatMap { [$0.row, $0.dir == .down ? $0.row + $0.word.count - 1 : $0.row] }
            let cols = placed.flatMap { [$0.col, $0.dir == .across ? $0.col + $0.word.count - 1 : $0.col] }
            let attemptArea = (rows.max()! - rows.min()! + 1) * (cols.max()! - cols.min()! + 1)
            if placed.count > best.count || (placed.count == best.count && attemptArea < bestArea) {
                best = placed
                bestArea = attemptArea
            }
            if best.count >= target, bestArea <= cozyArea { break }
        }

        // Normalize to 0-based coordinates and assign clue numbers.
        let minRow = best.map(\.row).min() ?? 0
        let minCol = best.map(\.col).min() ?? 0
        var normalized = best.map { p in
            var copy = p
            copy.row -= minRow
            copy.col -= minCol
            return copy
        }
        let height = (normalized.map { $0.dir == .down ? $0.row + $0.word.count - 1 : $0.row }.max() ?? 0) + 1
        let width = (normalized.map { $0.dir == .across ? $0.col + $0.word.count - 1 : $0.col }.max() ?? 0) + 1

        let starts = Set(normalized.map { GridCell(row: $0.row, col: $0.col) })
            .sorted { ($0.row, $0.col) < ($1.row, $1.col) }
        let numberByCell = Dictionary(
            uniqueKeysWithValues: starts.enumerated().map { ($0.element, $0.offset + 1) }
        )
        for i in normalized.indices {
            normalized[i].number = numberByCell[GridCell(row: normalized[i].row, col: normalized[i].col)] ?? 0
        }

        return CrosswordPuzzle(placements: normalized, width: width, height: height)
    }
}
