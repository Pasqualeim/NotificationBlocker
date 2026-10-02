package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Working hours: a desk in a high-rise office. A monitor where lines of code type themselves in a
 * loop, a keyboard whose keys light up while typing, a steaming mug, a swaying plant and a wall clock
 * on the real time. Behind the desk a floor-to-ceiling glass wall on a skyline of towers: the real
 * sky (sun or moon), lit windows after dark, a park below in the season's colors, snow on the roofs
 * in winter, rain or snowfall, and now and then a plane.
 *
 * Indoor colors use [indoor]: daylight during the day, warm lamp light when it gets dark outside.
 * The screen is emissive (never dimmed) and after dark it casts a soft light around it.
 */
class DeskPainting(env: ZenState) : ScenePainting(env) {

    private val indoorLight = ZenColor.mix(light.ambient, P.IndoorLight, light.lantern * 0.85f)

    private fun indoor(color: Int) = ZenColor.multiply(color, indoorLight)

    private val overcastSky = rain || env.snow

    /** Sun and moon move across the glass, above the skyline. */
    private val sunX = WIN_X + 8 + sun.x * (WIN_W - 16)
    private val sunY = WIN_Y + 8 + (sun.y - 0.1f) / 0.54f * 44f

    // Outside ------------------------------------------------------------------------------------

    override fun sky(p: ZenPainter) {
        skyGradient(p, WIN_Y + WIN_H)
        if (overcastSky) {
            p.glow(sunX, sunY, 18f, a(if (sun.isSun) P.Sun else P.Moon, 0.25f))
            p.rect(0f, 0f, W, WIN_Y + WIN_H, a(lit(P.Overcast), 0.35f))
        } else {
            sunOrMoon(p, sunX, sunY)
        }
        farTowers(p)
        landmark(p)
        nearTowers(p)
        park(p)
    }

    /** Hazy towers in the distance, with a few spires. */
    private fun farTowers(p: ZenPainter) {
        val far = ZenColor.mix(lit(P.TowerGlassDark), light.skyLow, 0.55f)
        val farLit = ZenColor.mix(far, P.LanternLight, light.lantern * 0.6f)
        var x = WIN_X - 4
        var i = 0
        while (x < WIN_X + WIN_W) {
            val w = 9f + hash(i, 91) % 9
            val top = 56f + hash(i, 92) % 22
            p.rect(x, top, w, SKYLINE_BASE - top, far)
            if (hash(i, 93) % 5 == 0) p.line(x + w / 2, top, x + w / 2, top - 7, 0.6f, far)
            if (light.lantern > 0.2f) {
                for (k in 0 until 4) {
                    val wy = top + 3 + hash(i, k, 94) % (SKYLINE_BASE - top - 4).toInt()
                    p.rect(x + 1.5f + hash(i, k, 95) % (w - 3).toInt(), wy, 1.2f, 1.2f, farLit)
                }
            }
            x += w + 1.5f
            i++
        }
    }

    /** A tall stepped tower with its spire, the landmark of the view. */
    private fun landmark(p: ZenPainter) {
        val stone = lit(P.TowerStone)
        val shade = lit(P.TowerStoneShade)
        val x = LANDMARK_X
        val steps = arrayOf(floatArrayOf(14f, 42f), floatArrayOf(10f, 32f), floatArrayOf(6.5f, 25f), floatArrayOf(3.5f, 20f))
        for (s in steps) {
            p.rect(x - s[0], s[1], s[0] * 2, SKYLINE_BASE - s[1], stone)
            p.rect(x + s[0] - 2.2f, s[1], 2.2f, SKYLINE_BASE - s[1], shade)
        }
        p.line(x, 20f, x, 9f, 0.9f, lit(P.Spire))
        if (season == Season.WINTER) for (s in steps) p.rect(x - s[0], s[1] - 0.6f, s[0] * 2, 1f, lit(P.Snow))
        // Window rows, warm after dark
        val glass = ZenColor.mix(stone, light.skyLow, 0.3f)
        val warm = ZenColor.mix(glass, P.LanternLight, light.lantern)
        var y = 45f
        var row = 0
        while (y < SKYLINE_BASE - 2) {
            var wx = x - 12f
            var col = 0
            while (wx < x + 11f) {
                p.rect(wx, y, 1.6f, 2f, if (hash(row, col, 97) % 10 < 4) warm else glass)
                wx += 3.2f
                col++
            }
            y += 4.5f
            row++
        }
        if (light.lantern > 0.3f && !overcastSky) p.circle(x, 9f, 0.9f, P.PlaneLight)
    }

