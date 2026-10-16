package com.pasquale.notificationblocker.ui.screens

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.canPostNotifications
import com.pasquale.notificationblocker.ui.hasNotificationListenerPermission
import com.pasquale.notificationblocker.ui.openNotificationAccess
import com.pasquale.notificationblocker.ui.components.HeroHeader
import com.pasquale.notificationblocker.ui.components.QuickPauseSheet
import com.pasquale.notificationblocker.ui.components.quickPauseOptions
import com.pasquale.notificationblocker.ui.components.TodayMetrics
import com.pasquale.notificationblocker.ui.components.HomeColumn
import com.pasquale.notificationblocker.ui.components.MorningReportCard
import com.pasquale.notificationblocker.ui.components.MorningReportUi
import com.pasquale.notificationblocker.ui.components.PermissionCard
import com.pasquale.notificationblocker.ui.components.SceneCard
import com.pasquale.notificationblocker.ui.components.rememberAmbientClock
import com.pasquale.notificationblocker.ui.components.TodayCard
import com.pasquale.notificationblocker.ui.components.TodayPause
import com.pasquale.notificationblocker.ui.components.TodayPauses
import com.pasquale.notificationblocker.ui.components.ZenNotificationCard
import com.pasquale.notificationblocker.ui.components.homeScene
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.LifeMessage
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    sceneActive: Boolean = true,
) {
    val context = LocalContext.current
    val isBlockingEnabled by viewModel.isBlockingEnabled.collectAsStateWithLifecycle()
    val isInOffHoursNow by viewModel.isInOffHoursNow.collectAsStateWithLifecycle()
    val isPauseEnded by viewModel.isPauseEnded.collectAsStateWithLifecycle()
    val pauses by viewModel.allPauses.collectAsStateWithLifecycle()
    val minutesToNext by viewModel.minutesToNextPause.collectAsStateWithLifecycle()
    var showQuickPause by rememberSaveable { mutableStateOf(false) }
    val pauseEndedKeys by viewModel.pauseEndedKeys.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val nextPauseStart by viewModel.nextPauseStart.collectAsStateWithLifecycle()
    val lifeMessage by viewModel.lifeMessage.collectAsStateWithLifecycle()
    val zenPromptDismissed by viewModel.zenPromptDismissed.collectAsStateWithLifecycle()
    val morningReport by viewModel.morningReport.collectAsStateWithLifecycle()

    var hasListenerPermission by remember { mutableStateOf(hasNotificationListenerPermission(context)) }
    var canPostNotifications by remember { mutableStateOf(canPostNotifications(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canPostNotifications = granted
        viewModel.onZenPermissionResult(granted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOffHoursStatus()
        hasListenerPermission = hasNotificationListenerPermission(context)
        canPostNotifications = canPostNotifications(context)
    }

    // The window can open or close while Home is on screen: re-check it every minute
    val currentMinutes by rememberCurrentMinutes()
    LaunchedEffect(currentMinutes) { viewModel.refreshOffHoursStatus() }

    val now = LocalDateTime.now()
    val todayPauses = remember(pauses, pauseEndedKeys, currentMinutes) { TodayPauses.of(pauses, now, pauseEndedKeys) }

    MainScreenContent(
        isBlockingEnabled = isBlockingEnabled,
        isInOffHoursNow = isInOffHoursNow,
        todayPauses = todayPauses,
        livedTodayMinutes = history[now.toLocalDate()]?.livedMinutes ?: 0,
        heldToday = history[now.toLocalDate()]?.held ?: 0,
        daylightTodayMinutes = history[now.toLocalDate()]?.daylightMinutes ?: 0,
        currentMinutes = currentMinutes,
        nextPauseStart = nextPauseStart,
        hasListenerPermission = hasListenerPermission,
        isPauseEnded = isPauseEnded,
        onPauseAgain = viewModel::pauseAgain,
        lifeMessage = lifeMessage,
        // Asked once, only after the listener permission, which is the one blocking needs
        showZenPrompt = hasListenerPermission && !canPostNotifications && !zenPromptDismissed,
        onAllowZenNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismissZenPrompt = { viewModel.onZenPermissionResult(granted = false) },
        onBlockingEnabledChanged = viewModel::setBlockingEnabled,
        onRequestPermission = { openNotificationAccess(context) },
        modifier = modifier,
        sceneActive = sceneActive,
        morningReport = morningReport,
        onDismissMorningReport = viewModel::onMorningReportDismissed,
        onQuickPause = { showQuickPause = true },
        onOpenApp = viewModel::openApp,
    )

    if (showQuickPause) {
        QuickPauseSheet(
            options = quickPauseOptions(minutesToNext),
            nowMinute = currentMinutes,
            onStart = { option ->
                viewModel.startQuickPause(option.minutes, option.untilNext)
                showQuickPause = false
            },
            onDismiss = { showQuickPause = false },
        )
    }
}

@Composable
fun MainScreenContent(
    isBlockingEnabled: Boolean,
    isInOffHoursNow: Boolean,
    todayPauses: List<TodayPause>,
    livedTodayMinutes: Int,
    currentMinutes: Int,
    hasListenerPermission: Boolean,
    onBlockingEnabledChanged: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    nextPauseStart: Int? = null,
    sceneActive: Boolean = true,
    isPauseEnded: Boolean = false,
    onPauseAgain: () -> Unit = {},
    lifeMessage: LifeMessage? = null,
    showZenPrompt: Boolean = false,
    onAllowZenNotification: () -> Unit = {},
    onDismissZenPrompt: () -> Unit = {},
    morningReport: MorningReportUi? = null,
    onDismissMorningReport: () -> Unit = {},
    heldToday: Int = 0,
    daylightTodayMinutes: Int = 0,
    onQuickPause: (() -> Unit)? = null,
    onOpenApp: (String) -> Unit = {},
) {
    var animatedMetricsVisible by remember { mutableStateOf(value = false) }
    var animatedTopCardVisible by remember { mutableStateOf(value = false) }
    var animatedScheduleVisible by remember { mutableStateOf(value = false) }
    var animatedSceneVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        animatedTopCardVisible = true
        delay(Motion.Stagger)
        animatedSceneVisible = true
        delay(Motion.Stagger)
        animatedMetricsVisible = true
        delay(Motion.Stagger)
        animatedScheduleVisible = true
    }

    val scrollState = rememberScrollState()
    // One clock for the scene and the glow behind the bell. It stops while the page scrolls or another
    // screen slides over Home: on slower phones the scroll gets the whole frame budget
    val ambientClock = rememberAmbientClock(active = sceneActive && !scrollState.isScrollInProgress)

    // Home fits one screen on every phone, with the scene whole: HomeColumn scales the cards down
    // before the scene (rule: docs/DESIGN_SYSTEM.md, "Home in una schermata") and reads the viewport
    // from the min height that fillMaxSize passes through verticalScroll. It scrolls only as a last
    // resort, e.g. with the largest system font
    HomeColumn(
        modifier = modifier
            .fillMaxSize()
            // No stretch at the edges: the page is barely taller than the screen, so almost every scroll
            // reaches an edge, and the stretch redraws the whole page offscreen (18 ms of GPU per frame
            // on a Galaxy A32 against 5 ms without it)
            .verticalScroll(scrollState, overscrollEffect = null)
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
    ) {
        // One card at the top, the most urgent thing first: an extra card would push the page past the
        // screen and HomeColumn would shrink every text, then grow it back once the card is gone
        val topCard = when {
            !hasListenerPermission -> TopCard.Permission
            showZenPrompt -> TopCard.ZenPrompt
            morningReport != null -> TopCard.Report(morningReport)
            else -> TopCard.Status
        }
        AnimatedContent(
            targetState = topCard,
            // A report that changes while shown is updated in place, not cross-faded
            contentKey = { it::class },
            transitionSpec = {
                fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) togetherWith
                    fadeOut(animationSpec = Motion.standard(Motion.SHORT))
            },
            modifier = Modifier.homeEntrance(animatedTopCardVisible),
            label = "HomeTopCard",
        ) { card ->
            when (card) {
                TopCard.Permission -> PermissionCard(onRequestPermission = onRequestPermission)
                TopCard.ZenPrompt -> ZenNotificationCard(
                    onAllow = onAllowZenNotification,
                    onDismiss = onDismissZenPrompt,
                )
                is TopCard.Report -> MorningReportCard(report = card.report, onDismiss = onDismissMorningReport, onOpenApp = onOpenApp)
                TopCard.Status -> HeroHeader(
                    ambientClock = ambientClock,
                    isBlockingEnabled = isBlockingEnabled,
                    isInOffHoursNow = isInOffHoursNow,
                    onBlockingEnabledChanged = onBlockingEnabledChanged,
                    // The free-time line does not fit a pause ended early: the card says when the next one starts
                    // "You switch off at 18:30" only repeats the title, which already says when the pause starts
                    lifeMessage = if (isPauseEnded || lifeMessage is LifeMessage.EveningAfterWork) null else lifeMessage,
                    isPauseEnded = isPauseEnded,
                    nextPauseStart = nextPauseStart ?: -1,
                    onPauseAgain = onPauseAgain,
                    onQuickPause = onQuickPause,
                )
            }
        }

        SceneCard(
            isOffWork = isBlockingEnabled && isInOffHoursNow && !isPauseEnded,
            modifier = Modifier.homeScene().homeEntrance(animatedSceneVisible),
            clock = ambientClock,
        )

        TodayMetrics(
            livedMinutes = livedTodayMinutes,
            held = heldToday,
            daylightMinutes = daylightTodayMinutes,
            modifier = Modifier.homeEntrance(animatedMetricsVisible),
        )

        TodayCard(
            pauses = todayPauses,
            nowMinute = currentMinutes,
            isBlockingEnabled = isBlockingEnabled,
            modifier = Modifier.homeEntrance(animatedScheduleVisible),
        )
    }
}

/**
 * What the card at the top of Home shows, one at a time: the listener permission (nothing works
 * without it), then the optional notification prompt, then the morning report, then the status card
 * with the master switch.
 */
private sealed interface TopCard {
    data object Permission : TopCard
    data object ZenPrompt : TopCard
    data class Report(val report: MorningReportUi) : TopCard
    data object Status : TopCard
}

/**
 * Entrance of one Home block: it fades in and rises into place once [visible]. The block is composed
 * and measured from the first frame, so [HomeColumn] fits the final page and nothing is resized
 * while the blocks come in one after the other.
 */
@Composable
private fun Modifier.homeEntrance(visible: Boolean): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.standard(Motion.MEDIUM),
        label = "HomeEntrance",
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * size.height / 2f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.time_picker_title),
                style = MaterialTheme.typography.labelMedium,
            )
        },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = (state.hour * 60) + state.minute
                    onConfirm(minutes)
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    )
}

@Preview(showBackground = true, name = "Main Screen - Light Theme")
@Composable
fun MainScreenPreviewLight() {
    NotificationBlockerTheme(darkTheme = false) {
        MainScreenContent(
            isBlockingEnabled = true,
            isInOffHoursNow = true,
            todayPauses = emptyList(),
            livedTodayMinutes = 130,
            currentMinutes = 19 * 60,
            hasListenerPermission = true,
            onBlockingEnabledChanged = {},
            onRequestPermission = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Main Screen - Dark Theme")
@Composable
fun MainScreenPreviewDark() {
    NotificationBlockerTheme(darkTheme = true) {
        MainScreenContent(
            isBlockingEnabled = true,
            isInOffHoursNow = false,
            todayPauses = emptyList(),
            livedTodayMinutes = 0,
            currentMinutes = 12 * 60,
            hasListenerPermission = false,
            onBlockingEnabledChanged = {},
            onRequestPermission = {},
        )
    }
}

