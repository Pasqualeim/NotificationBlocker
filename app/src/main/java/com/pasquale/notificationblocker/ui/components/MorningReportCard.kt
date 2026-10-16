package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

/** One app in the report: its name as the launcher shows it and how many notifications it had. */
data class ReportApp(val packageName: String, val name: String, val count: Int)

/** The report as shown: [apps] are the apps with most held notifications, [moreApps] how many others. */
data class MorningReportUi(val total: Int, val apps: List<ReportApp>, val moreApps: Int)

/**
 * "During your break": how many work notifications came in and from which apps, each with "Open",
 * and the reminder that they are in their apps (held notifications do not come back). Shown once
 * after the pause ends: on Home it takes the top card's place until dismissed.
 */
@Composable
fun MorningReportCard(
    report: MorningReportUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: (packageName: String) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.report_count, report.total, report.total),
                style = MaterialTheme.typography.titleMedium,
            )
            if (report.apps.isNotEmpty()) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        for (app in report.apps) AppLine(app = app, onOpen = { onOpenApp(app.packageName) })
                        if (report.moreApps > 0) {
                            Text(
                                text = pluralStringResource(R.plurals.report_more_apps, report.moreApps, report.moreApps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 56.dp, bottom = 8.dp),
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.report_nothing_lost),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.report_dismiss), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun AppLine(app: ReportApp, onOpen: () -> Unit) {
    val openLabel = stringResource(R.string.report_open_app, app.name)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(packageName = app.packageName, contentDescription = null, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = pluralStringResource(R.plurals.report_app_count, app.count, app.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpen, modifier = Modifier.semantics { contentDescription = openLabel }) {
            Text(stringResource(R.string.report_open))
        }
    }
}

private val previewReport = MorningReportUi(
    total = 23,
    apps = listOf(ReportApp("com.slack", "Slack", 14), ReportApp("com.google.android.gm", "Gmail", 6)),
    moreApps = 1,
)

@Preview(showBackground = true, name = "Report - several apps")
@Composable
fun MorningReportCardPreview() {
    NotificationBlockerTheme {
        MorningReportCard(report = previewReport, onDismiss = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Report - one app (dark)")
@Composable
fun MorningReportCardOneAppPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        MorningReportCard(
            report = MorningReportUi(total = 1, apps = listOf(ReportApp("com.google.android.gm", "Gmail", 1)), moreApps = 0),
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
