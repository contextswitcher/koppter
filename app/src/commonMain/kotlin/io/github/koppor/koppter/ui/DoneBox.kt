package io.github.koppor.koppter.ui

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Task
import kotlinx.coroutines.launch

/** What a tap on the mark of a TODO of LaTeX says. */
internal const val LATEX_DONE_HINT = "Can't be ticked off here: done when the TODO is removed from the LaTeX document"

/**
 * The box to tick the task off, or, for an open TODO of LaTeX, which cannot be ticked off, the mark of a document in its place.
 * A tap on the mark, and on the desktop the pointer resting on it, says how the TODO is done: a phone has no hover.
 */
// [impl->dsn~latex-todos~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DoneBox(task: Task, onToggle: () -> Unit, tag: String = "task-done") {
    if (task.completable) {
        Checkbox(checked = task.completed, onCheckedChange = { onToggle() }, modifier = Modifier.testTag(tag))
        return
    }
    // It stays until the next tap elsewhere: on a phone, a hint that vanishes by itself is gone while it is read.
    val tooltip = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(LATEX_DONE_HINT) } },
        state = tooltip,
    ) {
        IconButton(onClick = { scope.launch { tooltip.show() } }, modifier = Modifier.testTag("$tag-latex")) {
            Icon(DocumentIcon, contentDescription = LATEX_DONE_HINT, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A page with a folded corner and lines of text, as the icon *Description* of Material draws it (Apache License 2.0):
 * the core set of icons that the application uses has no document. The fill is replaced by the tint of [Icon].
 */
private val DocumentIcon: ImageVector = ImageVector.Builder("Document", 24.dp, 24.dp, 24f, 24f).addPath(
    addPathNodes("M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zM16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z"),
    fill = SolidColor(Color.Black),
).build()
