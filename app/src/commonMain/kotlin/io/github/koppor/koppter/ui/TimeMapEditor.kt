package io.github.koppor.koppter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.Paint
import io.github.koppor.koppter.plan.TimeMaps
import kotlin.math.abs

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** The height of a cell of a quarter of an hour: a day fits on the screen of a laptop. */
private val CELL_HEIGHT = 5.dp

/** The colors of the paints, the same as those of the state of the plan on a task. */
@Composable
private fun color(paint: Paint): Color = when (paint) {
    Paint.GREEN -> LocalFineColor.current
    Paint.YELLOW -> MaterialTheme.colorScheme.secondary
    Paint.RED -> MaterialTheme.colorScheme.error
    Paint.BLACK -> MaterialTheme.colorScheme.onSurface
}

/**
 * Edits the name, the dates and the definition of the entry [key] of the time maps [entries], or of a new one for `null`.
 * The definition is typed or painted on the days of a week: the chosen color, or none with the eraser, goes to each quarter of an hour that is touched.
 * Painting writes the definition; the name of the map whose times restrict it stays in front.
 */
// [impl->dsn~time-map-editor~1]
@Composable
fun TimeMapEditor(key: String?, entries: List<Pair<String, String>>, onSave: (key: String, definition: String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val others = entries.filter { it.first != key }
    val initialText = entries.firstOrNull { it.first == key }?.second ?: ""
    val initialName = key?.substringBefore(' ') ?: ""
    val initialDates = key?.substringAfter(' ', "") ?: ""
    var name by remember { mutableStateOf(initialName) }
    var dates by remember { mutableStateOf(initialDates) }
    var text by remember { mutableStateOf(initialText) }
    val newKey = listOf(name.trim(), dates.trim()).filter(String::isNotEmpty).joinToString(" ")
    val maps = TimeMaps(others + (newKey to text))
    val initial = remember { TimeMaps(entries).cells(key ?: "") }
    val cells = remember { mutableStateListOf(*initial.toTypedArray()) }
    var brush by remember { mutableStateOf<Paint?>(Paint.GREEN) }
    val parent = maps.parentOf(newKey)
    // The grid keeps the handler of its first composition.
    val currentParent by rememberUpdatedState(parent)
    // A definition without rules holds all the time: an empty one is no definition, except on dates, where it is none.
    val definition = text.trim().ifEmpty { "none".takeIf { dates.isNotBlank() } }
    val problems = maps.problems - TimeMaps(others).problems.toSet()
    val error = when {
        name.isBlank() || name.trim().contains(' ') -> "A map needs a name without spaces."
        others.any { it.first == newKey } -> "$newKey exists already."
        definition == null -> "A map needs times."
        else -> null
    }
    // A problem does not prevent saving: the map that a definition names may be added afterwards; planning reports it.
    val warning = error ?: problems.firstOrNull()
    val paints = Paint.entries.associateWith { color(it) }
    val empty = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.outlineVariant

    fun paint(index: Int, paint: Paint?) {
        cells[index] = paint
        text = TimeMaps.definition(currentParent, cells) ?: ""
    }

    GuardedDialog(changed = listOf(name, dates, text) != listOf(initialName, initialDates, initialText), onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 720.dp).testTag("time-map-editor")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (key == null) "New time map" else "Time map $key", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f).testTag("time-map-name"))
                    OutlinedTextField(
                        dates, { dates = it },
                        label = { Text("Only on dates (optional)") },
                        placeholder = { Text("2026-12-24..2027-01-06 2027-04-02") },
                        singleLine = true,
                        modifier = Modifier.weight(2f).testTag("time-map-dates"),
                    )
                }
                Text(
                    "Green: tasks are planned here. Yellow and then red: only for a task that would otherwise be done after its due date or not before its barrier. " +
                        "Black: only for the tasks of this map; the other maps leave it free." +
                        (parent?.let { " Only the times that $it has count." } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (paint in Paint.entries + null) {
                        FilterChip(
                            selected = brush == paint,
                            onClick = { brush = paint },
                            label = { Text(paint?.word?.replaceFirstChar(Char::uppercase) ?: "Erase") },
                            leadingIcon = { Box(Modifier.size(12.dp).background(paint?.let { paints.getValue(it) } ?: empty, CircleShape)) },
                            modifier = Modifier.testTag("time-map-paint-${paint?.word ?: "erase"}"),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(44.dp))
                    for (day in DAY_NAMES) Text(day, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.width(44.dp)) {
                        for (hour in 0 until 24) {
                            Text("${hour.toString().padStart(2, '0')}:00", Modifier.height(CELL_HEIGHT * 4), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Canvas(
                        Modifier.weight(1f).height(CELL_HEIGHT * TimeMaps.CELLS_PER_DAY).testTag("time-map-grid").pointerInput(Unit) {
                            fun cellOf(position: Offset) = Pair(
                                (position.x / size.width * 7).toInt().coerceIn(0, 6),
                                (position.y / size.height * TimeMaps.CELLS_PER_DAY).toInt().coerceIn(0, TimeMaps.CELLS_PER_DAY - 1),
                            )

                            // A fast drag reports positions several cells apart: the cells between them are painted as well.
                            fun paint(from: Pair<Int, Int>, to: Pair<Int, Int>) {
                                val steps = maxOf(abs(to.first - from.first), abs(to.second - from.second), 1)
                                for (step in 0..steps) {
                                    val day = from.first + (to.first - from.first) * step / steps
                                    val cell = from.second + (to.second - from.second) * step / steps
                                    paint(day * TimeMaps.CELLS_PER_DAY + cell, brush)
                                }
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var last = cellOf(down.position)
                                paint(last, last)
                                down.consume()
                                // The drag paints and does not scroll what lies around the grid.
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val next = cellOf(change.position)
                                    paint(last, next)
                                    last = next
                                    change.consume()
                                }
                            }
                        },
                    ) {
                        val width = size.width / 7
                        val height = size.height / TimeMaps.CELLS_PER_DAY
                        cells.forEachIndexed { index, paint ->
                            val day = index / TimeMaps.CELLS_PER_DAY
                            val cell = index % TimeMaps.CELLS_PER_DAY
                            drawRect(paint?.let { paints.getValue(it) } ?: empty, Offset(day * width, cell * height), Size(width, height))
                        }
                        for (hour in 0..24) drawLine(line, Offset(0f, hour * 4 * height), Offset(size.width, hour * 4 * height))
                        for (day in 0..7) drawLine(line, Offset(day * width, 0f), Offset(day * width, size.height))
                    }
                }
                OutlinedTextField(
                    text,
                    { typed ->
                        text = typed
                        TimeMaps(others + (newKey to typed)).cells(newKey).forEachIndexed { index, paint -> cells[index] = paint }
                    },
                    label = { Text("Definition") },
                    supportingText = { Text(warning ?: "Mon-Fri 08:00-12:00 13:00-17:00, Mon-Fri 07:00-08:00 yellow") },
                    isError = warning != null,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().testTag("time-map-definition"),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (key != null) TextButton(onClick = onDelete, modifier = Modifier.testTag("time-map-delete")) { Text("Delete") }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            text = initialText
                            initial.forEachIndexed { index, paint -> cells[index] = paint }
                        },
                        enabled = text != initialText,
                        modifier = Modifier.testTag("time-map-revert"),
                    ) { Text("Revert") }
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("time-map-cancel")) { Text("Cancel") }
                    Button(onClick = { onSave(newKey, definition!!) }, enabled = error == null, modifier = Modifier.testTag("time-map-save")) { Text("Save") }
                }
            }
        }
    }
}
