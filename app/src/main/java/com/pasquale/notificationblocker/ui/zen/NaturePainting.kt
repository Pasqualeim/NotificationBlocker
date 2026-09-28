package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * After work: a quiet Japanese landscape. A snow-capped mountain in the open center, the seasonal
 * tree pair, a stone lantern, a clear stream with stepping stones and a heron fishing, bamboo swaying
 * in the foreground. Spring petals, autumn leaves, winter snow; fireflies on summer nights; koi in
 * the stream. On rainy days a grey sky, rain and rings on the water.
 */
class NaturePainting(env: ZenState) : ScenePainting(env) {

    private val sunX = sun.x * W
    private val sunY = sun.y * H

    override fun sky(p: ZenPainter) {
        skyGradient(p, GROUND_Y)
        sunOrMoon(p, sunX, sunY)
        overcast(p, GROUND_Y)
        p.rect(0f, GROUND_Y, W, H - GROUND_Y, lit(flora.ground))
    }

    override fun skyMotion(p: ZenPainter, t: Float) {
        stars(p, t, maxY = 64)
        clouds(p, t, top = 14f)
        birds(p, t, top = 30f)
    }

    // Landscape -----------------------------------------------------------------------------------

    override fun landscape(p: ZenPainter) {
        mountain(p)
        ridge(p)
        forest(p)
        ground(p)
        tree(p, 82f, GROUND_Y + 8, TREE_1, main = true)
        tree(p, 254f, GROUND_Y + 9, TREE_2, main = false)
        lantern(p)
        frontBank(p)
    }

    private fun mountainY(x: Float): Float {
        val d = (abs(x - PEAK_X) / 110f).coerceAtMost(1f)
        return PEAK_Y + (GROUND_Y - PEAK_Y) * d.pow(1.6f) + if (d < 0.03f) 1f else 0f
    }

    private fun mountain(p: ZenPainter) {
        val color = ZenColor.mix(lit(ZenColor.mix(flora.hillFar, 0xFF4A5A7A.toInt(), 0.25f)), light.skyLow, 0.35f)
        begin()
        var x = PEAK_X - 110
        while (x <= PEAK_X + 110) {
            pt(x, mountainY(x))
            x += 4
        }
        fill(p, color)
        // Snow cap with a ragged lower edge (only a trace in summer)
        val cap = when (season) {
            Season.WINTER -> 24f
            Season.SUMMER -> 6f
            else -> 13f
        }
        begin()
        x = PEAK_X - 64
        while (x <= PEAK_X + 64) {
            val top = mountainY(x)
            if (top < PEAK_Y + cap) pt(x, top)
            x += 2
        }
        x = PEAK_X + 64
        var i = 0
        while (x >= PEAK_X - 64) {
            val top = mountainY(x)
            if (top < PEAK_Y + cap) pt(x, minOf(PEAK_Y + cap + if (i % 2 == 0) 2f else -1.5f, top + 6f))
            x -= 2
            i++
        }
        fill(p, ZenColor.mix(lit(P.Snow), light.skyLow, 0.2f))
    }

    private fun ridge(p: ZenPainter) {
        begin()
        pt(0f, GROUND_Y + 1)
        var x = 0f
        while (x <= W) {
            pt(x, 72f + 6 * sin(x / 46 + 1) + 4 * sin(x / 19))
            x += 4
        }
        pt(W, GROUND_Y + 1)
        fill(p, ZenColor.mix(lit(flora.hillFar), light.skyLow, 0.45f))
    }

    private fun forest(p: ZenPainter) {
        val color = ZenColor.mix(lit(flora.forest), light.skyLow, 0.15f)
        val top = ZenColor.mix(color, lit(if (season == Season.WINTER) P.SnowShade else flora.groundLight), 0.25f)
        p.rect(0f, 84f, W, GROUND_Y - 84f + 1, color)
        var x = -4f
        var i = 0
        while (x < W + 8) {
            val r = 5f + hash(i, 13) % 5
            val cy = 85f + 2.5f * sin(x / 13) - r * 0.4f
            p.circle(x, cy, r, color)
            p.ellipse(x - r * 0.2f, cy - r * 0.55f, r * 0.55f, r * 0.3f, top)
            x += 6f + hash(i, 14) % 4
            i++
        }
    }

