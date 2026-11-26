package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.heightIn
import io.github.koppor.koppter.todotxt.toggleItem
import io.github.koppor.koppter.todotxt.checklist
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.background
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.Recurrence
import io.github.koppor.koppter.todotxt.imageName
import io.github.koppor.koppter.todotxt.noteImages
import io.github.koppor.koppter.todotxt.oneNoteLinks
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.waitingFor
import io.github.koppor.koppter.todotxt.waitsFor
import io.github.koppor.koppter.todotxt.withDependencies
import io.github.koppor.koppter.todotxt.deadlineChoices
import io.github.koppor.koppter.todotxt.dueChoices
import com.kez.picker.Picker
import com.kez.picker.PickerDefaults
import com.kez.picker.duration.DurationPicker
import com.kez.picker.duration.rememberDurationPickerState
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Duration.Companion.minutes

/**
 * Edits one task.
 *
 * The first field holds the description only; the metadata has its own fields below it.
 * Below the fields, the dialog shows the todo.txt line that *Save* writes.
 */
/** What *Cancel* keeps of a new task: the task without its dependencies, the note and the images added to it. */
data class Draft(val task: Task, val note: String, val images: Map<String, ByteArray>)

// [impl->dsn~edit-dialog~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDialog(
    original: Task,
    isNew: Boolean,
    dates: TaskDates?,
    /** The text of the note of the task; `null` if the storage keeps no notes. */
    originalNote: String?,
    /** The file name for the note if the task has none yet. */
    newNoteName: String,
    knownProjects: List<String>,
    knownContexts: List<String>,
    knownHashtags: List<String>,
    today: LocalDate,
    /** How many open tasks carry a name, keyed with its sign; the most used names are offered first. */
    nameUses: Map<String, Int> = emptyMap(),
    /** The contexts that a task takes with a project. */
    contextsOf: (String) -> List<String> = { emptyList() },
    /** The other tasks of the file, which the task can wait for or be waited for by. */
    others: List<Entry> = emptyList(),
    /** Called with the task, its note, the images that were added to the note, each under its file name, and the other tasks that changed with the dependencies. */
    onSave: (Task, note: String?, images: Map<String, ByteArray>, changed: List<Entry>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    /** The width of the dialog in dp where the window allows it, and what keeps a width that the user chose. */
    width: Int = 760,
    onWidth: (Int) -> Unit = {},
    /** Lets the user choose images and gives their bytes, none if none was chosen; `null` on a platform without that. */
    pickImage: (suspend () -> List<ByteArray>)? = null,
    /** As [pickImage], with the camera. */
    takePhoto: (suspend () -> List<ByteArray>)? = null,
    /** The image of a note that the storage has, by its file name. */
    imageOf: (String) -> ByteArray? = { null },
    /** What the dialog of a new task starts with instead of [original]. */
    draft: Draft? = null,
    /** Called when the dialog of a new task closes without saving: with what *Cancel* keeps, with `null` when it is discarded. */
    onDraft: (Draft?) -> Unit = {},
) {
    // The task as the line was last typed; the fields start anew from it.
    // [impl->dsn~edit-line~1]
    var base by remember { mutableStateOf(draft?.task ?: original) }
    /** The line while it is typed: what was typed stays, where the fields would write it in another form. */
    var line by remember { mutableStateOf<String?>(null) }
    // With the cursor at the end, where the user goes on typing.
    var description by remember(base) { mutableStateOf(TextFieldValue(base.description, TextRange(base.description.length))) }
    var priority by remember(base) { mutableStateOf(base.priority) }
    var due by remember(base) { mutableStateOf(base.tag(Task.DUE).orEmpty()) }
    var deadline by remember(base) { mutableStateOf(base.tag(Task.DEADLINE).orEmpty()) }
    var threshold by remember(base) { mutableStateOf(base.tag(Task.THRESHOLD).orEmpty()) }
    var recurrence by remember(base) { mutableStateOf(base.tag(Task.RECURRENCE).orEmpty()) }
    var duration by remember(base) { mutableStateOf(base.tag(Task.DURATION).orEmpty()) }
    var at by remember(base) { mutableStateOf(base.tag(Task.AT).orEmpty()) }
    var travel by remember(base) { mutableStateOf(base.tag(Task.TRAVEL).orEmpty()) }
    var spent by remember(base) { mutableStateOf(base.tag(Task.SPENT).orEmpty()) }
    var by by remember(base) { mutableStateOf(base.tag(Task.BY).orEmpty()) }
    var contextSwitcher by remember(base) { mutableStateOf(base.tag(Task.CONTEXT_SWITCHER).orEmpty()) }
    var projects by remember(base) { mutableStateOf(base.projects) }
    var contexts by remember(base) { mutableStateOf(base.contexts) }
    var hashtags by remember(base) { mutableStateOf(base.hashtags) }
    var waits by remember(base) { mutableStateOf(waitsFor(base, others).map { it.index }.toSet()) }
    var before by remember(base) { mutableStateOf(waitingFor(base, others).map { it.index }.toSet()) }
    var note by remember { mutableStateOf(TextFieldValue(draft?.note ?: originalNote.orEmpty())) }
    /** The images that were added in this dialog, which are written when the task is saved. */
    var images by remember { mutableStateOf(draft?.images ?: mapOf()) }
    var imageMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val noteFocus = remember { FocusRequester() }

    // [impl->dsn~note-images~1]
    fun attach(source: suspend () -> List<ByteArray>) {
        scope.launch {
            imageMessage = try {
                for (bytes in source()) {
                    val name = imageName(base.tag(Task.NOTE) ?: newNoteName) { it in images || it in noteImages(note.text) || imageOf(it) != null }
                    images = images + (name to bytes)
                    // In a paragraph of its own, which is how Markdown shows an image on its own line.
                    val text = note.text.trimEnd().let { if (it.isEmpty()) "" else "$it\n\n" } + "![]($name)\n"
                    note = TextFieldValue(text, TextRange(text.length))
                }
                // Unfocused, the field shows no cursor, and a tap on Android places it where the finger lands, often inside the Markdown of the image.
                if (note.text.isNotEmpty()) noteFocus.requestFocus()
                ""
            } catch (e: Exception) {
                "Not attached: ${e.message ?: e}"
            }
        }
    }

    val dueValid = due.isBlank() || Task.parseDate(due.trim()) != null
    val deadlineValid = deadline.isBlank() || Task.parseDeadline(deadline.trim()) != null
    val dueDate = Task.parseDate(due.trim())
    // [impl->dsn~start-before-due~1]
    val startDate = Task.parseDate(threshold.trim()) ?: dueDate?.let { Task.startBefore(threshold.trim(), it) }
    val thresholdValid = threshold.isBlank() || startDate != null
    val recurrenceValid = recurrence.isBlank() || Recurrence.parse(recurrence.trim()) != null
    val durationValid = duration.isBlank() || Task.parseDuration(duration.trim()) != null
    val atValid = at.isBlank() || Task.parseTime(at.trim()) != null
    val travelValid = travel.isBlank() || Task.parseDuration(travel.trim()) != null
    val spentValid = spent.isBlank() || Task.parseDuration(spent.trim()) != null
    val byValid = by.isBlank() || Task.parseTime(by.trim()) != null
    val fields = base
        .edit(description = description.text, priority = priority, projects = projects, contexts = contexts, hashtags = hashtags)
        .withTag(Task.DUE, due.trim())
        .withTag(Task.BY, by.trim())
        .withTag(Task.DEADLINE, deadline.trim())
        .withTag(Task.THRESHOLD, startDate?.toString() ?: threshold.trim())
        .withTag(Task.RECURRENCE, recurrence.trim())
        .withTag(Task.DURATION, duration.trim())
        .withTag(Task.AT, at.trim())
        .withTag(Task.TRAVEL, travel.trim())
        .withTag(Task.SPENT, spent.trim())
        // A link pasted into the description counts only while the field is empty.
        .withTag(Task.CONTEXT_SWITCHER, Task.contextSwitcherId(contextSwitcher))
        .let { if (contextSwitcher.isBlank()) it.withContextSwitcherTags() else it }
        // [impl->dsn~onenote-tag~1]
        .withoutOneNoteWebLinks()
        // [impl->dsn~notes~1]
        .let { if (originalNote == null) it else it.withTag(Task.NOTE, if (note.text.isBlank()) null else base.tag(Task.NOTE) ?: newNoteName) }
    // [impl->dsn~edit-dates~1]
    // Words of a description that was not touched stay words: they were there when the task was written.
    val dated = if (description.text == base.description) fields else fields.withDates(today)
    /** Where the words that became dates start in the description; `null` if there are none. */
    val phraseStart = dated.description.length.takeIf { dated != fields && description.text.startsWith(dated.description) }
    // [impl->dsn~edit-dependencies~1]
    val (result, changed) = withDependencies(dated, others, waits, before)
    val valid = dueValid && deadlineValid && thresholdValid && recurrenceValid && durationValid && atValid && byValid && travelValid && spentValid && result.body.isNotBlank() &&
        // What a new task starts with is no task yet.
        (!isNew || result != original)

    val descriptionFocus = remember { FocusRequester() }
    val keyboard = LocalKeyboard.current
    // Without a keyboard, the focus brings up the keyboard of the screen, which hides half of an existing task; a new task is typed first.
    LaunchedEffect(Unit) { if (keyboard || isNew) descriptionFocus.requestFocus() }

    val state = Triple(result.toLine(), note.text, changed)
    val initial = remember { state }

    // [impl->dsn~edit-resize~1]
    var shownWidth by remember { mutableStateOf(width.dp) }
    val density = LocalDensity.current
    // [impl->dsn~new-task-draft~1]
    // A draft counts as a change, so that the question is the way to discard it.
    GuardedDialog(changed = state != initial || draft != null, onDismiss = { if (isNew) onDraft(null); onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            // By default wide enough for the priorities and the days of the due date in one row each.
            modifier = Modifier.widthIn(max = shownWidth).testTag("edit-dialog")
                // [impl->dsn~keyboard-shortcuts~1]
                .onPreviewKeyEvent {
                    val save = it.type == KeyEventType.KeyDown && it.key == Key.Enter && (it.isCtrlPressed || it.isMetaPressed)
                    if (save && valid) onSave(result, if (originalNote == null) null else note.text, images, changed)
                    save
                },
        ) {
            Box {
                Column {
                    // [impl->dsn~edit-header~1]
                    // The buttons stand in the heading, so that they stay in view while the fields scroll and no keyboard of a phone covers them.
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (draft != null) "New task (draft)" else if (isNew) "New task" else "Edit task",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).testTag("edit-heading"),
                        )
                        TextButton(
                            onClick = {
                                // [impl->dsn~new-task-draft~1]
                                if (isNew) onDraft(Draft(dated, note.text, images).takeIf { dated != original || note.text.isNotBlank() })
                                onDismiss()
                            },
                            modifier = Modifier.testTag("edit-cancel"),
                        ) { Text("Cancel") }
                        Button(onClick = { onSave(result, if (originalNote == null) null else note.text, images, changed) }, enabled = valid, modifier = Modifier.testTag("edit-save")) { Text("Save") }
                    }
                    val scroll = rememberScrollState()
                    // Where the fields pass below the heading, a line marks its edge.
                    if (scroll.canScrollBackward) HorizontalDivider()
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.copy(text = it.text.replace('\n', ' ')) },
                            label = { Text("Description") },
                            // Marked, as the words leave the description when the task is saved.
                            visualTransformation = phraseColor(phraseStart, MaterialTheme.colorScheme.tertiaryContainer),
                            supportingText = phraseStart?.let { { Text("The marked words become the dates of the task.", Modifier.testTag("edit-dates-hint")) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocus).testTag("edit-description"),
                        )

                        // [impl->dsn~note-checklist~1]
                        // Above the other fields: ticking off an item is what a task with a checklist is opened for.
                        val items = checklist(note.text)
                        if (items.isNotEmpty()) {
                            Column(Modifier.testTag("edit-checklist")) {
                                Text("Checklist (${items.count { it.checked }}/${items.size})", style = MaterialTheme.typography.labelLarge)
                                for (item in items) {
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(item.checked, role = Role.Checkbox) { note = note.copy(text = toggleItem(note.text, item.line)) }
                                            .testTag("edit-checklist-${item.line}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(item.checked, onCheckedChange = null)
                                        Text(
                                            item.text,
                                            Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                                        )
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val names = LocalPriorityNames.current
                            val choices = (listOf<Char?>(null) + names.choices).let { if (priority in it) it else it + priority }
                            for (choice in choices) {
                                FilterChip(
                                    colors = selectedChipColors(),
                                    selected = priority == choice,
                                    onClick = { priority = choice },
                                    label = { Text(choice?.let(names::name) ?: "No priority") },
                                    modifier = Modifier.testTag("edit-priority-${choice ?: "none"}"),
                                )
                            }
                        }

                        // [impl->dsn~due-chips~1]
                        // The days of the menu of a task card, which a device without a mouse does not open for a task without due date.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Due", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically))
                            for ((title, date) in dueChoices(today)) {
                                FilterChip(
                                    colors = selectedChipColors(),
                                    selected = due.trim() == date.toString(),
                                    onClick = { due = if (due.trim() == date.toString()) "" else date.toString() },
                                    label = { Text(title) },
                                    modifier = Modifier.testTag("edit-due-$date"),
                                )
                            }
                        }
                        // A flow row: three fields side by side on the desktop, wrapped on a phone.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val field = Modifier.width(165.dp)
                            WithHelp("The day by which the task is to be done (due:).") { DateField("Due date", due, { due = it }, dueValid, field.testTag("edit-due")) }
                            // [impl->dsn~due-time~1]
                            WithHelp("The time of the due date by which the task is to be done (dueby:).") {
                                TimeField("Due time", "14:00", by, { by = it }, byValid, field.testTag("edit-by"))
                            }
                            // [impl->dsn~deadline~1]
                            WithHelp("The day that others set for the task, after which it is late (deadline:).") {
                                DateField(
                                    "Deadline", deadline, { deadline = it }, deadlineValid, field.testTag("edit-deadline"),
                                    // The deadline is often the day that the task is due on.
                                    shortcuts = listOfNotNull(Shortcut("Use due date", due.trim()).takeIf { dueDate != null }),
                                    due = dueDate,
                                    menu = deadlineChoices(today).map { (title, date) -> title to date.toString() },
                                )
                            }
                            // Called the threshold date by Simpletask and the start date by topydo; the tag is the same.
                            WithHelp("The task is hidden and not planned before this day (t:): a date, or a distance before the due date such as 7d or 1w.") {
                                // [impl->dsn~start-before-due~1]
                                val before = if (dueDate == null) emptyList() else startChoices
                                DateField(
                                    "Start date", threshold, { threshold = it }, thresholdValid, field.testTag("edit-threshold"),
                                    shortcuts = before,
                                    due = dueDate,
                                    menu = before.map { it.title to it.value },
                                    placeholder = "YYYY-MM-DD, 7d",
                                    note = startNote(threshold.trim(), startDate, dueDate),
                                    shown = startDate,
                                )
                            }
                            WithHelp("Completing the task adds its next occurrence, such as 1w or +1m (rec:).") {
                                RecurrenceField(recurrence, { recurrence = it }, recurrenceValid, field.testTag("edit-recurrence"))
                            }
                            // [impl->dsn~fixed-time~1]
                            WithHelp("The time at which the task takes place (at:).") {
                                TimeField("Fixed time", "12:00", at, { at = it }, atValid, field.testTag("edit-at"))
                            }
                            // [impl->dsn~duration~1]
                            WithHelp("How long the task takes, such as 30m or 2h (dur:).") {
                                DurationField("Duration", "45m, 1h30m", duration, { duration = it }, durationValid, field.testTag("edit-duration"))
                            }
                            // [impl->dsn~travel-time~1]
                            WithHelp("The way to the task, kept free before and after it (tt:).") {
                                DurationField("Travel time", "30m", travel, { travel = it }, travelValid, field.testTag("edit-travel"))
                            }
                            // [impl->dsn~time-spent~1]
                            WithHelp("The time that was worked on the task already (spent:).") {
                                DurationField("Time spent", "1h30m", spent, { spent = it }, spentValid, field.testTag("edit-spent"))
                            }
                        }

                        // [impl->dsn~context-switcher-tag~1]
                        OutlinedTextField(
                            value = contextSwitcher,
                            onValueChange = { contextSwitcher = it },
                            label = { Text("ContextSwitcher task") },
                            placeholder = { Text("category/task or a copied link") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit-cs"),
                        )

                        // [impl->dsn~project-contexts~1]
                        // Only when the project is added, so that a context that the user removes stays removed.
                        NameList("Project", "+", projects, knownProjects, { chosen ->
                            contexts = (contexts + (chosen - projects).flatMap(contextsOf)).distinct()
                            projects = chosen
                        }, "edit-project", projectColors(), nameUses)
                        NameList("Context", "@", contexts, knownContexts, { contexts = it }, "edit-context", contextColors(), nameUses)
                        // [impl->dsn~hashtags-in-ui~1]
                        // "#123" in a line is a word of the description, not a hashtag.
                        NameList("Hashtag", "#", hashtags, knownHashtags, { hashtags = it }, "edit-hashtag", hashtagColors(), nameUses, outlined = true, valid = Task::isHashtagName)
                        // [impl->dsn~edit-dependencies~1]
                        WithHelp("The tasks that are to be done before this one (id: here, p: there).") { TaskChoice("Waits for", waits, others, { waits = it }, "edit-waits") }
                        WithHelp("The tasks that are to be done after this one (p: here, id: there).") { TaskChoice("Before", before, others, { before = it }, "edit-before") }

                        if (originalNote != null) {
                            OutlinedTextField(
                                value = note,
                                // [impl->dsn~onenote-links~1]
                                onValueChange = { typed ->
                                    // Only what stands before the cursor, where a pasted text ends, so that the cursor stays behind it.
                                    val cursor = typed.selection.end
                                    val before = oneNoteLinks(typed.text.substring(0, cursor))
                                    note = if (before.length == cursor) typed else TextFieldValue(before + typed.text.substring(cursor), TextRange(before.length))
                                },
                                label = { Text("Note (Markdown)") },
                                minLines = 2,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth().focusRequester(noteFocus).testTag("edit-note"),
                            )
                            // [impl->dsn~note-images~1]
                            if (pickImage != null || takePhoto != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pickImage?.let { TextButton(onClick = { attach(it) }, modifier = Modifier.testTag("edit-add-image")) { Text("Add image") } }
                                    takePhoto?.let { TextButton(onClick = { attach(it) }, modifier = Modifier.testTag("edit-take-photo")) { Text("Take photo") } }
                                }
                            }
                            if (imageMessage.isNotEmpty()) {
                                Text(imageMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("edit-image-message"))
                            }
                            val shown = noteImages(note.text)
                            if (shown.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (name in shown) {
                                        key(name) { NoteImage(name, images[name] ?: remember { imageOf(name) }) }
                                    }
                                }
                            }
                        }

                        Text("Line in todo.txt", style = MaterialTheme.typography.labelMedium)
                        // [impl->dsn~edit-line~1]
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                            BasicTextField(
                                value = line ?: result.toLine(),
                                onValueChange = { typed ->
                                    line = typed.replace('\n', ' ')
                                    base = Task.parse(line!!)
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.padding(8.dp).fillMaxWidth().onFocusChanged { if (!it.isFocused) line = null }.testTag("edit-line"),
                            )
                        }

                        // [impl->dsn~dates-in-dialog~1]
                        val history = listOfNotNull(dates?.added?.let { "Added $it" }, dates?.completed?.let { "Completed $it" })
                        if (history.isNotEmpty()) {
                            Text(
                                history.joinToString(" · ") + " (from the git history)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("edit-dates"),
                            )
                        }

                        // Rarely used and not to be undone by Cancel: at the end, away from Save.
                        if (!isNew) {
                            TextButton(onClick = onDelete, modifier = Modifier.testTag("edit-delete")) { Text("Delete") }
                        }
                    }
                }
                // [impl->dsn~edit-resize~1]
                // For a pointer: a finger has the whole width of a phone anyway.
                if (keyboard) {
                    Box(Modifier.matchParentSize()) {
                        Box(
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(14.dp).testTag("edit-resize")
                                .pointerHoverIcon(PointerIcon.Hand)
                                .draggable(
                                    // The dialog stays in the middle of the window, so its edge follows the pointer if the width changes by twice the way.
                                    rememberDraggableState { delta -> shownWidth = (shownWidth + with(density) { (2 * delta).toDp() }).coerceIn(420.dp, 2400.dp) },
                                    Orientation.Horizontal,
                                    onDragStopped = { onWidth(shownWidth.value.roundToInt()) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(4.dp, 48.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** How long the pointer rests on a field before its help is shown, in milliseconds. */
private const val HELP_DELAY = 500L

/** An image of the note, small; the name of a file that is missing or no image that the platform shows. */
// [impl->dsn~note-images~1]
// ponytail: decoded at its full size while the dialog is drawn, which fits the images that the application scales down when they are attached;
// a smaller copy made on another thread if notes get large images from elsewhere.
@Composable
private fun NoteImage(name: String, bytes: ByteArray?) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Image $name", modifier = Modifier.height(96.dp).testTag("edit-image-$name"))
    } else {
        Text("$name is not shown", style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("edit-image-missing-$name"))
    }
}

/**
 * Shows the help while the pointer rests on the field.
 * A touch screen has no pointer that rests, and a long press there belongs to the text of the field.
 */
// [impl->dsn~edit-help~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithHelp(help: String, field: @Composable () -> Unit) {
    // Not the mutex that all tooltips share: the tooltip of the button that opened the dialog holds it, as the pointer never left that button.
    val tooltip = rememberTooltipState(isPersistent = true, mutatorMutex = remember { MutatorMutex() })
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    LaunchedEffect(hovered) {
        if (hovered) {
            delay(HELP_DELAY)
            tooltip.show()
        } else {
            tooltip.dismiss()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(help) } },
        state = tooltip,
        focusable = false,
        enableUserInput = false,
    ) {
        Box(Modifier.hoverable(interactions)) { field() }
    }
}

/** Gives the text from [start] on a background; nothing for `null`. */
private fun phraseColor(start: Int?, color: Color) = VisualTransformation { text ->
    val marked = if (start == null || start >= text.length) text else AnnotatedString.Builder(text).apply { addStyle(SpanStyle(background = color), start, text.length) }.toAnnotatedString()
    TransformedText(marked, OffsetMapping.Identity)
}

internal const val MILLIS_PER_DAY = 86_400_000L

/** A clock, which the icons that come with Compose lack: the path of the Material icon "access time". */
private val Clock: ImageVector by lazy {
    materialIcon("Clock") {
        materialPath {
            moveTo(11.99f, 2f)
            curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
            reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
            reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
            reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
            close()
            moveTo(12.5f, 7f)
            horizontalLineTo(11f)
            verticalLineToRelative(6f)
            lineToRelative(5.25f, 3.15f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.5f, -2.67f)
            close()
        }
    }
}

/** A time of the day, which is typed or chosen on the clock face of Material. */
// [impl->dsn~edit-pickers~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(label: String, example: String, value: String, onChange: (String) -> Unit, valid: Boolean, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(example) },
        isError = !valid,
        singleLine = true,
        trailingIcon = { IconButton(onClick = { picking = true }) { Icon(Clock, contentDescription = "Choose $label") } },
        modifier = modifier,
    )
    if (picking) {
        // Without a time, the clock starts at noon: the middle of the day is the shortest way to any time.
        val minutes = Task.parseTime(value.trim()) ?: (12 * 60)
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { onChange(""); picking = false }) { Text("No time") } },
            title = { Text(label) },
        ) {
            TimePicker(state)
        }
    }
}

