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
import java.time.LocalDateTime

class WorkScheduleTest {

    private val pauses = WorkSchedule.DEFAULT.offWorkPauses()
    private fun held(at: LocalDateTime) = PauseSchedule.activeAt(pauses, at).isNotEmpty()

    // 2026-10-16 is a Friday
    private fun fri(h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, 16, h, m)

    @Test
    fun mondayToFriday_nineToSix_holdsEverythingElse() {
        assertFalse(held(fri(10)))
        assertTrue(held(fri(18)))
        assertTrue(held(fri(23)))
        // The whole weekend, Friday night to Monday morning
        assertTrue(held(fri(12).plusDays(1)))
        assertTrue(held(fri(3).plusDays(2)))
        assertTrue(held(fri(8, 59).plusDays(3)))
        assertFalse(held(fri(9).plusDays(3)))
        // Tuesday early morning: Monday's evening
        assertTrue(held(fri(7).plusDays(4)))
    }

    @Test
    fun theWeekend_isOnePause_fromFridayEvening_toMondayMorning() {
        assertEquals(listOf("work_off", "work_off3"), pauses.map { it.id })
        assertTrue(pauses.all { WorkSchedule.isOffWork(it) })
        val saturday = PauseSchedule.activeAt(pauses, fri(12).plusDays(1)).single()
        assertEquals("2026-10-16#work_off3", saturday.key)
        assertEquals(fri(9).plusDays(3), saturday.end)
        // "End the pause" on Friday evening lets work through until Monday 09:00, not until midnight
        val ended = setOf("2026-10-16#work_off3")
        assertTrue(PauseSchedule.activeAt(pauses, fri(12).plusDays(2), ended).isEmpty())
        assertTrue(PauseSchedule.activeAt(pauses, fri(18).plusDays(3), ended).isNotEmpty())
        // The week card still fills the weekend
        val week = WeekCoverage.of(pauses)
        assertEquals(listOf(0 to 1440), week.groups.last().stretches.map { it.start to it.end })
    }

    @Test
    fun noWorkDays_isAllFree_andOddHoursKeepOnlyTheFreeDays() {
        assertEquals(listOf(WorkSchedule.OFF_WORK), WorkSchedule(emptySet(), 540, 1080).offWorkPauses().map { it.id })
        val odd = WorkSchedule(setOf(MONDAY, FRIDAY), 1080, 540).offWorkPauses()
        assertEquals(1, odd.size)
        assertEquals(setOf(MONDAY, FRIDAY), Pause.ALL_DAYS - odd.single().days)
    }

    @Test
    fun aSingleWorkDay_isFollowedByAWeekOff() {
        val monday = WorkSchedule(setOf(MONDAY), 540, 1080).offWorkPauses().single()
        assertEquals(7, monday.nights)
    }

    @Test
    fun codec_roundTrips_andRejectsBrokenText() {
        assertEquals(WorkSchedule.DEFAULT, WorkSchedule.decode(WorkSchedule.encode(WorkSchedule.DEFAULT)))
        assertEquals(WorkSchedule(emptySet(), 540, 1080), WorkSchedule.decode("|540|1080"))
        assertNull(WorkSchedule.decode(null))
        assertNull(WorkSchedule.decode("1,9|540|1080"))
        assertNull(WorkSchedule.decode("1,2|540"))
    }

    @Test
    fun theWeekend_countsOnItsDays_andMondayShowsOneLine() {
        // Friday 18:00 to Monday 09:00, asleep 23:00-07:00: Sat and Sun 16 h each, Friday 5 h, Monday 2 h
        val config = FreeTimeLedger.Config(true, setOf("work"), pauses, emptySet(), SleepWindow(23 * 60, 7 * 60))
        val days = FreeTimeLedger.settle(emptyMap(), fri(17), fri(10).plusDays(3), config, sun = { 7 * 60 to 18 * 60 })
        assertEquals(listOf(5 * 60, 16 * 60, 16 * 60, 2 * 60), (0..3).map { days[fri(0).toLocalDate().plusDays(it.toLong())]!!.livedMinutes })
        // Monday on Today: this morning's end of the weekend and tonight's evening are one line
        val monday = com.pasquale.notificationblocker.ui.components.TodayPauses.of(pauses, fri(12).plusDays(3), emptySet())
        assertEquals(2, monday.size)
        assertEquals(1, com.pasquale.notificationblocker.ui.components.TodayPauses.oneEach(monday).size)
    }
}
