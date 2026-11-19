package io.github.koppor.koppter.ui

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
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.rememberDatePickerState
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
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.dueChoices
import com.kez.picker.Picker
import com.kez.picker.PickerDefaults
import com.kez.picker.duration.DurationPicker
import com.kez.picker.duration.rememberDurationPickerState
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

/**
 * Edits one task.
 *
 * The first field holds the description only; the metadata has its own fields below it.
 * Below the fields, the dialog shows the todo.txt line that *Save* writes.
 */
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
    /** The contexts that a task takes with a project. */
    contextsOf: (String) -> List<String> = { emptyList() },
    /** Called with the task, its note and the images that were added to the note, each under its file name. */
    onSave: (Task, note: String?, images: Map<String, ByteArray>) -> Unit,
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
) {
    // The task as the line was last typed; the fields start anew from it.
    // [impl->dsn~edit-line~1]
    var base by remember { mutableStateOf(original) }
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
    var note by remember { mutableStateOf(TextFieldValue(originalNote.orEmpty())) }
    /** The images that were added in this dialog, which are written when the task is saved. */
    var images by remember { mutableStateOf(mapOf<String, ByteArray>()) }
    var imageMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
                ""
            } catch (e: Exception) {
                "Not attached: ${e.message ?: e}"
            }
        }
    }

    val dueValid = due.isBlank() || Task.parseDate(due.trim()) != null
    val deadlineValid = deadline.isBlank() || Task.parseDeadline(deadline.trim()) != null
    val thresholdValid = threshold.isBlank() || Task.parseDate(threshold.trim()) != null
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
        .withTag(Task.THRESHOLD, threshold.trim())
        .withTag(Task.RECURRENCE, recurrence.trim())
        .withTag(Task.DURATION, duration.trim())
        .withTag(Task.AT, at.trim())
        .withTag(Task.TRAVEL, travel.trim())
        .withTag(Task.SPENT, spent.trim())
        // A link pasted into the description counts only while the field is empty.
        .withTag(Task.CONTEXT_SWITCHER, Task.contextSwitcherId(contextSwitcher))
        .let { if (contextSwitcher.isBlank()) it.withContextSwitcherTags() else it }
        // [impl->dsn~notes~1]
        .let { if (originalNote == null) it else it.withTag(Task.NOTE, if (note.text.isBlank()) null else base.tag(Task.NOTE) ?: newNoteName) }
    // [impl->dsn~edit-dates~1]
    // Words of a description that was not touched stay words: they were there when the task was written.
    val result = if (description.text == base.description) fields else fields.withDates(today)
    /** Where the words that became dates start in the description; `null` if there are none. */
    val phraseStart = result.description.length.takeIf { result != fields && description.text.startsWith(result.description) }
    val valid = dueValid && deadlineValid && thresholdValid && recurrenceValid && durationValid && atValid && byValid && travelValid && spentValid && result.body.isNotBlank() &&
        // What a new task starts with is no task yet.
        (!isNew || result != original)

    val descriptionFocus = remember { FocusRequester() }
    val keyboard = LocalKeyboard.current
    // Without a keyboard, the focus brings up the keyboard of the screen, which hides half of an existing task; a new task is typed first.
    LaunchedEffect(Unit) { if (keyboard || isNew) descriptionFocus.requestFocus() }

    val state = result.toLine() to note.text
    val initial = remember { state }

    // [impl->dsn~edit-resize~1]
    var shownWidth by remember { mutableStateOf(width.dp) }
    val density = LocalDensity.current
    GuardedDialog(changed = state != initial, onDismiss = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            // By default wide enough for the priorities and the days of the due date in one row each.
            modifier = Modifier.widthIn(max = shownWidth).testTag("edit-dialog")
                // [impl->dsn~keyboard-shortcuts~1]
                .onPreviewKeyEvent {
                    val save = it.type == KeyEventType.KeyDown && it.key == Key.Enter && (it.isCtrlPressed || it.isMetaPressed)
                    if (save && valid) onSave(result, if (originalNote == null) null else note.text, images)
                    save
                },
        ) {
            Box {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(if (isNew) "New task" else "Edit task", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("edit-heading"))
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

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val names = LocalPriorityNames.current
                        val choices = (listOf<Char?>(null) + names.choices).let { if (priority in it) it else it + priority }
                        for (choice in choices) {
                            FilterChip(
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
                                shortcut = ("Use due date" to due.trim()).takeIf { due.isNotBlank() && dueValid },
                            )
                        }
                        // Called the threshold date by Simpletask and the start date by topydo; the tag is the same.
                        WithHelp("The task is hidden and not planned before this day (t:).") {
                            DateField("Start date", threshold, { threshold = it }, thresholdValid, field.testTag("edit-threshold"))
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
                    }, "edit-project", projectColors())
                    NameList("Context", "@", contexts, knownContexts, { contexts = it }, "edit-context", contextColors())
                    // [impl->dsn~hashtags-in-ui~1]
                    // "#123" in a line is a word of the description, not a hashtag.
                    NameList("Hashtag", "#", hashtags, knownHashtags, { hashtags = it }, "edit-hashtag", hashtagColors(), outlined = true, valid = Task::isHashtagName)

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
                            modifier = Modifier.fillMaxWidth().testTag("edit-note"),
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

                    Row(Modifier.fillMaxWidth()) {
                        if (!isNew) {
                            TextButton(onClick = onDelete, modifier = Modifier.testTag("edit-delete")) { Text("Delete") }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit-cancel")) { Text("Cancel") }
                        Button(onClick = { onSave(result, if (originalNote == null) null else note.text, images) }, enabled = valid, modifier = Modifier.testTag("edit-save")) { Text("Save") }
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
    val tooltip = rememberTooltipState(isPersistent = true)
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

private const val MILLIS_PER_DAY = 86_400_000L

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
private fun TimeField(label: String, example: String, value: String, onChange: (String) -> Unit, valid: Boolean, modifier: Modifier) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    valid: Boolean,
    modifier: Modifier,
    /** A button of the calendar and the value that it writes into the field. */
    shortcut: Pair<String, String>? = null,
) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        isError = !valid,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { picking = true }) { Icon(Icons.Default.DateRange, contentDescription = "Choose $label") }
        },
        modifier = modifier,
    )
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Task.parseDate(value.trim().substringBefore('T'))?.let { it.toEpochDays() * MILLIS_PER_DAY },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays((it / MILLIS_PER_DAY).toInt()).toString()) }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    shortcut?.let { (title, date) -> TextButton(onClick = { onChange(date); picking = false }) { Text(title) } }
                    TextButton(onClick = { onChange(""); picking = false }) { Text("No date") }
                }
            },
        ) {
            DatePicker(state)
        }
    }
}

/** The projects, contexts or hashtags of the task as removable chips, the known names as suggestions, and a field of the size of a chip to add one. */
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
    // The names of the task first, then the known names that can be added with one click.
    // Any part of the name counts, so that a name below another one is found by its last level: `rea` finds `research>reading`.
    val suggestions = known.filter { it !in names && it.contains(input.trim().removePrefix(sign), ignoreCase = true) }
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        for (name in suggestions) {
            SuggestionChip(onClick = { add(name) }, label = { Text("$sign$name") }, modifier = Modifier.testTag("$tag-suggest-$name"))
        }
        BasicTextField(
            value = input,
            onValueChange = { if (it.endsWith(' ')) add(it) else input = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add(input) }),
            modifier = Modifier.align(Alignment.CenterVertically).width(150.dp).height(32.dp)
                .border(1.dp, colors.outline, MaterialTheme.shapes.small).testTag("$tag-add"),
            decorationBox = { field ->
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text("Add ${kind.lowercase()}…", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
    }
}