    /** Closer glass towers: vertical facades that reflect the sky, lit floors after dark. */
    private fun nearTowers(p: ZenPainter) {
        for ((k, tower) in TOWERS.withIndex()) {
            val x = tower[0]
            val w = tower[1]
            val top = tower[2]
            val glass = ZenColor.mix(lit(if (k % 2 == 0) P.TowerGlass else P.TowerGlassDark), light.skyMid, 0.3f)
            val shade = ZenColor.mix(glass, lit(P.TowerGlassDark), 0.6f)
            p.rect(x, top, w, SKYLINE_BASE - top, glass)
            p.rect(x + w - 3, top, 3f, SKYLINE_BASE - top, shade)
            // The sky reflected on the upper floors
            p.verticalGradient(x, top, w - 3, 16f, a(light.skyLow, 0.45f), a(light.skyLow, 0f))
            if (season == Season.WINTER) p.rect(x, top - 0.6f, w, 1f, lit(P.Snow))
            // Vertical mullions of the facade
            var mx = x + 3
            while (mx < x + w - 3) {
                p.rect(mx, top + 1, 0.4f, SKYLINE_BASE - top - 1, a(shade, 0.7f))
                mx += 3.4f
            }
            // Lit floors after dark
            if (light.lantern > 0.05f) {
                var fy = top + 3
                var floor = 0
                while (fy < SKYLINE_BASE - 2) {
                    if (hash(k, floor, 98) % 3 == 0) {
                        val fx = x + 1 + hash(k, floor, 99) % (w * 0.5f).toInt()
                        p.rect(fx, fy, 3f + hash(k, floor, 100) % 6, 1.3f, a(P.LanternLight, 0.85f * light.lantern))
                    }
                    fy += 3.2f
                    floor++
                }
            }
        }
    }

    /** The park at the foot of the towers, in the season's colors. */
    private fun park(p: ZenPainter) {
        p.rect(WIN_X - 2, SKYLINE_BASE, WIN_W + 4, WIN_Y + WIN_H - SKYLINE_BASE, lit(flora.ground))
        val tree = lit(flora.tree)
        val treeDark = lit(flora.treeDark)
        var x = WIN_X
        var i = 0
        while (x < WIN_X + WIN_W + 6) {
            val r = 3.5f + hash(i, 96) % 3
            if (season == Season.WINTER) {
                p.line(x, SKYLINE_BASE + 2, x, SKYLINE_BASE - r, 0.7f, lit(P.Trunk))
                p.ellipse(x, SKYLINE_BASE - r * 0.6f, r * 0.7f, r * 0.5f, a(tree, 0.8f))
            } else {
                p.circle(x, SKYLINE_BASE - r * 0.2f + 1, r, treeDark)
                p.circle(x - 0.8f, SKYLINE_BASE - r * 0.45f, r * 0.75f, tree)
            }
            x += 6f + hash(i, 97) % 4
            i++
        }
    }

    override fun skyMotion(p: ZenPainter, t: Float) {
        stars(p, t, maxY = 36)
        clouds(p, t, top = 18f, count = 3)
        birds(p, t, top = 30f)
        plane(p, t)
        when {
            rain -> for (i in 0 until 80) rainDrop(p, t, i, WIN_X, WIN_Y, WIN_X + WIN_W, WIN_Y + WIN_H, 0.4f)
            env.snow -> snowfall(p, t)
        }
    }

