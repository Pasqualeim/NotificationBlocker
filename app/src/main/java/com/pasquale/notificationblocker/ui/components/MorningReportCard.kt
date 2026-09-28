package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

/** The morning report as shown: [appNames] are the apps with most held notifications, [moreApps] the rest. */
data class MorningReportUi(val total: Int, val appNames: List<String>, val moreApps: Int)

/**
 * "While you were off": how many work notifications waited outside during the last window and
 * from which apps, with the reassurance that nothing is lost. Shown once after the window ends.
 */
@Composable
fun MorningReportCard(
    report: MorningReportUi?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The last report stays drawn while the card animates out, when report is already null
    val lastReport = remember { mutableStateOf(report) }
    if (report != null) lastReport.value = report

    AnimatedVisibility(
        visible = report != null,
        enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) +
            expandVertically(animationSpec = Motion.standard(Motion.MEDIUM)),
        exit = fadeOut(animationSpec = Motion.standard(Motion.SHORT)) +
            shrinkVertically(animationSpec = Motion.standard(Motion.MEDIUM)),
        modifier = modifier,
    ) {
        val shown = lastReport.value ?: return@AnimatedVisibility
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sun_dim),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.report_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.report_count, shown.total, shown.total) + " " + appsText(shown),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.report_nothing_lost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.report_dismiss), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun appsText(report: MorningReportUi): String {
    val names = report.appNames
    return when {
        names.isEmpty() -> ""
        report.moreApps > 0 && names.size >= 2 ->
            pluralStringResource(R.plurals.report_apps_more, report.moreApps, names[0], names[1], report.moreApps)
        names.size >= 2 -> stringResource(R.string.report_apps_two, names[0], names[1])
        else -> stringResource(R.string.report_apps_one, names[0])
    }
}

@Preview(showBackground = true, name = "Morning report - several apps")
@Composable
fun MorningReportCardPreview() {
    NotificationBlockerTheme {
        MorningReportCard(
            report = MorningReportUi(total = 23, appNames = listOf("Slack", "Teams"), moreApps = 2),
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Morning report - one app")
@Composable
fun MorningReportCardOneAppPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        MorningReportCard(
            report = MorningReportUi(total = 1, appNames = listOf("Gmail"), moreApps = 0),
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
