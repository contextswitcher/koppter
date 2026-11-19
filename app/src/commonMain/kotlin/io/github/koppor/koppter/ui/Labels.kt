package io.github.koppor.koppter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.plan.PlanState
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import io.github.koppor.koppter.todotxt.LINK_CONTEXT
import io.github.koppor.koppter.todotxt.LINK_HASHTAG
import io.github.koppor.koppter.todotxt.LINK_PROJECT
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.lastLevel
import io.github.koppor.koppter.todotxt.linkTo
import kotlinx.datetime.LocalDate

/** Tells the state of the plan of a task and a text about it; `null` for a task that shows none. */
val LocalPlanOf = staticCompositionLocalOf<(Task) -> Pair<PlanState, String>?> { { null } }

/** What a click on the context label of a task does with the context. */
/** The names under which the priorities are shown. */
val LocalPriorityNames = staticCompositionLocalOf { PriorityNames() }

/** The links of projects, contexts and hashtags, each under its name with its sign; see [ConfigFile.link]. */
val LocalLabelLinks = staticCompositionLocalOf { emptyMap<String, String>() }

val LocalShowContext = staticCompositionLocalOf<(String) -> Unit> { {} }

val LocalShowProject = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Sets the due date of a task of the list, as the menu of a task card does. */
val LocalSetDue = staticCompositionLocalOf<(Entry, LocalDate?) -> Unit> { { _, _ -> } }

// The address ends before a mark that ends a sentence or closes a bracket around it.
private val WEB_ADDRESS = Regex("https?://\\S*[^\\s.,;:!?)\\]]")

/** The parts of a text that are addresses of the web. */
internal fun webAddresses(text: String): List<IntRange> = WEB_ADDRESS.findAll(text).map { it.range }.toList()

/** The number of characters of an address that a task shows. */
private const val ADDRESS_LENGTH = 50

/** The address as a task shows it: a long one is cut. */
internal fun shortened(address: String): String =
    if (address.length > ADDRESS_LENGTH) address.take(ADDRESS_LENGTH - 1) + "\u2026" else address

/**
 * The text with its addresses of the web as links, which open in the browser.
 * A long address is cut, and the whole one appears when the pointer of the mouse rests on it.
 */
// [impl->dsn~links~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
) {
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    // The places of the cut addresses in the text that is shown.
    val cut = mutableListOf<Pair<IntRange, String>>()
    val shown = buildAnnotatedString {
        var end = 0
        for (range in webAddresses(text)) {
            append(text.substring(end, range.first))
            val address = text.substring(range)
            val start = length
            withLink(LinkAnnotation.Url(address, linkStyle)) { append(shortened(address)) }
            if (address.length > ADDRESS_LENGTH) cut += (start until length) to address
            end = range.last + 1
        }
        append(text.substring(end))
    }
    if (cut.isEmpty()) {
        Text(shown, modifier, style = style, color = color, textDecoration = textDecoration)
        return
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var hovered by remember { mutableStateOf<String?>(null) }
    val tooltip = rememberTooltipState(isPersistent = true)
    LaunchedEffect(hovered) { if (hovered != null) tooltip.show() else tooltip.dismiss() }
    // The tooltip box would show its tooltip over the whole text and on a long press, which starts the drag of a card.
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(hovered.orEmpty()) } },
        state = tooltip,
        modifier = modifier,
        focusable = false,
        enableUserInput = false,
    ) {
        Text(
            shown,
            Modifier.pointerInput(text) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointer = event.changes.first()
                        hovered = layout?.takeIf { event.type != PointerEventType.Exit && pointer.type == PointerType.Mouse }?.let { layout ->
                            // The place between two characters that is nearest, also for a pointer beside the text.
                            val offset = layout.getOffsetForPosition(pointer.position)
                            cut.firstOrNull { (range, _) ->
                                listOf(offset - 1, offset).any { it in range && layout.getBoundingBox(it).contains(pointer.position) }
                            }?.second
                        }
                    }
                }
            },
            style = style,
            color = color,
            textDecoration = textDecoration,
            onTextLayout = { layout = it },
        )
    }
}

/**
 * One label of a task: a rounded box with an optional key, such as `due:`, in front of the value.
 *
 * @param outlined draws a border around the box, for a label whose container has the color of its surroundings
 */
@Composable
fun Label(text: String, container: Color, content: Color, modifier: Modifier = Modifier, key: String? = null, outlined: Boolean = false) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(percent = 50),
        border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            if (key != null) {
                Text("$key ", style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.7f))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * The labels of a task in the order projects, contexts, hashtags, tags, priority.
 * All colors come from the theme, so that the labels follow light and dark mode.
 */
