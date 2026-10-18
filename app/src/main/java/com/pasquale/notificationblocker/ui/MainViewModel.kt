package com.pasquale.notificationblocker.ui

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasquale.notificationblocker.data.DayStats
import com.pasquale.notificationblocker.data.MorningReport
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.DaysOff
import com.pasquale.notificationblocker.data.PauseSchedule
import com.pasquale.notificationblocker.data.QuickPause
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.data.SleepWindow
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.notification.ZenNotificationManager
import com.pasquale.notificationblocker.tile.ZenTileService
import com.pasquale.notificationblocker.ui.components.MorningReportUi
import com.pasquale.notificationblocker.ui.components.ReportApp
import com.pasquale.notificationblocker.ui.zen.LifeCopy
import com.pasquale.notificationblocker.ui.zen.LifeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

data class AppInfo(
    val packageName: String,
    val name: String,
    val isBlocked: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)

    private val _isBlockingEnabled = MutableStateFlow(preferencesManager.isBlockingEnabled)
    val isBlockingEnabled: StateFlow<Boolean> = _isBlockingEnabled.asStateFlow()


    private val _isInOffHoursNow = MutableStateFlow(preferencesManager.isInOffHoursNow())
    val isInOffHoursNow: StateFlow<Boolean> = _isInOffHoursNow.asStateFlow()

    // Inside the window, but ended early from the break notification
    private val _isPauseEnded = MutableStateFlow(preferencesManager.isPauseEndedNow())
    val isPauseEnded: StateFlow<Boolean> = _isPauseEnded.asStateFlow()

    private val _pauses = MutableStateFlow(preferencesManager.pauses)
    val pauses: StateFlow<List<Pause>> = _pauses.asStateFlow()

    // Regular pauses plus the one-offs ("Pause now", days off): what Today shows
    private val _allPauses = MutableStateFlow(preferencesManager.allPauses())
    val allPauses: StateFlow<List<Pause>> = _allPauses.asStateFlow()

    // Days off: the ranges the user added, and whether public holidays count
    private val _daysOff = MutableStateFlow(DaysOff.ranges(preferencesManager.oneOffPauses).filterNot { it.last.isBefore(LocalDate.now()) })
    val daysOff: StateFlow<List<DaysOff.Range>> = _daysOff.asStateFlow()

    private val _holidaysEnabled = MutableStateFlow(preferencesManager.holidaysEnabled)
    val holidaysEnabled: StateFlow<Boolean> = _holidaysEnabled.asStateFlow()

    // When the user works: the rest is paused (null: never set, the extra pauses alone apply)
    private val _workSchedule = MutableStateFlow(preferencesManager.workSchedule)
    val workSchedule: StateFlow<WorkSchedule?> = _workSchedule.asStateFlow()

    fun setWorkSchedule(schedule: WorkSchedule) {
        preferencesManager.workSchedule = schedule
        _workSchedule.value = schedule
        onPausesChanged()
        refreshZenNotification()
    }

    // When the user usually sleeps: held, but not counted as free time
    private val _sleepWindow = MutableStateFlow(preferencesManager.sleepWindow)
    val sleepWindow: StateFlow<SleepWindow> = _sleepWindow.asStateFlow()

    /** Minutes to the next pause start when it is close enough for "until the next break", else null. */
    private val _minutesToNextPause = MutableStateFlow(computeMinutesToNext())
    val minutesToNextPause: StateFlow<Int?> = _minutesToNextPause.asStateFlow()

    // Pauses ended early ("End the pause"), for the state shown next to each pause
    private val _pauseEndedKeys = MutableStateFlow(preferencesManager.pauseEndedKeys)
    val pauseEndedKeys: StateFlow<Set<String>> = _pauseEndedKeys.asStateFlow()

    private val _history = MutableStateFlow(preferencesManager.history)
    val history: StateFlow<Map<LocalDate, DayStats>> = _history.asStateFlow()

    /** Minute of the day the next pause starts (for "Pause again" and the status card), null if none is set. */
    private val _nextPauseStart = MutableStateFlow(computeNextPauseStart())
    val nextPauseStart: StateFlow<Int?> = _nextPauseStart.asStateFlow()

    private val _blockedAppsCount = MutableStateFlow(preferencesManager.getBlockedApps().size)
    val blockedAppsCount: StateFlow<Int> = _blockedAppsCount.asStateFlow()

    private val _zenPromptDismissed = MutableStateFlow(preferencesManager.zenPromptDismissed)
    val zenPromptDismissed: StateFlow<Boolean> = _zenPromptDismissed.asStateFlow()

    private val _lifeMessage = MutableStateFlow(computeLifeMessage())
    val lifeMessage: StateFlow<LifeMessage> = _lifeMessage.asStateFlow()

    private val _sunshineMinutes = MutableStateFlow(computeSunshine())
    val sunshineMinutes: StateFlow<Int> = _sunshineMinutes.asStateFlow()

    private val _morningReport = MutableStateFlow<MorningReportUi?>(null)
    val morningReport: StateFlow<MorningReportUi?> = _morningReport.asStateFlow()

    // Window and total of the report on screen, so the minute tick doesn't re-read app labels
    private var shownReport: Pair<String, Int>? = null

    // The Quick Settings tile and the break notification write these preferences: follow them live
    // (kept as a field, the SharedPreferences registry only holds listeners weakly)
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferencesManager.KEY_BLOCKING_ENABLED || key == PreferencesManager.KEY_PAUSE_ENDED_KEYS ||
            key == PreferencesManager.KEY_PAUSES || key == PreferencesManager.KEY_ONE_OFF_PAUSES ||
            key == PreferencesManager.KEY_HOLIDAYS_ENABLED || key == PreferencesManager.KEY_WORK_SCHEDULE) {
            _isBlockingEnabled.value = preferencesManager.isBlockingEnabled
            refreshOffHoursStatus()
        }
    }

    init {
        preferencesManager.registerListener(prefsListener)
        refreshMorningReport()
    }

    override fun onCleared() {
        preferencesManager.unregisterListener(prefsListener)
    }

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private var loadAppsJob: Job? = null

    fun setBlockingEnabled(enabled: Boolean) {
        preferencesManager.isBlockingEnabled = enabled
        _isBlockingEnabled.value = enabled
        _isPauseEnded.value = preferencesManager.isPauseEndedNow()
        // Turning blocking back on is an explicit request: show the zen notification again
        if (enabled) preferencesManager.zenDismissedWindow = null
        refreshZenNotification()
        refreshLife()
        ZenTileService.requestUpdate(getApplication())
    }


    /** Brings back a pause ended early from the break notification. */
    fun pauseAgain() {
        ZenNotificationManager.resumePause(getApplication())
        refreshOffHoursStatus()
    }

    /** Called on resume: the clock may have crossed the window boundary while the app was away. */
    fun refreshOffHoursStatus() {
        preferencesManager.settleHistory()
        _pauses.value = preferencesManager.pauses
        _workSchedule.value = preferencesManager.workSchedule
        _allPauses.value = preferencesManager.allPauses()
        _daysOff.value = DaysOff.ranges(preferencesManager.oneOffPauses).filterNot { it.last.isBefore(LocalDate.now()) }
        _holidaysEnabled.value = preferencesManager.holidaysEnabled
        _minutesToNextPause.value = computeMinutesToNext()
        _pauseEndedKeys.value = preferencesManager.pauseEndedKeys
        _history.value = preferencesManager.history
        _nextPauseStart.value = computeNextPauseStart()

        _isInOffHoursNow.value = preferencesManager.isInOffHoursNow()
        _isPauseEnded.value = preferencesManager.isPauseEndedNow()
        refreshZenNotification()
        refreshLife()
        refreshMorningReport()
    }

    /** Shows what waited outside during the last window, once it is over and until dismissed. */
    private fun refreshMorningReport() {
        val report = preferencesManager.lastReport()
        // A pause ended early counts as over: what was held so far is shown right away
        val currentWindow = if (preferencesManager.isPausedNow()) preferencesManager.currentWindowKey() else null
        if (report == null || !MorningReport.shouldShow(report, LocalDate.now(), currentWindow, preferencesManager.reportSeenWindow)) {
            shownReport = null
            _morningReport.value = null
            return
        }
        val key = report.window to report.total
        if (shownReport == key) return
        shownReport = key
        val pm = getApplication<Application>().packageManager
        val apps = report.apps.take(REPORT_NAMED_APPS).map { app ->
            val name = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, 0)).toString() }
                .getOrDefault(app.packageName)
            ReportApp(app.packageName, name, app.count)
        }
        _morningReport.value = MorningReportUi(
            total = report.total,
            apps = apps,
            moreApps = report.apps.size - apps.size,
        )
    }

    /** Opens [packageName] from the report: its held notifications are only there now. */
    fun openApp(packageName: String) {
        val app = getApplication<Application>()
        val intent = app.packageManager.getLaunchIntentForPackage(packageName) ?: return
        // An app updated or disabled since the report was read: nothing to open
        runCatching { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun onMorningReportDismissed() {
        preferencesManager.reportSeenWindow = shownReport?.first ?: return
        shownReport = null
        _morningReport.value = null
    }

    private fun refreshLife() {
        _lifeMessage.value = computeLifeMessage()
        _sunshineMinutes.value = computeSunshine()
    }

    private fun computeLifeMessage(): LifeMessage {
        val pause = focusPause()
        return LifeCopy.message(
            now = LocalDateTime.now(),
            blockingEnabled = preferencesManager.isBlockingEnabled && pause != null,
            start = pause?.start ?: 0,
            end = pause?.end ?: 0,
        )
    }

    private fun computeSunshine() = focusPause()?.let { LifeCopy.sunshineInWindow(LocalDate.now(), it.start, it.end) } ?: 0

    /** The pause the free-time line talks about: the one in progress, or else the next one to start. */
    private fun focusPause(): Pause? {
        val now = LocalDateTime.now()
        val pauses = preferencesManager.allPauses()
        val ended = preferencesManager.pauseEndedKeys
        PauseSchedule.activeAt(pauses, now, ended).firstOrNull()?.let { return it.pause }
        return PauseSchedule.nextOccurrence(pauses, now, ended)?.pause
    }

    private fun computeNextPauseStart(): Int? =
        PauseSchedule.nextStart(preferencesManager.allPauses(), LocalDateTime.now(), preferencesManager.pauseEndedKeys)
            ?.let { it.hour * 60 + it.minute }

    private fun computeMinutesToNext(): Int? =
        QuickPause.minutesToNext(preferencesManager.allPauses(), LocalDateTime.now(), preferencesManager.pauseEndedKeys)

    /**
     * "Pause now" for [minutes], or until the next break when [untilNext] (measured now, so it hands
     * over with no gap even if the sheet stayed open). Turns Nook on; the notification and tile follow.
     */
    fun startQuickPause(minutes: Int, untilNext: Boolean = false) {
        val length = if (untilNext) computeMinutesToNext() ?: minutes else minutes
        preferencesManager.startQuickPause(length)
        _isBlockingEnabled.value = true
        preferencesManager.zenDismissedWindow = null
        refreshOffHoursStatus()
        refreshZenNotification()
        refreshLife()
        ZenTileService.requestUpdate(getApplication())
    }

    fun addDaysOff(first: LocalDate, last: LocalDate) {
        preferencesManager.addDaysOff(first, last)
        onPausesChanged()
        refreshZenNotification()
    }

    fun removeDaysOff(id: String) {
        preferencesManager.removeDaysOff(id)
        onPausesChanged()
        refreshZenNotification()
    }

    fun setSleepWindow(window: SleepWindow) {
        preferencesManager.sleepWindow = window
        _sleepWindow.value = window
        _history.value = preferencesManager.history
    }

    fun setHolidaysEnabled(enabled: Boolean) {
        preferencesManager.holidaysEnabled = enabled
        onPausesChanged()
        refreshZenNotification()
    }

    /** True on a fresh install until the first-launch setup is done. Read once, when the app starts. */
    val needsOnboarding: Boolean = preferencesManager.needsOnboarding

    /** Saves the breaks picked in the first-launch setup and turns Nook on. */
    fun completeOnboarding(schedule: WorkSchedule, pauses: List<Pause>) {
        preferencesManager.completeOnboarding(schedule, pauses)
        _isBlockingEnabled.value = true
        onPausesChanged()
        refreshZenNotification()
        refreshLife()
    }

    /** Adds [pause], or replaces the one with the same id. */
    fun savePause(pause: Pause) {
        val all = preferencesManager.pauses
        preferencesManager.pauses = if (all.any { it.id == pause.id }) all.map { if (it.id == pause.id) pause else it } else all + pause
        onPausesChanged()
    }

    fun deletePause(id: String) {
        preferencesManager.pauses = preferencesManager.pauses.filterNot { it.id == id }
        onPausesChanged()
    }

    fun setPauseEnabled(id: String, enabled: Boolean) {
        val pause = preferencesManager.pauses.firstOrNull { it.id == id } ?: return
        savePause(pause.copy(enabled = enabled))
    }

    /** Turns [packageName] on or off for one pause; the first change copies the work apps as a start. */
    fun setPauseApp(id: String, packageName: String, blocked: Boolean) {
        val pause = preferencesManager.pauses.firstOrNull { it.id == id } ?: return
        val apps = pause.apps ?: preferencesManager.getBlockedApps()
        savePause(pause.copy(apps = if (blocked) apps + packageName else apps - packageName))
    }

    /** Back to "all the work apps" for one pause. */
    fun resetPauseApps(id: String) {
        val pause = preferencesManager.pauses.firstOrNull { it.id == id } ?: return
        savePause(pause.copy(apps = null))
    }

    /** A fresh id for a new pause: short, stable, never reused. */
    fun newPauseId(): String = "p" + System.currentTimeMillis().toString(36)

    private fun onPausesChanged() {
        refreshOffHoursStatus()
        ZenTileService.requestUpdate(getApplication())
    }

    /** Posts, updates or removes the zen status notification to match the current state. */
    fun refreshZenNotification() = ZenNotificationManager.refresh(getApplication())

    /** The user answered the notification permission prompt ("Not now" counts as a no). */
    fun onZenPermissionResult(granted: Boolean) {
        if (!granted) {
            preferencesManager.zenPromptDismissed = true
            _zenPromptDismissed.value = true
        }
        refreshZenNotification()
    }

    fun toggleAppBlocked(packageName: String, blocked: Boolean) {
        preferencesManager.setAppBlocked(packageName, blocked)
        _blockedAppsCount.value = preferencesManager.getBlockedApps().size
        // The first work app makes the break notification meaningful, removing the last one hides it
        refreshZenNotification()

        // Update local state without full reload
        _installedApps.value = _installedApps.value.map { appInfo ->
            if (appInfo.packageName == packageName) appInfo.copy(isBlocked = blocked) else appInfo
        }
    }

    /** Loads the app list, or refreshes it quietly (no skeleton) when it is already on screen. */
    fun loadInstalledApps() {
        if (loadAppsJob?.isActive == true) return

        loadAppsJob = viewModelScope.launch {
            if (_installedApps.value.isEmpty()) _isLoadingApps.value = true
            try {
                val apps = withContext(Dispatchers.IO) { queryLaunchableApps() }
                // Toggles made while the query ran win over the snapshot it read
                val blockedApps = preferencesManager.getBlockedApps()
                _installedApps.value = apps.map { it.copy(isBlocked = it.packageName in blockedApps) }
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    // Launchable apps (incl. preinstalled ones like Gmail/Teams), matching the <queries> manifest entry
    private fun queryLaunchableApps(): List<AppInfo> {
        val app = getApplication<Application>()
        val pm = app.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val blockedApps = preferencesManager.getBlockedApps()

        return pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.applicationInfo }
            .filter { it.packageName != app.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                AppInfo(
                    packageName = info.packageName,
                    name = pm.getApplicationLabel(info).toString(),
                    isBlocked = info.packageName in blockedApps
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private companion object {
        const val REPORT_NAMED_APPS = 2
    }
}
