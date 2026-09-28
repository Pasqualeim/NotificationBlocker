package com.pasquale.notificationblocker.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Motion tokens (docs/MOTION.md). Every duration, easing and press spring in the UI comes from
 * here, never inline numbers. Choreography that belongs to a single component (e.g. the bell's
 * shake steps) may keep a named private constant, listed in the MOTION.md catalog.
 */
object Motion {
    /** Press feedback. */
    const val INSTANT = 100

    /** Small elements: colors and borders (AppItemRow), fade-outs. */
    const val SHORT = 200

    /** Content changes (AnimatedContent), entrances, the bell slash. */
    const val MEDIUM = 300

    /** Color changes of large surfaces (HeroHeader). */
    const val LONG = 400

    /** Slow, calm transitions: scene crossfade, shimmer sweep. */
    const val SLOW = 1200

    /** Half cycle of ambient loops (breathing glow) and "hold" pauses. */
    const val AMBIENT = 2000

    /** Delay between items of a staggered entrance. */
    val Stagger: Duration = 80.milliseconds

    /** Easing of every tween unless a component documents otherwise. */
    val StandardEasing: Easing = FastOutSlowInEasing

    /** Scale of a pressed CTA or pill. */
    const val PRESS_SCALE = 0.97f

    /** Tween with the standard easing. */
    fun <T> standard(durationMillis: Int = MEDIUM, delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = StandardEasing)

    /** Soft, slightly bouncy spring for press feedback (CTA, time pills). */
    fun <T> press(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
}
