package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * After work: a wooden pier on a calm lake. The pier runs from the bottom left into the water and
 * ends with a lantern post; a rowboat is tied beside it and rocks gently. The far shore is a line of
 * trees mirrored in the water, two ducks swim slowly across. A branch frames the top left corner and
 * reeds with cattails the bottom right, both moving in the breeze.
 *
 * Seasons and weather: blossoms and petals in spring, water lilies and fireflies in summer, red and
 * gold shore with leaves floating on the water in autumn, bare branch and snow on the pier and the
 * boat in winter. Rain rings on the water, mist on the lake at dawn, the low sun or the moon
 * reflected, the lantern light trembling on the water after dark.
 */
class NaturePainting(env: ZenState) : ScenePainting(env) {

    private val sunX = sun.x * W
    private val sunY = sun.y * HORIZON * 1.15f

    private val winter = season == Season.WINTER
    private val waterTop = ZenColor.mix(light.skyLow, light.water, 0.35f)

    override fun sky(p: ZenPainter) {
        skyGradient(p, HORIZON)
        sunOrMoon(p, sunX, sunY)
        overcast(p, HORIZON)
    }

    override fun skyMotion(p: ZenPainter, t: Float) {
        stars(p, t, maxY = 54)
        clouds(p, t, top = 16f)
        birds(p, t, top = 30f)
    }

    // Landscape -----------------------------------------------------------------------------------

    override fun landscape(p: ZenPainter) {
        haze(p)
        lake(p)
        shore(p, reflection = true)
        shore(p, reflection = false)
        if (season == Season.SUMMER || season == Season.SPRING) lilies(p)
        pier(p)
    }

    /** A soft band of distant hills behind the trees. */
    private fun haze(p: ZenPainter) {
        begin()
        pt(0f, HORIZON)
        var x = 0f
        while (x <= W) {
            pt(x, 56f + 4 * sin(x / 52 + 1) + 2 * sin(x / 19))
            x += 5
        }
        pt(W, HORIZON)
        fill(p, ZenColor.mix(lit(flora.hillFar), light.skyLow, 0.55f))
    }

    private fun lake(p: ZenPainter) {
        val deep = if (winter) ZenColor.mix(light.waterDeep, light.skyLow, 0.25f) else light.waterDeep
        p.verticalGradient(0f, HORIZON, W, H - HORIZON, waterTop, deep)
    }

    /**
     * The far shore: round trees and a few firs, with their reflection in the water below
     * ([reflection], squashed and faded).
     */
    private fun shore(p: ZenPainter, reflection: Boolean) {
        val tree = ZenColor.mix(lit(flora.forest), light.skyLow, 0.25f)
        val treeLight = ZenColor.mix(tree, lit(if (winter) P.SnowShade else flora.groundLight), 0.3f)
        val fir = ZenColor.mix(lit(P.Fir), light.skyLow, 0.3f)
        fun y(base: Float) = if (reflection) HORIZON + (HORIZON - base) * 0.75f else base
        fun c(color: Int) = if (reflection) a(ZenColor.mix(color, waterTop, 0.45f), 0.6f) else color
        var x = -6f
        var i = 0
        while (x < W + 8) {
            val lift = 2.5f * sin(x / 37) + 2f
            if (hash(i, 15) % 4 == 0) {
                val h = 11f + hash(i, 16) % 6
                begin()
                pt(x - 4, y(HORIZON + 0.5f))
                pt(x, y(HORIZON - lift - h))
                pt(x + 4, y(HORIZON + 0.5f))
                fill(p, c(fir))
                if (winter && !reflection) p.ellipse(x, HORIZON - lift - h * 0.55f, 1.8f, 0.8f, lit(P.Snow))
            } else {
                val r = 4f + hash(i, 13) % 5
                val cy = HORIZON - lift - r * 0.5f
                p.ellipse(x, y(cy), r, if (reflection) r * 0.75f else r, c(tree))
                if (!reflection && hash(i, 17) % 3 == 0) p.circle(x - r * 0.3f, cy - r * 0.35f, r * 0.45f, c(treeLight))
            }
            x += 5f + hash(i, 14) % 5
            i++
        }
        if (!reflection) p.rect(0f, HORIZON - 0.4f, W, 0.8f, a(lit(flora.groundDark), 0.6f))
    }

