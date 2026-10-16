package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.pasquale.notificationblocker.ui.theme.Motion
import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.DaySpan
import com.pasquale.notificationblocker.data.DayStats
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.theme.sunlight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Seven columns, one per day: the break time lived in sage, its daylight part in warm yellow at the bottom.
 * A day with no break is a neutral dot, never a red or empty slot. The scale grows past 12 h only if needed.
 */
@Composable
fun WeekChart(days: List<DayStats>, today: LocalDate, modifier: Modifier = Modifier) {
    val maxHours = maxOf(12, (days.maxOfOrNull { it.livedMinutes } ?: 0) / 60 + 1)
    val sage = MaterialTheme.colorScheme.secondary
    val sun = MaterialTheme.colorScheme.sunlight
    val dot = MaterialTheme.colorScheme.outlineVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val locale = LocalConfiguration.current.locales[0]
    // The bars grow up from the base line each time another week shows
    val grow = remember(days.firstOrNull()?.date) { Animatable(0f) }
    LaunchedEffect(grow) { grow.animateTo(1f, Motion.standard(Motion.SLOW)) }
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (day in days) {
                    val label = day.date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale) + ": " + durationText(day.livedMinutes)
                    Canvas(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight()
                            .semantics { contentDescription = label },
                    ) {
                        val radius = CornerRadius(7.dp.toPx())
                        // Days to come: nothing yet. A past day with no break: a neutral dot
                        if (day.date.isAfter(today)) return@Canvas
                        if (day.livedMinutes == 0) {
                            drawCircle(dot, 4.dp.toPx(), Offset(size.width / 2, size.height - 4.dp.toPx()))
                            return@Canvas
                        }
                        val h = size.height * day.livedMinutes / (maxHours * 60f) * grow.value
                        val s = size.height * day.daylightMinutes / (maxHours * 60f) * grow.value
                        drawRoundRect(sage, Offset(0f, size.height - h), Size(size.width, h), radius)
                        if (s > 0f) drawRoundRect(sun, Offset(0f, size.height - s), Size(size.width, s), radius)
                    }
                }
            }
            // Hour marks on the right
            Column(
                modifier = Modifier.fillMaxHeight().padding(start = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                for (h in listOf(maxHours, maxHours * 2 / 3, maxHours / 3, 0)) {
                    Text(text = "$h h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(grid, Offset.Zero, Offset(size.width, 0f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 30.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (day in days) {
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The month as a calendar, Monday first: the fuller the circle, the more break time that day. Days to
 * come are only outlined; today has a ring.
 */
@Composable
fun MonthCalendar(days: List<DayStats>, today: LocalDate, modifier: Modifier = Modifier) {
    val first = days.first().date
    val locale = LocalConfiguration.current.locales[0]
    val offset = first.dayOfWeek.ordinal
    val cells: List<DayStats?> = List(offset) { null } + days
    val sage = MaterialTheme.colorScheme.secondary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (d in DayOfWeek.entries) {
                Text(
                    text = d.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        for (week in cells.chunked(7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val future = day.date.isAfter(today)
                            val fill = (0.25f + 0.75f * (day.livedMinutes / (11f * 60)).coerceAtMost(1f)).takeIf { day.livedMinutes > 0 }
                            val label = durationText(day.livedMinutes)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .then(if (day.date == today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (fill != null) {
                                            Modifier.background(sage.copy(alpha = fill))
                                        } else {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        },
                                    )
                                    .semantics { contentDescription = "${day.date.dayOfMonth}: $label" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (fill != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        fill != null && fill > 0.55f -> MaterialTheme.colorScheme.onSecondary
                                        fill != null -> MaterialTheme.colorScheme.onSecondaryContainer
                                        future -> MaterialTheme.colorScheme.outlineVariant
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day from 00 at the top to 24 at the bottom: daylight as a warm band, the breaks lived as colored
 * stretches, a dashed line at the current time (today only).
 *
 * @param colorOf color of a pause, by id
 */
@Composable
fun DayChart(
    spans: List<DaySpan>,
    sunrise: Int,
    sunset: Int,
    nowMinute: Int?,
    colorOf: (String) -> Color,
    modifier: Modifier = Modifier,
) {
    val band = MaterialTheme.colorScheme.sunlight.copy(alpha = 0.25f)
    val nowColor = MaterialTheme.colorScheme.onSurface
    val day = OffHours.MINUTES_PER_DAY.toFloat()
    Row(modifier = modifier.height(200.dp)) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            for (h in listOf(0, 6, 12, 18, 24)) {
                Text(text = OffHours.format(h * 60).substring(0, 2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = CornerRadius(8.dp.toPx())
                drawRoundRect(band, Offset(0f, size.height * sunrise / day), Size(size.width, size.height * (sunset - sunrise) / day), r)
                for (span in spans) {
                    val top = size.height * span.startMinute / day
                    val h = maxOf(4.dp.toPx(), size.height * (span.endMinute - span.startMinute) / day)
                    drawRoundRect(colorOf(span.pause.id), Offset(8.dp.toPx(), top), Size(size.width * 0.42f, h), CornerRadius(6.dp.toPx()))
                }
                nowMinute?.let {
                    val y = size.height * it / day
                    drawLine(nowColor, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(R.string.stats_sunrise, OffHours.format(sunrise)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.stats_sunset, OffHours.format(sunset)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A small colored square and its label, for the chart legends. */
@Composable
fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The totals under the chart, as a segmented list: free time, daylight, notifications paused, average a day. */
@Composable
fun TotalsRow(livedMinutes: Int, daylightMinutes: Int, held: Int, modifier: Modifier = Modifier, averageMinutes: Int? = null) {
    val count = if (averageMinutes != null) 4 else 3
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        TotalLine(0, count, painterResource(R.drawable.ic_mug), stringResource(R.string.stats_tile_yours), durationText(livedMinutes))
        TotalLine(1, count, painterResource(R.drawable.ic_tile_sun), stringResource(R.string.stats_tile_daylight), durationText(daylightMinutes), tint = MaterialTheme.colorScheme.sunlight)
        TotalLine(2, count, rememberVectorPainter(Icons.Filled.Notifications), stringResource(R.string.stats_tile_held), held.toString())
        if (averageMinutes != null) {
            TotalLine(3, count, painterResource(R.drawable.ic_nav_time), stringResource(R.string.stats_average), durationText(averageMinutes))
        }
    }
}

/** One total: its icon in an expressive shape, the label, the figure at the end. [tint] null: the container's own content color. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TotalLine(
    position: Int,
    count: Int,
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    value: String,
    tint: Color? = null,
) {
    val (container, content) = pauseContainerColors(position)
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = position, count = count),
        colors = segmentColors(),
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).background(container, expressiveShape(position)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(painter = icon, contentDescription = null, tint = tint ?: content, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = { Text(text = value, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
    ) {
        Text(text = label)
    }
}

/**
 * Ring of the daylight share: the whole ring is the free time of the period, the warm arc the part
 * of it in daylight, with the percentage inside. The arc sweeps in when a new period shows.
 */
@Composable
fun DaylightRing(livedMinutes: Int, daylightMinutes: Int, modifier: Modifier = Modifier) {
    val share = if (livedMinutes > 0) daylightMinutes.toFloat() / livedMinutes else 0f
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(share) { sweep.animateTo(share, Motion.standard(Motion.SLOW)) }
    val track = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
    val sun = MaterialTheme.colorScheme.sunlight
    Box(modifier = modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            if (sweep.value > 0f) {
                drawArc(
                    sun, -90f, 360f * sweep.value, false, Offset(inset, inset), arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(sweep.value * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                text = stringResource(R.string.stats_ring_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private val previewToday = LocalDate.of(2026, 10, 15)

private fun previewDays(first: LocalDate, count: Int) = (0 until count).map { i ->
    val date = first.plusDays(i.toLong())
    val lived = when {
        date.isAfter(previewToday) || i == 2 -> 0
        date.dayOfWeek.value >= 6 -> 600
        else -> 150 + i * 20
    }
    DayStats(date, lived, lived / 3)
}

@Preview(showBackground = true, name = "Week chart")
@Composable
fun WeekChartPreview() {
    NotificationBlockerTheme {
        WeekChart(previewDays(LocalDate.of(2026, 10, 12), 7), previewToday, Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Month calendar (dark)")
@Composable
fun MonthCalendarPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        MonthCalendar(previewDays(LocalDate.of(2026, 10, 1), 31), previewToday, Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "Day chart, legend and totals")
@Composable
fun DayChartPreview() {
    val evening = com.pasquale.notificationblocker.data.Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)
    NotificationBlockerTheme {
        val blue = MaterialTheme.colorScheme.tertiary
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DayChart(
                spans = listOf(DaySpan(evening, 0, 510), DaySpan(evening, 1110, 1140)),
                sunrise = 7 * 60 + 22,
                sunset = 18 * 60 + 29,
                nowMinute = 19 * 60,
                colorOf = { blue },
            )
            LegendItem(MaterialTheme.colorScheme.tertiary, "Sera")
            TotalsRow(livedMinutes = 130, daylightMinutes = 40, held = 5)
        }
    }
}
