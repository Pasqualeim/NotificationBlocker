package com.pasquale.notificationblocker.ui.components

/** How much Home is scaled to fit one screen: [restScale] for the cards, [sceneScale] for the scene. */
data class HomeFitPlan(val restScale: Float, val sceneScale: Float)

/**
 * The rule that keeps Home on one screen, pure so it can be tested (docs/DESIGN_SYSTEM.md, "Home in
 * una schermata"). The scene is never cropped: it is drawn whole, at its aspect ratio, or smaller.
 * When the page does not fit the viewport, in this order:
 *
 * 1. everything at natural size, if it fits;
 * 2. the cards (header, status, schedule, gaps) shrink together, down to [REST_MIN_SCALE];
 * 3. only then the scene shrinks too, whole, down to [SCENE_MIN_SCALE];
 * 4. if even that does not fit (huge system font, permission card showing), the page scrolls.
 */
object HomeFit {
    const val REST_MIN_SCALE = 0.8f
    const val SCENE_MIN_SCALE = 0.65f

    private const val SEARCH_STEPS = 7

    /**
     * @param viewport height available to the page, in px (0 = unknown: natural size)
     * @param sceneNatural height of the scene at full width, in px
     * @param restHeight height of everything but the scene when the cards are scaled by the given
     *   factor, gaps included, in px; it may measure real layouts, so it is called sparingly
     */
    fun plan(viewport: Int, sceneNatural: Int, restHeight: (scale: Float) -> Int): HomeFitPlan {
        if (viewport <= 0 || restHeight(1f) + sceneNatural <= viewport) return HomeFitPlan(1f, 1f)

        if (restHeight(REST_MIN_SCALE) + sceneNatural <= viewport) {
            // Largest card scale that still fits: bisection, the height grows with the scale
            var fits = REST_MIN_SCALE
            var tooBig = 1f
            repeat(SEARCH_STEPS) {
                val middle = (fits + tooBig) / 2f
                if (restHeight(middle) + sceneNatural <= viewport) fits = middle else tooBig = middle
            }
            return HomeFitPlan(fits, 1f)
        }

        return HomeFitPlan(REST_MIN_SCALE, sceneScale(viewport - restHeight(REST_MIN_SCALE), sceneNatural))
    }

    /** Scale of the scene when [room] px are left for it: whole, never enlarged, never below its floor. */
    fun sceneScale(room: Int, sceneNatural: Int): Float =
        (room.toFloat() / sceneNatural).coerceIn(SCENE_MIN_SCALE, 1f)
}
