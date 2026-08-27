package com.skdaddle.spellit.engine

import com.skdaddle.spellit.data.WordData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MisspellTest {
    @Test
    fun fakesAreNeverRealWordsOrListWords() {
        for (bank in WordData.builtInBanks) {
            for (entry in bank.entries) {
                val fakes = Misspell.make(entry.word, 3)
                for (fake in fakes) {
                    val lower = fake.lowercase()
                    assertFalse("\"$fake\" is in the real-word guard", lower in WordData.realWordGuard)
                    assertFalse("\"$fake\" is a list word", lower in Misspell.allListWords)
                    assertFalse("\"$fake\" equals its source", lower == entry.word.lowercase())
                }
            }
        }
    }

    @Test
    fun builtInWordsGetFullCandidates() {
        // Every built-in word must be able to produce at least 3 fakes, or
        // Spot the Word can't build its four options.
        for (bank in WordData.builtInBanks) {
            for (entry in bank.entries) {
                val fakes = Misspell.make(entry.word, 3)
                assertEquals("\"${entry.word}\" produced ${fakes.size} fakes", 3, fakes.size)
            }
        }
    }

    @Test
    fun capitalizedWordsKeepTheirLeadingCapital() {
        // February is the one capitalized built-in entry; fakes must match its
        // casing so the real answer doesn't stand out.
        val fakes = Misspell.make("February", 3)
        assertEquals(3, fakes.size)
        for (fake in fakes) {
            assertTrue("\"$fake\" should start uppercase", fake.first().isUpperCase())
        }
    }

    @Test
    fun customWordsUseCautiousRulesOnly() {
        // "zorbit" is not in any list, so only cautious edits apply:
        // first-letter double, last-letter double (len >= 4). No doubled
        // interior pair exists, so exactly those two.
        val candidates = Misspell.cautiousCandidates("zorbit")
        assertEquals(listOf("zorbitt", "zzorbit"), candidates)
    }

    @Test
    fun cautiousNeverDoublesGeneralLetters() {
        // hoping must NOT produce hopping (a real word) under cautious rules.
        val candidates = Misspell.cautiousCandidates("hoping")
        assertFalse("hopping" in candidates)
    }

    @Test
    fun matchCaseCopiesLeadingCapitalOnly() {
        assertEquals("Febuary", matchCase(model = "February", text = "febuary"))
        assertEquals("cat", matchCase(model = "dog", text = "cat"))
        assertEquals("", matchCase(model = "Dog", text = ""))
    }
}
