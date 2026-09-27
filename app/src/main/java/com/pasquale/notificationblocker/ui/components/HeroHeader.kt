package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.LifeMessage

enum class HeroStatus {
    DISABLED,
    ACTIVE_OUTSIDE,
    ACTIVE_INSIDE
}

@Composable
fun HeroHeader(
    isBlockingEnabled: Boolean,
    isInOffHoursNow: Boolean,
    onBlockingEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lifeMessage: LifeMessage? = null,
) {
    val status = when {
        !isBlockingEnabled -> HeroStatus.DISABLED
        isInOffHoursNow -> HeroStatus.ACTIVE_INSIDE
        else -> HeroStatus.ACTIVE_OUTSIDE
    }

    HeroHeaderContent(
        status = status,
        isBlockingEnabled = isBlockingEnabled,
        onBlockingEnabledChanged = onBlockingEnabledChanged,
        lifeMessage = lifeMessage,
        modifier = modifier,
    )
}

@Composable
private fun HeroHeaderContent(
    status: HeroStatus,
    isBlockingEnabled: Boolean,
    onBlockingEnabledChanged: (Boolean) -> Unit,
    lifeMessage: LifeMessage?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val animatedContainerColor by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.surfaceContainerHigh
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.secondaryContainer
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(durationMillis = 400),
        label = "HeroContainerColor",
    )

    val animatedContentColor by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.onSurface
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.onSecondaryContainer
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(durationMillis = 400),
        label = "HeroContentColor",
    )

    // Warm breathing light / gentle sunlight animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "HeroGlowTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HeroGlowScale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HeroGlowAlpha",
    )

    val animatedIconBackground by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        },
        animationSpec = tween(durationMillis = 400),
        label = "HeroIconBackground",
    )

    val animatedIconTint by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.secondary
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 400),
        label = "HeroIconTint",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val greetingRes = rememberWarmGreetingRes()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            animatedContainerColor,
                            when (status) {
                                HeroStatus.DISABLED -> animatedContainerColor
                                HeroStatus.ACTIVE_OUTSIDE -> secondaryColor.copy(alpha = 0.12f)
                                HeroStatus.ACTIVE_INSIDE -> primaryColor.copy(alpha = 0.18f)
                            }
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tactile icon container with breathing sunlight glow in active state
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .drawBehind {
                                if (status == HeroStatus.ACTIVE_INSIDE) {
                                    val radius = (size.minDimension / 2f) * glowScale
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = glowAlpha),
                                                primaryColor.copy(alpha = glowAlpha * 0.35f),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = center
                                    )
                                }
                            }
                            .background(color = animatedIconBackground, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MutedBellIcon(
                            muted = isBlockingEnabled,
                            tint = animatedIconTint,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + slideInVertically { height -> height / 3 }) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        modifier = Modifier.weight(1f),
                        label = "HeroStatusTransition",
                    ) { targetStatus ->
                        val (titleText, subtitleRes) = when (targetStatus) {
                            HeroStatus.DISABLED -> stringResource(R.string.status_disabled) to R.string.hero_subtitle_disabled
                            HeroStatus.ACTIVE_OUTSIDE -> stringResource(R.string.status_active_outside) to R.string.hero_subtitle_outside
                            HeroStatus.ACTIVE_INSIDE -> stringResource(greetingRes) to R.string.hero_subtitle_inside
                        }

                        Column {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = animatedContentColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(subtitleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = animatedContentColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Daylight & free time left message
                if (lifeMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedContent(
                        targetState = lifeMessage,
                        transitionSpec = { fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(200)) },
                        label = "HeroLifeMessage",
                    ) { message ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(if (message.isSunny()) R.drawable.ic_sun_dim else R.drawable.ic_moon),
                                contentDescription = null,
                                tint = animatedIconTint,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = lifeMessageText(message),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = animatedContentColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prominent Master Switch inside surface card with haptic feedback
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.master_switch_label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    if (isBlockingEnabled) R.string.master_switch_status_on else R.string.master_switch_status_off
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val cdSwitch = stringResource(R.string.cd_master_switch)
                        Switch(
                            checked = isBlockingEnabled,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBlockingEnabledChanged(checked)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = cdSwitch
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberWarmGreetingRes(): Int {
    val minutes by rememberCurrentMinutes()
    return when (minutes / 60) {
        in 5..11 -> R.string.greeting_time_breathe
        in 12..17 -> R.string.greeting_enjoy_sun
        in 18..22 -> R.string.greeting_welcome_life
        else -> R.string.greeting_time_begins
    }
}

@Preview(showBackground = true, name = "Hero - Active Inside")
@Composable
fun HeroHeaderPreviewActiveInside() {
    NotificationBlockerTheme {
        HeroHeader(
            isBlockingEnabled = true,
            isInOffHoursNow = true,
            onBlockingEnabledChanged = {},
            modifier = Modifier.padding(16.dp),
            lifeMessage = LifeMessage.SunLeft(minutes = 200),
        )
    }
}

@Preview(showBackground = true, name = "Hero - Active Outside")
@Composable
fun HeroHeaderPreviewActiveOutside() {
    NotificationBlockerTheme {
        HeroHeader(
            isBlockingEnabled = true,
            isInOffHoursNow = false,
            onBlockingEnabledChanged = {},
            modifier = Modifier.padding(16.dp),
            lifeMessage = LifeMessage.SunAfterWork(start = 17 * 60, minutes = 205),
        )
    }
}

@Preview(showBackground = true, name = "Hero - Disabled")
@Composable
fun HeroHeaderPreviewDisabled() {
    NotificationBlockerTheme {
        HeroHeader(
            isBlockingEnabled = false,
            isInOffHoursNow = false,
            onBlockingEnabledChanged = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