    /** Once in a while a plane crosses high up, with a blinking light after dark. */
    private fun plane(p: ZenPainter, t: Float) {
        val cycle = t % PLANE_CYCLE
        if (cycle > 22f || overcastSky) return
        val x = WIN_X + WIN_W + 10 - cycle / 22f * (WIN_W + 20)
        val y = 22f + cycle * 0.2f
        p.line(x - 4, y, x + 4, y + 0.6f, 1.2f, lit(P.Plane))
        p.line(x + 1, y + 0.2f, x + 3, y + 1.8f, 0.9f, lit(P.Plane))
        if (light.lantern > 0.3f && (t * 1.2f) % 1f < 0.3f) p.circle(x - 4, y, 0.9f, P.PlaneLight)
    }

    private fun snowfall(p: ZenPainter, t: Float) {
        for (i in 0 until 60) {
            val speed = 7f + hash(i, 121) % 7
            val y = WIN_Y + (t * speed + hash(i, 122)) % (WIN_H + 4) - 2
            val x = WIN_X + hash(i, 123) % WIN_W.toInt() + sin(t * 0.9f + i) * 2.2f
            p.circle(x, y, 0.6f + (hash(i, 124) % 3) * 0.25f, a(lit(P.Snow), 0.92f))
        }
    }

    // Room ----------------------------------------------------------------------------------------

    override fun landscape(p: ZenPainter) {
        window(p)
        desk(p)
        clock(p)
        // After dark, the screen casts a soft light on the wall and desk around it
        if (light.lantern > 0.05f) p.glow(SCR_X + SCR_W / 2, DESK_Y - 10, 64f, a(P.ScreenWarmGlow, 0.2f * light.lantern))
        monitor(p)
        keyboard(p)
        mug(p)
        pot(p)
    }

    /** Floor-to-ceiling glass wall with thin dark frames. */
    private fun window(p: ZenPainter) {
        val right = WIN_X + WIN_W
        val bottom = WIN_Y + WIN_H
        // Faint reflections on the glass
        for (k in 0 until 3) {
            val x0 = WIN_X + 30 + k * 64
            begin()
            pt(x0, WIN_Y)
            pt(x0 + 12, WIN_Y)
            pt(x0 - 22, bottom)
            pt(x0 - 34, bottom)
            fill(p, a(P.Cloud, 0.04f + 0.04f * light.rays))
        }
        val wall = indoor(P.StudioWall)
        p.rect(0f, 0f, W, WIN_Y, wall)
        p.rect(0f, 0f, WIN_X, H, wall)
        p.rect(right, 0f, W - right, H, wall)
        p.verticalGradient(right, DESK_Y - 24, W - right, 24f, a(indoor(P.StudioWallShade), 0f), indoor(P.StudioWallShade))
        val metal = indoor(P.WindowMetal)
        p.rect(WIN_X - 2, WIN_Y - 2, WIN_W + 4, 2f, metal)
        p.rect(WIN_X - 2, WIN_Y, 2f, WIN_H, metal)
        p.rect(right, WIN_Y, 2f, WIN_H, metal)
        for (k in 1 until PANES) p.rect(WIN_X + k * WIN_W / PANES - 0.6f, WIN_Y, 1.2f, WIN_H, metal)
    }

    private fun desk(p: ZenPainter) {
        p.rect(0f, DESK_Y, W, H - DESK_Y, indoor(P.StudioDesk))
        p.rect(0f, DESK_Y, W, 2.5f, indoor(P.StudioDeskTop))
        p.rect(0f, H - 4, W, 4f, indoor(P.StudioDeskEdge))
    }

    private fun clock(p: ZenPainter) {
        val rim = indoor(P.Monitor)
        p.circle(CLOCK_X, CLOCK_Y, CLOCK_R, rim)
        p.circle(CLOCK_X, CLOCK_Y, CLOCK_R - 1.5f, indoor(P.ClockFace))
        for (k in 0 until 12) {
            val ang = k * PI.toFloat() / 6
            val r0 = if (k % 3 == 0) CLOCK_R - 4f else CLOCK_R - 3f
            p.line(
                CLOCK_X + sin(ang) * r0, CLOCK_Y - cos(ang) * r0,
                CLOCK_X + sin(ang) * (CLOCK_R - 2.4f), CLOCK_Y - cos(ang) * (CLOCK_R - 2.4f),
                if (k % 3 == 0) 0.8f else 0.45f, rim,
            )
        }
        // Real time: hour and minute hands follow the clock of the phone
        hand(p, (env.clock % (12 * 60)) / 720f, CLOCK_R * 0.5f, 1.3f, rim)
        hand(p, (env.clock % 60) / 60f, CLOCK_R * 0.74f, 0.9f, rim)
    }

