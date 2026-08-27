package com.skdaddle.spellit.engine

/**
 * Exact port of the site's mulberry32 PRNG so date-seeded picks (Daily Bee)
 * produce the same words on Android, iOS, and the web.
 */
class Mulberry32(seed: UInt) {
    private var state: UInt = seed

    /** Matches the JS implementation bit-for-bit, returning in [0, 1). */
    fun nextDouble(): Double {
        state += 0x6D2B_79F5u
        var t = state
        t = (t xor (t shr 15)) * (t or 1u)
        t = t xor (t + (t xor (t shr 7)) * (t or 61u))
        return (t xor (t shr 14)).toDouble() / 4_294_967_296.0
    }
}

/**
 * Port of the site's djb2-style string hash (matches hashString in
 * daily-bee-game.tsx).
 */
fun seedHash(text: String): UInt {
    var hash = 5381u
    for (ch in text) {
        hash = (hash * 33u) xor (ch.code.toUInt() and 0xFFFFu)
    }
    return hash
}

/** Fisher–Yates shuffle matching the site's shuffle(), so seeded picks agree. */
fun <T> seededShuffle(items: List<T>, rng: Mulberry32): List<T> {
    val out = items.toMutableList()
    for (i in out.size - 1 downTo 1) {
        val j = (rng.nextDouble() * (i + 1)).toInt()
        val tmp = out[i]
        out[i] = out[j]
        out[j] = tmp
    }
    return out
}

fun <T> seededPick(items: List<T>, n: Int, rng: Mulberry32): List<T> =
    seededShuffle(items, rng).take(n)

/** Unseeded convenience for everyday rounds. */
fun <T> pickRandom(items: List<T>, n: Int): List<T> = items.shuffled().take(n)