    private fun streamTop(x: Float) = STREAM_TOP + 1.8f * sin(x / 26)
    private fun streamBottom(x: Float) = STREAM_BOTTOM + 2f * sin(x / 34 + 1)

    private fun ground(p: ZenPainter) {
        // Water first; ground and front bank cover its wavy edges
        p.verticalGradient(0f, STREAM_TOP - 3, W, STREAM_BOTTOM - STREAM_TOP + 6, ZenColor.mix(light.water, light.skyLow, 0.35f), light.waterDeep)
        begin()
        pt(0f, GROUND_Y)
        pt(W, GROUND_Y)
        var x = W
        while (x >= 0) {
            pt(x, streamTop(x))
            x -= 4
        }
        fill(p, lit(flora.ground))
        p.verticalGradient(0f, GROUND_Y, W, 6f, lit(flora.groundDark), a(lit(flora.ground), 0f))
        for (i in 0 until 46) {
            val sx = (hash(i, 31) % W.toInt()).toFloat()
            val sy = GROUND_Y + 5 + hash(i, 32) % 18
            p.ellipse(sx, sy, 2.5f, 0.8f, a(lit(if (i % 2 == 0) flora.groundDark else flora.groundLight), 0.7f))
        }
        // Bank edge along the water
        x = 0f
        while (x < W) {
            p.line(x, streamTop(x), x + 4, streamTop(x + 4), 1.4f, lit(flora.groundDark))
            x += 4
        }
        for ((sx, sy) in STONES) {
            p.ellipse(sx, sy + 1.2f, 7.5f, 2.6f, light.waterDeep)
            p.ellipse(sx, sy, 7f, 2.6f, lit(P.Stone))
            p.ellipse(sx - 1, sy - 1, 4.5f, 1.2f, lit(if (season == Season.WINTER) P.Snow else P.StoneLight))
        }
    }

    private fun tree(p: ZenPainter, trunkX: Float, baseY: Float, canopy: List<FloatArray>, main: Boolean) {
        val trunk = lit(P.Trunk)
        val top = canopy.minOf { it[1] }
        begin()
        pt(trunkX - 3.5f, baseY)
        pt(trunkX + 3.5f, baseY)
        pt(trunkX + 1.2f, top)
        pt(trunkX - 1.2f, top)
        fill(p, trunk)
        p.line(trunkX - 1.5f, baseY - 1, trunkX - 0.5f, top + 6, 0.9f, lit(P.TrunkLight))
        for ((i, c) in canopy.withIndex()) {
            p.line(trunkX, c[1] + c[2] * 0.7f, c[0], c[1], if (i == 0) 2.4f else 1.8f, trunk)
        }
        if (season == Season.WINTER) {
            // Bare twigs with snow resting on them
            for ((ci, c) in canopy.withIndex()) {
                val cx = c[0]
                val cy = c[1]
                val r = c[2]
                for (i in 0 until (r * 0.8f).toInt()) {
                    val ang = (hash(ci, i, 1) % 628) / 100f
                    val len = r * (0.4f + (hash(ci, i, 2) % 50) / 100f)
                    val ex = cx + cos(ang) * len
                    val ey = cy + sin(ang) * len * 0.8f
                    p.line(cx, cy, ex, ey, 1f, trunk)
                    p.ellipse(ex, ey - 0.8f, 2f, 0.9f, lit(P.Snow))
                }
                p.ellipse(cx, cy - 1.2f, 3f, 1.1f, lit(P.Snow))
            }
            return
        }
        val color = lit(if (main) flora.tree else flora.tree2)
        val light = lit(if (main) flora.treeLight else flora.tree2Light)
        val dark = lit(if (main) flora.treeDark else flora.tree2Dark)
        for (c in canopy) p.circle(c[0] + 1, c[1] + 2, c[2], dark)
        for (c in canopy) p.circle(c[0], c[1], c[2] - 0.5f, color)
        for (c in canopy) p.circle(c[0] - c[2] * 0.28f, c[1] - c[2] * 0.3f, c[2] * 0.55f, light)
        // Leaf / blossom texture
        for ((ci, c) in canopy.withIndex()) {
            val r = c[2]
            for (i in 0 until (r * 1.4f).toInt()) {
                val ang = (hash(ci, i, 3) % 628) / 100f
                val d = r * (hash(ci, i, 4) % 90) / 100f
                p.circle(c[0] + cos(ang) * d, c[1] + sin(ang) * d, 1.1f, if (i % 3 == 0) light else dark)
            }
        }
    }

