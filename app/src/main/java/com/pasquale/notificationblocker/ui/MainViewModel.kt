package com.pasquale.notificationblocker.ui

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasquale.notificationblocker.data.MorningReport
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.notification.ZenNotificationManager
import com.pasquale.notificationblocker.tile.ZenTileService
import com.pasquale.notificationblocker.ui.components.MorningReportUi
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

    private val _startTimeMinutes = MutableStateFlow(preferencesManager.startTimeMinutes)
    val startTimeMinutes: StateFlow<Int> = _startTimeMinutes.asStateFlow()

    private val _endTimeMinutes = MutableStateFlow(preferencesManager.endTimeMinutes)
    val endTimeMinutes: StateFlow<Int> = _endTimeMinutes.asStateFlow()

    private val _isInOffHoursNow = MutableStateFlow(preferencesManager.isInOffHoursNow())
    val isInOffHoursNow: StateFlow<Boolean> = _isInOffHoursNow.asStateFlow()

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

    // The Quick Settings tile writes the same preference: follow it live (kept as a field, the
    // SharedPreferences registry only holds listeners weakly)
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferencesManager.KEY_BLOCKING_ENABLED) {
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
        // Turning blocking back on is an explicit request: show the zen notification again
        if (enabled) preferencesManager.zenDismissedWindow = null
        refreshZenNotification()
        refreshLife()
        ZenTileService.requestUpdate(getApplication())
    }

    fun setStartTime(minutes: Int) {
        preferencesManager.startTimeMinutes = minutes
        _startTimeMinutes.value = minutes
        refreshOffHoursStatus()
        ZenTileService.requestUpdate(getApplication())
    }

    fun setEndTime(minutes: Int) {
        preferencesManager.endTimeMinutes = minutes
        _endTimeMinutes.value = minutes
        refreshOffHoursStatus()
        ZenTileService.requestUpdate(getApplication())
    }

    /** Called on resume: the clock may have crossed the window boundary while the app was away. */
    fun refreshOffHoursStatus() {
        _isInOffHoursNow.value = preferencesManager.isInOffHoursNow()
        refreshZenNotification()
        refreshLife()
        refreshMorningReport()
    }

    /** Shows what waited outside during the last window, once it is over and until dismissed. */
    private fun refreshMorningReport() {
        val report = preferencesManager.lastReport()
        val currentWindow = if (preferencesManager.isInOffHoursNow()) preferencesManager.currentWindowKey() else null
        if (report == null || !MorningReport.shouldShow(report, LocalDate.now(), currentWindow, preferencesManager.reportSeenWindow)) {
            shownReport = null
            _morningReport.value = null
            return
        }
        val key = report.window to report.total
        if (shownReport == key) return
        shownReport = key
        val pm = getApplication<Application>().packageManager
        val names = report.apps.take(REPORT_NAMED_APPS).map { app ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, 0)).toString() }
                .getOrDefault(app.packageName)
        }
        _morningReport.value = MorningReportUi(
            total = report.total,
            appNames = names,
            moreApps = report.apps.size - names.size,
        )
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

    private fun computeLifeMessage() = LifeCopy.message(
        now = LocalDateTime.now(),
        blockingEnabled = preferencesManager.isBlockingEnabled,
        start = preferencesManager.startTimeMinutes,
        end = preferencesManager.endTimeMinutes,
    )

    private fun computeSunshine() =
        LifeCopy.sunshineInWindow(LocalDate.now(), preferencesManager.startTimeMinutes, preferencesManager.endTimeMinutes)

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
