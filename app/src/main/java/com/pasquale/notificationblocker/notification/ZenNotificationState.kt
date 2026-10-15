package com.pasquale.notificationblocker.notification

import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.PauseSchedule
import com.pasquale.notificationblocker.ui.zen.TimeOfDay
import com.pasquale.notificationblocker.ui.zen.ZenEnvironment
import java.time.LocalDateTime

/** Phase of the day shown by the zen notification: picks the illustration and the message. */
enum class ZenPhase {
    MORNING, DAYLIGHT, SUNSET, NIGHT;

    companion object {
        fun of(timeOfDay: TimeOfDay): ZenPhase = when (timeOfDay) {
            TimeOfDay.DAWN -> MORNING
            TimeOfDay.DAYLIGHT -> DAYLIGHT
            TimeOfDay.GOLDEN_HOUR -> SUNSET
            TimeOfDay.NIGHT -> NIGHT
        }
    }
}

/**
 * What the zen notification shows right now. Pure data, resolved from the clock and the
 * preferences, so the rules are unit tested and the manager only renders it.
 *
 * @param window key of the pause in progress that started first ([com.pasquale.notificationblocker.data.Occurrence.key])
 * @param endMinutes when work comes through again (minutes from midnight), null for pauses that never stop
 * @param filteredCount work notifications held so far in this pause
 * @param heldApps packages of the apps they came from, most first
 * @param timeoutMillis time until the pauses end: the system removes the notification then
 */
data class ZenNotificationState(
    val phase: ZenPhase,
    val window: String,
    val endMinutes: Int?,
    val filteredCount: Int,
    val heldApps: List<String>,
    val timeoutMillis: Long?,
) {
    /** Changes only when something visible changes: avoids re-posting every minute. */
    val contentKey: String
        get() = "$phase|$window|$endMinutes|$filteredCount|${heldApps.take(NAMED_APPS)}|${heldApps.size}"

    companion object {
        /** Apps named in the text ("from Slack and Teams"); the others are counted ("and 2 more"). */
        const val NAMED_APPS = 2

        /**
         * The state to show at [now], or null when the notification must not be shown. It is shown only
         * while Nook really holds work notifications: blocking on, a pause in progress that was not ended
         * early ([ended]) and has apps to silence, and the notification access granted ([canHold]).
         * A pause the user swiped the notification away from ([dismissedWindow]) stays quiet.
         *
         * @param held work notifications held in a pause: total and apps, most first
         */
        fun resolve(
            now: LocalDateTime,
            blockingEnabled: Boolean,
            pauses: List<Pause>,
            workApps: Set<String>,
            dismissedWindow: String?,
            ended: Set<String> = emptySet(),
            canHold: Boolean = true,
            held: (window: String) -> Pair<Int, List<String>>,
        ): ZenNotificationState? {
            if (!blockingEnabled || !canHold) return null
            if (PauseSchedule.holdingAt(pauses, workApps, now, ended).isEmpty()) return null
            val window = PauseSchedule.counterOccurrence(pauses, workApps, now, ended)?.key ?: return null
            if (window == dismissedWindow) return null
            val minutes = now.hour * 60 + now.minute
            val (sunrise, sunset) = ZenEnvironment.sunTimes(now.toLocalDate())
            val until = PauseSchedule.blockedUntil(pauses, now, ended)
            val (count, apps) = held(window)
            return ZenNotificationState(
                phase = ZenPhase.of(ZenEnvironment.timeOfDay(minutes, sunrise, sunset)),
                window = window,
                endMinutes = PauseSchedule.untilMinutes(pauses, now, until, ended),
                filteredCount = count,
                heldApps = apps,
                timeoutMillis = until?.let { PauseSchedule.millisUntil(now, it) },
            )
        }
    }
}
