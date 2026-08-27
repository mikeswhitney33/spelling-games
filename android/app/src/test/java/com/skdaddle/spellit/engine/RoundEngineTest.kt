package com.skdaddle.spellit.engine

import com.skdaddle.spellit.model.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundEngineTest {
    private fun pool(n: Int) = (1..n).map { WordEntry("word$it") }

    @Test
    fun playsThroughATenWordRound() {
        val engine = RoundEngine()
        engine.start(pool(30))
        assertEquals(10, engine.words.size)
        assertEquals(RoundEngine.Phase.PLAYING, engine.phase)

        repeat(10) {
            engine.record(correct = true)
            engine.advance()
        }
        assertEquals(RoundEngine.Phase.DONE, engine.phase)
        assertEquals(10, engine.score)
        assertEquals(10, engine.bestStreak)
        assertNull(engine.current)
        assertTrue(engine.missedWords.isEmpty())
    }

    @Test
    fun missTracksStreakAndMissedWords() {
        val engine = RoundEngine()
        engine.startFixed(pool(3))
        engine.record(correct = true)
        engine.advance()
        engine.record(correct = false)
        engine.advance()
        engine.record(correct = true)
        engine.advance()

        assertEquals(RoundEngine.Phase.DONE, engine.phase)
        assertEquals(2, engine.score)
        assertEquals(1, engine.bestStreak)
        assertEquals(1, engine.missedWords.size)
    }

    @Test
    fun shortPoolShrinksTheRound() {
        val engine = RoundEngine()
        engine.start(pool(4))
        assertEquals(4, engine.words.size)
    }

    @Test
    fun clearEmptiesEverything() {
        val engine = RoundEngine()
        engine.start(pool(20))
        engine.record(correct = true)
        engine.clear()
        assertEquals(0, engine.words.size)
        assertEquals(0, engine.score)
        assertNull(engine.current)
    }

    @Test
    fun recordAfterDoneIsIgnored() {
        val engine = RoundEngine()
        engine.startFixed(pool(1))
        engine.record(correct = true)
        engine.advance()
        assertEquals(RoundEngine.Phase.DONE, engine.phase)
        engine.record(correct = true)
        assertEquals(1, engine.score)
    }

    @Test
    fun starsMatchTheSharedThresholds() {
        assertEquals(3, RoundEngine.stars(9, 10))
        assertEquals(2, RoundEngine.stars(7, 10))
        assertEquals(1, RoundEngine.stars(5, 10))
        assertEquals(0, RoundEngine.stars(4, 10))
        assertEquals(0, RoundEngine.stars(0, 0))
    }
}
