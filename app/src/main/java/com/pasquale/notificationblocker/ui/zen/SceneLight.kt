package com.pasquale.notificationblocker.ui.zen

import java.time.LocalDate
import java.time.LocalTime

/**
 * How the lo-fi room scenes (res/raw/scene_work, scene_rest) look at a given moment: the opacity
 * of every layer the app drives (tools/lottie/import_creator.py lists them) and the season color of
 * the tree outside the window. Pure, so tests and previews can pick any date and time.
 *
 * The three sky layers are stacked day < sunset < night, so a blend between two neighbours only
 * needs the upper one's opacity. Sunrise uses the sunset sky (warm pink-orange) too. The keys are
 * the same minutes around sunrise and sunset as [ZenEnvironment]'s light.
 *
 * All alphas are in 0..1.
 */
data class SceneLight(
    val skyDay: Float,
    val skySunset: Float,
    val skyNight: Float,
    val sun: Float,
    val moon: Float,
    val stars: Float,
    val clouds: Float,
    val cityLights: Float,
    val roomShade: Float,
    val lampGlow: Float,
    val foliage: Int,
) {
    companion object {
        fun resolve(date: LocalDate, time: LocalTime): SceneLight {
            val (sunrise, sunset) = ZenEnvironment.sunTimes(date)
            val minute = time.hour * 60 + time.minute
            val (warm, night) = skyWeights(minute, sunrise, sunset)
            val dusk = warm + night // how far the day sky is covered
            return SceneLight(
                skyDay = 1f,
                skySunset = dusk.coerceIn(0f, 1f),
                skyNight = if (dusk > 0f) (night / dusk).coerceIn(0f, 1f) else 0f,
                sun = 1f - night,
                moon = night,
                stars = night,
                clouds = 1f - CLOUDS_AT_NIGHT_DIM * night,
                cityLights = (night + CITY_AT_DUSK * warm).coerceIn(0f, 1f),
                roomShade = ROOM_SHADE_NIGHT * night + ROOM_SHADE_DUSK * warm,
                lampGlow = (night + LAMP_AT_DUSK * warm).coerceIn(0f, 1f),
                foliage = ZenEnvironment.resolve(date, time).flora.tree,
            )
        }

        /**
         * Weights of the warm (sunrise/sunset) and night skies at [minute]; the day sky is the rest.
         * At most two of day/warm/night are non-zero at once.
         */
        fun skyWeights(minute: Int, sunrise: Int, sunset: Int): Pair<Float, Float> {
            // (minute, warm, night) keys, linearly blended in between
            val keys = listOf(
                Triple(sunrise - 70, 0f, 1f),
                Triple(sunrise - 5, 1f, 0f),
                Triple(sunrise + 70, 0f, 0f),
                Triple(sunset - 90, 0f, 0f),
                Triple(sunset - 20, 1f, 0f),
                Triple(sunset + 15, 1f, 0f),
                Triple(sunset + 70, 0f, 1f),
            )
            if (minute <= keys.first().first || minute >= keys.last().first) return 0f to 1f
            val next = keys.indexOfFirst { it.first > minute }
            val (m0, w0, n0) = keys[next - 1]
            val (m1, w1, n1) = keys[next]
            val t = (minute - m0).toFloat() / (m1 - m0)
            return (w0 + (w1 - w0) * t) to (n0 + (n1 - n0) * t)
        }

        private const val CLOUDS_AT_NIGHT_DIM = 0.7f
        private const val CITY_AT_DUSK = 0.6f
        private const val ROOM_SHADE_NIGHT = 0.55f
        private const val ROOM_SHADE_DUSK = 0.2f
        private const val LAMP_AT_DUSK = 0.5f
    }
}