    private fun lantern(p: ZenPainter) {
        val x = LANTERN_X
        val base = LANTERN_BASE
        val stone = lit(P.Stone)
        val dark = lit(P.StoneDark)
        begin()
        pt(x - 11, base)
        pt(x + 11, base)
        pt(x + 8, base - 5)
        pt(x - 8, base - 5)
        fill(p, stone)
        p.rect(x - 3, 86f, 6f, base - 5 - 86f, stone)
        p.rect(x + 1.5f, 86f, 1.5f, base - 5 - 86f, dark)
        p.rect(x - 9, 83f, 18f, 3f, dark)
        p.rect(x - 7, 72f, 14f, 11f, stone)
        p.rect(x - 7, 72f, 1.5f, 11f, lit(P.StoneLight))
        p.rect(x - 4, 74.5f, 8f, 6f, lit(P.LanternWindow))
        begin()
        pt(x - 16, 70.5f)
        pt(x - 13, 72.5f)
        pt(x + 13, 72.5f)
        pt(x + 16, 70.5f)
        pt(x + 9, 66f)
        pt(x + 2, 62f)
        pt(x - 2, 62f)
        pt(x - 9, 66f)
        fill(p, dark)
        p.circle(x, 60.5f, 2.2f, dark)
        if (season == Season.WINTER) {
            p.ellipse(x, 64.5f, 12f, 3.2f, lit(P.Snow))
            p.ellipse(x, 59.5f, 2.4f, 1.3f, lit(P.Snow))
            p.ellipse(x, 82.5f, 9f, 1.4f, lit(P.Snow))
        }
    }

    private fun frontBank(p: ZenPainter) {
        begin()
        var x = 0f
        while (x <= W) {
            pt(x, streamBottom(x))
            x += 4
        }
        pt(W, H)
        pt(0f, H)
        fill(p, lit(flora.ground))
        x = 0f
        while (x < W) {
            p.line(x, streamBottom(x) + 0.6f, x + 4, streamBottom(x + 4) + 0.6f, 1.2f, lit(flora.groundLight))
            x += 4
        }
        if (season == Season.WINTER) {
            for (i in 0 until 20) {
                val sx = (hash(i, 83) % W.toInt()).toFloat()
                p.ellipse(sx, streamBottom(sx) + 5 + hash(i, 84) % 16, 4f, 1f, lit(P.SnowShade))
            }
        }
        for ((sx, w) in BANK_STONES) {
            p.ellipse(sx, H - 4, w, 3.4f, lit(P.StoneDark))
            p.ellipse(sx - 1, H - 5.5f, w * 0.65f, 1.4f, lit(if (season == Season.WINTER) P.Snow else P.Stone))
        }
    }

    // Foreground ----------------------------------------------------------------------------------

    override fun foreground(p: ZenPainter, t: Float) {
        water(p, t)
        heron(p, t)
        grass(p, t)
        bamboo(p, t)
        rays(p, t)
        particles(p, t)
        if (rain) shower(p, t)
        lanternGlow(p, t)
        if (env.timeOfDay == TimeOfDay.DAWN) fog(p, t, DAWN_MIST)
    }

