package io.github.koppor.koppter.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * The filter criteria as a column: text, threshold date, priorities, projects, contexts, hashtags, completion.
 * Priorities, projects, contexts and hashtags offer the values that occur in the file, as the labels that the tasks carry.
 */
// [impl->dsn~filter-panel~1]
@Composable
fun FilterPanel(
    filter: Filter,
    onChange: (Filter) -> Unit,
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
    /** For the field of the text, so that a shortcut can give it the focus. */
    textModifier: Modifier = Modifier,
    /** Closes the panel where it lies above the list; `null` where it stands beside the list. */
    onClose: (() -> Unit)? = null,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp).testTag("filter-panel")) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Filter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).testTag("filter-heading"))
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("filter-close")) { Icon(Icons.Default.Close, contentDescription = "Close the filter") }
            }
        }
        OutlinedTextField(
            value = filter.text,
            onValueChange = { onChange(filter.copy(text = it)) },
            placeholder = { Text("Search text") },
            singleLine = true,
            trailingIcon = {
                if (filter.text.isNotEmpty()) {
                    IconButton(onClick = { onChange(filter.copy(text = "")) }, modifier = Modifier.testTag("filter-text-clear")) {
                        Icon(Icons.Default.Close, contentDescription = "Clear the search text")
                    }
                }
            },
            modifier = textModifier.fillMaxWidth().testTag("filter-text"),
        )

        Option("Show tasks before their start date", !filter.hideFuture, "filter-future") {
            onChange(filter.copy(hideFuture = !it))
        }
        // [impl->dsn~top-tasks~1]
        Option("Only the first $topCount tasks", topOnly, "top", onTopOnly)
        // [impl->dsn~next-actions~1]
        Option("Only the next action of each project", nextOnly, "next-actions", onNextOnly)

        LabelFilter("Priorities", priorities, filter.priorities, { "filter-priority-$it" }, { onChange(filter.copy(priorities = it)) }) { value, suffix, modifier ->
            PriorityLabel(value, modifier, suffix)
        }
        // [impl->dsn~project-hierarchy~1]
        LabelFilter(
            "Projects", projects, filter.projects, { "filter-project-$it" }, { onChange(filter.copy(projects = it)) },
            order = hierarchyOrder, depth = { project -> project.count { it == LEVEL_SEPARATOR } },
            excluded = filter.excludedProjects, onInclude = { onChange(filter.copy(excludedProjects = filter.excludedProjects - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(projects = filter.projects - value, excludedProjects = filter.excludedProjects + value)) }
            // A selected project stands outside the tree and shows its path.
            WithLinkMenu(LINK_PROJECT, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedProjects }) {
                ProjectLabel(value, modifier, suffix, path = suffix.isNotEmpty())
            }
        }
        // [impl->dsn~context-hierarchy~1]
        LabelFilter(
            "Contexts", contexts, filter.contexts, { "filter-context-$it" }, { onChange(filter.copy(contexts = it)) },
            order = hierarchyOrder, depth = { context -> context.count { it == LEVEL_SEPARATOR } },
            excluded = filter.excludedContexts, onInclude = { onChange(filter.copy(excludedContexts = filter.excludedContexts - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(contexts = filter.contexts - value, excludedContexts = filter.excludedContexts + value)) }
            // A selected context stands outside the tree and shows its path.
            WithLinkMenu(LINK_CONTEXT, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedContexts }) {
                ContextLabel(value, modifier, suffix, path = suffix.isNotEmpty())
            }
        }
        // [impl->dsn~hashtags-in-ui~1]
        LabelFilter(
            "Hashtags", hashtags, filter.hashtags, { "filter-hashtag-$it" }, { onChange(filter.copy(hashtags = it)) },
            excluded = filter.excludedHashtags, onInclude = { onChange(filter.copy(excludedHashtags = filter.excludedHashtags - it)) },
        ) { value, suffix, modifier ->
            val exclude = { onChange(filter.copy(hashtags = filter.hashtags - value, excludedHashtags = filter.excludedHashtags + value)) }
            WithLinkMenu(LINK_HASHTAG, value, longPress = true, onExclude = exclude.takeIf { value !in filter.excludedHashtags }) {
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
            FilterChip(selected = open, onClick = { show(!open, done) }, label = { Text("Open") }, modifier = Modifier.testTag("filter-open"))
            FilterChip(selected = done, onClick = { show(open, !done) }, label = { Text("Done") }, modifier = Modifier.testTag("filter-done"))
        }

        OutlinedButton(
            onClick = {
                onChange(Filter())
                onTopOnly(false)
                onNextOnly(false)
            },
            enabled = !filter.isDefault || topOnly || nextOnly,
            modifier = Modifier.padding(top = 12.dp).testTag("filter-clear"),
        ) {
            Text("Clear all filters")
        }
    }
}

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
    /** The level of a value in a hierarchy; with levels, the values form a tree, one value per line. */
    depth: (T) -> Int = { 0 },
    excluded: Set<T> = emptySet(),
    onInclude: (T) -> Unit = {},
    label: @Composable (value: T, suffix: String, modifier: Modifier) -> Unit,
) {
    if (values.isEmpty() && selected.isEmpty() && excluded.isEmpty()) return
    // The area that reacts to a click is larger than the label, which is too small for a finger.
    fun modifier(value: T, isSelected: Boolean) = Modifier
        .clip(CircleShape)
        .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onChange(if (it) selected + value else selected - value) })
        .testTag(tag(value))
        .padding(vertical = 5.dp)
    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.CenterVertically).padding(vertical = 5.dp))
        if (selected.isEmpty() && excluded.isEmpty()) {
            Text(
                "all",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
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
                        .padding(vertical = 5.dp),
                )
            }
        }
    }
    val others = values.sortedWith(order).filter { it !in selected && it !in excluded }
    if (values.any { depth(it) > 0 }) {
        Column {
            for (value in others) label(value, "", modifier(value, false).padding(start = 16.dp * depth(value)))
        }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (value in others) label(value, "", modifier(value, false))
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
@Composable
fun <T> RadioRow(options: List<Pair<T, String>>, selected: T, tag: String, onSelect: (T) -> Unit) {
    Row {
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
