package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OffHoursTest {

    private fun t(hours: Int, minutes: Int = 0) = hours * 60 + minutes

    @Test
    fun sameDayWindow_includesStartExcludesEnd() {
        val start = t(9)
        val end = t(17)
        assertTrue(OffHours.isWithin(t(9), start, end))
        assertTrue(OffHours.isWithin(t(16, 59), start, end))
        assertFalse(OffHours.isWithin(t(17), start, end))
        assertFalse(OffHours.isWithin(t(8, 59), start, end))
    }

    @Test
    fun overnightWindow_wrapsPastMidnight() {
        val start = t(22)
        val end = t(7)
        assertTrue(OffHours.isWithin(t(22), start, end))
        assertTrue(OffHours.isWithin(t(23, 59), start, end))
        assertTrue(OffHours.isWithin(t(0), start, end))
        assertTrue(OffHours.isWithin(t(6, 59), start, end))
        assertFalse(OffHours.isWithin(t(7), start, end))
        assertFalse(OffHours.isWithin(t(12), start, end))
        assertFalse(OffHours.isWithin(t(21, 59), start, end))
    }

    @Test
    fun equalStartAndEnd_coversWholeDay() {
        assertTrue(OffHours.isWithin(t(0), t(8), t(8)))
        assertTrue(OffHours.isWithin(t(12), t(8), t(8)))
    }

    @Test
    fun format_padsHoursAndMinutes() {
        assertEquals("07:05", OffHours.format(t(7, 5)))
        assertEquals("22:00", OffHours.format(t(22)))
    }

    @Test
    fun windowStartDate_overnightWindowAfterMidnight_isPreviousDay() {
        val today = LocalDate.of(2026, 9, 26)
        assertEquals(today, OffHours.windowStartDate(today, t(23), t(22), t(7)))
        assertEquals(today.minusDays(1), OffHours.windowStartDate(today, t(1), t(22), t(7)))
    }

    @Test
    fun windowStartDate_sameDayAndWholeDayWindows_areToday() {
        val today = LocalDate.of(2026, 9, 26)
        assertEquals(today, OffHours.windowStartDate(today, t(10), t(9), t(17)))
        assertEquals(today, OffHours.windowStartDate(today, t(3), t(8), t(8)))
    }

    @Test
    fun nextStart_isTheUsualStart_orMidnightForAWholeDayWindow() {
        assertEquals(t(22), OffHours.nextStart(t(22), t(7)))
        assertEquals(t(18), OffHours.nextStart(t(18), t(23)))
        assertEquals(0, OffHours.nextStart(t(12), t(12)))
    }
}
