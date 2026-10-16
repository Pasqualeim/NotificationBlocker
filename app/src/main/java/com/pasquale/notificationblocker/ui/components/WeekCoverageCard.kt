package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.WeekCoverage
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Top of the Breaks tab: the hours a week without work notifications, and the week drawn as seven
 * 24 h tracks with every pause in its color. The stretches grow in from the left when the card
 * first shows; today's row is marked.
 */
@Composable
fun WeekCoverageCard(coverage: WeekCoverage, today: DayOfWeek, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    // Saved, so the card does not grow again when it scrolls back into the list
    var played by rememberSaveable { mutableStateOf(false) }
    val grow = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            grow.animateTo(1f, Motion.standard(Motion.SLOW))
            played = true
        }
    }
    val colors = (0 until 3).map { pauseColor(it) }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val day = OffHours.MINUTES_PER_DAY.toFloat()

    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.week_coverage_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CountUpText(
                target = coverage.weeklyMinutes / 60,
                style = MaterialTheme.typography.displaySmall,
                format = { stringResource(R.string.life_duration_hours, it) },
            )
            Text(
                text = stringResource(R.string.week_coverage_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            for (d in DayOfWeek.entries) {
                val isToday = d == today
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = d.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val segments = coverage.days.getValue(d)
                    val todayRing = MaterialTheme.colorScheme.outline
                    val dayName = d.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    val covered = segments.joinToString { OffHours.format(it.start) + "–" + OffHours.format(it.end % OffHours.MINUTES_PER_DAY) }
                    Canvas(modifier = Modifier.weight(1f).height(12.dp).semantics { contentDescription = "$dayName: $covered" }) {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(track, Offset.Zero, size, r)
                        for (s in segments) {
                            // A short pause still shows as a dot, kept inside the track
                            val full = maxOf(size.width * (s.end - s.start) / day, size.height)
                            val x = minOf(size.width * s.start / day, size.width - full)
                            val w = full * grow.value
                            if (w > 0f) drawRoundRect(colors[s.pauseIndex % 3], Offset(x, 0f), Size(w, size.height), r)
                        }
                        if (isToday) {
                            drawRoundRect(todayRing, Offset.Zero, size, r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (h in listOf(0, 6, 12, 18, 24)) {
                    Text(
                        text = OffHours.format(h * 60).substring(0, 2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val previewPauses = listOf(
    Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet()),
    Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30),
    Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
)

@Preview(showBackground = true, name = "Week coverage")
@Composable
fun WeekCoverageCardPreview() {
    NotificationBlockerTheme {
        WeekCoverageCard(WeekCoverage.of(previewPauses), LocalDate.of(2026, 10, 16).dayOfWeek, Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Week coverage (dark)")
@Composable
fun WeekCoverageCardDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        WeekCoverageCard(WeekCoverage.of(previewPauses), DayOfWeek.MONDAY, Modifier.padding(16.dp))
    }
}
