package com.pasquale.notificationblocker.data

/**
 * When the user usually sleeps, in minutes from midnight; wraps past midnight when [start] > [end]
 * (23:00 to 07:00). Nook keeps holding work notifications while you sleep, so the morning starts clean,
 * but those minutes are not free time won back from work: the history and the weekly hours leave them
 * out. [start] == [end] means no sleep window.
 */
data class SleepWindow(val start: Int, val end: Int) {

    /** The asleep parts of one calendar day, as [from, to) minute pairs. */
    val ofDay: List<Pair<Int, Int>>
        get() = when {
            start == end -> emptyList()
            start < end -> listOf(start to end)
            else -> listOfNotNull((0 to end).takeIf { end > 0 }, start to DAY)
        }

    /** Whether [minute] of a day falls in the window. */
    operator fun contains(minute: Int): Boolean = ofDay.any { (a, b) -> minute in a until b }

    /** The awake parts of [from, to) (minutes of one day), in order. */
    fun awake(from: Int, to: Int): List<Pair<Int, Int>> {
        var parts = listOf(from to to)
        for ((a, b) in ofDay) {
            parts = parts.flatMap { (x, y) ->
                listOf(x to minOf(y, a), maxOf(x, b) to y).filter { it.second > it.first }
            }
        }
        return parts
    }

    companion object {
        private const val DAY = OffHours.MINUTES_PER_DAY
        val DEFAULT = SleepWindow(23 * 60, 7 * 60)
    }
}
