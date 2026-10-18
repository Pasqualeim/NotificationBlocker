package com.pasquale.notificationblocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedListItem
import com.pasquale.notificationblocker.ui.components.expressiveShape
import com.pasquale.notificationblocker.ui.components.segmentColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.PauseCodec
import com.pasquale.notificationblocker.data.PauseSchedule
import com.pasquale.notificationblocker.ui.components.WorkHoursSheet
import com.pasquale.notificationblocker.ui.components.WorkScheduleCard
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.PauseRow
import com.pasquale.notificationblocker.ui.components.entrance
import com.pasquale.notificationblocker.ui.components.PauseEditorSheet
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek
import java.time.LocalDateTime

/** The Breaks tab: when you work, the work apps, the other pauses with the days off; "Add a break" floats bottom right. */
@Composable
fun PausesScreen(
    viewModel: MainViewModel,
    onEditWorkApps: () -> Unit,
    onEditPauseApps: (pauseId: String) -> Unit,
    modifier: Modifier = Modifier,
    onDaysOff: () -> Unit = {},
) {
    val pauses by viewModel.pauses.collectAsStateWithLifecycle()
    val ended by viewModel.pauseEndedKeys.collectAsStateWithLifecycle()
    val workApps by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
    val daysOff by viewModel.daysOff.collectAsStateWithLifecycle()
    val holidays by viewModel.holidaysEnabled.collectAsStateWithLifecycle()
    val work by viewModel.workSchedule.collectAsStateWithLifecycle()
    var editingHours by rememberSaveable { mutableStateOf(false) }
    // The "on now" label follows the clock
    val minute by rememberCurrentMinutes()
    val onNow = remember(pauses, ended, minute) {
        PauseSchedule.activeAt(pauses, LocalDateTime.now(), ended).mapTo(HashSet()) { it.pause.id }
    }

    // The pause in the editor, kept as text so it survives a rotation; "new" until it is saved once
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editingIsNew by rememberSaveable { mutableStateOf(false) }
    val editingPause = editing?.let { PauseCodec.decode(it).firstOrNull() }

    PausesContent(
        pauses = pauses,
        onNow = onNow,
        workAppsCount = workApps,
        onPauseClick = { pause ->
            editing = PauseCodec.encode(listOf(pause))
            editingIsNew = false
        },
        onPauseEnabledChange = viewModel::setPauseEnabled,
        onAddPause = {
            editing = PauseCodec.encode(listOf(Pause(id = viewModel.newPauseId(), name = "", start = 13 * 60, end = 14 * 60)))
            editingIsNew = true
        },
        onEditWorkApps = onEditWorkApps,
        onDaysOff = onDaysOff,
        daysOffRanges = daysOff.size,
        holidaysOn = holidays,
        work = work,
        onWorkChange = viewModel::setWorkSchedule,
        onWorkHours = { editingHours = true },
        modifier = modifier,
    )

    if (editingHours) {
        WorkHoursSheet(
            schedule = work ?: WorkSchedule.DEFAULT,
            onSave = {
                viewModel.setWorkSchedule(it)
                editingHours = false
            },
            onDismiss = { editingHours = false },
        )
    }

    if (editingPause != null) {
        // The stored pause, if any: the app choice is written straight away by the app list
        val stored = pauses.firstOrNull { it.id == editingPause.id }
        PauseEditorSheet(
            pause = if (stored != null) editingPause.copy(apps = stored.apps) else editingPause,
            isNew = editingIsNew,
            onSave = { pause ->
                viewModel.savePause(pause)
                editing = null
            },
            onDelete = {
                viewModel.deletePause(editingPause.id)
                editing = null
            },
            onEditApps = { pause ->
                // The app list writes this pause: save it first, then reopen the sheet as an existing pause
                viewModel.savePause(pause)
                editing = PauseCodec.encode(listOf(pause))
                editingIsNew = false
                onEditPauseApps(pause.id)
            },
            onResetApps = { viewModel.resetPauseApps(editingPause.id) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun PausesContent(
    pauses: List<Pause>,
    onNow: Set<String>,
    workAppsCount: Int,
    onPauseClick: (Pause) -> Unit,
    onPauseEnabledChange: (String, Boolean) -> Unit,
    onAddPause: () -> Unit,
    onEditWorkApps: () -> Unit,
    modifier: Modifier = Modifier,
    onDaysOff: () -> Unit = {},
    daysOffRanges: Int = 0,
    holidaysOn: Boolean = false,
    work: WorkSchedule? = WorkSchedule.DEFAULT,
    onWorkChange: (WorkSchedule) -> Unit = {},
    onWorkHours: () -> Unit = {},
) {
    // Controls only, the most important first: when (work), what (apps), then the extra pauses.
    // The week chart and sleep live in Stats: they explain the numbers, they decide nothing here
    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room under the last row for the "Add a break" button
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "work") {
            SectionHeader(R.string.work_section)
            // Without a schedule yet (Nook updated from a version with pauses only) the usual one is
            // shown, and saved at the first change
            val shown = work ?: WorkSchedule.DEFAULT
            WorkScheduleCard(
                schedule = shown,
                onDaysChange = { onWorkChange(shown.copy(days = it)) },
                onHoursClick = onWorkHours,
                active = work != null,
                modifier = Modifier.entrance(0),
            )
        }
        item(key = "apps") {
            SectionHeader(R.string.pauses_section_apps)
            LinkRow(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.select_apps),
                subtitle = pluralStringResource(R.plurals.pauses_work_apps, workAppsCount, workAppsCount),
                onClick = onEditWorkApps,
                position = 0,
                count = 1,
                shapeIndex = 3,
                modifier = Modifier.entrance(1),
            )
        }
        // A segmented group, like the system settings: one segment per pause, then the days off
        item(key = "pauses") {
            SectionHeader(R.string.pauses_section_list)
            val count = pauses.size + 1
            Column(
                modifier = Modifier.entrance(2),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                pauses.forEachIndexed { index, pause ->
                    PauseRow(
                        pause = pause,
                        // Slot 0 is the time outside work: the other pauses start at 1, as on Today
                        index = index + 1,
                        onClick = { onPauseClick(pause) },
                        onEnabledChange = { onPauseEnabledChange(pause.id, it) },
                        isOnNow = pause.id in onNow,
                        position = index,
                        count = count,
                    )
                }
                LinkRow(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.days_off_title),
                    subtitle = listOfNotNull(
                        if (daysOffRanges > 0) pluralStringResource(R.plurals.days_off_row, daysOffRanges, daysOffRanges) else null,
                        if (holidaysOn) stringResource(R.string.days_off_row_holidays) else null,
                    ).joinToString("  ·  ").ifEmpty { stringResource(R.string.days_off_row_empty) },
                    onClick = onDaysOff,
                    position = count - 1,
                    count = count,
                    shapeIndex = 2,
                )
            }
        }
    }
    ExtendedFloatingActionButton(
        onClick = onAddPause,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(text = stringResource(R.string.pause_add)) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
    )
    }
}

/**
 * A segment that opens a screen or a sheet: icon in an expressive shape, title, one line, chevron.
 * Also used by Stats for the sleep row.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: Int,
    count: Int,
    shapeIndex: Int,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = position, count = count),
        colors = segmentColors(),
        modifier = modifier,
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, expressiveShape(shapeIndex)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
        },
        supportingContent = { Text(text = subtitle) },
        trailingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    ) {
        Text(text = title)
    }
}

/** The title above a group of segments, like the section titles of the system settings. */
@Composable
internal fun SectionHeader(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Preview(showBackground = true, name = "Breaks")
@Composable
fun PausesPreview() {
    NotificationBlockerTheme {
        PausesContent(
            pauses = listOf(
                Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet()),
                Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30),
                Pause("weekend", "Weekend", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            ),
            onNow = setOf("evening"),
            workAppsCount = 6,
            onPauseClick = {},
            onPauseEnabledChange = { _, _ -> },
            onAddPause = {},
            onEditWorkApps = {},
        )
    }
}
