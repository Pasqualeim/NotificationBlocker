package com.pasquale.notificationblocker.widget

import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.QuickPause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class NookWidgetStateTest {

    // 2026-10-16 is a Friday
    private val friday = LocalDate.of(2026, 10, 16)
    private val lunch = Pause("lunch", "Pranzo", 13 * 60, 14 * 60, days = DayOfWeek.entries.take(5).toSet())
    private val evening = Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30)
    private val pauses = listOf(lunch, evening)

    @Test
    fun off_offersPauseNow() {
        val state = NookWidgetState.resolve(friday.atTime(10, 0), false, pauses, emptySet())
        assertEquals(NookWidgetState.Kind.OFF, state.kind)
        assertTrue(state.offersQuickPause)
    }

    @Test
    fun between_namesTheNextPause() {
        val state = NookWidgetState.resolve(friday.atTime(10, 0), true, pauses, emptySet())
        assertEquals(NookWidgetState.Kind.BETWEEN, state.kind)
        assertEquals(lunch, state.pause)
        assertEquals(13 * 60, state.minute)
        assertEquals(friday, state.date)
    }

    @Test
    fun paused_saysWhenWorkComesBack_andOffersNothing() {
        val state = NookWidgetState.resolve(friday.atTime(20, 0), true, pauses, emptySet())
        assertEquals(NookWidgetState.Kind.PAUSED, state.kind)
        assertEquals(evening, state.pause)
        assertEquals(8 * 60 + 30, state.minute)
        assertEquals(friday.plusDays(1), state.date)
        assertFalse(state.offersQuickPause)
    }

    @Test
    fun quickPause_showsAsPaused() {
        val quick = QuickPause.create(LocalDateTime.of(friday, java.time.LocalTime.of(10, 0)), 60)
        val state = NookWidgetState.resolve(friday.atTime(10, 30), true, pauses + quick, emptySet())
        assertEquals(NookWidgetState.Kind.PAUSED, state.kind)
        assertEquals(11 * 60, state.minute)
    }

    @Test
    fun endedEarly_isNotPaused() {
        val key = "$friday#evening"
        val state = NookWidgetState.resolve(friday.atTime(20, 0), true, pauses, setOf(key))
        assertEquals(NookWidgetState.Kind.ENDED, state.kind)
        assertFalse(state.offersQuickPause)
    }

    @Test
    fun noPauses_betweenWithNothingNext() {
        val state = NookWidgetState.resolve(friday.atTime(10, 0), true, emptyList(), emptySet())
        assertEquals(NookWidgetState.Kind.BETWEEN, state.kind)
        assertNull(state.pause)
    }
}
