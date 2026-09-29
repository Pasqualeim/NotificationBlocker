package com.pasquale.notificationblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Master switch: when false nothing is ever blocked, regardless of the schedule
    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING_ENABLED, value) }

    // Store time as minutes from midnight
    var startTimeMinutes: Int
        get() = prefs.getInt(KEY_START_TIME, 22 * 60) // Default 22:00
        set(value) = prefs.edit { putInt(KEY_START_TIME, value) }

    var endTimeMinutes: Int
        get() = prefs.getInt(KEY_END_TIME, 7 * 60) // Default 07:00
        set(value) = prefs.edit { putInt(KEY_END_TIME, value) }

    // ISO date of the last off-hours window that played the "end of shift" celebration
    var lastCelebratedWindow: String?
        get() = prefs.getString(KEY_LAST_CELEBRATED_WINDOW, null)
        set(value) = prefs.edit { putString(KEY_LAST_CELEBRATED_WINDOW, value) }

    // Asked once: the user tapped "Not now" (or denied) on the zen notification prompt
    var zenPromptDismissed: Boolean
        get() = prefs.getBoolean(KEY_ZEN_PROMPT_DISMISSED, false)
        set(value) = prefs.edit { putBoolean(KEY_ZEN_PROMPT_DISMISSED, value) }

    // Window (ISO start date) in which the user swiped the zen notification away: not reposted until the next one
    var zenDismissedWindow: String?
        get() = prefs.getString(KEY_ZEN_DISMISSED_WINDOW, null)
        set(value) = prefs.edit { putString(KEY_ZEN_DISMISSED_WINDOW, value) }

    /** Key of the off-hours window containing now (ISO date of its start). */
    fun currentWindowKey(): String = OffHours.windowStartDate(
        today = LocalDate.now(),
        currentMinutes = OffHours.currentMinutes(),
        start = startTimeMinutes,
        end = endTimeMinutes,
    ).toString()

    /** Work notifications dismissed during [window]; the counter restarts with each window. */
    fun filteredCount(window: String): Int =
        if (prefs.getString(KEY_FILTERED_WINDOW, null) == window) prefs.getInt(KEY_FILTERED_COUNT, 0) else 0

    /**
     * Counts a held notification once per [notificationKey] in [window]: apps that re-post the same
     * notification (updates, progress) do not inflate the counter. The app is kept with the key for
     * the morning report.
     */
    @Synchronized
    fun recordFiltered(window: String, packageName: String, notificationKey: String) {
        val sameWindow = prefs.getString(KEY_FILTERED_WINDOW, null) == window
        val seen = if (sameWindow) prefs.getStringSet(KEY_FILTERED_KEYS, emptySet()).orEmpty() else emptySet()
        val entry = MorningReport.entry(packageName, notificationKey)
        if (entry in seen || notificationKey in seen) return
        prefs.edit {
            putString(KEY_FILTERED_WINDOW, window)
            putInt(KEY_FILTERED_COUNT, filteredCount(window) + 1)
            putStringSet(KEY_FILTERED_KEYS, seen + entry)
        }
    }

    /** Held notifications of the last window that had any; null if none was ever held. */
    fun lastReport(): MorningReport? {
        val window = prefs.getString(KEY_FILTERED_WINDOW, null)
        return MorningReport.build(window, prefs.getInt(KEY_FILTERED_COUNT, 0), prefs.getStringSet(KEY_FILTERED_KEYS, emptySet()).orEmpty())
    }

    // Window whose morning report the user dismissed
    var reportSeenWindow: String?
        get() = prefs.getString(KEY_REPORT_SEEN_WINDOW, null)
        set(value) = prefs.edit { putString(KEY_REPORT_SEEN_WINDOW, value) }

    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setAppBlocked(packageName: String, blocked: Boolean) {
        // Always write a new set: mutating the instance returned by getStringSet is unsafe
        val apps = getBlockedApps().toMutableSet()
        if (blocked) apps.add(packageName) else apps.remove(packageName)
        prefs.edit { putStringSet(KEY_BLOCKED_APPS, apps) }
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun isInOffHoursNow(): Boolean =
        OffHours.isWithin(OffHours.currentMinutes(), startTimeMinutes, endTimeMinutes)

    fun shouldBlock(packageName: String): Boolean = BlockingRule.shouldBlock(
        packageName = packageName,
        blockingEnabled = isBlockingEnabled,
        blockedApps = getBlockedApps(),
        currentMinutes = OffHours.currentMinutes(),
        start = startTimeMinutes,
        end = endTimeMinutes,
    )

    companion object {
        private const val PREFS_NAME = "notification_blocker_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_BLOCKING_ENABLED = "blocking_enabled"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_LAST_CELEBRATED_WINDOW = "last_celebrated_window"
        private const val KEY_ZEN_PROMPT_DISMISSED = "zen_prompt_dismissed"
        private const val KEY_ZEN_DISMISSED_WINDOW = "zen_dismissed_window"
        private const val KEY_FILTERED_WINDOW = "filtered_window"
        private const val KEY_FILTERED_COUNT = "filtered_count"
        private const val KEY_FILTERED_KEYS = "filtered_keys"
        private const val KEY_REPORT_SEEN_WINDOW = "report_seen_window"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
