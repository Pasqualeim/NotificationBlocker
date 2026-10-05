package com.pasquale.notificationblocker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlin.coroutines.coroutineContext

/**
 * The one clock of the slow motion on Home: the scene and the glow behind the bell both read it.
 * [seconds] only moves while the clock is active, and at most [FRAME_RATE] times a second.
 *
 * Two animations of their own would each redraw the window on every vsync: on a 90 Hz phone with a
 * weak GPU (Galaxy A32, ~17 ms to draw Home) the glow alone kept it at 90 draws a second, all late,
 * and everything else on the page stuttered. On one clock they share the same frames.
 */
@Stable
class AmbientClock {
    var seconds by mutableFloatStateOf(0f)
        internal set
}

/**
 * @param active false freezes the clock on its current instant (the page scrolls, another screen
 *   slides over Home); it resumes from the same instant, without a jump. Frozen too with
 *   "Remove animations".
 */
@Composable
fun rememberAmbientClock(active: Boolean = true): AmbientClock {
    val clock = remember { AmbientClock() }
    LaunchedEffect(active) {
        if (!active || coroutineContext[MotionDurationScale]?.scaleFactor == 0f) return@LaunchedEffect
        // The clock only runs while active: start from the frozen instant
        val start = withFrameNanos { it } - (clock.seconds * 1e9f).toLong()
        var lastFrame = start
        while (true) {
            withFrameNanos { now ->
                // Vsync timestamps jitter: without the slack the frame due on the 3rd vsync at 90 Hz (2nd at
                // 60 Hz) often slipped to the next one, 22.5 fps in uneven steps instead of a steady 30
                if (now - lastFrame >= FRAME_NANOS - FRAME_SLACK_NANOS) {
                    lastFrame = now
                    clock.seconds = (now - start) / 1e9f
                }
            }
        }
    }
    return clock
}

private const val FRAME_RATE = 30
private const val FRAME_NANOS = 1_000_000_000L / FRAME_RATE
private const val FRAME_SLACK_NANOS = 2_000_000L
