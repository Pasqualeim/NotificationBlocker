package com.pasquale.notificationblocker.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.PreferencesManager
import com.pasquale.notificationblocker.notification.ZenNotificationManager
import java.time.LocalDateTime

/**
 * Quick Settings tile "Unplug & Sun": one tap toggles blocking. The app picks up the change through
 * its SharedPreferences listener, the zen notification is refreshed right away.
 *
 * Declared as an active tile: it only redraws when the shade opens or when [requestUpdate] is called
 * (e.g. after toggling from the app), never on a timer.
 */
class ZenTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val prefs = PreferencesManager.getInstance(this)
        val enable = !prefs.isBlockingEnabled
        prefs.isBlockingEnabled = enable
        // Turning blocking on is an explicit request: show the zen notification again
        if (enable) prefs.zenDismissedWindow = null
        ZenNotificationManager.refresh(this)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val prefs = PreferencesManager.getInstance(this)
        val state = ZenTileState.resolve(LocalDateTime.now(), prefs.isBlockingEnabled, prefs.startTimeMinutes, prefs.endTimeMinutes)
        tile.state = if (state.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(if (state.evening) R.string.tile_label_evening else R.string.tile_label_day)
        tile.icon = Icon.createWithResource(
            this,
            when (state.icon) {
                TileIcon.LEAF -> R.drawable.ic_tile_leaf
                TileIcon.SUN -> R.drawable.ic_tile_sun
                TileIcon.LANTERN -> R.drawable.ic_tile_lantern
            },
        )
        val subtitle = when (val s = state.subtitle) {
            TileSubtitle.Off -> getString(R.string.tile_subtitle_off)
            TileSubtitle.AllDay -> getString(R.string.zen_all_day)
            is TileSubtitle.Until -> getString(R.string.zen_until, OffHours.format(s.end))
            is TileSubtitle.From -> getString(R.string.tile_subtitle_from, OffHours.format(s.start))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tile.stateDescription = subtitle
        tile.updateTile()
    }

    companion object {
        /** Asks the system to redraw the tile (no-op if the user has not added it). */
        fun requestUpdate(context: Context) {
            runCatching { requestListeningState(context, ComponentName(context, ZenTileService::class.java)) }
        }
    }
}
