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
 * @param timeoutMillis time until the window ends: the system removes the notification then
 */
data class ZenNotificationState(
    val phase: ZenPhase,
    val window: String,
    val endMinutes: Int?,
    val filteredCount: Int,
    val timeoutMillis: Long?,
) {
    /** Changes only when something visible changes: avoids re-posting every minute. */
    val contentKey: String get() = "$phase|$window|$endMinutes|$filteredCount"

    companion object {
        /**
         * The state to show at [now], or null when the notification must not be shown: blocking off,
         * outside the window, or dismissed by the user during this window.
         */
        fun resolve(
            now: LocalDateTime,
            blockingEnabled: Boolean,
            start: Int,
            end: Int,
            dismissedWindow: String?,
            filteredCount: (window: String) -> Int,
        ): ZenNotificationState? {
            val minutes = now.hour * 60 + now.minute
            if (!blockingEnabled || !OffHours.isWithin(minutes, start, end)) return null
            val window = OffHours.windowStartDate(now.toLocalDate(), minutes, start, end).toString()
            if (window == dismissedWindow) return null
            val (sunrise, sunset) = ZenEnvironment.sunTimes(now.toLocalDate())
            val untilEnd = OffHours.minutesUntilEnd(minutes, start, end)
            return ZenNotificationState(
                phase = ZenPhase.of(ZenEnvironment.timeOfDay(minutes, sunrise, sunset)),
                window = window,
                endMinutes = if (untilEnd == null) null else end,
                filteredCount = filteredCount(window),
                timeoutMillis = untilEnd?.let { it * 60_000L - now.second * 1000L },
            )
        }
    }
}