    /** Water lily pads beside the pier; in summer some are in flower. */
    private fun lilies(p: ZenPainter) {
        for ((k, pad) in PADS.withIndex()) {
            val x = pad[0]
            val y = pad[1]
            val r = pad[2]
            p.ellipse(x, y + 0.5f, r, r * 0.35f, a(light.waterDeep, 0.5f))
            p.ellipse(x, y, r, r * 0.35f, lit(P.LilyPad))
            begin()
            pt(x, y)
            pt(x + r, y - r * 0.12f)
            pt(x + r, y + r * 0.12f)
            fill(p, ZenColor.mix(waterTop, light.waterDeep, (y - HORIZON) / (H - HORIZON)))
            if (season == Season.SUMMER && k % 2 == 0) {
                p.circle(x - r * 0.2f, y - r * 0.25f, r * 0.32f, lit(P.Lily))
                p.circle(x - r * 0.2f, y - r * 0.32f, r * 0.14f, lit(P.Beak))
            }
        }
    }

    // Pier geometry: a quad from the bottom left (near, wide) to the far end in the lake

    private fun pierLeft(g: Float) = PIER_NEAR_L + (PIER_FAR_L - PIER_NEAR_L) * g

    private fun pierRight(g: Float) = PIER_NEAR_R + (PIER_FAR_R - PIER_NEAR_R) * g

    private fun pierY(g: Float) = PIER_NEAR_Y + (PIER_FAR_Y - PIER_NEAR_Y) * g

    private fun onPier(x: Float, y: Float): Boolean {
        if (y < PIER_FAR_Y - 1) return false
        val g = ((PIER_NEAR_Y - y) / (PIER_NEAR_Y - PIER_FAR_Y)).coerceIn(0f, 1f)
        return x > pierLeft(g) - 2 && x < pierRight(g) + 3
    }

    private fun pier(p: ZenPainter) {
        // Pilings under the right edge, with their reflection
        for (g in floatArrayOf(0.12f, 0.38f, 0.6f, 0.8f, 0.97f)) {
            val x = pierRight(g)
            val y = pierY(g)
            val w = 1f + (1 - g) * 2.6f
            val h = 3f + (1 - g) * 7f
            p.rect(x - w, y, w, h, lit(P.PierShade))
            p.rect(x - w, y + h, w, h * 0.8f, a(lit(P.PierShade), 0.35f))
        }
        // Side of the deck
        begin()
        pt(PIER_NEAR_R, PIER_NEAR_Y)
        pt(PIER_FAR_R, PIER_FAR_Y)
        pt(PIER_FAR_R, PIER_FAR_Y + 1.4f)
        pt(PIER_NEAR_R, PIER_NEAR_Y + 5f)
        fill(p, lit(P.PierShade))
        // Deck
        begin()
        pt(PIER_NEAR_L, PIER_NEAR_Y)
        pt(PIER_NEAR_R, PIER_NEAR_Y)
        pt(PIER_FAR_R, PIER_FAR_Y)
        pt(PIER_FAR_L, PIER_FAR_Y)
        fill(p, lit(P.Pier))
        // Planks: closer together in the distance
        for (k in 1 until PLANKS) {
            val g = 1f - (1f - k / PLANKS.toFloat()).pow(1.7f)
            val width = 0.25f + (1 - g) * 0.7f
            p.line(pierLeft(g), pierY(g), pierRight(g), pierY(g), width, lit(P.PierGap))
        }
        // Light on the left edge
        p.line(PIER_NEAR_L, PIER_NEAR_Y, PIER_FAR_L, PIER_FAR_Y, 1.2f, lit(P.PierLight))
        if (winter) {
            begin()
            pt(PIER_NEAR_L + 6, PIER_NEAR_Y)
            pt(PIER_NEAR_R - 10, PIER_NEAR_Y)
            pt(PIER_FAR_R - 2, PIER_FAR_Y + 0.5f)
            pt(PIER_FAR_L + 1, PIER_FAR_Y + 0.5f)
            fill(p, a(lit(P.Snow), 0.85f))
        }
        // Mooring post at the far left corner, lantern post at the far right one
        p.rect(PIER_FAR_L - 0.8f, PIER_FAR_Y - 6, 1.8f, 7f, lit(P.PierShade))
        val lx = LANTERN_X
        p.rect(lx - 0.9f, LANTERN_Y + 5, 1.8f, PIER_FAR_Y - LANTERN_Y - 4, lit(P.LampIron))
        p.rect(lx - 2.4f, LANTERN_Y, 4.8f, 5f, ZenColor.mix(lit(P.LampGlass), P.LanternLight, light.lantern))
        begin()
        pt(lx - 3.2f, LANTERN_Y + 0.3f)
        pt(lx, LANTERN_Y - 2.4f)
        pt(lx + 3.2f, LANTERN_Y + 0.3f)
        fill(p, lit(P.LampIron))
        p.rect(lx - 2.6f, LANTERN_Y + 5f, 5.2f, 0.8f, lit(P.LampIron))
        if (winter) p.ellipse(lx, LANTERN_Y - 1.4f, 2.4f, 0.8f, lit(P.Snow))
    }

