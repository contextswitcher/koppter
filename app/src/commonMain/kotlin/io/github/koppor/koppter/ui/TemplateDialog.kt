package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.Template
import io.github.koppor.koppter.todotxt.dueChoices
import kotlinx.datetime.LocalDate

private val TEMPLATE_EXAMPLE = """
    ## Swimming

    - Pack the bag +swim
      - [ ] towel
      - [ ] goggles
    - Buy a ticket +swim
""".trimIndent()

/**
 * What a new `templates.txt` starts with: a title and the rules, which stand outside of the templates and are therefore ignored,
 * and two templates; Markdown that markdownlint passes with its default rules.
 */
internal val NEW_TEMPLATES = """
    # Templates

    Each section "## name" is a template.
    Its list items are the tasks, in the notation of todo.txt.
    The lines indented below a task are its note, such as a checklist.
    The due date and time are chosen when the tasks are added.

""".trimIndent() + "\n" + TEMPLATE_EXAMPLE + "\n\n---\n\n## Business trip\n\n- Book the train +travel\n- Submit the travel request +travel\n"

/** Chooses a template of `templates.txt` and the due date and time of its tasks. */
// [impl->dsn~templates~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateDialog(
    templates: List<Template>,
    today: LocalDate,
    onAdd: (Template, LocalDate?, String?) -> Unit,
    onDismiss: () -> Unit,
    /** Creates `templates.txt` with an example and opens it in another program; `null` where the platform cannot. */
    onCreate: (() -> Unit)? = null,
) {
    // A single template needs no choice.
    var chosen by remember { mutableStateOf(templates.singleOrNull()) }
    var due by remember { mutableStateOf("") }
    var by by remember { mutableStateOf("") }
    val dueDate = Task.parseDate(due.trim())
    val dueValid = due.isBlank() || dueDate != null
    val byValid = by.isBlank() || Task.parseTime(by.trim()) != null
    val valid = chosen != null && dueValid && byValid
    // Only a choice is lost, which takes a tap to make again.
    GuardedDialog(changed = false, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().testTag("template-dialog")) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("From a template", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("template-cancel")) { Text("Cancel") }
                    Button(
                        onClick = { chosen?.let { onAdd(it, dueDate, by.trim().ifEmpty { null }) } },
                        enabled = valid,
                        modifier = Modifier.testTag("template-add"),
                    ) { Text(chosen?.tasks?.size?.let { if (it == 1) "Add 1 task" else "Add $it tasks" } ?: "Add") }
                }
                val scroll = rememberScrollState()
                // Marks the edge of the heading where the fields pass below it; always laid out, so that the fields do not jump when it appears.
                HorizontalDivider(Modifier.alpha(if (scroll.canScrollBackward) 1f else 0f))
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (templates.isEmpty()) {
                        Text("There are no templates yet. They are kept in templates.txt next to todo.txt, for example:", modifier = Modifier.testTag("template-empty"))
                        Text(TEMPLATE_EXAMPLE, fontFamily = FontFamily.Monospace)
                        Text("A section ## starts a template, each list item below it is a task, and the lines indented below a task are its note.")
                        // [impl->dsn~templates-file~1]
                        // It opens another program, so the dialog closes: the templates are read again when it is opened next.
                        onCreate?.let { create ->
                            Button(onClick = { create(); onDismiss() }, modifier = Modifier.testTag("template-create")) { Text("Create templates.txt and edit it") }
                        }
                    }
                    Column(Modifier.selectableGroup()) {
                        for ((i, template) in templates.withIndex()) {
                            Row(
                                Modifier.fillMaxWidth().selectable(chosen == template, role = Role.RadioButton) { chosen = template }
                                    .padding(vertical = 4.dp).testTag("template-$i"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = chosen == template, onClick = null)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(template.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        template.tasks.joinToString(" · ") { Task.parse(it.line).description },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                    if (templates.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Due, for all tasks (optional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically))
                            for ((title, date) in dueChoices(today)) {
                                FilterChip(
                                    colors = selectedChipColors(),
                                    selected = due.trim() == date.toString(),
                                    onClick = { due = if (due.trim() == date.toString()) "" else date.toString() },
                                    label = { Text(title) },
                                    modifier = Modifier.testTag("template-due-$date"),
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val field = Modifier.width(165.dp)
                            DateField("Due date", due, { due = it }, dueValid, field.testTag("template-due"))
                            TimeField("Due time", "14:00", by, { by = it }, byValid, field.testTag("template-by"))
                        }
                    }
                }
            }
        }
    }
}
