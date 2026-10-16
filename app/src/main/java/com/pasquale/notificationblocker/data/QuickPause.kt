package com.pasquale.notificationblocker.data

import java.time.LocalDateTime

/**
 * "Pause now": a one-off pause that starts at the current minute and lasts the chosen time, without
 * creating a break in the list. It is an ordinary dated [Pause], so the blocking rule, the history,
 * the break notification and the tile treat it like any other pause.
 */
object QuickPause {

    /**
     * Prefix of every quick pause id. Each one gets its own id ("quick1305"), so a second quick pause on
     * the same day has its own occurrence key: its own held counter, report and break notification.
     * One at a time: starting a new one replaces the previous.
     */
    const val ID_PREFIX = "quick"

    fun isQuick(pause: Pause): Boolean = pause.date != null && pause.id.startsWith(ID_PREFIX)

    /** Longest quick pause: just under a day, so its start and end times never coincide. */
    const val MAX_MINUTES = OffHours.MINUTES_PER_DAY - 1

    /** The quick pause that starts at [now] and lasts [minutes] (clamped to 1..[MAX_MINUTES]). */
    fun create(now: LocalDateTime, minutes: Int): Pause {
        val start = now.hour * 60 + now.minute
        val length = minutes.coerceIn(1, MAX_MINUTES)
        return Pause(
            id = ID_PREFIX + String.format(java.util.Locale.ROOT, "%02d%02d", now.hour, now.minute),
            name = "",
            start = start,
            end = (start + length) % OffHours.MINUTES_PER_DAY,
            date = now.toLocalDate(),
        )
    }

    /**
     * Minutes from [now] to the next pause start in [pauses], when it is within [withinMinutes]: the
     * "until the next break" option, which hands over to that pause with no gap. Null otherwise.
     */
    fun minutesToNext(pauses: List<Pause>, now: LocalDateTime, ended: Set<String>, withinMinutes: Int = 12 * 60): Int? {
        val next = PauseSchedule.nextStart(pauses.filterNot { isQuick(it) }, now, ended) ?: return null
        val minutes = java.time.Duration.between(now.withSecond(0).withNano(0), next).toMinutes().toInt()
        return minutes.takeIf { it in 1..withinMinutes }
    }
}
