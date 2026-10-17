package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepWindowTest {

    private val night = SleepWindow(23 * 60, 7 * 60)

    @Test
    fun overnightWindow_isTwoPartsOfTheDay() {
        assertEquals(listOf(0 to 420, 1380 to 1440), night.ofDay)
        assertTrue(23 * 60 + 30 in night)
        assertTrue(6 * 60 in night)
        assertFalse(7 * 60 in night)
    }

    @Test
    fun awake_cutsTheSleepOut() {
        // The evening 18:30-24:00 and the morning 00:00-08:30 of a pause, with sleep 23:00-07:00
        assertEquals(listOf(1110 to 1380), night.awake(1110, 1440))
        assertEquals(listOf(420 to 510), night.awake(0, 510))
        // A whole day keeps its 16 awake hours, in one piece
        assertEquals(listOf(420 to 1380), night.awake(0, 1440))
        // A daytime sleep (night shift) splits the day
        assertEquals(listOf(0 to 480, 960 to 1440), SleepWindow(8 * 60, 16 * 60).awake(0, 1440))
    }

    @Test
    fun sameTimes_leaveNothingOut() {
        val none = SleepWindow(0, 0)
        assertTrue(none.ofDay.isEmpty())
        assertEquals(listOf(0 to 1440), none.awake(0, 1440))
    }
}