// [impl->dsn~task-labels~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLabels(task: Task, today: LocalDate, onDue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    // [impl->dsn~plan-state~1]
    LocalPlanOf.current(task)?.let { (state, text) ->
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(text) } },
            state = rememberTooltipState(),
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = text,
                tint = when (state) {
                    PlanState.FITS -> LocalFineColor.current
                    PlanState.TIGHT -> colors.secondary
                    PlanState.LATE -> colors.error
                },
                modifier = Modifier.size(18.dp).testTag("plan-$state"),
            )
        }
    }
    // [impl->dsn~project-hierarchy~1]
    val showProject = LocalShowProject.current
    task.projects.forEach { project ->
        WithLinkMenu(LINK_PROJECT, project) {
            WithPath("+$project") {
                ProjectLabel(
                    project,
                    Modifier.clip(RoundedCornerShape(percent = 50))
                        .clickable(onClickLabel = "Show the tasks of this project", role = Role.Button) { showProject(project) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("label-project-$project"),
                )
            }
        }
    }
    // [impl->dsn~context-hierarchy~1]
    val showContext = LocalShowContext.current
    task.contexts.forEach { context ->
        val label = @Composable {
            ContextLabel(
                context,
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .clickable(onClickLabel = "Show the tasks of this context", role = Role.Button) { showContext(context) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag("label-context-$context"),
            )
        }
        WithLinkMenu(LINK_CONTEXT, context) { WithPath("@$context", label) }
    }
    task.hashtags.forEach { hashtag -> WithLinkMenu(LINK_HASHTAG, hashtag) { HashtagLabel(hashtag, Modifier.testTag("label-hashtag-$hashtag")) } }
    task.tags.forEach { (key, value) ->
        // [impl->dsn~deadline~1]
        // A due date that has passed is highlighted as on its day while the deadline lies ahead.
        val date = when (key) {
            Task.DUE -> Task.parseDate(value)
            Task.DEADLINE -> Task.parseDeadline(value)?.first
            else -> null
        }
        val passed = !task.completed && date != null && date < today
        val overdue = passed && (key == Task.DEADLINE || task.isOverdue(today))
        val dueToday = !task.completed && date == today || passed && !overdue
        // [impl->dsn~due-menu~1]
        val dueMenu = Modifier.clip(RoundedCornerShape(percent = 50))
            .clickable(onClickLabel = "Move the due date", role = Role.Button, onClick = onDue)
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("label-due")
        when {
            // [impl->dsn~context-switcher-tag~1]
            key == Task.CONTEXT_SWITCHER -> Label(
                value,
                colors.surfaceVariant,
                colors.onSurfaceVariant,
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .clickable(onClickLabel = "Switch to the task in ContextSwitcher", role = Role.Button) { uriHandler.openUri(Task.contextSwitcherLink(value)) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag("label-cs"),
                key = "$key:",
            )
            // [impl->dsn~outlook-tag~1]
            // The value is an EntryID of almost a hundred characters, which says nothing to the user.
            key == Task.OUTLOOK -> Outlook.entryId(value)?.let { entryId ->
                Label(
                    "Outlook",
                    colors.tertiaryContainer,
                    colors.onTertiaryContainer,
                    Modifier.clip(RoundedCornerShape(percent = 50))
                        .clickable(onClickLabel = "Open the item in Outlook", role = Role.Button) { uriHandler.openUri(Outlook.anchor(entryId)) }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("label-outlook"),
                )
            }
            else -> {
                val (container, content) = when {
                    overdue -> colors.errorContainer to colors.onErrorContainer
                    dueToday -> colors.primaryContainer to colors.onPrimaryContainer
                    else -> colors.surfaceVariant to colors.onSurfaceVariant
                }
                // [impl->dsn~deadline~1]
                // A deadline of another zone is shown as it counts here; what was written is at hand as well.
                val local = if (key == Task.DEADLINE) Task.localDeadline(value) else null
                val label = @Composable {
                    Label(local ?: value, container, content, if (key == Task.DUE) dueMenu else Modifier, key = "$key:")
                }
                if (local != null) WithTooltip("As written: $value", label) else label()
            }
        }
    }
    task.priority?.let { PriorityLabel(it) }
}

/**
 * Container and content color of a project, a context and a hashtag, wherever one is shown:
 * as a label of a task, in the filter panel and as a chip of the edit dialog.
 */
@Composable
fun projectColors(): Pair<Color, Color> = MaterialTheme.colorScheme.run { tertiaryContainer to onTertiaryContainer }

@Composable
fun contextColors(): Pair<Color, Color> = MaterialTheme.colorScheme.run { secondaryContainer to onSecondaryContainer }

@Composable
fun hashtagColors(): Pair<Color, Color> = MaterialTheme.colorScheme.run { surface to onSurface }

/** @param suffix text after the value, such as the mark of a label that can be removed */
/** Shows the label, which holds the last level only, with the whole path when the pointer rests on it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithPath(path: String, label: @Composable () -> Unit) {
    if (LEVEL_SEPARATOR in path) WithTooltip(path, label) else label()
}

/** Shows the label with the text when the pointer rests on it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithTooltip(text: String, label: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = label,
    )
}

/**
 * Offers the link to the tasks of a project, a context or a hashtag in a menu that a click with the secondary button on the label opens,
 * and the link that the configuration gives the label.
 *
 * @param key the key of the link, such as [LINK_PROJECT]
 * @param onExclude hides the tasks of the label; `null` where the menu does not offer that
 * @param copyLink whether the menu offers the link to the tasks of the label
 * @param onRename gives the label the new name in all tasks; `null` where the menu does not offer that
 * @param longPress opens the menu also when a finger stays on the label; not for the label of a task, where a long press picks up the card
 */
// [impl->dsn~app-link-copy~1]
@Composable
fun WithLinkMenu(
    key: String,
    name: String,
    longPress: Boolean = false,
    onExclude: (() -> Unit)? = null,
    copyLink: Boolean = true,
    onRename: ((String) -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    // ponytail: the clipboard of text, which is deprecated for one that needs code per platform; move when it is removed.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val sign = when (key) {
        LINK_PROJECT -> "+"
        LINK_CONTEXT -> "@"
        else -> "#"
    }
    val link = ConfigFile.link(LocalLabelLinks.current, sign + name)
    Box(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    // Before the label and the card around it see the click, which the card would answer with its own menu.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        open = true
                    }
                }
            }
        }.pointerInput(longPress) {
            // A finger that stays on the label: there is no secondary button on a touch screen.
            if (longPress) awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.type == PointerType.Mouse) return@awaitEachGesture
                try {
                    // Ends without the menu if the finger is lifted, which is a click on the label, or moves, which scrolls the list.
                    withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(PointerEventPass.Initial) }
                } catch (_: PointerEventTimeoutCancellationException) {
                    open = true
                    // The rest of the touch is taken, so that lifting the finger is no click on the label or on the card.
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
        },
    ) {
        label()
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.testTag("link-menu")) {
            if (copyLink) {
                DropdownMenuItem(
                    text = { Text("Copy link") },
                    onClick = {
                        clipboard.setText(AnnotatedString(linkTo(key, name)))
                        open = false
                    },
                    modifier = Modifier.testTag("copy-link"),
                )
            }
            // [impl->dsn~label-links~1]
            if (link != null) {
                DropdownMenuItem(
                    text = { Text("Open link") },
                    onClick = {
                        uriHandler.openUri(link)
                        open = false
                    },
                    modifier = Modifier.testTag("open-link"),
                )
            }
            // [impl->dsn~label-rename~1]
            if (onRename != null) {
                DropdownMenuItem(
                    text = { Text("Rename\u2026") },
                    onClick = {
                        open = false
                        renaming = true
                    },
                    modifier = Modifier.testTag("rename-label"),
                )
            }
            // [impl->dsn~filter-exclusion~1]
            if (onExclude != null) {
                DropdownMenuItem(
                    text = { Text("Hide these tasks") },
                    onClick = {
                        onExclude()
                        open = false
                    },
                    modifier = Modifier.testTag("exclude-label"),
                )
            }
        }
        if (renaming && onRename != null) {
            var to by remember { mutableStateOf(name) }
            // A label is one word, and a hashtag does not start with a digit.
            val valid = to.isNotEmpty() && to != name && to.none { it.isWhitespace() } && (key != LINK_HASHTAG || Task.isHashtagName(to))
            AlertDialog(
                onDismissRequest = { renaming = false },
                confirmButton = {
                    TextButton(onClick = { onRename(to); renaming = false }, enabled = valid, modifier = Modifier.testTag("rename-ok")) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = { renaming = false }, modifier = Modifier.testTag("rename-cancel")) { Text("Cancel") } },
                title = { Text("Rename $sign$name") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(to, { to = it }, prefix = { Text(sign) }, singleLine = true, modifier = Modifier.testTag("rename-name"))
                        Text("In all tasks and scheduled tasks.", style = MaterialTheme.typography.bodySmall)
                    }
                },
            )
        }
    }
}

