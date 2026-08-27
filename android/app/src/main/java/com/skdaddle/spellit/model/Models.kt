package com.skdaddle.spellit.model

import kotlinx.serialization.Serializable

enum class GradeBand(
    val label: String,
    val short: String,
    val blurb: String,
    /**
     * Seed component matching the web app's grade ids, so the Daily Bee
     * serves the same words on every platform.
     */
    val seedKey: String,
) {
    K1(
        "Grades K–1",
        "K–1",
        "Short, sound-it-out words like cat, sun, and hop.",
        "k-1",
    ),
    G23(
        "Grades 2–3",
        "2–3",
        "Everyday words with tricky parts, like friend and because.",
        "2-3",
    ),
    G45(
        "Grades 4–5",
        "4–5",
        "Longer words people often misspell, like separate and library.",
        "4-5",
    ),
    G6_PLUS(
        "Grades 6+",
        "6+",
        "Challenge words like rhythm, committee, and mischievous.",
        "6-plus",
    ),
}

@Serializable
data class WordEntry(
    val word: String,
    val hint: String? = null,
    val sentence: String? = null,
)

data class WordBank(
    val id: String,
    val name: String,
    val blurb: String,
    val builtIn: Boolean,
    val entries: List<WordEntry>,
) {
    /** Never-empty title, even if a custom list was renamed to nothing. */
    val displayName: String
        get() = name.trim().ifEmpty { "My list" }
}

data class EndingTask(
    val base: String,
    val suffix: String,
    val word: String,
    val hint: String,
    val also: List<String>,
)
