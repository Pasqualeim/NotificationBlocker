package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.theme.TimeDisplayTextStyle
import com.pasquale.notificationblocker.ui.zen.LifeCopy

@Composable
fun ScheduleCard(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBlockingEnabled: Boolean = true,
    sunshineMinutes: Int? = null,
) {
    val durationMinutes = calculateQuietDuration(startTimeMinutes, endTimeMinutes)
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60

    val durationText = if (minutes == 0) {
        stringResource(R.string.duration_hours_only, hours)
    } else {
        stringResource(R.string.duration_format, hours, minutes)
    }

    val dedicatedText = if (minutes == 0) {
        pluralStringResource(R.plurals.schedule_hours_dedicated, hours, hours)
    } else {
        stringResource(R.string.schedule_time_dedicated, durationText)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and the "time for you" badge on one line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text(
                        text = dedicatedText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            // Start -> end pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePill(
                    label = stringResource(R.string.schedule_start_time),
                    time = OffHours.format(startTimeMinutes),
                    onClick = onStartTimeClick,
                    accessibilityLabel = stringResource(R.string.cd_start_time_picker),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp),
                    )
                }
                TimePill(
                    label = stringResource(R.string.schedule_end_time),
                    time = OffHours.format(endTimeMinutes),
                    onClick = onEndTimeClick,
                    accessibilityLabel = stringResource(R.string.cd_end_time_picker),
                    modifier = Modifier.weight(1f)
                )
            }

            // 24h timeline with the "now" marker
            Timeline24h(
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                isBlockingEnabled = isBlockingEnabled,
            )

            // Daylight info if available
            if (sunshineMinutes != null) {
                val sunny = sunshineMinutes >= LifeCopy.MIN_SUN_MINUTES
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(if (sunny) R.drawable.ic_sun_dim else R.drawable.ic_moon),
                        contentDescription = null,
                        tint = if (sunny) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (sunny) {
                            stringResource(R.string.schedule_sunshine, durationText(sunshineMinutes))
                        } else {
                            stringResource(R.string.schedule_no_sunshine)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimePill(
    label: String,
    time: String,
    onClick: () -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Motion.PRESS_SCALE else 1f,
        animationSpec = Motion.press(),
        label = "TimePillScale",
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = accessibilityLabel,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = TimeDisplayTextStyle.copy(fontSize = 24.sp, lineHeight = 30.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun calculateQuietDuration(start: Int, end: Int): Int {
    return when {
        start == end -> 1440
        start < end -> end - start
        else -> (1440 - start) + end
    }
}

@Preview(showBackground = true, name = "Schedule Card Preview")
@Composable
fun ScheduleCardPreview() {
    NotificationBlockerTheme {
        ScheduleCard(
            startTimeMinutes = 17 * 60, // 17:00
            endTimeMinutes = 8 * 60,   // 08:00
            onStartTimeClick = {},
            onEndTimeClick = {},
            modifier = Modifier.padding(16.dp),
            sunshineMinutes = 95,
        )
    }
}
