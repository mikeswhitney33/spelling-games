package com.skdaddle.spellit.engine

import android.content.Context
import com.skdaddle.spellit.model.GradeBand

/**
 * Remembered grade pick for the band-driven games (Daily Bee, Ending
 * Machine), mirroring iOS `@AppStorage("spellit.grade")`. Stored as the iOS
 * raw value (k1/g23/g45/g6plus) so nothing is invented here.
 */
object GradeStore {
    private const val PREFS_NAME = "spellit"
    private const val KEY = "spellit.grade"

    private val rawValues = mapOf(
        GradeBand.K1 to "k1",
        GradeBand.G23 to "g23",
        GradeBand.G45 to "g45",
        GradeBand.G6_PLUS to "g6plus",
    )

    fun read(context: Context): GradeBand {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, null)
        return rawValues.entries.firstOrNull { it.value == raw }?.key ?: GradeBand.G23
    }

    fun save(context: Context, grade: GradeBand) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, rawValues.getValue(grade))
            .apply()
    }
}