    private fun water(p: ZenPainter, t: Float) {
        koi(p, t)
        // Current flowing to the right, faster near the viewer
        for (i in 0 until 16) {
            val y = STREAM_TOP + 3 + hash(i, 1) % (STREAM_BOTTOM - STREAM_TOP - 5).toInt()
            val speed = 6f + (y - STREAM_TOP) * 0.9f
            val len = 4f + hash(i, 2) % 7
            val x = (t * speed + hash(i, 3)) % (W + 20) - 10
            p.line(x, y, x + len, y, 1f, a(light.waterLight, 0.65f))
        }
        if (sun.isSun && light.rays > 0.05f && !rain) {
            for (i in 0 until 16) {
                val flash = sin(t * 5 + i * 2.1f)
                if (flash < 0.55f) continue
                val gx = (hash(i, 41) % W.toInt()).toFloat()
                val gy = STREAM_TOP + 3 + hash(i, 42) % (STREAM_BOTTOM - STREAM_TOP - 5).toInt()
                p.circle(gx, gy, 0.9f, a(P.Cloud, (flash - 0.55f) * 2.2f * light.rays))
            }
        }
        // Reflection of the moon, or of a low sun
        if (!sun.isSun || sun.y > 0.4f) {
            val color = if (sun.isSun) P.SunGlow else ZenColor.mix(light.water, P.Moon, 0.75f)
            var k = 0
            var y = STREAM_TOP + 3
            while (y < STREAM_BOTTOM - 1) {
                val w = 13f - k * 1.6f
                val jitter = sin(t * 2.2f + k * 1.7f) * 1.5f
                p.line(sunX - w / 2 + jitter, y, sunX + w / 2 + jitter, y, 1.1f, a(color, 0.75f))
                y += 3
                k++
            }
        }
    }

    /** Two koi gliding downstream under the surface, their tails swinging. */
    private fun koi(p: ZenPainter, t: Float) {
        val alpha = 0.7f * (0.35f + 0.65f * light.rays)
        for (i in 0 until 2) {
            val x = (t * (5f + i * 2f) + i * 170) % (W + 40) - 20
            val y = STREAM_TOP + 7 + i * 6 + sin(t * 0.5f + i * 2f) * 1.5f
            val swing = sin(t * 5f + i) * 1.4f
            begin()
            pt(x - 3.5f, y)
            pt(x - 7f, y - 1.8f + swing)
            pt(x - 7f, y + 1.8f + swing)
            fill(p, a(lit(P.Koi), alpha))
            p.ellipse(x, y, 4.2f, 1.5f, a(lit(if (i == 0) P.Koi else P.KoiWhite), alpha))
            p.ellipse(x + 1f, y - 0.3f, 1.6f, 0.8f, a(lit(if (i == 0) P.KoiWhite else P.Koi), alpha))
        }
    }

    /** Rain over the whole scene and rings spreading on the stream. */
    private fun shower(p: ZenPainter, t: Float) {
        for (i in 0 until 110) rainDrop(p, t, i, 0f, 0f, W, H, 0.3f)
        for (i in 0 until 10) {
            val period = 1.2f + (hash(i, 121) % 10) / 10f
            val phase = ((t + hash(i, 122) % 100 / 10f) / period) % 1f
            val cycle = ((t + hash(i, 122) % 100 / 10f) / period).toInt()
            val x = (hash(i, cycle, 123) % W.toInt()).toFloat()
            val y = STREAM_TOP + 3 + hash(i, cycle, 124) % (STREAM_BOTTOM - STREAM_TOP - 5).toInt()
            val r = 1f + phase * 5f
            p.ellipse(x, y, r, r * 0.3f, a(light.waterLight, 0.6f * (1f - phase)))
            p.ellipse(x, y, r - 0.6f, (r - 0.6f) * 0.3f, a(light.waterDeep, 0.6f * (1f - phase)))
        }
    }