/** A duration, which is typed or chosen on two wheels for its hours and minutes. */
// [impl->dsn~edit-pickers~1]
@Composable
private fun DurationField(label: String, example: String, value: String, onChange: (String) -> Unit, valid: Boolean, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(example) },
        isError = !valid,
        singleLine = true,
        trailingIcon = { IconButton(onClick = { picking = true }) { Icon(Clock, contentDescription = "Choose $label") } },
        modifier = modifier,
    )
    if (picking) {
        // Steps of five minutes keep the wheel short; any other duration is typed.
        val items = remember { PickerDefaults.durationPickerItems(minuteItems = (0..55 step 5).toList()) }
        // The wheels take whole minutes only, and the nearest of their values for any other duration.
        val state = rememberDurationPickerState(items, Task.parseDuration(value.trim())?.inWholeMinutes?.minutes ?: 30.minutes)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = { onChange(Task.durationText(state.selectedDuration)); picking = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { onChange(""); picking = false }) { Text("No duration") } },
            title = { Text(label) },
            text = {
                DurationPicker(
                    state = state,
                    items = items,
                    format = PickerDefaults.durationPickerFormat(hourItemText = { "$it h" }, minuteItemText = { "$it min" }),
                    modifier = Modifier.testTag("duration-picker"),
                )
            },
        )
    }
}