    // Foreground ----------------------------------------------------------------------------------

    override fun foreground(p: ZenPainter, t: Float) {
        water(p, t)
        if (season == Season.AUTUMN) floatingLeaves(p, t)
        ducks(p, t)
        boat(p, t)
        lanternGlow(p, t)
        reeds(p, t)
        branch(p, t)
        particles(p, t)
        if (rain) shower(p, t)
        if (env.timeOfDay == TimeOfDay.DAWN) mist(p, t)
    }

    /** Ripples drifting slowly, longer as they come closer; the sun or moon reflected below it. */
    private fun water(p: ZenPainter, t: Float) {
        val ripple = a(light.waterLight, 0.4f)
        for (i in 0 until 34) {
            val f = (hash(i, 1) % 1000) / 1000f
            val y = HORIZON + 2 + f.pow(1.5f) * (H - HORIZON - 2)
            val len = 2f + f * 12f
            val x = (hash(i, 2) % W.toInt()) + sin(t * 0.4f + i) * (2f + f * 4f)
            if (onPier(x, y) || onPier(x + len, y)) continue
            p.line(x, y, x + len, y, 0.4f + f * 0.5f, a(ripple, 0.25f + 0.25f * sin(t * 0.7f + i * 1.3f) + 0.25f))
        }
        // Reflection column of a low sun, or of the moon
        if (!rain && (!sun.isSun || sun.y > 0.35f)) {
            val color = if (sun.isSun) P.SunGlow else ZenColor.mix(light.water, P.Moon, 0.8f)
            var k = 0
            var y = HORIZON + 2
            while (y < HORIZON + 40) {
                val w = (9f - k * 0.5f) * (0.7f + 0.3f * sin(t * 1.8f + k * 1.9f))
                val x = sunX + sin(t * 1.3f + k * 2.3f) * 1.2f
                if (!onPier(x, y)) p.line(x - w / 2, y, x + w / 2, y, 0.9f, a(color, 0.6f - k * 0.035f))
                y += 2.6f
                k++
            }
        } else if (sun.isSun && light.rays > 0.05f && !rain) {
            for (i in 0 until 12) {
                val flash = sin(t * 3.2f + i * 2.1f)
                if (flash < 0.6f) continue
                val y = HORIZON + 6 + hash(i, 42) % (H - HORIZON - 10).toInt()
                val x = (hash(i, 41) % W.toInt()).toFloat()
                if (!onPier(x, y)) p.circle(x, y, 0.5f + (y - HORIZON) / 80f, a(P.Cloud, (flash - 0.6f) * 2.5f * light.rays))
            }
        }
    }

