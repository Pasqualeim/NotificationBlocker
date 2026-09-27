package com.pasquale.notificationblocker.ui.zen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LifeCopyTest {

    // Summer day: sun ~05:34-20:24; winter day: ~07:37-16:55 (see ZenEnvironment.sunTimes)
    private val summer = LocalDate.of(2026, 7, 10)
    private val winter = LocalDate.of(2026, 1, 15)
    private val summerRise = ZenEnvironment.sunTimes(summer).first
    private val summerSet = ZenEnvironment.sunTimes(summer).second

    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m)

    @Test
    fun blockingOff_invitesToTakeTheSunBack() {
        assertEquals(LifeMessage.TurnOn, LifeCopy.message(at(summer, 18), false, 17 * 60, 9 * 60))
    }

    @Test
    fun offWorkInSummerAfternoon_countsTheSunshineLeft() {
        val message = LifeCopy.message(at(summer, 18), true, 17 * 60, 9 * 60)
        assertEquals(LifeMessage.SunLeft(summerSet - 18 * 60), message)
    }

    @Test
    fun sunshineLeft_isCappedByTheEndOfTheWindow() {
        // Lunch-break window 12:00-14:00: only 1 h left at 13:00 even if the sun sets much later
        assertEquals(LifeMessage.SunLeft(60), LifeCopy.message(at(summer, 13), true, 12 * 60, 14 * 60))
    }

    @Test
    fun offWorkAfterSunset_orBeforeSunrise() {
        assertEquals(LifeMessage.EveningIsYours, LifeCopy.message(at(summer, 22), true, 17 * 60, 9 * 60))
        assertEquals(LifeMessage.DayBeginning, LifeCopy.message(at(summer, 4), true, 17 * 60, 9 * 60))
    }

    @Test
    fun working_announcesTheSunWaitingAfterWork_notTheNextMorning() {
        val message = LifeCopy.message(at(summer, 10), true, 17 * 60, 9 * 60)
        assertEquals(LifeMessage.SunAfterWork(17 * 60, summerSet - 17 * 60), message)
    }

    @Test
    fun workingInWinter_theEveningWillBeYours() {
        assertEquals(LifeMessage.EveningAfterWork(18 * 60), LifeCopy.message(at(winter, 10), true, 18 * 60, 8 * 60))
    }

    @Test
    fun sunshineInWindow_includesEveningAndNextMorning() {
        val total = LifeCopy.sunshineInWindow(summer, 17 * 60, 9 * 60)
        assertEquals((summerSet - 17 * 60) + (9 * 60 - summerRise), total)
        assertEquals(summerSet - 17 * 60, LifeCopy.sunshineInWindow(summer, 17 * 60, 9 * 60, includeNextMorning = false))
        assertEquals(0, LifeCopy.sunshineInWindow(winter, 22 * 60, 7 * 60))
        assertTrue(LifeCopy.sunshineInWindow(summer, 0, 0) >= summerSet - summerRise)
    }

    @Test
    fun wholeDayWindow_isAlwaysOffWork() {
        assertTrue(LifeCopy.message(LocalDateTime.of(summer, LocalTime.NOON), true, 0, 0) is LifeMessage.SunLeft)
    }
}
