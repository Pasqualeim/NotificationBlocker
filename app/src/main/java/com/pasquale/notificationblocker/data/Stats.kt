package com.pasquale.notificationblocker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** The three views of "Your time". */
enum class StatsRange { DAY, WEEK, MONTH }

/**
 * One period of the history: every day in it (empty days included, so charts get a slot for each) and
 * the totals. Pure (unit tested).
 */
data class PeriodStats(val range: StatsRange, val first: LocalDate, val days: List<DayStats>) {
    val last: LocalDate get() = days.last().date
    val livedMinutes: Int get() = days.sumOf { it.livedMinutes }
    val daylightMinutes: Int get() = days.sumOf { it.daylightMinutes }
    val held: Int get() = days.sumOf { it.held }

    companion object {
        /** The period of [range] that contains [anchor]: the day, its week (Monday first) or its month. */
        fun of(range: StatsRange, anchor: LocalDate, history: Map<LocalDate, DayStats>): PeriodStats {
            val first = firstDay(range, anchor)
            val last = when (range) {
                StatsRange.DAY -> first
                StatsRange.WEEK -> first.plusDays(6)
                StatsRange.MONTH -> first.with(TemporalAdjusters.lastDayOfMonth())
            }
            val days = generateSequence(first) { it.plusDays(1) }
                .takeWhile { !it.isAfter(last) }
                .map { history[it] ?: DayStats(it) }
                .toList()
            return PeriodStats(range, first, days)
        }

        fun firstDay(range: StatsRange, anchor: LocalDate): LocalDate = when (range) {
            StatsRange.DAY -> anchor
            StatsRange.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            StatsRange.MONTH -> anchor.withDayOfMonth(1)
        }

        /** [anchor] moved by [steps] periods of [range] (negative: back in time). */
        fun shift(range: StatsRange, anchor: LocalDate, steps: Long): LocalDate = when (range) {
            StatsRange.DAY -> anchor.plusDays(steps)
            StatsRange.WEEK -> anchor.plusWeeks(steps)
            StatsRange.MONTH -> anchor.plusMonths(steps)
        }
    }
}

/**
 * A stretch of one day lived on a break, for the day chart: [startMinute] and [endMinute] are minutes from
 * midnight of that day (0 to 1440). They come from the times kept in the history ([DayStats.spans]); days
 * recorded before those were kept only have minutes per pause, so their stretches are estimated from the
 * pause times (pauses with minutes lived that day, today up to [now], the hours asleep cut out).
 */
data class DaySpan(val pause: Pause, val startMinute: Int, val endMinute: Int)

object DaySpans {

    fun of(day: DayStats, pauses: List<Pause>, now: LocalDateTime, sleep: SleepWindow? = null): List<DaySpan> {
        val dayStart = day.date.atStartOfDay()
        val dayEnd = dayStart.plusDays(1)
        val limit = if (now.isBefore(dayEnd)) maxOf(now, dayStart) else dayEnd
        // shortcut: the day of the update has minutes from before the times were kept, so its chart shows
        // only the part after it; once, for one day, so not worth a migration
        if (day.spans.isNotEmpty()) {
            // A deleted pause has no name or color left: its stretches are dropped, as in the legend
            val byId = pauses.associateBy { it.id }
            return day.spans.mapNotNull { s -> byId[s.pauseId]?.let { DaySpan(it, s.start, s.end) } }
        }
        return pauses.filter { (day.byPause[it.id] ?: 0) > 0 }.flatMap { pause ->
            listOf(day.date.minusDays(1), day.date)
                .filter { pause.startsOn(it) }
                .map { Occurrence(pause, it) }
                .flatMap { o ->
                    val a = maxOf(o.start, dayStart)
                    val b = minOf(o.end, limit)
                    if (!b.isAfter(a)) return@flatMap emptyList()
                    val from = minutesFrom(dayStart, a)
                    val to = minutesFrom(dayStart, b)
                    (sleep?.awake(from, to) ?: listOf(from to to)).map { (x, y) -> DaySpan(pause, x, y) }
                }
        }.sortedBy { it.startMinute }
    }

    private fun minutesFrom(start: LocalDateTime, t: LocalDateTime) =
        java.time.Duration.between(start, t).toMinutes().toInt().coerceIn(0, OffHours.MINUTES_PER_DAY)
}
