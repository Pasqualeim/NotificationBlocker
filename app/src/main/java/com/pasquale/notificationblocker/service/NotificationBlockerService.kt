package com.pasquale.notificationblocker.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.notification.ZenNotificationManager

class NotificationBlockerService : NotificationListenerService() {

    private lateinit var prefs: PreferencesManager

    /**
     * Keeps the zen notification in step with the clock without alarms: TIME_TICK arrives every
     * minute only while the screen is on (when the shade can be seen), plus unlock and time changes.
     * The window end itself is handled by the notification's own timeout.
     */
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = ZenNotificationManager.refresh(context)
    }
    private var clockRegistered = false

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        // The off-hours window is evaluated at post time, so no alarm is needed to keep it in sync
        if (prefs.shouldBlock(notification.packageName)) {
            Log.d(TAG, "Canceling notification from ${notification.packageName}")
            cancelNotification(notification.key)
            // Group summaries duplicate their children; ongoing ones cannot be canceled at all
            val isSummary = notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
            if (notification.isClearable && !isSummary) {
                prefs.recordFiltered(prefs.currentWindowKey(), notification.key)
            }
        }
        if (notification.packageName != packageName) ZenNotificationManager.refresh(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        if (!clockRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(this, clockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            clockRegistered = true
        }
        ZenNotificationManager.refresh(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener disconnected")
        unregisterClock()
    }

    override fun onDestroy() {
        unregisterClock()
        super.onDestroy()
    }

    private fun unregisterClock() {
        if (!clockRegistered) return
        unregisterReceiver(clockReceiver)
        clockRegistered = false
    }

    private companion object {
        const val TAG = "NotificationBlocker"
    }
}
