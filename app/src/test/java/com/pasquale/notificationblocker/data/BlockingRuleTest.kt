package com.pasquale.notificationblocker.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingRuleTest {

    private val work = "com.work.app"
    private val other = "com.other.app"

    private fun t(hours: Int, minutes: Int = 0) = hours * 60 + minutes

    private fun shouldBlock(
        pkg: String = work,
        enabled: Boolean = true,
        apps: Set<String> = setOf(work),
        now: Int = t(23),
        start: Int = t(22),
        end: Int = t(7),
        endedEarly: Boolean = false,
    ) = BlockingRule.shouldBlock(pkg, enabled, apps, now, start, end, endedEarly)

    @Test
    fun blocksSelectedAppInsideWindow() = assertTrue(shouldBlock())

    @Test
    fun masterSwitchOff_neverBlocks() = assertFalse(shouldBlock(enabled = false))

    @Test
    fun outsideWindow_doesNotBlock() = assertFalse(shouldBlock(now = t(12)))

    @Test
    fun unselectedApp_isNotBlocked() = assertFalse(shouldBlock(pkg = other))

    @Test
    fun noSelectedApps_blocksNothing() = assertFalse(shouldBlock(apps = emptySet()))

    @Test
    fun pauseEndedEarly_letsWorkThrough() = assertFalse(shouldBlock(endedEarly = true))

    @Test
    fun windowBoundaries_areHalfOpen() {
        assertTrue(shouldBlock(now = t(22)))
        assertFalse(shouldBlock(now = t(7)))
        assertTrue(shouldBlock(now = t(6, 59)))
    }

    @Test
    fun startEqualsEnd_blocksAllDay() {
        assertTrue(shouldBlock(now = t(12), start = t(8), end = t(8)))
        assertTrue(shouldBlock(now = t(3), start = t(8), end = t(8)))
    }
}
