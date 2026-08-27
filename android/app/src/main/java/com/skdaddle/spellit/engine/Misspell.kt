package com.skdaddle.spellit.engine

import com.skdaddle.spellit.data.WordData

/**
 * Port of the site's misspelling generator: single-edit candidates filtered
 * against the dictionary-derived guard so a "fake" is never a real word.
 */
object Misspell {
    private val vowelSwaps: Map<Char, List<Char>> = mapOf(
        'a' to listOf('e', 'u'), 'e' to listOf('a', 'i'), 'i' to listOf('e', 'y'),
        'o' to listOf('u', 'a'), 'u' to listOf('o', 'e'), 'y' to listOf('i', 'e'),
    )

    private val phoneticSwaps: Map<Char, List<Char>> = mapOf(
        'c' to listOf('k', 's'), 'k' to listOf('c'), 's' to listOf('z', 'c'), 'z' to listOf('s'),
        'f' to listOf('v'), 'v' to listOf('f'), 'g' to listOf('j'), 'j' to listOf('g'),
    )

    private val vowelish = "aeiouhgk".toSet()

    fun candidates(word: String): List<String> {
        val chars = word.toCharArray()
        val out = mutableSetOf<String>()

        fun replaced(i: Int, ch: Char): String {
            val copy = chars.copyOf()
            copy[i] = ch
            return String(copy)
        }

        // Transpose adjacent letters
        for (i in 0 until chars.size - 1) {
            if (chars[i] == chars[i + 1]) continue
            val copy = chars.copyOf()
            copy[i] = chars[i + 1]
            copy[i + 1] = chars[i]
            out.add(String(copy))
        }
        // Vowel and phonetic swaps
        for (i in chars.indices) {
            vowelSwaps[chars[i]]?.forEach { out.add(replaced(i, it)) }
            phoneticSwaps[chars[i]]?.forEach { out.add(replaced(i, it)) }
        }
        // Undouble a doubled letter
        for (i in 0 until chars.size - 1) {
            if (chars[i] != chars[i + 1]) continue
            out.add(String(chars.copyOf().toMutableList().apply { removeAt(i) }.toCharArray()))
        }
        // Double a letter
        for (i in chars.indices) {
            val copy = chars.toMutableList()
            copy.add(i, chars[i])
            out.add(String(copy.toCharArray()))
        }
        // Drop a silent-ish letter
        for (i in 1 until maxOf(chars.size - 1, 1)) {
            if (chars[i] !in vowelish) continue
            out.add(String(chars.copyOf().toMutableList().apply { removeAt(i) }.toCharArray()))
        }

        out.remove(word)
        return out.filter { it.length >= 2 }
    }

    /**
     * Every built-in word, lowercase — never a valid "fake", and the boundary
     * for when the cautious custom-word rules apply.
     */
    val allListWords: Set<String> by lazy {
        WordData.builtInBanks.flatMap { bank -> bank.entries.map { it.word.lowercase() } }.toSet()
    }

    /**
     * Candidates for custom words the dictionary guard can't vet: only edits
     * that essentially never produce real English words (general doubling
     * lands on real words exactly where spelling lists live —
     * hoping/hopping, diner/dinner).
     */
    fun cautiousCandidates(word: String): List<String> {
        val out = mutableSetOf<String>()
        val chars = word.toCharArray()
        for (i in 0 until chars.size - 1) {
            if (chars[i] != chars[i + 1]) continue
            val copy = chars.toMutableList()
            copy.add(i, chars[i])
            out.add(String(copy.toCharArray()))
        }
        out.add(chars[0] + word)
        if (chars.size >= 4) {
            out.add(word + chars[chars.size - 1])
        }
        out.remove(word)
        return out.sorted()
    }

    fun make(word: String, count: Int): List<String> {
        // Generate from the lowercased word so the lowercase-keyed swap tables
        // apply to every letter, then restore the leading capital so the real
        // answer's casing doesn't give it away. Custom words the dictionary
        // guard can't vet fall back to the cautious rules.
        val lower = word.lowercase()
        val cautious = lower !in allListWords
        val raw = if (cautious) cautiousCandidates(lower) else candidates(lower)
        val pool = raw.filter { it !in WordData.realWordGuard && it !in allListWords }
        return pool.shuffled().take(count).map { matchCase(model = word, text = it) }
    }
}

/** Copy the model word's leading capital (if any) onto text. */
fun matchCase(model: String, text: String): String {
    val first = model.firstOrNull() ?: return text
    if (!first.isUpperCase() || text.isEmpty()) return text
    return text[0].uppercaseChar() + text.substring(1)
}
