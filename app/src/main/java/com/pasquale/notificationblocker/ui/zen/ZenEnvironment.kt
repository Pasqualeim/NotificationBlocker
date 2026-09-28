package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Meteorological seasons (northern hemisphere). */
enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    companion object {
        fun of(date: LocalDate): Season = when (date.monthValue) {
            in 3..5 -> SPRING
            in 6..8 -> SUMMER
            in 9..11 -> AUTUMN
            else -> WINTER
        }
    }
}

enum class TimeOfDay { DAWN, DAYLIGHT, GOLDEN_HOUR, NIGHT }

/** Where the sun (or the moon, at night) sits, as fractions of the scene width and height. */
data class Celestial(val isSun: Boolean, val x: Float, val y: Float)

/**
 * Everything the zen forest needs to know about "now": season, time of day, the blended light
 * palette (so dawn and sunset fade smoothly instead of switching) and the sun/moon position.
 */
data class ZenState(
    val season: Season,
    val timeOfDay: TimeOfDay,
    val light: ZenPalette.Light,
    val celestial: Celestial,
    val rain: Boolean = false,
) {
    val flora: ZenPalette.Flora
        get() = when (season) {
            Season.SPRING -> ZenPalette.Spring
            Season.SUMMER -> ZenPalette.Summer
            Season.AUTUMN -> ZenPalette.Autumn
            Season.WINTER -> ZenPalette.Winter
        }
}

/**
 * Resolves the real light of the day. Pure functions of date and time, so tests (and previews) can
 * simulate any moment.
 *
 * Sunrise and sunset follow the day of the year with a simple cosine model tuned for Italian
 * latitudes and local (daylight saving) time: about 05:30-20:30 at the summer solstice and
 * 07:45-16:45 at the winter one. Leaving work at 17:00 in summer still means full sun.
 */
object ZenEnvironment {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val SUMMER_SOLSTICE_DAY = 172

    fun now(): ZenState = LocalDateTime.now().let { resolve(it.toLocalDate(), it.toLocalTime()) }

    fun resolve(date: LocalDate, time: LocalTime): ZenState {
        val (sunrise, sunset) = sunTimes(date)
        val minute = time.hour * 60 + time.minute
        val rain = isRaining(date, time.hour)
        val light = light(minute, sunrise, sunset)
        return ZenState(
            season = Season.of(date),
            timeOfDay = timeOfDay(minute, sunrise, sunset),
            light = if (rain) rainy(light) else light,
            celestial = celestial(minute, sunrise, sunset),
            rain = rain,
        )
    }

    /** The same light under rain clouds: greyer sky, dimmer and cooler ambient, no sun rays. */
    fun rainy(l: ZenPalette.Light): ZenPalette.Light = l.copy(
        skyTop = ZenColor.mix(l.skyTop, ZenColor.multiply(ZenPalette.Overcast, l.ambient), 0.6f),
        skyMid = ZenColor.mix(l.skyMid, ZenColor.multiply(ZenPalette.RainCloud, l.ambient), 0.6f),
        skyLow = ZenColor.mix(l.skyLow, ZenColor.multiply(ZenPalette.RainCloud, l.ambient), 0.5f),
        ambient = ZenColor.multiply(l.ambient, ZenPalette.RainAmbient),
        mist = ZenColor.mix(l.mist, ZenPalette.RainCloud, 0.4f),
        water = ZenColor.mix(l.water, ZenColor.multiply(ZenPalette.Overcast, l.ambient), 0.4f),
        rays = 0f,
    )

    /**
     * Made-up but stable weather: some days have a six-hour shower, more often in spring and autumn,
     * never in winter (the garden already has its snow). Same date and hour, same answer.
     */
    fun isRaining(date: LocalDate, hour: Int): Boolean {
        val chance = when (Season.of(date)) {
            Season.SPRING -> 22
            Season.SUMMER -> 10
            Season.AUTUMN -> 28
            Season.WINTER -> 0
        }
        if (hash(date.year, date.dayOfYear, RAIN_SEED) % 100 >= chance) return false
        val start = hash(date.year, date.dayOfYear, RAIN_SEED + 1) % 24
        return (hour - start + 24) % 24 < RAIN_HOURS
    }