    private fun hand(p: ZenPainter, turn: Float, len: Float, width: Float, color: Int) {
        val ang = turn * 2 * PI.toFloat()
        p.line(CLOCK_X, CLOCK_Y, CLOCK_X + sin(ang) * len, CLOCK_Y - cos(ang) * len, width, color)
    }

    private fun monitor(p: ZenPainter) {
        val body = indoor(P.Monitor)
        p.rect(MON_X + MON_W / 2 - 6, MON_Y + MON_H - 2, 12f, DESK_Y - MON_Y - MON_H + 2, body)
        p.ellipse(MON_X + MON_W / 2, DESK_Y + 0.5f, 18f, 2.2f, body)
        // Bezel with rounded corners
        p.rect(MON_X + 3, MON_Y, MON_W - 6, MON_H, body)
        p.rect(MON_X, MON_Y + 3, MON_W, MON_H - 6, body)
        for (cx in floatArrayOf(MON_X + 3, MON_X + MON_W - 3)) for (cy in floatArrayOf(MON_Y + 3, MON_Y + MON_H - 3)) {
            p.circle(cx, cy, 3f, body)
        }
        // Screen: emissive, so not dimmed by the room light
        p.rect(SCR_X, SCR_Y, SCR_W, SCR_H, P.MonitorScreen)
        p.circle(SCR_X + 4.5f, SCR_Y + 4.5f, 1.2f, P.CodeCoral)
        p.circle(SCR_X + 8.5f, SCR_Y + 4.5f, 1.2f, P.CodeAmber)
        p.circle(SCR_X + 12.5f, SCR_Y + 4.5f, 1.2f, P.CodeSage)
    }

    private fun keyboard(p: ZenPainter) {
        p.rect(KEY_X, KEY_Y, KEY_W, 9f, indoor(P.Keyboard))
        val key = indoor(P.Key)
        for (k in 0 until KEYS_ROW) p.rect(keyX(k), KEY_Y + 1.4f, KEY_SIZE, 2.4f, key)
        p.rect(KEY_X + 3, KEY_Y + 5f, 9f, 2.4f, key)
        p.rect(KEY_X + 14, KEY_Y + 5f, KEY_W - 30, 2.4f, key)
        p.rect(KEY_X + KEY_W - 14, KEY_Y + 5f, 11f, 2.4f, key)
    }

    private fun keyX(k: Int) = KEY_X + 3 + k * (KEY_SIZE + 1.3f)

    private fun mug(p: ZenPainter) {
        val mug = indoor(P.Terracotta)
        p.line(MUG_X + 7, MUG_TOP + 5, MUG_X + 10.5f, MUG_TOP + 6.5f, 1.8f, mug)
        p.line(MUG_X + 10.5f, MUG_TOP + 6.5f, MUG_X + 10.5f, MUG_TOP + 10.5f, 1.8f, mug)
        p.line(MUG_X + 10.5f, MUG_TOP + 10.5f, MUG_X + 7, MUG_TOP + 12f, 1.8f, mug)
        p.rect(MUG_X - 7, MUG_TOP, 14f, DESK_Y + 1 - MUG_TOP, mug)
        p.rect(MUG_X - 7, MUG_TOP, 14f, 2.4f, indoor(P.TerracottaShade))
    }

    private fun pot(p: ZenPainter) {
        begin()
        pt(PLANT_X - 9, PLANT_Y)
        pt(PLANT_X + 9, PLANT_Y)
        pt(PLANT_X + 6.5f, DESK_Y + 1)
        pt(PLANT_X - 6.5f, DESK_Y + 1)
        fill(p, indoor(P.Terracotta))
        p.rect(PLANT_X - 10, PLANT_Y - 2.5f, 20f, 3.5f, indoor(P.TerracottaShade))
    }

