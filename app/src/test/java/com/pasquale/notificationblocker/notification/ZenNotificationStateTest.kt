package com.pasquale.notificationblocker.notification

import com.pasquale.notificationblocker.data.OffHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ZenNotificationStateTest {

    private val summer = LocalDateTime.of(2026, 7, 10, 0, 0)
    private val winter = LocalDateTime.of(2026, 1, 15, 0, 0)

    private fun resolve(
        now: LocalDateTime,
        start: Int = 17 * 60,
        end: Int = 9 * 60,
        enabled: Boolean = true,
        dismissed: String? = null,
        count: Int = 0,
    ) = ZenNotificationState.resolve(now, enabled, start, end, dismissed) { count }

    @Test
    fun hiddenWhenBlockingIsOff_orOutsideTheWindow() {
        assertNull(resolve(summer.withHour(18), enabled = false))
        assertNull(resolve(summer.withHour(12)))
        assertNotNull(resolve(summer.withHour(18)))
    }

    @Test
    fun phaseFollowsTheRealLight() {
        // Summer: still full sun at 17:30, sunset around 20:00, night later
        assertEquals(ZenPhase.DAYLIGHT, resolve(summer.withHour(17).withMinute(30))!!.phase)
        assertEquals(ZenPhase.SUNSET, resolve(summer.withHour(20))!!.phase)
        assertEquals(ZenPhase.NIGHT, resolve(summer.withHour(23))!!.phase)
        assertEquals(ZenPhase.MORNING, resolve(summer.withHour(5).withMinute(30))!!.phase)
        // Winter: 17:00 is already sunset, 19:00 is night
        assertEquals(ZenPhase.SUNSET, resolve(winter.withHour(17))!!.phase)
        assertEquals(ZenPhase.NIGHT, resolve(winter.withHour(19))!!.phase)
    }

    @Test
    fun overnightWindow_belongsToTheDayItStarted_andTimesOutAtItsEnd() {
        val evening = resolve(summer.withHour(22))!!
        val morning = resolve(summer.plusDays(1).withHour(8).withMinute(30).withSecond(15))!!
        assertEquals(evening.window, morning.window)
        assertEquals(9 * 60, morning.endMinutes)
        assertEquals(30 * 60_000L - 15_000L, morning.timeoutMillis)
        assertEquals(11 * 60 * 60_000L, evening.timeoutMillis)
    }

    @Test
    fun wholeDayWindow_hasNoEndAndNoTimeout() {
        val state = resolve(summer.withHour(12), start = 0, end = 0)!!
        assertNull(state.endMinutes)
        assertNull(state.timeoutMillis)
    }

    @Test
    fun dismissedByTheUser_staysHiddenForThatWindowOnly() {
        val window = resolve(summer.withHour(18))!!.window
        assertNull(resolve(summer.withHour(23), dismissed = window))
        assertNotNull(resolve(summer.plusDays(1).withHour(18), dismissed = window))
    }

    @Test
    fun contentKey_changesOnlyWithWhatIsVisible() {
        val a = resolve(summer.withHour(18), count = 2)!!
        val b = resolve(summer.withHour(18).withMinute(40), count = 2)!!
        assertEquals(a.contentKey, b.contentKey)
        assertNotEquals(a.contentKey, resolve(summer.withHour(18), count = 3)!!.contentKey)
        assertNotEquals(a.contentKey, resolve(summer.withHour(21))!!.contentKey)
    }

    @Test
    fun minutesUntilEnd_wrapsPastMidnight() {
        assertEquals(60, OffHours.minutesUntilEnd(6 * 60, 22 * 60, 7 * 60))
        assertEquals(9 * 60, OffHours.minutesUntilEnd(22 * 60, 22 * 60, 7 * 60))
        assertEquals(30, OffHours.minutesUntilEnd(16 * 60 + 30, 9 * 60, 17 * 60))
        assertNull(OffHours.minutesUntilEnd(12 * 60, 8 * 60, 8 * 60))
    }
}
