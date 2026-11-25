package io.github.koppor.koppter.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.todotxt.LINK_CONTEXT
import io.github.koppor.koppter.todotxt.LINK_HASHTAG
import io.github.koppor.koppter.todotxt.LINK_PROJECT
import io.github.koppor.koppter.todotxt.Completion
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.LEVEL_SEPARATOR
import io.github.koppor.koppter.todotxt.hierarchyOrder
import io.github.koppor.koppter.todotxt.isWithin

/**
 * The filter criteria as a column: start date, priorities, projects, contexts, hashtags, completion.
 * The text of the filter is a field of the main screen, above the list, or of this panel where it lies in a drawer.
 * Priorities, projects, contexts and hashtags offer the values that occur in the file, as the labels that the tasks carry.
 */
// [impl->dsn~filter-panel~2]
@Composable
fun FilterPanel(
    filter: Filter,
    onChange: (Filter) -> Unit,
    /** Renames a project, a context or a hashtag, told by its sign, in all tasks. */
    onRename: (sign: Char, from: String, to: String) -> Unit,
    priorities: List<Char>,
    projects: List<String>,
    contexts: List<String>,
    hashtags: List<String>,
    /** The number of tasks that the list is cut to if [topOnly] is set. */
    topCount: Int,
    topOnly: Boolean,
    onTopOnly: (Boolean) -> Unit,
    nextOnly: Boolean,
    onNextOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Closes the panel where it lies above the list; `null` where it stands beside the list. */
    onClose: (() -> Unit)? = null,
    /** The field for the text of the filter, below the heading; `null` where the main screen shows it above the list. */
    search: (@Composable () -> Unit)? = null,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp).testTag("filter-panel")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Filter", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).testTag("filter-heading"))
            // In the heading, not below the criteria: there it lay out of view in a window of common height, and the switches Open and Done stand last.
            TextButton(
                onClick = {
                    onChange(Filter())
                    onTopOnly(false)
                    onNextOnly(false)
                },
                enabled = !filter.isDefault || topOnly || nextOnly,
                modifier = Modifier.testTag("filter-clear"),
            ) {
                Text("Clear all")
            }
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("filter-close")) { Icon(Icons.Default.Close, contentDescription = "Close the filter") }
            }
        }
        search?.invoke()
        // [impl->dsn~filter-label-search~2]
        var projectSearch by remember { mutableStateOf("") }
        var contextSearch by remember { mutableStateOf("") }
        var hashtagSearch by remember { mutableStateOf("") }
        // A text typed for one kind of label may name a label of another kind; the line below the field says so
        // instead of narrowing the other kinds at once, which would move the panel under the typing.
        val kinds = listOf(
            Triple("projects", projects, projectSearch) to { it: String -> projectSearch = it },
            Triple("contexts", contexts, contextSearch) to { it: String -> contextSearch = it },
            Triple("hashtags", hashtags, hashtagSearch) to { it: String -> hashtagSearch = it },
        )
        fun elsewhere(own: String): Elsewhere? {
            val word = kinds.first { it.first.first == own }.first.third
            val hits = kinds.filter { (kind, _) -> kind.first != own && kind.third != word }
                .map { (kind, set) -> Triple(kind.first, found(kind.second, word).size, set) }
                .filter { it.second > 0 }
            if (hits.isEmpty()) return null
            return Elsewhere(hits.joinToString(", ", prefix = "also: ") { (name, count) -> "$name ($count)" }) { hits.forEach { it.third(word) } }
        }
        val touch = !LocalKeyboard.current

        Option("Show tasks before their start date", !filter.hideFuture, "filter-future") {
            onChange(filter.copy(hideFuture = !it))
        }
        // [impl->dsn~top-tasks~1]
        Option("Only the first $topCount tasks", topOnly, "top", onTopOnly)
        // [impl->dsn~next-actions~1]
        Option("Only the next action of each project", nextOnly, "next-actions", onNextOnly)

        LabelFilter("Priorities", priorities, filter.priorities, { "filter-priority-$it" }, { onChange(filter.copy(priorities = it)) }, touch = touch) { value, suffix, modifier ->
            PriorityLabel(value, modifier, suffix)
        }
        // [impl->dsn~project-hierarchy~1]
        LabelFilter(
            "Projects", narrowed(projects, projectSearch), filter.projects, { "filter-project-$it" }, { onChange(filter.copy(projects = it)) },
            search = projectSearch, onSearch = { projectSearch = it }, searchTag = "filter-projects-search", elsewhere = elsewhere("projects"), touch = touch,
            matches = found(projects, projectSearch).size, total = projects.size,
            order = hierarchyOrder, depth = { project -> project.count { it == LEVEL_SEPARATOR } },
            excluded = filter.excludedProjects, onInclude = { onChange(filter.copy(excludedProjects = filter.excludedProjects - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(projects = filter.projects - value, excludedProjects = filter.excludedProjects + value)) }
            // A selected project stands outside the tree and shows its path.
            WithLinkMenu(LINK_PROJECT, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedProjects }, copyLink = false, onRename = { onRename('+', value, it) }) {
                ProjectLabel(value, modifier, suffix, path = suffix.isNotEmpty())
            }
        }
        // [impl->dsn~context-hierarchy~1]
        LabelFilter(
            "Contexts", narrowed(contexts, contextSearch), filter.contexts, { "filter-context-$it" }, { onChange(filter.copy(contexts = it)) },
            search = contextSearch, onSearch = { contextSearch = it }, searchTag = "filter-contexts-search", elsewhere = elsewhere("contexts"), touch = touch,
            matches = found(contexts, contextSearch).size, total = contexts.size,
            order = hierarchyOrder, depth = { context -> context.count { it == LEVEL_SEPARATOR } },
            excluded = filter.excludedContexts, onInclude = { onChange(filter.copy(excludedContexts = filter.excludedContexts - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(contexts = filter.contexts - value, excludedContexts = filter.excludedContexts + value)) }
            // A selected context stands outside the tree and shows its path.
            WithLinkMenu(LINK_CONTEXT, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedContexts }, copyLink = false, onRename = { onRename('@', value, it) }) {
                ContextLabel(value, modifier, suffix, path = suffix.isNotEmpty())
            }
        }
        // [impl->dsn~hashtags-in-ui~1]
        LabelFilter(
            "Hashtags", narrowed(hashtags, hashtagSearch), filter.hashtags, { "filter-hashtag-$it" }, { onChange(filter.copy(hashtags = it)) },
            search = hashtagSearch, onSearch = { hashtagSearch = it }, searchTag = "filter-hashtags-search", elsewhere = elsewhere("hashtags"), touch = touch,
            matches = found(hashtags, hashtagSearch).size, total = hashtags.size,
            excluded = filter.excludedHashtags, onInclude = { onChange(filter.copy(excludedHashtags = filter.excludedHashtags - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(hashtags = filter.hashtags - value, excludedHashtags = filter.excludedHashtags + value)) }
            WithLinkMenu(LINK_HASHTAG, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedHashtags }, copyLink = false, onRename = { onRename('#', value, it) }) {
                HashtagLabel(value, modifier, suffix)
            }
        }

        // The criterion that is changed least often stands last.
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val open = filter.completion != Completion.COMPLETED
            val done = filter.completion != Completion.OPEN
            fun show(open: Boolean, done: Boolean) = onChange(
                filter.copy(
                    completion = when {
                        open && done -> Completion.ALL
                        open -> Completion.OPEN
                        done -> Completion.COMPLETED
                        // Switching off the only selected state selects the other one: a list of neither is of no use.
                        else -> if (filter.completion == Completion.OPEN) Completion.COMPLETED else Completion.OPEN
                    },
                ),
            )
            FilterChip(colors = selectedChipColors(), selected = open, onClick = { show(!open, done) }, label = { Text("Open") }, modifier = Modifier.testTag("filter-open"))
            FilterChip(colors = selectedChipColors(), selected = done, onClick = { show(open, !done) }, label = { Text("Done") }, modifier = Modifier.testTag("filter-done"))
        }
    }
}