private val UNIT_NAMES = mapOf(
    Recurrence.Unit.DAY to "day",
    Recurrence.Unit.BUSINESS_DAY to "business day",
    Recurrence.Unit.WEEK to "week",
    Recurrence.Unit.MONTH to "month",
    Recurrence.Unit.YEAR to "year",
)

/** The field of the recurrence, with a button that opens wheels for the amount and the unit and a choice of the day that the interval counts from. */
// [impl->dsn~edit-pickers~1]
@Composable
private fun RecurrenceField(value: String, onChange: (String) -> Unit, valid: Boolean, modifier: Modifier) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Recurrence") },
        placeholder = { Text("w, 2w, +1m") },
        isError = !valid,
        singleLine = true,
        trailingIcon = { IconButton(onClick = { picking = true }) { Icon(Icons.Default.Refresh, contentDescription = "Choose Recurrence") } },
        modifier = modifier,
    )
    if (picking) {
        // Up to 30 keeps the wheel short; any other amount is typed.
        val amounts = remember { (1..30).toList() }
        val start = remember { Recurrence.parse(value.trim()) ?: Recurrence(1, Recurrence.Unit.WEEK) }
        var amount by remember { mutableStateOf(start.amount.coerceAtMost(amounts.last())) }
        var unit by remember { mutableStateOf(start.unit) }
        var strict by remember { mutableStateOf(start.strict) }
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = { onChange(Recurrence(amount, unit, strict).toString()); picking = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { onChange(""); picking = false }) { Text("No recurrence") } },
            title = { Text("Recurrence") },
            text = {
                Column {
                    Row(
                        Modifier.fillMaxWidth().testTag("recurrence-picker"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Every")
                        Picker(amounts, amount, { amount = it }, Modifier.weight(1f))
                        val plural = if (amount == 1) "" else "s"
                        Picker(
                            Recurrence.Unit.entries,
                            unit,
                            { unit = it },
                            Modifier.weight(2f),
                            format = PickerDefaults.itemFormat(itemText = { UNIT_NAMES.getValue(it) + plural }),
                            isInfinity = false,
                        )
                    }
                    Text("Counted from", Modifier.padding(top = 12.dp))
                    RadioRow(listOf(false to "the completion", true to "the due date"), strict, "recurrence-from") { strict = it }
                }
            },
        )
    }
}

