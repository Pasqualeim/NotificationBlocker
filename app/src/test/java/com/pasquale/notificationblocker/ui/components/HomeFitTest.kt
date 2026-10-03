package com.pasquale.notificationblocker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFitTest {

    // Cards 600 px tall at full width, scene 200 px
    private val scene = 200
    private val rest = 600

    @Test
    fun fitsAtNaturalSize_nothingScaled() {
        assertEquals(1f, HomeFit.restScale(800, scene, rest), 0f)
        assertEquals(1f, HomeFit.restScale(1200, scene, rest), 0f)
        assertEquals(1f, HomeFit.sceneScale(800 - rest, scene), 0f)
    }

    @Test
    fun unknownViewport_isNaturalSize() {
        assertEquals(1f, HomeFit.restScale(0, scene, rest), 0f)
    }

    @Test
    fun slightlyShort_shrinksOnlyTheCards_sceneStaysWhole() {
        val scale = HomeFit.restScale(740, scene, rest)
        assertEquals(540f / 600f, scale, 0.0001f)
        assertEquals(1f, HomeFit.sceneScale(740 - (rest * scale).toInt(), scene), 0f)
    }

    @Test
    fun cardsStopAtTheFloor_thenTheSceneShrinks() {
        // 0.8 * 600 = 480 for the cards, 200 for the scene: 680 needed, 640 available
        assertEquals(HomeFit.REST_MIN_SCALE, HomeFit.restScale(640, scene, rest), 0f)
        assertEquals((640 - 480) / 200f, HomeFit.sceneScale(640 - 480, scene), 0.001f)
    }

    @Test
    fun sceneNeverShrinksBelowItsFloor_pageScrollsInstead() {
        assertEquals(HomeFit.REST_MIN_SCALE, HomeFit.restScale(400, scene, rest), 0f)
        assertEquals(HomeFit.SCENE_MIN_SCALE, HomeFit.sceneScale(400 - 480, scene), 0f)
    }

    @Test
    fun sceneIsNeverEnlarged() {
        assertEquals(1f, HomeFit.sceneScale(5000, scene), 0f)
    }
}
