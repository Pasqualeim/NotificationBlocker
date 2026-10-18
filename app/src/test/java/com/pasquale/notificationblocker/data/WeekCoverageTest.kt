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
        assertEquals(listOf(CoverageSegment(1110, 1440, 1)), week.days[DayOfWeek.FRIDAY])
        assertEquals(listOf(CoverageSegment(0, 510, 1)), week.days[DayOfWeek.SATURDAY])
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
        assertTrue(week.days.getValue(DayOfWeek.MONDAY).none { it.pauseIndex == 3 })
        // 5 weekdays of 14 h, plus 2 whole days
        assertEquals(5 * 14 * 60 + 2 * 1440, week.weeklyMinutes)
    }

    @Test
    fun sleep_isLeftOutOfTheWeeklyHours() {
        val pauses = listOf(
            Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30),
            Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = weekdays),
            Pause("weekend", "Weekend", 0, 0, days = weekend),
        )
        val week = WeekCoverage.of(pauses, SleepWindow(23 * 60, 7 * 60))
        // Weekdays 07:00-08:30, lunch and 18:30-23:00 (7 h); weekend days 16 h awake: 67 h, not 123
        assertEquals(5 * 7 * 60 + 2 * 16 * 60, week.weeklyMinutes)
        // The bars still show what is held, sleep included
        assertEquals(1440, unionOf(week, DayOfWeek.SATURDAY))
    }

    @Test
    fun pauseEndingAtMidnight_addsNoMorningStretch() {
        val late = Pause("late", "Tardi", 22 * 60, 0, days = setOf(DayOfWeek.MONDAY))
        val week = WeekCoverage.of(listOf(late))
        assertTrue(week.days.getValue(DayOfWeek.TUESDAY).isEmpty())
        assertEquals(120, week.weeklyMinutes)
    }

    @Test
    fun groups_joinTheDaysThatLookTheSame() {
        val evening = Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)
        val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet())
        val weekend = Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        val groups = WeekCoverage.of(listOf(evening, lunch, weekend)).groups
        assertEquals(2, groups.size)
        assertEquals(DayOfWeek.entries.take(5), groups[0].days)
        // Morning of last night's evening, lunch, tonight's evening
        assertEquals(listOf(0 to 510, 780 to 840, 1110 to 1440), groups[0].stretches.map { it.start to it.end })
        assertEquals(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), groups[1].days)
        // The whole day swallows the evening and the morning: one stretch, in the weekend's color
        assertEquals(listOf(0 to 1440), groups[1].stretches.map { it.start to it.end })
        assertEquals(3, groups[1].stretches.single().pauseIndex)
    }

    @Test
    fun merge_joinsTouchingStretches_andKeepsTheLongestPause() {
        val merged = WeekCoverage.merge(
            listOf(CoverageSegment(600, 780, 1), CoverageSegment(480, 600, 0), CoverageSegment(900, 960, 2)),
        )
        assertEquals(listOf(CoverageSegment(480, 780, 1), CoverageSegment(900, 960, 2)), merged)
    }

    @Test
    fun noPauses_isOneGroupWithNothingFree() {
        val groups = WeekCoverage.of(emptyList()).groups
        assertEquals(1, groups.size)
        assertEquals(7, groups[0].days.size)
        assertTrue(groups[0].stretches.isEmpty())
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
