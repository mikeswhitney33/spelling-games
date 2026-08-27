package com.skdaddle.spellit.engine

import com.skdaddle.spellit.data.WordData
import com.skdaddle.spellit.model.WordEntry
import kotlin.random.Random

data class WordSearchPlacement(
    val word: String,
    val hint: String,
    val row: Int,
    val col: Int,
    val dRow: Int,
    val dCol: Int,
) {
    val cells: List<GridCell>
        get() = (0 until word.length).map { i ->
            GridCell(row = row + dRow * i, col = col + dCol * i)
        }
}

data class WordSearchPuzzle(
    val grid: List<List<Char>>,
    val placements: List<WordSearchPlacement>,
    val size: Int,
)

/**
 * Port of the site's word-search generator, including the placement-aware
 * safety scan: no blocked word may touch a fill cell, and no stray copy of
 * an answer word may appear off its real placement.
 */
object WordSearchGenerator {
    data class Config(val size: Int, val count: Int, val diagonals: Boolean)

    /** Grid size scales with the pool's word lengths instead of a grade band. */
    fun configForPool(pool: List<WordEntry>): Config {
        val lengths = pool.map { it.word.length }.filter { it <= 12 }
        val maxLen = lengths.maxOrNull() ?: 5
        val size = minOf(12, maxOf(7, maxLen + 2))
        return Config(size = size, count = if (size <= 8) 5 else 6, diagonals = size >= 10)
    }

    private val alphabet = "abcdefghijklmnopqrstuvwxyz".toList()

    fun generate(pool: List<WordEntry>): WordSearchPuzzle {
        val config = configForPool(pool)
        val usable = pool.filter { it.word.length <= config.size }
        val directions: List<Pair<Int, Int>> =
            if (config.diagonals) listOf(0 to 1, 1 to 0, 1 to 1) else listOf(0 to 1, 1 to 0)

        var bestPlacements: List<WordSearchPlacement> = emptyList()
        var bestCells: Map<GridCell, Char> = emptyMap()

        for (round in 0 until 10) {
            if (bestPlacements.size >= config.count) break
            val entries = pickRandom(usable, minOf(usable.size, config.count + 4))
            val cells = mutableMapOf<GridCell, Char>()
            val placements = mutableListOf<WordSearchPlacement>()

            entry@ for (entry in entries) {
                if (placements.size >= config.count) break
                val word = entry.word.lowercase()
                // Reverse pairs (nap/pan) make selections ambiguous — keep one.
                val reversed = word.reversed()
                if (placements.any { it.word.lowercase() == reversed }) continue
                val chars = word.toCharArray()

                for (tryIndex in 0 until 60) {
                    val (dRow, dCol) = directions.random()
                    val maxRow = config.size - (if (dRow != 0) chars.size else 1)
                    val maxCol = config.size - (if (dCol != 0) chars.size else 1)
                    if (maxRow < 0 || maxCol < 0) continue@entry
                    val row = Random.nextInt(maxRow + 1)
                    val col = Random.nextInt(maxCol + 1)
                    // Keep the entry's casing for the word-chip list; the grid
                    // cells hold the lowercase letters.
                    val candidate = WordSearchPlacement(
                        word = entry.word, hint = entry.hint ?: "",
                        row = row, col = col, dRow = dRow, dCol = dCol,
                    )
                    var ok = true
                    candidate.cells.forEachIndexed { i, cell ->
                        val existing = cells[cell]
                        if (existing != null && existing != chars[i]) ok = false
                    }
                    if (!ok) continue
                    candidate.cells.forEachIndexed { i, cell -> cells[cell] = chars[i] }
                    placements.add(candidate)
                    break
                }
            }
            if (placements.size > bestPlacements.size) {
                bestPlacements = placements.toList()
                bestCells = cells.toMap()
            }
        }

        val wordLetters = bestPlacements.flatMap { it.word.lowercase().toList() }

        fun buildGrid(fill: () -> Char): List<List<Char>> =
            (0 until config.size).map { r ->
                (0 until config.size).map { c ->
                    bestCells[GridCell(r, c)] ?: fill()
                }
            }

        repeat(20) {
            val grid = buildGrid {
                if (Random.nextBoolean() && wordLetters.isNotEmpty()) wordLetters.random()
                else alphabet.random()
            }
            if (!hasProblem(grid, bestPlacements)) {
                return WordSearchPuzzle(grid, bestPlacements.shuffled(), config.size)
            }
        }
        for (attempt in 0..20) {
            val grid = buildGrid { alphabet.random() }
            if (attempt == 20 || !hasProblem(grid, bestPlacements)) {
                return WordSearchPuzzle(grid, bestPlacements.shuffled(), config.size)
            }
        }
        error("unreachable")
    }

    private fun lines(size: Int): List<List<GridCell>> {
        val lines = mutableListOf<List<GridCell>>()
        for (r in 0 until size) lines.add((0 until size).map { GridCell(r, it) })
        for (c in 0 until size) lines.add((0 until size).map { GridCell(it, c) })
        for (d in (-size + 1) until size) {
            val diag = mutableListOf<GridCell>()
            val antiDiag = mutableListOf<GridCell>()
            for (r in 0 until size) {
                val c = r + d
                if (c in 0 until size) diag.add(GridCell(r, c))
                val ac = size - 1 - r + d
                if (ac in 0 until size) antiDiag.add(GridCell(r, ac))
            }
            if (diag.size >= 3) lines.add(diag)
            if (antiDiag.size >= 3) lines.add(antiDiag)
        }
        return lines
    }

    private fun hasProblem(grid: List<List<Char>>, placements: List<WordSearchPlacement>): Boolean {
        val placedCells = mutableSetOf<GridCell>()
        val paths = mutableMapOf<String, MutableSet<List<GridCell>>>()
        for (p in placements) {
            // Grid cells are lowercase, so scan with the lowercased word.
            val word = p.word.lowercase()
            val cells = p.cells
            placedCells.addAll(cells)
            paths.getOrPut(word) { mutableSetOf() }.add(cells)
            paths.getOrPut(word) { mutableSetOf() }.add(cells.reversed())
        }

        for (line in lines(grid.size)) {
            for (oriented in listOf(line, line.reversed())) {
                val text = String(oriented.map { grid[it.row][it.col] }.toCharArray())
                for (bad in WordData.blockedWords) {
                    var from = 0
                    while (true) {
                        val start = text.indexOf(bad, from)
                        if (start < 0) break
                        val window = oriented.subList(start, start + bad.length)
                        if (window.any { it !in placedCells }) return true
                        from = start + 1
                    }
                }
                for (word in paths.keys) {
                    var from = 0
                    while (true) {
                        val start = text.indexOf(word, from)
                        if (start < 0) break
                        val path = oriented.subList(start, start + word.length)
                        if (paths[word]?.contains(path) != true) return true
                        from = start + 1
                    }
                }
            }
        }
        return false
    }
}
