package io.github.koppor.koppter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
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
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.lastLevel
import kotlinx.datetime.LocalDate

/** Tells the state of the plan of a task and a text about it; `null` for a task that shows none. */
val LocalPlanOf = staticCompositionLocalOf<(Task) -> Pair<PlanState, String>?> { { null } }

/** What a click on the context label of a task does with the context. */
/** The names under which the priorities are shown. */
val LocalPriorityNames = staticCompositionLocalOf { PriorityNames() }

val LocalShowContext = staticCompositionLocalOf<(String) -> Unit> { {} }

val LocalShowProject = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Sets the due date of a task of the list, as the menu of a task card does. */
val LocalSetDue = staticCompositionLocalOf<(Entry, LocalDate) -> Unit> { { _, _ -> } }

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
        WithPath("@$context", label)
    }
    task.hashtags.forEach { HashtagLabel(it) }
    task.tags.forEach { (key, value) ->
        val overdue = key == Task.DUE && task.isOverdue(today)
        val dueToday = key == Task.DUE && !task.completed && task.due == today
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
            overdue -> Label(value, colors.errorContainer, colors.onErrorContainer, dueMenu, key = "$key:")
            dueToday -> Label(value, colors.primaryContainer, colors.onPrimaryContainer, dueMenu, key = "$key:")
            else -> Label(value, colors.surfaceVariant, colors.onSurfaceVariant, if (key == Task.DUE) dueMenu else Modifier, key = "$key:")
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
    if (LEVEL_SEPARATOR in path) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(path) } },
            state = rememberTooltipState(),
            content = label,
        )
    } else {
        label()
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
