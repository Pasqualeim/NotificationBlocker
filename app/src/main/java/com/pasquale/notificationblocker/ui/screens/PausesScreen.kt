package com.pasquale.notificationblocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.PauseCodec
import com.pasquale.notificationblocker.data.PauseSchedule
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.PauseRow
import com.pasquale.notificationblocker.ui.components.WeekCoverageCard
import com.pasquale.notificationblocker.ui.components.entrance
import com.pasquale.notificationblocker.data.WeekCoverage
import java.time.LocalDate
import com.pasquale.notificationblocker.ui.components.PauseEditorSheet
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek
import java.time.LocalDateTime

/** The Breaks tab: every pause as a row of one group, "Add a break", and the work apps they silence. */
@Composable
fun PausesScreen(
    viewModel: MainViewModel,
    onEditWorkApps: () -> Unit,
    onEditPauseApps: (pauseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pauses by viewModel.pauses.collectAsStateWithLifecycle()
    val ended by viewModel.pauseEndedKeys.collectAsStateWithLifecycle()
    val workApps by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
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
        modifier = modifier,
    )

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
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "week") {
                WeekCoverageCard(
                    coverage = remember(pauses) { WeekCoverage.of(pauses) },
                    today = LocalDate.now().dayOfWeek,
                    modifier = Modifier.entrance(0),
                )
            }
            if (pauses.isNotEmpty()) item(key = "pausesHeader") { SectionHeader(R.string.pauses_section_list) }
            if (pauses.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.pauses_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                // One rounded group, rows split by dividers, like the system settings
                item(key = "pauses") {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.entrance(1),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            pauses.forEachIndexed { index, pause ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                PauseRow(
                                    pause = pause,
                                    index = index,
                                    onClick = { onPauseClick(pause) },
                                    onEnabledChange = { onPauseEnabledChange(pause.id, it) },
                                    isOnNow = pause.id in onNow,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "apps") {
                SectionHeader(R.string.pauses_section_apps)
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.entrance(2),
                ) {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onEditWorkApps),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        },
                        headlineContent = { Text(text = stringResource(R.string.select_apps)) },
                        supportingContent = { Text(text = pluralStringResource(R.plurals.pauses_work_apps, workAppsCount, workAppsCount)) },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
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

/** A small label above a group of rows, like the section titles of the system settings. */
@Composable
private fun SectionHeader(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
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
