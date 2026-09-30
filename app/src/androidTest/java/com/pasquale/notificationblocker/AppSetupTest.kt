package com.pasquale.notificationblocker

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks that the installed app is wired the way Play and Android expect. */
@RunWith(AndroidJUnit4::class)
class AppSetupTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pm = context.packageManager

    @Test
    fun applicationId_isTheFinalOne() = assertEquals("com.pasquale.nook", context.packageName)

    @Test
    fun launcherActivity_isResolvable() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        assertNotNull(pm.resolveActivity(intent, 0))
    }

    @Test
    fun listenerService_isProtectedByTheSystemPermission() {
        val service = pm.getServiceInfo(
            ComponentName(context, "com.pasquale.notificationblocker.service.NotificationBlockerService"), 0,
        )
        assertEquals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", service.permission)
        val intent = Intent(NotificationListenerService.SERVICE_INTERFACE).setPackage(context.packageName)
        assertTrue(pm.queryIntentServices(intent, 0).isNotEmpty())
    }

    @Test
    fun tileService_isProtectedByTheSystemPermission() {
        val tile = pm.getServiceInfo(ComponentName(context, "com.pasquale.notificationblocker.tile.ZenTileService"), 0)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", tile.permission)
    }

    @Test
    fun dismissReceiver_isNotExported() {
        val receiver = pm.getReceiverInfo(
            ComponentName(context, "com.pasquale.notificationblocker.notification.ZenNotificationManager\$DismissReceiver"), 0,
        )
        assertFalse(receiver.exported)
    }

    @Test
    fun noPermissionsBeyondNotifications() {
        val requested = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        val forbidden = setOf(
            "android.permission.INTERNET", "android.permission.QUERY_ALL_PACKAGES", "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED",
        )
        assertTrue("Unexpected permissions: ${requested intersect forbidden}", (requested intersect forbidden).isEmpty())
        assertTrue("android.permission.POST_NOTIFICATIONS" in requested)
    }
}