    /** Autumn leaves floating on the water, drifting slowly to the right. */
    private fun floatingLeaves(p: ZenPainter, t: Float) {
        for (i in 0 until 7) {
            val y = HORIZON + 14 + hash(i, 141) % 60
            val x = (t * 1.2f + hash(i, 142)) % (W + 20) - 10
            if (onPier(x, y)) continue
            val s = 1f + (y - HORIZON) / 40f
            val ang = i * 1.3f + sin(t * 0.5f + i) * 0.3f
            val dx = cos(ang) * s
            val dy = sin(ang) * s * 0.4f
            begin()
            pt(x - dx, y - dy)
            pt(x - dy, y + s * 0.3f)
            pt(x + dx, y + dy)
            pt(x + dy, y - s * 0.3f)
            fill(p, lit(if (i % 2 == 0) flora.particle else flora.particle2))
        }
    }

    /** Two ducks gliding across the far water, with their V-shaped wake. Asleep after dark. */
    private fun ducks(p: ZenPainter, t: Float) {
        if (light.stars > 0.5f) return
        for (i in 0 until 2) {
            val x = (t * 2f + i * 11f + 60f) % (W + 60) - 30
            val y = 86f + i * 3f + sin(t * 0.9f + i) * 0.3f
            if (abs(x - LANTERN_X) < 5 && y < PIER_FAR_Y) continue
            val s = 1f + i * 0.12f
            p.line(x - 3 * s, y + 0.8f, x - 12 * s, y - 0.6f, 0.4f, a(light.waterLight, 0.5f))
            p.line(x - 3 * s, y + 0.8f, x - 12 * s, y + 2.4f, 0.4f, a(light.waterLight, 0.5f))
            p.ellipse(x, y, 3.2f * s, 1.3f * s, lit(P.Duck))
            begin()
            pt(x - 2.6f * s, y - 0.4f * s)
            pt(x - 4.2f * s, y - 1.4f * s)
            pt(x - 2.4f * s, y + 0.6f * s)
            fill(p, lit(P.Duck))
            p.circle(x + 2.4f * s, y - 1.6f * s, 1.1f * s, lit(P.DuckHead))
            p.line(x + 3.3f * s, y - 1.5f * s, x + 4.5f * s, y - 1.3f * s, 0.7f * s, lit(P.Beak))
        }
    }

    /** The rowboat tied to the pier, rocking gently on the water. */
    private fun boat(p: ZenPainter, t: Float) {
        val bob = sin(t * BOAT_ROCK) * 0.5f
        val tilt = sin(t * BOAT_ROCK + 1.2f) * 0.5f
        val x = BOAT_X
        val y = BOAT_Y + bob
        // Mooring line to the post on the pier
        p.line(PIER_FAR_R + 1, PIER_FAR_Y + 0.5f, x - 13, y - 1 - tilt, 0.4f, lit(P.Rope))
        // Reflection
        p.ellipse(x, y + 4.5f, 13f, 1.6f, a(ZenColor.mix(lit(P.BoatHull), light.waterDeep, 0.5f), 0.4f))
        // Hull: pointed bow on the left, square stern on the right
        begin()
        pt(x - 15, y - 1.6f - tilt)
        pt(x + 13, y - 1.8f + tilt)
        pt(x + 12, y + 2.6f + tilt * 0.5f)
        pt(x + 4, y + 3.6f)
        pt(x - 7, y + 3.2f)
        fill(p, lit(P.BoatHull))
        p.line(x - 15, y - 1.6f - tilt, x + 13, y - 1.8f + tilt, 1.1f, lit(P.BoatStripe))
        p.ellipse(x - 0.5f, y - 1.9f, 12.5f, 1.4f, lit(P.BoatInside))
        p.line(x + 2, y - 3f, x + 2, y - 0.8f, 0.7f, lit(P.PierShade))
        p.line(x + 3, y - 2.6f, x + 11, y - 4.6f, 0.6f, lit(P.PierShade))
        if (winter) p.ellipse(x - 0.5f, y - 2.4f, 11f, 1.1f, lit(P.Snow))
        // Small ripples around the hull
        val r = 0.5f + 0.5f * sin(t * BOAT_ROCK * 2)
        p.line(x - 18 - r, y + 3.5f, x - 13, y + 3.5f, 0.4f, a(light.waterLight, 0.5f))
        p.line(x + 13, y + 3.8f, x + 18 + r, y + 3.8f, 0.4f, a(light.waterLight, 0.5f))
    }

