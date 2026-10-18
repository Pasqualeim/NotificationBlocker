package com.pasquale.notificationblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Master switch: when false nothing is ever blocked, regardless of the pauses
    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
        set(value) = settleThen {
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
        set(value) = settleThen { putString(KEY_PAUSES, PauseCodec.encode(value)) }

    /**
     * One-off pauses on a date ("Pause now", days off): kept apart from [pauses] so the Breaks list and
     * the usual week show only the user's regular breaks. Past ones are dropped when the list is written.
     */
    var oneOffPauses: List<Pause>
        get() = PauseCodec.decode(prefs.getString(KEY_ONE_OFF_PAUSES, null).orEmpty())
        set(value) = settleThen { putOneOffs(value) }

    private fun SharedPreferences.Editor.putOneOffs(value: List<Pause>) {
        val yesterday = LocalDate.now().minusDays(1)
        // A range of days off stays whole until its last day is past, so its dates never shrink one by one
        val liveRanges = value.filter { DaysOff.isRange(it) && !it.date!!.isBefore(yesterday) }.mapTo(HashSet()) { it.id }
        val kept = value.filter { it.date == null || !it.date.isBefore(yesterday) || (DaysOff.isRange(it) && it.id in liveRanges) }
        putString(KEY_ONE_OFF_PAUSES, PauseCodec.encode(kept))
    }

    /**
     * The first-launch setup should run: never finished, and Nook never set up. People updating from an
     * older Nook have pauses, a window or the switch saved, and skip it. Work apps are not a sign: the
     * setup writes them as they are ticked, and leaving halfway must bring it back next time.
     */
    val needsOnboarding: Boolean
        get() = !prefs.getBoolean(KEY_ONBOARDING_DONE, false) &&
            listOf(KEY_PAUSES, KEY_START_TIME, KEY_BLOCKING_ENABLED).none { prefs.contains(it) }

    /**
     * Ends the first-launch setup: saves the [schedule], the extra [pauses] and turns Nook on. An empty
     * list is saved as such, so the old default window (22:00-07:00) does not appear unasked.
     */
    fun completeOnboarding(schedule: WorkSchedule, pauses: List<Pause>) = settleThen {
        putString(KEY_WORK_SCHEDULE, WorkSchedule.encode(schedule))
        putString(KEY_PAUSES, PauseCodec.encode(pauses))
        putBoolean(KEY_BLOCKING_ENABLED, true)
        putBoolean(KEY_ONBOARDING_DONE, true)
    }

    /**
     * When the user works; the time outside it is paused ([WorkSchedule.offWorkPauses]). Null when it
     * was never set: Nook updated from a version that had only pauses keeps working on those.
     */
    var workSchedule: WorkSchedule?
        get() = WorkSchedule.decode(prefs.getString(KEY_WORK_SCHEDULE, null))
        set(value) {
            if (value == workSchedule) return
            settleThen {
                if (value == null) remove(KEY_WORK_SCHEDULE) else putString(KEY_WORK_SCHEDULE, WorkSchedule.encode(value))
                // A new schedule makes new stretches off work: an old "ended early" must not carry over
                putStringSet(KEY_PAUSE_ENDED_KEYS, pauseEndedKeys.filterNot { it.substringAfter('#').startsWith(WorkSchedule.OFF_WORK) }.toSet())
            }
        }

    /** When the user usually sleeps: still held, but left out of the free time (see [SleepWindow]). */
    var sleepWindow: SleepWindow
        get() = SleepWindow(
            prefs.getInt(KEY_SLEEP_START, SleepWindow.DEFAULT.start),
            prefs.getInt(KEY_SLEEP_END, SleepWindow.DEFAULT.end),
        )
        set(value) = settleThen {
            putInt(KEY_SLEEP_START, value.start)
            putInt(KEY_SLEEP_END, value.end)
        }

    /** Public holidays count as days off (off by default: not everyone has them free). */
    var holidaysEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOLIDAYS_ENABLED, false)
        set(value) = settleThen { putBoolean(KEY_HOLIDAYS_ENABLED, value) }

    /**
     * Every pause that can be in progress: the time outside work, the extra ones, the one-offs, then the
     * public holidays from yesterday to a week ahead (as far as [PauseSchedule] looks). Blocking and
     * history use this.
     */
    fun allPauses(today: LocalDate = LocalDate.now()): List<Pause> {
        val holidays = if (holidaysEnabled) DaysOff.holidayPauses(today.minusDays(1), today.plusDays(8)) else emptyList()
        return workSchedule?.offWorkPauses().orEmpty() + pauses + oneOffPauses + holidays
    }

    /** Adds a range of days off from [first] to [last]. */
    @Synchronized
    fun addDaysOff(first: LocalDate, last: LocalDate) {
        val id = DaysOff.RANGE_PREFIX + System.currentTimeMillis().toString(36)
        oneOffPauses = oneOffPauses + DaysOff.rangePauses(id, first, last)
    }

    @Synchronized
    fun removeDaysOff(id: String) {
        oneOffPauses = oneOffPauses.filterNot { it.id == id && DaysOff.isRange(it) }
    }

    /** The quick pause ("Pause now"), if one was started and has not been replaced. */
    fun quickPause(): Pause? = oneOffPauses.firstOrNull { QuickPause.isQuick(it) }

    /**
     * Starts a quick pause of [minutes] from now, replacing an earlier one. Asking for a pause also turns
     * Nook on; unlike the master switch it leaves "End the pause" on other pauses alone (the button is
     * not offered while a pause is ended early).
     */
    @Synchronized
    fun startQuickPause(minutes: Int, now: LocalDateTime = LocalDateTime.now()) {
        val quick = QuickPause.create(now, minutes)
        val others = oneOffPauses.filterNot { QuickPause.isQuick(it) }
        settleThen {
            putOneOffs(others + quick)
            putBoolean(KEY_BLOCKING_ENABLED, true)
        }
    }

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

    /** Ends early every pause in progress; keys older than the longest pause are dropped on the way. */
    @Synchronized
    fun endPausesNow() {
        val now = LocalDateTime.now()
        val keep = pauseEndedKeys.filter { key ->
            Occurrence.dateOf(key)?.isBefore(now.toLocalDate().minusDays(Pause.MAX_NIGHTS + 1L)) == false
        }
        val active = PauseSchedule.activeAt(allPauses(), now).map { it.key }
        settleThen { putStringSet(KEY_PAUSE_ENDED_KEYS, (keep + active).toSet()) }
    }

    /** Undoes [endPausesNow] ("Pause again" on Home). */
    fun resumePauses() = settleThen { remove(KEY_PAUSE_ENDED_KEYS) }

    /** The history, one entry per calendar day that had some break or held notification. */
    val history: Map<LocalDate, DayStats>
        get() = HistoryCodec.decode(prefs.getString(KEY_HISTORY, null).orEmpty())

    /**
     * Credits the break minutes lived since the last look at the clock ([FreeTimeLedger.settle]). Called on
     * every refresh, and before every change of a setting that decides whether the pause is on ([settleThen]),
     * so the time before a change counts with the old settings.
     */
    @Synchronized
    fun settleHistory(now: LocalDateTime = LocalDateTime.now()) {
        val seenAt = prefs.getLong(KEY_HISTORY_SEEN_AT, -1L)
        val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // First look: start counting from here
        if (seenAt < 0) {
            prefs.edit { putLong(KEY_HISTORY_SEEN_AT, nowMillis) }
            return
        }
        // The clock went back: wait until it passes the last look again, so no minute counts twice
        if (nowMillis <= seenAt) return
        val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(seenAt), ZoneId.systemDefault())
        val config = FreeTimeLedger.Config(isBlockingEnabled, getBlockedApps(), allPauses(), pauseEndedKeys, sleepWindow)
        val before = history
        val after = FreeTimeLedger.settle(before, from, now, config)
        prefs.edit {
            if (after != before) putString(KEY_HISTORY, HistoryCodec.encode(after))
            putLong(KEY_HISTORY_SEEN_AT, nowMillis)
        }
    }

    /** Settles the history, then writes a setting that can turn the pause on or off. */
    @Synchronized
    private fun settleThen(write: SharedPreferences.Editor.() -> Unit) {
        settleHistory()
        prefs.edit(action = write)
    }

    /** True when inside a pause, but every pause in progress was ended early by the user. */
    fun isPauseEndedNow(): Boolean {
        val now = LocalDateTime.now()
        val all = allPauses()
        return PauseSchedule.activeAt(all, now).isNotEmpty() && PauseSchedule.activeAt(all, now, pauseEndedKeys).isEmpty()
    }

    /** True while Nook really holds work notifications: on, inside a pause, not ended early. */
    fun isPausedNow(): Boolean = isBlockingEnabled && PauseSchedule.activeAt(allPauses(), LocalDateTime.now(), pauseEndedKeys).isNotEmpty()

    /** Key of the pause held notifications are counted in ([PauseSchedule.counterOccurrence]); null outside pauses. */
    fun currentWindowKey(): String? =
        PauseSchedule.counterOccurrence(allPauses(), getBlockedApps(), LocalDateTime.now(), pauseEndedKeys)?.key

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
        val days = FreeTimeLedger.addHeld(history, LocalDate.now())
        prefs.edit {
            putString(KEY_FILTERED_WINDOW, window)
            putInt(KEY_FILTERED_COUNT, filteredCount(window) + 1)
            putStringSet(KEY_FILTERED_KEYS, seen + entry)
            putString(KEY_HISTORY, HistoryCodec.encode(days))
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
        settleThen { putStringSet(KEY_BLOCKED_APPS, apps) }
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    /** Inside at least one pause, whether or not it was ended early. */
    fun isInOffHoursNow(): Boolean = PauseSchedule.activeAt(allPauses(), LocalDateTime.now()).isNotEmpty()

    fun shouldBlock(packageName: String): Boolean = PauseSchedule.shouldBlock(
        packageName = packageName,
        blockingEnabled = isBlockingEnabled,
        workApps = getBlockedApps(),
        pauses = allPauses(),
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
        const val KEY_ONE_OFF_PAUSES = "one_off_pauses"
        const val KEY_HOLIDAYS_ENABLED = "holidays_enabled"
        const val KEY_WORK_SCHEDULE = "work_schedule"
        const val KEY_SLEEP_START = "sleep_start"
        const val KEY_SLEEP_END = "sleep_end"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val LEGACY_PAUSE_ID = "main"
        private const val KEY_HISTORY = "history"
        private const val KEY_HISTORY_SEEN_AT = "history_seen_at"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
