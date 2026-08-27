package com.skdaddle.spellit.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStoreTest {
    @Test
    fun firstFinishStartsAStreak() {
        val next = DailyStore.advance(DailyStreak(), "2026-08-27", "2026-08-26")
        assertEquals(DailyStreak("2026-08-27", 1, 1), next)
    }

    @Test
    fun consecutiveDaysExtendTheStreak() {
        val start = DailyStreak("2026-08-26", 3, 5)
        val next = DailyStore.advance(start, "2026-08-27", "2026-08-26")
        assertEquals(DailyStreak("2026-08-27", 4, 5), next)
    }

    @Test
    fun aGapResetsToOne() {
        val start = DailyStreak("2026-08-20", 9, 9)
        val next = DailyStore.advance(start, "2026-08-27", "2026-08-26")
        assertEquals(DailyStreak("2026-08-27", 1, 9), next)
    }

    @Test
    fun secondFinishSameDayDoesNothing() {
        val start = DailyStreak("2026-08-27", 4, 4)
        val next = DailyStore.advance(start, "2026-08-27", "2026-08-26")
        assertEquals(start, next)
    }

    @Test
    fun yesterdayCrossesMonthAndYearBoundaries() {
        assertEquals("2026-08-26", DailyStore.yesterdayOf("2026-08-27"))
        assertEquals("2026-07-31", DailyStore.yesterdayOf("2026-08-01"))
        assertEquals("2025-12-31", DailyStore.yesterdayOf("2026-01-01"))
        assertEquals("2024-02-29", DailyStore.yesterdayOf("2024-03-01"))
    }
}
