package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.DeskPainting
import com.pasquale.notificationblocker.ui.zen.NaturePainting
import com.pasquale.notificationblocker.ui.zen.ScenePainting
import com.pasquale.notificationblocker.ui.zen.ZenEnvironment
import com.pasquale.notificationblocker.ui.zen.ZenPainter
import com.pasquale.notificationblocker.ui.zen.ZenState
import java.time.LocalDate
import java.time.LocalTime

/**
 * Animated scene at the bottom of Home, in the real light of the day (dawn, sun, sunset, night) and
 * season. Working: the view from the desk, a window on the city. Off work: a quiet Japanese
 * landscape. The two cross-fade slowly when the state changes.
 *
 * Vector layers drawn with Compose (crisp at any density). The still ones are rasterized once into
 * GPU textures; only the two moving layers redraw, capped at 30 fps. The clock is re-read every minute.
 * With "Remove animations" the scene is a still frame and the swap is instant.
 *
 * @param environment fixed light/season for previews; null follows the clock
 */
@Composable
fun ZenScene(
    isOffWork: Boolean,
    modifier: Modifier = Modifier,
    environment: ZenState? = null,
) {
    var time by remember { mutableFloatStateOf(0f) }
    // Light and season follow the minute tick, independent of the frame loop, so they stay
    // right also with "Remove animations" (when the loop never runs)
    val minutes by rememberCurrentMinutes()
    val clockEnvironment = remember(minutes) { ZenEnvironment.now() }

    LaunchedEffect(Unit) {
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) return@LaunchedEffect
        val start = withFrameNanos { it }
        var lastFrame = start
        while (true) {
            withFrameNanos { now ->
                if (now - lastFrame >= FRAME_NANOS) {
                    lastFrame = now
                    time = (now - start) / 1e9f
                }
            }
        }
    }

    val env = environment ?: clockEnvironment

    Crossfade(
        targetState = isOffWork,
        animationSpec = Motion.standard(Motion.SLOW),
        label = "ZenSceneCrossfade",
        modifier = modifier.fillMaxSize(),
    ) { offWork ->
        val painting = remember(env, offWork) { if (offWork) NaturePainting(env) else DeskPainting(env) }
        Box(modifier = Modifier.fillMaxSize()) {
            ZenLayer(cached = true) { painting.sky(this) }
            ZenLayer(cached = false) { painting.skyMotion(this, time) }
            ZenLayer(cached = true) { painting.landscape(this) }
            ZenLayer(cached = false) { painting.foreground(this, time) }
        }
    }
}

/**
 * One layer, re-recorded only when the state it reads (time, openness, environment) changes.
 * [cached] layers are also rasterized offscreen once, so the GPU composites a texture each frame
 * instead of replaying hundreds of shapes.
 */
@Composable
private fun BoxScope.ZenLayer(cached: Boolean, draw: ZenPainter.() -> Unit) {
    val painter = remember { ComposeZenPainter() }
    Spacer(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                compositingStrategy = if (cached) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawBehind { painter.paint(this, draw) },
    )
}

private const val FRAME_NANOS = 1_000_000_000L / 30

/** [ZenPainter] on a Compose [DrawScope], scaled from scene units to the layer size. */
private class ComposeZenPainter : ZenPainter {
    private lateinit var scope: DrawScope
    private val path = Path()

    fun paint(drawScope: DrawScope, draw: ZenPainter.() -> Unit) {
        scope = drawScope
        val scale = maxOf(drawScope.size.width / ScenePainting.W, drawScope.size.height / ScenePainting.H)
        val dx = (drawScope.size.width - ScenePainting.W * scale) / 2f
        val dy = (drawScope.size.height - ScenePainting.H * scale) / 2f
        drawScope.translate(dx, dy) {
            drawScope.scale(scale, pivot = Offset.Zero) { draw() }
        }
    }

    override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int) =
        scope.drawRect(Color(color), Offset(x, y), Size(w, h))

    override fun verticalGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int) =
        scope.drawRect(Brush.verticalGradient(listOf(Color(top), Color(bottom)), startY = y, endY = y + h), Offset(x, y), Size(w, h))

    override fun circle(cx: Float, cy: Float, r: Float, color: Int) =
        scope.drawCircle(Color(color), r, Offset(cx, cy))

    override fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) =
        scope.drawOval(Color(color), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))

    override fun glow(cx: Float, cy: Float, r: Float, color: Int) {
        val center = Offset(cx, cy)
        scope.drawCircle(Brush.radialGradient(listOf(Color(color), Color(color).copy(alpha = 0f)), center, r), r, center)
    }

    override fun polygon(points: FloatArray, count: Int, color: Int) {
        path.rewind()
        path.moveTo(points[0], points[1])
        for (i in 1 until count) path.lineTo(points[i * 2], points[i * 2 + 1])
        path.close()
        scope.drawPath(path, Color(color))
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) =
        scope.drawLine(Color(color), Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)
}

@Preview(showBackground = true, name = "Zen scene - Working (desk), spring morning")
@Composable
fun ZenScenePreviewWorking() {
    NotificationBlockerTheme {
        ZenScene(
            isOffWork = false,
            modifier = Modifier.padding(16.dp),
            environment = ZenEnvironment.resolve(LocalDate.of(2026, 4, 14), LocalTime.of(10, 0)),
        )
    }
}

@Preview(showBackground = true, name = "Zen scene - Off work (nature), summer 17:00")
@Composable
fun ZenScenePreviewSummer() {
    NotificationBlockerTheme {
        ZenScene(
            isOffWork = true,
            modifier = Modifier.padding(16.dp),
            environment = ZenEnvironment.resolve(LocalDate.of(2026, 7, 10), LocalTime.of(17, 0)),
        )
    }
}

@Preview(showBackground = true, name = "Zen scene - Off work (nature), winter night")
@Composable
fun ZenScenePreviewWinterNight() {
    NotificationBlockerTheme(darkTheme = true) {
        ZenScene(
            isOffWork = true,
            modifier = Modifier.padding(16.dp),
            environment = ZenEnvironment.resolve(LocalDate.of(2026, 1, 15), LocalTime.of(21, 0)),
        )
    }
}
