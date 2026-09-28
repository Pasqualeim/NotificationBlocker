package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.sin

/**
 * A scene in the light of [env], split into layers by what makes them change, so the app can
 * rasterize the still ones once on the GPU: [sky] and [landscape] change only with [env],
 * [skyMotion] (behind the landscape) and [foreground] (in front of it) with time in seconds.
 *
 * Coordinates are scene units, [W] x [H]. Every "daylight" color goes through [lit] (multiplied by the
 * ambient light); sky, water and light sources come from the time-of-day palette, so the same drawing
 * works at noon, at sunset and at night.
 */
abstract class ScenePainting(val env: ZenState) {

    protected val light = env.light
    protected val flora = env.flora
    protected val season = env.season
    protected val sun = env.celestial
    protected val rain = env.rain

    private val pts = FloatArray(512)
    private var n = 0

    abstract fun sky(p: ZenPainter)

    abstract fun skyMotion(p: ZenPainter, t: Float)

    abstract fun landscape(p: ZenPainter)

    abstract fun foreground(p: ZenPainter, t: Float)

    protected fun lit(color: Int) = ZenColor.multiply(color, light.ambient)

    protected fun a(color: Int, alpha: Float) = ZenColor.alpha(color, alpha)

    // Polygon builder on a reused buffer: no allocations per frame
    protected fun begin() {
        n = 0
    }

    protected fun pt(x: Float, y: Float) {
        pts[n * 2] = x
        pts[n * 2 + 1] = y
        n++
    }

    protected fun fill(p: ZenPainter, color: Int) {
        if (n > 2) p.polygon(pts, n, color)
    }

    /** Opaque sky from the top down to [bottom], in two gradient stops. */
    protected fun skyGradient(p: ZenPainter, bottom: Float) {
        val mid = bottom * 0.55f
        p.rect(0f, 0f, W, H, light.skyMid) // opaque base: no antialiased seams between the gradients
        p.verticalGradient(0f, 0f, W, mid, light.skyTop, light.skyMid)
        p.verticalGradient(0f, mid - 0.5f, W, bottom - mid + 1, light.skyMid, light.skyLow)
    }

    /** On rainy days a grey veil over the sky (and the sun or moon behind it), down to [bottom]. */
    protected fun overcast(p: ZenPainter, bottom: Float) {
        if (rain) p.rect(0f, 0f, W, bottom, a(lit(P.Overcast), OVERCAST_ALPHA))
    }

    protected fun sunOrMoon(p: ZenPainter, x: Float, y: Float) {
        if (rain) {
            // Only a pale disc behind the clouds
            p.glow(x, y, 24f, a(if (sun.isSun) P.Sun else P.Moon, 0.25f))
            p.circle(x, y, if (sun.isSun) 8f else 7f, a(if (sun.isSun) P.Sun else P.Moon, 0.3f))
            return
        }
        if (sun.isSun) {
            p.glow(x, y, 60f, a(P.SunGlow, 0.55f * (0.6f + 0.4f * light.rays)))
            p.glow(x, y, 20f, a(P.Sun, 0.8f))
            p.circle(x, y, 8f, ZenColor.mix(P.Sun, P.SunGlow, 1f - light.rays))
        } else {
            p.glow(x, y, 40f, a(P.Moon, 0.22f))
            p.circle(x, y, 7f, P.Moon)
            p.circle(x - 2f, y - 1.5f, 1.7f, a(P.MoonShade, 0.8f))
            p.circle(x + 2.6f, y + 2f, 1.2f, a(P.MoonShade, 0.8f))
            p.circle(x + 1f, y - 3.6f, 0.9f, a(P.MoonShade, 0.8f))
        }
    }

