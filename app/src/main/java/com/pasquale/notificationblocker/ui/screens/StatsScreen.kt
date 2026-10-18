package com.pasquale.notificationblocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.DaySpans
import com.pasquale.notificationblocker.data.DayStats
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.PeriodStats
import com.pasquale.notificationblocker.data.SleepWindow
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.data.StatsRange
import com.pasquale.notificationblocker.data.SunTimes
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.CountUpText
import com.pasquale.notificationblocker.ui.components.DayChart
import com.pasquale.notificationblocker.ui.components.DaylightRing
import com.pasquale.notificationblocker.ui.components.entrance
import com.pasquale.notificationblocker.ui.components.LegendItem
import com.pasquale.notificationblocker.ui.components.MonthCalendar
import com.pasquale.notificationblocker.ui.components.TotalsRow
import com.pasquale.notificationblocker.ui.components.WeekChart
import com.pasquale.notificationblocker.ui.components.TimeRangeSheet
import com.pasquale.notificationblocker.ui.components.WeekCoverageCard
import com.pasquale.notificationblocker.data.WeekCoverage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.pasquale.notificationblocker.ui.components.durationText
import com.pasquale.notificationblocker.ui.components.pauseColor
import com.pasquale.notificationblocker.ui.components.pauseName
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.theme.sunlight
import java.time.LocalDate
import java.time.LocalDateTime

