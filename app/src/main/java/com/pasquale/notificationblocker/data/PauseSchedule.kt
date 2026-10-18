package com.pasquale.notificationblocker.data

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * When the pauses are on: pure logic over the list of [Pause]s and the clock (unit tested).
 *
 * Still no alarms (see CLAUDE.md): everything is decided by looking at the clock when a notification
 * arrives or when the app refreshes. Pauses may overlap ("Evening" every day and "Weekend" all day): a
 * notification is held if at least one pause in progress holds its app.
 *
 * [ended] holds the keys of the occurrences the user ended early ("End the pause"): they let work
 * through until they would have ended anyway.
 */
object PauseSchedule {

    /** How far [nextStart] and [blockedUntil] look ahead; a pause that never stops reads as "all day". */
    private const val HORIZON_DAYS = 7L

    /** The occurrence of [pause] in progress at [now], or null. Disabled pauses never happen. */
    fun occurrenceAt(pause: Pause, now: LocalDateTime): Occurrence? {
        if (!pause.enabled || pause.days.isEmpty()) return null
        val today = now.toLocalDate()
        // The one that started today, or one that started up to its number of nights ago
        return (0..pause.nights).asSequence().map { today.minusDays(it.toLong()) }
            .filter { pause.startsOn(it) }
            .map { Occurrence(pause, it) }
            .firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
    }

    /** Occurrences in progress at [now], earliest start first, without the ones in [ended]. */
    fun activeAt(pauses: List<Pause>, now: LocalDateTime, ended: Set<String> = emptySet()): List<Occurrence> =
        pauses.mapNotNull { occurrenceAt(it, now) }
            .filter { it.key !in ended }
            .sortedWith(compareBy<Occurrence> { it.start }.thenBy { it.pause.id })

    /** The single blocking rule: what [PreferencesManager.shouldBlock] evaluates with the real clock. */
    fun shouldBlock(
        packageName: String,
        blockingEnabled: Boolean,
        workApps: Set<String>,
        pauses: List<Pause>,
        now: LocalDateTime,
        ended: Set<String> = emptySet(),
    ): Boolean = blockingEnabled && activeAt(pauses, now, ended).any { packageName in it.pause.appsOrDefault(workApps) }

    /** Occurrences in progress that really hold something: at least one app to silence. */
    fun holdingAt(pauses: List<Pause>, workApps: Set<String>, now: LocalDateTime, ended: Set<String>): List<Occurrence> =
        activeAt(pauses, now, ended).filter { it.pause.appsOrDefault(workApps).isNotEmpty() }

    /**
     * When work notifications come through again: the end of the stretch of pauses in progress at [now],
     * following pauses that start right as another ends (Friday's evening, then the weekend). Null when
     * nothing is in progress, or when the stretch goes beyond [HORIZON_DAYS] (a pause on every day, all day).
     */
    fun blockedUntil(pauses: List<Pause>, now: LocalDateTime, ended: Set<String> = emptySet()): LocalDateTime? {
        var until = activeAt(pauses, now, ended).maxOfOrNull { it.end } ?: return null
        val horizon = now.plusDays(HORIZON_DAYS)
        while (until.isBefore(horizon)) {
            val next = activeAt(pauses, until, ended).maxOfOrNull { it.end } ?: return until
            if (!next.isAfter(until)) return until
            until = next
        }
        return null
    }

    /**
     * The pause in progress whose held notifications are counted (the one that started first among those
     * that hold something), so the notification, the counter and the report all use the same key. A pause
     * ended early still counts if nothing else is in progress. Null outside pauses.
     */
    fun counterOccurrence(pauses: List<Pause>, workApps: Set<String>, now: LocalDateTime, ended: Set<String>): Occurrence? =
        holdingAt(pauses, workApps, now, ended).firstOrNull() ?: activeAt(pauses, now).firstOrNull()

    /**
     * The time to show for [until] ("until 07:00"), or null when "all day" says it better: when work comes
     * back a day or more from [now] (the weekend), or at the midnight that ends a whole-day pause in progress.
     */
    fun untilMinutes(pauses: List<Pause>, now: LocalDateTime, until: LocalDateTime?, ended: Set<String> = emptySet()): Int? {
        if (until == null || !until.isBefore(now.plusDays(1))) return null
        val minutes = until.hour * 60 + until.minute
        val wholeDay = activeAt(pauses, now, ended).any { it.pause.isWholeDay }
        return minutes.takeUnless { it == 0 && wholeDay }
    }

    /** Real time from [now] to [until] on the phone's clock, daylight saving included. */
    fun millisUntil(now: LocalDateTime, until: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        Duration.between(now.atZone(zone), until.atZone(zone)).toMillis()

    /** The next time a pause starts after [now] (one in progress is skipped), or null if none is set. */
    fun nextStart(pauses: List<Pause>, now: LocalDateTime, ended: Set<String> = emptySet()): LocalDateTime? =
        nextOccurrence(pauses, now, ended)?.start

    /** The next pause to start after [now]; on a tie, the first in the list. */
    fun nextOccurrence(pauses: List<Pause>, now: LocalDateTime, ended: Set<String> = emptySet()): Occurrence? {
        val today = now.toLocalDate()
        return pauses.asSequence()
            .filter { it.enabled }
            .flatMap { pause ->
                (0..HORIZON_DAYS).asSequence()
                    .map { today.plusDays(it) }
                    .filter { pause.startsOn(it) }
                    .map { Occurrence(pause, it) }
            }
            .filter { it.start.isAfter(now) && it.key !in ended }
            .minByOrNull { it.start }
    }
}
