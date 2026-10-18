package com.pasquale.notificationblocker.data

import java.time.DayOfWeek

/** A stretch of one weekday covered by a pause: minutes [start, end) of that calendar day. */
data class CoverageSegment(val start: Int, val end: Int, val pauseIndex: Int)

/**
 * The typical week drawn by the Breaks tab: for each weekday, the stretches covered by the enabled
 * pauses (an overnight pause shows on the day it starts and on the morning after), and the awake
 * minutes of the week covered by at least one pause, overlaps counted once, [sleep] left out.
 */
data class WeekCoverage(
    val days: Map<DayOfWeek, List<CoverageSegment>>,
    val weeklyMinutes: Int,
    val sleep: SleepWindow? = null,
) {

    /**
     * Weekdays that look the same, Monday first: in a typical week most of them do, so the card draws
     * one bar per group ("Mon-Fri", "Sat-Sun") instead of seven near-identical rows.
     *
     * @param stretches the first of those days as the times without work notifications: overlapping or
     *   touching stretches merged (see [merge]), each with one pause for its color
     */
    data class DayGroup(val days: List<DayOfWeek>, val stretches: List<CoverageSegment>)

    val groups: List<DayGroup>
        // Days that read the same (same free times) share a bar, even if different pauses make them;
        // the first of them gives the colors
        get() {
            val merged = DayOfWeek.entries.associateWith { merge(days.getValue(it)) }
            return DayOfWeek.entries
                .groupBy { day -> merged.getValue(day).map { it.start to it.end } }
                .values
                .map { sameDays -> DayGroup(sameDays, merged.getValue(sameDays.first())) }
                .sortedBy { it.days.first() }
        }

    companion object {
        private const val DAY = OffHours.MINUTES_PER_DAY

        /** [CoverageSegment.pauseIndex] is the pause's color slot ([WorkSchedule.colorSlots]). */
        fun of(pauses: List<Pause>, sleep: SleepWindow? = null): WeekCoverage {
            val slots = WorkSchedule.colorSlots(pauses)
            val days = DayOfWeek.entries.associateWith { day ->
                pauses.flatMap { p ->
                    val index = slots.getValue(p.id)
                    // One-off pauses (a day off, "Pause now") are not part of the usual week
                    if (!p.enabled || p.date != null) return@flatMap emptyList()
                    buildList {
                        if (day in p.days) {
                            when {
                                p.isWholeDay -> add(CoverageSegment(0, DAY, index))
                                p.start < p.end -> add(CoverageSegment(p.start, p.end, index))
                                else -> add(CoverageSegment(p.start, DAY, index))
                            }
                        }
                        if (!p.isWholeDay && p.start > p.end) {
                            // The days in between of a pause of several nights (Friday evening to Monday)
                            for (k in 1 until p.nights) if (day.minus(k.toLong()) in p.days) add(CoverageSegment(0, DAY, index))
                            // The morning part of the night it ends
                            if (day.minus(p.nights.toLong()) in p.days && p.end > 0) add(CoverageSegment(0, p.end, index))
                        }
                    }
                }.sortedBy { it.start }
            }
            val awake = days.values.sumOf { segments ->
                merge(segments).sumOf { s ->
                    sleep?.awake(s.start, s.end)?.sumOf { (a, b) -> b - a } ?: (s.end - s.start)
                }
            }
            return WeekCoverage(days, awake, sleep)
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
    }
}
