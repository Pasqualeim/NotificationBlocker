package com.pasquale.notificationblocker.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pasquale.notificationblocker.MainActivity
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.tile.ZenTileService
import java.time.LocalDateTime

/**
 * The break status notification ("zen" in code): silent, ongoing, shown only while Nook really holds
 * work notifications (see [ZenNotificationState.resolve]). Title: until when work is paused; text:
 * how many were paused and from which apps (names hidden on the lock screen); expanded: a miniature of the
 * lake pier for the phase of the day and a calm line. "End the pause" lets work through until the
 * next window: the notification goes away, and Home offers "Pause again".
 *
 * No alarms (see CLAUDE.md): the system removes it at the end of the window through
 * [NotificationCompat.Builder.setTimeoutAfter]; [refresh] is called by the app and by the listener
 * service (every minute while the screen is on, on unlock, on time changes, on each held notification),
 * and only re-posts when the visible content changes.
 */
object ZenNotificationManager {

    const val CHANNEL_ID = "zen_status"
    private const val NOTIFICATION_ID = 1001
    private const val UNKNOWN = "?"

    /** Content key of what is currently posted; null = nothing; [UNKNOWN] = not known yet in this process. */
    @Volatile
    private var postedKey: String? = UNKNOWN

    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasListenerAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    @Synchronized
    fun refresh(context: Context) {
        val app = context.applicationContext
        val prefs = PreferencesManager.getInstance(app)
        val state = ZenNotificationState.resolve(
            now = LocalDateTime.now(),
            blockingEnabled = prefs.isBlockingEnabled,
            start = prefs.startTimeMinutes,
            end = prefs.endTimeMinutes,
            dismissedWindow = prefs.zenDismissedWindow,
            pauseEndedWindow = prefs.pauseEndedWindow,
            // Without work apps or without the notification access Nook holds nothing: no "paused" claim
            canHold = prefs.getBlockedApps().isNotEmpty() && hasListenerAccess(app),
            held = { window ->
                val report = prefs.lastReport()?.takeIf { it.window == window }
                (report?.total ?: 0) to report?.apps?.map { it.packageName }.orEmpty()
            },
        )
        val manager = NotificationManagerCompat.from(app)
        if (state == null || !canPost(app)) {
            if (postedKey != null) manager.cancel(NOTIFICATION_ID)
            postedKey = null
            return
        }
        if (state.contentKey == postedKey) return
        ensureChannel(app)
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            manager.notify(NOTIFICATION_ID, build(app, state))
            postedKey = state.contentKey
        }
    }

    /** Ends the pause of [window] early: work notifications come through until the next window. */
    @Synchronized
    fun endPause(context: Context, window: String) {
        val app = context.applicationContext
        PreferencesManager.getInstance(app).pauseEndedWindow = window
        refresh(app)
        ZenTileService.requestUpdate(app)
    }

    /** Undoes [endPause] ("Pause again" on Home): the pause holds work notifications again. */
    @Synchronized
    fun resumePause(context: Context) {
        val app = context.applicationContext
        PreferencesManager.getInstance(app).pauseEndedWindow = null
        refresh(app)
        ZenTileService.requestUpdate(app)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.zen_channel_name))
            .setDescription(context.getString(R.string.zen_channel_description))
            .setShowBadge(false)
            .setVibrationEnabled(false)
            .setSound(null, null)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun build(context: Context, state: ZenNotificationState): Notification {
        val title = title(context, state)
        // On a locked screen that hides private content, the app names are left out
        val publicVersion = builder(context, state, title, heldText(context, state, withApps = false))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        return builder(context, state, title, heldText(context, state, withApps = true))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .addAction(
                0,
                context.getString(R.string.zen_action_end),
                broadcast(context, EndPauseReceiver::class.java, state.window),
            )
            .setDeleteIntent(broadcast(context, DismissReceiver::class.java, state.window))
            .build()
    }

    private fun builder(context: Context, state: ZenNotificationState, title: String, text: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_nook)
            .setContentTitle(title)
            .setContentText(text)
            .setCustomContentView(views(context, state, R.layout.notification_zen_collapsed, title, text))
            .setCustomBigContentView(views(context, state, R.layout.notification_zen_expanded, title, text))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp(context))
            .apply { state.timeoutMillis?.let(::setTimeoutAfter) }

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(context: Context, receiver: Class<out BroadcastReceiver>, window: String?) =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, receiver).apply { window?.let { putExtra(EXTRA_WINDOW, it) } },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun views(context: Context, state: ZenNotificationState, layout: Int, title: String, text: String) =
        RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.zen_title, title)
            setTextViewText(R.id.zen_text, text)
            if (layout == R.layout.notification_zen_expanded) setTextViewText(R.id.zen_message, message(context, state.phase))
            // Only one illustration is visible; ProgressBar starts its animated vector on its own
            val art = when (state.phase) {
                ZenPhase.MORNING, ZenPhase.DAYLIGHT -> R.id.zen_art_day
                ZenPhase.SUNSET -> R.id.zen_art_sunset
                ZenPhase.NIGHT -> R.id.zen_art_night
            }
            for (id in intArrayOf(R.id.zen_art_day, R.id.zen_art_sunset, R.id.zen_art_night)) {
                setViewVisibility(id, if (id == art) View.VISIBLE else View.GONE)
            }
        }

    /** "Work paused until 07:00", or "all day" for a whole-day window. */
    fun title(context: Context, state: ZenNotificationState): String =
        state.endMinutes?.let { context.getString(R.string.zen_title_until, OffHours.format(it)) }
            ?: context.getString(R.string.zen_title_all_day)

    fun message(context: Context, phase: ZenPhase): String = context.getString(
        when (phase) {
            ZenPhase.MORNING -> R.string.zen_msg_morning
            ZenPhase.DAYLIGHT -> R.string.zen_msg_daylight
            ZenPhase.SUNSET -> R.string.zen_msg_sunset
            ZenPhase.NIGHT -> R.string.zen_msg_night
        },
    )

    /** "3 notifications from Slack and Teams paused"; without names when [withApps] is false. */
    private fun heldText(context: Context, state: ZenNotificationState, withApps: Boolean): String {
        val count = state.filteredCount
        val res = context.resources
        if (count == 0) return context.getString(R.string.zen_held_none)
        val names = if (withApps) state.heldApps.take(ZenNotificationState.NAMED_APPS).map { label(context, it) } else emptyList()
        if (names.isEmpty()) return res.getQuantityString(R.plurals.zen_held, count, count)
        val more = state.heldApps.size - names.size
        val apps = when {
            more > 0 && names.size == 2 -> res.getQuantityString(R.plurals.zen_apps_more, more, names[0], names[1], more)
            names.size == 2 -> context.getString(R.string.zen_apps_two, names[0], names[1])
            else -> names[0]
        }
        return res.getQuantityString(R.plurals.zen_held_from, count, count, apps)
    }

    private fun label(context: Context, packageName: String): String {
        val pm = context.packageManager
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)
    }

    private const val EXTRA_WINDOW = "window"

    // Same lock as refresh(), so a swipe cannot interleave with a post in progress
    @Synchronized
    private fun onDismissed(context: Context, window: String) {
        PreferencesManager.getInstance(context).zenDismissedWindow = window
        postedKey = null
    }

    /** Swiped away (Android 14 lets users dismiss ongoing notifications): respect it until the next window. */
    class DismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val window = intent.getStringExtra(EXTRA_WINDOW) ?: return
            onDismissed(context, window)
        }
    }

    /** "End the pause" in the break notification. */
    class EndPauseReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val window = intent.getStringExtra(EXTRA_WINDOW) ?: return
            endPause(context, window)
        }
    }
}
