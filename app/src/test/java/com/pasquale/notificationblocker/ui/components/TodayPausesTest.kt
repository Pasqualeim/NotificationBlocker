package com.pasquale.notificationblocker.ui.components

import com.pasquale.notificationblocker.data.Pause
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class TodayPausesTest {

    // 2026-10-15 is a Thursday
    private val now = LocalDateTime.of(2026, 10, 15, 19, 0)
    private val evening = Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)
    private val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60)
    private val weekend = Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

    @Test
    fun todaysStretches_inTimeOrder_withTheirState() {
        val today = TodayPauses.of(listOf(lunch, evening, weekend), now, emptySet())
        assertEquals(listOf(0 to 510, 780 to 840, 1110 to 1440), today.map { it.startMinute to it.endMinute })
        assertEquals(listOf(TodayPauseState.LIVED, TodayPauseState.LIVED, TodayPauseState.NOW), today.map { it.state })
    }

    @Test
    fun endedEarly_andLater() {
        val early = LocalDateTime.of(2026, 10, 15, 12, 0)
        assertEquals(TodayPauseState.LATER, TodayPauses.of(listOf(lunch), early, emptySet()).single().state)
        val ended = TodayPauses.of(listOf(evening), now, setOf("2026-10-15#evening")).last()
        assertEquals(TodayPauseState.ENDED, ended.state)
    }

    @Test
    fun oneLinePerPause_keepsTheOneOnNow() {
        val lines = TodayPauses.oneEach(TodayPauses.of(listOf(lunch, evening), now, emptySet()))
        assertEquals(listOf("lunch", "evening"), lines.map { it.pause.id })
        assertEquals(TodayPauseState.NOW, lines.last().state)
    }

    @Test
    fun disabledPauses_areLeftOut() =
        assertEquals(emptyList<TodayPause>(), TodayPauses.of(listOf(lunch.copy(enabled = false)), now, emptySet()))

    @Test
    fun overnightPauseOnNow_countsTheWholeOccurrence() {
        val evening = Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)
        // 20:00: 12 h 30 left of a 14 h pause, though only 18:30-24:00 is on today's bar
        val beforeMidnight = TodayPauses.of(listOf(evening), LocalDateTime.of(2026, 10, 15, 20, 0), emptySet())
            .single { it.state == TodayPauseState.NOW }
        assertEquals(14 * 60, beforeMidnight.totalMinutes)
        assertEquals(12 * 60 + 30, beforeMidnight.minutesLeft)
        // 02:00: last night's pause, 6 h 30 left
        val afterMidnight = TodayPauses.of(listOf(evening), LocalDateTime.of(2026, 10, 16, 2, 0), emptySet())
            .single { it.state == TodayPauseState.NOW }
        assertEquals(14 * 60, afterMidnight.totalMinutes)
        assertEquals(6 * 60 + 30, afterMidnight.minutesLeft)
    }
}
