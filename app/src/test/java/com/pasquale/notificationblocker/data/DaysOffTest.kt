package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DaysOffTest {

    private val apps = setOf("com.slack")

    @Test
    fun easter_matchesKnownDates() {
        assertEquals(LocalDate.of(2026, 4, 5), DaysOff.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), DaysOff.easter(2027))
        assertEquals(LocalDate.of(2025, 4, 20), DaysOff.easter(2025))
    }

    @Test
    fun holidays_areTwelveAYear_withEasterMonday() {
        val days = DaysOff.holidays(2026)
        assertEquals(12, days.size)
        assertEquals(Holiday.EASTER_MONDAY, days[LocalDate.of(2026, 4, 6)])
    }

    @Test
    fun range_blocksEveryDayOfIt_andNothingAfter() {
        val range = DaysOff.rangePauses("off1", LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 2))
        assertEquals(5, range.size)
        assertTrue(PauseSchedule.shouldBlock("com.slack", true, apps, range, LocalDate.of(2026, 11, 4).atTime(15, 0)))
        assertFalse(PauseSchedule.shouldBlock("com.slack", true, apps, range, LocalDate.of(2026, 11, 7).atTime(15, 0)))
        assertEquals(listOf(DaysOff.Range("off1", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))), DaysOff.ranges(range))
    }

    @Test
    fun holidayPauses_coverTheWindow_acrossNewYear() {
        val pauses = DaysOff.holidayPauses(LocalDate.of(2026, 12, 24), LocalDate.of(2027, 1, 7))
        assertEquals(listOf(25, 26, 1, 6), pauses.map { it.date!!.dayOfMonth })
        assertEquals(Holiday.CHRISTMAS.name, pauses.first().name)
        assertEquals(LocalDate.of(2026, 11, 1) to Holiday.ALL_SAINTS, DaysOff.nextHoliday(LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun longRange_isCappedAtMaxDays() {
        val range = DaysOff.rangePauses("off2", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 15))
        assertEquals(DaysOff.MAX_DAYS, range.size)
    }
}
