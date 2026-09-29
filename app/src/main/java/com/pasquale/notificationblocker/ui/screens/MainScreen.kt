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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.pasquale.notificationblocker.ui.MainViewModel
import com.pasquale.notificationblocker.ui.components.EndOfShiftCard
import com.pasquale.notificationblocker.ui.components.HeroHeader
import com.pasquale.notificationblocker.ui.components.MorningReportCard
import com.pasquale.notificationblocker.ui.components.MorningReportUi
import com.pasquale.notificationblocker.ui.components.PermissionCard
import com.pasquale.notificationblocker.ui.components.SceneCard
import com.pasquale.notificationblocker.ui.components.ScheduleCard
import com.pasquale.notificationblocker.ui.components.ZenNotificationCard
import com.pasquale.notificationblocker.ui.components.rememberCurrentMinutes
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.LifeMessage
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToAppSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isBlockingEnabled by viewModel.isBlockingEnabled.collectAsStateWithLifecycle()
    val isInOffHoursNow by viewModel.isInOffHoursNow.collectAsStateWithLifecycle()
    val startTimeMinutes by viewModel.startTimeMinutes.collectAsStateWithLifecycle()
    val endTimeMinutes by viewModel.endTimeMinutes.collectAsStateWithLifecycle()
    val blockedAppsCount by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
    val showEndOfShift by viewModel.showEndOfShift.collectAsStateWithLifecycle()
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
        showEndOfShift = showEndOfShift,
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
        onEndOfShiftShown = viewModel::onEndOfShiftShown,
        modifier = modifier,
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
    showEndOfShift: Boolean,
    onBlockingEnabledChanged: (Boolean) -> Unit,
    onStartTimeChanged: (Int) -> Unit,
    onEndTimeChanged: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToAppSelection: () -> Unit,
    onEndOfShiftShown: () -> Unit,
    modifier: Modifier = Modifier,
    lifeMessage: LifeMessage? = null,
    sunshineMinutes: Int? = null,
    showZenPrompt: Boolean = false,
    onAllowZenNotification: () -> Unit = {},
    onDismissZenPrompt: () -> Unit = {},
    morningReport: MorningReportUi? = null,
    onDismissMorningReport: () -> Unit = {},
) {
    var showStartTimePicker by remember { mutableStateOf(value = false) }
    var showEndTimePicker by remember { mutableStateOf(value = false) }

    var animatedHeaderVisible by remember { mutableStateOf(value = false) }
    var animatedPermissionVisible by remember { mutableStateOf(value = false) }
    var animatedHeroVisible by remember { mutableStateOf(value = false) }
    var animatedScheduleVisible by remember { mutableStateOf(value = false) }
    var animatedSceneVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        animatedHeaderVisible = true
        delay(Motion.Stagger)
        animatedPermissionVisible = true
        delay(Motion.Stagger)
        animatedHeroVisible = true
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
                            .height(56.dp)
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
                        Text(
                            text = stringResource(R.string.select_apps),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
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
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(
                visible = animatedHeaderVisible,
                enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) + slideInVertically(
                    animationSpec = Motion.standard(Motion.MEDIUM),
                ) { it / 2 },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mug),
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.main_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            AnimatedVisibility(
                visible = animatedPermissionVisible,
                enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) + slideInVertically(
                    animationSpec = Motion.standard(Motion.MEDIUM),
                ) { it / 2 },
            ) {
                Column {
                    PermissionCard(
                        visible = !hasListenerPermission,
                        onRequestPermission = onRequestPermission,
                    )
                    ZenNotificationCard(
                        visible = showZenPrompt,
                        onAllow = onAllowZenNotification,
                        onDismiss = onDismissZenPrompt,
                    )
                }
            }

            MorningReportCard(
                report = if (animatedHeroVisible) morningReport else null,
                onDismiss = onDismissMorningReport,
            )

            EndOfShiftCard(
                visible = showEndOfShift && animatedHeroVisible,
                onFinished = onEndOfShiftShown,
            )

            AnimatedVisibility(
                visible = animatedHeroVisible,
                enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) + slideInVertically(
                    animationSpec = Motion.standard(Motion.MEDIUM),
                ) { it / 2 },
            ) {
                HeroHeader(
                    isBlockingEnabled = isBlockingEnabled,
                    isInOffHoursNow = isInOffHoursNow,
                    onBlockingEnabledChanged = onBlockingEnabledChanged,
                    lifeMessage = lifeMessage,
                )
            }

            AnimatedVisibility(
                visible = animatedSceneVisible,
                enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) + slideInVertically(
                    animationSpec = Motion.standard(Motion.MEDIUM),
                ) { it / 2 },
            ) {
                SceneCard(isOffWork = isBlockingEnabled && isInOffHoursNow)
            }

            AnimatedVisibility(
                visible = animatedScheduleVisible,
                enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) + slideInVertically(
                    animationSpec = Motion.standard(Motion.MEDIUM),
                ) { it / 2 },
            ) {
                ScheduleCard(
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes,
                    isBlockingEnabled = isBlockingEnabled,
                    onStartTimeClick = { showStartTimePicker = true },
                    onEndTimeClick = { showEndTimePicker = true },
                    sunshineMinutes = sunshineMinutes,
                )
            }
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
            showEndOfShift = false,
            onBlockingEnabledChanged = {},
            onStartTimeChanged = {},
            onEndTimeChanged = {},
            onRequestPermission = {},
            onNavigateToAppSelection = {},
            onEndOfShiftShown = {},
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
            showEndOfShift = false,
            onBlockingEnabledChanged = {},
            onStartTimeChanged = {},
            onEndTimeChanged = {},
            onRequestPermission = {},
            onNavigateToAppSelection = {},
            onEndOfShiftShown = {},
        )
    }
}

