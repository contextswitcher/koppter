package io.github.koppor.koppter.ui

import androidx.compose.ui.text.style.TextDecoration
import kotlin.math.abs
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.key
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.outlook.OutlookCalendar
import io.github.koppor.koppter.outlook.OutlookBusy
import kotlin.time.Duration.Companion.minutes
import io.github.koppor.koppter.todotxt.Task
import androidx.compose.runtime.mutableStateListOf
import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.todotxt.Entry
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val NO_PLAN = "Nothing is planned yet. Plan gives the open tasks their times."

/**
 * What is up today and on the six days after it: the blocks of the plan by day, each with its task,
 * today and the two days after it also without blocks,
 * and for today the tasks that are due but have no block today.
 */
// [impl->dsn~agenda~1]
@Composable
fun Agenda(
    blocks: List<TextBlock>?,
    dueUnplanned: List<Entry>,
    entryOf: (TextBlock) -> Entry?,
    today: LocalDate,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    onWorked: (Entry, Int) -> Unit,
    days: Int = 7,
    running: Entry? = null,
    onClock: (Entry) -> Unit = {},
) {
    // A block is counted once, also when the button is hit twice.
    val counted = remember { mutableStateListOf<TextBlock>() }
    BoxWithConstraints {
        val narrow = maxWidth < 500.dp
        // The space below lets the last task scroll clear of the floating buttons.
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 88.dp).testTag("agenda"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (blocks == null) Text(NO_PLAN, Modifier.padding(8.dp))
            for (offset in 0 until days) {
                val day = today.plus(DatePeriod(days = offset))
                val ofDay = blocks.orEmpty().filter { it.date == day }
                if (ofDay.isEmpty() && offset > 2) continue
                Text(
                    listOfNotNull("Today".takeIf { offset == 0 }, "Tomorrow".takeIf { offset == 1 }, CalendarTxt.dayText(day)).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp).testTag("agenda-day-$day"),
                )
                if (ofDay.isEmpty()) Text("No block", Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (block in ofDay) {
                    val entry = entryOf(block)
                    val time = @Composable {
                        Text(CalendarTxt.timesText(block.span), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
                    }
                    val card = @Composable {
                        if (entry != null) {
                            TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
                        } else {
                            // The task was completed, deleted or edited after it was planned.
                            Text(block.text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val actions = @Composable {
                        // [impl->dsn~time-spent~1]
                        if (entry != null && offset == 0 && block !in counted) {
                            TextButton(
                                onClick = {
                                    counted += block
                                    onWorked(entry, block.span.length)
                                },
                                modifier = Modifier.testTag("agenda-worked"),
                            ) { Text("+${Task.durationText(block.span.length.minutes)} spent") }
                        }
                        if (entry != null && offset == 0) ClockButton(entry, running, onClock)
                    }
                    if (narrow) {
                        // On a phone, the card takes the whole width: the time and the buttons stand above it.
                        // The space above the time keeps the buttons with the card below them.
                    Column(Modifier.padding(top = 8.dp).testTag("agenda-block")) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                time()
                                Box(Modifier.weight(1f))
                                actions()
                            }
                            card()
                        }
                    } else {
                        Row(Modifier.testTag("agenda-block"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            time()
                            Box(Modifier.weight(1f)) { card() }
                            actions()
                        }
                    }
                }
                if (offset == 0 && dueUnplanned.isNotEmpty()) {
                    Text(
                        "Due and without a block today",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp).testTag("agenda-due"),
                    )
                    for (entry in dueUnplanned) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { TaskCard(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) }) }
                            ClockButton(entry, running, onClock)
                        }
                    }
                }
            }
        }
    }
}

/** The calendar of `Today` still shows the start of the text of a block. */
private val MIN_CALENDAR_WIDTH = 260.dp

/** A block of half an hour still shows the start of its text; a whole day fits on a phone that is held upright. */
private val MIN_HOUR_HEIGHT = 20.dp

