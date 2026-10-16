package com.pasquale.notificationblocker.data

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/** The Italian national public holidays (Nook's first market), Easter Sunday and Monday included. */
enum class Holiday { NEW_YEAR, EPIPHANY, EASTER, EASTER_MONDAY, LIBERATION, LABOUR, REPUBLIC, ASSUMPTION, ALL_SAINTS, IMMACULATE, CHRISTMAS, ST_STEPHEN }

/**
 * Days off: holidays (a range of dates, "2-6 November") and the public holidays, both turned into
 * whole-day one-off [Pause]s, so blocking and history need nothing new.
 */
object DaysOff {

    /** Id prefix of a range of days off; every day of one range shares the id ("off" + a stamp). */
    const val RANGE_PREFIX = "off"

    /** Id of the public holidays (the pause name holds the [Holiday]). */
    const val HOLIDAY_ID = "holiday"

    /** Longest range of days off one entry may hold. */
    const val MAX_DAYS = 62

    /** A range of days off as stored: whole-day pauses with the same id, one per date. */
    data class Range(val id: String, val first: LocalDate, val last: LocalDate) {
        val days: Int get() = (ChronoUnit.DAYS.between(first, last) + 1).toInt()
    }

    fun isRange(pause: Pause): Boolean = pause.date != null && pause.id.startsWith(RANGE_PREFIX)

    fun isHoliday(pause: Pause): Boolean = pause.date != null && pause.id == HOLIDAY_ID

    /** The whole-day pauses of a range from [first] to [last] (swapped if reversed, capped at [MAX_DAYS]). */
    fun rangePauses(id: String, first: LocalDate, last: LocalDate): List<Pause> {
        val (a, b) = if (last.isBefore(first)) last to first else first to last
        return generateSequence(a) { it.plusDays(1) }
            .takeWhile { !it.isAfter(b) }
            .take(MAX_DAYS)
            .map { Pause(id = id, name = "", start = 0, end = 0, date = it) }
            .toList()
    }

    /** The ranges in a list of one-off pauses, earliest first. */
    fun ranges(oneOffs: List<Pause>): List<Range> =
        oneOffs.filter { isRange(it) }
            .groupBy { it.id }
            .map { (id, days) -> Range(id, days.minOf { it.date!! }, days.maxOf { it.date!! }) }
            .sortedBy { it.first }

    /** Whole-day pauses for the public holidays from [from] to [to], named after their [Holiday]. */
    fun holidayPauses(from: LocalDate, to: LocalDate): List<Pause> =
        (from.year..to.year).flatMap { holidays(it).entries }
            .filter { (date, _) -> !date.isBefore(from) && !date.isAfter(to) }
            .sortedBy { it.key }
            .map { (date, h) -> Pause(id = HOLIDAY_ID, name = h.name, start = 0, end = 0, date = date) }

    /** The next public holiday from [from] on (today included), within a year. */
    fun nextHoliday(from: LocalDate): Pair<LocalDate, Holiday>? =
        (from.year..from.year + 1).flatMap { holidays(it).entries }
            .filter { !it.key.isBefore(from) }
            .minByOrNull { it.key }
            ?.toPair()

    fun holidays(year: Int): Map<LocalDate, Holiday> {
        val easter = easter(year)
        return mapOf(
            LocalDate.of(year, Month.JANUARY, 1) to Holiday.NEW_YEAR,
            LocalDate.of(year, Month.JANUARY, 6) to Holiday.EPIPHANY,
            easter to Holiday.EASTER,
            easter.plusDays(1) to Holiday.EASTER_MONDAY,
            LocalDate.of(year, Month.APRIL, 25) to Holiday.LIBERATION,
            LocalDate.of(year, Month.MAY, 1) to Holiday.LABOUR,
            LocalDate.of(year, Month.JUNE, 2) to Holiday.REPUBLIC,
            LocalDate.of(year, Month.AUGUST, 15) to Holiday.ASSUMPTION,
            LocalDate.of(year, Month.NOVEMBER, 1) to Holiday.ALL_SAINTS,
            LocalDate.of(year, Month.DECEMBER, 8) to Holiday.IMMACULATE,
            LocalDate.of(year, Month.DECEMBER, 25) to Holiday.CHRISTMAS,
            LocalDate.of(year, Month.DECEMBER, 26) to Holiday.ST_STEPHEN,
        )
    }

    /** Easter Sunday in the Gregorian calendar (anonymous Gregorian algorithm). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
