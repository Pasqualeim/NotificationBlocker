package com.pasquale.notificationblocker.data

import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

object OffHours {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * Returns true if [currentMinutes] falls inside the off-hours window [start, end).
     * All values are minutes from midnight. The window may wrap past midnight
     * (e.g. 22:00 -> 07:00). When start == end the window covers the whole day.
     */
    fun isWithin(currentMinutes: Int, start: Int, end: Int): Boolean = when {
        start == end -> true
        start < end -> currentMinutes in start until end
        else -> currentMinutes >= start || currentMinutes < end
    }

    /**
     * Date on which the window containing [currentMinutes] started. An overnight window
     * (22:00 -> 07:00) checked at 01:00 started the day before, so both halves of the same
     * night map to one date. Only meaningful when [isWithin] is true.
     */
    fun windowStartDate(today: LocalDate, currentMinutes: Int, start: Int, end: Int): LocalDate =
        if (start > end && currentMinutes < end) today.minusDays(1) else today

    /**
     * Minutes left until the window ends, seen from [currentMinutes] inside it; null for a
     * whole-day window (start == end), which never ends.
     */
    fun minutesUntilEnd(currentMinutes: Int, start: Int, end: Int): Int? {
        if (start == end) return null
        return (end - currentMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    fun currentMinutes(calendar: Calendar = Calendar.getInstance()): Int =
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    fun format(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
}