    /** A heron standing in the shallows; now and then it dips its head toward the water. */
    private fun heron(p: ZenPainter, t: Float) {
        val x = HERON_X
        val feet = STREAM_TOP + 5
        val cycle = t % HERON_CYCLE
        val dip = if (cycle < 2.4f) sin(cycle / 2.4f * Math.PI.toFloat()).pow(2) else 0f
        val white = lit(P.HeronWhite)
        val shade = lit(P.HeronShade)
        // Reflection
        p.ellipse(x, feet + 5, 7f, 2f, a(P.HeronWhite, 0.18f * (0.4f + 0.6f * light.rays + light.stars * 0.3f)))
        p.line(x - 1, feet - 8, x - 1.5f, feet, 0.7f, lit(P.HeronLeg))
        p.line(x + 1.5f, feet - 8, x + 2f, feet, 0.7f, lit(P.HeronLeg))
        p.ellipse(x, feet - 11, 7f, 3.6f, white)
        p.ellipse(x - 1, feet - 10, 5.5f, 2.2f, shade)
        begin()
        pt(x - 6, feet - 12)
        pt(x - 11, feet - 9)
        pt(x - 6, feet - 9.5f)
        fill(p, shade)
        val headX = x + 6 + dip * 5
        val headY = feet - 22 + dip * 10
        p.line(x + 4, feet - 13, x + 6.5f + dip * 2, feet - 18 + dip * 4, 1.8f, white)
        p.line(x + 6.5f + dip * 2, feet - 18 + dip * 4, headX, headY, 1.6f, white)
        p.circle(headX, headY, 1.9f, white)
        p.line(headX + 1.5f, headY + dip * 0.8f, headX + 6.5f, headY + 0.8f + dip * 3, 0.9f, lit(P.HeronBeak))
    }

    private fun grass(p: ZenPainter, t: Float) {
        if (season == Season.WINTER) return
        // Grass tufts leaning in the breeze
        for (i in 0 until 34) {
            val gx = (hash(i, 81) % W.toInt()).toFloat()
            val gy = streamBottom(gx) + 3 + hash(i, 82) % 18
            val lean = sin(t * 1.4f + gx * 0.08f) * 1.2f
            val color = lit(if (i % 2 == 0) flora.groundLight else flora.groundDark)
            for (b in -1..1) p.line(gx + b * 1.4f, gy, gx + b * 2.2f + lean, gy - 4f - abs(b), 0.8f, color)
        }
    }

    private fun bamboo(p: ZenPainter, t: Float) {
        val breeze = if (season == Season.SUMMER) 5f else 3.2f
        for ((i, stalk) in STALKS.withIndex()) {
            val baseX = stalk[0]
            val width = stalk[1]
            val phase = i * 0.9f
            val sway = sin(t * 0.9f + phase) * breeze + sin(t * 2.3f + phase) * 0.5f
            fun xAt(y: Float) = baseX + sway * ((H - y) / H).coerceAtLeast(0f).pow(1.6f)
            var y0 = H + 2
            var node = 0
            while (y0 > -8) {
                val y1 = y0 - SEGMENT
                p.line(xAt(y0), y0, xAt(y1), y1, width, lit(flora.bambooStalk))
                p.line(xAt(y0) - width * 0.22f, y0, xAt(y1) - width * 0.22f, y1, width * 0.28f, lit(flora.bambooLight))
                p.line(xAt(y1) - width * 0.6f, y1, xAt(y1) + width * 0.6f, y1, 1.3f, lit(flora.bambooNode))
                if (y1 < 96 && hash(i, node, 5) % 3 != 0) leaves(p, t, xAt(y1), y1, i, node)
                y0 = y1
                node++
            }
        }
    }

