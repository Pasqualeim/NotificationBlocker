package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class QuickPauseTest {

    // 2026-10-16 is a Friday
    private val friday = LocalDate.of(2026, 10, 16)
    private val apps = setOf("com.slack")

    @Test
    fun datedPause_happensOnItsDateOnly() {
        val off = Pause("off", "Ferie", 0, 0, date = friday)
        assertTrue(PauseSchedule.shouldBlock("com.slack", true, apps, listOf(off), friday.atTime(10, 0)))
        // Same weekday a week later: nothing
        assertFalse(PauseSchedule.shouldBlock("com.slack", true, apps, listOf(off), friday.plusDays(7).atTime(10, 0)))
        assertNull(PauseSchedule.nextOccurrence(listOf(off), friday.plusDays(1).atTime(0, 0)))
    }

    @Test
    fun quickPause_startsNow_andCanCrossMidnight() {
        val quick = QuickPause.create(friday.atTime(23, 30), 60)
        assertEquals(23 * 60 + 30, quick.start)
        assertEquals(30, quick.end)
        val now = friday.plusDays(1).atTime(0, 15)
        assertTrue(PauseSchedule.shouldBlock("com.slack", true, apps, listOf(quick), now))
        assertEquals(friday.plusDays(1).atTime(0, 30), PauseSchedule.blockedUntil(listOf(quick), now))
        assertFalse(PauseSchedule.shouldBlock("com.slack", true, apps, listOf(quick), friday.plusDays(1).atTime(0, 30)))
    }

    @Test
    fun twoQuickPausesOnTheSameDay_haveTheirOwnKeys() {
        val first = QuickPause.create(friday.atTime(9, 0), 30)
        val second = QuickPause.create(friday.atTime(16, 20), 30)
        assertTrue(QuickPause.isQuick(first) && QuickPause.isQuick(second))
        assertTrue(Occurrence(first, friday).key != Occurrence(second, friday).key)
        // A weekly pause named like one is not a quick pause
        assertFalse(QuickPause.isQuick(Pause("quickie", "Caffè", 600, 615)))
    }

    @Test
    fun quickPause_countsInTheHistory() {
        val quick = QuickPause.create(friday.atTime(10, 0), 30)
        val config = FreeTimeLedger.Config(true, apps, listOf(quick), emptySet())
        val days = FreeTimeLedger.settle(emptyMap(), friday.atTime(9, 0), friday.atTime(12, 0), config, sun = { 7 * 60 to 18 * 60 })
        assertEquals(30, days.getValue(friday).livedMinutes)
        assertEquals(30, days.getValue(friday).byPause[quick.id])
    }

    @Test
    fun quickPause_lengthIsClampedBelowADay() {
        val quick = QuickPause.create(friday.atTime(9, 0), 5000)
        assertFalse(quick.isWholeDay)
        assertEquals(9 * 60 - 1, quick.end)
    }

    @Test
    fun untilNext_handsOverToTheNextBreak() {
        val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet())
        val now = LocalDateTime.of(friday, java.time.LocalTime.of(11, 5, 40))
        val minutes = QuickPause.minutesToNext(listOf(lunch), now, emptySet())
        assertEquals(115, minutes)
        val quick = QuickPause.create(now, minutes!!)
        // No gap: the quick pause ends as lunch starts, so work comes back at 14:00
        assertEquals(friday.atTime(14, 0), PauseSchedule.blockedUntil(listOf(lunch, quick), now))
        // Too far ahead: no option
        assertNull(QuickPause.minutesToNext(listOf(lunch), friday.atTime(14, 30), emptySet()))
    }

    @Test
    fun codec_keepsTheDate_andReadsOldLines() {
        val quick = QuickPause.create(friday.atTime(9, 0), 30)
        assertEquals(listOf(quick), PauseCodec.decode(PauseCodec.encode(listOf(quick))))
        val old = "lunch\tPranzo\t780\t840\t31\t1\t*"
        assertNull(PauseCodec.decode(old).single().date)
    }
}
