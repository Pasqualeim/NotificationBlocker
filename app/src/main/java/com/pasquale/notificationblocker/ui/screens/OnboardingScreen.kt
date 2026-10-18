package com.pasquale.notificationblocker.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.pasquale.notificationblocker.ui.canPostNotifications
import com.pasquale.notificationblocker.ui.hasNotificationListenerPermission
import com.pasquale.notificationblocker.ui.openNotificationAccess
import com.pasquale.notificationblocker.ui.components.PauseAvatar
import com.pasquale.notificationblocker.ui.components.WeekCoverageCard
import com.pasquale.notificationblocker.ui.components.expressiveShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.SleepWindow
import com.pasquale.notificationblocker.data.WeekCoverage
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.ui.components.WorkHoursSheet
import com.pasquale.notificationblocker.ui.components.WorkScheduleCard
import com.pasquale.notificationblocker.ui.AppInfo
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.AppIconImage
import com.pasquale.notificationblocker.ui.components.pauseTimes
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.DayOfWeek

/** Apps that usually bring work, offered first in the setup when installed. */
private val WORK_APP_HINTS = listOf(
    "com.Slack", "com.microsoft.teams", "com.google.android.gm", "com.microsoft.office.outlook",
    "com.google.android.apps.dynamite", "us.zoom.videomeetings", "com.whatsapp.w4b", "com.google.android.calendar",
    "com.linkedin.android", "com.asana.app", "com.trello", "com.atlassian.android.jira.core", "notion.id",
)

/** The lunch break offered in step 2, on the work days; ticked to start with. */
private fun lunchBreak(schedule: WorkSchedule, name: String) = Pause("lunch", name, 13 * 60, 14 * 60, days = schedule.days)

private const val STEPS = 3

/**
 * First launch, three steps: which apps are work, when you work (the rest is paused, plus an optional
 * lunch break), and the notification access. Ends on Today with Nook on. Each step can be changed later in the app.
 */
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onEditAllApps: () -> Unit, onDone: () -> Unit) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadInstalledApps() }
    val context = LocalContext.current
    val lunchName = stringResource(R.string.onboarding_lunch)
    // Checked again when the user comes back from the system page, so step 3 can say it worked
    var hasAccess by remember { mutableStateOf(hasNotificationListenerPermission(context)) }
    // Android asks for the break notification when the setup ends (see onFinish): step 3 says so first
    var willAskNotification by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = hasNotificationListenerPermission(context)
        willAskNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications(context)
    }
    // The choices while the system dialog is up, as text ("schedule#lunch"): saved, so a recreation
    // behind the dialog still completes the setup; cleared once used, so it completes exactly once
    var finishing by rememberSaveable { mutableStateOf<String?>(null) }
    val complete = { choice: String ->
        val schedule = WorkSchedule.decode(choice.substringBefore('#')) ?: WorkSchedule.DEFAULT
        val lunch = choice.substringAfter('#') == "1"
        // A lunch break with no work day would never happen: not saved
        viewModel.completeOnboarding(schedule, if (lunch && schedule.days.isNotEmpty()) listOf(lunchBreak(schedule, lunchName)) else emptyList())
        onDone()
    }
    // The break notification is asked for here, at the end of the setup, where the user is still
    // giving permissions: asked later on Today it took the status card's place on the first look
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onZenPermissionResult(granted)
        finishing?.also { finishing = null }?.let(complete)
    }
    OnboardingContent(
        suggestions = remember(apps) { WORK_APP_HINTS.mapNotNull { hint -> apps.firstOrNull { it.packageName == hint } } },
        onToggleApp = viewModel::toggleAppBlocked,
        onEditAllApps = onEditAllApps,
        onOpenAccess = { openNotificationAccess(context) },
        hasAccess = hasAccess,
        willAskNotification = willAskNotification,
        onFinish = onFinish@{ schedule, lunch ->
            // A second tap while the dialog is up would finish twice
            if (finishing != null) return@onFinish
            val choice = WorkSchedule.encode(schedule) + "#" + (if (lunch) "1" else "0")
            if (hasAccess && willAskNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                finishing = choice
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                complete(choice)
            }
        },
    )
}

