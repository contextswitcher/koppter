package io.github.koppor.koppter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.Holidays
import io.github.koppor.koppter.storage.ConfigFile
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The parts of the configuration for planning, each as lines of the form `name = value`. */
data class PlanSettings(val timeMaps: String, val contextMaps: String, val barriers: String, val limits: String = "", val sideline: String = "")

/** What a run of the planner did, as texts for the user. */
data class PlanResult(val summary: String, val problems: List<String>, val unplaced: List<String>)

/**
 * Edits the time maps, the time maps of the contexts and the barriers, and plans the open tasks into `calendar.txt`.
 * The dialog stays open after planning and tells what was planned and which tasks do not fit.
 */
// [impl->dsn~plan-dialog~1]
@Composable
fun PlanDialog(
    settings: PlanSettings,
    /** The addresses of the calendars with busy times; `null` if the platform cannot read them. */
    calendars: String?,
    onPlan: suspend (PlanSettings, calendars: String?) -> PlanResult?,
    onDismiss: () -> Unit,
    today: LocalDate,
) {
    var timeMaps by remember { mutableStateOf(settings.timeMaps) }
    var contextMaps by remember { mutableStateOf(settings.contextMaps) }
    var barriers by remember { mutableStateOf(settings.barriers) }
    var limits by remember { mutableStateOf(settings.limits) }
    var sideline by remember { mutableStateOf(settings.sideline) }
    var addresses by remember { mutableStateOf(calendars) }
    var result by remember { mutableStateOf<PlanResult?>(null) }
    var running by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var choosingState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    @Composable
    fun field(label: String, value: String, onChange: (String) -> Unit, help: String, tag: String) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            // In the small style also while the field is empty: at the size of the content, the label reads as content.
            label = { Text("$label (optional)", style = MaterialTheme.typography.bodySmall) },
            supportingText = { Text(help) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }

    val state = listOf(timeMaps, contextMaps, barriers, limits, sideline, addresses)
    // Planning saves what the fields hold.
    var saved by remember { mutableStateOf(state) }

    GuardedDialog(changed = state != saved, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("plan-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Gives each open task the earliest free time of its time map, tasks with a higher priority first, and writes the result to " +
                        "${CalendarTxt.NAME} next to the todo.txt file. The settings are kept in ${ConfigFile.NAME}.",
                    style = MaterialTheme.typography.bodySmall,
                )
                // [impl->dsn~time-map-editor~1]
                val maps = ConfigFile.parseLines(timeMaps)
                OutlinedCard(Modifier.fillMaxWidth().testTag("plan-time-maps")) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Time maps (optional)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                        for ((key, value) in maps) MapRow(key, value) { editing = key }
                        FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editing = "" }, modifier = Modifier.testTag("plan-add-map")) { Text("Add time map") }
                            // The holidays go to the first map, the general times; the maps restricted to it follow.
                            // [impl->dsn~holidays~1]
                            maps.firstOrNull()?.first?.substringBefore(' ')?.let { first ->
                                Box {
                                    TextButton(onClick = { choosingState = true }, modifier = Modifier.testTag("plan-add-holidays")) { Text("Add holidays to $first") }
                                    DropdownMenu(choosingState, onDismissRequest = { choosingState = false }) {
                                        for ((code, state) in Holidays.STATES) {
                                            DropdownMenuItem(
                                                text = { Text(state) },
                                                onClick = {
                                                    val (key, value) = Holidays.entry(first, code, today)
                                                    if (maps.none { it.first == key }) timeMaps = timeMaps.trimEnd() + "\n$key = $value"
                                                    choosingState = false
                                                },
                                                modifier = Modifier.testTag("plan-holidays-$code"),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "The times in which tasks are planned. The first map is for tasks whose contexts name none; without a map: Mon-Fri 09:00-17:00. " +
                        "A map whose definition starts with the name of another one is restricted to its times: General 08:00-11:00. " +
                        "A map with dates replaces the map of its name on these dates, a later one an earlier one; none leaves no time on them. " +
                        "Add holidays adds those of a state up to the end of next year.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                editing?.let { key ->
                    TimeMapEditor(
                        key.ifEmpty { null },
                        maps,
                        onSave = { newKey, definition ->
                            val line = "$newKey = $definition"
                            // Only the line of the map changes; the others stay as they were typed.
                            timeMaps = if (key.isEmpty()) {
                                (timeMaps.trimEnd() + "\n" + line).trimStart()
                            } else {
                                timeMaps.lines().joinToString("\n") { if (ConfigFile.parseLines(it).firstOrNull()?.first == key) line else it }
                            }
                            editing = null
                        },
                        onDelete = {
                            timeMaps = timeMaps.lines().filterNot { ConfigFile.parseLines(it).firstOrNull()?.first == key }.joinToString("\n")
                            editing = null
                        },
                        onDismiss = { editing = null },
                    )
                }
                field(
                    "Time maps of contexts", contextMaps, { contextMaps = it },
                    "One context per line: @research = Morning-Focus. It holds for the contexts below it.",
                    "plan-context-maps",
                )
                field(
                    "Barriers", barriers, { barriers = it },
                    "The day behind which a task is not planned: week, month, quarter, year or a date. " +
                        "One per line: default = quarter, +Thesis = 2027-03-31, @research = year. The earliest barrier of a task counts. Without one: quarter.",
                    "plan-barriers",
                )
                // [impl->dsn~plan-limits~1]
                field(
                    "Limits", limits, { limits = it },
                    "How much is planned for a project or a context, rules separated by commas: @chores = max 5h/week, block 30m. " +
                        "max with /day or /week: not more is planned. min with /week: this much is planned first in each week: @paper = min 1h/week. " +
                        "block: a task is not cut into shorter parts; an hour unless stated, also as default = block 2h.",
                    "plan-limits",
                )
                // [impl->dsn~busy-times~1]
                addresses?.let {
                    field(
                        "Calendars with busy times", it, { text -> addresses = text },
                        "Addresses of iCalendar files, one per line, as Outlook and Google Calendar publish them for a calendar. " +
                            "Nothing is planned into the times of their events. Kept on this device only, as such an address is a secret.",
                        "plan-calendars",
                    )
                }
                // [impl->dsn~sideline~1]
                field(
                    "Sideline", sideline, { sideline = it },
                    "Events of the calendars whose title contains one of these words, one per line, stay in the calendar, " +
                        "but tasks are planned into their time. A repeating event counts with all its occurrences.",
                    "plan-sideline",
                )
                result?.let { shown ->
                    Text(shown.summary, modifier = Modifier.testTag("plan-summary"))
                    for (problem in shown.problems) {
                        Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("plan-problem"))
                    }
                    if (shown.unplaced.isNotEmpty()) {
                        Text("Do not fit before their barrier", style = MaterialTheme.typography.titleSmall)
                        for (task in shown.unplaced) {
                            Text(task, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("plan-unplaced"))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("plan-close")) { Text("Close") }
                    Button(
                        onClick = {
                            running = true
                            saved = state
                            scope.launch {
                                try {
                                    result = onPlan(PlanSettings(timeMaps, contextMaps, barriers, limits, sideline), addresses)
                                } finally {
                                    running = false
                                }
                            }
                        },
                        enabled = !running,
                        modifier = Modifier.testTag("plan-update"),
                    ) {
                        Text(if (running) "Planning" else "Save and plan")
                    }
                }
            }
        }
    }
}

/** A time map in the list of the plan dialog: a click on it, or on *Edit* that hovering shows, opens the editor. */
@Composable
private fun MapRow(key: String, value: String, onEdit: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().hoverable(interactions).clickable(onClick = onEdit).padding(start = 16.dp, end = 8.dp).testTag("plan-map-$key"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(key, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
        // Without a pointer that hovers, as on a phone, the row itself opens the editor.
        TextButton(onClick = onEdit, modifier = Modifier.alpha(if (hovered) 1f else 0f).testTag("plan-edit-map-$key")) { Text("Edit") }
    }
}
