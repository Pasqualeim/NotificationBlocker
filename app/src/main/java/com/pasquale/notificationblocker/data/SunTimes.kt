package com.pasquale.notificationblocker.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sunrise and sunset for a fixed place (the app never asks for the location) in the phone's time zone,
 * daylight saving included: in Italy about 05:30-20:50 at the summer solstice and 07:40-16:40 at the
 * winter one, within a few minutes of the real times. Used by the scenes and by the history (hours of
 * daylight lived on a break).
 */
object SunTimes {

    // Central Italy. Day length depends on the latitude only; elsewhere it is an approximation
    private const val LATITUDE = 43.0

    // Where the zone's people live, west of its standard meridian (15° per hour): Rome for Italian time.
    // In most zones the land lies west of the meridian
    private const val DEGREES_WEST_OF_MERIDIAN = 2.5

    // Sun's center at sunrise/sunset: its radius plus the refraction near the horizon
    private const val HORIZON_ALTITUDE = -0.833

    /**
     * Sunrise and sunset in minutes from midnight, on the clock of [zone] (daylight saving included).
     * NOAA approximations of the sun's declination and of the equation of time, at [LATITUDE].
     */
    fun of(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
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
}
