package com.pasquale.notificationblocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.DaysOff
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.holidayName
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Days off: the public holidays switch, the ranges of days off, and "Add days" with a range picker. */
@Composable
fun DaysOffScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val ranges by viewModel.daysOff.collectAsStateWithLifecycle()
    val holidays by viewModel.holidaysEnabled.collectAsStateWithLifecycle()
    DaysOffContent(
        ranges = ranges,
        holidaysEnabled = holidays,
        today = LocalDate.now(),
        onHolidaysChange = viewModel::setHolidaysEnabled,
        onAdd = viewModel::addDaysOff,
        onRemove = viewModel::removeDaysOff,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysOffContent(
    ranges: List<DaysOff.Range>,
    holidaysEnabled: Boolean,
    today: LocalDate,
    onHolidaysChange: (Boolean) -> Unit,
    onAdd: (LocalDate, LocalDate) -> Unit,
    onRemove: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.days_off_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picking = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.days_off_add)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.days_off_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    val next = DaysOff.nextHoliday(today)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.days_off_holidays)) },
                        supportingContent = {
                            // Always says which country's holidays these are
                            Text(
                                listOfNotNull(
                                    stringResource(R.string.days_off_holidays_text),
                                    next?.let { stringResource(R.string.days_off_holidays_next, holidayName(it.second), dayText(it.first)) },
                                ).joinToString("\n"),
                            )
                        },
                        trailingContent = {
                            Switch(checked = holidaysEnabled, onCheckedChange = onHolidaysChange)
                        },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.days_off_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            item {
                if (ranges.isEmpty()) {
                    Text(
                        text = stringResource(R.string.days_off_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            ranges.forEachIndexed { i, range ->
                                if (i > 0) HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                RangeRow(range = range, onRemove = { onRemove(range.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        RangePickerDialog(
            today = today,
            onConfirm = { first, last ->
                onAdd(first, last)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun RangeRow(range: DaysOff.Range, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.days_off_remove)
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
        },
        headlineContent = { Text(rangeText(range.first, range.last)) },
        supportingContent = { Text(pluralStringResource(R.plurals.days_off_days, range.days, range.days)) },
        trailingContent = {
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = removeLabel) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(today: LocalDate, onConfirm: (LocalDate, LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDateRangePickerState(
        // Days off are ahead: past days cannot be picked
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        },
    )
    fun date(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis ?: start
    // One entry holds at most DaysOff.MAX_DAYS days: longer ranges are refused, not cut short
    val tooLong = start != null && end != null && DaysOff.Range("", date(start), date(end)).days > DaysOff.MAX_DAYS
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (tooLong) {
                Text(
                    text = pluralStringResource(R.plurals.days_off_too_long, DaysOff.MAX_DAYS, DaysOff.MAX_DAYS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            TextButton(onClick = { if (start != null && end != null) onConfirm(date(start), date(end)) }, enabled = start != null && !tooLong) {
                Text(stringResource(R.string.pause_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DateRangePicker(
            state = state,
            title = { Text(stringResource(R.string.days_off_pick), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            showModeToggle = false,
            modifier = Modifier.height(520.dp),
        )
    }
}

/** "2–6 November", or "30 Dec–3 Jan" across months. */
@Composable
private fun rangeText(first: LocalDate, last: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    if (first == last) return dayText(first)
    val sameMonth = first.month == last.month && first.year == last.year
    val a = DateTimeFormatter.ofPattern(if (sameMonth) "d" else "d MMM", locale).format(first)
    return stringResource(R.string.stats_range, a, DateTimeFormatter.ofPattern(if (sameMonth) "d MMMM" else "d MMM", locale).format(last))
}

@Composable
private fun dayText(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMMM", LocalConfiguration.current.locales[0]).format(date)

@Preview(showBackground = true, name = "Days off")
@Composable
fun DaysOffPreview() {
    val today = LocalDate.of(2026, 10, 16)
    NotificationBlockerTheme {
        DaysOffContent(
            ranges = listOf(DaysOff.Range("off1", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))),
            holidaysEnabled = true,
            today = today,
            onHolidaysChange = {},
            onAdd = { _, _ -> },
            onRemove = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Days off - empty")
@Composable
fun DaysOffEmptyPreview() {
    NotificationBlockerTheme {
        DaysOffContent(emptyList(), false, LocalDate.of(2026, 10, 16), {}, { _, _ -> }, {}, {})
    }
}
