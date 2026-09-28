package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import javax.imageio.ImageIO

class ZenSceneTest {

    private val springDay = LocalDate.of(2026, 4, 15)
    private val summerDay = LocalDate.of(2026, 7, 10)
    private val autumnDay = LocalDate.of(2026, 10, 20)
    private val winterDay = LocalDate.of(2026, 1, 15)
    private val days = listOf(springDay, summerDay, autumnDay, winterDay)
    private val times = listOf(LocalTime.of(6, 30), LocalTime.of(12, 0), LocalTime.of(17, 0), LocalTime.of(19, 30), LocalTime.of(23, 0))

    private fun env(date: LocalDate, time: LocalTime) = ZenEnvironment.resolve(date, time)

    private fun ZenState.wet() = copy(rain = true, light = ZenEnvironment.rainy(light))

    // Environment ---------------------------------------------------------------------------------

    @Test
    fun season_followsMeteorologicalMonths() {
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 2, 28)))
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 3, 1)))
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 5, 31)))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 6, 1)))
        assertEquals(Season.SUMMER, Season.of(LocalDate.of(2026, 8, 31)))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 9, 1)))
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 11, 30)))
        assertEquals(Season.WINTER, Season.of(LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun summerAfternoon_isFullDaylight() {
        // The point of the app: leaving work at 17:00 in summer means sun, not night
        val state = env(summerDay, LocalTime.of(17, 0))
        assertEquals(TimeOfDay.DAYLIGHT, state.timeOfDay)
        assertTrue(state.celestial.isSun)
        assertEquals(ZenPalette.Daylight, state.light)
    }

    @Test
    fun winterAfternoon_isAlreadySunsetThenNight() {
        assertEquals(TimeOfDay.GOLDEN_HOUR, env(winterDay, LocalTime.of(16, 30)).timeOfDay)
        val night = env(winterDay, LocalTime.of(18, 30))
        assertEquals(TimeOfDay.NIGHT, night.timeOfDay)
        assertFalse(night.celestial.isSun)
        assertEquals(ZenPalette.Night, night.light)
    }

    @Test
    fun timeOfDay_coversDawnDaySunsetNight() {
        assertEquals(TimeOfDay.NIGHT, env(springDay, LocalTime.of(3, 0)).timeOfDay)
        assertEquals(TimeOfDay.DAWN, env(springDay, LocalTime.of(6, 50)).timeOfDay)
        assertEquals(TimeOfDay.DAYLIGHT, env(springDay, LocalTime.of(12, 0)).timeOfDay)
        assertEquals(TimeOfDay.GOLDEN_HOUR, env(springDay, LocalTime.of(19, 0)).timeOfDay)
        assertEquals(TimeOfDay.NIGHT, env(springDay, LocalTime.of(22, 0)).timeOfDay)
    }

    @Test
    fun summerDays_areLongerThanWinterDays() {
        val (summerRise, summerSet) = ZenEnvironment.sunTimes(summerDay)
        val (winterRise, winterSet) = ZenEnvironment.sunTimes(winterDay)
        assertTrue(summerSet - summerRise > winterSet - winterRise + 5 * 60)
        assertTrue(summerSet > 20 * 60 && winterSet < 17 * 60 + 30)
    }

    @Test
    fun light_blendsSmoothlyAroundSunset() {
        // Minute by minute, no color channel jumps by more than a few steps
        var previous = env(summerDay, LocalTime.of(18, 0)).light.skyMid
        for (m in 18 * 60 + 1..23 * 60) {
            val current = env(summerDay, LocalTime.of(m / 60, m % 60)).light.skyMid
            for (shift in intArrayOf(0, 8, 16)) {
                val delta = kotlin.math.abs(((current shr shift) and 0xFF) - ((previous shr shift) and 0xFF))
                assertTrue("jump of $delta at minute $m", delta <= 6)
            }
            previous = current
        }
    }

    @Test
    fun sunIsHighAtNoonAndLowAtSunset() {
        val noon = env(summerDay, LocalTime.of(13, 0)).celestial
        val evening = env(summerDay, LocalTime.of(20, 30)).celestial
        assertTrue(noon.y < 0.15f)
        assertTrue(evening.y > 0.5f)
        assertTrue(evening.x < noon.x) // east (right) to west (left)
    }

    @Test
    fun rain_isStableSometimesAndNeverInWinter() {
        val year = (0 until 365).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }
        val rainyHours = year.associateWith { day -> (0 until 24).count { ZenEnvironment.isRaining(day, it) } }
        // A shower lasts six hours, a few days a season have one
        assertTrue(rainyHours.values.all { it == 0 || it == 6 })
        val rainyDays = rainyHours.filterValues { it > 0 }.keys
        assertTrue(rainyDays.size in 20..80)
        assertTrue(rainyDays.none { Season.of(it) == Season.WINTER })
        // Same date and hour, same weather
        for (day in rainyDays) assertEquals(ZenEnvironment.isRaining(day, 9), ZenEnvironment.isRaining(day, 9))
    }

    @Test
    fun rainyScenes_lookDifferentFromDryOnes() {
        val dry = env(springDay, LocalTime.of(12, 0)).copy(rain = false, light = ZenPalette.Daylight)
        for (offWork in listOf(false, true)) {
            val wet = render(dry.wet(), 5f, offWork)
            assertTrue(wet.pixels().all { it ushr 24 == 0xFF })
            assertNotEquals(render(dry, 5f, offWork).pixels().toList(), wet.pixels().toList())
        }
    }

    // Rendering -----------------------------------------------------------------------------------

    private fun scene(e: ZenState, offWork: Boolean): ScenePainting = if (offWork) NaturePainting(e) else DeskPainting(e)

    private fun render(e: ZenState, t: Float, offWork: Boolean, scale: Int = 1): BufferedImage {
        val painting = scene(e, offWork)
        val image = BufferedImage((ScenePainting.W * scale).toInt(), (ScenePainting.H * scale).toInt(), BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.scale(scale.toDouble(), scale.toDouble())
        val p = AwtPainter(g)
        painting.sky(p)
        painting.skyMotion(p, t)
        painting.landscape(p)
        painting.foreground(p, t)
        g.dispose()
        return image
    }

    private fun BufferedImage.pixels() = getRGB(0, 0, width, height, null, 0, width)

    @Test
    fun everyPixelIsOpaque_inBothScenesForEverySeasonAndTime() {
        for (date in days) for (time in times) for (offWork in listOf(false, true)) {
            assertTrue("$date $time $offWork", render(env(date, time), 12.5f, offWork).pixels().all { it ushr 24 == 0xFF })
        }
    }

    @Test
    fun seasonsAndTimesLookDifferent_inBothScenes() {
        for (offWork in listOf(false, true)) {
            val frames = days.flatMap { date -> times.map { time -> render(env(date, time), 3f, offWork).pixels().toList() } }
            assertEquals(frames.size, frames.toSet().size)
        }
    }

    @Test
    fun deskAndNatureAreDifferentScenes_andBothMove() {
        val e = env(summerDay, LocalTime.of(12, 0))
        assertNotEquals(render(e, 5f, false).pixels().toList(), render(e, 5f, true).pixels().toList())
        for (offWork in listOf(false, true)) {
            assertNotEquals(render(e, 5f, offWork).pixels().toList(), render(e, 6f, offWork).pixels().toList())
        }
    }

    /** Writes preview PNGs (3x) to app/build/zen-previews to eyeball the art without a device. */
    @Test
    fun writePreviews() {
        val dir = File("build/zen-previews").apply {
            deleteRecursively()
            mkdirs()
        }
        for (offWork in listOf(false, true)) for (date in days) for (time in times) {
            val e = env(date, time)
            val name = "%s_%s_%02d%02d_%s.png".format(
                if (offWork) "nature" else "desk", e.season.name.lowercase(), time.hour, time.minute, e.timeOfDay.name.lowercase(),
            )
            ImageIO.write(render(e, 20f, offWork, scale = 3), "png", File(dir, name))
        }
        for (offWork in listOf(false, true)) for (time in listOf(LocalTime.of(12, 0), LocalTime.of(21, 0))) {
            val e = env(autumnDay, time).wet()
            val name = "%s_rain_%02d%02d.png".format(if (offWork) "nature" else "desk", time.hour, time.minute)
            ImageIO.write(render(e, 20f, offWork, scale = 3), "png", File(dir, name))
        }
    }
}

/** [ZenPainter] on AWT, for JVM previews and tests. */
private class AwtPainter(private val g: Graphics2D) : ZenPainter {
    private fun color(c: Int) = Color(c, true)

    override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        g.color = color(color)
        g.fill(Rectangle2D.Float(x, y, w, h))
    }

    override fun verticalGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int) {
        g.paint = GradientPaint(x, y, color(top), x, y + h, color(bottom))
        g.fill(Rectangle2D.Float(x, y, w, h))
    }

    override fun circle(cx: Float, cy: Float, r: Float, color: Int) = ellipse(cx, cy, r, r, color)

    override fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        g.color = color(color)
        g.fill(Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2))
    }

    override fun glow(cx: Float, cy: Float, r: Float, color: Int) {
        if (r <= 0f) return
        g.paint = RadialGradientPaint(cx, cy, r, floatArrayOf(0f, 1f), arrayOf(color(color), color(color and 0xFFFFFF)))
        g.fill(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
    }

    override fun polygon(points: FloatArray, count: Int, color: Int) {
        val path = Path2D.Float()
        path.moveTo(points[0], points[1])
        for (i in 1 until count) path.lineTo(points[i * 2], points[i * 2 + 1])
        path.closePath()
        g.color = color(color)
        g.fill(path)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        g.color = color(color)
        g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Line2D.Float(x1, y1, x2, y2))
    }
}
