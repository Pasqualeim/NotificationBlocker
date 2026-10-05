package com.pasquale.notificationblocker.ui.zen

import com.pasquale.notificationblocker.ui.theme.ZenPalette
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
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
    /** Snowfall (winter only): grey sky like rain, flakes instead of drops. */
    val snow: Boolean = false,
    /** Minutes from midnight, for the wall clock of the desk. */
    val clock: Int = 12 * 60,
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
 * Sunrise and sunset come from the standard solar formulas for a fixed place (the app never asks
 * for the location) in the phone's time zone, daylight saving included: in Italy about 05:30-20:50
 * at the summer solstice and 07:40-16:40 at the winter one, within a few minutes of the real times.
 * Leaving work at 17:00 in summer still means full sun.
 */
object ZenEnvironment {

    private const val MINUTES_PER_DAY = 24 * 60

    // Central Italy. Day length depends on the latitude only; elsewhere it is an approximation
    private const val LATITUDE = 43.0

    // Where the zone's people live, west of its standard meridian (15° per hour): Rome for Italian time.
    // In most zones the land lies west of the meridian
    private const val DEGREES_WEST_OF_MERIDIAN = 2.5

    // Sun's center at sunrise/sunset: its radius plus the refraction near the horizon
    private const val HORIZON_ALTITUDE = -0.833

    fun now(): ZenState = LocalDateTime.now().let { resolve(it.toLocalDate(), it.toLocalTime()) }

    fun resolve(date: LocalDate, time: LocalTime): ZenState {
        val (sunrise, sunset) = sunTimes(date)
        val minute = time.hour * 60 + time.minute
        val rain = isRaining(date, time.hour)
        val snow = isSnowing(date, time.hour)
        val light = light(minute, sunrise, sunset)
        return ZenState(
            season = Season.of(date),
            timeOfDay = timeOfDay(minute, sunrise, sunset),
            light = if (rain || snow) rainy(light) else light,
            celestial = celestial(minute, sunrise, sunset),
            rain = rain,
            snow = snow,
            clock = minute,
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

    /** Like [isRaining], for winter: some days have six hours of snowfall. */
    fun isSnowing(date: LocalDate, hour: Int): Boolean {
        if (Season.of(date) != Season.WINTER) return false
        if (hash(date.year, date.dayOfYear, SNOW_SEED) % 100 >= SNOW_CHANCE) return false
        val start = hash(date.year, date.dayOfYear, SNOW_SEED + 1) % 24
        return (hour - start + 24) % 24 < RAIN_HOURS
    }

    private const val RAIN_SEED = 7_331
    private const val SNOW_SEED = 9_157
    private const val SNOW_CHANCE = 30
    private const val RAIN_HOURS = 6

    /**
     * Sunrise and sunset in minutes from midnight, on the clock of [zone] (daylight saving included).
     * NOAA approximations of the sun's declination and of the equation of time, at [LATITUDE].
     */
    fun sunTimes(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
        val g = 2 * PI / 365 * (date.dayOfYear - 1)
        val declination = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) +
            0.000907 * sin(2 * g) - 0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        val equationOfTime = 229.18 * (
            0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g)
            )
        val latitude = Math.toRadians(LATITUDE)
        val cosHourAngle = (sin(Math.toRadians(HORIZON_ALTITUDE)) - sin(latitude) * sin(declination)) /
            (cos(latitude) * cos(declination))
        // Minutes from solar noon to sunset: the earth turns 1° every 4 minutes
        val halfDay = 4 * Math.toDegrees(acos(cosHourAngle.coerceIn(-1.0, 1.0)))
        val noonHere = date.atTime(12, 0).atZone(zone)
        val standardOffset = zone.rules.getStandardOffset(noonHere.toInstant()).totalSeconds / 60
        val longitude = standardOffset / 4.0 - DEGREES_WEST_OF_MERIDIAN
        val noon = 12 * 60 - 4 * longitude - equationOfTime + noonHere.offset.totalSeconds / 60
        return (noon - halfDay).roundToInt() to (noon + halfDay).roundToInt()
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
