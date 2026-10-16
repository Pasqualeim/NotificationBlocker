package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.theme.sunlight
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The greeting of the Today tab's top bar, for the time of day ([minute] from midnight). */
@Composable
fun todayGreeting(minute: Int): String = stringResource(
    when (minute / 60) {
        in 5..11 -> R.string.greeting_time_breathe
        in 12..17 -> R.string.greeting_enjoy_sun
        else -> R.string.greeting_welcome_life
    },
)

/** "venerdì 16 ottobre": the date under the greeting, in the phone's language. */
@Composable
fun todayDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(date).replaceFirstChar { it.titlecase(locale) }
}

/** The brand mark at the end of the Today top bar: the mug on a primary cookie shape. */
@Composable
fun NookBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, expressiveShape(0)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mug),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** "8 h 30" or "45 min": a duration short enough for a third of the screen. */
@Composable
fun compactDuration(minutes: Int): String =
    if (minutes < 60) stringResource(R.string.life_duration_minutes, minutes) else stringResource(R.string.duration_compact, minutes / 60, minutes % 60)

/** Three figures of today under the scene: free time, notifications paused, daylight in a pause. */
@Composable
fun TodayMetrics(livedMinutes: Int, held: Int, daylightMinutes: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val figure = MaterialTheme.typography.titleLarge
        MetricTile(
            icon = painterResource(R.drawable.ic_mug),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            container = MaterialTheme.colorScheme.secondaryContainer,
            shapeIndex = 0,
            value = { CountUpText(livedMinutes, figure, format = { compactDuration(it) }) },
            label = stringResource(R.string.metric_free),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = painterResource(R.drawable.ic_tile_sun),
            tint = MaterialTheme.colorScheme.sunlight,
            shapeIndex = 3,
            value = { CountUpText(daylightMinutes, figure, format = { compactDuration(it) }) },
            label = stringResource(R.string.metric_daylight),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = rememberVectorPainter(Icons.Filled.Notifications),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            shapeIndex = 1,
            value = { CountUpText(held, figure) },
            label = stringResource(R.string.metric_paused),
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, name = "Today badge and metrics")
@Composable
fun TodayHeaderPreview() {
    NotificationBlockerTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NookBadge()
            TodayMetrics(livedMinutes = 510, held = 12, daylightMinutes = 60)
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Today badge and metrics (dark)")
@Composable
fun TodayHeaderDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NookBadge()
            TodayMetrics(livedMinutes = 0, held = 0, daylightMinutes = 0)
        }
    }
}
