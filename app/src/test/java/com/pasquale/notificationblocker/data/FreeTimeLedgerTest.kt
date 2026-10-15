package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.LocalDateTime

class FreeTimeLedgerTest {

    // 2026-10-16 is a Friday; fixed sun: up 07:00, down 18:00
    private val friday = LocalDate.of(2026, 10, 16)
    private fun fri(h: Int, m: Int = 0) = friday.atTime(h, m)
    private fun sat(h: Int, m: Int = 0) = fri(h, m).plusDays(1)
    private val sun: (LocalDate) -> Pair<Int, Int> = { 7 * 60 to 18 * 60 }

    private val evening = Pause("evening", "", 17 * 60, 9 * 60)
    private val weekend = Pause("weekend", "", 0, 0, days = setOf(SATURDAY, SUNDAY))

    private fun config(
        pauses: List<Pause> = listOf(evening),
        enabled: Boolean = true,
        apps: Set<String> = setOf("work"),
        ended: Set<String> = emptySet(),
    ) = FreeTimeLedger.Config(enabled, apps, pauses, ended)

    private fun settle(
        from: LocalDateTime,
        to: LocalDateTime,
        config: FreeTimeLedger.Config = config(),
        days: Map<LocalDate, DayStats> = emptyMap(),
    ) = FreeTimeLedger.settle(days, from, to, config, sun)

    @Test
    fun countsOnlyMinutesInsideAPause() {
        val days = settle(fri(16), fri(19))
        assertEquals(120, days[friday]!!.livedMinutes)
        // 17:00-18:00 had the sun
        assertEquals(60, days[friday]!!.daylightMinutes)
        assertEquals(mapOf("evening" to 120), days[friday]!!.byPause)
    }

    @Test
    fun overnightPause_isSplitBetweenTheTwoDays() {
        val days = settle(fri(17), sat(9))
        assertEquals(7 * 60, days[friday]!!.livedMinutes)
        assertEquals(9 * 60, days[friday.plusDays(1)]!!.livedMinutes)
        // Saturday 07:00-09:00 in the sun
        assertEquals(120, days[friday.plusDays(1)]!!.daylightMinutes)
    }

    @Test
    fun nothingCounts_whenOffWithoutAppsOrEndedEarly() {
        assertTrue(settle(fri(17), fri(20), config(enabled = false)).isEmpty())
        assertTrue(settle(fri(17), fri(20), config(apps = emptySet())).isEmpty())
        assertTrue(settle(fri(17), fri(20), config(ended = setOf("2026-10-16#evening"))).isEmpty())
    }

    @Test
    fun overlappingPauses_countEachMinuteOnce_forTheFirstStarted() {
        val days = settle(sat(17), sat(19), config(listOf(evening, weekend)))
        val saturday = days[friday.plusDays(1)]!!
        assertEquals(120, saturday.livedMinutes)
        // The weekend started at midnight, before the evening
        assertEquals(mapOf("weekend" to 120), saturday.byPause)
    }

    @Test
    fun manySmallLooks_addUpToOneBigLook() {
        var days = emptyMap<LocalDate, DayStats>()
        var from = fri(16, 59).withSecond(30)
        repeat(50) {
            val to = from.plusSeconds(97)
            days = settle(from, to, days = days)
            from = to
        }
        assertEquals(settle(fri(16, 59).withSecond(30), from)[friday], days[friday])
    }

    @Test
    fun clockGoingBack_addsNothing() = assertTrue(settle(fri(20), fri(18)).isEmpty())

    @Test
    fun longGaps_countOnlyTheirLastThreeDays() {
        val days = settle(fri(17).minusDays(60), fri(17))
        assertNull(days[friday.minusDays(30)])
        assertTrue(days.keys.all { !it.isBefore(friday.minusDays(3)) })
    }

    @Test
    fun daylightSaving_countsRealTime() {
        val rome = java.time.ZoneId.of("Europe/Rome")
        // 2026-10-25: clocks go back at 03:00, the night from 22:00 to 07:00 lasts ten hours
        val all = FreeTimeLedger.Config(true, setOf("work"), listOf(Pause("n", "", 22 * 60, 7 * 60)), emptySet())
        val days = FreeTimeLedger.settle(emptyMap(), LocalDateTime.of(2026, 10, 24, 22, 0), LocalDateTime.of(2026, 10, 25, 7, 0), all, sun, rome)
        assertEquals(10 * 60, days.values.sumOf { it.livedMinutes })
    }

    @Test
    fun aLongGap_isCheap() {
        val start = System.nanoTime()
        settle(fri(17).minusDays(3), fri(17), config(listOf(evening, weekend)))
        assertTrue((System.nanoTime() - start) < 200_000_000L)
    }

    @Test
    fun heldNotifications_addUpPerDay() {
        val days = FreeTimeLedger.addHeld(FreeTimeLedger.addHeld(emptyMap(), friday), friday)
        assertEquals(2, days[friday]!!.held)
    }

    @Test
    fun codec_roundTrips_andSkipsBrokenLines() {
        val days = settle(fri(17), sat(9)).let { FreeTimeLedger.addHeld(it, friday) }
        val decoded = HistoryCodec.decode(HistoryCodec.encode(days) + "\nnot a day")
        assertEquals(days, decoded)
    }
}