    // Foreground ----------------------------------------------------------------------------------

    override fun foreground(p: ZenPainter, t: Float) {
        if (rain) dropsOnGlass(p, t)
        code(p, t)
        steam(p, t)
        plantLeaves(p, t)
        secondHand(p, t)
    }

    /**
     * Lines of code typing themselves on the screen, eight keystrokes per line, then the screen
     * fades and the loop starts again. The keyboard lights up only while a line is being typed.
     */
    private fun code(p: ZenPainter, t: Float) {
        val phase = (t % CODE_CYCLE) / CODE_CYCLE
        val alpha = when {
            phase < 0.03f -> phase / 0.03f
            phase < 0.88f -> 1f
            phase < 0.95f -> 1f - (phase - 0.88f) / 0.07f
            else -> 0f
        }
        var cursorX = SCR_X + 6
        var cursorY = lineY(0)
        var typing = false
        for (k in LINE_START.indices) {
            val start = LINE_START[k]
            val end = LINE_END[k]
            if (phase < start) break
            val progress = floor(((phase - start) / (end - start)).coerceIn(0f, 1f) * KEYSTROKES) / KEYSTROKES
            val x0 = SCR_X + 6 + LINE_INDENT[k] * 6f
            val y = lineY(k)
            val length = LINE_WIDTH[k] * progress
            if (length > 0f) p.line(x0, y, x0 + length, y, 2.4f, a(LINE_COLOR[k], alpha * if (LINE_COLOR[k] == P.CodeCream) 0.8f else 1f))
            cursorX = x0 + length + 2.4f
            cursorY = y
            typing = phase < end
        }
        if ((t % 1f) < 0.55f || typing) p.rect(cursorX, cursorY - 2.4f, 1.1f, 4.8f, a(P.CodeCursor, alpha))
        if (!typing) return
        val lit = a(P.KeyLit, 0.6f)
        for (k in LIT_KEYS.indices) {
            val period = LIT_PERIOD[k]
            if (((t + k * 0.23f) % period) / period < 0.3f) p.rect(keyX(LIT_KEYS[k]), KEY_Y + 1.4f, KEY_SIZE, 2.4f, lit)
        }
        if (((t + 0.4f) % 1.4f) / 1.4f < 0.25f) p.rect(KEY_X + 14, KEY_Y + 5f, KEY_W - 30, 2.4f, lit)
    }

    private fun lineY(k: Int) = SCR_Y + 11f + k * 6f

    private fun steam(p: ZenPainter, t: Float) {
        for (k in 0 until 2) {
            var y = MUG_TOP - 2
            var prevX = MUG_X - 2 + k * 4
            var prevY = y
            while (y > MUG_TOP - 22) {
                y -= 2f
                val fade = (y - (MUG_TOP - 22)) / 20f
                val x = MUG_X - 2 + k * 4 + sin(y * 0.3f + t * 1.7f + k * 2.4f) * (1.8f - fade)
                p.line(prevX, prevY, x, y, 1.2f, a(P.Steam, 0.5f * fade))
                prevX = x
                prevY = y
            }
        }
    }

    /** The plant sways gently, about four degrees each way. */
    private fun plantLeaves(p: ZenPainter, t: Float) {
        val sway = sin(t * 2 * PI.toFloat() / PLANT_SWAY_SECONDS) * 0.07f
        for ((k, ang) in floatArrayOf(-2.5f, -2.05f, -1.57f, -1.1f, -0.65f).withIndex()) {
            val a = ang + sway * (1f + k * 0.1f)
            val len = 15f + hash(k, 73) % 7
            leaf(p, PLANT_X, PLANT_Y - 2, cos(a), sin(a), len, 3.2f, indoor(if (k % 2 == 0) P.StudioLeafDark else P.StudioLeaf))
        }
    }

