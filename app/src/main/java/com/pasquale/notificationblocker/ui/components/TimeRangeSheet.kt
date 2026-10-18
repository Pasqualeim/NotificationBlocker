package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.screens.TimePickerDialog

/**
 * A bottom sheet that sets a stretch of the day: [title], one line on what it changes ([text]), two
 * time boxes and Save, like the pause editor. Used for the work hours and the sleep window. [hint],
 * when not null for the chosen times, is shown under the boxes; [canSave] false disables Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSheet(
    title: String,
    text: String,
    startLabel: Int,
    endLabel: Int,
    initialStart: Int,
    initialEnd: Int,
    onSave: (start: Int, end: Int) -> Unit,
    onDismiss: () -> Unit,
    hint: @Composable (start: Int, end: Int) -> String? = { _, _ -> null },
    canSave: (start: Int, end: Int) -> Boolean = { _, _ -> true },
) {
    var start by rememberSaveable { mutableIntStateOf(initialStart) }
    var end by rememberSaveable { mutableIntStateOf(initialEnd) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeBox(startLabel, start, Modifier.weight(1f)) { picking = "start" }
                TimeBox(endLabel, end, Modifier.weight(1f)) { picking = "end" }
            }
            hint(start, end)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSave(start, end) },
                    enabled = canSave(start, end),
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
