package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek

/**
 * "When you work", on the Breaks tab and step 2 of the first launch: the work days as pills ticked
 * right here, the hours as a row that opens the time sheet, and one line on what that leaves free
 * ("Sab–dom free all day"). Everything outside it is paused ([WorkSchedule.offWorkPauses]). Not
 * [active]: the schedule is a suggestion not in use yet, and the line says that a tap turns it on.
 */
@Composable
fun WorkScheduleCard(
    schedule: WorkSchedule,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onHoursClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.work_days_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            DayPills(
                days = schedule.days,
                size = 40,
                onToggle = { day -> onDaysChange(if (day in schedule.days) schedule.days - day else schedule.days + day) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Surface(onClick = onHoursClick, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.work_hours_label), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = OffHours.format(schedule.start) + "–" + OffHours.format(schedule.end),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            Text(
                text = when {
                    // Nook updated from a version with pauses only: the usual schedule is shown, not yet in use
                    !active -> stringResource(R.string.work_not_set)
                    schedule.days.isEmpty() -> stringResource(R.string.work_free_all)
                    schedule.freeDays.isEmpty() -> stringResource(R.string.work_free_none)
                    else -> stringResource(R.string.work_free_days, daysSummary(schedule.freeDays))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

/** The work hours sheet: start and end of the working day, the end after the start (a day shift). */
@Composable
fun WorkHoursSheet(schedule: WorkSchedule, onSave: (WorkSchedule) -> Unit, onDismiss: () -> Unit) {
    TimeRangeSheet(
        title = stringResource(R.string.work_hours_title),
        text = stringResource(R.string.work_hours_text),
        startLabel = R.string.work_hours_start,
        endLabel = R.string.work_hours_end,
        initialStart = schedule.start,
        initialEnd = schedule.end,
        onSave = { start, end -> onSave(schedule.copy(start = start, end = end)) },
        onDismiss = onDismiss,
        hint = { start, end -> if (end <= start) stringResource(R.string.work_hours_order) else null },
        canSave = { start, end -> start < end },
    )
}

@Preview(showBackground = true, name = "Work schedule")
@Composable
fun WorkScheduleCardPreview() {
    NotificationBlockerTheme {
        WorkScheduleCard(WorkSchedule.DEFAULT, onDaysChange = {}, onHoursClick = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "Work schedule, every day")
@Composable
fun WorkScheduleCardEveryDayPreview() {
    NotificationBlockerTheme {
        WorkScheduleCard(WorkSchedule.DEFAULT.copy(days = DayOfWeek.entries.toSet()), onDaysChange = {}, onHoursClick = {}, modifier = Modifier.padding(16.dp))
    }
}
