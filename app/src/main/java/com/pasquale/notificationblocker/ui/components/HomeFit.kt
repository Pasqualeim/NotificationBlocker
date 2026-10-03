package com.pasquale.notificationblocker.ui.components

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
    const val SCENE_MIN_SCALE = 0.6f

    /**
     * Scale of the cards, from their natural height [rest] at full width (gaps included) and the
     * scene's [sceneNatural], in px; [viewport] 0 = unknown: natural size. Laid out 1/scale wider, the
     * cards wrap less, so their real height is at most rest × scale: the result always fits, with at
     * worst a little room left, and it needs one look at the cards instead of a search.
     */
    fun restScale(viewport: Int, sceneNatural: Int, rest: Int): Float {
        if (viewport <= 0 || rest <= 0 || rest + sceneNatural <= viewport) return 1f
        return ((viewport - sceneNatural).toFloat() / rest).coerceIn(REST_MIN_SCALE, 1f)
    }

    /** Scale of the scene when [room] px are left for it: whole, never enlarged, never below its floor. */
    fun sceneScale(room: Int, sceneNatural: Int): Float =
        if (sceneNatural <= 0) 1f else (room.toFloat() / sceneNatural).coerceIn(SCENE_MIN_SCALE, 1f)
}