/**
 * The appointments of Outlook that the calendar views show next to the blocks of the plan, and what the user does with one.
 *
 * @param onShow reads the appointments of the days from the first to before the second, which the view shows
 * @param sidelined whether the time of the appointment counts as free for planning
 */
// [impl->dsn~outlook-appointments~1]
class CalendarAppointments(
    val all: List<OutlookBusy>,
    val onShow: suspend (LocalDate, LocalDate) -> Unit,
    val sidelined: (OutlookBusy) -> Boolean,
    val onOpen: (OutlookBusy) -> Unit,
    val onSideline: (OutlookBusy, Boolean) -> Unit,
) {
    fun onDay(day: LocalDate): List<Pair<OutlookBusy, Span>> = OutlookCalendar.onDay(all, day)
}

/** The blocks of the plan of one week as a calendar: one column per day, the time running down; with [appointments], those of Outlook next to them. */
// [impl->dsn~agenda~1]
@Composable
fun Week(
    blocks: List<TextBlock>?,
    entryOf: (TextBlock) -> Entry?,
    suffixOf: (Entry) -> String,
    today: LocalDate,
    onOpen: (Entry) -> Unit,
    appointments: CalendarAppointments? = null,
) {
    val thisWeek = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
    var monday by remember { mutableStateOf(thisWeek) }
    val days = (0..6).map { monday.plus(DatePeriod(days = it)) }
    // [impl->dsn~outlook-appointments~1]
    LaunchedEffect(monday, appointments != null) { appointments?.onShow(monday, monday.plus(DatePeriod(days = 7))) }
    val ofWeek = blocks.orEmpty().filter { it.date in days }
    val spans = ofWeek.map { it.span } + days.flatMap { day -> appointments?.onDay(day).orEmpty().map { it.second } }
    // Office hours at least, so that an empty week has a grid as well.
    val firstHour = minOf(8, spans.minOfOrNull { it.start / 60 } ?: 8)
    val lastHour = maxOf(18, spans.maxOfOrNull { (it.end + 59) / 60 } ?: 18)

    Column(Modifier.padding(8.dp).testTag("week")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monday = monday.minus(DatePeriod(days = 7)) }, modifier = Modifier.testTag("week-previous")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            IconButton(onClick = { monday = monday.plus(DatePeriod(days = 7)) }, modifier = Modifier.testTag("week-next")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
            Text("Week ${CalendarTxt.week(monday)} · $monday to ${days.last()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("week-title"))
            if (monday != thisWeek) TextButton(onClick = { monday = thisWeek }, modifier = Modifier.testTag("week-today")) { Text("This week") }
        }
        if (blocks == null) Text(NO_PLAN, Modifier.padding(8.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // On a phone, the days keep a readable width and the week scrolls sideways.
            val dayWidth = maxOf(110.dp, (maxWidth - 44.dp) / 7)
            // The hours share the height that is left, so that the week is seen at once; only in a very low window the week scrolls.
            val hourHeight = maxOf(MIN_HOUR_HEIGHT, (maxHeight - 28.dp) / (lastHour - firstHour))
            Row(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(44.dp).padding(top = 28.dp).testTag("week-hours")) {
                    for (hour in firstHour until lastHour) {
                        Text("${hour.toString().padStart(2, '0')}:00", Modifier.height(hourHeight), style = MaterialTheme.typography.labelSmall)
                    }
                }
                for (day in days) {
                    Column(Modifier.width(dayWidth)) {
                        Text(
                            CalendarTxt.dayText(day).replace("${day.year}-", ""),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.height(28.dp).padding(start = 4.dp),
                        )
                        DayColumn(ofWeek.filter { it.date == day }, firstHour, lastHour, hourHeight, entryOf, suffixOf, onOpen, highlight = day == today, appointments = appointments, day = day)
                    }
                }
            }
        }
    }
}

