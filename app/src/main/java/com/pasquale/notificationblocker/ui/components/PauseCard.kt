package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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

/**
 * One pause in the Breaks list: a color mark, name, times and days, and its own switch. A tap anywhere
 * else opens the editor.
 */
@Composable
fun PauseCard(
    pause: Pause,
    color: Color,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isOnNow: Boolean = false,
) {
    val name = pauseName(pause)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .width(6.dp)
                    .height(56.dp)
                    .background(color, CircleShape),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = pauseTimes(pause),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (pause.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                DayPills(days = pause.days)
            }
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (isOnNow) {
                    Text(
                        text = stringResource(R.string.pause_state_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val toggleLabel = stringResource(R.string.pause_toggle, name)
                Switch(
                    checked = pause.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = toggleLabel },
                )
            }
        }
    }
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
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet())

@Preview(showBackground = true, name = "Pause card")
@Composable
fun PauseCardPreview() {
    NotificationBlockerTheme {
        PauseCard(lunch, MaterialTheme.colorScheme.tertiary, onClick = {}, onEnabledChange = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pause card - on now, whole day (dark)")
@Composable
fun PauseCardDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        PauseCard(
            Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            MaterialTheme.colorScheme.primary,
            onClick = {},
            onEnabledChange = {},
            modifier = Modifier.padding(16.dp),
            isOnNow = true,
        )
    }
}
