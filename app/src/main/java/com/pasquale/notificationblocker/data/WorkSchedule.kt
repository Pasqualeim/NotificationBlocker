package com.pasquale.notificationblocker.data

import java.time.DayOfWeek

/**
 * When the user works: the [days] and the hours [start]-[end] (minutes from midnight, a day shift:
 * [start] < [end]). Everything else is free time, so Nook holds work notifications there: the
 * schedule is turned into the pauses that cover it ([offWorkPauses]), and blocking, history, Today,
 * the break notification, tile and widget read them like any other pause.
 *
 * shortcut: day shifts only (start before end); a night shift needs its own rule, add it when asked.
 */
data class WorkSchedule(val days: Set<DayOfWeek>, val start: Int, val end: Int) {

    /** The days off work, all day ("Sab–dom" with the default schedule). */
    val freeDays: Set<DayOfWeek> get() = Pause.ALL_DAYS - days

    /**
     * The pauses outside work: one per stretch off work, from the end of a work day to the start of the
     * next one, so Friday's runs to Monday 09:00 and the weekend is a single pause (ending it early, its
     * count of held notifications and its morning report cover the whole of it). Work days with the
     * same number of nights to the next work day share one pause ("work_off" Mon-Thu, "work_off3" Fri).
     */
    fun offWorkPauses(): List<Pause> {
        // No work day: every day is free, all day. Hours that do not make a day shift: the free days only
        if (days.isEmpty()) return listOf(Pause(OFF_WORK, "", 0, 0))
        if (start >= end) return listOfNotNull(Pause(OFF_WORK, "", 0, 0, days = freeDays).takeIf { freeDays.isNotEmpty() })
        return days.groupBy { day -> (1..Pause.MAX_NIGHTS).first { day.plus(it.toLong()) in days } }
            .toSortedMap()
            .map { (nights, workDays) ->
                Pause(if (nights == 1) OFF_WORK else OFF_WORK + nights, "", end, start, days = workDays.toSet(), nights = nights)
            }
    }

    companion object {
        const val OFF_WORK = "work_off"

        /** Monday to Friday, 09:00-18:00: what most people mean by "work". */
        val DEFAULT = WorkSchedule(DayOfWeek.entries.take(5).toSet(), 9 * 60, 18 * 60)

        /** Nights of the pause named in an occurrence key ("2026-10-16#work_off3" → 3); 1 for any other pause. */
        fun nightsOf(key: String): Int =
            key.substringAfter('#').takeIf { it.startsWith(OFF_WORK) }?.removePrefix(OFF_WORK)?.toIntOrNull() ?: 1

        /** True for the pauses made from the schedule: they are edited through it, not one by one. */
        fun isOffWork(pause: Pause): Boolean = pause.id.startsWith(OFF_WORK)

        /**
         * The color slot of each pause id in [pauses] (for `pauseColor`): the time outside work is one
         * thing, so one color (0); the other pauses take 1, 2, 3... in order, one per id (a range of days
         * off is one pause per date, all with one id).
         */
        fun colorSlots(pauses: List<Pause>): Map<String, Int> {
            val slots = LinkedHashMap<String, Int>()
            var next = 1
            for (p in pauses) if (isOffWork(p)) slots[p.id] = 0 else slots.getOrPut(p.id) { next++ }
            return slots
        }

        /** "1,2,3,4,5|540|1080" (ISO day numbers, start, end); null when it cannot be read. */
        fun encode(schedule: WorkSchedule): String =
            schedule.days.map { it.value }.sorted().joinToString(",") + "|" + schedule.start + "|" + schedule.end

        fun decode(text: String?): WorkSchedule? {
            val f = text?.split('|') ?: return null
            if (f.size != 3) return null
            val days = f[0].split(',').filter { it.isNotBlank() }.map { DayOfWeek.of(it.toIntOrNull()?.takeIf { d -> d in 1..7 } ?: return null) }
            val start = f[1].toIntOrNull()?.takeIf { it in 0 until OffHours.MINUTES_PER_DAY } ?: return null
            val end = f[2].toIntOrNull()?.takeIf { it in 0 until OffHours.MINUTES_PER_DAY } ?: return null
            return WorkSchedule(days.toSet(), start, end)
        }
    }
}
