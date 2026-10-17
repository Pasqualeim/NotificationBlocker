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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.pasquale.notificationblocker.data.CoverageSegment
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.WeekCoverage
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek

/**
 * Top of the Breaks tab (and step 2 of the first launch): the hours a week without work
 * notifications, then one 24 h bar per group of days that look the same ("Lun–ven", "Sab–dom"),
 * every pause in its color and the free times written under it, and a two-item legend. The
 * stretches grow in from the left when the card first shows.
 */
@Composable
fun WeekCoverageCard(coverage: WeekCoverage, modifier: Modifier = Modifier) {
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
            Spacer(modifier = Modifier.height(20.dp))
            // One bar per group of days that look the same, with its times written under it: seven
            // near-identical rows and an hour axis left people guessing what the bars meant
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (group in coverage.groups) {
                    DayGroupRow(group = group, colors = colors, track = track, grow = grow.value)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // The colors are the pauses, named by the dots under each bar: only the empty part needs a word
            Text(
                text = stringResource(R.string.week_legend_active),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One group of days: their name, the 24 h bar with its free stretches in their pause's color, and the times in words. */
@Composable
private fun DayGroupRow(group: WeekCoverage.DayGroup, colors: List<Color>, track: Color, grow: Float) {
    val day = OffHours.MINUTES_PER_DAY.toFloat()
    val label = daysSummary(group.days.toSet())
    val times = group.stretches.map { stretchText(it) }
    val spoken = label + ": " + times.ifEmpty { listOf(stringResource(R.string.week_no_breaks)) }.joinToString()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            val pill = RoundRect(Rect(Offset.Zero, size), CornerRadius(size.height / 2))
            drawPath(Path().apply { addRoundRect(pill) }, track)
            // Square-ish stretches clipped to the pill: with round ends a one-hour lunch read as a dot
            val r = CornerRadius(2.dp.toPx())
            clipPath(Path().apply { addRoundRect(pill) }) {
                // The merged stretches, one color each: overlapping pauses drawn one over the other (the
                // evening over a whole weekend day) read as two different things
                for (s in group.stretches) {
                    val full = maxOf(size.width * (s.end - s.start) / day, 4.dp.toPx())
                    val x = minOf(size.width * s.start / day, size.width - full)
                    val w = full * grow
                    if (w > 0f) drawRoundRect(colors[s.pauseIndex % colors.size], Offset(x, 0f), Size(w, size.height), r)
                }
            }
        }
        if (group.stretches.isEmpty()) {
            Text(
                text = stringResource(R.string.week_no_breaks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                group.stretches.forEachIndexed { i, s ->
                    LegendItem(color = colors[s.pauseIndex % colors.size], label = times[i])
                }
            }
        }
    }
}

/** "fino alle 08:30", "13:00–14:00", "dalle 18:30" or "Tutto il giorno". */
@Composable
private fun stretchText(s: CoverageSegment): String {
    val day = OffHours.MINUTES_PER_DAY
    return when {
        s.start == 0 && s.end >= day -> stringResource(R.string.pause_all_day)
        s.start == 0 -> stringResource(R.string.week_stretch_until, OffHours.format(s.end))
        s.end >= day -> stringResource(R.string.week_stretch_from, OffHours.format(s.start))
        else -> OffHours.format(s.start) + "–" + OffHours.format(s.end)
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
        WeekCoverageCard(WeekCoverage.of(previewPauses), Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Week coverage (dark)")
@Composable
fun WeekCoverageCardDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        WeekCoverageCard(WeekCoverage.of(previewPauses.take(2)), Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "Week coverage - no breaks")
@Composable
fun WeekCoverageCardEmptyPreview() {
    NotificationBlockerTheme {
        WeekCoverageCard(WeekCoverage.of(emptyList()), Modifier.padding(16.dp))
    }
}