/** @param path shows all levels of a project below another one instead of the last one */
@Composable
fun ProjectLabel(project: String, modifier: Modifier = Modifier, suffix: String = "", path: Boolean = false) {
    val (container, content) = projectColors()
    Label("+${if (path) project else project.lastLevel}$suffix", container, content, modifier)
}

/** @param path shows all levels of a hierarchical context instead of the last one */
@Composable
fun ContextLabel(context: String, modifier: Modifier = Modifier, suffix: String = "", path: Boolean = false) {
    val (container, content) = contextColors()
    Label("@${if (path) context else context.lastLevel}$suffix", container, content, modifier)
}

// [impl->dsn~hashtags-in-ui~1]
/** Outlined, as the theme has no third container color that differs enough from those of projects and contexts. */
@Composable
fun HashtagLabel(hashtag: String, modifier: Modifier = Modifier, suffix: String = "") {
    val (container, content) = hashtagColors()
    Label("#$hashtag$suffix", container, content, modifier, outlined = true)
}

@Composable
fun PriorityLabel(priority: Char, modifier: Modifier = Modifier, suffix: String = "") {
    val colors = MaterialTheme.colorScheme
    val text = LocalPriorityNames.current.name(priority) + suffix
    when (priority) {
        'A' -> Label(text, colors.error, colors.onError, modifier)
        'B' -> Label(text, colors.tertiary, colors.onTertiary, modifier)
        'C' -> Label(text, colors.secondary, colors.onSecondary, modifier)
        else -> Label(text, colors.surfaceVariant, colors.onSurfaceVariant, modifier)
    }
}
