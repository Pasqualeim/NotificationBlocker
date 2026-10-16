package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.ui.screens.TimePickerDialog
import java.time.DayOfWeek

/**
 * Bottom sheet to create or change a pause: name, start and end, days, and which apps it silences.
 * Nothing is written until "Save", except through the apps row: the app list is another screen and
 * writes the pause directly, so the pause is saved before it opens (only with at least one day).
 *
 * @param isNew a pause not saved yet: no "Delete"
 * @param onEditApps opens the app list for this pause, with the pause as it is in the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseEditorSheet(
    pause: Pause,
    isNew: Boolean,
    onSave: (Pause) -> Unit,
    onDelete: () -> Unit,
    onEditApps: (Pause) -> Unit,
    onResetApps: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(pause.id) { mutableStateOf(pause.name) }
    var start by rememberSaveable(pause.id) { mutableIntStateOf(pause.start) }
    var end by rememberSaveable(pause.id) { mutableIntStateOf(pause.end) }
    var days by rememberSaveable(pause.id) { mutableStateOf(pause.days.map { it.ordinal }) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    fun edited() = pause.copy(
        name = name.trim(),
        start = start,
        end = end,
        days = days.map { DayOfWeek.entries[it] }.toSet(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(if (isNew) R.string.pause_new_title else R.string.pause_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            NameField(name = name, onNameChange = { name = it.take(MAX_NAME) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeBox(R.string.pause_start, start, Modifier.weight(1f)) { picking = "start" }
                TimeBox(R.string.pause_end, end, Modifier.weight(1f)) { picking = "end" }
            }
            if (start == end) {
                Text(
                    text = stringResource(R.string.pause_whole_day_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.pause_days),
                style = MaterialTheme.typography.titleSmall,
            )
            DayPills(
                days = days.map { DayOfWeek.entries[it] }.toSet(),
                size = 40,
                onToggle = { day -> days = if (day.ordinal in days) days - day.ordinal else days + day.ordinal },
            )
            // Choosing apps saves the pause first (the list is another screen): only once it can happen
            AppsRow(pause = pause, enabled = days.isNotEmpty(), onClick = { onEditApps(edited()) }, onReset = onResetApps)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text(text = stringResource(R.string.pause_delete))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSave(edited()) },
                    // A pause needs at least one day to ever happen
                    enabled = days.isNotEmpty(),
                    modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 48.dp),
                ) {
                    Text(text = stringResource(R.string.pause_save))
                }
            }
        }
    }

    picking?.let { which ->
        TimePickerDialog(
            initialMinutes = if (which == "start") start else end,
            onConfirm = { minutes ->
                if (which == "start") start = minutes else end = minutes
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

private const val MAX_NAME = 24

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit) {
    // Bare BasicTextField, like SearchField: the Material TextField took ~60 ms to compose
    BasicTextField(
        value = name,
        onValueChange = onNameChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        decorationBox = { inner ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.pause_name_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    if (name.isEmpty()) {
                        Text(
                            text = stringResource(R.string.pause_name_hint),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun TimeBox(label: Int, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = OffHours.format(minutes), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun AppsRow(pause: Pause, enabled: Boolean, onClick: () -> Unit, onReset: () -> Unit) {
    val apps = pause.apps
    Column {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(text = stringResource(R.string.pause_apps_label)) },
            supportingContent = {
                Text(
                    text = if (apps == null) {
                        stringResource(R.string.pause_apps_all)
                    } else {
                        pluralStringResource(R.plurals.pause_apps_custom, apps.size, apps.size)
                    },
                )
            },
            trailingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        }
        if (apps != null) {
            TextButton(onClick = onReset, modifier = Modifier.padding(start = 4.dp)) {
                Text(text = stringResource(R.string.pause_apps_reset))
            }
        }
    }
}
