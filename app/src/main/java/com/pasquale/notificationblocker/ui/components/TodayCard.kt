package com.pasquale.notificationblocker.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.data.Occurrence
import com.pasquale.notificationblocker.data.OffHours
import com.pasquale.notificationblocker.data.Pause
import com.pasquale.notificationblocker.data.WorkSchedule
import com.pasquale.notificationblocker.ui.theme.Motion
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Where one of today's pauses stands. */
enum class TodayPauseState { LIVED, NOW, ENDED, LATER }

/**
 * One of today's pauses on Home: its stretch of today (minutes 0-1440), where it stands, and for the
 * whole occurrence (which may start yesterday or end tomorrow) its length and the minutes left.
 */
@Immutable
data class TodayPause(
    val pause: Pause,
    val colorIndex: Int,
    val startMinute: Int,
    val endMinute: Int,
    val state: TodayPauseState,
    val totalMinutes: Int = endMinute - startMinute,
    val minutesLeft: Int = 0,
)

/** When work notifications come back: [daysAway] from today, on [day], at [minute] (null: at the end of that day). */
@Immutable
data class WorkBack(val daysAway: Int, val day: LocalDate, val minute: Int?)

object TodayPauses {

    /** Today's stretches of every enabled pause, in time order, with where each stands at [now]. */
    fun of(pauses: List<Pause>, now: LocalDateTime, ended: Set<String>): List<TodayPause> {
        val today: LocalDate = now.toLocalDate()
        val dayStart = today.atStartOfDay()
        val dayEnd = dayStart.plusDays(1)
        val slots = WorkSchedule.colorSlots(pauses)
        return pauses.map { pause -> slots.getValue(pause.id) to pause }
            .filter { it.second.enabled }
            .flatMap { (index, pause) ->
                (pause.nights downTo 0).map { today.minusDays(it.toLong()) }
                    .filter { pause.startsOn(it) }
                    .map { Occurrence(pause, it) }
                    .filter { it.end.isAfter(dayStart) && it.start.isBefore(dayEnd) }
                    .map { o ->
                        val a = maxOf(o.start, dayStart)
                        val b = minOf(o.end, dayEnd)
                        val state = when {
                            o.key in ended && now.isBefore(o.end) -> TodayPauseState.ENDED
                            !now.isBefore(o.end) -> TodayPauseState.LIVED
                            !now.isBefore(o.start) -> TodayPauseState.NOW
                            else -> TodayPauseState.LATER
                        }
                        TodayPause(
                            pause, index, minutes(dayStart, a), minutes(dayStart, b), state,
                            totalMinutes = java.time.Duration.between(o.start, o.end).toMinutes().toInt(),
                            minutesLeft = java.time.Duration.between(now, o.end).toMinutes().toInt().coerceAtLeast(0),
                        )
                    }
            }
            .sortedBy { it.startMinute }
    }

    /** The stretch that matters most for each pause: on now, then ended early, then later today, then lived. */
    fun oneEach(pauses: List<TodayPause>): List<TodayPause> =
        // The time off work is one line, though last night's stretch and tonight's have different ids
        pauses.groupBy { if (WorkSchedule.isOffWork(it.pause)) WorkSchedule.OFF_WORK else it.pause.id }.values
            .map { same -> same.minBy { listOf(TodayPauseState.NOW, TodayPauseState.ENDED, TodayPauseState.LATER, TodayPauseState.LIVED).indexOf(it.state) } }
            .sortedBy { it.startMinute }

    /**
     * When work comes back ([until], from `PauseSchedule.blockedUntil`), as the "Now" line says it: the day
     * it shows under and its minute, or a null minute for "all of that day" (a midnight end belongs to the
     * day before, so a whole weekend reads "through Sunday", not "until Monday 00:00").
     */
    fun workBack(now: LocalDateTime, until: LocalDateTime): WorkBack {
        val midnight = until.toLocalTime() == LocalTime.MIDNIGHT
        val day = if (midnight) until.toLocalDate().minusDays(1) else until.toLocalDate()
        return WorkBack(
            daysAway = ChronoUnit.DAYS.between(now.toLocalDate(), day).toInt(),
            day = day,
            minute = if (midnight) null else until.hour * 60 + until.minute,
        )
    }

    private fun minutes(from: LocalDateTime, to: LocalDateTime) =
        java.time.Duration.between(from, to).toMinutes().toInt().coerceIn(0, OffHours.MINUTES_PER_DAY)
}