/** A field of one line that has a button at its end, tagged [tag]`-clear`, which empties it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, tag: String, modifier: Modifier = Modifier) {
    // Built from its parts, as `OutlinedTextField` has a height of at least 56 dp, much space around one line of text.
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().testTag(tag),
    ) { field ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = field,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interaction,
            placeholder = { Text(placeholder) },
            // Not the slot of the trailing icon, which is at least 48 dp high.
            suffix = {
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, contentDescription = "Clear the text",
                        modifier = Modifier.size(20.dp).clickable(role = Role.Button) { onChange("") }.testTag("$tag-clear"),
                    )
                }
            },
            colors = colors,
            contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 8.dp, bottom = 8.dp),
            container = { OutlinedTextFieldDefaults.Container(enabled = true, isError = false, interactionSource = interaction, colors = colors) },
        )
    }
}

/** The [values] whose name contains the [search] text, each with the values above it, so that a tree keeps its shape. */
private fun narrowed(values: List<String>, search: String): List<String> {
    if (search.isBlank()) return values
    val found = found(values, search)
    return values.filter { value -> found.any { it.isWithin(value) } }
}

/** The [values] whose name contains the [search] text; a leading sign is ignored. */
private fun found(values: List<String>, search: String): List<String> {
    val word = search.trim().trimStart('+', '@', '#')
    return if (word.isEmpty()) emptyList() else values.filter { it.contains(word, ignoreCase = true) }
}