    private const val RAIN_SEED = 7_331
    private const val RAIN_HOURS = 6

    /** Sunrise and sunset in minutes from midnight. */
    fun sunTimes(date: LocalDate): Pair<Int, Int> {
        val phase = cos(2 * PI * (date.dayOfYear - SUMMER_SOLSTICE_DAY) / 365.0)
        val dayLength = 12 * 60 + 3 * 60 * phase
        val noon = 12 * 60 + 37 + 23 * phase // 13:00 in summer (DST), ~12:15 in winter
        return (noon - dayLength / 2).roundToInt() to (noon + dayLength / 2).roundToInt()
    }

    fun timeOfDay(minute: Int, sunrise: Int, sunset: Int): TimeOfDay = when (minute) {
        in sunrise - 40 until sunrise + 50 -> TimeOfDay.DAWN
        in sunrise + 50 until sunset - 70 -> TimeOfDay.DAYLIGHT
        in sunset - 70 until sunset + 30 -> TimeOfDay.GOLDEN_HOUR
        else -> TimeOfDay.NIGHT
    }

    /** Light keyframes around sunrise and sunset, linearly blended in between. */
    private fun light(minute: Int, sunrise: Int, sunset: Int): ZenPalette.Light {
        val keys = listOf(
            sunrise - 70 to ZenPalette.Night,
            sunrise - 5 to ZenPalette.Dawn,
            sunrise + 70 to ZenPalette.Daylight,
            sunset - 90 to ZenPalette.Daylight,
            sunset - 20 to ZenPalette.GoldenHour,
            sunset + 15 to ZenPalette.GoldenHour,
            sunset + 70 to ZenPalette.Night,
        )
        if (minute <= keys.first().first || minute >= keys.last().first) return ZenPalette.Night
        val next = keys.indexOfFirst { it.first > minute }
        val (m0, l0) = keys[next - 1]
        val (m1, l1) = keys[next]
        return blend(l0, l1, (minute - m0).toFloat() / (m1 - m0))
    }

    fun blend(a: ZenPalette.Light, b: ZenPalette.Light, t: Float): ZenPalette.Light {
        if (t <= 0f) return a
        if (t >= 1f) return b
        fun c(x: Int, y: Int) = ZenColor.mix(x, y, t)
        fun f(x: Float, y: Float) = x + (y - x) * t
        return ZenPalette.Light(
            skyTop = c(a.skyTop, b.skyTop), skyMid = c(a.skyMid, b.skyMid), skyLow = c(a.skyLow, b.skyLow),
            ambient = c(a.ambient, b.ambient), mist = c(a.mist, b.mist),
            water = c(a.water, b.water), waterDeep = c(a.waterDeep, b.waterDeep),
            waterLight = c(a.waterLight, b.waterLight),
            rays = f(a.rays, b.rays), lantern = f(a.lantern, b.lantern), stars = f(a.stars, b.stars),
        )
    }

    /** Sun arc from sunrise (right) to sunset (left); the moon takes the same arc during the night. */
    private fun celestial(minute: Int, sunrise: Int, sunset: Int): Celestial {
        val sunUp = minute in sunrise - 15..sunset + 15
        val progress = if (sunUp) {
            (minute - (sunrise - 15)).toFloat() / (sunset - sunrise + 30)
        } else {
            val nightStart = sunset + 15
            val nightLength = MINUTES_PER_DAY - (sunset - sunrise + 30)
            ((minute - nightStart + MINUTES_PER_DAY) % MINUTES_PER_DAY).toFloat() / nightLength
        }
        val x = 0.92f - progress * 0.84f
        val y = (0.64 - sin(progress * PI) * 0.54).toFloat()
        return Celestial(isSun = sunUp, x = x, y = y)
    }
}
