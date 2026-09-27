package com.pasquale.notificationblocker.notification

import android.Manifest
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
import java.time.LocalDateTime

/**
 * The "zen" status notification: silent, ongoing, shown while blocking is active inside the
 * off-hours window. It shows a small animated illustration for the phase of the day, a calm
 * message, the end of the window and how many work notifications were held.
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
            filteredCount = prefs::filteredCount,
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

    private fun build(context: Context, state: ZenNotificationState) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_zen)
        .setContentTitle(message(context, state.phase))
        .setContentText(subtitle(context, state))
        .setCustomContentView(views(context, state, R.layout.notification_zen_collapsed))
        .setCustomBigContentView(views(context, state, R.layout.notification_zen_expanded))
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .setDeleteIntent(
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, DismissReceiver::class.java).putExtra(EXTRA_WINDOW, state.window),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .apply { state.timeoutMillis?.let(::setTimeoutAfter) }
        .build()

    private fun views(context: Context, state: ZenNotificationState, layout: Int) =
        RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.zen_title, message(context, state.phase))
            setTextViewText(R.id.zen_text, subtitle(context, state))
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

    fun message(context: Context, phase: ZenPhase): String = context.getString(
        when (phase) {
            ZenPhase.MORNING -> R.string.zen_msg_morning
            ZenPhase.DAYLIGHT -> R.string.zen_msg_daylight
            ZenPhase.SUNSET -> R.string.zen_msg_sunset
            ZenPhase.NIGHT -> R.string.zen_msg_night
        },
    )

    private fun subtitle(context: Context, state: ZenNotificationState): String {
        val until = state.endMinutes?.let { context.getString(R.string.zen_until, OffHours.format(it)) }
            ?: context.getString(R.string.zen_all_day)
        val held = if (state.filteredCount == 0) {
            context.getString(R.string.zen_filtered_none)
        } else {
            context.resources.getQuantityString(R.plurals.zen_filtered, state.filteredCount, state.filteredCount)
        }
        return context.getString(R.string.zen_subtitle, until, held)
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
}
