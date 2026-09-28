package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

private val ShakeAngles = listOf(-24f, 20f, -16f, 12f, -7f, 3f, 0f)

// Bell-only choreography: each shake step is quicker than any Motion token, so it reads as a ring
private const val SHAKE_STEP_MILLIS = 70

/**
 * Bell icon that rings (shakes) and then gets crossed out when [muted] becomes true.
 * The slash retracts when [muted] goes back to false. No shake on first composition.
 */
@Composable
fun MutedBellIcon(
    muted: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    var previousMuted by remember { mutableStateOf(muted) }

    LaunchedEffect(muted) {
        if (muted && !previousMuted) {
            ShakeAngles.forEach { angle ->
                rotation.animateTo(angle, Motion.standard(SHAKE_STEP_MILLIS))
            }
        }
        previousMuted = muted
    }

    val slashProgress by animateFloatAsState(
        targetValue = if (muted) 1f else 0f,
        animationSpec = Motion.standard(
            durationMillis = Motion.MEDIUM,
            delayMillis = if (muted) ShakeAngles.size * SHAKE_STEP_MILLIS else 0,
        ),
        label = "BellSlashProgress",
    )

    Box(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = rotation.value
                    // Pivot near the top, like a real bell hanging from its hook
                    transformOrigin = TransformOrigin(0.5f, 0.1f)
                },
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            if (slashProgress > 0f) {
                val start = Offset(size.width * 0.12f, size.height * 0.12f)
                val end = Offset(
                    x = start.x + size.width * 0.76f * slashProgress,
                    y = start.y + size.height * 0.76f * slashProgress,
                )
                val stroke = size.minDimension * 0.09f
                // Cut a gap around the slash so it reads over the bell. Black is only a mask:
                // BlendMode.Clear keeps the stroke's shape and ignores its color
                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = stroke * 2.4f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Clear,
                )
                drawLine(
                    color = tint,
                    start = start,
                    end = end,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MutedBellIconPreview() {
    NotificationBlockerTheme {
        MutedBellIcon(
            muted = true,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
    }
}
