package com.pasquale.notificationblocker.tile

import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.zen.TimeOfDay
import com.pasquale.notificationblocker.ui.zen.ZenEnvironment
import java.time.LocalDateTime

/** Icon of the Quick Settings tile: a leaf in the morning, the sun by day, a lantern in the evening. */
enum class TileIcon { LEAF, SUN, LANTERN }

/** What the tile subtitle says. */
sealed interface TileSubtitle {
    data object Off : TileSubtitle
    data object AllDay : TileSubtitle
    /** Blocking now, until [end]. */
    data class Until(val end: Int) : TileSubtitle
    /** Armed, starts at [start]. */
    data class From(val start: Int) : TileSubtitle
}

/**
 * Pure description of the "Unplug & Sun" tile at a given moment: active when blocking is enabled,
 * label and icon follow the light of the day (same model as the zen scene and notification).
 */
data class ZenTileState(
    val active: Boolean,
    val icon: TileIcon,
    val evening: Boolean,
    val subtitle: TileSubtitle,
) {
    companion object {
        fun resolve(now: LocalDateTime, blockingEnabled: Boolean, start: Int, end: Int): ZenTileState {
            val minute = now.hour * 60 + now.minute
            val (sunrise, sunset) = ZenEnvironment.sunTimes(now.toLocalDate())
            val icon = when (ZenEnvironment.timeOfDay(minute, sunrise, sunset)) {
                TimeOfDay.DAWN -> TileIcon.LEAF
                TimeOfDay.DAYLIGHT -> TileIcon.SUN
                TimeOfDay.GOLDEN_HOUR, TimeOfDay.NIGHT -> TileIcon.LANTERN
            }
            val subtitle = when {
                !blockingEnabled -> TileSubtitle.Off
                start == end -> TileSubtitle.AllDay
                OffHours.isWithin(minute, start, end) -> TileSubtitle.Until(end)
                else -> TileSubtitle.From(start)
            }
            return ZenTileState(blockingEnabled, icon, evening = icon == TileIcon.LANTERN, subtitle = subtitle)
        }
    }
}