/**
 * "Today" on Home, as a segmented list like the system apps' settings: a section title, then the first
 * segment with the pause on now and how far it has gone (or the next one today) over a 24 h bar with
 * every pause in its color and a dot at the current time, then one segment per pause with its avatar,
 * times and where it stands.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodayCard(
    pauses: List<TodayPause>,
    nowMinute: Int,
    modifier: Modifier = Modifier,
    isBlockingEnabled: Boolean = true,
    workBack: WorkBack? = null,
) {
    // One line per pause, even when it shows twice on the bar (last night's evening and tonight's)
    val rows = TodayPauses.oneEach(pauses)
    val count = 1 + rows.size
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        Text(
            text = stringResource(R.string.today_card_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        val first = ListItemDefaults.segmentedShapes(index = 0, count = count)
        Surface(
            shape = first.shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isBlockingEnabled) NowLine(pauses = pauses, nowMinute = nowMinute, workBack = workBack)
                val colors = pauses.associate { it.colorIndex to pauseColor(it.colorIndex) }
                DayBar(pauses = pauses, colorOf = { colors[it] ?: Color.Unspecified }, nowMinute = nowMinute)
                if (pauses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.today_no_pauses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        rows.forEachIndexed { i, p ->
            // A plain row in the segment's shape, not a SegmentedListItem: its minimum height left
            // empty room under each pause, and every dp here is taken from the scene (Home fits one screen)
            Surface(
                shape = ListItemDefaults.segmentedShapes(index = i + 1, count = count).shape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                // One TalkBack stop per pause: name, times and state together
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PauseAvatar(pause = p.pause, index = p.colorIndex, size = 36.dp, enabled = isBlockingEnabled)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = pauseName(p.pause), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            text = pauseTimes(p.pause),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // With Nook off nothing is on or lived; a pause still to come needs no label
                    if (isBlockingEnabled) StateChip(p.state)
                }
            }
        }
    }
}

/**
 * The pause on now, how much of it is gone and when work comes back, or the next one today; nothing
 * when neither.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowLine(pauses: List<TodayPause>, nowMinute: Int, workBack: WorkBack?) {
    // The bar follows the pause that ends first; the time on the right is when work really comes back,
    // past every pause that follows on (the evening, then the weekend)
    val now = pauses.filter { it.state == TodayPauseState.NOW }.minByOrNull { it.minutesLeft }
    val next = pauses.firstOrNull { it.state == TodayPauseState.LATER && it.startMinute > nowMinute }
    when {
        now != null -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val total = now.totalMinutes.coerceAtLeast(1)
            val left = now.minutesLeft.coerceIn(0, total)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.today_now, pauseName(now.pause)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = workBackText(workBack, nowMinute) ?: stringResource(R.string.today_left, durationText(left)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            val target = 1f - left.toFloat() / total
            val progress = remember { Animatable(0f) }
            LaunchedEffect(target) { progress.animateTo(target, Motion.standard(Motion.SLOW)) }
            // The wave stands still (waveSpeed 0): a moving one redraws the page on every frame
            LinearWavyProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth(),
                color = pauseColor(now.colorIndex),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                waveSpeed = 0.dp,
            )
        }
        next != null -> Text(
            text = stringResource(R.string.today_next, pauseName(next.pause), OffHours.format(next.startMinute), durationText(next.startMinute - nowMinute)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "ancora 3 h", "fino a domani alle 08:30", "fino a lunedì alle 08:30", "fino a tutto domenica"; null when unknown. */
@Composable
private fun workBackText(back: WorkBack?, nowMinute: Int): String? {
    back ?: return null
    if (back.daysAway <= 0) {
        // The minute tick can run a minute ahead of the time blockedUntil was computed at: fall back then
        val left = (back.minute ?: OffHours.MINUTES_PER_DAY) - nowMinute
        return if (left > 0) stringResource(R.string.today_left, durationText(left)) else null
    }
    val locale = LocalConfiguration.current.locales[0]
    val day = if (back.daysAway == 1) stringResource(R.string.today_day_tomorrow) else OffHours.date("EEEE", back.day, locale)
    return if (back.minute == null) stringResource(R.string.today_through_day, day)
    else stringResource(R.string.today_until_day, day, OffHours.format(back.minute))
}

/** Small pill with where a pause stands today; none for a pause still to come. */
@Composable
private fun StateChip(state: TodayPauseState) {
    val (text, container, content) = when (state) {
        TodayPauseState.NOW -> Triple(R.string.pause_state_now, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        TodayPauseState.LIVED -> Triple(R.string.pause_state_lived, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
        TodayPauseState.ENDED -> Triple(R.string.pause_state_ended, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
        TodayPauseState.LATER -> return
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .background(container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun DayBar(pauses: List<TodayPause>, colorOf: (Int) -> Color, nowMinute: Int) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val dotFill = MaterialTheme.colorScheme.surface
    val dotRing = MaterialTheme.colorScheme.onSurface
    val day = OffHours.MINUTES_PER_DAY.toFloat()
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            val barH = 10.dp.toPx()
            val top = (size.height - barH) / 2
            val r = CornerRadius(barH / 2)
            drawRoundRect(track, Offset(0f, top), Size(size.width, barH), r)
            for (p in pauses) {
                val x = size.width * p.startMinute / day
                val w = maxOf(barH, size.width * (p.endMinute - p.startMinute) / day)
                drawRoundRect(colorOf(p.colorIndex), Offset(x, top), Size(w, barH), r)
            }
            val nx = size.width * nowMinute / day
            drawCircle(dotFill, 6.dp.toPx(), Offset(nx, size.height / 2))
            drawCircle(dotRing, 6.dp.toPx(), Offset(nx, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (h in listOf(0, 6, 12, 18, 24)) {
                Text(
                    text = OffHours.format(h * 60).substring(0, 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val previewPauses = listOf(
    TodayPause(Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30), 0, 0, 510, TodayPauseState.LIVED),
    TodayPause(Pause("lunch", "Pranzo", 13 * 60, 14 * 60), 1, 780, 840, TodayPauseState.LIVED),
    TodayPause(Pause("evening", "Sera", 18 * 60 + 30, 8 * 60 + 30), 0, 1110, 1440, TodayPauseState.NOW),
)

@Preview(showBackground = true, name = "Today card")
@Composable
fun TodayCardPreview() {
    NotificationBlockerTheme {
        TodayCard(previewPauses, nowMinute = 1132, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Today card - no pauses (dark)")
@Composable
fun TodayCardEmptyPreview() {
    NotificationBlockerTheme(darkTheme = true) {
        TodayCard(emptyList(), nowMinute = 600, modifier = Modifier.padding(16.dp))
    }
}
