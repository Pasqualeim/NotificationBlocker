package com.pasquale.notificationblocker.widget

import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.PauseSchedule
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * What the home screen widget shows now: pure, unit tested. Like the tile it reads the clock when it
 * is refreshed (every minute with the screen on, with the break notification) and never sets alarms.
 *
 * @param pause the pause in progress ([Kind.PAUSED]) or the next one ([Kind.BETWEEN])
 * @param minute when work comes back ([Kind.PAUSED], null for "all day") or when the next pause starts
 * @param date the day [minute] falls on, for "tomorrow" or a weekday
 */
data class NookWidgetState(
    val kind: Kind,
    val pause: Pause? = null,
    val minute: Int? = null,
    val date: LocalDate? = null,
) {
    enum class Kind { OFF, PAUSED, ENDED, BETWEEN }

    /** "Pause now" is offered only where it changes something: Nook off or between pauses. */
    val offersQuickPause: Boolean get() = kind == Kind.OFF || kind == Kind.BETWEEN

    companion object {
        fun resolve(now: LocalDateTime, blockingEnabled: Boolean, pauses: List<Pause>, ended: Set<String>): NookWidgetState {
            if (!blockingEnabled) return NookWidgetState(Kind.OFF)
            val active = PauseSchedule.activeAt(pauses, now, ended)
            if (active.isNotEmpty()) {
                val until = PauseSchedule.blockedUntil(pauses, now, ended)
                return NookWidgetState(
                    kind = Kind.PAUSED,
                    pause = active.first().pause,
                    minute = PauseSchedule.untilMinutes(pauses, now, until, ended),
                    date = until?.toLocalDate(),
                )
            }
            val next = PauseSchedule.nextOccurrence(pauses, now, ended)
            val kind = if (PauseSchedule.activeAt(pauses, now).isNotEmpty()) Kind.ENDED else Kind.BETWEEN
            return NookWidgetState(
                kind = kind,
                pause = next?.pause,
                minute = next?.start?.let { it.hour * 60 + it.minute },
                date = next?.start?.toLocalDate(),
            )
        }
    }
}
