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
import androidx.compose.material.icons.filled.DateRange
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
            // Title & Human language calculation badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.schedule_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Natural human language calculation badge ("15 ore dedicate a te")
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = dedicatedText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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

            // 24h Timeline bar
            Timeline24h(
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                isBlockingEnabled = isBlockingEnabled,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Side-by-side Time Pills ("Dalle 17:00" -> "Alle 08:00")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cdStart = stringResource(R.string.cd_start_time_picker)
                TimePill(
                    timeText = stringResource(R.string.schedule_from, OffHours.format(startTimeMinutes)),
                    subText = stringResource(R.string.schedule_start_time),
                    onClick = onStartTimeClick,
                    accessibilityLabel = cdStart,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                val cdEnd = stringResource(R.string.cd_end_time_picker)
                TimePill(
                    timeText = stringResource(R.string.schedule_to, OffHours.format(endTimeMinutes)),
                    subText = stringResource(R.string.schedule_end_time),
                    onClick = onEndTimeClick,
                    accessibilityLabel = cdEnd,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TimePill(
    timeText: String,
    subText: String,
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = TimeDisplayTextStyle.copy(fontSize = 20.sp, lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
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
