package com.pasquale.notificationblocker.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pasquale.notificationblocker.service.NotificationBlockerService

/** Whether Nook has the notification access it needs to hold work notifications. */
fun hasNotificationListenerPermission(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Whether Nook may post its break notification. Below Android 13 posting needs no runtime permission. */
fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Opens the system page that grants the notification access. From Android 11 it is Nook's own page,
 * with the one switch to turn on; the full list of apps (where people had to find Nook among many)
 * is the fallback for older versions and for systems that lack the page.
 */
fun openNotificationAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val component = ComponentName(context, NotificationBlockerService::class.java)
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Some makers drop the page: the list below always exists
        }
    }
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}
