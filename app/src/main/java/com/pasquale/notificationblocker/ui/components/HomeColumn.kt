package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlin.math.min
import kotlin.math.roundToInt

private const val SCENE_ID = "scene"

/** Marks the child of [HomeColumn] that is the scene: it is never cropped, see [HomeFit]. */
fun Modifier.homeScene(): Modifier = layoutId(SCENE_ID)

/**
 * Column that keeps Home on one screen, whatever the phone (rule in docs/DESIGN_SYSTEM.md, "Home in
 * una schermata"; the policy itself is [HomeFit]). Children are stacked top to bottom with [spacing].
 * The child marked [homeScene] is given the full width at [sceneAspectRatio] (width / height) and is
 * never cropped; when the page is taller than the viewport the other children are scaled down together
 * (laid out wider, drawn smaller, so text and cards keep their proportions), the scene only after
 * them. When even that does not fit, the page is taller than the viewport and the parent scrolls.
 *
 * The viewport is the minimum height the parent passes in (`fillMaxSize()` before `verticalScroll`).
 * Zero-height children add no [spacing], and it grows with their height, so cards that expand in
 * do not make the page jump. Children must be composed from the first frame (animate their alpha
 * and offset, do not add them later): the scale depends on the height of all of them.
 */
@Composable
fun HomeColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    sceneAspectRatio: Float = 2f,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val sceneIndex = measurables.indexOfFirst { it.layoutId == SCENE_ID }
        val lastIndex = measurables.lastIndex
        val sceneNatural = if (sceneIndex >= 0) (width / sceneAspectRatio).roundToInt() else 0

        // Compose allows one measure() per child, so the search for the scale asks the cards for their
        // intrinsic height at the wider width they would have (scale times smaller when drawn), and only
        // the chosen scale is really measured
        fun wideWidth(scale: Float) = (width / scale).roundToInt()

        fun gapAfter(height: Int?): Int = height?.let { min(it, gap) } ?: gap

        fun restHeight(scale: Float): Int {
            val heights = measurables.mapIndexed { index, measurable ->
                if (index == sceneIndex) null else measurable.minIntrinsicHeight(wideWidth(scale))
            }
            val unscaled = heights.sumOf { it ?: 0 } + (0 until lastIndex).sumOf { gapAfter(heights[it]) }
            return (unscaled * scale).roundToInt()
        }

        val plan = HomeFit.plan(constraints.minHeight, sceneNatural, ::restHeight)
        val scale = plan.restScale
        val wide = Constraints(maxWidth = wideWidth(scale))
        val rest = measurables.mapIndexed { index, measurable ->
            if (index == sceneIndex) null else measurable.measure(wide)
        }
        // The search above relies on intrinsic heights, a little prudent; once the cards are really
        // measured at their floor, the scene takes exactly what is left
        val restActual = (
            rest.sumOf { it?.height ?: 0 } + (0 until lastIndex).sumOf { gapAfter(rest[it]?.height) }
            ) * scale
        val sceneScale = if (scale <= HomeFit.REST_MIN_SCALE && constraints.minHeight > 0) {
            HomeFit.sceneScale(constraints.minHeight - restActual.roundToInt(), sceneNatural)
        } else {
            plan.sceneScale
        }
        val sceneWidth = (width * sceneScale).roundToInt()
        val sceneHeight = (sceneNatural * sceneScale).roundToInt()
        val scene = measurables.getOrNull(sceneIndex)?.measure(Constraints.fixed(sceneWidth, sceneHeight))

        var y = 0f
        val ys = measurables.indices.map { index ->
            val top = y.roundToInt()
            val height = if (index == sceneIndex) sceneHeight.toFloat() else rest[index]!!.height * scale
            val gapBelow = if (index < lastIndex) gapAfter(rest[index]?.height) * scale else 0f
            y += height + gapBelow
            top
        }

        layout(width, y.roundToInt()) {
            measurables.indices.forEach { index ->
                if (index == sceneIndex) {
                    scene!!.placeRelative((width - sceneWidth) / 2, ys[index])
                } else if (scale == 1f) {
                    rest[index]!!.placeRelative(0, ys[index])
                } else {
                    rest[index]!!.placeRelativeWithLayer(0, ys[index]) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeColumnSample(modifier: Modifier = Modifier) {
    NotificationBlockerTheme {
        HomeColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.homeScene().fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer))
            Box(Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.secondaryContainer))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "HomeColumn - tall screen, natural size")
@Composable
fun HomeColumnTallPreview() = HomeColumnSample()

@Preview(showBackground = true, widthDp = 360, heightDp = 420, name = "HomeColumn - short screen, cards then scene shrink")
@Composable
fun HomeColumnShortPreview() = HomeColumnSample()