/** The line below the field of one kind of label that names the other kinds in which its text finds labels. */
private class Elsewhere(val text: String, val show: () -> Unit)

/**
 * One criterion with several values, shown as the labels the task list uses for them.
 * The selected values stand beside the title, the others below it; a click moves a label to the other group.
 * The excluded values stand beside the title as well, after the word *not*; a click on one ends its exclusion.
 * Both groups are sorted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Comparable<T>> LabelFilter(
    title: String,
    values: List<T>,
    selected: Set<T>,
    tag: (T) -> String,
    onChange: (Set<T>) -> Unit,
    order: Comparator<T> = naturalOrder(),
    /** The level of a value in a hierarchy; a value with values below it forms a tree, one value per line. */
    depth: (T) -> Int = { 0 },
    excluded: Set<T> = emptySet(),
    onInclude: (T) -> Unit = {},
    /** The text that narrows [values]; `null` where the criterion has no field for it. */
    search: String? = null,
    onSearch: (String) -> Unit = {},
    searchTag: String = "",
    elsewhere: Elsewhere? = null,
    /** The number of values that [search] finds, of [total]. */
    matches: Int = 0,
    total: Int = 0,
    /** Without a keyboard, the area of a label that reacts to a tap is higher, for a finger. */
    touch: Boolean = false,
    label: @Composable (value: T, suffix: String, modifier: Modifier) -> Unit,
) {
    if (values.isEmpty() && search.isNullOrEmpty() && selected.isEmpty() && excluded.isEmpty()) return
    // The area that reacts to a click is larger than the label, which is too small for a finger.
    val reach = if (touch) 10.dp else 5.dp
    // A selected label has a frame in the accent color: the cross after its name alone hardly sets it apart.
    val frame = MaterialTheme.colorScheme.primary
    fun modifier(value: T, isSelected: Boolean) = Modifier
        .clip(CircleShape)
        .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onChange(if (it) selected + value else selected - value) })
        .testTag(tag(value))
        .padding(vertical = reach)
        .let { if (isSelected) it.border(2.dp, frame, CircleShape) else it }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 5.dp))
        if (selected.isEmpty() && excluded.isEmpty()) {
            Text("any", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // At the end of the title line, as the field that adds a label stands at the end of the labels of a task in the editor.
        if (search != null) {
            Spacer(Modifier.weight(1f))
            SearchField(search, onSearch, "Search…", searchTag, Modifier.width(150.dp))
        }
    }
    // [impl->dsn~filter-label-search-elsewhere~1]
    // The search cuts the list, which says so.
    if (!search.isNullOrBlank()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text("$matches of $total match", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("$searchTag-count"))
            if (elsewhere != null) {
                Text(elsewhere.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("$searchTag-elsewhere"))
                TextButton(onClick = elsewhere.show, modifier = Modifier.testTag("$searchTag-elsewhere-show")) { Text("Search there too") }
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (value in selected.sortedWith(order)) label(value, " \u00d7", modifier(value, true))
        // [impl->dsn~filter-exclusion~1]
        for (value in excluded.sortedWith(order)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("not", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                label(
                    value,
                    " \u00d7",
                    Modifier.clip(CircleShape)
                        .clickable(onClickLabel = "Show these tasks again", role = Role.Button) { onInclude(value) }
                        .testTag("${tag(value)}-excluded")
                        .padding(vertical = reach),
                )
            }
        }
    }
    val others = values.sortedWith(order).filter { it !in selected && it !in excluded }
    // A value with values below it starts a tree of one value per line; the values without any share lines.
    val trees = mutableListOf<MutableList<T>>()
    for (value in others) if (depth(value) == 0 || trees.isEmpty()) trees += mutableListOf(value) else trees.last() += value
    var index = 0
    while (index < trees.size) {
        if (trees[index].size > 1) {
            Column {
                for (value in trees[index]) label(value, "", modifier(value, false).padding(start = 16.dp * depth(value)))
            }
            index++
        } else {
            val leaves = trees.drop(index).takeWhile { it.size == 1 }.flatten()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (value in leaves) label(value, "", modifier(value, false).padding(start = 16.dp * depth(value)))
            }
            index += leaves.size
        }
    }
}

@Composable
private fun Option(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(6.dp))
        Text(label)
    }
}

/** One of several options; the tag of an option is [tag] followed by its label. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> RadioRow(options: List<Pair<T, String>>, selected: T, tag: String, onSelect: (T) -> Unit) {
    FlowRow {
        for ((option, label) in options) {
            Row(
                Modifier.selectable(selected = selected == option, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .testTag("$tag-$label").padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(label, Modifier.padding(start = 4.dp))
            }
        }
    }
}
