package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WeekCoverageTest {

    private val weekdays = DayOfWeek.entries.take(5).toSet()
    private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    @Test
    fun overnightPause_splitsAcrossTwoDays() {
        val evening = Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30, days = setOf(DayOfWeek.FRIDAY))
        val week = WeekCoverage.of(listOf(evening))
        assertEquals(listOf(CoverageSegment(1110, 1440, 0)), week.days[DayOfWeek.FRIDAY])
        assertEquals(listOf(CoverageSegment(0, 510, 0)), week.days[DayOfWeek.SATURDAY])
        assertEquals(330 + 510, week.weeklyMinutes)
    }

    @Test
    fun overlaps_countOnce_andDisabledPausesAreLeftOut() {
        val pauses = listOf(
            Pause("evening", "Sera", 18 * 60, 8 * 60),
            Pause("weekend", "Weekend", 0, 0, days = weekend),
            Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = weekdays, enabled = false),
        )
        val week = WeekCoverage.of(pauses)
        // Saturday: the whole day, Friday night's morning and Saturday evening all inside it
        assertEquals(1440, unionOf(week, DayOfWeek.SATURDAY))
        assertTrue(week.days.getValue(DayOfWeek.MONDAY).none { it.pauseIndex == 2 })
        // 5 weekdays of 14 h, plus 2 whole days
        assertEquals(5 * 14 * 60 + 2 * 1440, week.weeklyMinutes)
    }

    @Test
    fun pauseEndingAtMidnight_addsNoMorningStretch() {
        val late = Pause("late", "Tardi", 22 * 60, 0, days = setOf(DayOfWeek.MONDAY))
        val week = WeekCoverage.of(listOf(late))
        assertTrue(week.days.getValue(DayOfWeek.TUESDAY).isEmpty())
        assertEquals(120, week.weeklyMinutes)
    }

    private fun unionOf(week: WeekCoverage, day: DayOfWeek): Int {
        var total = 0
        var reach = 0
        for (s in week.days.getValue(day).sortedBy { it.start }) {
            val from = maxOf(s.start, reach)
            if (s.end > from) {
                total += s.end - from
                reach = s.end
            }
        }
        return total
    }
}
