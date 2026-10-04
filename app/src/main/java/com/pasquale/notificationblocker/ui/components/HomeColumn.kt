package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlinx.coroutines.delay
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
 * do not make the page jump. On a screen taller than the page the gaps grow, up to [maxSpacing], and
 * the page stays anchored at the top.
 *
 * Cost: a layout runs on every frame of a height animation (a status change in the hero card), so it
 * measures the cards once, at the current scale, exactly like a Column. A new scale relays out every
 * card (new width, new text layout), so it is not chased frame by frame: the layout only notes what it
 * wants, and the scale changes once the heights have been still for [SETTLE_MILLIS]; until then an
 * overflowing page is clipped at the bottom for a moment, never by shrinking the scene. Growing back
 * needs the natural height of the cards (one intrinsic look), done only then.
 */
@Composable
fun HomeColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    maxSpacing: Dp = 24.dp,
    sceneAspectRatio: Float = 2f,
    content: @Composable () -> Unit,
) {
    // Scale of the cards, read while measuring: a new value lays the page out again
    val scale = remember { mutableFloatStateOf(HomeScaleMemory.scale) }
    // What the last layout asked for: a smaller scale, a check for growing back, or nothing
    val wanted = remember { mutableFloatStateOf(NOTHING) }
    // Set once the heights are still and there was room: the next layout looks at the natural height
    val growCheck = remember { mutableStateOf(false) }

    val request = wanted.floatValue
    LaunchedEffect(request) {
        if (request == NOTHING) return@LaunchedEffect
        // Settle first, so an animation in progress is not chased; the very first fit is immediate (the
        // cards are still invisible, entering)
        if (HomeScaleMemory.fitted) delay(SETTLE_MILLIS)
        HomeScaleMemory.fitted = true
        if (request == GROW) {
            growCheck.value = true
        } else {
            scale.floatValue = request
            HomeScaleMemory.scale = request
        }
    }

    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val viewport = constraints.minHeight
        val sceneIndex = measurables.indexOfFirst { it.layoutId == SCENE_ID }
        val lastIndex = measurables.lastIndex
        val sceneNatural = if (sceneIndex >= 0) (width / sceneAspectRatio).roundToInt() else 0

        fun gapAfter(height: Int?): Int = height?.let { min(it, gap) } ?: gap

        fun total(heights: List<Int?>): Int =
            heights.sumOf { it ?: 0 } + (0 until lastIndex).sumOf { gapAfter(heights[it]) }

        val current = if (viewport > 0) scale.floatValue else 1f
        val wide = Constraints(maxWidth = (width / current).roundToInt())
        val rest = measurables.mapIndexed { index, measurable ->
            if (index == sceneIndex) null else measurable.measure(wide)
        }
        val unscaled = total(rest.map { it?.height })
        val restHeight = (unscaled * current).roundToInt()

        if (viewport > 0) {
            val room = viewport - restHeight - sceneNatural
            if (growCheck.value) {
                growCheck.value = false
                val natural = total(
                    measurables.mapIndexed { index, it -> if (index == sceneIndex) null else it.minIntrinsicHeight(width) },
                )
                val target = maxOf(current, HomeFit.restScale(viewport, sceneNatural, natural))
                if (target != current) {
                    scale.floatValue = target
                    HomeScaleMemory.scale = target
                }
            }
            wanted.floatValue = when {
                // Too tall: the same cards laid out wider only get shorter, so this scale fits
                room < 0 && current > HomeFit.REST_MIN_SCALE -> HomeFit.restScale(viewport, sceneNatural, unscaled)
                current < 1f && room > GROW_ROOM.roundToPx() -> GROW
                else -> NOTHING
            }
        }

        // The scene stays whole at 2:1; it shrinks (still whole) only once the cards are at their floor
        val sceneScale = if (viewport > 0 && current <= HomeFit.REST_MIN_SCALE) {
            HomeFit.sceneScale(viewport - restHeight, sceneNatural)
        } else {
            1f
        }
        val sceneWidth = (width * sceneScale).roundToInt()
        val sceneHeight = (sceneNatural * sceneScale).roundToInt()
        val scene = measurables.getOrNull(sceneIndex)?.measure(Constraints.fixed(sceneWidth, sceneHeight))

        // Room left on a tall screen goes into the gaps, up to maxSpacing each; the rest stays at the bottom
        val gaps = (0 until lastIndex).count { gapAfter(rest[it]?.height) > 0 }
        val spare = viewport - restHeight - sceneHeight
        val extraGap = if (viewport > 0 && spare > 0 && gaps > 0) {
            min(spare / gaps, (maxSpacing - spacing).roundToPx().coerceAtLeast(0)).toFloat()
        } else {
            0f
        }

        var y = 0f
        val ys = measurables.indices.map { index ->
            val top = y.roundToInt()
            val height = if (index == sceneIndex) sceneHeight.toFloat() else rest[index]!!.height * current
            val gapBelow = if (index < lastIndex) {
                val base = gapAfter(rest[index]?.height)
                base * current + if (base > 0) extraGap else 0f
            } else {
                0f
            }
            y += height + gapBelow
            top
        }

        // At least the viewport: a shorter page reported as is would be centered in it by the parent, and
        // would move up and down whenever the card at the top changes height; it stays at the top instead
        layout(width, maxOf(y.roundToInt(), constraints.minHeight)) {
            measurables.indices.forEach { index ->
                if (index == sceneIndex) {
                    scene!!.placeRelative((width - sceneWidth) / 2, ys[index])
                } else if (current == 1f) {
                    rest[index]!!.placeRelative(0, ys[index])
                } else {
                    rest[index]!!.placeRelativeWithLayer(0, ys[index]) {
                        scaleX = current
                        scaleY = current
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
        }
    }
}

/** The last scale, kept for the process: back from the app list, Home opens already fitted. */
private object HomeScaleMemory {
    var scale = 1f
    var fitted = false
}

private const val NOTHING = 0f
private const val GROW = -1f

// Heights still for this long = the animation is over; then the scale may change (one relayout)
private const val SETTLE_MILLIS = 200L

// Less room than this left under the cards is not worth a relayout to grow them back. Above the height
// difference between the hero states, so the master switch never rescales the page (a toggle-off frees
// about 30dp on a Galaxy A32): the page stays still and the gap is at the bottom
private val GROW_ROOM = 56.dp

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
