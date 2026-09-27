package com.pasquale.notificationblocker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pasquale.notificationblocker.data.OffHours
import kotlinx.coroutines.delay

/**
 * Minutes from midnight, updated at the start of every minute while the screen is started.
 * The clock is re-read immediately on every start: `delay` does not advance while the device
 * sleeps, so after an unlock (or a time/zone change) the value is fresh instead of up to a
 * minute stale. Nothing ticks in the background.
 */
@Composable
fun rememberCurrentMinutes(): State<Int> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(initialValue = OffHours.currentMinutes(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = OffHours.currentMinutes()
                delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
            }
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
