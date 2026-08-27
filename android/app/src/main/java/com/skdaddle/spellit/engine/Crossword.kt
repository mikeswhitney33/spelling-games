package com.skdaddle.spellit.engine

import com.skdaddle.spellit.model.WordEntry

enum class CrosswordDirection { ACROSS, DOWN }

data class GridCell(val row: Int, val col: Int)

data class CrosswordPlacement(
    val word: String,
    val hint: String,
    val row: Int,
    val col: Int,
    val dir: CrosswordDirection,
    val number: Int = 0,
) {
    val id: String get() = "$word-$row-$col"

    val cells: List<GridCell>
        get() = (0 until word.length).map { i ->
            GridCell(
                row = if (dir == CrosswordDirection.DOWN) row + i else row,
                col = if (dir == CrosswordDirection.ACROSS) col + i else col,
            )
        }
}

data class CrosswordPuzzle(
    val placements: List<CrosswordPlacement>,
    val width: Int,
    val height: Int,
)

/**
 * Port of the site's greedy crossword generator: perpendicular crossings
 * only, standard adjacency rules, compactness-scored retries.
 */
object CrosswordGenerator {
    const val MAX_WORD_LENGTH = 9

    fun generate(pool: List<WordEntry>, target: Int = 5): CrosswordPuzzle {
        val usable = pool.filter { it.word.length <= MAX_WORD_LENGTH }
        var best: List<CrosswordPlacement> = emptyList()
        var bestArea = Int.MAX_VALUE
        val cozyArea = 120

        for (attempt in 0 until 30) {
            val words = pickRandom(usable, minOf(usable.size, 12))
            val first = words.firstOrNull() ?: break
            val grid = mutableMapOf<GridCell, Char>()
            val dirs = mutableMapOf<GridCell, MutableSet<Boolean>>() // true = across
            val placed = mutableListOf<CrosswordPlacement>()

            fun setWord(entry: WordEntry, row: Int, col: Int, dir: CrosswordDirection) {
                val placement = CrosswordPlacement(
                    word = entry.word, hint = entry.hint ?: "", row = row, col = col, dir = dir,
                )
                val chars = entry.word.toCharArray()
                placement.cells.forEachIndexed { i, cell ->
                    // Store lowercase so capitalized entries ("February") still
                    // cross lowercase words sharing the letter.
                    grid[cell] = chars[i].lowercaseChar()
                    dirs.getOrPut(cell) { mutableSetOf() }.add(dir == CrosswordDirection.ACROSS)
                }
                placed.add(placement)
            }

            fun canPlace(word: CharArray, row: Int, col: Int, dir: CrosswordDirection): Boolean {
                val dr = if (dir == CrosswordDirection.DOWN) 1 else 0
                val dc = if (dir == CrosswordDirection.ACROSS) 1 else 0
                if (grid.containsKey(GridCell(row - dr, col - dc))) return false
                if (grid.containsKey(GridCell(row + dr * word.size, col + dc * word.size))) return false
                var crossings = 0
                for (i in word.indices) {
                    val cell = GridCell(row + dr * i, col + dc * i)
                    val existing = grid[cell]
                    if (existing != null) {
                        if (existing != word[i].lowercaseChar()) return false
                        if (dirs[cell]?.contains(dir == CrosswordDirection.ACROSS) == true) return false
                        crossings += 1
                    } else {
                        if (grid.containsKey(GridCell(cell.row + dc, cell.col + dr))) return false
                        if (grid.containsKey(GridCell(cell.row - dc, cell.col - dr))) return false
                    }
                }
                return crossings > 0
            }

            setWord(first, 0, 0, CrosswordDirection.ACROSS)
            for (entry in words.drop(1)) {
                if (placed.size >= target) break
                val wordChars = entry.word.toCharArray()
                val options = mutableListOf<Triple<Int, Int, CrosswordDirection>>()
                for (p in placed) {
                    val pChars = p.word.toCharArray()
                    for (i in pChars.indices) {
                        for (j in wordChars.indices) {
                            if (pChars[i].lowercaseChar() != wordChars[j].lowercaseChar()) continue
                            val option = if (p.dir == CrosswordDirection.ACROSS) {
                                Triple(p.row - j, p.col + i, CrosswordDirection.DOWN)
                            } else {
                                Triple(p.row + i, p.col - j, CrosswordDirection.ACROSS)
                            }
                            if (canPlace(wordChars, option.first, option.second, option.third)) {
                                options.add(option)
                            }
                        }
                    }
                }
                if (options.isNotEmpty()) {
                    // Prefer compact placements.
                    fun area(o: Triple<Int, Int, CrosswordDirection>): Int {
                        val rows = placed.flatMap {
                            listOf(it.row, if (it.dir == CrosswordDirection.DOWN) it.row + it.word.length - 1 else it.row)
                        } + listOf(o.first, if (o.third == CrosswordDirection.DOWN) o.first + wordChars.size - 1 else o.first)
                        val cols = placed.flatMap {
                            listOf(it.col, if (it.dir == CrosswordDirection.ACROSS) it.col + it.word.length - 1 else it.col)
                        } + listOf(o.second, if (o.third == CrosswordDirection.ACROSS) o.second + wordChars.size - 1 else o.second)
                        return (rows.max() - rows.min() + 1) * (cols.max() - cols.min() + 1)
                    }
                    val pick = options.sortedBy { area(it) }.take(3).random()
                    setWord(entry, pick.first, pick.second, pick.third)
                }
            }

            val rows = placed.flatMap {
                listOf(it.row, if (it.dir == CrosswordDirection.DOWN) it.row + it.word.length - 1 else it.row)
            }
            val cols = placed.flatMap {
                listOf(it.col, if (it.dir == CrosswordDirection.ACROSS) it.col + it.word.length - 1 else it.col)
            }
            val attemptArea = (rows.max() - rows.min() + 1) * (cols.max() - cols.min() + 1)
            if (placed.size > best.size || (placed.size == best.size && attemptArea < bestArea)) {
                best = placed.toList()
                bestArea = attemptArea
            }
            if (best.size >= target && bestArea <= cozyArea) break
        }

        // Normalize to 0-based coordinates and assign clue numbers.
        val minRow = best.minOfOrNull { it.row } ?: 0
        val minCol = best.minOfOrNull { it.col } ?: 0
        var normalized = best.map { it.copy(row = it.row - minRow, col = it.col - minCol) }
        val height = (normalized.maxOfOrNull {
            if (it.dir == CrosswordDirection.DOWN) it.row + it.word.length - 1 else it.row
        } ?: 0) + 1
        val width = (normalized.maxOfOrNull {
            if (it.dir == CrosswordDirection.ACROSS) it.col + it.word.length - 1 else it.col
        } ?: 0) + 1

        val starts = normalized.map { GridCell(it.row, it.col) }.toSet()
            .sortedWith(compareBy({ it.row }, { it.col }))
        val numberByCell = starts.withIndex().associate { (i, cell) -> cell to i + 1 }
        normalized = normalized.map {
            it.copy(number = numberByCell[GridCell(it.row, it.col)] ?: 0)
        }

        return CrosswordPuzzle(placements = normalized, width = width, height = height)
    }
}
