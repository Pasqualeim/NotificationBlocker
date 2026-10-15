package com.pasquale.notificationblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDateTime

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Master switch: when false nothing is ever blocked, regardless of the pauses
    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
        set(value) = prefs.edit {
            putBoolean(KEY_BLOCKING_ENABLED, value)
            // Turning blocking on again is an explicit request: pauses ended early start again
            if (value) remove(KEY_PAUSE_ENDED_KEYS)
        }

    /**
     * The user's pauses. Before Nook 2 there was one window ("start_time"/"end_time"): read as a single
     * pause every day until the list is first written.
     */
    var pauses: List<Pause>
        get() = if (prefs.contains(KEY_PAUSES)) PauseCodec.decode(prefs.getString(KEY_PAUSES, null).orEmpty()) else listOf(legacyPause())
        @Synchronized set(value) = prefs.edit { putString(KEY_PAUSES, PauseCodec.encode(value)) }

    private fun legacyPause() = Pause(
        id = LEGACY_PAUSE_ID,
        name = "",
        start = prefs.getInt(KEY_START_TIME, 22 * 60), // Default 22:00
        end = prefs.getInt(KEY_END_TIME, 7 * 60), // Default 07:00
    )

    // The Home screen still edits one pause (the first) until it gets its own pause list
    var startTimeMinutes: Int
        get() = (pauses.firstOrNull() ?: legacyPause()).start
        set(value) = updateFirstPause { it.copy(start = value) }

    var endTimeMinutes: Int
        get() = (pauses.firstOrNull() ?: legacyPause()).end
        set(value) = updateFirstPause { it.copy(end = value) }

    @Synchronized
    private fun updateFirstPause(change: (Pause) -> Pause) {
        val all = pauses
        pauses = listOf(change(all.firstOrNull() ?: legacyPause())) + all.drop(1)
    }

    // Asked once: the user tapped "Not now" (or denied) on the zen notification prompt
    var zenPromptDismissed: Boolean
        get() = prefs.getBoolean(KEY_ZEN_PROMPT_DISMISSED, false)
        set(value) = prefs.edit { putBoolean(KEY_ZEN_PROMPT_DISMISSED, value) }

    // Occurrence (see [Occurrence.key]) in which the user swiped the zen notification away: not reposted until the next one
    var zenDismissedWindow: String?
        get() = prefs.getString(KEY_ZEN_DISMISSED_WINDOW, null)
        set(value) = prefs.edit { putString(KEY_ZEN_DISMISSED_WINDOW, value) }

    /**
     * Occurrences the user ended early ("End the pause" in the break notification): work notifications
     * come through until they would have ended. Cleared when blocking is turned on again.
     */
    val pauseEndedKeys: Set<String>
        get() = prefs.getStringSet(KEY_PAUSE_ENDED_KEYS, emptySet()).orEmpty()

    /** Ends early every pause in progress; keys older than two days are dropped on the way. */
    @Synchronized
    fun endPausesNow() {
        val now = LocalDateTime.now()
        val keep = pauseEndedKeys.filter { key ->
            Occurrence.dateOf(key)?.isBefore(now.toLocalDate().minusDays(2)) == false
        }
        val active = PauseSchedule.activeAt(pauses, now).map { it.key }
        prefs.edit { putStringSet(KEY_PAUSE_ENDED_KEYS, (keep + active).toSet()) }
    }

    /** Undoes [endPausesNow] ("Pause again" on Home). */
    fun resumePauses() = prefs.edit { remove(KEY_PAUSE_ENDED_KEYS) }

    /** True when inside a pause, but every pause in progress was ended early by the user. */
    fun isPauseEndedNow(): Boolean {
        val now = LocalDateTime.now()
        return PauseSchedule.activeAt(pauses, now).isNotEmpty() && PauseSchedule.activeAt(pauses, now, pauseEndedKeys).isEmpty()
    }

    /** True while Nook really holds work notifications: on, inside a pause, not ended early. */
    fun isPausedNow(): Boolean = isBlockingEnabled && PauseSchedule.activeAt(pauses, LocalDateTime.now(), pauseEndedKeys).isNotEmpty()

    /** Key of the pause held notifications are counted in ([PauseSchedule.counterOccurrence]); null outside pauses. */
    fun currentWindowKey(): String? =
        PauseSchedule.counterOccurrence(pauses, getBlockedApps(), LocalDateTime.now(), pauseEndedKeys)?.key

    /** Work notifications dismissed during [window]; the counter restarts with each window. */
    fun filteredCount(window: String): Int =
        if (prefs.getString(KEY_FILTERED_WINDOW, null) == window) prefs.getInt(KEY_FILTERED_COUNT, 0) else 0

    /**
     * Counts a held notification once per [notificationKey] in [window]: apps that re-post the same
     * notification (updates, progress) do not inflate the counter. The app is kept with a fingerprint
     * of the key for the morning report (see [MorningReport.entry]).
     */
    @Synchronized
    fun recordFiltered(window: String, packageName: String, notificationKey: String) {
        val sameWindow = prefs.getString(KEY_FILTERED_WINDOW, null) == window
        val seen = if (sameWindow) prefs.getStringSet(KEY_FILTERED_KEYS, emptySet()).orEmpty() else emptySet()
        val entry = MorningReport.entry(packageName, notificationKey)
        // Entries written by older versions in this same window: the raw key, alone or after the package
        if (entry in seen || notificationKey in seen || "$packageName\n$notificationKey" in seen) return
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

    /** Inside at least one pause, whether or not it was ended early. */
    fun isInOffHoursNow(): Boolean = PauseSchedule.activeAt(pauses, LocalDateTime.now()).isNotEmpty()

    fun shouldBlock(packageName: String): Boolean = PauseSchedule.shouldBlock(
        packageName = packageName,
        blockingEnabled = isBlockingEnabled,
        workApps = getBlockedApps(),
        pauses = pauses,
        now = LocalDateTime.now(),
        ended = pauseEndedKeys,
    )

    companion object {
        private const val PREFS_NAME = "notification_blocker_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_BLOCKING_ENABLED = "blocking_enabled"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_ZEN_PROMPT_DISMISSED = "zen_prompt_dismissed"
        private const val KEY_ZEN_DISMISSED_WINDOW = "zen_dismissed_window"
        private const val KEY_FILTERED_WINDOW = "filtered_window"
        private const val KEY_FILTERED_COUNT = "filtered_count"
        private const val KEY_FILTERED_KEYS = "filtered_keys"
        private const val KEY_REPORT_SEEN_WINDOW = "report_seen_window"
        const val KEY_PAUSE_ENDED_KEYS = "pause_ended_keys"
        const val KEY_PAUSES = "pauses"
        private const val LEGACY_PAUSE_ID = "main"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
