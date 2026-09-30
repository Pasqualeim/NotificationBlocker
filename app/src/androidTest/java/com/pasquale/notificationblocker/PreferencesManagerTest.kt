package com.pasquale.notificationblocker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pasquale.notificationblocker.data.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real SharedPreferences on a device: what the JVM unit tests cannot cover. */
@RunWith(AndroidJUnit4::class)
class PreferencesManagerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        clear()
        prefs = PreferencesManager.getInstance(context)
    }

    @After
    fun tearDown() = clear()

    private fun clear() {
        context.getSharedPreferences("notification_blocker_prefs", 0).edit().clear().commit()
    }

    @Test
    fun defaults_areOffAndTenToSeven() {
        assertFalse(prefs.isBlockingEnabled)
        assertEquals(22 * 60, prefs.startTimeMinutes)
        assertEquals(7 * 60, prefs.endTimeMinutes)
        assertTrue(prefs.getBlockedApps().isEmpty())
        assertFalse(prefs.shouldBlock("com.example.work"))
    }

    @Test
    fun blockedApps_roundTripAndRemove() {
        prefs.setAppBlocked("a.b", true)
        prefs.setAppBlocked("c.d", true)
        assertEquals(setOf("a.b", "c.d"), prefs.getBlockedApps())
        prefs.setAppBlocked("a.b", false)
        assertEquals(setOf("c.d"), prefs.getBlockedApps())
    }

    @Test
    fun shouldBlock_wholeDayWindowBlocksOnlySelectedApps() {
        prefs.isBlockingEnabled = true
        prefs.startTimeMinutes = 480
        prefs.endTimeMinutes = 480 // start == end: the whole day
        prefs.setAppBlocked("work.app", true)
        assertTrue(prefs.shouldBlock("work.app"))
        assertFalse(prefs.shouldBlock("friends.app"))
        prefs.isBlockingEnabled = false
        assertFalse(prefs.shouldBlock("work.app"))
    }

    @Test
    fun recordFiltered_countsEachNotificationOnce_andRestartsPerWindow() {
        prefs.recordFiltered("2026-09-29", "work.app", "key1")
        prefs.recordFiltered("2026-09-29", "work.app", "key1") // an update of the same notification
        prefs.recordFiltered("2026-09-29", "work.app", "key2")
        prefs.recordFiltered("2026-09-29", "other.app", "key3")
        assertEquals(3, prefs.filteredCount("2026-09-29"))
        assertEquals(0, prefs.filteredCount("2026-09-30"))
        prefs.recordFiltered("2026-09-30", "work.app", "key1")
        assertEquals(1, prefs.filteredCount("2026-09-30"))
    }

    @Test
    fun lastReport_isNullUntilSomethingWasHeld() {
        assertNull(prefs.lastReport())
        prefs.recordFiltered("2026-09-29", "work.app", "k")
        assertEquals(1, prefs.lastReport()?.total)
    }
}
