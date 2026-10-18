package com.pasquale.notificationblocker.data

import java.security.MessageDigest
import java.time.LocalDate

/** How many held notifications one app had in a window. */
data class AppCount(val packageName: String, val count: Int)

/**
 * What waited outside during one off-hours window: shown once on Home after the window ends
 * ("While you were off, 23 work notifications waited outside: Slack, Teams and 2 more").
 *
 * @param window key of the pause occurrence ([Occurrence.key]; older versions stored the bare ISO date)
 * @param apps apps with held notifications, most first
 */
data class MorningReport(val window: String, val total: Int, val apps: List<AppCount>) {

    companion object {
        /**
         * Package of a stored held-notification entry. Entries are "package\nfingerprint"; older
         * ones are "package\nkey" or the bare notification key ("user|package|id|tag|uid").
         */
        fun packageOf(entry: String): String? {
            val newline = entry.indexOf('\n')
            if (newline > 0) return entry.substring(0, newline)
            return entry.split('|').getOrNull(1)?.takeIf { it.isNotEmpty() }
        }

        /**
         * Stored entry of a held notification: its app and a [fingerprint] of its key, enough to count
         * it once. The key itself is never stored: its tag can be a chat or contact id (a phone number
         * in some messaging apps), and the prefs file goes into the Android backup.
         */
        fun entry(packageName: String, notificationKey: String) = "$packageName\n${fingerprint(notificationKey)}"

        /** First 8 bytes of the SHA-256 of [notificationKey], in hex. */
        fun fingerprint(notificationKey: String): String =
            MessageDigest.getInstance("SHA-256").digest(notificationKey.toByteArray())
                .take(8)
                .joinToString("") { "%02x".format(it) }

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
         * dismissed it ([seenWindow]), and only for a window that started as many nights ago as it lasts, so an
         * old one never reads as "last night".
         */
        fun shouldShow(
            report: MorningReport?,
            today: LocalDate,
            currentWindow: String?,
            seenWindow: String?,
        ): Boolean {
            if (report == null || report.window == seenWindow || report.window == currentWindow) return false
            val start = Occurrence.dateOf(report.window) ?: return false
            // As far back as that pause lasts: a weekend off work starts on Friday, its report shows on Monday
            return !start.isBefore(today.minusDays(WorkSchedule.nightsOf(report.window).toLong())) && !start.isAfter(today)
        }
    }
}