    protected fun stars(p: ZenPainter, t: Float, maxY: Int) {
        if (light.stars <= 0.05f || rain) return
        for (i in 0 until 44) {
            val twinkle = 0.55f + 0.45f * sin(t * 1.3f + i * 2.1f)
            val r = 0.5f + (hash(i, 6) % 3) * 0.3f
            p.circle((hash(i, 11) % W.toInt()).toFloat(), (hash(i, 5) % maxY).toFloat(), r, a(P.Star, light.stars * twinkle))
        }
    }

    /** Clouds drifting slowly to the left, [count] of them from height [top]. */
    protected fun clouds(p: ZenPainter, t: Float, top: Float, count: Int = 3) {
        for (i in 0 until if (rain) count + 2 else count) {
            val speed = 2.2f + i * 1.1f
            val x = W + 40 - ((t * speed + i * 137) % (W + 80))
            cloud(p, x, top + i * 11, 1f - i * 0.22f)
        }
    }

    private fun cloud(p: ZenPainter, x: Float, y: Float, s: Float) {
        val white = if (rain) P.RainCloud else P.Cloud
        val body = a(lit(white), 0.92f)
        p.ellipse(x, y + 4.5f * s, 22f * s, 4.5f * s, a(lit(if (rain) P.Overcast else P.CloudShade), 0.92f))
        p.circle(x - 10 * s, y + 1 * s, 7f * s, body)
        p.circle(x, y - 3 * s, 10f * s, body)
        p.circle(x + 11 * s, y + 0.5f * s, 7.5f * s, body)
        p.ellipse(x, y + 3 * s, 21f * s, 4f * s, body)
    }

    /** Two birds gliding across, only in daylight. */
    protected fun birds(p: ZenPainter, t: Float, top: Float) {
        if (light.rays <= 0.5f || rain) return
        val color = lit(P.Bird)
        for (i in 0 until 2) {
            val x = (t * 13 + i * 70) % (W + 40) - 20
            val y = top + i * 8 + sin(t * 0.8f + i) * 3
            val wing = sin(t * 8 + i * 1.3f) * 2.6f
            p.line(x - 4, y - wing, x, y + 1f, 0.9f, color)
            p.line(x, y + 1f, x + 4, y - wing, 0.9f, color)
        }
    }

    /**
     * One raindrop streak of a shower falling slightly to the left: drop [i] at time [t], inside
     * [top]..[bottom] and [left]..[right]. Nothing is drawn where [rainHidden] says so.
     */
    protected fun rainDrop(p: ZenPainter, t: Float, i: Int, left: Float, top: Float, right: Float, bottom: Float, alpha: Float) {
        val speed = 140f + hash(i, 101) % 60
        val span = bottom - top + RAIN_LEN
        val y = top + (t * speed + hash(i, 102)) % span - RAIN_LEN
        val x = left + hash(i, 103) % (right - left).toInt() - (y - top) * RAIN_SLANT
        if (x < left || rainHidden(x, y)) return
        p.line(x, maxOf(y, top), x - RAIN_LEN * RAIN_SLANT, minOf(y + RAIN_LEN, bottom), 0.6f, a(lit(P.Rain), alpha))
    }

    /** Where rain must not be drawn (e.g. objects in front of the window). */
    protected open fun rainHidden(x: Float, y: Float): Boolean = false

    /** Lance-shaped leaf from (x, y) toward [dx], [dy] (unit vector), [len] long. */
    protected fun leaf(p: ZenPainter, x: Float, y: Float, dx: Float, dy: Float, len: Float, width: Float, color: Int) {
        val mx = x + dx * len * 0.4f
        val my = y + dy * len * 0.4f
        begin()
        pt(x, y)
        pt(mx - dy * width, my + dx * width)
        pt(x + dx * len, y + dy * len)
        pt(mx + dy * width, my - dx * width)
        fill(p, color)
    }

    companion object {
        /** Scene units: the composable scales them to its size (2:1). */
        const val W = 320f
        const val H = 160f

        private const val OVERCAST_ALPHA = 0.35f
        const val RAIN_LEN = 7f
        const val RAIN_SLANT = 0.12f
    }
}
