package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

/** One choice of the "Pause now" sheet: its length, and whether it runs until the next break. */
data class QuickPauseOption(val minutes: Int, val untilNext: Boolean = false)

/** The fixed lengths, plus "until the next break" when one starts within [QuickPause.minutesToNext]'s reach. */
fun quickPauseOptions(minutesToNext: Int?): List<QuickPauseOption> =
    listOf(30, 60, 120).map { QuickPauseOption(it) } +
        listOfNotNull(minutesToNext?.takeIf { it !in setOf(30, 60, 120) }?.let { QuickPauseOption(it, untilNext = true) })

/**
 * "Pause now": a bottom sheet with a few lengths, each with the time it ends, and one button to start.
 *
 * @param nowMinute the current minute of the day, for the end times
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPauseSheet(
    options: List<QuickPauseOption>,
    nowMinute: Int,
    onStart: (QuickPauseOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        QuickPauseContent(options = options, nowMinute = nowMinute, onStart = onStart)
    }
}

@Composable
private fun QuickPauseContent(options: List<QuickPauseOption>, nowMinute: Int, onStart: (QuickPauseOption) -> Unit) {
    // One hour is the most common ask; kept in range if "until the next break" drops out while open
    var picked by rememberSaveable { mutableIntStateOf(1) }
    val chosen = picked.coerceIn(0, options.lastIndex)
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.quick_pause_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.quick_pause_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, option ->
                val selected = i == chosen
                val ends = OffHours.format((nowMinute + option.minutes) % OffHours.MINUTES_PER_DAY)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, role = Role.RadioButton) { picked = i },
                ) {
                    Row(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (option.untilNext) stringResource(R.string.quick_pause_until_next) else durationText(option.minutes),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.quick_pause_until, ends),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onStart(options[chosen]) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(text = stringResource(R.string.quick_pause_start))
        }
    }
}

@Preview(showBackground = true, name = "Pause now")
@Composable
fun QuickPauseContentPreview() {
    NotificationBlockerTheme {
        QuickPauseContent(quickPauseOptions(115), nowMinute = 11 * 60 + 5, onStart = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pause now (dark)")
@Composable
fun QuickPauseContentDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        QuickPauseContent(quickPauseOptions(null), nowMinute = 21 * 60, onStart = {})
    }
}
