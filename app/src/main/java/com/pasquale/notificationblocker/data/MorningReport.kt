package com.pasquale.notificationblocker.data

import java.time.LocalDate

/** How many held notifications one app had in a window. */
data class AppCount(val packageName: String, val count: Int)

/**
 * What waited outside during one off-hours window: shown once on Home after the window ends
 * ("While you were off, 23 work notifications waited outside: Slack, Teams and 2 more").
 *
 * @param window ISO start date of the window (see [OffHours.windowStartDate])
 * @param apps apps with held notifications, most first
 */
data class MorningReport(val window: String, val total: Int, val apps: List<AppCount>) {

    companion object {
        /**
         * Package of a stored held-notification entry. Entries are "package\nkey"; older ones
         * are the bare notification key ("user|package|id|tag|uid").
         */
        fun packageOf(entry: String): String? {
            val newline = entry.indexOf('\n')
            if (newline > 0) return entry.substring(0, newline)
            return entry.split('|').getOrNull(1)?.takeIf { it.isNotEmpty() }
        }

        fun entry(packageName: String, notificationKey: String) = "$packageName\n$notificationKey"

        /** The report of [window] from the stored counter and entries; null if nothing was held. */
        fun build(window: String?, total: Int, entries: Set<String>): MorningReport? {
            if (window == null || total <= 0) return null
            val apps = entries.mapNotNull(::packageOf)
                .groupingBy { it }
                .eachCount()
                .map { (pkg, count) -> AppCount(pkg, count) }
                .sortedWith(compareByDescending<AppCount> { it.count }.thenBy { it.packageName })
            return MorningReport(window, total, apps)
        }

        /**
         * Show [report] once its window is over: not while still inside it, not after the user
         * dismissed it ([seenWindow]), and only for a window that started today or yesterday, so an
         * old one never reads as "last night".
         */
        fun shouldShow(
            report: MorningReport?,
            today: LocalDate,
            currentWindow: String?,
            seenWindow: String?,
        ): Boolean {
            if (report == null || report.window == seenWindow || report.window == currentWindow) return false
            val start = runCatching { LocalDate.parse(report.window) }.getOrNull() ?: return false
            return !start.isBefore(today.minusDays(1)) && !start.isAfter(today)
        }
    }
}