/** The locale of the device, with weeks that start on Monday as everywhere in the application: the calendar of Material takes the first day of its weeks from a locale alone. */
@OptIn(ExperimentalMaterial3Api::class)
internal expect fun mondayFirstLocale(): CalendarLocale

/** Adds entries to the context menu of the text field in [content]; the desktop and Android build these menus each in their own way. */
@Composable
internal expect fun TextMenu(items: List<Pair<String, () -> Unit>>, content: @Composable () -> Unit)

/** A button of the calendar of a [DateField], the value that it writes into the field and the sentence shown while the pointer rests on it. */
internal data class Shortcut(val title: String, val value: String, val help: String? = null)

/** The distances before the due date that the start date offers. */
private val startChoices = listOf(7 to "1w", 3 to "3d").map { (days, value) ->
    Shortcut(
        "${if (days == 7) "1 week" else "$days days"} before due", value,
        "The task appears $days days before its due date, and again so after each recurrence ($value).",
    )
}

/** The other form of the start date below its field: the date for a distance, the distance for a date; the error for a distance without due date. */
private fun startNote(text: String, start: LocalDate?, due: LocalDate?): String? = when {
    text.isEmpty() -> null
    start == null -> "Needs a due date".takeIf { due == null && Task.startBefore(text, LocalDate(2000, 1, 1)) != null }
    Task.parseDate(text) == null -> "Starts $start"
    due == null -> null
    else -> start.daysUntil(due).takeIf { it >= 0 }?.let { if (it == 1) "1 day before due" else "$it days before due" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    valid: Boolean,
    modifier: Modifier,
    /** Buttons of the calendar. */
    shortcuts: List<Shortcut> = emptyList(),
    /** Entries of the context menu of the field and the value that each writes into it. */
    menu: List<Pair<String, String>> = emptyList(),
    placeholder: String = "YYYY-MM-DD",
    /** A line below the field; an error if the field is not [valid]. */
    note: String? = null,
    /** The due date that the field depends on, which the calendar names. */
    due: LocalDate? = null,
    /** The day that the calendar opens on. */
    shown: LocalDate? = Task.parseDate(value.trim().substringBefore('T')),
) {
    var picking by remember { mutableStateOf(false) }
    TextMenu(menu.map { (title, date) -> title to { onChange(date) } }) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = !valid,
            supportingText = note?.let { { Text(it) } },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { picking = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose $label") }
            },
            modifier = modifier,
        )
    }
    if (picking) {
        // [impl->adr~week-starts-on-monday~1]
        val state = remember {
            DatePickerState(
                locale = mondayFirstLocale(),
                initialSelectedDateMillis = shown?.let { it.toEpochDays() * MILLIS_PER_DAY },
                // [impl->dsn~date-picker-context~1]
                // Without a date of its own, the field is set near the due date, not near today.
                initialDisplayedMonthMillis = (shown ?: due)?.let { it.toEpochDays() * MILLIS_PER_DAY },
            )
        }
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays((it / MILLIS_PER_DAY).toInt()).toString()) }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { onChange(""); picking = false }) { Text("No date") } },
        ) {
            // [impl->dsn~date-picker-context~1]
            // The calendar marks only today and the chosen day: the due date that the field depends on stands in the title.
            Column {
                DatePicker(
                    state,
                    title = { Text(label + (due?.let { " \u00b7 due $it" } ?: ""), Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp).testTag("date-picker-title")) },
                    // Measured after the shortcuts, which would otherwise find no room on a low screen.
                    modifier = Modifier.weight(1f, fill = false),
                )
                // Above the buttons that close the calendar, as they close it as well.
                FlowRow(Modifier.padding(horizontal = 12.dp)) {
                    for (shortcut in shortcuts) {
                        val button = @Composable {
                            TextButton(onClick = { onChange(shortcut.value); picking = false }, modifier = Modifier.testTag("date-shortcut-${shortcut.value}")) { Text(shortcut.title) }
                        }
                        if (shortcut.help == null) button() else WithHelp(shortcut.help, button)
                    }
                }
            }
        }
    }
}

