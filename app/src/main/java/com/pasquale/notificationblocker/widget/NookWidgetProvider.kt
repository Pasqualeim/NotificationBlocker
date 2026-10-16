package com.pasquale.notificationblocker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.pasquale.notificationblocker.MainActivity
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.notification.ZenNotificationManager
import com.pasquale.notificationblocker.tile.ZenTileService
import com.pasquale.notificationblocker.ui.components.pauseLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

/**
 * The home screen widget: Nook's status, the pause in progress or the next one, and "Pause 1 h".
 * RemoteViews (no Glance dependency). Redrawn by [update] from [ZenNotificationManager.refresh], so
 * it follows the clock every minute with the screen on, like the break notification; the system's
 * 30-minute update is only a fallback.
 */
class NookWidgetProvider : AppWidgetProvider() {

    // The system asks for a drawing (new widget, the 30-minute fallback, a host that lost its views): always send it
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context, force = true)

    override fun onDisabled(context: Context) {
        shownKey = null
    }

    /**
     * "Pause 1 h" from the widget. A separate receiver, not exported: the provider must be exported for
     * the system's widget broadcasts, and other apps must not be able to start a pause.
     */
    class QuickPauseReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val app = context.applicationContext
            PreferencesManager.getInstance(app).startQuickPause(QUICK_MINUTES)
            ZenTileService.requestUpdate(app)
            // Refreshes the break notification and this widget
            ZenNotificationManager.refresh(app)
        }
    }

    companion object {
        /** The widget offers one length: the most common ask, longer ones are a tap away in the app. */
        private const val QUICK_MINUTES = 60

        /** Content of the last views sent, so the minute tick redraws only when something changed. */
        @Volatile
        private var shownKey: String? = null

        /** Redraws the widgets; unless [force], only when their text changed since the last drawing. */
        fun update(context: Context, force: Boolean = false) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, NookWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val prefs = PreferencesManager.getInstance(app)
            val now = LocalDateTime.now()
            val state = NookWidgetState.resolve(now, prefs.isBlockingEnabled, prefs.allPauses(), prefs.pauseEndedKeys)
            val title = app.getString(
                when (state.kind) {
                    NookWidgetState.Kind.OFF -> R.string.widget_title_off
                    NookWidgetState.Kind.PAUSED -> R.string.widget_title_paused
                    NookWidgetState.Kind.ENDED -> R.string.status_ended
                    NookWidgetState.Kind.BETWEEN -> R.string.widget_title_work
                },
            )
            val subtitle = subtitle(app, state, now.toLocalDate())
            val key = "${ids.joinToString()}|$title|$subtitle|${state.offersQuickPause}"
            if (!force && key == shownKey) return
            val views = RemoteViews(app.packageName, R.layout.widget_nook).apply {
                setTextViewText(R.id.widget_title, title)
                setTextViewText(R.id.widget_subtitle, subtitle)
                setViewVisibility(R.id.widget_quick, if (state.offersQuickPause) View.VISIBLE else View.GONE)
                setOnClickPendingIntent(R.id.widget_root, openApp(app))
                setOnClickPendingIntent(R.id.widget_quick, quickPause(app))
            }
            manager.updateAppWidget(ids, views)
            shownKey = key
        }

        private fun subtitle(context: Context, state: NookWidgetState, today: LocalDate): String {
            val pause = state.pause
            val name = pause?.let { pauseLabel(context.resources, it) }
            return when (state.kind) {
                NookWidgetState.Kind.OFF -> context.getString(R.string.widget_sub_off)
                NookWidgetState.Kind.PAUSED -> when (val minute = state.minute) {
                    null -> context.getString(R.string.widget_sub_paused_all_day, name)
                    else -> context.getString(R.string.widget_sub_paused, name, whenText(context, state.date, today, minute))
                }
                NookWidgetState.Kind.ENDED, NookWidgetState.Kind.BETWEEN ->
                    if (name == null || state.minute == null) {
                        context.getString(R.string.widget_sub_none)
                    } else {
                        context.getString(R.string.widget_sub_next, name, whenText(context, state.date, today, state.minute))
                    }
            }
        }

        /** "13:00", "tomorrow 08:30" or "Sat 08:30". */
        private fun whenText(context: Context, date: LocalDate?, today: LocalDate, minute: Int): String {
            val time = OffHours.format(minute)
            return when (date) {
                null, today -> time
                today.plusDays(1) -> context.getString(R.string.widget_tomorrow, time)
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, context.resources.configuration.locales[0]) + " " + time
            }
        }

        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun quickPause(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, QuickPauseReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
