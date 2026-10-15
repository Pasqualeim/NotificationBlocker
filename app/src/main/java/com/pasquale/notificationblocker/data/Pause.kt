package com.pasquale.notificationblocker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One break the user set ("Lunch" 13:00-14:00 Mon-Fri, "Evening" 18:30-08:30 every day, "Weekend" all
 * day Sat-Sun). Times are minutes from midnight; [start] == [end] is a whole day.
 *
 * @param days the days on which the pause *starts*: Friday's 18:30-08:30 ends on Saturday morning
 * @param apps the apps this pause silences; null (the usual case) means all the work apps
 */
data class Pause(
    val id: String,
    val name: String,
    val start: Int,
    val end: Int,
    val days: Set<DayOfWeek> = ALL_DAYS,
    val enabled: Boolean = true,
    val apps: Set<String>? = null,
) {
    val isWholeDay: Boolean get() = start == end

    /** The apps this pause holds back, given the user's list of work apps. */
    fun appsOrDefault(workApps: Set<String>): Set<String> = apps ?: workApps

    companion object {
        val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    }
}

/**
 * One time a [pause] happens, the one that starts on [startDate]. Its [key] ("2026-10-15#lunch")
 * names it in the preferences: ended early, notification swiped away, held notifications.
 */
data class Occurrence(val pause: Pause, val startDate: LocalDate) {

    val key: String get() = "$startDate#${pause.id}"

    /** A whole-day pause covers its calendar day; the others run from their start to their end time. */
    val start: LocalDateTime
        get() = if (pause.isWholeDay) startDate.atStartOfDay() else startDate.atTime(pause.start / 60, pause.start % 60)

    val end: LocalDateTime
        get() = when {
            pause.isWholeDay -> startDate.plusDays(1).atStartOfDay()
            pause.start < pause.end -> startDate.atTime(pause.end / 60, pause.end % 60)
            else -> startDate.plusDays(1).atTime(pause.end / 60, pause.end % 60)
        }

    companion object {
        /** Date part of an occurrence key; older versions stored the bare date, which reads the same. */
        fun dateOf(key: String): LocalDate? = runCatching { LocalDate.parse(key.substringBefore('#')) }.getOrNull()
    }
}

/**
 * Pauses as one line each in the preferences: plain text, no JSON library, readable by the JVM tests.
 * Fields are tab-separated: id, name, start, end, days (bit 0 = Monday), enabled (1/0), apps
 * (comma-separated, or "*" for all the work apps).
 */
object PauseCodec {

    private const val ALL_APPS = "*"

    fun encode(pauses: List<Pause>): String = pauses.joinToString("\n") { p ->
        listOf(
            clean(p.id),
            clean(p.name),
            p.start.toString(),
            p.end.toString(),
            p.days.sumOf { 1 shl it.ordinal }.toString(),
            if (p.enabled) "1" else "0",
            p.apps?.joinToString(",") ?: ALL_APPS,
        ).joinToString("\t")
    }

    /** Lines that cannot be read are skipped: a broken pause must not break the others. */
    fun decode(text: String): List<Pause> = text.lineSequence().mapNotNull { line ->
        val f = line.split('\t')
        if (f.size < 7) return@mapNotNull null
        val start = f[2].toIntOrNull()?.takeIf { it in 0 until OffHours.MINUTES_PER_DAY } ?: return@mapNotNull null
        val end = f[3].toIntOrNull()?.takeIf { it in 0 until OffHours.MINUTES_PER_DAY } ?: return@mapNotNull null
        val mask = f[4].toIntOrNull() ?: return@mapNotNull null
        Pause(
            id = f[0],
            name = f[1],
            start = start,
            end = end,
            days = DayOfWeek.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet(),
            enabled = f[5] == "1",
            apps = if (f[6] == ALL_APPS) null else f[6].split(',').filter { it.isNotEmpty() }.toSet(),
        )
    }.toList()

    // Names are typed by the user: a tab or a newline would split the line
    private fun clean(text: String) = text.replace('\t', ' ').replace('\n', ' ')
}
