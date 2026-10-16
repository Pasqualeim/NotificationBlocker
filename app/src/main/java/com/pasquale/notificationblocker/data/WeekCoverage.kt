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

    /**
     * Weekdays that look the same, Monday first: in a typical week most of them do, so the card draws
     * one bar per group ("Mon-Fri", "Sat-Sun") instead of seven near-identical rows.
     *
     * @param segments the pauses of the first of those days, as they cover it
     * @param stretches the same day as the times without work notifications: overlapping or touching
     *   stretches merged (see [merge]), each with one pause for its color
     */
    data class DayGroup(val days: List<DayOfWeek>, val segments: List<CoverageSegment>, val stretches: List<CoverageSegment>)

    val groups: List<DayGroup>
        // Days that read the same (same free times) share a bar, even if different pauses make them;
        // the first of them gives the colors
        get() = DayOfWeek.entries
            .groupBy { day -> merge(days.getValue(day)).map { it.start to it.end } }
            .values
            .map { sameDays -> days.getValue(sameDays.first()).let { DayGroup(sameDays, it, merge(it)) } }
            .sortedBy { it.days.first() }

    companion object {
        private const val DAY = OffHours.MINUTES_PER_DAY

        /** [CoverageSegment.pauseIndex] is the pause's place in [pauses], so it matches its color. */
        fun of(pauses: List<Pause>): WeekCoverage {
            val days = DayOfWeek.entries.associateWith { day ->
                val yesterday = day.minus(1)
                pauses.flatMapIndexed { index, p ->
                    // One-off pauses (a day off, "Pause now") are not part of the usual week
                    if (!p.enabled || p.date != null) return@flatMapIndexed emptyList()
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

        /**
         * Overlapping or touching segments joined, in time order. Each keeps the pause of its longest
         * part (the first one on a tie): a Saturday off reads as the weekend, not as Friday's evening
         * that runs into its morning.
         */
        fun merge(segments: List<CoverageSegment>): List<CoverageSegment> {
            val groups = mutableListOf<MutableList<CoverageSegment>>()
            for (s in segments.sortedBy { it.start }) {
                val last = groups.lastOrNull()
                if (last != null && s.start <= last.maxOf { it.end }) last += s else groups += mutableListOf(s)
            }
            return groups.map { parts ->
                CoverageSegment(parts.first().start, parts.maxOf { it.end }, parts.maxBy { it.end - it.start }.pauseIndex)
            }
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
