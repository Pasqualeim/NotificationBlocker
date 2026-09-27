package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.data.OffHours
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The line on Home that values the daylight and free time left in the day. Philosophy: unplugging
 * is not about night and sleep, it is about getting life (and the sun) back.
 */
sealed interface LifeMessage {
    /** Blocking is off. */
    data object TurnOn : LifeMessage

    /** Off work with the sun up: [minutes] of daylight left in the window. */
    data class SunLeft(val minutes: Int) : LifeMessage

    /** Off work after sunset. */
    data object EveningIsYours : LifeMessage

    /** Off work before sunrise. */
    data object DayBeginning : LifeMessage

    /** Working: the window starts at [start] with [minutes] of daylight still ahead. */
    data class SunAfterWork(val start: Int, val minutes: Int) : LifeMessage

    /** Working: by [start] the sun will have set. */
    data class EveningAfterWork(val start: Int) : LifeMessage
}

object LifeCopy {

    /** Less daylight than this is not worth a "sunshine" message. */
    const val MIN_SUN_MINUTES = 15

    fun message(now: LocalDateTime, blockingEnabled: Boolean, start: Int, end: Int): LifeMessage {
        if (!blockingEnabled) return LifeMessage.TurnOn
        val minute = now.hour * 60 + now.minute
        val (sunrise, sunset) = ZenEnvironment.sunTimes(now.toLocalDate())
        if (OffHours.isWithin(minute, start, end)) {
            val untilEnd = OffHours.minutesUntilEnd(minute, start, end) ?: OffHours.MINUTES_PER_DAY
            return when {
                minute < sunrise -> LifeMessage.DayBeginning
                minute >= sunset -> LifeMessage.EveningIsYours
                else -> LifeMessage.SunLeft(minOf(sunset, minute + untilEnd) - minute)
            }
        }
        // Only the sun right after work counts here, not the next morning's
        val sun = sunshineInWindow(now.toLocalDate(), start, end, includeNextMorning = false)
        return if (sun >= MIN_SUN_MINUTES) LifeMessage.SunAfterWork(start, sun) else LifeMessage.EveningAfterWork(start)
    }

    /**
     * Minutes of daylight inside the window that starts on [date] (sunrise to sunset, same sun times
     * used for the morning after in an overnight window).
     */
    fun sunshineInWindow(date: LocalDate, start: Int, end: Int, includeNextMorning: Boolean = true): Int {
        val (sunrise, sunset) = ZenEnvironment.sunTimes(date)
        val endAbs = when {
            start == end -> start + OffHours.MINUTES_PER_DAY
            end > start -> end
            else -> end + OffHours.MINUTES_PER_DAY
        }
        val day = OffHours.MINUTES_PER_DAY
        val sameDay = overlap(start, endAbs, sunrise, sunset)
        return if (includeNextMorning) sameDay + overlap(start, endAbs, sunrise + day, sunset + day) else sameDay
    }

    private fun overlap(a0: Int, a1: Int, b0: Int, b1: Int) = maxOf(0, minOf(a1, b1) - maxOf(a0, b0))
}
