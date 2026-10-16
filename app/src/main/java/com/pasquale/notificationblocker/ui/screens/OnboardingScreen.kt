package com.pasquale.notificationblocker.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.WeekCoverage
import com.pasquale.notificationblocker.ui.AppInfo
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.AppIconImage
import com.pasquale.notificationblocker.ui.components.daysSummary
import com.pasquale.notificationblocker.ui.components.pauseTimes
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek

/** Apps that usually bring work, offered first in the setup when installed. */
private val WORK_APP_HINTS = listOf(
    "com.Slack", "com.microsoft.teams", "com.google.android.gm", "com.microsoft.office.outlook",
    "com.google.android.apps.dynamite", "us.zoom.videomeetings", "com.whatsapp.w4b", "com.google.android.calendar",
    "com.linkedin.android", "com.asana.app", "com.trello", "com.atlassian.android.jira.core", "notion.id",
)

/** The break templates of step 2, with which ones start ticked. */
private val TEMPLATES = listOf(
    Pause("evening", "", 18 * 60 + 30, 8 * 60 + 30) to true,
    Pause("lunch", "", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet()) to true,
    Pause("weekend", "", 0, 0, days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) to false,
)

private const val STEPS = 3

/**
 * First launch, three steps: which apps are work, when to switch off (from templates), and the
 * notification access. Ends on Today with Nook on. Each step can be changed later in the app.
 */
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onEditAllApps: () -> Unit, onDone: () -> Unit) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadInstalledApps() }
    val context = LocalContext.current
    val templateNames = listOf(stringResource(R.string.onboarding_evening), stringResource(R.string.onboarding_lunch), stringResource(R.string.onboarding_weekend))
    OnboardingContent(
        suggestions = remember(apps) { WORK_APP_HINTS.mapNotNull { hint -> apps.firstOrNull { it.packageName == hint } } },
        onToggleApp = viewModel::toggleAppBlocked,
        onEditAllApps = onEditAllApps,
        onOpenAccess = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        onFinish = { picked ->
            viewModel.completeOnboarding(picked.map { i -> TEMPLATES[i].first.copy(name = templateNames[i]) })
            onDone()
        },
    )
}

@Composable
fun OnboardingContent(
    suggestions: List<AppInfo>,
    onToggleApp: (String, Boolean) -> Unit,
    onEditAllApps: () -> Unit,
    onOpenAccess: () -> Unit,
    onFinish: (picked: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableStateOf(TEMPLATES.indices.filter { TEMPLATES[it].second }) }
    // System back steps back like the Back button; on the first step it leaves the app
    BackHandler(enabled = step > 0) { step-- }

    // Opaque, like every screen of the app (see the transitions in MainActivity)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (step + 1f) / STEPS },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_step, step + 1, STEPS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                0 -> AppsStep(suggestions, onToggleApp, onEditAllApps)
                1 -> PausesStep(picked) { i -> picked = if (i in picked) picked - i else (picked + i).sorted() }
                else -> AccessStep(onOpenAccess)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.onboarding_back)) }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { if (step < STEPS - 1) step++ else onFinish(picked) }, modifier = Modifier.height(48.dp)) {
                Text(stringResource(if (step < STEPS - 1) R.string.onboarding_next else R.string.onboarding_finish))
            }
        }
    }
    }
}

@Composable
private fun AppsStep(suggestions: List<AppInfo>, onToggleApp: (String, Boolean) -> Unit, onEditAllApps: () -> Unit) {
    Text(stringResource(R.string.onboarding_apps_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(
        stringResource(R.string.onboarding_apps_text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    for (app in suggestions) {
        ChoiceRow(checked = app.isBlocked, onToggle = { onToggleApp(app.packageName, !app.isBlocked) }) {
            AppIconImage(packageName = app.packageName, contentDescription = null, size = 36.dp)
            Text(app.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        }
    }
    OutlinedButton(onClick = onEditAllApps, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (suggestions.isEmpty()) R.string.onboarding_apps_pick else R.string.onboarding_apps_more))
    }
}

@Composable
private fun PausesStep(picked: List<Int>, onToggle: (Int) -> Unit) {
    val names = listOf(R.string.onboarding_evening, R.string.onboarding_lunch, R.string.onboarding_weekend)
    Text(stringResource(R.string.onboarding_pauses_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(
        stringResource(R.string.onboarding_pauses_text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TEMPLATES.forEachIndexed { i, (pause, _) ->
        ChoiceRow(checked = i in picked, onToggle = { onToggle(i) }) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(names[i]), style = MaterialTheme.typography.titleMedium)
                Text(
                    pauseTimes(pause) + "  ·  " + daysSummary(pause.days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val hours = WeekCoverage.of(picked.map { TEMPLATES[it].first }).weeklyMinutes / 60
    if (hours > 0) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text(
                pluralStringResource(R.plurals.onboarding_hours, hours, hours),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@Composable
private fun AccessStep(onOpenAccess: () -> Unit) {
    Text(stringResource(R.string.onboarding_access_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    // The permission disclosure, with its four points (what it sees, what it doesn't, nothing leaves, revocable)
    Text(
        stringResource(R.string.permission_warning_text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onOpenAccess, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(stringResource(R.string.permission_warning_button))
    }
    Text(
        stringResource(R.string.onboarding_access_later),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A tappable row with a checkbox at the end, filled when [checked]. */
@Composable
private fun ChoiceRow(checked: Boolean, onToggle: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (checked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
            Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary))
        }
    }
}

@Preview(showBackground = true, name = "Onboarding - apps")
@Composable
fun OnboardingPreview() {
    NotificationBlockerTheme {
        OnboardingContent(
            suggestions = listOf(AppInfo("com.Slack", "Slack", true), AppInfo("com.google.android.gm", "Gmail", false)),
            onToggleApp = { _, _ -> },
            onEditAllApps = {},
            onOpenAccess = {},
            onFinish = {},
        )
    }
}
