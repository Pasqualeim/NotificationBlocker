package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SceneLightTest {

    private val summer = LocalDate.of(2026, 6, 21)
    private val winter = LocalDate.of(2026, 12, 21)

    @Test
    fun `midday shows the plain day sky with the sun and an unlit room`() {
        val light = SceneLight.resolve(summer, LocalTime.of(13, 0))
        assertEquals(1f, light.skyDay, 0f)
        assertEquals(0f, light.skySunset, 0f)
        assertEquals(0f, light.skyNight, 0f)
        assertEquals(1f, light.sun, 0f)
        assertEquals(0f, light.moon, 0f)
        assertEquals(0f, light.roomShade, 0f)
        assertEquals(0f, light.lampGlow, 0f)
        assertEquals(0f, light.cityLights, 0f)
    }

    @Test
    fun `deep night shows the night sky, the moon, the lamp and the city lights`() {
        val light = SceneLight.resolve(summer, LocalTime.of(23, 30))
        assertEquals(1f, light.skySunset, 0f) // under the night sky
        assertEquals(1f, light.skyNight, 0f)
        assertEquals(0f, light.sun, 0f)
        assertEquals(1f, light.moon, 0f)
        assertEquals(1f, light.stars, 0f)
        assertEquals(1f, light.lampGlow, 0f)
        assertEquals(1f, light.cityLights, 0f)
        assertTrue(light.clouds < 0.5f)
        assertTrue(light.roomShade > 0.5f)
    }

    @Test
    fun `golden hour shows the warm sky with the lamp half on`() {
        val (_, sunset) = ZenEnvironment.sunTimes(summer)
        val light = SceneLight.resolve(summer, LocalTime.of(0, 0).plusMinutes(sunset.toLong()))
        assertEquals(1f, light.skySunset, 0f)
        assertEquals(0f, light.skyNight, 0f)
        assertEquals(1f, light.sun, 0f)
        assertEquals(0.5f, light.lampGlow, 0.001f)
    }

    @Test
    fun `winter 17h is already dark, summer 17h is full daylight`() {
        assertTrue(SceneLight.resolve(winter, LocalTime.of(18, 30)).skyNight > 0.9f)
        assertEquals(0f, SceneLight.resolve(summer, LocalTime.of(17, 0)).skySunset, 0f)
    }

    @Test
    fun `sky layers change smoothly minute by minute over the whole day`() {
        for (date in listOf(summer, winter)) {
            var previous = SceneLight.resolve(date, LocalTime.MIDNIGHT)
            for (minute in 1 until 24 * 60) {
                val current = SceneLight.resolve(date, LocalTime.of(minute / 60, minute % 60))
                for ((a, b) in listOf(
                    previous.skySunset to current.skySunset,
                    previous.skyNight to current.skyNight,
                    previous.lampGlow to current.lampGlow,
                    previous.roomShade to current.roomShade,
                )) {
                    assertTrue("jump at $date minute $minute", kotlin.math.abs(a - b) <= 0.03f)
                }
                previous = current
            }
        }
    }

    @Test
    fun `every alpha stays in range`() {
        for (minute in 0 until 24 * 60 step 7) {
            val l = SceneLight.resolve(summer, LocalTime.of(minute / 60, minute % 60))
            for (alpha in listOf(l.skyDay, l.skySunset, l.skyNight, l.sun, l.moon, l.stars, l.clouds, l.cityLights, l.roomShade, l.lampGlow)) {
                assertTrue(alpha in 0f..1f)
            }
        }
    }

    @Test
    fun `the tree outside follows the season`() {
        assertEquals(ZenPalette.Autumn.tree, SceneLight.resolve(LocalDate.of(2026, 10, 20), LocalTime.NOON).foliage)
        assertEquals(ZenPalette.Spring.tree, SceneLight.resolve(LocalDate.of(2026, 4, 10), LocalTime.NOON).foliage)
    }
}