@Composable
fun OnboardingContent(
    suggestions: List<AppInfo>,
    onToggleApp: (String, Boolean) -> Unit,
    onEditAllApps: () -> Unit,
    onOpenAccess: () -> Unit,
    onFinish: (schedule: WorkSchedule, lunch: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hasAccess: Boolean = false,
    willAskNotification: Boolean = false,
    initialStep: Int = 0,
) {
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    var scheduleText by rememberSaveable { mutableStateOf(WorkSchedule.encode(WorkSchedule.DEFAULT)) }
    val schedule = WorkSchedule.decode(scheduleText) ?: WorkSchedule.DEFAULT
    var lunch by rememberSaveable { mutableStateOf(true) }
    var editingHours by rememberSaveable { mutableStateOf(false) }
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
                1 -> WorkStep(
                    schedule = schedule,
                    onDaysChange = { scheduleText = WorkSchedule.encode(schedule.copy(days = it)) },
                    onHoursClick = { editingHours = true },
                    lunch = lunch,
                    onLunchChange = { lunch = it },
                )
                else -> AccessStep(onOpenAccess, hasAccess, willAskNotification)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.onboarding_back)) }
            Spacer(modifier = Modifier.weight(1f))
            // One filled button at a time: on the last step, before the access is given, the
            // filled one is "Allow access" above and this one only skips it
            if (step == STEPS - 1 && !hasAccess) {
                TextButton(onClick = { onFinish(schedule, lunch) }, modifier = Modifier.height(48.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            } else {
                Button(onClick = { if (step < STEPS - 1) step++ else onFinish(schedule, lunch) }, modifier = Modifier.height(48.dp)) {
                    Text(stringResource(if (step < STEPS - 1) R.string.onboarding_next else R.string.onboarding_finish))
                }
            }
        }
    }
    }
    if (editingHours) {
        WorkHoursSheet(
            schedule = schedule,
            onSave = {
                scheduleText = WorkSchedule.encode(it)
                editingHours = false
            },
            onDismiss = { editingHours = false },
        )
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
private fun WorkStep(
    schedule: WorkSchedule,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onHoursClick: () -> Unit,
    lunch: Boolean,
    onLunchChange: (Boolean) -> Unit,
) {
    Text(stringResource(R.string.onboarding_work_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(
        stringResource(R.string.onboarding_work_text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    WorkScheduleCard(schedule = schedule, onDaysChange = onDaysChange, onHoursClick = onHoursClick)
    val lunchPause = lunchBreak(schedule, stringResource(R.string.onboarding_lunch))
    ChoiceRow(checked = lunch, onToggle = { onLunchChange(!lunch) }) {
        PauseAvatar(pause = lunchPause, index = 1)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(lunchPause.name, style = MaterialTheme.typography.titleMedium)
            Text(
                pauseTimes(lunchPause),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // The week of the Breaks tab, redrawn as the choices change: it shows which hours stay free of work
    WeekCoverageCard(
        coverage = WeekCoverage.of(schedule.offWorkPauses() + lunchPause.copy(enabled = lunch), SleepWindow.DEFAULT),
    )
}

@Composable
private fun AccessStep(onOpenAccess: () -> Unit, hasAccess: Boolean, willAskNotification: Boolean) {
    Text(stringResource(R.string.onboarding_access_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(
        stringResource(R.string.onboarding_access_text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // The permission disclosure, one point per line: what it sees, what it doesn't, nothing leaves, revocable
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DisclosurePoint(Icons.Default.Notifications, R.string.onboarding_access_sees, 0)
            DisclosurePoint(Icons.Default.Lock, R.string.onboarding_access_reads_nothing, 1)
            DisclosurePoint(Icons.Default.Phone, R.string.onboarding_access_stays, 2)
            DisclosurePoint(Icons.Default.Settings, R.string.onboarding_access_revocable, 3)
        }
    }
    if (hasAccess) {
        // Announced by TalkBack when the user comes back from the system page
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, expressiveShape(0)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(R.string.onboarding_access_done),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        // Without this the system prompt that follows ("Allow Nook to send you notifications?")
        // read as a contradiction in an app that pauses notifications
        if (willAskNotification) {
            Text(
                stringResource(R.string.onboarding_notification_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Button(onClick = onOpenAccess, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.onboarding_access_button))
        }
        Text(
            stringResource(R.string.onboarding_access_how),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One point of the access disclosure: an icon in an expressive shape and one sentence. */
@Composable
private fun DisclosurePoint(icon: ImageVector, @StringRes text: Int, shapeIndex: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, expressiveShape(shapeIndex)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        }
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
    }
}

/** A tappable row with a checkbox at the end, filled when [checked]. */
@Composable
private fun ChoiceRow(checked: Boolean, onToggle: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        // Picked rows keep a neutral fill, marked by the border and the tick: a tinted fill hid the
        // pause avatar drawn in the same container color
        color = if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
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
            onFinish = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - access given")
@Composable
fun OnboardingAccessDonePreview() {
    NotificationBlockerTheme {
        OnboardingContent(
            suggestions = emptyList(),
            onToggleApp = { _, _ -> },
            onEditAllApps = {},
            onOpenAccess = {},
            onFinish = { _, _ -> },
            hasAccess = true,
            willAskNotification = true,
            initialStep = 2,
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - access missing")
@Composable
fun OnboardingAccessPreview() {
    NotificationBlockerTheme {
        OnboardingContent(
            suggestions = emptyList(),
            onToggleApp = { _, _ -> },
            onEditAllApps = {},
            onOpenAccess = {},
            onFinish = { _, _ -> },
            initialStep = 2,
        )
    }
}
