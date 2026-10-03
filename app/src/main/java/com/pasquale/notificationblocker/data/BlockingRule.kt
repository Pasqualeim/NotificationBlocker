package com.pasquale.notificationblocker.data

/** Pure blocking rule: what [PreferencesManager.shouldBlock] evaluates with the real clock and prefs. */
object BlockingRule {

    fun shouldBlock(
        packageName: String,
        blockingEnabled: Boolean,
        blockedApps: Set<String>,
        currentMinutes: Int,
        start: Int,
        end: Int,
        endedEarly: Boolean = false,
    ): Boolean =
        blockingEnabled && OffHours.isWithin(currentMinutes, start, end) && !endedEarly && packageName in blockedApps
}
