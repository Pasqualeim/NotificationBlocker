package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    // 2026-10-15 is a Thursday
    private val thursday = LocalDate.of(2026, 10, 15)
    private val history = mapOf(
        thursday to DayStats(thursday, livedMinutes = 130, daylightMinutes = 40, held = 5, byPause = mapOf("evening" to 70, "lunch" to 60)),
        thursday.minusDays(1) to DayStats(thursday.minusDays(1), livedMinutes = 600, daylightMinutes = 0, held = 2),
        thursday.minusDays(10) to DayStats(thursday.minusDays(10), livedMinutes = 60),
    )

    @Test
    fun week_startsOnMonday_andHasSevenDays() {
        val week = PeriodStats.of(StatsRange.WEEK, thursday, history)
        assertEquals(LocalDate.of(2026, 10, 12), week.first)
        assertEquals(LocalDate.of(2026, 10, 18), week.last)
        assertEquals(7, week.days.size)
        assertEquals(730, week.livedMinutes)
        assertEquals(40, week.daylightMinutes)
        assertEquals(7, week.held)
    }

    @Test
    fun month_coversItsCalendarMonth() {
        val month = PeriodStats.of(StatsRange.MONTH, thursday, history)
        assertEquals(31, month.days.size)
        assertEquals(790, month.livedMinutes)
    }

    @Test
    fun day_isJustThatDay() {
        val day = PeriodStats.of(StatsRange.DAY, thursday, history)
        assertEquals(listOf(history[thursday]), day.days)
    }

    @Test
    fun emptyDays_getAnEmptySlot() {
        val week = PeriodStats.of(StatsRange.WEEK, thursday.plusDays(14), history)
        assertTrue(week.days.all { it.livedMinutes == 0 })
        assertEquals(7, week.days.size)
    }

    @Test
    fun shift_movesByWholePeriods() {
        assertEquals(thursday.minusDays(1), PeriodStats.shift(StatsRange.DAY, thursday, -1))
        assertEquals(thursday.minusWeeks(1), PeriodStats.shift(StatsRange.WEEK, thursday, -1))
        assertEquals(LocalDate.of(2026, 11, 15), PeriodStats.shift(StatsRange.MONTH, thursday, 1))
    }

    @Test
    fun daySpans_comeFromThePausesLivedThatDay_upToNow() {
        val evening = Pause("evening", "Evening", 18 * 60 + 30, 8 * 60 + 30)
        val lunch = Pause("lunch", "Lunch", 13 * 60, 14 * 60)
        val weekend = Pause("weekend", "Weekend", 0, 0)
        val spans = DaySpans.of(history[thursday]!!, listOf(evening, lunch, weekend), thursday.atTime(19, 0))
        // Last night's evening up to 08:30, lunch, tonight's evening up to now; no weekend (nothing lived)
        assertEquals(listOf(0 to 510, 780 to 840, 1110 to 1140), spans.map { it.startMinute to it.endMinute })
        assertEquals(listOf("evening", "lunch", "evening"), spans.map { it.pause.id })
        // Asleep 23:00-07:00: last night's evening starts at 07:00 on the chart
        val awake = DaySpans.of(history[thursday]!!, listOf(evening, lunch, weekend), thursday.atTime(19, 0), SleepWindow(23 * 60, 7 * 60))
        assertEquals(listOf(420 to 510, 780 to 840, 1110 to 1140), awake.map { it.startMinute to it.endMinute })
    }
}