    private fun secondHand(p: ZenPainter, t: Float) {
        val ang = floor(t % 60f) / 60f * 2 * PI.toFloat()
        p.line(CLOCK_X, CLOCK_Y, CLOCK_X + sin(ang) * (CLOCK_R - 3), CLOCK_Y - cos(ang) * (CLOCK_R - 3), 0.5f, indoor(P.Terracotta))
        p.circle(CLOCK_X, CLOCK_Y, 1f, indoor(P.Monitor))
    }

    /** Drops sliding down the glass on rainy days. */
    private fun dropsOnGlass(p: ZenPainter, t: Float) {
        for (i in 0 until 22) {
            val x = WIN_X + 2 + hash(i, 111) % (WIN_W - 4).toInt()
            val speed = 2f + hash(i, 112) % 5
            val y = WIN_Y + (t * speed + hash(i, 113)) % WIN_H
            val onMullion = (1 until PANES).any { abs(x - (WIN_X + it * WIN_W / PANES)) < 1.5f }
            val behindDesk = (x > MON_X - 2 && x < MON_X + MON_W + 2 && y > MON_Y - 2) || (x > PLANT_X - 22 && x < PLANT_X + 22 && y > PLANT_Y - 22)
            if (onMullion || behindDesk || x > WIN_X + WIN_W - 2) continue
            p.line(x, maxOf(WIN_Y, y - 5f), x, y, 0.5f, a(lit(P.Rain), 0.18f))
            p.circle(x, y, 0.8f, a(lit(P.Rain), 0.55f))
        }
    }

    companion object {
        const val WIN_X = 16f
        const val WIN_Y = 10f
        const val WIN_W = 200f
        const val DESK_Y = 118f
        const val WIN_H = DESK_Y - WIN_Y
        private const val PANES = 4
        private const val SKYLINE_BASE = 104f
        private const val LANDMARK_X = 64f
        private const val PLANE_CYCLE = 40f

        private const val MON_X = 178f
        private const val MON_Y = 44f
        private const val MON_W = 106f
        private const val MON_H = 66f
        private const val SCR_X = MON_X + 4
        private const val SCR_Y = MON_Y + 4
        private const val SCR_W = MON_W - 8
        private const val SCR_H = MON_H - 13

        private const val KEY_X = MON_X + MON_W / 2 - 34
        private const val KEY_Y = 123f
        private const val KEY_W = 68f
        private const val KEY_SIZE = 5.2f
        private const val KEYS_ROW = 10

        private const val MUG_X = 150f
        private const val MUG_TOP = 103f
        private const val PLANT_X = 36f
        private const val PLANT_Y = 106f
        private const val PLANT_SWAY_SECONDS = 10f
        private const val CLOCK_X = 262f
        private const val CLOCK_Y = 24f
        private const val CLOCK_R = 11f

        /** One loop of the code on screen, in seconds. */
        private const val CODE_CYCLE = 9f
        private const val KEYSTROKES = 8f
        private val LINE_START = floatArrayOf(0.02f, 0.12f, 0.26f, 0.34f, 0.50f, 0.60f, 0.68f)
        private val LINE_END = floatArrayOf(0.10f, 0.24f, 0.32f, 0.48f, 0.58f, 0.66f, 0.72f)
        private val LINE_INDENT = intArrayOf(0, 1, 1, 2, 2, 1, 0)
        private val LINE_WIDTH = floatArrayOf(36f, 52f, 28f, 60f, 40f, 24f, 16f)
        private val LINE_COLOR = intArrayOf(P.CodeAmber, P.CodeCream, P.CodeSage, P.CodeCoral, P.CodeCream, P.CodeSage, P.CodeAmber)
        private val LIT_KEYS = intArrayOf(1, 4, 7, 3)
        private val LIT_PERIOD = floatArrayOf(0.7f, 0.7f, 0.7f, 0.9f)

        /** Glass towers in front of the landmark: x, width, top. */
        private val TOWERS = arrayOf(
            floatArrayOf(14f, 20f, 58f), floatArrayOf(86f, 16f, 50f), floatArrayOf(104f, 22f, 64f),
            floatArrayOf(130f, 14f, 44f), floatArrayOf(148f, 24f, 60f), floatArrayOf(176f, 18f, 52f),
            floatArrayOf(198f, 20f, 66f),
        )
    }
}