/** The "Your time" tab: day, week or month of break time lived, daylight and notifications kept out. */
@Composable
fun StatsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    // One-offs too: a quick break has minutes in the history like any pause
    val pauses by viewModel.allPauses.collectAsStateWithLifecycle()
    // Today's numbers keep up with the clock while the tab is open
    val minute by rememberCurrentMinutes()
    LaunchedEffect(minute) { viewModel.refreshOffHoursStatus() }
    val sleep by viewModel.sleepWindow.collectAsStateWithLifecycle()
    var editingSleep by rememberSaveable { mutableStateOf(false) }
    StatsContent(
        history = history,
        pauses = pauses,
        now = LocalDateTime.now(),
        modifier = modifier,
        sleep = sleep,
        onSleep = { editingSleep = true },
    )
    if (editingSleep) {
        TimeRangeSheet(
            title = stringResource(R.string.sleep_sheet_title),
            text = stringResource(R.string.sleep_sheet_text),
            startLabel = R.string.sleep_bedtime,
            endLabel = R.string.sleep_wake,
            initialStart = sleep.start,
            initialEnd = sleep.end,
            onSave = { start, end ->
                viewModel.setSleepWindow(SleepWindow(start, end))
                editingSleep = false
            },
            onDismiss = { editingSleep = false },
            // Two equal times leave no sleep out: say so, or the hours would jump up with no reason
            hint = { start, end -> if (start == end) stringResource(R.string.sleep_none_hint) else null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsContent(
    history: Map<LocalDate, DayStats>,
    pauses: List<Pause>,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
    sleep: SleepWindow? = null,
    onSleep: () -> Unit = {},
) {
    val today = now.toLocalDate()
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var anchor by rememberSaveable { mutableStateOf(today.toString()) }
    val anchorDate = LocalDate.parse(anchor)
    val period = remember(range, anchorDate, history) { PeriodStats.of(range, anchorDate, history) }
    val canGoForward = period.last.isBefore(today)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Connected button group of the Expressive system apps: the chosen range turns round
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val labels = listOf(R.string.stats_day, R.string.stats_week, R.string.stats_month)
            val last = StatsRange.entries.lastIndex
            StatsRange.entries.forEachIndexed { i, r ->
                ToggleButton(
                    checked = range == r,
                    onCheckedChange = { range = r },
                    // Read out as one choice of three ("selected"), not as a checkbox
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton; selected = range == r },
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        last -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(text = stringResource(labels[i]))
                }
            }
        }
        // A new user saw only zeros and empty charts: say when they start filling up
        if (history.values.none { it.livedMinutes > 0 }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { anchor = PeriodStats.shift(range, anchorDate, -1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.stats_prev))
            }
            Text(
                text = periodTitle(period),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            // No future periods: nothing to show there yet
            IconButton(
                onClick = { anchor = PeriodStats.shift(range, anchorDate, 1).toString() },
                enabled = canGoForward,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.stats_next))
            }
        }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.entrance(1),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.stats_total_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CountUpText(
                            target = period.livedMinutes,
                            style = MaterialTheme.typography.headlineLarge,
                            format = { durationText(it) },
                        )
                        Text(
                            text = stringResource(R.string.stats_daylight, durationText(period.daylightMinutes)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (range != StatsRange.DAY) DaylightRing(period.livedMinutes, period.daylightMinutes)
                }
                val colors = WorkSchedule.colorSlots(pauses).mapValues { pauseColor(it.value) }
                when (range) {
                    StatsRange.DAY -> {
                        val day = period.days.first()
                        val (sunrise, sunset) = remember(day.date) { SunTimes.of(day.date) }
                        val spans = remember(day, pauses, now, sleep) { DaySpans.of(day, pauses, now, sleep) }
                        DayChart(
                            spans = spans,
                            sunrise = sunrise,
                            sunset = sunset,
                            nowMinute = if (day.date == today) now.hour * 60 + now.minute else null,
                            colorOf = { colors[it] ?: Color.Unspecified },
                        )
                        // The times in words under the bar, one entry per pause, then the daylight line
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            spans.groupBy { it.pause.id }.values.forEach { same ->
                                val times = same.joinToString(", ") { OffHours.format(it.startMinute) + "–" + OffHours.format(it.endMinute) }
                                LegendItem(colors[same.first().pause.id] ?: MaterialTheme.colorScheme.secondary, pauseName(same.first().pause) + " " + times)
                            }
                            LegendItem(MaterialTheme.colorScheme.sunlight, stringResource(R.string.stats_day_light, OffHours.format(sunrise), OffHours.format(sunset)))
                        }
                    }
                    StatsRange.WEEK -> {
                        WeekChart(days = period.days, today = today)
                        Row {
                            LegendItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.stats_total_label))
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendItem(MaterialTheme.colorScheme.sunlight, stringResource(R.string.stats_legend_daylight))
                        }
                    }
                    StatsRange.MONTH -> MonthCalendar(days = period.days, today = today)
                }
            }
        }
        // Average over the days of the period already lived since the history began, so a week in
        // progress or the month Nook was installed in is not diluted
        val since = history.keys.minOrNull() ?: today
        val elapsed = period.days.count { !it.date.isAfter(today) && !it.date.isBefore(since) }
        TotalsRow(
            livedMinutes = period.livedMinutes,
            daylightMinutes = period.daylightMinutes,
            held = period.held,
            averageMinutes = if (range != StatsRange.DAY && elapsed > 0) period.livedMinutes / elapsed else null,
            modifier = Modifier.entrance(2),
        )
        if (range == StatsRange.MONTH) {
            Text(
                text = stringResource(R.string.stats_month_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        // The usual week the pauses make (one-offs left out by WeekCoverage), and the sleep that the
        // numbers above leave out: both explain the totals, so they live here and not in Breaks
        SectionHeader(R.string.stats_section_week)
        WeekCoverageCard(
            coverage = remember(pauses, sleep) { WeekCoverage.of(pauses, sleep) },
            modifier = Modifier.entrance(3),
        )
        if (sleep != null) {
            LinkRow(
                icon = ImageVector.vectorResource(R.drawable.ic_moon),
                title = stringResource(R.string.sleep_row_title),
                subtitle = if (sleep.start == sleep.end) stringResource(R.string.sleep_row_none)
                else stringResource(R.string.sleep_row, OffHours.format(sleep.start), OffHours.format(sleep.end)),
                onClick = onSleep,
                position = 0,
                count = 1,
                shapeIndex = 1,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** "Thursday 15 October", "12 - 18 October", "October 2026", in the phone's language. */
@Composable
private fun periodTitle(period: PeriodStats): String {
    val locale = LocalConfiguration.current.locales[0]
    fun fmt(pattern: String, date: LocalDate) =
        OffHours.date(pattern, date, locale).replaceFirstChar { it.titlecase(locale) }
    return when (period.range) {
        StatsRange.DAY -> fmt("EEEE d MMMM", period.first)
        StatsRange.WEEK -> stringResource(
            R.string.stats_range,
            fmt(if (period.first.month == period.last.month) "d" else "d MMM", period.first),
            fmt("d MMMM", period.last),
        )
        StatsRange.MONTH -> fmt("LLLL yyyy", period.first)
    }
}

private fun sampleHistory(today: LocalDate): Map<LocalDate, DayStats> =
    (0L..20L).associate { back ->
        val date = today.minusDays(back)
        val weekend = date.dayOfWeek.value >= 6
        val lived = if (back == 1L) 0 else if (weekend) 600 else 150
        date to DayStats(date, lived, lived / 3, held = lived / 30, byPause = mapOf("evening" to lived))
    }

@Preview(showBackground = true, name = "Your time - week")
@Composable
fun StatsWeekPreview() {
    val now = LocalDateTime.of(2026, 10, 15, 18, 52)
    NotificationBlockerTheme {
        StatsContent(sampleHistory(now.toLocalDate()), listOf(Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)), now, sleep = SleepWindow.DEFAULT)
    }
}
