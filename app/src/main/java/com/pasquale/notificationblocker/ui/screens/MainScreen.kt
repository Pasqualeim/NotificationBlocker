package com.pasquale.notificationblocker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.HeroHeader
import com.pasquale.notificationblocker.ui.components.HomeColumn
import com.pasquale.notificationblocker.ui.components.MorningReportCard
import com.pasquale.notificationblocker.ui.components.MorningReportUi
import com.pasquale.notificationblocker.ui.components.PermissionCard
import com.pasquale.notificationblocker.ui.components.SceneCard
import com.pasquale.notificationblocker.ui.components.rememberAmbientClock
import com.pasquale.notificationblocker.ui.components.ScheduleCard
import com.pasquale.notificationblocker.ui.components.ZenNotificationCard
import com.pasquale.notificationblocker.ui.components.homeScene
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.LifeMessage
import kotlinx.coroutines.delay

// Above this system font scale the bottom button stacks its label and badge (they do not fit side by side)
private const val LARGE_FONT_SCALE = 1.3f

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToAppSelection: () -> Unit,
    modifier: Modifier = Modifier,
    sceneActive: Boolean = true,
) {
    val context = LocalContext.current
    val isBlockingEnabled by viewModel.isBlockingEnabled.collectAsStateWithLifecycle()
    val isInOffHoursNow by viewModel.isInOffHoursNow.collectAsStateWithLifecycle()
    val isPauseEnded by viewModel.isPauseEnded.collectAsStateWithLifecycle()
    val startTimeMinutes by viewModel.startTimeMinutes.collectAsStateWithLifecycle()
    val endTimeMinutes by viewModel.endTimeMinutes.collectAsStateWithLifecycle()
    val blockedAppsCount by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
    val lifeMessage by viewModel.lifeMessage.collectAsStateWithLifecycle()
    val sunshineMinutes by viewModel.sunshineMinutes.collectAsStateWithLifecycle()
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

    MainScreenContent(
        isBlockingEnabled = isBlockingEnabled,
        isInOffHoursNow = isInOffHoursNow,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        blockedAppsCount = blockedAppsCount,
        hasListenerPermission = hasListenerPermission,
        isPauseEnded = isPauseEnded,
        onPauseAgain = viewModel::pauseAgain,
        lifeMessage = lifeMessage,
        sunshineMinutes = sunshineMinutes,
        // Asked once, only after the listener permission, which is the one blocking needs
        showZenPrompt = hasListenerPermission && !canPostNotifications && !zenPromptDismissed,
        onAllowZenNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismissZenPrompt = { viewModel.onZenPermissionResult(granted = false) },
        onBlockingEnabledChanged = viewModel::setBlockingEnabled,
        onStartTimeChanged = viewModel::setStartTime,
        onEndTimeChanged = viewModel::setEndTime,
        onRequestPermission = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        onNavigateToAppSelection = onNavigateToAppSelection,
        modifier = modifier,
        sceneActive = sceneActive,
        morningReport = morningReport,
        onDismissMorningReport = viewModel::onMorningReportDismissed,
    )
}