/** The chosen tasks as removable chips and a field whose text suggests the open tasks that contain it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskChoice(label: String, chosen: Set<Int>, others: List<Entry>, onChange: (Set<Int>) -> Unit, tag: String) {
    var input by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    // Without text no suggestions: a file has too many tasks to offer them all.
    val suggestions = if (input.isBlank()) emptyList() else others.filter { !it.task.completed && it.index !in chosen && it.task.description.contains(input.trim(), ignoreCase = true) }.take(5)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // A fixed width, so that the search fields of *Waits for* and *Before* start at the same place.
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterVertically).width(72.dp))
        for (entry in others.filter { it.index in chosen }) {
            InputChip(
                selected = true,
                onClick = { onChange(chosen - entry.index) },
                label = { Text(entry.task.description) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove ${entry.task.description}") },
                modifier = Modifier.testTag("$tag-${entry.index}"),
            )
        }
        for (entry in suggestions) {
            SuggestionChip(onClick = { onChange(chosen + entry.index); input = "" }, label = { Text(entry.task.description) }, modifier = Modifier.testTag("$tag-suggest-${entry.index}"))
        }
        BasicTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.align(Alignment.CenterVertically).width(200.dp).height(32.dp)
                .border(1.dp, colors.outline, MaterialTheme.shapes.small).testTag("$tag-add"),
            decorationBox = { field ->
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text("Search task…", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
    }
}

/** The projects, contexts or hashtags of the task as removable chips, the most used known names as suggestions, the others behind *More…*, and a field of the size of a chip to search and add one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameList(
    kind: String,
    sign: String,
    names: List<String>,
    known: List<String>,
    onChange: (List<String>) -> Unit,
    tag: String,
    /** Container and content color of a name of the task: those of its label in the list. */
    chipColors: Pair<Color, Color>,
    /** How many open tasks carry a name, keyed with its sign. */
    uses: Map<String, Int>,
    outlined: Boolean = false,
    /** Whether the name is one that the todo.txt line keeps as this kind of metadata. */
    valid: (String) -> Boolean = { true },
) {
    var input by remember { mutableStateOf("") }
    fun add(name: String) {
        val clean = name.trim().removePrefix(sign).replace(Regex("\\s+"), "-")
        if (clean.isNotEmpty() && clean !in names && valid(clean)) onChange(names + clean)
        input = ""
    }
    var more by remember { mutableStateOf(false) }
    // Any part of the name counts, so that a name below another one is found by its last level: `rea` finds `research>reading`.
    val matching = known.filter { it !in names && it.contains(input.trim().removePrefix(sign), ignoreCase = true) }
    // A file has dozens of names; all of them as chips push the fields below out of view.
    // So without typed text only the most used ones are chips, and the others scroll in one row behind *More…*.
    val top = if (input.isBlank()) matching.sortedByDescending { uses["$sign$it"] ?: 0 }.take(TOP_NAMES) else matching
    val rest = if (input.isBlank()) matching - top.toSet() else emptyList()
    val colors = MaterialTheme.colorScheme
    val field: @Composable (Modifier) -> Unit = { modifier ->
        BasicTextField(
            value = input,
            onValueChange = { if (it.endsWith(' ')) add(it) else input = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add(input) }),
            modifier = modifier.width(150.dp).height(32.dp)
                .border(1.dp, colors.outline, MaterialTheme.shapes.small).testTag("$tag-add"),
            decorationBox = { field ->
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text("Add ${kind.lowercase()}…", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
    }
    BoxWithConstraints {
        // Wide: the field at the right end of a row of its own, so that the fields of projects, contexts and hashtags stand one below the other.
        // Narrow: after the chips, as a column of its own would leave the chips one per line.
        val wide = maxWidth >= 600.dp
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (name in names) {
                    InputChip(
                        selected = true,
                        onClick = { onChange(names - name) },
                        label = { Text("$sign$name") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $kind $name") },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = chipColors.first,
                            selectedLabelColor = chipColors.second,
                            selectedTrailingIconColor = chipColors.second,
                        ),
                        border = if (outlined) InputChipDefaults.inputChipBorder(true, selected = true, selectedBorderColor = colors.outline, selectedBorderWidth = 1.dp) else null,
                        modifier = Modifier.testTag("$tag-$name"),
                    )
                }
                for (name in top) {
                    SuggestionChip(onClick = { add(name) }, label = { Text("$sign$name") }, modifier = Modifier.testTag("$tag-suggest-$name"))
                }
                if (rest.isNotEmpty()) {
                    // A button, not a chip: as a chip it read as one more name.
                    TextButton(onClick = { more = !more }, modifier = Modifier.testTag("$tag-more")) { Text(if (more) "Less" else "More (${rest.size})…") }
                }
                if (more && rest.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("$tag-rest"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (name in rest) {
                            SuggestionChip(onClick = { add(name) }, label = { Text("$sign$name") }, modifier = Modifier.testTag("$tag-suggest-$name"))
                        }
                    }
                }
                if (!wide) field(Modifier.align(Alignment.CenterVertically))
            }
            // The top padding centers the field on the first row of chips, which are 48 dp high with their touch area.
            if (wide) field(Modifier.padding(top = 8.dp))
        }
    }
}

/** The number of known names that are chips before *More…*. */
private const val TOP_NAMES = 3
