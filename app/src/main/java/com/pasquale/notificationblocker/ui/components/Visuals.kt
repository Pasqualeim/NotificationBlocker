package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlinx.coroutines.delay

/** The icon of a pause, from when it starts: morning sun, midday sun, evening lantern, night moon. */
@DrawableRes
fun pauseIcon(pause: Pause): Int = when {
    pause.isWholeDay -> R.drawable.ic_tile_sun
    pause.start in 5 * 60 until 11 * 60 -> R.drawable.ic_sun_dim
    pause.start in 11 * 60 until 17 * 60 -> R.drawable.ic_tile_sun
    pause.start in 17 * 60 until 21 * 60 -> R.drawable.ic_tile_lantern
    else -> R.drawable.ic_moon
}

/** Container and content color of a pause's avatar, matching [pauseColor] at the same index. */
@Composable
fun pauseContainerColors(index: Int): Pair<Color, Color> = when (index % 3) {
    0 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    1 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
}

/**
 * The Material 3 Expressive shape of an avatar or icon badge, one per [index] like [pauseContainerColors]:
 * the cookie, clover and burst shapes of the system apps, so the pauses of a list read apart at a glance.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun expressiveShape(index: Int): Shape = when (index % 4) {
    0 -> MaterialShapes.Cookie9Sided
    1 -> MaterialShapes.Clover4Leaf
    2 -> MaterialShapes.Cookie6Sided
    else -> MaterialShapes.SoftBurst
}.toShape()

/**
 * Colors of a segment in Nook's segmented lists: the segments sit on [androidx.compose.material3.ColorScheme.surfaceContainer]
 * over the page's surface, as in the system settings (the default segment color is the page's own).
 */
@Composable
fun segmentColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/** Avatar of a pause: its time-of-day icon on its tinted, shaped container; muted when [enabled] is false. */
@Composable
fun PauseAvatar(pause: Pause, index: Int, modifier: Modifier = Modifier, size: Dp = 40.dp, enabled: Boolean = true) {
    val (container, content) = pauseContainerColors(index)
    Box(
        modifier = modifier
            .size(size)
            .background(if (enabled) container else MaterialTheme.colorScheme.surfaceContainerHighest, expressiveShape(index)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(pauseIcon(pause)),
            contentDescription = null,
            tint = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * Entrance of a block in a list or page: it fades in and rises a little, [index] steps of
 * [Motion.Stagger] after the first. Once, when the block is first composed; never again.
 */
@Composable
fun Modifier.entrance(index: Int = 0): Modifier {
    // Saved, so a block that scrolls out of a lazy list and back does not enter again
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        delay(Motion.Stagger * index)
        progress.animateTo(1f, Motion.standard(Motion.MEDIUM))
        played = true
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * ENTRANCE_RISE_DP * density
    }
}

// How far a block rises while it enters
private const val ENTRANCE_RISE_DP = 24f

/**
 * A number that counts up to [target] the first time it shows (over [COUNT_UP_MILLIS]); later values,
 * like the minutes of today ticking on, are shown at once. [format] turns the value into text.
 */
@Composable
fun CountUpText(
    target: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    format: @Composable (Int) -> String = { it.toString() },
) {
    var played by rememberSaveable { mutableStateOf(false) }
    val value = remember { Animatable(if (played) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        if (played) {
            value.snapTo(target.toFloat())
        } else {
            value.animateTo(target.toFloat(), Motion.standard(COUNT_UP_MILLIS))
            played = true
        }
    }
    // Read out as the final figure, not the one mid-count
    val finalText = format(target)
    Text(
        text = format(value.value.toInt()),
        style = style,
        color = color,
        modifier = modifier.semantics { contentDescription = finalText },
        maxLines = 1,
    )
}

// Long enough to read as counting, short enough not to wait for it
private const val COUNT_UP_MILLIS = 700

/**
 * A small figure with an icon: the icon on its [container] in an expressive shape ([shapeIndex], see
 * [expressiveShape]), the value, a label under it.
 */
@Composable
fun MetricTile(
    icon: Painter,
    tint: Color,
    value: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    container: Color = tint.copy(alpha = 0.16f),
    shapeIndex: Int = 0,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(container, expressiveShape(shapeIndex)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            value()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, name = "Pause avatars and a metric")
@Composable
fun VisualsPreview() {
    NotificationBlockerTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PauseAvatar(Pause("lunch", "Pranzo", 13 * 60, 14 * 60), 0)
            PauseAvatar(Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30), 1)
            PauseAvatar(Pause("night", "Notte", 22 * 60, 7 * 60), 2, enabled = false)
            MetricTile(
                icon = painterResource(R.drawable.ic_tile_sun),
                tint = MaterialTheme.colorScheme.secondary,
                value = { Text("8 h 30", style = MaterialTheme.typography.titleLarge) },
                label = "Free today",
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Pause avatars (dark)")
@Composable
fun VisualsDarkPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PauseAvatar(Pause("lunch", "Pranzo", 13 * 60, 14 * 60), 0)
            PauseAvatar(Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30), 1)
            PauseAvatar(Pause("weekend", "Weekend", 0, 0), 2)
        }
    }
}
