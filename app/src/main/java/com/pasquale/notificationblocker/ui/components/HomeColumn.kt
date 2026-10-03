package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlin.math.min

private const val FLEXIBLE_ID = "flexible"

/** Marks the child of [HomeColumn] that gets the height the others leave free. */
fun Modifier.homeFlexible(): Modifier = layoutId(FLEXIBLE_ID)

/**
 * Column that makes Home fit one screen. Every child keeps its natural height except the one marked
 * with [homeFlexible]: it takes what is left of the viewport, never more than [flexibleMaxAspectRatio]
 * (width / height) allows and never less than [flexibleMinHeight]. Only when even the minimum does not
 * fit (huge font, tiny window) the content is taller than the viewport and the parent scrolls.
 *
 * The viewport is the minimum height the parent passes in (`fillMaxSize()` before `verticalScroll`).
 * Zero-height children add no [spacing], and it grows with their height, so cards that expand in
 * do not make the page jump.
 */
@Composable
fun HomeColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    flexibleMinHeight: Dp = 120.dp,
    flexibleMaxAspectRatio: Float = 2f,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val natural = constraints.copy(minWidth = 0, minHeight = 0)
        val flexibleIndex = measurables.indexOfFirst { it.layoutId == FLEXIBLE_ID }

        val placeables = measurables.mapIndexed { index, measurable ->
            if (index == flexibleIndex) null else measurable.measure(natural)
        }
        fun gapAfter(index: Int): Int = placeables[index]?.let { min(it.height, gap) } ?: gap

        val used = placeables.sumOf { it?.height ?: 0 } + (0 until measurables.lastIndex).sumOf(::gapAfter)
        val maxFlexible = (width / flexibleMaxAspectRatio).toInt()
        val flexible = if (constraints.minHeight > 0) {
            (constraints.minHeight - used).coerceIn(flexibleMinHeight.roundToPx().coerceAtMost(maxFlexible), maxFlexible)
        } else {
            maxFlexible
        }

        layout(width, used + if (flexibleIndex >= 0) flexible else 0) {
            var y = 0
            measurables.forEachIndexed { index, measurable ->
                val placeable = placeables[index] ?: measurable.measure(Constraints.fixed(width, flexible))
                placeable.placeRelative(0, y)
                // The flexible slot is placed at the height computed above, whatever its content reports
                val height = if (index == flexibleIndex) flexible else placeable.height
                y += height + if (index < measurables.lastIndex) gapAfter(index) else 0
            }
        }
    }
}

@Composable
private fun HomeColumnSample(modifier: Modifier = Modifier) {
    NotificationBlockerTheme {
        HomeColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.homeFlexible().fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer))
            Box(Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.secondaryContainer))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "HomeColumn - tall screen, flexible capped at 2:1")
@Composable
fun HomeColumnTallPreview() = HomeColumnSample()

@Preview(showBackground = true, widthDp = 360, heightDp = 420, name = "HomeColumn - short screen, flexible shrinks")
@Composable
fun HomeColumnShortPreview() = HomeColumnSample()