/** Starts the clock of the task, or stops it if it runs. */
// [impl->dsn~time-tracking~1]
@Composable
private fun ClockButton(entry: Entry, running: Entry?, onClock: (Entry) -> Unit) {
    if (entry == running) {
        // Stays a word: the running task is the exception in the list, and core Material icons have no stop symbol.
        TextButton(onClick = { onClock(entry) }, modifier = Modifier.testTag("clock-stop")) { Text("Stop") }
    } else {
        IconButton(onClick = { onClock(entry) }, modifier = Modifier.testTag("clock-start")) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
        }
    }
}

/** The task whose clock runs, with the time since its start; shown above every view. */
// [impl->dsn~time-tracking~1]
@Composable
fun RunningClock(entry: Entry, now: kotlinx.datetime.LocalDateTime, onOpen: () -> Unit, onStop: () -> Unit, onAway: (() -> Unit)?) {
    val started = entry.task.started ?: return
    val elapsed = entry.task.runningFor(now)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().testTag("running-clock")) {
        BoxWithConstraints {
            val title = @Composable { modifier: Modifier ->
                Text(
                    entry.task.description,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier.clickable(onClick = onOpen),
                )
            }
            val controls = @Composable {
                Text(
                    "since ${started.time.toString().take(5)} · ${Task.durationText(elapsed).ifEmpty { "0m" }}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("running-clock-time"),
                )
                onAway?.let { TextButton(onClick = it, modifier = Modifier.testTag("go-away")) { Text("Away") } }
                TextButton(onClick = onStop, modifier = Modifier.testTag("running-clock-stop")) { Text("Stop") }
            }
            if (maxWidth < 600.dp) {
                // On a phone, the task gets a line of its own, so that its name is read in full.
                Column(Modifier.padding(start = 12.dp, top = 6.dp, end = 4.dp)) {
                    title(Modifier)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f))
                        controls()
                    }
                }
            } else {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    title(Modifier.weight(1f))
                    controls()
                }
            }
        }
    }
}

/** Since when the user is away, with the time since then; *Back* ends it. */
// [impl->dsn~time-log~1]
@Composable
fun AwayBar(since: kotlinx.datetime.LocalDateTime, now: kotlinx.datetime.LocalDateTime, onBack: () -> Unit) {
    val elapsed = Task.between(since, now)
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth().testTag("away-bar")) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Away", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "since ${since.time.toString().take(5)} · ${Task.durationText(elapsed).ifEmpty { "0m" }}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            TextButton(onClick = onBack, modifier = Modifier.testTag("away-back")) { Text("Back") }
        }
    }
}

