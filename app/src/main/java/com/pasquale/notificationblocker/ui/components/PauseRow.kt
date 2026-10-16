package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Color of a pause in lists and charts, by its place in the list: sage, dusty blue, terracotta. */
@Composable
fun pauseColor(index: Int): Color = when (index % 3) {
    0 -> MaterialTheme.colorScheme.secondary
    1 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

/** The pause's name, or "Break" for one without (the pause carried over from Nook 1). */
@Composable
fun pauseName(pause: Pause): String = pause.name.ifBlank { stringResource(R.string.pause_default_name) }

/** "13:00 - 14:00", or "All day". */
@Composable
fun pauseTimes(pause: Pause): String =
    if (pause.isWholeDay) {
        stringResource(R.string.pause_all_day)
    } else {
        stringResource(R.string.pause_times, OffHours.format(pause.start), OffHours.format(pause.end))
    }

/** "Every day", "Weekdays", "Weekends", or the short names of the days ("Mon, Wed, Fri"). */
@Composable
fun daysSummary(days: Set<DayOfWeek>): String {
    val locale = LocalConfiguration.current.locales[0]
    val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return when {
        days.size == 7 -> stringResource(R.string.pause_days_every)
        days == DayOfWeek.entries.toSet() - weekend -> stringResource(R.string.pause_days_weekdays)
        days == weekend -> stringResource(R.string.pause_days_weekend)
        else -> DayOfWeek.entries.filter { it in days }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) } }
    }
}

/**
 * One pause in the Breaks list, as a list row: its avatar (time-of-day icon in the pause's color), name,
 * times and days, and its own switch. A tap anywhere else opens the editor.
 */
@Composable
fun PauseRow(
    pause: Pause,
    index: Int,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isOnNow: Boolean = false,
) {
    val name = pauseName(pause)
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { PauseAvatar(pause = pause, index = index, enabled = pause.enabled) },
        headlineContent = { Text(text = name) },
        supportingContent = {
            Text(text = listOfNotNull(pauseTimes(pause), daysSummary(pause.days)).joinToString("  ·  "))
        },
        overlineContent = if (isOnNow) {
            { Text(text = stringResource(R.string.pause_state_now), color = MaterialTheme.colorScheme.secondary) }
        } else {
            null
        },
        trailingContent = {
            val toggleLabel = stringResource(R.string.pause_toggle, name)
            Switch(
                checked = pause.enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics { contentDescription = toggleLabel },
            )
        },
    )
}

/** The seven days, Monday first; the chosen ones filled. Tappable when [onToggle] is given (the editor). */
@Composable
fun DayPills(
    days: Set<DayOfWeek>,
    modifier: Modifier = Modifier,
    onToggle: ((DayOfWeek) -> Unit)? = null,
    size: Int = 24,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(if (onToggle != null) 8.dp else 4.dp)) {
        for (day in DayOfWeek.entries) {
            val on = day in days
            val label = day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
            val fullName = day.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .then(
                        if (on) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        },
                    )
                    .then(
                        if (onToggle != null) {
                            Modifier.toggleable(value = on, role = Role.Checkbox) { onToggle(day) }
                        } else {
                            Modifier
                        },
                    )
                    .semantics { contentDescription = fullName },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (size > 30) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                    color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet())

@Preview(showBackground = true, name = "Pause row")
@Composable
fun PauseRowPreview() {
    NotificationBlockerTheme {
        PauseRow(lunch, 0, onClick = {}, onEnabledChange = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pause row - on now, whole day (dark)")
@Composable
fun PauseRowDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        PauseRow(
            Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            2,
            onClick = {},
            onEnabledChange = {},
            modifier = Modifier.padding(16.dp),
            isOnNow = true,
        )
    }
}
