package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette as P
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Working hours, lo-fi style: a cozy room with a window on the city (same real sky, sun or moon;
 * lit windows after dark; park trees in the season's colors). The character, headphones on, works
 * at the desk; a cat watches the street from the sill, the mug steams, the lamp takes over at dusk.
 *
 * Indoor colors go through [indoor]: daylight during the day, warm lamp light once it is dark.
 * Light sources (screen, bulb, lit windows) are emissive and skip it.
 */
class DeskPainting(env: ZenState) : ScenePainting(env) {

    private val indoorLight = ZenColor.mix(light.ambient, P.IndoorLight, light.lantern * 0.85f)

    private fun indoor(color: Int) = ZenColor.multiply(color, indoorLight)

    /** Sun and moon move across the window instead of the whole scene. */
    private val sunX = WIN_X + sun.x * WIN_W
    private val sunY = WIN_Y + 8 + (sun.y - 0.1f) / 0.54f * (WIN_H - 12)

    override fun sky(p: ZenPainter) {
        skyGradient(p, WIN_Y + WIN_H)
        sunOrMoon(p, sunX, sunY)
    }

    override fun skyMotion(p: ZenPainter, t: Float) {
        stars(p, t, maxY = 60)
        clouds(p, t, top = WIN_Y + 8)
        birds(p, t, top = WIN_Y + 14)
    }

    // Room (still, cached) ------------------------------------------------------------------------

    override fun landscape(p: ZenPainter) {
        city(p)
        walls(p)
        window(p)
        curtains(p)
        shelf(p)
        wallArt(p)
        floor(p)
        chair(p)
        legs(p)
        desk(p)
        lamp(p)
        laptop(p)
        mug(p)
        plant(p)
        cat(p)
    }

    private fun city(p: ZenPainter) {
        val bottom = WIN_Y + WIN_H
        // Far skyline, hazy with distance
        val far = ZenColor.mix(lit(P.BuildingFar), light.skyLow, 0.45f)
        var x = WIN_X - 4
        var i = 0
        while (x < WIN_X + WIN_W) {
            val w = 7f + hash(i, 91) % 8
            val top = WIN_Y + 30f + hash(i, 92) % 14
            p.rect(x, top, w, bottom - top, far)
            x += w + 0.5f
            i++
        }
        // Near buildings with their window grid, lit after dark
        val near = lit(P.BuildingNear)
        val side = lit(P.BuildingShade)
        val glass = lit(P.CityWindow)
        val warm = ZenColor.mix(glass, P.CityLight, light.lantern)
        x = WIN_X - 5
        i = 0
        while (x < WIN_X + WIN_W) {
            val w = 11f + hash(i, 94) % 10
            val top = WIN_Y + 40f + hash(i, 95) % 12
            p.rect(x, top, w, bottom - top, near)
            p.rect(x + w - 2, top, 2f, bottom - top, side)
            var wy = top + 3
            var row = 0
            while (wy < bottom - 3) {
                var wx = x + 2
                var col = 0
                while (wx < x + w - 4) {
                    p.rect(wx, wy, 1.6f, 2f, if (hash(i, row, col) % 10 < 4) warm else glass)
                    wx += 3.4f
                    col++
                }
                wy += 4
                row++
            }
            x += w + 1
            i++
        }
        // Park trees at the foot of the buildings, in the season's colors
        val tree = lit(flora.tree)
        val treeDark = lit(flora.treeDark)
        x = WIN_X
        i = 0
        while (x < WIN_X + WIN_W + 4) {
            val r = 3f + hash(i, 96) % 3
            p.circle(x, bottom - r * 0.3f + 1, r, treeDark)
            p.circle(x - 0.8f, bottom - r * 0.6f, r * 0.72f, tree)
            x += 5.5f + hash(i, 97) % 4
            i++
        }
    }

    private fun walls(p: ZenPainter) {
        val wall = indoor(P.RoomWall)
        val right = WIN_X + WIN_W
        val bottom = WIN_Y + WIN_H
        p.rect(0f, 0f, W, WIN_Y, wall)
        p.rect(0f, 0f, WIN_X, H, wall)
        p.rect(right, 0f, W - right, H, wall)
        p.rect(0f, bottom, W, H - bottom, wall)
        // Soft light falling from the window onto the lower wall during the day
        p.verticalGradient(WIN_X - 20, bottom, WIN_W + 40, 30f, a(P.LampLight, 0.10f * light.rays), a(P.LampLight, 0f))
        p.rect(0f, BAND_Y, W, FLOOR_Y - 3 - BAND_Y, indoor(P.RoomWallBand))
        p.rect(0f, BAND_Y, W, 1f, indoor(P.WoodLight))
    }

    private fun window(p: ZenPainter) {
        val frame = indoor(P.Wood)
        val shade = indoor(P.WoodDark)
        val right = WIN_X + WIN_W
        val bottom = WIN_Y + WIN_H
        // Faint reflections on the glass
        for (k in 0 until 2) {
            val x0 = WIN_X + 22 + k * 52
            begin()
            pt(x0, WIN_Y)
            pt(x0 + 9, WIN_Y)
            pt(x0 - 14, bottom)
            pt(x0 - 23, bottom)
            fill(p, a(P.Steam, 0.05f + 0.04f * light.rays))
        }
        p.rect(WIN_X - 3, WIN_Y - 3, WIN_W + 6, 3f, frame)
        p.rect(WIN_X - 3, WIN_Y, 3f, WIN_H, frame)
        p.rect(right, WIN_Y, 3f, WIN_H, frame)
        p.rect(WIN_X + WIN_W / 2 - 1.2f, WIN_Y, 2.4f, WIN_H, frame)
        p.rect(WIN_X, WIN_Y + WIN_H * 0.45f - 1.2f, WIN_W, 2.4f, frame)
        // Sill: the cat's spot
        p.rect(WIN_X - 7, bottom, WIN_W + 14, 3.5f, frame)
        p.rect(WIN_X - 7, bottom + 3.5f, WIN_W + 14, 1.2f, shade)
    }

    private fun curtains(p: ZenPainter) {
        val curtain = indoor(P.Curtain)
        val fold = indoor(P.CurtainFold)
        p.line(WIN_X - 22, WIN_Y - 5, WIN_X + WIN_W + 22, WIN_Y - 5, 1.4f, indoor(P.Lamp))
        for (side in 0..1) {
            val x0 = if (side == 0) WIN_X - 17 else WIN_X + WIN_W + 3
            begin()
            pt(x0, WIN_Y - 5)
            pt(x0 + 14, WIN_Y - 5)
            pt(x0 + 13 + (if (side == 0) 1f else -1f) * 2, WIN_Y + WIN_H + 14)
            pt(x0 + 7, WIN_Y + WIN_H + 17)
            pt(x0 + 1 - (if (side == 0) 1f else -1f) * 1, WIN_Y + WIN_H + 14)
            fill(p, curtain)
            for (k in 1..2) p.line(x0 + k * 4.6f, WIN_Y - 3, x0 + k * 4.6f + (k - 1.5f), WIN_Y + WIN_H + 14, 0.9f, fold)
        }
    }

    private fun shelf(p: ZenPainter) {
        val books = intArrayOf(P.Book1, P.Book3, P.Book2, P.Book4, P.Book1, P.Book3)
        var x = SHELF_X + 3
        for ((k, color) in books.withIndex()) {
            val w = 3.2f + hash(k, 41) % 2
            val h = 9f + hash(k, 42) % 5
            if (k == 4) {
                // One book leaning on the others
                begin()
                pt(x, SHELF_Y)
                pt(x + w, SHELF_Y)
                pt(x + w + 4, SHELF_Y - h + 1)
                pt(x + 4, SHELF_Y - h + 1)
                fill(p, indoor(color))
                x += 8
            } else {
                p.rect(x, SHELF_Y - h, w, h, indoor(color))
                p.rect(x, SHELF_Y - h + 2, w, 0.8f, indoor(P.ClockFace))
                x += w + 0.4f
            }
        }
        // A small trailing plant at the end of the shelf
        val px = SHELF_X + 44
        p.rect(px - 3.5f, SHELF_Y - 6, 7f, 6f, indoor(P.Pot))
        for (k in 0 until 5) {
            val ang = -2.4f + k * 0.45f
            leaf(p, px, SHELF_Y - 6, cos(ang), sin(ang), 6f + k % 2 * 2, 1.4f, indoor(if (k % 2 == 0) P.Plant else P.PlantDark))
        }
        for (k in 0 until 4) leaf(p, px + 2.5f, SHELF_Y + 1 + k * 3.5f, 0.5f, 1f, 3.5f, 1.1f, indoor(P.PlantDark))
        p.rect(SHELF_X, SHELF_Y, 50f, 2.5f, indoor(P.Wood))
        p.rect(SHELF_X + 4, SHELF_Y + 2.5f, 1.5f, 4f, indoor(P.WoodDark))
        p.rect(SHELF_X + 44, SHELF_Y + 2.5f, 1.5f, 4f, indoor(P.WoodDark))
        // Wall clock (the hands move in the foreground)
        p.circle(CLOCK_X, CLOCK_Y, 7.5f, indoor(P.Wood))
        p.circle(CLOCK_X, CLOCK_Y, 6.3f, indoor(P.ClockFace))
        for (k in 0 until 12 step 3) {
            val ang = k * PI.toFloat() / 6
            p.circle(CLOCK_X + cos(ang) * 5f, CLOCK_Y + sin(ang) * 5f, 0.5f, indoor(P.Lamp))
        }
    }

    private fun wallArt(p: ZenPainter) {
        // Sticky notes and a small framed print on the right wall
        p.rect(226f, 30f, 7f, 7f, indoor(P.Note))
        p.rect(235f, 33f, 7f, 7f, indoor(P.Note2))
        p.rect(228f, 40f, 7f, 7f, indoor(P.Note))
        p.rect(252f, 22f, 26f, 20f, indoor(P.Wood))
        p.rect(254f, 24f, 22f, 16f, indoor(P.ClockFace))
        begin()
        pt(255f, 39f)
        pt(262f, 29f)
        pt(267f, 34f)
        pt(270f, 31f)
        pt(275f, 39f)
        fill(p, indoor(P.Book2))
        p.circle(270f, 28f, 1.8f, indoor(P.LampShade))
    }

    private fun floor(p: ZenPainter) {
        p.rect(0f, FLOOR_Y - 3, W, 3f, indoor(P.WoodDark))
        p.rect(0f, FLOOR_Y, W, H - FLOOR_Y, indoor(P.Floor))
        for (k in 1..3) p.rect(0f, FLOOR_Y + k * 9f, W, 0.7f, indoor(P.FloorLine))
        p.ellipse(186f, 146f, 86f, 9f, indoor(P.Rug))
        p.ellipse(186f, 146f, 74f, 6.5f, indoor(P.RugInner))
        p.ellipse(186f, 146f, 64f, 5f, indoor(P.Rug))
    }

    private fun chair(p: ZenPainter) {
        val wood = indoor(P.Chair)
        val woodDark = indoor(P.ChairDark)
        val cushion = indoor(P.ChairCushion)

        // Far chair legs (shadowed/behind)
        p.line(116.5f, 106.5f, 114.5f, FLOOR_Y, 1.5f, woodDark)
        p.line(131.5f, 106.5f, 133f, FLOOR_Y, 1.5f, woodDark)

        // Backrest frame & vertical posts
        p.rect(110.5f, 75f, 4.5f, 18f, wood) // Backrest main vertical post
        p.rect(111f, 74f, 5f, 4f, wood) // Top rounded crown of backrest
        p.rect(112.5f, 92f, 2.5f, 12f, wood) // Lower back support post
        p.line(113.5f, 78f, 113.5f, 92f, 1.2f, woodDark) // Spindle line detail

        // Wooden seat frame
        p.rect(111.5f, 104f, 24.5f, 3f, wood)
        p.rect(111.5f, 106f, 24.5f, 1f, woodDark) // seat underside shadow

        // Seat cushion (warm padded cushion on top)
        begin()
        pt(112f, 104f)
        pt(135.5f, 104f)
        pt(136f, 102.5f)
        pt(112.5f, 102.5f)
        fill(p, cushion)

        // Near chair legs (in front)
        p.line(114.5f, 106.5f, 112.5f, FLOOR_Y, 2f, wood)
        p.line(133.5f, 106.5f, 135.5f, FLOOR_Y, 2f, wood)

        // Lower stability rung (footrest crossbar between legs)
        p.line(113.5f, 117f, 134.5f, 117f, 1.3f, woodDark)
    }

    private fun legs(p: ZenPainter) {
        val pants = indoor(P.Pants)
        val pantsShade = indoor(P.PantsShade)
        val sock = indoor(P.Sock)
        val slipper = indoor(P.Slipper)

        // --- Far Leg (Behind) ---
        // Far thigh
        begin()
        pt(118f, 98.5f)
        pt(138.5f, 99.5f)
        pt(140.5f, 102.5f)
        pt(138f, 105.5f)
        pt(118f, 104.5f)
        fill(p, pantsShade)

        // Far shin
        begin()
        pt(135f, 102f)
        pt(140.5f, 102.5f)
        pt(139.5f, 121.5f)
        pt(135.5f, 121.5f)
        fill(p, pantsShade)

        // Far sock & slipper on floor
        p.rect(136f, 121f, 3.5f, 2f, sock)
        p.ellipse(138f, 123.8f, 4.5f, 1.8f, slipper)

        // --- Near Leg (In Front) ---
        // Near thigh (horizontal on seat)
        begin()
        pt(119f, 97.5f)
        pt(140f, 98.5f)
        pt(142.5f, 101.5f)
        pt(140.5f, 105f)
        pt(119f, 104.5f)
        fill(p, pants)
        p.ellipse(141f, 101.5f, 2.2f, 2.8f, pants) // rounded knee curve

        // Near shin (vertical down to floor)
        begin()
        pt(137f, 101.5f)
        pt(142.5f, 101.5f)
        pt(141.5f, 121.5f)
        pt(137f, 121.5f)
        fill(p, pants)

        // Pants cuff fold above ankle
        p.rect(137f, 120f, 4.5f, 1.5f, pantsShade)

        // Near sock
        p.rect(137.5f, 121f, 3.8f, 2.2f, sock)

        // Near cozy slipper on the floor
        p.ellipse(141f, 123.6f, 5.2f, 2f, slipper)
        p.ellipse(141f, 122.8f, 4.2f, 1.2f, indoor(P.Headphones)) // slipper collar detail
    }

    private fun desk(p: ZenPainter) {
        val top = indoor(P.Wood)
        val side = indoor(P.WoodDark)
        p.rect(DESK_X, DESK_Y, DESK_W, 3.5f, top)
        p.rect(DESK_X, DESK_Y, DESK_W, 0.8f, indoor(P.WoodLight))
        p.rect(DESK_X + 2, DESK_Y + 3.5f, DESK_W - 4, 3f, side)
        p.rect(DESK_X + 3, DESK_Y + 6, 3f, FLOOR_Y - DESK_Y - 6, side)
        p.rect(DESK_X + DESK_W - 30, DESK_Y + 6, 27f, 16f, top)
        p.rect(DESK_X + DESK_W - 30, DESK_Y + 13.5f, 27f, 0.8f, side)
        p.rect(DESK_X + DESK_W - 18, DESK_Y + 9, 4f, 1.2f, indoor(P.Lamp))
        p.rect(DESK_X + DESK_W - 18, DESK_Y + 17, 4f, 1.2f, indoor(P.Lamp))
        p.rect(DESK_X + DESK_W - 6, DESK_Y + 22, 3f, FLOOR_Y - DESK_Y - 22, side)
    }

    private fun lamp(p: ZenPainter) {
        val dark = indoor(P.Lamp)
        p.ellipse(LAMP_X, DESK_Y, 6f, 1.6f, dark)
        p.line(LAMP_X, DESK_Y, LAMP_X - 5, 80f, 1.4f, dark)
        p.line(LAMP_X - 5, 80f, LAMP_X - 16, 74f, 1.4f, dark)
        p.circle(LAMP_X - 5, 80f, 1.3f, dark)
        begin()
        pt(LAMP_X - 23, 72f)
        pt(LAMP_X - 13, 70f)
        pt(LAMP_X - 10, 77f)
        pt(LAMP_X - 25, 80f)
        fill(p, indoor(P.LampShade))
        // The bulb glows once it gets dark
        p.circle(LAMP_X - 17.5f, 79.5f, 2f, ZenColor.mix(indoor(P.ClockFace), P.LampLight, light.lantern))
    }

    private fun laptop(p: ZenPainter) {
        val body = indoor(P.Laptop)
        begin()
        pt(LAPTOP_X, DESK_Y)
        pt(LAPTOP_X + 30, DESK_Y)
        pt(LAPTOP_X + 33, DESK_Y - 2.5f)
        pt(LAPTOP_X + 3, DESK_Y - 2.5f)
        fill(p, indoor(P.LaptopShade))
        begin()
        pt(LAPTOP_X + 3, DESK_Y - 2.5f)
        pt(LAPTOP_X + 33, DESK_Y - 2.5f)
        pt(LAPTOP_X + 30, DESK_Y - 22f)
        pt(LAPTOP_X + 6, DESK_Y - 21.5f)
        fill(p, body)
        // Screen: emissive, so not dimmed by the room light
        begin()
        pt(LAPTOP_X + 5, DESK_Y - 4f)
        pt(LAPTOP_X + 31, DESK_Y - 4f)
        pt(LAPTOP_X + 28.5f, DESK_Y - 20.5f)
        pt(LAPTOP_X + 7.5f, DESK_Y - 20f)
        fill(p, P.Screen)
        for (k in 0 until 4) {
            val y = DESK_Y - 17.5f + k * 3.2f
            val w = 9f + hash(k, 71) % 9
            p.rect(LAPTOP_X + 9.5f - k * 0.3f, y, w, 1f, if (k == 2) P.ScreenAccent else P.ScreenLine)
        }
    }

    private fun mug(p: ZenPainter) {
        val mug = indoor(P.Mug)
        p.rect(MUG_X - 4, DESK_Y - 9, 8f, 9f, mug)
        p.rect(MUG_X - 4, DESK_Y - 6, 8f, 2f, indoor(P.MugBand))
        p.ellipse(MUG_X, DESK_Y - 9, 4f, 1.1f, indoor(P.Hair))
        p.line(MUG_X + 4, DESK_Y - 7, MUG_X + 6.5f, DESK_Y - 6, 1.3f, mug)
        p.line(MUG_X + 6.5f, DESK_Y - 6, MUG_X + 6.5f, DESK_Y - 3, 1.3f, mug)
        p.line(MUG_X + 6.5f, DESK_Y - 3, MUG_X + 4, DESK_Y - 2, 1.3f, mug)
    }

    private fun plant(p: ZenPainter) {
        val x = PLANT_X
        for ((k, ang) in floatArrayOf(-2.7f, -2.35f, -2.0f, -1.75f, -1.5f, -1.25f, -0.95f, -0.6f).withIndex()) {
            val len = 22f + hash(k, 73) % 12
            leaf(p, x, 136f, cos(ang), sin(ang), len, 3.4f, indoor(if (k % 2 == 0) P.PlantDark else P.Plant))
        }
        begin()
        pt(x - 10, 135f)
        pt(x + 10, 135f)
        pt(x + 7.5f, 154f)
        pt(x - 7.5f, 154f)
        fill(p, indoor(P.Pot))
        p.rect(x - 11, 133f, 22f, 3f, indoor(P.PotShade))
    }

    private fun cat(p: ZenPainter) {
        val fur = indoor(P.Cat)
        val stripe = indoor(P.CatStripe)
        val sill = WIN_Y + WIN_H
        p.ellipse(CAT_X, sill - 5, 7f, 5.5f, fur)
        p.circle(CAT_X - 6, sill - 11, 4.2f, fur)
        begin()
        pt(CAT_X - 9.6f, sill - 13)
        pt(CAT_X - 9f, sill - 17.5f)
        pt(CAT_X - 6.5f, sill - 14.5f)
        fill(p, fur)
        begin()
        pt(CAT_X - 5.5f, sill - 14.8f)
        pt(CAT_X - 3.2f, sill - 17.8f)
        pt(CAT_X - 2.4f, sill - 13.2f)
        fill(p, fur)
        for (k in 0 until 3) p.line(CAT_X - 1 + k * 3, sill - 9.5f, CAT_X + k * 3, sill - 6, 0.8f, stripe)
        // Watching the street: eyes toward the window
        p.circle(CAT_X - 8.2f, sill - 11.5f, 0.55f, indoor(P.Hair))
        p.circle(CAT_X - 5.6f, sill - 11.5f, 0.55f, indoor(P.Hair))
    }

    // Foreground: the character and everything that moves ----------------------------------------

    override fun foreground(p: ZenPainter, t: Float) {
        catTail(p, t)
        clockHands(p, t)
        character(p, t)
        steam(p, t)
        // Cursor blinking at the end of the text line
        if ((t * 1.1f) % 1f < 0.55f) p.rect(LAPTOP_X + 22f, DESK_Y - 11.1f, 0.8f, 1.8f, P.ScreenLine)
        // After dark the lamp and the screen light the room
        if (light.lantern > 0.02f) {
            p.glow(LAMP_X - 17, 84f, 75f, a(P.LampLight, 0.30f * light.lantern))
            p.glow(LAPTOP_X + 16, DESK_Y - 12, 28f, a(P.ScreenLine, 0.18f * light.lantern))
        }
    }

    private fun catTail(p: ZenPainter, t: Float) {
        val fur = indoor(P.Cat)
        val sill = WIN_Y + WIN_H
        val swing = sin(t * 2 * PI.toFloat() / 2.4f) * 0.55f
        var x = CAT_X + 6
        var y = sill - 2
        for (k in 1..5) {
            val ang = -0.2f - k * 0.32f + swing * k / 5
            val nx = x + cos(ang) * 2.4f
            val ny = y + sin(ang) * 2.4f
            p.line(x, y, nx, ny, 2.2f - k * 0.2f, fur)
            x = nx
            y = ny
        }
    }

    private fun clockHands(p: ZenPainter, t: Float) {
        val hand = indoor(P.Lamp)
        val minute = t * 0.2f - PI.toFloat() / 2
        p.line(CLOCK_X, CLOCK_Y, CLOCK_X + cos(minute) * 5f, CLOCK_Y + sin(minute) * 5f, 0.7f, hand)
        p.line(CLOCK_X, CLOCK_Y, CLOCK_X + 3f, CLOCK_Y - 1.2f, 0.9f, hand)
    }

    private fun character(p: ZenPainter, t: Float) {
        // Head bob to music (0 - 0.8 units), shoulders follow (~35%)
        val bob = (sin(t * 2 * PI.toFloat() * 0.75f) * 0.5f + 0.5f) * 0.8f
        val sh = bob * 0.35f
        val hoodie = indoor(P.Hoodie)
        val hoodieShade = indoor(P.HoodieShade)
        val skin = indoor(P.Skin)
        val skinShade = indoor(P.SkinShade)
        val hair = indoor(P.Hair)
        val cheek = a(P.Cheek, 0.65f)
        val phones = indoor(P.Headphones)
        val tapNear = if (sin(t * 11f) > 0.2f) -0.6f else 0f
        val tapFar = if (sin(t * 11f + PI.toFloat()) > 0.2f) -0.6f else 0f
        val handY = DESK_Y - 3.2f

        // --- Far Arm (Behind Torso, typing on keyboard) ---
        p.line(130f, 80f + sh, 134.5f, 92f, 4.2f, hoodieShade)
        p.line(134.5f, 92f, LAPTOP_X + 9f, handY - 0.4f + tapFar, 3.4f, hoodieShade)
        p.circle(LAPTOP_X + 9.5f, handY - 0.2f + tapFar, 1.8f, skinShade)

        // --- Torso & Oversized Hoodie ---
        // Main slouchy hoodie body
        begin()
        pt(118.5f, 104f)
        pt(133.5f, 104f)
        pt(136.5f, 97f)
        pt(137.8f, 86f)
        pt(135.5f, 78.5f + sh)
        pt(129f, 76f + sh)
        pt(122.5f, 77.5f + sh)
        pt(119f, 84f)
        pt(117.5f, 95f)
        fill(p, hoodie)

        // Shaded back and lower underside
        begin()
        pt(118.5f, 104f)
        pt(123.5f, 104f)
        pt(122.5f, 86f)
        pt(122.5f, 77.5f + sh)
        pt(119f, 84f)
        pt(117.5f, 95f)
        fill(p, hoodieShade)

        // Slouchy hood bunched around shoulders
        p.ellipse(125f, 77.2f + sh, 5.8f, 2.8f, hoodieShade)
        p.ellipse(125.5f, 76.8f + sh, 4.8f, 2.0f, hoodie)

        // Kangaroo pocket on front
        begin()
        pt(129.5f, 92f)
        pt(136.8f, 93f)
        pt(135.2f, 100f)
        pt(128.5f, 99.5f)
        fill(p, hoodieShade)

        // Hoodie drawstrings
        p.line(133.5f, 80f + sh, 131.5f, 89.5f, 0.7f, indoor(P.Sock))
        p.circle(131.5f, 90f, 0.7f, indoor(P.Sock))
        p.line(134.5f, 80.5f + sh, 133f, 88.5f, 0.7f, indoor(P.Sock))
        p.circle(133f, 89f, 0.7f, indoor(P.Sock))

        // --- Neck & Head (3/4 profile facing right towards laptop) ---
        val hx = 133.5f
        val hy = 65.5f + bob

        // Short neck (~4 units)
        p.rect(130.5f, hy + 4.8f, 4.5f, 5.2f, skinShade)

        // Hair mass / back of head
        p.circle(hx - 1f, hy - 0.5f, 8.8f, hair)

        // Face skin (smooth 3/4 profile)
        begin()
        pt(hx - 1f, hy - 6f)        // forehead top
        pt(hx + 4.5f, hy - 4.5f)     // temple
        pt(hx + 7f, hy - 1f)        // upper cheek / brow
        pt(hx + 8.5f, hy + 2.2f)     // cute nose tip
        pt(hx + 6.8f, hy + 3.8f)     // upper lip
        pt(hx + 7.2f, hy + 6.5f)     // rounded chin
        pt(hx + 1f, hy + 7.5f)      // jawline
        pt(hx - 2f, hy + 3f)        // jaw back / lower ear
        fill(p, skin)

        // Ear (partially visible under hair)
        p.ellipse(hx - 2f, hy + 1.8f, 1.8f, 2.4f, skin)
        p.ellipse(hx - 2f, hy + 1.8f, 1.1f, 1.5f, skinShade)

        // Rosy cheek blush
        p.ellipse(hx + 4.8f, hy + 3.8f, 1.6f, 1.2f, cheek)

        // Eyes: closed in peaceful focus / looking down at screen
        p.line(hx + 4.2f, hy + 0.8f, hx + 6.6f, hy + 1.5f, 0.9f, hair)
        p.line(hx + 6.6f, hy + 1.5f, hx + 7.1f, hy + 1.1f, 0.7f, hair)

        // Soft relaxed smile line
        p.line(hx + 6.2f, hy + 4.6f, hx + 7.2f, hy + 4.8f, 0.6f, skinShade)

        // --- Hair Bun & Bangs ---
        val bunX = hx - 8.2f
        val bunY = hy - 6.5f
        p.circle(bunX, bunY, 4.2f, hair) // main bun
        p.ellipse(bunX + 1.8f, bunY + 1.2f, 1.4f, 2.2f, phones) // scrunchie tie
        p.line(bunX - 1f, bunY + 3.5f, hx - 6.5f, hy + 3f, 0.8f, hair) // loose hair wisps

        // Layered fringe / bangs
        begin()
        pt(hx - 3.5f, hy - 8.6f)
        pt(hx + 4.8f, hy - 7.2f)
        pt(hx + 8.2f, hy - 2.2f)
        pt(hx + 5.2f, hy - 3.2f)
        pt(hx + 3.5f, hy - 1.5f)
        pt(hx + 1.5f, hy - 3.0f)
        pt(hx - 1f, hy - 0.8f)
        fill(p, hair)

        // --- Headphones ---
        p.line(hx - 3.8f, hy - 8.8f, hx + 2f, hy - 8.4f, 1.6f, phones)
        p.line(hx - 3.8f, hy - 8.8f, hx - 3.6f, hy - 2.5f, 1.6f, phones)
        p.ellipse(hx - 3.2f, hy + 0.8f, 2.8f, 3.8f, phones) // outer cup
        p.ellipse(hx - 3.2f, hy + 0.8f, 1.8f, 2.6f, indoor(P.ChairDark)) // cushion pad

        // --- Near Arm (In Front of Torso, typing on keyboard) ---
        p.line(128.5f, 81f + sh, 135.5f, 93f, 4.6f, hoodie)
        val nearHandX = LAPTOP_X + 5.5f
        val nearHandY = handY + tapNear
        p.line(135.5f, 93f, nearHandX, nearHandY, 4.0f, hoodie)
        p.rect(nearHandX - 1.8f, nearHandY - 1.8f, 1.8f, 3.6f, hoodieShade) // sleeve cuff
        p.ellipse(nearHandX + 1.2f, nearHandY + 0.3f, 2.0f, 1.4f, skin)
        p.line(nearHandX + 1.2f, nearHandY + 0.5f, nearHandX + 2.8f, nearHandY + 0.8f, 0.8f, skinShade)
    }

    private fun steam(p: ZenPainter, t: Float) {
        for (k in 0 until 2) {
            var y = DESK_Y - 11
            var prevX = MUG_X - 1.5f + k * 3
            var prevY = y
            while (y > DESK_Y - 28) {
                y -= 1.5f
                val fade = (y - (DESK_Y - 28)) / 17f
                val x = MUG_X - 1.5f + k * 3 + sin(y * 0.35f + t * 1.6f + k * 2f) * (1.4f - fade)
                p.line(prevX, prevY, x, y, 0.9f, a(P.Steam, 0.5f * fade))
                prevX = x
                prevY = y
            }
        }
    }

    companion object {
        const val WIN_X = 104f
        const val WIN_Y = 14f
        const val WIN_W = 104f
        const val WIN_H = 62f
        const val BAND_Y = 102f
        const val FLOOR_Y = 125f
        const val DESK_X = 142f
        const val DESK_Y = 98f
        const val DESK_W = 124f
        const val LAPTOP_X = 147f
        const val LAMP_X = 258f
        const val MUG_X = 212f
        const val PLANT_X = 298f
        const val CAT_X = 196f
        const val SHELF_X = 22f
        const val SHELF_Y = 52f
        const val CLOCK_X = 47f
        const val CLOCK_Y = 26f
    }
}
