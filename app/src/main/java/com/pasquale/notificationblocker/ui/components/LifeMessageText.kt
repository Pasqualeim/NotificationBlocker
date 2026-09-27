package com.pasquale.notificationblocker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.zen.LifeMessage

/** Localized text of a [LifeMessage]. */
@Composable
fun lifeMessageText(message: LifeMessage): String = when (message) {
    LifeMessage.TurnOn -> stringResource(R.string.life_turn_on)
    is LifeMessage.SunLeft -> stringResource(R.string.life_sun_left, durationText(message.minutes))
    LifeMessage.EveningIsYours -> stringResource(R.string.life_evening_yours)
    LifeMessage.DayBeginning -> stringResource(R.string.life_day_beginning)
    is LifeMessage.SunAfterWork -> stringResource(R.string.life_sun_after_work, OffHours.format(message.start), durationText(message.minutes))
    is LifeMessage.EveningAfterWork -> stringResource(R.string.life_evening_after_work, OffHours.format(message.start))
}

/** True when the message is about daylight (sun icon), false for evening and night (moon icon). */
fun LifeMessage.isSunny(): Boolean = this is LifeMessage.SunLeft || this is LifeMessage.SunAfterWork || this == LifeMessage.TurnOn

/** "3 h 20 min", "3 h" or "45 min". */
@Composable
fun durationText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.life_duration_minutes, m)
        m == 0 -> stringResource(R.string.life_duration_hours, h)
        else -> stringResource(R.string.duration_format, h, m)
    }
}
