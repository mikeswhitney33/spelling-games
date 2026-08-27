package com.skdaddle.spellit.engine

import com.skdaddle.spellit.data.WordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorsTest {
    private val pool = WordData.builtInBanks.first { it.id == "band-2-3" }.entries

    @Test
    fun crosswordPlacesCrossingWordsInsideTheGrid() {
        repeat(5) {
            val puzzle = CrosswordGenerator.generate(pool)
            assertTrue("expected at least 2 placements", puzzle.placements.size >= 2)

            val letters = mutableMapOf<GridCell, Char>()
            var crossings = 0
            for (p in puzzle.placements) {
                assertTrue(p.number > 0)
                val chars = p.word.lowercase().toCharArray()
                p.cells.forEachIndexed { i, cell ->
                    assertTrue(cell.row in 0 until puzzle.height)
                    assertTrue(cell.col in 0 until puzzle.width)
                    val existing = letters[cell]
                    if (existing != null) {
                        assertEquals("conflicting letters at $cell", existing, chars[i])
                        crossings += 1
                    }
                    letters[cell] = chars[i]
                }
            }
            assertTrue("every extra word must cross", crossings >= puzzle.placements.size - 1)
        }
    }

    @Test
    fun crosswordSkipsWordsOverTheLengthCap() {
        val puzzle = CrosswordGenerator.generate(pool, target = 5)
        for (p in puzzle.placements) {
            assertTrue(p.word.length <= CrosswordGenerator.MAX_WORD_LENGTH)
        }
    }

    @Test
    fun wordSearchGridContainsEveryPlacementExactly() {
        repeat(5) {
            val puzzle = WordSearchGenerator.generate(pool)
            assertTrue("expected some placements", puzzle.placements.isNotEmpty())
            assertEquals(puzzle.size, puzzle.grid.size)
            for (row in puzzle.grid) assertEquals(puzzle.size, row.size)

            for (p in puzzle.placements) {
                val chars = p.word.lowercase().toCharArray()
                p.cells.forEachIndexed { i, cell ->
                    assertEquals(
                        "grid letter mismatch for ${p.word} at $cell",
                        chars[i],
                        puzzle.grid[cell.row][cell.col],
                    )
                }
            }
        }
    }

    @Test
    fun wordSearchNeverKeepsReversePairs() {
        repeat(5) {
            val puzzle = WordSearchGenerator.generate(pool)
            val words = puzzle.placements.map { it.word.lowercase() }
            for (word in words) {
                if (word.reversed() == word) continue
                assertFalse(
                    "both $word and its reverse placed",
                    word.reversed() in words && word < word.reversed(),
                )
            }
        }
    }

    @Test
    fun wordSearchConfigScalesWithWordLength() {
        val shortPool = WordData.builtInBanks.first { it.id == "band-k-1" }.entries
        val config = WordSearchGenerator.configForPool(shortPool)
        assertTrue(config.size in 7..12)

        val longPool = WordData.builtInBanks.first { it.id == "band-6-plus" }.entries
        val longConfig = WordSearchGenerator.configForPool(longPool)
        assertTrue(longConfig.size >= config.size)
    }
}
