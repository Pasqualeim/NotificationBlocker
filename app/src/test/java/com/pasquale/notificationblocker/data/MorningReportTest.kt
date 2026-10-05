package com.pasquale.notificationblocker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MorningReportTest {

    private val today = LocalDate.of(2026, 9, 29)

    @Test
    fun packageOf_readsNewAndLegacyEntries() {
        assertEquals("com.slack", MorningReport.packageOf(MorningReport.entry("com.slack", "0|com.slack|4|null|10123")))
        assertEquals("com.microsoft.teams", MorningReport.packageOf("0|com.microsoft.teams|7|tag|10200"))
        assertNull(MorningReport.packageOf("garbage"))
        assertEquals("com.slack", MorningReport.packageOf("com.slack\n0|com.slack|4|null|10123"))
    }

    @Test
    fun entry_neverStoresTheNotificationKey() {
        // A messaging app's tag can be the chat id, with the phone number in it
        val key = "0|com.whatsapp|1|393331234567@s.whatsapp.net|10123"
        val entry = MorningReport.entry("com.whatsapp", key)
        assertFalse(entry.contains("3933312"))
        assertEquals(entry, MorningReport.entry("com.whatsapp", key)) // same notification, same entry
        assertNotEquals(entry, MorningReport.entry("com.whatsapp", key.replace("|1|", "|2|")))
    }

    @Test
    fun build_countsPerAppMostFirst() {
        val entries = setOf(
            MorningReport.entry("com.slack", "a"),
            MorningReport.entry("com.slack", "b"),
            MorningReport.entry("com.slack", "c"),
            MorningReport.entry("com.teams", "d"),
            "0|com.gmail|1|null|1",
        )
        val report = MorningReport.build("2026-09-28", 5, entries)!!
        assertEquals(5, report.total)
        assertEquals(listOf(AppCount("com.slack", 3), AppCount("com.gmail", 1), AppCount("com.teams", 1)), report.apps)
    }

    @Test
    fun build_nothingHeld_isNull() {
        assertNull(MorningReport.build("2026-09-28", 0, emptySet()))
        assertNull(MorningReport.build(null, 3, emptySet()))
    }

    @Test
    fun shouldShow_onceAfterTheWindow_forRecentWindowsOnly() {
        val lastNight = MorningReport("2026-09-28", 4, emptyList())
        assertTrue(MorningReport.shouldShow(lastNight, today, currentWindow = null, seenWindow = null))
        // Still inside that window
        assertFalse(MorningReport.shouldShow(lastNight, today, currentWindow = "2026-09-28", seenWindow = null))
        // Already dismissed
        assertFalse(MorningReport.shouldShow(lastNight, today, currentWindow = null, seenWindow = "2026-09-28"))
        // A window from days ago is not "last night"
        assertFalse(MorningReport.shouldShow(MorningReport("2026-09-25", 4, emptyList()), today, null, null))
        // A daytime window that ended earlier today
        assertTrue(MorningReport.shouldShow(MorningReport("2026-09-29", 2, emptyList()), today, null, null))
        assertFalse(MorningReport.shouldShow(null, today, null, null))
    }
}