    /** After dark the lantern glows and its light trembles on the water below. */
    private fun lanternGlow(p: ZenPainter, t: Float) {
        if (light.lantern <= 0.05f) return
        val flicker = 0.92f + 0.08f * sin(t * 11) * sin(t * 6.3f)
        val glow = light.lantern * flicker
        p.glow(LANTERN_X, LANTERN_Y + 2.5f, 30f, a(P.LanternLight, 0.4f * glow))
        p.rect(LANTERN_X - 1.6f, LANTERN_Y + 0.8f, 3.2f, 3.6f, a(P.LanternFlicker, 0.6f * glow))
        var k = 0
        var y = PIER_FAR_Y + 4
        while (y < PIER_FAR_Y + 34) {
            val w = (5f - k * 0.3f) * (0.6f + 0.4f * sin(t * 2.1f + k * 1.7f))
            val x = LANTERN_X + 2 + sin(t * 1.5f + k * 2.1f) * 1f
            if (!onPier(x, y)) p.line(x - w / 2, y, x + w / 2, y, 0.8f, a(P.LanternLight, (0.55f - k * 0.04f) * glow))
            y += 2.8f
            k++
        }
    }

    /** Reeds with cattails in the bottom corners, swaying; their color follows the season. */
    private fun reeds(p: ZenPainter, t: Float) {
        val (reed, reedDark) = when (season) {
            Season.AUTUMN -> P.ReedAutumn to P.Cattail
            Season.WINTER -> P.ReedWinter to P.ReedAutumn
            else -> P.Reed to P.ReedDark
        }
        for ((i, s) in REEDS.withIndex()) {
            val x = s[0]
            val h = s[1]
            val lean = sin(t * 1.1f + i * 0.7f) * h * 0.04f + s[2]
            val baseY = H + 2
            val midX = x + lean * 0.35f
            val midY = baseY - h * 0.55f
            val tipX = x + lean
            val tipY = baseY - h
            val color = lit(if (i % 2 == 0) reed else reedDark)
            p.line(x, baseY, midX, midY, 1f, color)
            p.line(midX, midY, tipX, tipY, 0.8f, color)
            if (i % 3 == 0) {
                // Cattail head
                val cx = x + lean * 0.86f
                val cy = baseY - h * 0.86f
                p.line(cx - lean * 0.05f, cy + 3.5f, cx + lean * 0.05f, cy - 3.5f, 2.4f, lit(P.Cattail))
                if (winter) p.ellipse(cx, cy - 3.6f, 1.4f, 0.7f, lit(P.Snow))
            } else if (i % 3 == 1) {
                // A long blade arching out
                val dir = if (i % 2 == 0) 1f else -1f
                leaf(p, midX, midY + 4, dir * 0.45f, -0.9f, h * 0.55f, 1.1f, color)
            }
        }
    }

