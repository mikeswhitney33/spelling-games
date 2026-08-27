package com.skdaddle.spellit.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skdaddle.spellit.model.WordEntry

/** The shared 10-word round state machine, mirroring the site's useGameRound. */
class RoundEngine {
    enum class Phase { PLAYING, DONE }

    companion object {
        const val ROUND_LENGTH = 10

        fun stars(score: Int, total: Int): Int {
            if (total <= 0) return 0
            val ratio = score.toDouble() / total
            return when {
                ratio >= 0.9 -> 3
                ratio >= 0.7 -> 2
                ratio >= 0.5 -> 1
                else -> 0
            }
        }
    }

    var words by mutableStateOf<List<WordEntry>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var score by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    var bestStreak by mutableIntStateOf(0)
        private set
    var results by mutableStateOf<List<Boolean>>(emptyList())
        private set
    var phase by mutableStateOf(Phase.PLAYING)
        private set

    /** Bumps whenever a fresh round starts, for per-word view identity. */
    var roundId by mutableIntStateOf(0)
        private set

    val current: WordEntry?
        get() = if (phase == Phase.PLAYING && index < words.size) words[index] else null

    val isLastWord: Boolean
        get() = index + 1 >= words.size

    fun start(pool: List<WordEntry>, length: Int = ROUND_LENGTH) {
        words = pickRandom(pool, length)
        resetProgress()
    }

    /** Deterministic variant for the Daily Bee (set is seeded, order shuffles). */
    fun startFixed(fixedWords: List<WordEntry>) {
        words = fixedWords.shuffled()
        resetProgress()
    }

    private fun resetProgress() {
        index = 0
        score = 0
        streak = 0
        bestStreak = 0
        results = emptyList()
        phase = Phase.PLAYING
        roundId += 1
    }

    /**
     * Empty the round entirely (e.g. the active bank became too small),
     * hiding the score bar and any stale summary.
     */
    fun clear() {
        words = emptyList()
        resetProgress()
    }

    fun record(correct: Boolean) {
        if (phase != Phase.PLAYING) return
        if (correct) {
            score += 1
            streak += 1
            bestStreak = maxOf(bestStreak, streak)
        } else {
            streak = 0
        }
        results = results + correct
    }

    fun advance() {
        if (phase != Phase.PLAYING) return
        if (index + 1 >= words.size) {
            phase = Phase.DONE
        } else {
            index += 1
        }
    }

    val missedWords: List<WordEntry>
        get() = words.withIndex()
            .filter { (i, _) -> i < results.size && !results[i] }
            .map { it.value }
}