    private fun leaves(p: ZenPainter, t: Float, x: Float, y: Float, stalk: Int, node: Int) {
        val side = if ((stalk + node) % 2 == 0) 1 else -1
        for (i in 0 until 3) {
            val dir = if (i == 1) -side else side
            val angle = 0.35f + i * 0.28f + sin(t * 2.6f + stalk + node * 0.7f + i) * 0.07f
            val len = 11f + hash(stalk, node, i) % 5
            val mx = x + cos(angle) * dir * len * 0.4f
            val my = y + sin(angle) * len * 0.4f
            leaf(p, x, y, cos(angle) * dir, sin(angle), len, 1.3f, lit(if ((i + node) % 2 == 0) flora.bambooLeaf else flora.bambooLeafDark))
            if (season == Season.WINTER) p.ellipse(mx, my - 1.2f, 2.4f, 0.8f, lit(P.Snow))
        }
    }

    /** Slanted shafts of sunlight filtering through the bamboo, gently pulsing. */
    private fun rays(p: ZenPainter, t: Float) {
        if (!sun.isSun || light.rays < 0.05f || rain) return
        val strength = light.rays * when (season) {
            Season.SUMMER -> 0.2f
            Season.WINTER -> 0.09f
            else -> 0.14f
        }
        val slope = -(sunX - W / 2) / 180f
        val color = ZenColor.mix(P.SunGlow, P.Sun, 0.5f)
        val bottom = STREAM_BOTTOM
        for (i in 0 until 4) {
            val x0 = 24f + i * 82 + hash(i, 51) % 24
            val w = 9f + hash(i, 52) % 10
            val alpha = strength * (0.55f + 0.45f * sin(t * 0.35f + i * 1.7f))
            for (core in 0..1) {
                val half = if (core == 0) w else w * 0.45f
                begin()
                pt(x0 - half / 2, 0f)
                pt(x0 + half / 2, 0f)
                pt(x0 + slope * bottom + half * 0.8f, bottom)
                pt(x0 + slope * bottom - half * 0.8f, bottom)
                fill(p, a(color, alpha * 0.5f))
            }
        }
    }

    private fun particles(p: ZenPainter, t: Float) {
        when (season) {
            Season.SPRING -> falling(p, t, count = 26, period = 9f, drift = 70f, sway = 5f, size = 1.6f)
            Season.SUMMER -> {
                if (light.rays > 0.3f) falling(p, t, count = 6, period = 7f, drift = 110f, sway = 6f, size = 2.2f)
                fireflies(p, t)
            }
            Season.AUTUMN -> falling(p, t, count = 18, period = 8f, drift = 50f, sway = 8f, size = 2.4f)
            Season.WINTER -> falling(p, t, count = 54, period = 14f, drift = 24f, sway = 4f, size = 0f)
        }
    }

    /** Petals, leaves ([size] > 0, tumbling) or snowflakes ([size] 0): fall across the scene in the wind. */
    private fun falling(p: ZenPainter, t: Float, count: Int, period: Float, drift: Float, sway: Float, size: Float) {
        for (i in 0 until count) {
            val pd = period * (1f + (hash(i, 21) % 50) / 100f)
            val phase = ((t + (hash(i, 22) % 1000) / 10f) / pd) % 1f
            val x = hash(i, 23) % (W + drift).toInt() - phase * drift + sway * sin(t * 0.9f + i)
            val y = -4 + phase * (H + 8)
            val color = lit(if (i % 3 == 0) flora.particle2 else flora.particle)
            if (size == 0f) {
                p.circle(x, y, 0.7f + (hash(i, 24) % 3) * 0.35f, a(color, 0.92f))
                continue
            }
            // Tumbling: the leaf turns, so its apparent width oscillates
            val spin = t * (1.5f + (hash(i, 25) % 10) / 10f) + i
            val w = size * (0.35f + 0.65f * abs(cos(spin)))
            val cx = cos(spin * 0.5f)
            val sy = sin(spin * 0.5f)
            begin()
            pt(x + cx * size, y + sy * size)
            pt(x - sy * w * 0.5f, y + cx * w * 0.5f)
            pt(x - cx * size, y - sy * size)
            pt(x + sy * w * 0.5f, y - cx * w * 0.5f)
            fill(p, color)
        }
    }

