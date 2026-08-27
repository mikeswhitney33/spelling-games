package com.skdaddle.spellit.engine

import com.skdaddle.spellit.data.WordData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Parity vectors generated from the web implementation (lib/game-utils.ts)
 * with scripts run against the real word lists. If any of these fail, the
 * Daily Bee would serve different words on Android than on the web and iOS.
 */
class SeededRandomTest {
    @Test
    fun mulberry32MatchesWebSequence() {
        val rng = Mulberry32(12345u)
        val expected = listOf(
            0.9797282677609473,
            0.3067522644996643,
            0.484205421525985,
            0.817934412509203,
            0.5094283693470061,
            0.34747186047025025,
            0.07375754183158278,
            0.7663964673411101,
        )
        for (value in expected) {
            assertEquals(value, rng.nextDouble(), 0.0)
        }
    }

    @Test
    fun seedHashMatchesWebHashString() {
        assertEquals(3502636655u, seedHash("2026-08-27-2-3"))
        assertEquals(3502558836u, seedHash("2026-08-27-k-1"))
        assertEquals(2982103592u, seedHash("2030-01-01-6-plus"))
        assertEquals(177604u, seedHash("a"))
        assertEquals(5381u, seedHash(""))
    }

    @Test
    fun seededShuffleMatchesWebOrder() {
        val rng = Mulberry32(seedHash("2026-08-27-2-3"))
        val shuffled = seededShuffle((0..9).toList(), rng)
        assertEquals(listOf(9, 3, 8, 2, 5, 7, 4, 1, 6, 0), shuffled)
    }

    @Test
    fun dailyBeePickMatchesWebForBand23() {
        val bank = WordData.builtInBanks.first { it.id == "band-2-3" }
        val rng = Mulberry32(seedHash("2026-08-27-2-3"))
        val picked = seededPick(bank.entries, 10, rng).map { it.word }
        assertEquals(
            listOf(
                "where", "together", "water", "laugh", "mother",
                "friend", "over", "thought", "night", "great",
            ),
            picked,
        )
    }

    @Test
    fun dailyBeePickMatchesWebForBand6Plus() {
        val bank = WordData.builtInBanks.first { it.id == "band-6-plus" }
        val rng = Mulberry32(seedHash("2026-12-31-6-plus"))
        val picked = seededPick(bank.entries, 10, rng).map { it.word }
        assertEquals(
            listOf(
                "grateful", "accommodate", "vacuum", "acquire", "exaggerate",
                "independent", "parallel", "argument", "sincerely", "maintenance",
            ),
            picked,
        )
    }
}