    /** A branch reaching in from the top left corner, gently swaying. */
    private fun branch(p: ZenPainter, t: Float) {
        val sway = sin(t * 0.5f) * 0.012f + sin(t * 1.3f) * 0.004f
        fun sx(x: Float) = x
        fun sy(x: Float, y: Float) = y + (x + 6) * sway * 8
        val wood = lit(P.Trunk)
        for (k in 0 until BRANCH.size - 1) {
            val a0 = BRANCH[k]
            val a1 = BRANCH[k + 1]
            p.line(sx(a0[0]), sy(a0[0], a0[1]), sx(a1[0]), sy(a1[0], a1[1]), a0[2], wood)
        }
        for (tw in TWIGS) p.line(tw[0], sy(tw[0], tw[1]), tw[2], sy(tw[2], tw[3]), 0.7f, wood)
        if (winter) {
            for (b in BRANCH) p.ellipse(b[0], sy(b[0], b[1]) - b[2] * 0.6f, 2.4f, 0.8f, lit(P.Snow))
            for (tw in TWIGS) p.ellipse(tw[2], sy(tw[2], tw[3]) - 0.6f, 1.4f, 0.6f, lit(P.Snow))
            return
        }
        // Leaf clusters: small lance-shaped leaves fanning out, each with its own flutter
        val colors = intArrayOf(lit(flora.treeDark), lit(flora.tree), lit(flora.treeLight))
        for ((ci, c) in FOLIAGE.withIndex()) {
            val cx = c[0]
            val cy = sy(c[0], c[1])
            val r = c[2]
            for (k in 0 until LEAVES_PER_CLUSTER) {
                val ang = (hash(ci, k, 51) % 628) / 100f + sin(t * 1.4f + ci + k) * 0.06f
                val len = r * (0.7f + (hash(ci, k, 52) % 40) / 100f)
                val ox = cx + cos(ang) * r * 0.15f
                val oy = cy + sin(ang) * r * 0.15f
                leaf(p, ox, oy, cos(ang), sin(ang), len, r * 0.22f, colors[k % 3])
            }
            if (season == Season.SPRING) {
                for (k in 0 until 4) {
                    val ang = (hash(ci, k, 53) % 628) / 100f
                    val d = r * 0.5f
                    p.circle(cx + cos(ang) * d, cy + sin(ang) * d, r * 0.16f, lit(flora.particle2))
                    p.circle(cx + cos(ang) * d, cy + sin(ang) * d, r * 0.06f, lit(P.Beak))
                }
            }
        }
    }

    private fun particles(p: ZenPainter, t: Float) {
        when (season) {
            Season.SPRING -> falling(p, t, count = 14, period = 10f, size = 1.4f, fromBranch = true)
            Season.SUMMER -> fireflies(p, t)
            Season.AUTUMN -> falling(p, t, count = 8, period = 9f, size = 2f, fromBranch = true)
            Season.WINTER -> falling(p, t, count = if (env.snow) 80 else 26, period = 14f, size = 0f, fromBranch = false)
        }
    }

