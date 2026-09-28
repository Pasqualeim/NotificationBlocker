package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Working hours: the view from the desk. A large office window on the city (same real sky, sun or
 * moon; lit windows after dark; park trees in the season's colors), a laptop, a steaming mug, a
 * notebook and a plant. Structured and calm, so the landscape after work reads as the contrast.
 *
 * Indoor colors use [indoor]: daylight during the day, warm lamp light when it gets dark outside.
 * After dark the laptop switches to a dark screen that casts a cool light on the desk; in the day
 * the sun draws the window panes on the desk. On rainy days the sky is grey and rain runs on the glass.
 */
class DeskPainting(env: ZenState) : ScenePainting(env) {

    private val indoorLight = ZenColor.mix(light.ambient, P.IndoorLight, light.lantern * 0.85f)

    private fun indoor(color: Int) = ZenColor.multiply(color, indoorLight)

    /** 0 = light screen (day), 1 = dark screen (night). */
    private val screenNight = light.lantern.coerceIn(0f, 1f)
    private val screenBg = ZenColor.mix(P.ScreenBg, P.ScreenBgNight, screenNight)

    /** Sun and moon move across the window instead of the whole scene. */
    private val sunX = WIN_X + sun.x * WIN_W
    private val sunY = WIN_Y + 6 + (sun.y - 0.1f) / 0.54f * (WIN_H - 4)

    override fun sky(p: ZenPainter) {
        skyGradient(p, WIN_Y + WIN_H)
        sunOrMoon(p, sunX, sunY)
        overcast(p, WIN_Y + WIN_H)
    }

    override fun skyMotion(p: ZenPainter, t: Float) {
        stars(p, t, maxY = 70)
        clouds(p, t, top = 22f)
        birds(p, t, top = 40f)
        plane(p, t)
    }

    /** Once in a while a plane crosses high up, with a blinking light after dark. */
    private fun plane(p: ZenPainter, t: Float) {
        val cycle = t % PLANE_CYCLE
        if (cycle > 26f || rain) return
        val x = WIN_X - 10 + cycle / 26f * (WIN_W + 20)
        val y = 30f - cycle * 0.25f
        p.line(x - 4, y, x + 4, y - 0.6f, 1.3f, lit(P.Plane))
        p.line(x - 1, y - 0.2f, x - 3, y + 1.6f, 0.9f, lit(P.Plane))
        if (light.lantern > 0.3f && (t * 1.2f) % 1f < 0.3f) p.circle(x + 4, y - 0.6f, 0.9f, P.PlaneLight)
    }

    // Room ----------------------------------------------------------------------------------------

    override fun landscape(p: ZenPainter) {
        city(p)
        window(p)
        desk(p)
        sunPatch(p)
        laptop(p)
        notebook(p)
        mug(p)
        plant(p)
    }

    private fun city(p: ZenPainter) {
        val bottom = WIN_Y + WIN_H
        // Far skyline, hazy with distance
        val far = ZenColor.mix(lit(P.BuildingFar), light.skyLow, 0.5f)
        var x = WIN_X - 4
        var i = 0
        while (x < WIN_X + WIN_W) {
            val w = 12f + hash(i, 91) % 14
            val top = 50f + hash(i, 92) % 26
            p.rect(x, top, w, bottom - top, far)
            if (hash(i, 93) % 4 == 0) p.line(x + w / 2, top, x + w / 2, top - 6, 0.7f, far)
            x += w + 1
            i++
        }
        // Near buildings with their window grid
        val near = lit(P.BuildingNear)
        val side = lit(P.BuildingShade)
        val glass = ZenColor.mix(near, light.skyLow, 0.35f)
        val warm = ZenColor.mix(glass, P.LanternLight, light.lantern)
        x = WIN_X - 6
        i = 0
        while (x < WIN_X + WIN_W) {
            val w = 18f + hash(i, 94) % 18
            val top = 66f + hash(i, 95) % 22
            p.rect(x, top, w, bottom - top, near)
            p.rect(x + w - 3, top, 3f, bottom - top, side)
            var wy = top + 4
            var row = 0
            while (wy < bottom - 4) {
                var wx = x + 3
                var col = 0
                while (wx < x + w - 5) {
                    p.rect(wx, wy, 2.4f, 3f, if (hash(i, row, col) % 10 < 4) warm else glass)
                    wx += 5
                    col++
                }
                wy += 6
                row++
            }
            x += w + 2
            i++
        }
        // Park trees at the foot of the buildings, in the season's colors
        val tree = lit(flora.tree)
        val treeDark = lit(flora.treeDark)
        x = WIN_X
        i = 0
        while (x < WIN_X + WIN_W + 6) {
            val r = 4f + hash(i, 96) % 4
            p.circle(x, bottom - r * 0.4f + 1, r, treeDark)
            p.circle(x - 1, bottom - r * 0.6f, r * 0.75f, tree)
            x += 7f + hash(i, 97) % 5
            i++
        }
    }

    private fun window(p: ZenPainter) {
        val wall = indoor(P.Wall)
        val frame = indoor(P.WindowFrame)
        val frameShade = indoor(P.WindowFrameShade)
        val right = WIN_X + WIN_W
        val bottom = WIN_Y + WIN_H
        // Faint reflections on the glass
        for (k in 0 until 3) {
            val x0 = WIN_X + 30 + k * 92
            begin()
            pt(x0, WIN_Y)
            pt(x0 + 14, WIN_Y)
            pt(x0 - 26, bottom)
            pt(x0 - 40, bottom)
            fill(p, a(P.Cloud, 0.05f + 0.03f * light.rays))
        }
        // Wall around the opening
        p.rect(0f, 0f, W, WIN_Y, wall)
        p.rect(0f, 0f, WIN_X, H, wall)
        p.rect(right, 0f, W - right, H, wall)
        p.rect(0f, bottom, W, H - bottom, wall)
        p.verticalGradient(0f, bottom + 6, W, 20f, a(indoor(P.WallShade), 0f), indoor(P.WallShade))
        // Frame, mullions and sill
        p.rect(WIN_X - 4, WIN_Y - 4, WIN_W + 8, 4f, frame)
        p.rect(WIN_X - 4, WIN_Y, 4f, WIN_H, frame)
        p.rect(right, WIN_Y, 4f, WIN_H, frame)
        for (mx in floatArrayOf(WIN_X + WIN_W / 3, WIN_X + WIN_W * 2 / 3)) {
            p.rect(mx - 1.5f, WIN_Y, 3f, WIN_H, frame)
            p.rect(mx + 0.8f, WIN_Y, 0.7f, WIN_H, frameShade)
        }
        p.rect(WIN_X - 8, bottom, WIN_W + 16, 4f, frame)
        p.rect(WIN_X - 8, bottom + 4, WIN_W + 16, 1.5f, frameShade)
    }

    private fun desk(p: ZenPainter) {
        p.rect(0f, DESK_Y, W, H - DESK_Y, indoor(P.Desk))
        p.rect(0f, DESK_Y, W, 1.5f, indoor(P.DeskLight))
        p.verticalGradient(0f, DESK_Y + 2, W, H - DESK_Y - 8, a(indoor(P.DeskLight), 0.25f), a(indoor(P.DeskLight), 0f))
        p.rect(0f, H - 5, W, 5f, indoor(P.DeskEdge))
    }

    /** The three window panes drawn by the sun on the desk, sliding as the sun crosses the sky. */
    private fun sunPatch(p: ZenPainter) {
        if (!sun.isSun || rain || light.rays < 0.05f) return
        val shift = (0.5f - sun.x) * 70f
        val pane = WIN_W / 3
        val color = a(P.SunPatch, SUN_PATCH_ALPHA * light.rays)
        for (i in 0 until 3) {
            val x0 = WIN_X + i * pane + 5 + shift
            begin()
            pt(x0, DESK_Y + 1.5f)
            pt(x0 + pane - 10, DESK_Y + 1.5f)
            pt(x0 + pane - 10 + shift * 0.5f, H - 6)
            pt(x0 + shift * 0.5f, H - 6)
            fill(p, color)
        }
    }

    private fun laptop(p: ZenPainter) {
        val body = indoor(P.LaptopBody)
        p.rect(LAPTOP_X - 2, 86f, 76f, 40f, body)
        begin()
        pt(LAPTOP_X - 8, 126f)
        pt(LAPTOP_X + 80, 126f)
        pt(LAPTOP_X + 86, 131f)
        pt(LAPTOP_X - 14, 131f)
        fill(p, indoor(P.LaptopBase))
        p.rect(LAPTOP_X - 14, 131f, 100f, 1.2f, indoor(P.LaptopBody))
        // Screen: emissive, so not dimmed by the room light
        val sx = LAPTOP_X + 1
        val sy = 89f
        val line = ZenColor.mix(P.ScreenLine, P.ScreenLineNight, screenNight)
        p.rect(sx, sy, 70f, 34f, screenBg)
        p.rect(sx, sy, 70f, 4f, P.ScreenHeader)
        p.rect(sx, sy + 4, 14f, 30f, ZenColor.mix(P.ScreenSide, P.ScreenSideNight, screenNight))
        for (k in 0 until 4) p.rect(sx + 3, sy + 8 + k * 5, 8f, 1.4f, line)
        for (k in 0 until 3) p.rect(sx + 18, sy + 8 + k * 4, 22f + (hash(k, 71) % 20), 1.4f, line)
        // The chart bars move: see foreground
    }

    private fun notebook(p: ZenPainter) {
        begin()
        pt(150f, 136f)
        pt(198f, 134f)
        pt(204f, 150f)
        pt(152f, 153f)
        fill(p, indoor(P.Paper))
        for (k in 1..4) p.line(152f + k * 0.4f, 136f + k * 3.3f, 199f + k * 1.2f, 134f + k * 3.1f, 0.5f, indoor(P.PaperLine))
        p.line(186f, 150f, 208f, 140f, 1.4f, indoor(P.Pen))
    }

    private fun mug(p: ZenPainter) {
        val mug = indoor(P.Mug)
        p.rect(MUG_X - 7, 114f, 14f, 16f, mug)
        p.ellipse(MUG_X, 130f, 7f, 1.6f, mug)
        p.rect(MUG_X + 3, 114f, 4f, 16f, indoor(P.MugShade))
        p.ellipse(MUG_X, 114f, 7f, 1.8f, indoor(P.MugShade))
        p.ellipse(MUG_X, 114.2f, 5.8f, 1.3f, indoor(P.Coffee))
        p.line(MUG_X + 7, 118f, MUG_X + 10.5f, 119.5f, 1.6f, mug)
        p.line(MUG_X + 10.5f, 119.5f, MUG_X + 10.5f, 124f, 1.6f, mug)
        p.line(MUG_X + 10.5f, 124f, MUG_X + 7, 126f, 1.6f, mug)
    }

    private fun plant(p: ZenPainter) {
        val x = PLANT_X
        val leafLight = indoor(P.Plant)
        val leafDark = indoor(P.PlantDark)
        for ((k, ang) in floatArrayOf(-2.6f, -2.2f, -1.9f, -1.6f, -1.3f, -1.0f, -0.6f).withIndex()) {
            val len = 16f + hash(k, 73) % 8
            leaf(p, x, 110f, cos(ang), sin(ang), len, 2.6f, if (k % 2 == 0) leafDark else leafLight)
        }
        begin()
        pt(x - 10, 110f)
        pt(x + 10, 110f)
        pt(x + 7, 130f)
        pt(x - 7, 130f)
        fill(p, indoor(P.Pot))
        p.rect(x - 11, 108f, 22f, 3f, indoor(P.PotShade))
    }

    // Foreground ----------------------------------------------------------------------------------

    override fun foreground(p: ZenPainter, t: Float) {
        if (rain) rainOnGlass(p, t)
        // The chart on the screen, its bars slowly rising and falling
        for (k in 0 until 6) {
            val h = (4f + hash(k, 72) % 9) * (0.78f + 0.22f * sin(t * 0.55f + k * 1.3f))
            p.rect(LAPTOP_X + 19 + k * 7, 89f + 31 - h, 4f, h, if (k == 4) P.ScreenAccent else P.ScreenBar)
        }
        // Steam curling up from the mug
        for (k in 0 until 3) {
            var y = 111f
            var prevX = MUG_X - 3 + k * 3
            var prevY = y
            while (y > 90f) {
                y -= 2f
                val fade = (y - 90f) / 21f
                val x = MUG_X - 3 + k * 3 + sin(y * 0.28f + t * 1.6f + k * 2f) * (1.8f - fade)
                p.line(prevX, prevY, x, y, 1.1f, a(P.Steam, 0.45f * fade))
                prevX = x
                prevY = y
            }
        }
        // Cursor blinking at the end of the text line
        val cursor = ZenColor.mix(P.ScreenHeader, P.ScreenBar, screenNight)
        if ((t * 1.1f) % 1f < 0.55f) p.rect(LAPTOP_X + 42.5f + hash(0, 71) % 20, 96.3f, 0.9f, 2.8f, cursor)
        // After dark, the screen casts a cool light on the desk in front of it
        if (screenNight > 0.05f) {
            p.glow(LAPTOP_X + 36, 130f, 60f, a(P.ScreenGlow, 0.2f * screenNight))
            p.glow(LAPTOP_X + 36, 106f, 46f, a(P.ScreenGlow, 0.1f * screenNight))
        }
    }

    /** Rain falling outside and drops sliding down the glass. */
    private fun rainOnGlass(p: ZenPainter, t: Float) {
        val bottom = WIN_Y + WIN_H
        for (i in 0 until 70) rainDrop(p, t, i, WIN_X, WIN_Y, WIN_X + WIN_W, bottom, 0.35f)
        for (i in 0 until 16) {
            val x = WIN_X + 2 + hash(i, 111) % (WIN_W - 4).toInt()
            val speed = 2f + hash(i, 112) % 5
            val y = WIN_Y + (t * speed + hash(i, 113)) % WIN_H
            if (rainHidden(x, y)) continue
            p.line(x, maxOf(WIN_Y, y - 6f), x, y, 0.5f, a(lit(P.Rain), 0.18f))
            p.circle(x, y, 0.9f, a(lit(P.Rain), 0.55f))
        }
    }

    override fun rainHidden(x: Float, y: Float): Boolean =
        (x > LAPTOP_X - 4 && x < LAPTOP_X + 76 && y > 84f) || // laptop
            (x > PLANT_X - 22 && x < PLANT_X + 22 && y > 86f) || // plant
            abs(x - (WIN_X + WIN_W / 3)) < 2f || abs(x - (WIN_X + WIN_W * 2 / 3)) < 2f // mullions

    companion object {
        const val WIN_X = 22f
        const val WIN_Y = 10f
        const val WIN_W = 276f
        const val WIN_H = 96f
        const val DESK_Y = 128f
        const val LAPTOP_X = 38f
        const val MUG_X = 236f
        const val PLANT_X = 282f
        private const val PLANE_CYCLE = 45f
        private const val SUN_PATCH_ALPHA = 0.22f
    }
}