    private fun fireflies(p: ZenPainter, t: Float) {
        if (light.lantern < 0.4f || rain) return
        for (i in 0 until 12) {
            val pulse = sin(t * 1.7f + i * 2.3f)
            if (pulse <= 0f) continue
            val x = hash(i, 62) % W.toInt() + 8 * sin(t * 0.4f + i)
            val y = 84f + hash(i, 63) % 40 + 4 * sin(t * 0.6f + i * 1.3f)
            p.glow(x, y, 6f, a(P.Firefly, 0.5f * pulse * light.lantern))
            p.circle(x, y, 0.9f, a(P.Firefly, pulse * light.lantern))
        }
    }

    private fun lanternGlow(p: ZenPainter, t: Float) {
        if (light.lantern <= 0.05f) return
        val flicker = 0.9f + 0.1f * sin(t * 13) * sin(t * 7.3f)
        val glow = light.lantern * flicker
        p.rect(LANTERN_X - 4, 74.5f, 8f, 6f, a(ZenColor.mix(P.LanternFlicker, P.LanternLight, flicker), glow))
        p.glow(LANTERN_X, 77.5f, 40f, a(P.LanternLight, 0.42f * glow))
        var y = STREAM_TOP + 4
        var k = 0
        while (y < STREAM_BOTTOM - 1) {
            val jitter = sin(t * 2.5f + k * 1.9f) * 1.2f
            val w = 6f - k
            p.line(LANTERN_X - w / 2 + jitter, y, LANTERN_X + w / 2 + jitter, y, 1f, a(P.LanternLight, 0.6f * glow))
            y += 3
            k++
        }
    }

    /** Morning mist drifting over the ground line. */
    private fun fog(p: ZenPainter, t: Float, amount: Float) {
        val mist = light.mist
        p.verticalGradient(0f, 40f, W, GROUND_Y - 40f, a(mist, 0f), a(mist, 0.8f * amount))
        p.verticalGradient(0f, GROUND_Y, W, H - GROUND_Y, a(mist, 0.8f * amount), a(mist, 0.3f * amount))
        for (i in 0 until 5) {
            val x = (t * (3f + i % 3) + i * 83) % (W + 120) - 60
            p.glow(x, 88f + 10 * sin(i * 1.9f), 46f, a(mist, 0.5f * amount))
        }
    }

    companion object {
        const val GROUND_Y = 92f
        const val STREAM_TOP = 118f
        const val STREAM_BOTTOM = 136f
        const val LANTERN_X = 214f
        const val LANTERN_BASE = 108f
        const val PEAK_X = 160f
        const val PEAK_Y = 32f
        const val HERON_X = 120f
        const val DAWN_MIST = 0.3f
        private const val HERON_CYCLE = 9f
        private const val SEGMENT = 17f

        private val TREE_1 = listOf(floatArrayOf(82f, 36f, 20f), floatArrayOf(61f, 50f, 14f), floatArrayOf(103f, 48f, 14f), floatArrayOf(84f, 58f, 12f))
        private val TREE_2 = listOf(floatArrayOf(254f, 50f, 14f), floatArrayOf(240f, 60f, 10f), floatArrayOf(268f, 60f, 10f))
        private val STONES = listOf(150f to 124f, 166f to 129f, 182f to 124f)
        private val BANK_STONES = listOf(70f to 7f, 118f to 5f, 226f to 8f)

        /** Foreground bamboo: x and thickness of each stalk (left grove, right grove). */
        private val STALKS = listOf(
            floatArrayOf(7f, 6f), floatArrayOf(22f, 5f), floatArrayOf(35f, 4f),
            floatArrayOf(287f, 4.5f), floatArrayOf(300f, 6f), floatArrayOf(314f, 5f),
        )
    }
}
