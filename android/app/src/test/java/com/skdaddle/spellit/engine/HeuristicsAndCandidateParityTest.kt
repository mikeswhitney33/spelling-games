package com.skdaddle.spellit.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the pieces the SeededRandomTest vectors don't cover: the candidate
 * edit rules (exact sets from lib/game-utils.ts misspellingCandidates) and
 * the word-length heuristics shared with blanksForWord / flashMsForWord.
 */
class HeuristicsAndCandidateParityTest {
    @Test
    fun candidateSetsMatchTheWebEditRules() {
        assertEquals(
            listOf(
                "bacause", "bbecause", "bcause", "bceause", "beacuse", "becaause",
                "becaese", "becaose", "becase", "becasue", "becauce", "becaues",
                "becausa", "becausee", "becausi", "becausse", "becauuse", "becauze",
                "beccause", "beceuse", "becuase", "becuse", "becuuse", "beecause",
                "bekause", "besause", "bicause", "ebcause",
            ),
            Misspell.candidates("because").sorted(),
        )
        assertEquals(
            listOf(
                "ffriend", "firend", "freend", "freind", "frend", "friand",
                "friedn", "frieend", "friendd", "friennd", "friiend", "friind",
                "frind", "frined", "frriend", "fryend", "rfiend", "vriend",
            ),
            Misspell.candidates("friend").sorted(),
        )
        assertEquals(
            listOf("act", "caat", "catt", "ccat", "cet", "ct", "cta", "cut", "kat", "sat"),
            Misspell.candidates("cat").sorted(),
        )
    }

    @Test
    fun blanksMatchBlanksForWord() {
        assertEquals(1, GameHeuristics.blanks("cat"))
        assertEquals(1, GameHeuristics.blanks("bed"))
        assertEquals(2, GameHeuristics.blanks("friend"))
        assertEquals(2, GameHeuristics.blanks("because"))
        assertEquals(3, GameHeuristics.blanks("necessary"))
        assertEquals(4, GameHeuristics.blanks("maintenance"))
        assertEquals(4, GameHeuristics.blanks("mischievous"))
    }

    @Test
    fun flashDurationMatchesFlashMsForWord() {
        assertEquals(4000L, GameHeuristics.flashMs("cat"))
        assertEquals(4000L, GameHeuristics.flashMs("bird"))
        assertEquals(3500L, GameHeuristics.flashMs("friend"))
        assertEquals(3500L, GameHeuristics.flashMs("because"))
        assertEquals(3000L, GameHeuristics.flashMs("necessary"))
    }
}
