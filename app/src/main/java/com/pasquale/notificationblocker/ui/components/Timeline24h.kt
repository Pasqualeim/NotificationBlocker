package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

@Composable
fun Timeline24h(
    startTimeMinutes: Int,
    endTimeMinutes: Int,
    modifier: Modifier = Modifier,
    isBlockingEnabled: Boolean = true,
    currentTimeMinutes: Int? = null,
    barHeight: Dp = 16.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (isBlockingEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val indicatorColor = MaterialTheme.colorScheme.secondary
    val indicatorGlowColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)

    val liveMinutes by rememberCurrentMinutes()
    val currentMins = currentTimeMinutes ?: liveMinutes
    val isNowActive = isBlockingEnabled && OffHours.isWithin(currentMins, startTimeMinutes, endTimeMinutes)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + 12.dp) // extra vertical space for indicator overflow
        ) {
            val width = size.width
            val barH = barHeight.toPx()
            val yOffset = (size.height - barH) / 2f
            val cornerRadius = CornerRadius(barH / 2f, barH / 2f)

            // 1. Draw base track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, yOffset),
                size = Size(width, barH),
                cornerRadius = cornerRadius
            )

            // 2. Draw active quiet hours range
            val startFrac = (startTimeMinutes.coerceIn(0, 1440) / 1440f)
            val endFrac = (endTimeMinutes.coerceIn(0, 1440) / 1440f)

            if (startTimeMinutes == endTimeMinutes) {
                // Full 24-hour quiet period
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, yOffset),
                    size = Size(width, barH),
                    cornerRadius = cornerRadius
                )
            } else if (startFrac < endFrac) {
                val startX = startFrac * width
                val activeW = (endFrac - startFrac) * width
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(startX, yOffset),
                    size = Size(activeW, barH),
                    cornerRadius = cornerRadius
                )
            } else {
                // Overnight wrap: startFrac > endFrac
                // Interval 1: startFrac -> 1.0 (midnight)
                val startX1 = startFrac * width
                val w1 = (1f - startFrac) * width
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(startX1, yOffset),
                    size = Size(w1, barH),
                    cornerRadius = cornerRadius
                )

                // Interval 2: 0.0 -> endFrac
                val w2 = endFrac * width
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, yOffset),
                    size = Size(w2, barH),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Draw current time indicator dot
            val currentFrac = (currentMins.coerceIn(0, 1440) / 1440f)
            val dotX = currentFrac * width
            val dotY = size.height / 2f

            if (isNowActive) {
                drawCircle(
                    color = indicatorGlowColor,
                    radius = barH * 0.85f,
                    center = Offset(dotX, dotY)
                )
            }

            drawCircle(
                color = indicatorColor,
                radius = barH * 0.55f,
                center = Offset(dotX, dotY)
            )

            drawCircle(
                color = Color.White,
                radius = barH * 0.25f,
                center = Offset(dotX, dotY)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Time axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelStyle = MaterialTheme.typography.labelSmall
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            Text(text = "00:00", style = labelStyle, color = labelColor)
            Text(text = "06:00", style = labelStyle, color = labelColor)
            Text(text = "12:00", style = labelStyle, color = labelColor)
            Text(text = "18:00", style = labelStyle, color = labelColor)
            Text(text = "24:00", style = labelStyle, color = labelColor)
        }
    }
}

@Preview(showBackground = true, name = "Timeline - Standard Day Range")
@Composable
fun Timeline24hPreviewStandard() {
    NotificationBlockerTheme {
        Timeline24h(
            startTimeMinutes = 8 * 60,  // 08:00
            endTimeMinutes = 18 * 60, // 18:00
            isBlockingEnabled = true,
            currentTimeMinutes = 12 * 60,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Timeline - Overnight Range")
@Composable
fun Timeline24hPreviewOvernight() {
    NotificationBlockerTheme {
        Timeline24h(
            startTimeMinutes = 22 * 60, // 22:00
            endTimeMinutes = 7 * 60,   // 07:00
            isBlockingEnabled = true,
            currentTimeMinutes = 23 * 60,
            modifier = Modifier.padding(16.dp)
        )
    }
}
