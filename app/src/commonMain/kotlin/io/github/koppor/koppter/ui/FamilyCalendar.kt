package io.github.koppor.koppter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.Family
import io.github.koppor.koppter.plan.RankingPeriod
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.todotxt.Entry
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** A column still shows some words of a task beside the box and the stars; with more members than fit, the columns scroll sideways. */
private val MIN_MEMBER_WIDTH = 200.dp
private val HOURS_WIDTH = 44.dp

/**
 * The day of a family: the ranking of the members by their stars, and a column per member with the tasks of the day to tick off
 * and, below them, the day as a calendar with the appointments of the member and the blocks of the `calendar.txt` of the list
 * of the member, such as the times at which the member is absent.
 */
// [impl->dsn~family-view~1]
@Composable
fun FamilyCalendar(
    entries: List<Entry>,
    members: List<String>,
    blocksOf: (String) -> List<TextBlock>,
    today: LocalDate,
    nowMinute: Int,
    period: RankingPeriod,
    onPeriod: (RankingPeriod) -> Unit,
    onToggle: (Entry) -> Unit,
    onOpen: (Entry) -> Unit,
    onAdd: (String) -> Unit,
    /** What kept the absences of members from being updated. */
    problems: List<String> = emptyList(),
) {
    // With the date as key: a tablet that runs for weeks shows the new day each morning.
    var day by remember(today) { mutableStateOf(today) }
    val ranking = Family.ranking(entries.map { it.task }, members, period, day)
    val appointments = members.associateWith { Family.appointments(entries, it, members, day) }
    val blocks = members.associateWith { member -> blocksOf(member).filter { it.date == day } }
    val spans = appointments.values.flatten().map { it.span } + blocks.values.flatten().map { it.span }
    // From breakfast to the evening at least, so that an empty day has a grid as well.
    val firstHour = minOf(7, spans.minOfOrNull { it.start / 60 } ?: 7)
    val lastHour = maxOf(21, spans.maxOfOrNull { (it.end + 59) / 60 } ?: 21)

    Column(Modifier.padding(8.dp).testTag("family")) {
        // On a phone, the periods of the ranking wrap below the day instead of leaving the screen.
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { day = day.minus(DatePeriod(days = 1)) }, modifier = Modifier.testTag("family-previous")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                IconButton(onClick = { day = day.plus(DatePeriod(days = 1)) }, modifier = Modifier.testTag("family-next")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
                Text((if (day == today) "Today, " else "") + CalendarTxt.dayText(day), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("family-day"))
                if (day != today) TextButton(onClick = { day = today }, modifier = Modifier.testTag("family-today")) { Text("Today") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (choice in RankingPeriod.entries) {
                    FilterChip(
                        selected = choice == period,
                        onClick = { onPeriod(choice) },
                        label = { Text(choice.title) },
                        modifier = Modifier.testTag("ranking-${choice.name.lowercase()}"),
                    )
                }
            }
            // All members in one place: of the columns, a phone shows two.
            FlowRow(Modifier.testTag("family-ranking"), itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (rank in ranking) {
                    // The first place stands out; with no stars at all, nobody is first.
                    val first = rank.place == 1 && rank.stars > 0
                    Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${rank.place}. ${rank.member} ${rank.stars}",
                            fontWeight = if (first) FontWeight.Bold else null,
                            color = if (first) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("family-rank-${rank.member}"),
                        )
                        Icon(Icons.Filled.Star, contentDescription = "stars", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        // [impl->dsn~absences~1]
        for (problem in problems) {
            Text(problem, Modifier.padding(horizontal = 12.dp).testTag("family-problem"), color = MaterialTheme.colorScheme.error)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val width = maxOf(MIN_MEMBER_WIDTH, (maxWidth - HOURS_WIDTH) / members.size)
            // The tasks leave the larger part to the calendar; more tasks than fit scroll.
            val tasksHeight = maxHeight * 0.4f
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Row {
                    Spacer(Modifier.width(HOURS_WIDTH))
                    for (member in members) MemberHeader(member, onAdd = { onAdd(member) }, Modifier.width(width))
                }
                Row(Modifier.heightIn(max = tasksHeight).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.width(HOURS_WIDTH))
                    for (member in members) {
                        Column(Modifier.width(width).testTag("family-tasks-$member")) {
                            val tasks = Family.todos(entries, member, members, day, today)
                            if (tasks.isEmpty()) {
                                Text("No tasks", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            for (entry in tasks) FamilyTask(entry, today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry) })
                        }
                    }
                }
                HorizontalDivider(Modifier.width(HOURS_WIDTH + width * members.size))
                BoxWithConstraints(Modifier.weight(1f)) {
                    // The hours share the height that is left, so that the day is seen at once; only in a low window it scrolls.
                    val hourHeight = maxOf(MIN_HOUR_HEIGHT, maxHeight / (lastHour - firstHour))
                    Row(Modifier.verticalScroll(rememberScrollState())) {
                        Column(Modifier.width(HOURS_WIDTH).testTag("family-hours")) {
                            for (hour in firstHour until lastHour) {
                                Text("${hour.toString().padStart(2, '0')}:00", Modifier.height(hourHeight), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        for (member in members) {
                            // An appointment is a block with a task; the number of its line tells two with the same text apart.
                            val tasks = appointments.getValue(member).associate { TextBlock(day, it.span, "${it.entry.task.description} #${it.entry.index}") to it.entry }
                            Box(Modifier.width(width).testTag("family-calendar-$member")) {
                                // The appointments last, so that one in a time of absence lies on top of it.
                                DayColumn(
                                    blocks.getValue(member) + tasks.keys, firstHour, lastHour, hourHeight,
                                    entryOf = { tasks[it] }, suffixOf = { "" }, onOpen = onOpen, highlight = false,
                                    nowMinute = nowMinute.takeIf { day == today },
                                    ends = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The name of a member and the button for a new task of the member. */
@Composable
private fun MemberHeader(member: String, onAdd: () -> Unit, modifier: Modifier) {
    Row(modifier.padding(start = 12.dp).testTag("family-member-$member"), verticalAlignment = Alignment.CenterVertically) {
        Text(member, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onAdd, modifier = Modifier.testTag("family-add-$member")) {
            Icon(Icons.Filled.Add, contentDescription = "New task of $member")
        }
    }
}

/** A task of a member: the box to tick it off, its text, which opens it, and the stars that it is worth. */
@Composable
private fun FamilyTask(entry: Entry, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit) {
    val task = entry.task
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("family-task"), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.completed, onCheckedChange = { onToggle() }, modifier = Modifier.testTag("family-toggle"))
        Column(Modifier.weight(1f)) {
            Text(
                task.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            // In words: a color alone does not say what is wrong with the task.
            if (task.isOverdue(today)) {
                Text("Overdue since ${task.deadline ?: task.due}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("family-overdue"))
            }
        }
        Text("${task.stars}", Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Filled.Star, contentDescription = "stars", Modifier.padding(end = 8.dp).size(16.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
