package com.pasquale.notificationblocker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFitTest {

    // Cards that are 600 px tall at scale 1 and shrink linearly with it; the scene is 200 px
    private val scene = 200
    private fun rest(scale: Float) = (600 * scale).toInt()

    private fun plan(viewport: Int) = HomeFit.plan(viewport, scene, ::rest)

    @Test
    fun fitsAtNaturalSize_nothingScaled() {
        assertEquals(HomeFitPlan(1f, 1f), plan(800))
        assertEquals(HomeFitPlan(1f, 1f), plan(1200))
    }

    @Test
    fun unknownViewport_isNaturalSize() {
        assertEquals(HomeFitPlan(1f, 1f), plan(0))
    }

    @Test
    fun slightlyShort_shrinksOnlyTheCards_sceneStaysWhole() {
        val plan = plan(740)
        assertEquals(1f, plan.sceneScale, 0f)
        assertTrue(plan.restScale < 1f && plan.restScale > HomeFit.REST_MIN_SCALE)
        // It uses the room: the result fits, and a hair more scale would not
        assertTrue(rest(plan.restScale) + scene <= 740)
        assertTrue(rest(plan.restScale + 0.01f) + scene > 740)
    }

    @Test
    fun cardsStopAtTheFloor_thenTheSceneShrinks() {
        // 0.8 * 600 = 480 for the cards, 200 for the scene: 680 needed, 640 available
        val plan = plan(640)
        assertEquals(HomeFit.REST_MIN_SCALE, plan.restScale, 0f)
        assertEquals((640 - 480) / 200f, plan.sceneScale, 0.001f)
    }

    @Test
    fun sceneNeverShrinksBelowItsFloor_pageScrollsInstead() {
        val plan = plan(400)
        assertEquals(HomeFit.REST_MIN_SCALE, plan.restScale, 0f)
        assertEquals(HomeFit.SCENE_MIN_SCALE, plan.sceneScale, 0f)
    }

    @Test
    fun scenesAreNeverEnlarged() {
        assertEquals(1f, plan(5000).sceneScale, 0f)
    }
}