@Composable
fun MainScreenContent(
    isBlockingEnabled: Boolean,
    isInOffHoursNow: Boolean,
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    blockedAppsCount: Int,
    hasListenerPermission: Boolean,
    onBlockingEnabledChanged: (Boolean) -> Unit,
    onStartTimeChanged: (Int) -> Unit,
    onEndTimeChanged: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToAppSelection: () -> Unit,
    modifier: Modifier = Modifier,
    sceneActive: Boolean = true,
    isPauseEnded: Boolean = false,
    onPauseAgain: () -> Unit = {},
    lifeMessage: LifeMessage? = null,
    sunshineMinutes: Int? = null,
    showZenPrompt: Boolean = false,
    onAllowZenNotification: () -> Unit = {},
    onDismissZenPrompt: () -> Unit = {},
    morningReport: MorningReportUi? = null,
    onDismissMorningReport: () -> Unit = {},
) {
    // Saveable: an open picker survives a rotation (the picker keeps its own hour and minute)
    var showStartTimePicker by rememberSaveable { mutableStateOf(value = false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(value = false) }

    var animatedHeaderVisible by remember { mutableStateOf(value = false) }
    var animatedTopCardVisible by remember { mutableStateOf(value = false) }
    var animatedScheduleVisible by remember { mutableStateOf(value = false) }
    var animatedSceneVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        animatedHeaderVisible = true
        delay(Motion.Stagger)
        animatedTopCardVisible = true
        delay(Motion.Stagger)
        animatedSceneVisible = true
        delay(Motion.Stagger)
        animatedScheduleVisible = true
    }

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) Motion.PRESS_SCALE else 1f,
        animationSpec = Motion.press(),
        label = "ButtonPressScale",
    )

    val scrollState = rememberScrollState()
    // One clock for the scene and the glow behind the bell. It stops while the page scrolls or another
    // screen slides over Home: on slower phones the scroll gets the whole frame budget
    val ambientClock = rememberAmbientClock(active = sceneActive && !scrollState.isScrollInProgress)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                // Same color as the page: the CTA floats, no two-tone band under it
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Button(
                        onClick = onNavigateToAppSelection,
                        interactionSource = buttonInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                            },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val appsBadge: @Composable () -> Unit = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.blocked_apps_count,
                                        blockedAppsCount,
                                        blockedAppsCount,
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                        if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) {
                            // Large text: label and badge stacked, each with the full width
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.select_apps),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                appsBadge()
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.select_apps),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            appsBadge()
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        // Home fits one screen on every phone, with the scene whole: HomeColumn scales the cards down
        // before the scene (rule: docs/DESIGN_SYSTEM.md, "Home in una schermata") and reads the viewport
        // from the min height that fillMaxSize passes through verticalScroll. It scrolls only as a last
        // resort, e.g. with the largest system font
        HomeColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // No stretch at the edges: the page is barely taller than the screen, so almost every scroll
                // reaches an edge, and the stretch redraws the whole page offscreen (18 ms of GPU per frame
                // on a Galaxy A32 against 5 ms without it)
                .verticalScroll(scrollState, overscrollEffect = null)
                // No bottom padding: the CTA bar already has 16dp above its button
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
        ) {
            Row(
                modifier = Modifier.homeEntrance(animatedHeaderVisible),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mug),
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.main_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

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
                    is TopCard.Report -> MorningReportCard(report = card.report, onDismiss = onDismissMorningReport)
                    TopCard.Status -> HeroHeader(
                        ambientClock = ambientClock,
                        isBlockingEnabled = isBlockingEnabled,
                        isInOffHoursNow = isInOffHoursNow,
                        onBlockingEnabledChanged = onBlockingEnabledChanged,
                        // The free-time line does not fit a pause ended early: the card says when the next one starts
                        lifeMessage = if (isPauseEnded) null else lifeMessage,
                        isPauseEnded = isPauseEnded,
                        nextPauseStart = OffHours.nextStart(startTimeMinutes, endTimeMinutes),
                        onPauseAgain = onPauseAgain,
                    )
                }
            }

            SceneCard(
                isOffWork = isBlockingEnabled && isInOffHoursNow && !isPauseEnded,
                modifier = Modifier.homeScene().homeEntrance(animatedSceneVisible),
                clock = ambientClock,
            )

            ScheduleCard(
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                isBlockingEnabled = isBlockingEnabled,
                onStartTimeClick = { showStartTimePicker = true },
                onEndTimeClick = { showEndTimePicker = true },
                sunshineMinutes = sunshineMinutes,
                modifier = Modifier.homeEntrance(animatedScheduleVisible),
            )
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialMinutes = startTimeMinutes,
            onConfirm = { minutes ->
                onStartTimeChanged(minutes)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialMinutes = endTimeMinutes,
            onConfirm = { minutes ->
                onEndTimeChanged(minutes)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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

private fun hasNotificationListenerPermission(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

// Below Android 13 posting needs no runtime permission, only notifications enabled for the app
private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Preview(showBackground = true, name = "Main Screen - Light Theme")
@Composable
fun MainScreenPreviewLight() {
    NotificationBlockerTheme(darkTheme = false) {
        MainScreenContent(
            isBlockingEnabled = true,
            isInOffHoursNow = true,
            startTimeMinutes = 22 * 60,
            endTimeMinutes = 7 * 60,
            blockedAppsCount = 4,
            hasListenerPermission = true,
            onBlockingEnabledChanged = {},
            onStartTimeChanged = {},
            onEndTimeChanged = {},
            onRequestPermission = {},
            onNavigateToAppSelection = {},
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
            startTimeMinutes = 22 * 60,
            endTimeMinutes = 7 * 60,
            blockedAppsCount = 12,
            hasListenerPermission = false,
            onBlockingEnabledChanged = {},
            onStartTimeChanged = {},
            onEndTimeChanged = {},
            onRequestPermission = {},
            onNavigateToAppSelection = {},
        )
    }
}

