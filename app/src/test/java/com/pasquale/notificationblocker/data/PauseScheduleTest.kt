package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime

class PauseScheduleTest {

    private val work = "com.work.app"
    private val other = "com.other.app"

    // 2026-10-16 is a Friday
    private fun fri(h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, 16, h, m)
    private fun sat(h: Int, m: Int = 0) = fri(h, m).plusDays(1)

    private fun t(h: Int, m: Int = 0) = h * 60 + m

    private val evening = Pause("evening", "Evening", t(22), t(7))
    private val lunch = Pause("lunch", "Lunch", t(13), t(14), days = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
    private val weekend = Pause("weekend", "Weekend", 0, 0, days = setOf(SATURDAY, SUNDAY))

    private fun block(
        now: LocalDateTime,
        pkg: String = work,
        pauses: List<Pause> = listOf(evening),
        enabled: Boolean = true,
        apps: Set<String> = setOf(work),
        ended: Set<String> = emptySet(),
    ) = PauseSchedule.shouldBlock(pkg, enabled, apps, pauses, now, ended)

    @Test
    fun blocksWorkAppInsideAPause() = assertTrue(block(fri(23)))

    @Test
    fun masterSwitchOff_neverBlocks() = assertFalse(block(fri(23), enabled = false))

    @Test
    fun outsideEveryPause_doesNotBlock() = assertFalse(block(fri(12)))

    @Test
    fun otherApps_andNoWorkApps_areNotBlocked() {
        assertFalse(block(fri(23), pkg = other))
        assertFalse(block(fri(23), apps = emptySet()))
    }

    @Test
    fun boundaries_areHalfOpen() {
        assertTrue(block(fri(22)))
        assertTrue(block(sat(6, 59)))
        assertFalse(block(sat(7)))
        assertFalse(block(fri(21, 59)))
    }

    @Test
    fun overnightPause_belongsToTheDayItStarted() {
        val friday = evening.copy(days = setOf(FRIDAY))
        assertTrue(block(sat(3), pauses = listOf(friday)))
        // Saturday's own evening is not set: nothing after Saturday 07:00
        assertFalse(block(sat(23), pauses = listOf(friday)))
        assertEquals("2026-10-16#evening", PauseSchedule.occurrenceAt(friday, sat(3))!!.key)
    }

    @Test
    fun days_limitWhenAPauseHappens() {
        assertTrue(block(fri(13, 30), pauses = listOf(lunch)))
        assertFalse(block(sat(13, 30), pauses = listOf(lunch)))
    }

    @Test
    fun wholeDayPause_coversItsCalendarDays() {
        assertTrue(block(sat(0), pauses = listOf(weekend)))
        assertTrue(block(sat(12), pauses = listOf(weekend)))
        assertFalse(block(fri(23, 59), pauses = listOf(weekend)))
        assertFalse(block(sat(12).plusDays(2), pauses = listOf(weekend)))
    }

    @Test
    fun disabledPause_neverHappens() = assertFalse(block(fri(23), pauses = listOf(evening.copy(enabled = false))))

    @Test
    fun perPauseApps_overrideTheWorkApps() {
        val mailOnly = lunch.copy(apps = setOf(other))
        assertTrue(block(fri(13, 30), pkg = other, pauses = listOf(mailOnly)))
        assertFalse(block(fri(13, 30), pkg = work, pauses = listOf(mailOnly)))
    }

    @Test
    fun overlappingPauses_holdIfAnyHolds() {
        val pauses = listOf(evening, weekend.copy(apps = setOf(other)))
        // Saturday 23:00: the evening holds the work app, the weekend the other one
        assertTrue(block(sat(23), pkg = work, pauses = pauses))
        assertTrue(block(sat(23), pkg = other, pauses = pauses))
        // Saturday noon: only the weekend
        assertFalse(block(sat(12), pkg = work, pauses = pauses))
    }

    @Test
    fun endedEarly_letsWorkThroughUntilTheNextOccurrence() {
        val ended = setOf("2026-10-16#evening")
        assertFalse(block(fri(23), ended = ended))
        assertFalse(block(sat(6), ended = ended))
        assertTrue(block(sat(22), ended = ended))
    }

    @Test
    fun endingOnePause_leavesTheOverlappingOneOn() {
        val pauses = listOf(evening, weekend)
        assertTrue(block(sat(23), pauses = pauses, ended = setOf("2026-10-17#evening")))
    }

    @Test
    fun blockedUntil_followsPausesThatTouch() {
        // Friday evening ends Saturday 07:00, but the weekend already started at midnight
        val until = PauseSchedule.blockedUntil(listOf(evening.copy(days = setOf(FRIDAY)), weekend), fri(23))
        assertEquals(sat(0).plusDays(2), until)
        assertEquals(sat(7), PauseSchedule.blockedUntil(listOf(evening), fri(23)))
        assertNull(PauseSchedule.blockedUntil(listOf(evening), fri(12)))
    }

    @Test
    fun blockedUntil_isNullForAPauseThatNeverStops() =
        assertNull(PauseSchedule.blockedUntil(listOf(Pause("all", "", t(8), t(8))), fri(12)))

    @Test
    fun nextStart_skipsThePauseInProgressAndTheEndedOnes() {
        assertEquals(fri(22), PauseSchedule.nextStart(listOf(evening), fri(12)))
        assertEquals(sat(22), PauseSchedule.nextStart(listOf(evening), fri(23), setOf("2026-10-16#evening")))
        // Lunch on Friday 15:00: the next one is on Monday
        assertEquals(fri(13).plusDays(3), PauseSchedule.nextStart(listOf(lunch), fri(15)))
        assertNull(PauseSchedule.nextStart(listOf(evening.copy(enabled = false)), fri(12)))
    }

    @Test
    fun codec_roundTrips_andSkipsBrokenLines() {
        val pauses = listOf(evening, lunch.copy(name = "Pranzo\tlungo", apps = setOf(work, other)), weekend.copy(enabled = false))
        val decoded = PauseCodec.decode(PauseCodec.encode(pauses) + "\nbroken line")
        assertEquals(3, decoded.size)
        assertEquals(evening, decoded[0])
        assertEquals("Pranzo lungo", decoded[1].name)
        assertEquals(setOf(work, other), decoded[1].apps)
        assertEquals(weekend.copy(enabled = false), decoded[2])
    }

    @Test
    fun occurrenceDate_readsNewAndLegacyKeys() {
        assertEquals(fri(0).toLocalDate(), Occurrence.dateOf("2026-10-16#evening"))
        assertEquals(fri(0).toLocalDate(), Occurrence.dateOf("2026-10-16"))
        assertNull(Occurrence.dateOf("garbage"))
    }

    @Test
    fun untilMinutes_saysAllDayForMidnightAndForADayOrMore() {
        assertEquals(t(7), PauseSchedule.untilMinutes(listOf(evening), fri(23), sat(7)))
        // A whole-day pause ends at midnight, the weekend on Monday: "all day"
        assertNull(PauseSchedule.untilMinutes(listOf(weekend), sat(10), sat(0).plusDays(1)))
        assertNull(PauseSchedule.untilMinutes(listOf(weekend), fri(23), sat(0).plusDays(2)))
        assertNull(PauseSchedule.untilMinutes(listOf(evening), fri(12), null))
        // An evening that ends at midnight still says "until 00:00"
        assertEquals(0, PauseSchedule.untilMinutes(listOf(Pause("late", "", t(20), 0)), fri(21), sat(0)))
    }

    @Test
    fun millisUntil_countsRealTimeAcrossDaylightSaving() {
        val rome = java.time.ZoneId.of("Europe/Rome")
        // 2026-10-25: clocks go back at 03:00, the night is one hour longer
        val start = LocalDateTime.of(2026, 10, 24, 22, 0)
        assertEquals(10 * 3_600_000L, PauseSchedule.millisUntil(start, start.plusHours(9), rome))
    }

    @Test
    fun counterOccurrence_isTheFirstPauseThatHoldsSomething() {
        val nothing = Pause("a", "", t(21), t(23), apps = emptySet())
        val pauses = listOf(nothing, evening)
        assertEquals("2026-10-16#evening", PauseSchedule.counterOccurrence(pauses, setOf(work), fri(22, 30), emptySet())!!.key)
    }
}
