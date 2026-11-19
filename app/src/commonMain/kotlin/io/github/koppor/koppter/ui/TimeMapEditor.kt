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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
 * Paints a time map on the days of a week: the chosen color, or none with the eraser, goes to each quarter of an hour that is touched.
 * Saving gives the definition of the map; the name of the map whose times restrict it stays in front.
 */
// [impl->dsn~time-map-editor~1]
@Composable
fun TimeMapEditor(name: String, maps: TimeMaps, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val initial = remember { maps.cells(name) }
    val cells = remember { mutableStateListOf(*initial.toTypedArray()) }
    var brush by remember { mutableStateOf<Paint?>(Paint.GREEN) }
    val parent = maps.parentOf(name)
    val definition = TimeMaps.definition(parent, cells)
    val paints = Paint.entries.associateWith { color(it) }
    val empty = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.outlineVariant

    GuardedDialog(changed = cells.toList() != initial, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 720.dp).testTag("time-map-editor")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Time map $name", style = MaterialTheme.typography.titleLarge)
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
                                    cells[day * TimeMaps.CELLS_PER_DAY + cell] = brush
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(definition ?: "A map needs times.", Modifier.weight(1f).testTag("time-map-definition"), style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = { initial.forEachIndexed { index, paint -> cells[index] = paint } },
                        enabled = cells.toList() != initial,
                        modifier = Modifier.testTag("time-map-revert"),
                    ) { Text("Revert") }
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("time-map-cancel")) { Text("Cancel") }
                    Button(onClick = { onSave(definition!!) }, enabled = definition != null, modifier = Modifier.testTag("time-map-save")) { Text("Save") }
                }
            }
        }
    }
}