/** The blocks of one day at their times, the hours from [firstHour] to [lastHour] running down; [nowMinute] draws a line at that time. */
@Composable
private fun DayColumn(
    blocks: List<TextBlock>,
    firstHour: Int,
    lastHour: Int,
    hourHeight: androidx.compose.ui.unit.Dp,
    entryOf: (TextBlock) -> Entry?,
    suffixOf: (Entry) -> String,
    onOpen: (Entry) -> Unit,
    highlight: Boolean,
    nowMinute: Int? = null,
    appointments: CalendarAppointments? = null,
    day: LocalDate? = null,
) {
    val ofDay = day?.let { appointments?.onDay(it) }.orEmpty()
    fun overlaps(a: Span, b: Span) = a.start < b.end && b.start < a.end
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(hourHeight * (lastHour - firstHour))
            .background(if (highlight) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
    ) {
        // Where a block and an appointment take the same time, the appointment stands in the left half and the block in the right one.
        val half = maxWidth / 2
        for (hour in 0 until lastHour - firstHour) HorizontalDivider(Modifier.offset(y = hourHeight * hour))
        // [impl->dsn~outlook-appointments~1]
        if (appointments != null) {
            for ((appointment, span) in ofDay) {
                key(appointment.entryId, appointment.start) {
                    AppointmentBox(
                        appointment, span, appointments,
                        Modifier.offset(y = hourHeight * (span.start - firstHour * 60) / 60).height(hourHeight * span.length / 60)
                            .then(if (blocks.any { overlaps(it.span, span) }) Modifier.width(half) else Modifier.fillMaxWidth()),
                    )
                }
            }
        }
        for (block in blocks) {
            val entry = entryOf(block)
            val beside = ofDay.any { overlaps(it.second, block.span) }
            Surface(
                color = if (entry != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (entry != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(start = if (beside) half else 0.dp).padding(horizontal = 2.dp).fillMaxWidth()
                    .offset(y = hourHeight * (block.span.start - firstHour * 60) / 60)
                    .height(hourHeight * block.span.length / 60)
                    .clickable(enabled = entry != null) { entry?.let(onOpen) }
                    .testTag("week-block"),
            ) {
                Text(
                    "${CalendarTxt.timesText(block.span).substringBefore('-')} ${entry?.let { it.task.description + suffixOf(it) } ?: block.text}",
                    style = MaterialTheme.typography.labelSmall,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        if (nowMinute != null && nowMinute in firstHour * 60 until lastHour * 60) {
            HorizontalDivider(
                Modifier.offset(y = hourHeight * (nowMinute - firstHour * 60) / 60).testTag("today-now"),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * An appointment of Outlook in a calendar view, in the color of its category in Outlook, so that it is recognized; one on the sideline is pale.
 * A click offers to open it in Outlook and to put it on the sideline or take it off.
 */
// [impl->dsn~outlook-appointments~1]
// [impl->dsn~outlook-sideline~1]
@Composable
private fun AppointmentBox(appointment: OutlookBusy, span: Span, actions: CalendarAppointments, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val sidelined = actions.sidelined(appointment)
    // The color is data that Outlook gives, not a color of the application: it is what the user knows the appointment by.
    val own = OutlookCalendar.rgb(appointment.color)?.let { (red, green, blue) -> Color(red, green, blue) }
    val container = (own ?: colors.secondaryContainer).let { if (sidelined) it.copy(alpha = 0.3f) else it }
    // Of the two text colors of the theme, the one that stands out more from the background.
    val content = when {
        sidelined || own == null -> if (own == null) colors.onSecondaryContainer else colors.onSurface
        abs(colors.onSurface.luminance() - own.luminance()) >= abs(colors.inverseOnSurface.luminance() - own.luminance()) -> colors.onSurface
        else -> colors.inverseOnSurface
    }
    Box(modifier) {
        Surface(
            color = container,
            contentColor = content,
            shape = MaterialTheme.shapes.extraSmall,
            border = if (sidelined) BorderStroke(1.dp, own ?: colors.outline) else null,
            modifier = Modifier.padding(horizontal = 2.dp).fillMaxSize().clickable { menu = true }
                .testTag(if (sidelined) "calendar-appointment-sideline" else "calendar-appointment"),
        ) {
            Text(
                "${CalendarTxt.timesText(span).substringBefore('-')} ${appointment.subject.ifEmpty { "(no subject)" }}",
                style = MaterialTheme.typography.labelSmall,
                textDecoration = if (sidelined) TextDecoration.LineThrough else null,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }, modifier = Modifier.testTag("appointment-menu")) {
            DropdownMenuItem(
                text = { Text("Open in Outlook") },
                onClick = {
                    menu = false
                    actions.onOpen(appointment)
                },
                modifier = Modifier.testTag("appointment-open"),
            )
            if (appointment.sideline || !sidelined) {
                DropdownMenuItem(
                    text = {
                        Text(
                            (if (appointment.sideline) "Take off the sideline" else "Sideline: its time counts as free") +
                                if (appointment.recurring) ", the whole series" else "",
                        )
                    },
                    onClick = {
                        menu = false
                        actions.onSideline(appointment, !appointment.sideline)
                    },
                    modifier = Modifier.testTag("appointment-sideline"),
                )
            } else {
                // A word of the plan dialog sidelines it; the category of Outlook cannot take it off.
                DropdownMenuItem(text = { Text("On the sideline by a word of its title") }, onClick = { menu = false }, enabled = false)
            }
        }
    }
}

/**
 * Today on one screen: the tasks of the agenda of today on the left and the day as a calendar on the right,
 * with a line at the current time. A narrow screen shows the tasks only.
 */
// [impl->dsn~today~1]
@Composable
fun Today(
    blocks: List<TextBlock>?,
    dueUnplanned: List<Entry>,
    entryOf: (TextBlock) -> Entry?,
    suffixOf: (Entry) -> String,
    today: LocalDate,
    nowMinute: Int,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    onWorked: (Entry, Int) -> Unit,
    running: Entry? = null,
    onClock: (Entry) -> Unit = {},
    onAway: (() -> Unit)? = null,
    calendarWidth: Int = 400,
    onCalendarWidth: (Int) -> Unit = {},
    appointments: CalendarAppointments? = null,
) {
    // [impl->dsn~outlook-appointments~1]
    LaunchedEffect(today, appointments != null) { appointments?.onShow(today, today.plus(DatePeriod(days = 1))) }
    val appointmentSpans = appointments?.onDay(today).orEmpty().map { it.second }
    val density = LocalDensity.current
    var shownWidth by remember(calendarWidth) { mutableStateOf(calendarWidth.dp) }
    val ofDay = blocks.orEmpty().filter { it.date == today }
    val planned = ofDay.filter { entryOf(it) != null }.sumOf { it.span.length }
    Column(Modifier.padding(8.dp).testTag("today")) {
        // The agenda below names the day; the sum of the planned time is what the day adds to it.
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            // [impl->dsn~time-log~1]
            // While a clock runs, its bar has the button.
            if (running == null) onAway?.let { TextButton(onClick = it, modifier = Modifier.testTag("go-away")) { Text("Away") } }
            Box(Modifier.weight(1f))
            Text(
                "${Task.durationText(planned.minutes).ifEmpty { "0m" }} planned",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("today-planned"),
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The cards of the agenda need about 500 dp to keep their labels on one line beside the text.
            val wide = maxWidth >= 760.dp
            val height = maxHeight
            // The agenda keeps 600 dp for its cards with times and buttons, also when the window has narrowed since the width was chosen.
            val maxCalendar = maxOf(MIN_CALENDAR_WIDTH, maxWidth - 600.dp)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { Agenda(blocks, dueUnplanned, entryOf, today, onToggle, onOpen, onWorked, days = 1, running = running, onClock = onClock) }
                if (wide) {
                    val shown = shownWidth.coerceIn(MIN_CALENDAR_WIDTH, maxCalendar)
                    // [impl->dsn~today-resize~1]
                    Box(
                        Modifier.fillMaxHeight().width(14.dp).testTag("today-resize")
                            .pointerHoverIcon(PointerIcon.Hand)
                            .draggable(
                                rememberDraggableState { delta -> shownWidth = (shownWidth.coerceIn(MIN_CALENDAR_WIDTH, maxCalendar) - with(density) { delta.toDp() }).coerceIn(MIN_CALENDAR_WIDTH, maxCalendar) },
                                Orientation.Horizontal,
                                onDragStopped = { onCalendarWidth(shownWidth.value.roundToInt()) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(4.dp, 48.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    }
                    val spans = ofDay.map { it.span } + appointmentSpans
                    val firstHour = minOf(8, spans.minOfOrNull { it.start / 60 } ?: 8, nowMinute / 60)
                    val lastHour = maxOf(18, spans.maxOfOrNull { (it.end + 59) / 60 } ?: 18, nowMinute / 60 + 1)
                    val hourHeight = maxOf(MIN_HOUR_HEIGHT, (height - 8.dp) / (lastHour - firstHour))
                    Row(Modifier.width(shown).padding(top = 8.dp).verticalScroll(rememberScrollState()).testTag("today-day")) {
                        Column(Modifier.width(44.dp)) {
                            for (hour in firstHour until lastHour) {
                                Text("${hour.toString().padStart(2, '0')}:00", Modifier.height(hourHeight), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DayColumn(ofDay, firstHour, lastHour, hourHeight, entryOf, suffixOf, onOpen, highlight = true, nowMinute = nowMinute, appointments = appointments, day = today)
                    }
                }
            }
        }
    }
}

/** The contexts that the typed text asks for: first those of which a level starts with it, then those that contain it. */
internal fun matchingContexts(contexts: List<String>, typed: String): List<String> {
    val wanted = typed.trimStart('@').lowercase()
    val (starting, containing) = contexts.filter { wanted in it.lowercase() }
        .partition { context -> context.lowercase().split(LEVEL_SEPARATOR).any { it.startsWith(wanted) } }
    return starting + containing
}

/**
 * The tasks without context, to be sorted. One task is marked; typing narrows the contexts, and Enter gives the first of them
 * to the marked task, or the typed text as a new context if none is left. With the pointer, a click on a context gives it to its task.
 */
// [impl->dsn~inbox~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Inbox(entries: List<Entry>, contexts: List<String>, onAssign: (Entry, String) -> Unit, onToggle: (Entry) -> Unit, onOpen: (Entry) -> Unit) {
    var typed by remember { mutableStateOf("") }
    var mark by remember { mutableStateOf(0) }
    // A task that got its context leaves the list, so the mark is then on the next one.
    val marked = mark.coerceIn(0, maxOf(0, entries.lastIndex))
    val matching = matchingContexts(contexts, typed)
    val focus = remember { FocusRequester() }
    val keyboard = LocalKeyboard.current
    LaunchedEffect(Unit) { if (keyboard) focus.requestFocus() }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp).testTag("inbox"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entries.isEmpty()) {
            Text("Every task has a context.", Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filterNot(Char::isWhitespace) },
                label = { Text("Context for the marked task") },
                supportingText = { Text("Enter gives the first context shown, or the typed one if none is left. The arrow keys mark another task.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("inbox-typed").onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.Enter -> (matching.firstOrNull() ?: typed.trimStart('@')).takeIf { typed.isNotEmpty() && it.isNotEmpty() }?.let { context ->
                            onAssign(entries[marked], context)
                            typed = ""
                        }
                        Key.DirectionDown -> mark = marked + 1
                        Key.DirectionUp -> mark = marked - 1
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            )
        }
        for ((index, entry) in entries.withIndex()) {
            Card(
                onClick = { onOpen(entry) },
                border = if (index == marked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth().testTag(if (index == marked) "inbox-marked" else "inbox-task"),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // The hashtags tell where a task comes from, such as #outlook; the task has no context and no project to show here.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
                        // A task that is done needs no context.
                        Checkbox(checked = entry.task.completed, onCheckedChange = { onToggle(entry) }, modifier = Modifier.testTag("task-done"))
                        Text(entry.task.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 4.dp))
                        entry.task.hashtags.forEach { HashtagLabel(it, Modifier.testTag("inbox-hashtag-$it")) }
                    }
                    // The contexts below are choices, not contexts of the task: a click gives the task that context.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
                        Text("Give it:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 5.dp))
                        for (context in if (index == marked) matching else contexts) {
                            // The area that reacts to a click is larger than the label, which is too small for a finger.
                            ContextLabel(
                                context,
                                Modifier.clip(CircleShape).clickable {
                                    onAssign(entry, context)
                                    // Back to the field, so that typing goes on after a click; the field is gone with the last task.
                                    if (keyboard && entries.size > 1) focus.requestFocus()
                                }.testTag("inbox-context-$context").padding(vertical = 5.dp),
                                path = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
