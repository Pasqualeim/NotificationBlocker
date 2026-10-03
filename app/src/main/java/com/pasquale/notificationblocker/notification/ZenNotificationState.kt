package com.pasquale.notificationblocker.notification

import com.pasquale.notificationblocker.data.OffHours
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
 * @param endMinutes end of the off-hours window (minutes from midnight), null for a whole-day window
 * @param filteredCount work notifications held so far in this window
 * @param heldApps packages of the apps they came from, most first
 * @param timeoutMillis time until the window ends: the system removes the notification then
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
         * while Nook really holds work notifications: blocking on, inside the window, not ended early
         * ([pauseEndedWindow]), at least one work app and the notification access granted ([canHold]).
         * A window the user swiped the notification away from ([dismissedWindow]) stays quiet.
         *
         * @param held work notifications held in a window: total and apps, most first
         */
        fun resolve(
            now: LocalDateTime,
            blockingEnabled: Boolean,
            start: Int,
            end: Int,
            dismissedWindow: String?,
            pauseEndedWindow: String? = null,
            canHold: Boolean = true,
            held: (window: String) -> Pair<Int, List<String>>,
        ): ZenNotificationState? {
            val minutes = now.hour * 60 + now.minute
            if (!blockingEnabled || !canHold || !OffHours.isWithin(minutes, start, end)) return null
            val window = OffHours.windowStartDate(now.toLocalDate(), minutes, start, end).toString()
            if (window == dismissedWindow || window == pauseEndedWindow) return null
            val (sunrise, sunset) = ZenEnvironment.sunTimes(now.toLocalDate())
            val untilEnd = OffHours.minutesUntilEnd(minutes, start, end)
            val (count, apps) = held(window)
            return ZenNotificationState(
                phase = ZenPhase.of(ZenEnvironment.timeOfDay(minutes, sunrise, sunset)),
                window = window,
                endMinutes = if (untilEnd == null) null else end,
                filteredCount = count,
                heldApps = apps,
                timeoutMillis = untilEnd?.let { it * 60_000L - now.second * 1000L },
            )
        }
    }
}
