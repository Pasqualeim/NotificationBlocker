package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pasquale.notificationblocker.ui.theme.Motion
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
        animationSpec = Motion.standard(Motion.LONG),
        label = "HeroContainerColor",
    )

    val animatedContentColor by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.onSurface
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.onSecondaryContainer
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = Motion.standard(Motion.LONG),
        label = "HeroContentColor",
    )

    val animatedIconBackground by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            // Opaque "paper" disc: the primary bell stays >= 3:1 even at the peak of the glow behind it
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        animationSpec = Motion.standard(Motion.LONG),
        label = "HeroIconBackground",
    )

    val animatedIconTint by animateColorAsState(
        targetValue = when (status) {
            HeroStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
            HeroStatus.ACTIVE_OUTSIDE -> MaterialTheme.colorScheme.secondary
            HeroStatus.ACTIVE_INSIDE -> MaterialTheme.colorScheme.primary
        },
        animationSpec = Motion.standard(Motion.LONG),
        label = "HeroIconTint",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val greetingRes = rememberWarmGreetingRes()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
                                // Kept faint: stronger tints drop the 0.85-alpha subtitle below 4.5:1 in dark
                                HeroStatus.ACTIVE_OUTSIDE -> secondaryColor.copy(alpha = 0.06f)
                                HeroStatus.ACTIVE_INSIDE -> primaryColor.copy(alpha = 0.06f)
                            }
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Bell, short status and the master switch on one line, as in the mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tactile icon container with breathing sunlight glow in active state
                    Box(contentAlignment = Alignment.Center) {
                        // Composed only while holding notifications, so the loop stops otherwise.
                        // The bell below keeps its state: the `if` group doesn't shift it
                        if (status == HeroStatus.ACTIVE_INSIDE) {
                            BreathingGlow(
                                color = primaryColor,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(color = animatedIconBackground, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MutedBellIcon(
                                muted = isBlockingEnabled,
                                tint = animatedIconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            val enter = fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) +
                                slideInVertically(animationSpec = Motion.standard(Motion.MEDIUM)) { height -> height / 3 }
                            enter togetherWith fadeOut(animationSpec = Motion.standard(Motion.SHORT))
                        },
                        modifier = Modifier.weight(1f),
                        label = "HeroStatusTitle",
                    ) { targetStatus ->
                        Text(
                            text = stringResource(
                                when (targetStatus) {
                                    HeroStatus.DISABLED -> R.string.status_disabled
                                    HeroStatus.ACTIVE_OUTSIDE -> R.string.status_standby
                                    HeroStatus.ACTIVE_INSIDE -> R.string.status_active
                                }
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedContentColor,
                            maxLines = 1,
                        )
                    }

                    val cdSwitch = stringResource(R.string.cd_master_switch)
                    Switch(
                        checked = isBlockingEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBlockingEnabledChanged(checked)
                        },
                        thumbContent = if (isBlockingEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.semantics {
                            contentDescription = cdSwitch
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // What happens now, and a warm line while holding notifications
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        fadeIn(animationSpec = Motion.standard(Motion.MEDIUM)) togetherWith
                            fadeOut(animationSpec = Motion.standard(Motion.SHORT))
                    },
                    label = "HeroStatusSubtitle",
                ) { targetStatus ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(
                                when (targetStatus) {
                                    HeroStatus.DISABLED -> R.string.hero_subtitle_disabled
                                    HeroStatus.ACTIVE_OUTSIDE -> R.string.hero_subtitle_outside
                                    HeroStatus.ACTIVE_INSIDE -> R.string.hero_subtitle_inside
                                }
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = animatedContentColor,
                        )
                        if (targetStatus == HeroStatus.ACTIVE_INSIDE) {
                            Text(
                                text = stringResource(greetingRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = animatedContentColor,
                            )
                        }
                    }
                }

                // Daylight & free time left message
                if (lifeMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedContent(
                        targetState = lifeMessage,
                        transitionSpec = {
                            fadeIn(animationSpec = Motion.standard(Motion.LONG)) togetherWith
                                fadeOut(animationSpec = Motion.standard(Motion.SHORT))
                        },
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
            }
        }
    }
}

// Warm breathing light / gentle sunlight behind the bell, drawn past its bounds (no clipping)
@Composable
private fun BreathingGlow(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeroGlowTransition")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = Motion.standard(Motion.AMBIENT),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HeroGlowScale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = Motion.standard(Motion.AMBIENT),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "HeroGlowAlpha",
    )

    Canvas(modifier = modifier) {
        val radius = (size.minDimension / 2f) * glowScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = glowAlpha),
                    color.copy(alpha = glowAlpha * 0.35f),
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
