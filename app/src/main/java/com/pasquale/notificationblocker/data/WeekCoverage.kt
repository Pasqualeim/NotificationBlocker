package com.pasquale.notificationblocker.data

import java.time.DayOfWeek

/** A stretch of one weekday covered by a pause: minutes [start, end) of that calendar day. */
data class CoverageSegment(val start: Int, val end: Int, val pauseIndex: Int)

/**
 * The typical week drawn by the Breaks tab: for each weekday, the stretches covered by the enabled
 * pauses (an overnight pause shows on the day it starts and on the morning after), and the minutes
 * of the week covered by at least one pause, overlaps counted once.
 */
data class WeekCoverage(val days: Map<DayOfWeek, List<CoverageSegment>>, val weeklyMinutes: Int) {

    companion object {
        private const val DAY = OffHours.MINUTES_PER_DAY

        /** [CoverageSegment.pauseIndex] is the pause's place in [pauses], so it matches its color. */
        fun of(pauses: List<Pause>): WeekCoverage {
            val days = DayOfWeek.entries.associateWith { day ->
                val yesterday = day.minus(1)
                pauses.flatMapIndexed { index, p ->
                    if (!p.enabled) return@flatMapIndexed emptyList()
                    buildList {
                        if (day in p.days) {
                            when {
                                p.isWholeDay -> add(CoverageSegment(0, DAY, index))
                                p.start < p.end -> add(CoverageSegment(p.start, p.end, index))
                                else -> add(CoverageSegment(p.start, DAY, index))
                            }
                        }
                        // The morning part of last night's pause
                        if (yesterday in p.days && !p.isWholeDay && p.start > p.end && p.end > 0) {
                            add(CoverageSegment(0, p.end, index))
                        }
                    }
                }.sortedBy { it.start }
            }
            return WeekCoverage(days, days.values.sumOf { unionLength(it) })
        }

        private fun unionLength(segments: List<CoverageSegment>): Int {
            var total = 0
            var reach = 0
            for (s in segments.sortedBy { it.start }) {
                val from = maxOf(s.start, reach)
                if (s.end > from) {
                    total += s.end - from
                    reach = s.end
                }
            }
            return total
        }
    }
}
