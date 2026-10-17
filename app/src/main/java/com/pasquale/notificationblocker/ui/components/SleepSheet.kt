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
import com.pasquale.notificationblocker.data.SleepWindow
import com.pasquale.notificationblocker.ui.screens.TimePickerDialog

/**
 * "When you sleep", from the "More" group of Breaks: bedtime and wake-up time, with one line on what
 * they change (work stays paused, those hours are not counted as free time). Saved with the button, like
 * the pause editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(window: SleepWindow, onSave: (SleepWindow) -> Unit, onDismiss: () -> Unit) {
    var start by rememberSaveable { mutableIntStateOf(window.start) }
    var end by rememberSaveable { mutableIntStateOf(window.end) }
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
            Text(text = stringResource(R.string.sleep_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.sleep_sheet_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeBox(R.string.sleep_bedtime, start, Modifier.weight(1f)) { picking = "start" }
                TimeBox(R.string.sleep_wake, end, Modifier.weight(1f)) { picking = "end" }
            }
            // Two equal times leave no sleep out: say so, or the hours would jump up with no reason
            if (start == end) {
                Text(
                    text = stringResource(R.string.sleep_none_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSave(SleepWindow(start, end)) },
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
