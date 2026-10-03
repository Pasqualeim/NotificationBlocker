package com.pasquale.notificationblocker.tile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ZenTileStateTest {

    private val summer = LocalDateTime.of(2026, 7, 10, 0, 0)

    private fun resolve(hour: Int, enabled: Boolean = true, start: Int = 17 * 60, end: Int = 9 * 60) =
        ZenTileState.resolve(summer.withHour(hour), enabled, start, end)

    @Test
    fun iconFollowsTheLight_leafSunLantern() {
        assertEquals(TileIcon.LEAF, resolve(5).icon)
        assertEquals(TileIcon.SUN, resolve(17).icon)
        assertEquals(TileIcon.LANTERN, resolve(20).icon)
        assertEquals(TileIcon.LANTERN, resolve(23).icon)
        assertTrue(resolve(23).evening)
        assertFalse(resolve(12).evening)
    }

    @Test
    fun activeOnlyWhenBlockingIsEnabled() {
        assertTrue(resolve(12).active)
        assertFalse(resolve(12, enabled = false).active)
    }

    @Test
    fun subtitle_tellsUntilWhenOrFromWhen() {
        assertEquals(TileSubtitle.Off, resolve(18, enabled = false).subtitle)
        assertEquals(TileSubtitle.Until(9 * 60), resolve(18).subtitle)
        assertEquals(TileSubtitle.From(17 * 60), resolve(12).subtitle)
        assertEquals(TileSubtitle.AllDay, resolve(12, start = 0, end = 0).subtitle)
    }

    @Test
    fun pauseEndedEarly_saysWhenTheNextOneStarts() {
        assertEquals(TileSubtitle.From(17 * 60), ZenTileState.resolve(summer.withHour(20), true, 17 * 60, 9 * 60, endedEarly = true).subtitle)
        // Whole-day window: a new one starts at midnight
        assertEquals(TileSubtitle.From(0), ZenTileState.resolve(summer.withHour(20), true, 8 * 60, 8 * 60, endedEarly = true).subtitle)
        assertTrue(ZenTileState.resolve(summer.withHour(20), true, 17 * 60, 9 * 60, endedEarly = true).active)
    }
}
