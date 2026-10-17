package com.pasquale.notificationblocker.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * One calendar day of the history: the base of the statistics and of the hedgehog's score.
 *
 * @param livedMinutes minutes the pause was really on (see [FreeTimeLedger])
 * @param daylightMinutes the part of [livedMinutes] between sunrise and sunset
 * @param held work notifications held back that day, each counted once
 * @param byPause [livedMinutes] split by pause id, for "how much of each pause you lived"
 */
data class DayStats(
    val date: LocalDate,
    val livedMinutes: Int = 0,
    val daylightMinutes: Int = 0,
    val held: Int = 0,
    val byPause: Map<String, Int> = emptyMap(),
)

/**
 * How much break time the user has lived, day by day. Pure logic (unit tested).
 *
 * A minute counts when the pause was really on: blocking enabled and a pause in progress, not ended early,
 * with apps to silence, and the user awake (outside [Config.sleep]). Opening a work app during the break
 * costs nothing (Nook does not see it); "End the pause" only stops the count from that moment. A minute
 * belongs to the calendar day it falls on, so an overnight pause is split between two days; when pauses
 * overlap it counts once, for the one that started first.
 *
 * Still no alarms: [PreferencesManager] settles the time between two looks at the clock ([settle]) before
 * every change of the settings, and whenever the app or the listener service refreshes. Between two looks
 * the settings did not change, so the minutes can be computed from the schedule alone.
 */
object FreeTimeLedger {

    /**
     * Longer gaps between two looks only count their last part. Three days cover a weekend with the phone
     * asleep; a longer gap is more likely the phone off or a backup restored on a new phone.
     */
    private val MAX_GAP: Duration = Duration.ofDays(3)

    /** The settings that decide whether a minute counts; constant between two looks at the clock. */
    data class Config(
        val blockingEnabled: Boolean,
        val workApps: Set<String>,
        val pauses: List<Pause>,
        val ended: Set<String>,
        /** Asleep minutes are left out: the pause still holds work, but sleep is not time won back. */
        val sleep: SleepWindow? = null,
    )

    /**
     * [days] with the minutes lived between [from] and [to] added. A minute counts once it is over, so
     * frequent looks at the clock add up to the same total as rare ones.
     *
     * Works on intervals, not minute by minute: the stretch is cut at every pause start and end, every
     * midnight, sunrise and sunset, and each piece is credited in one go. Lengths are real time in [zone],
     * so the hour skipped or repeated by daylight saving counts as it was lived.
     *
     * @param sun sunrise and sunset of a date, in minutes from midnight
     */
    fun settle(
        days: Map<LocalDate, DayStats>,
        from: LocalDateTime,
        to: LocalDateTime,
        config: Config,
        sun: (LocalDate) -> Pair<Int, Int> = { SunTimes.of(it) },
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, DayStats> {
        // Whole minutes only: the minute under way at [from] counts here, the one under way at [to] next time
        val start = maxOf(from, to.minus(MAX_GAP)).truncatedTo(ChronoUnit.MINUTES)
        val end = to.truncatedTo(ChronoUnit.MINUTES)
        if (!config.blockingEnabled || !end.isAfter(start)) return days

        // Occurrences that hold something and touch the stretch, earliest start first
        val occurrences = config.pauses.filter { it.enabled }.flatMap { pause ->
            generateSequence(start.toLocalDate().minusDays(1)) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end.toLocalDate()) }
                .filter { pause.startsOn(it) }
                .map { Occurrence(pause, it) }
                .toList()
        }.filter {
            it.key !in config.ended && it.pause.appsOrDefault(config.workApps).isNotEmpty() &&
                it.end.isAfter(start) && it.start.isBefore(end)
        }.sortedWith(compareBy<Occurrence> { it.start }.thenBy { it.pause.id })
        if (occurrences.isEmpty()) return days

        val cuts = sortedSetOf(start, end)
        for (o in occurrences) {
            cuts += o.start
            cuts += o.end
        }
        var date = start.toLocalDate()
        while (!date.isAfter(end.toLocalDate())) {
            val (sunrise, sunset) = sun(date)
            cuts += date.atStartOfDay()
            cuts += date.atStartOfDay().plusMinutes(sunrise.toLong())
            cuts += date.atStartOfDay().plusMinutes(sunset.toLong())
            config.sleep?.ofDay?.forEach { (a, b) ->
                cuts += date.atStartOfDay().plusMinutes(a.toLong())
                cuts += date.atStartOfDay().plusMinutes(b.toLong())
            }
            date = date.plusDays(1)
        }

        val result = days.toMutableMap()
        val points = cuts.filter { !it.isBefore(start) && !it.isAfter(end) }
        for ((a, b) in points.zipWithNext()) {
            // Overlapping pauses: the piece goes to the one that started first
            val pause = occurrences.firstOrNull { !a.isBefore(it.start) && a.isBefore(it.end) }?.pause ?: continue
            val ofDay = a.hour * 60 + a.minute
            if (config.sleep != null && ofDay in config.sleep) continue
            val minutes = Duration.between(a.atZone(zone), b.atZone(zone)).toMinutes().toInt()
            if (minutes <= 0) continue
            val day = result[a.toLocalDate()] ?: DayStats(a.toLocalDate())
            val (sunrise, sunset) = sun(day.date)
            result[day.date] = day.copy(
                livedMinutes = day.livedMinutes + minutes,
                daylightMinutes = day.daylightMinutes + if (ofDay in sunrise until sunset) minutes else 0,
                byPause = day.byPause + (pause.id to (day.byPause[pause.id] ?: 0) + minutes),
            )
        }
        return result
    }

    /** [days] with one more work notification held on [date]. */
    fun addHeld(days: Map<LocalDate, DayStats>, date: LocalDate): Map<LocalDate, DayStats> {
        val day = days[date] ?: DayStats(date)
        return days + (date to day.copy(held = day.held + 1))
    }
}

/**
 * The history as one line per day in the preferences (about 30 KB a year): date, lived, daylight, held,
 * then "pauseId=minutes" pairs, tab-separated. Lines that cannot be read are skipped.
 */
object HistoryCodec {

    fun encode(days: Map<LocalDate, DayStats>): String = days.values.sortedBy { it.date }.joinToString("\n") { d ->
        listOf(
            d.date.toString(),
            d.livedMinutes.toString(),
            d.daylightMinutes.toString(),
            d.held.toString(),
            d.byPause.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" },
        ).joinToString("\t")
    }

    fun decode(text: String): Map<LocalDate, DayStats> = text.lineSequence().mapNotNull { line ->
        val f = line.split('\t')
        if (f.size < 5) return@mapNotNull null
        val date = runCatching { LocalDate.parse(f[0]) }.getOrNull() ?: return@mapNotNull null
        val byPause = f[4].split(',').filter { '=' in it }.mapNotNull { pair ->
            val minutes = pair.substringAfterLast('=').toIntOrNull() ?: return@mapNotNull null
            pair.substringBeforeLast('=') to minutes
        }.toMap()
        DayStats(
            date = date,
            livedMinutes = f[1].toIntOrNull() ?: return@mapNotNull null,
            daylightMinutes = f[2].toIntOrNull() ?: return@mapNotNull null,
            held = f[3].toIntOrNull() ?: return@mapNotNull null,
            byPause = byPause,
        )
    }.associateBy { it.date }
}
