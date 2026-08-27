package com.skdaddle.spellit.engine

import android.content.Context
import java.util.Calendar
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DailyStreak(
    val lastPlayed: String = "",
    val streak: Int = 0,
    val best: Int = 0,
)

/** Daily Bee streak persistence + the date string that seeds the daily round. */
object DailyStore {
    private const val PREFS_NAME = "spellit"
    private const val KEY = "spellit.daily"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * GregorianCalendar explicitly (not Calendar.getInstance) so the daily
     * seed matches the website — a device set to e.g. the Buddhist calendar
     * would otherwise hash a different year.
     */
    fun formatDate(calendar: Calendar = java.util.GregorianCalendar()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(java.util.Locale.ROOT, year, month, day)
    }

    /** Pure streak transition, shared by save() and the tests. */
    fun advance(data: DailyStreak, today: String, yesterday: String): DailyStreak {
        if (data.lastPlayed == today) return data
        val streak = if (data.lastPlayed == yesterday) data.streak + 1 else 1
        return DailyStreak(
            lastPlayed = today,
            streak = streak,
            best = maxOf(data.best, streak),
        )
    }

    fun yesterdayOf(today: String): String {
        val parts = today.split("-").mapNotNull { it.toIntOrNull() }
        // A malformed stored date just breaks the streak chain (matching the
        // iOS fallback) instead of crashing save().
        if (parts.size != 3) return ""
        val calendar = java.util.GregorianCalendar(parts[0], parts[1] - 1, parts[2])
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return formatDate(calendar)
    }

    fun read(context: Context): DailyStreak {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY, null) ?: return DailyStreak()
        return runCatching { json.decodeFromString<DailyStreak>(data) }
            .getOrDefault(DailyStreak())
    }

    /** Record a completion for `today`; only the first finish of a day counts. */
    fun save(context: Context, today: String): DailyStreak {
        val next = advance(read(context), today, yesterdayOf(today))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(next))
            .apply()
        return next
    }
}