    /**
     * Petals or leaves ([size] > 0, tumbling, drifting from the branch when [fromBranch]) or
     * snowflakes ([size] 0, everywhere).
     */
    private fun falling(p: ZenPainter, t: Float, count: Int, period: Float, size: Float, fromBranch: Boolean) {
        for (i in 0 until count) {
            val pd = period * (1f + (hash(i, 21) % 50) / 100f)
            val phase = ((t + (hash(i, 22) % 1000) / 10f) / pd) % 1f
            val startX = if (fromBranch) (hash(i, 23) % 90).toFloat() else (hash(i, 23) % (W + 24).toInt()).toFloat()
            val x = startX + phase * if (fromBranch) 90f else -24f + 4 * sin(t * 0.9f + i)
            val y = (if (fromBranch) 20f else -4f) + phase * (H + 8)
            val color = lit(if (i % 3 == 0) flora.particle2 else flora.particle)
            if (size == 0f) {
                p.circle(x, y, 0.7f + (hash(i, 24) % 3) * 0.35f, a(color, 0.92f))
                continue
            }
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
        for (i in 0 until 10) {
            val pulse = sin(t * 1.7f + i * 2.3f)
            if (pulse <= 0f) continue
            val x = 200f + hash(i, 62) % 120 + 8 * sin(t * 0.4f + i)
            val y = 104f + hash(i, 63) % 50 + 4 * sin(t * 0.6f + i * 1.3f)
            p.glow(x, y, 6f, a(P.Firefly, 0.5f * pulse * light.lantern))
            p.circle(x, y, 0.9f, a(P.Firefly, pulse * light.lantern))
        }
    }

    /** Rain over the whole scene and rings spreading on the lake. */
    private fun shower(p: ZenPainter, t: Float) {
        for (i in 0 until 110) rainDrop(p, t, i, 0f, 0f, W, H, 0.3f)
        for (i in 0 until 14) {
            val period = 1.2f + (hash(i, 121) % 10) / 10f
            val phase = ((t + hash(i, 122) % 100 / 10f) / period) % 1f
            val cycle = ((t + hash(i, 122) % 100 / 10f) / period).toInt()
            val y = HORIZON + 4 + hash(i, cycle, 124) % (H - HORIZON - 6).toInt()
            val x = (hash(i, cycle, 123) % W.toInt()).toFloat()
            if (onPier(x, y)) continue
            val r = (1f + phase * 4f) * (0.4f + (y - HORIZON) / 60f)
            p.ellipse(x, y, r, r * 0.3f, a(light.waterLight, 0.6f * (1f - phase)))
        }
    }

    /** Morning mist lying on the lake, drifting. */
    private fun mist(p: ZenPainter, t: Float) {
        val mist = light.mist
        p.verticalGradient(0f, HORIZON - 18, W, 18f, a(mist, 0f), a(mist, 0.5f))
        p.verticalGradient(0f, HORIZON, W, 26f, a(mist, 0.5f), a(mist, 0f))
        for (i in 0 until 5) {
            val x = (t * (2.5f + i % 3) + i * 83) % (W + 120) - 60
            p.glow(x, HORIZON + 2 + 4 * sin(i * 1.9f), 40f, a(mist, 0.35f))
        }
    }

    companion object {
        const val HORIZON = 72f
        private const val PLANKS = 18
        private const val LEAVES_PER_CLUSTER = 11

        private const val PIER_NEAR_Y = 162f
        private const val PIER_FAR_Y = 100f
        private const val PIER_NEAR_L = 30f
        private const val PIER_NEAR_R = 112f
        private const val PIER_FAR_L = 160f
        private const val PIER_FAR_R = 180f
        private const val LANTERN_X = 178f
        private const val LANTERN_Y = 84f

        private const val BOAT_X = 208f
        private const val BOAT_Y = 112f
        private const val BOAT_ROCK = 0.8f

        /** Lily pads: x, y, radius. */
        private val PADS = arrayOf(
            floatArrayOf(140f, 140f, 5f), floatArrayOf(152f, 132f, 4f), floatArrayOf(132f, 152f, 6f),
            floatArrayOf(162f, 146f, 4.5f), floatArrayOf(146f, 124f, 3.2f),
        )

        /** Reed stalks: x, height, lean. */
        private val REEDS = arrayOf(
            floatArrayOf(270f, 34f, -2f), floatArrayOf(276f, 44f, -1f), floatArrayOf(282f, 30f, 1f),
            floatArrayOf(288f, 48f, -3f), floatArrayOf(294f, 38f, 0f), floatArrayOf(300f, 52f, -2f),
            floatArrayOf(306f, 36f, 1f), floatArrayOf(312f, 46f, -1f), floatArrayOf(318f, 40f, 0f),
            floatArrayOf(4f, 30f, 2f), floatArrayOf(10f, 40f, 3f), floatArrayOf(16f, 26f, 1f),
        )

        /** The branch: points (x, y, thickness of the segment that starts there). */
        private val BRANCH = arrayOf(
            floatArrayOf(-6f, 6f, 4f), floatArrayOf(20f, 13f, 3f), floatArrayOf(46f, 20f, 2.2f),
            floatArrayOf(70f, 24f, 1.4f), floatArrayOf(90f, 24f, 1f),
        )
        private val TWIGS = arrayOf(
            floatArrayOf(28f, 15f, 38f, 6f), floatArrayOf(50f, 21f, 58f, 32f), floatArrayOf(66f, 23f, 74f, 15f),
            floatArrayOf(12f, 10f, 14f, 22f),
        )

        /** Leaf clusters along the branch: x, y, radius. */
        private val FOLIAGE = arrayOf(
            floatArrayOf(14f, 20f, 7f), floatArrayOf(36f, 8f, 6.5f), floatArrayOf(30f, 22f, 6f),
            floatArrayOf(54f, 30f, 6f), floatArrayOf(60f, 18f, 5.5f), floatArrayOf(76f, 16f, 5f),
            floatArrayOf(86f, 26f, 4.5f), floatArrayOf(2f, 12f, 6f),
        )
    }
}
