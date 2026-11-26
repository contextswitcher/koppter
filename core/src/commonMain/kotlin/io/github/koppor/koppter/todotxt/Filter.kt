package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

enum class Completion { ALL, COMPLETED, OPEN }

/** Separates the levels of a context or a project: `research>reading` lies below `research`. */
// [impl->dsn~context-hierarchy~1]
// [impl->dsn~project-hierarchy~1]
const val LEVEL_SEPARATOR = '>'

/** Whether the context or project is [ancestor] or lies below it. */
fun String.isWithin(ancestor: String): Boolean = this == ancestor || startsWith("$ancestor$LEVEL_SEPARATOR")

/** The context with its ancestors, the outermost first: `a`, `a>b`, `a>b>c`. */
fun String.withAncestors(): List<String> = split(LEVEL_SEPARATOR).runningReduce { path, level -> "$path$LEVEL_SEPARATOR$level" }

/** The last level of the context, which is what a label shows. */
val String.lastLevel: String get() = substringAfterLast(LEVEL_SEPARATOR)

/** Sorts a context directly before the contexts below it. */
val hierarchyOrder: Comparator<String> = compareBy { it.replace(LEVEL_SEPARATOR, '\u0000') }

/**
 * Restricts the task list.
 *
 * The criteria are combined with *and*; within [priorities], [projects], [contexts] and [hashtags] one match
 * suffices. An empty set does not restrict. A project or context also matches those below it.
 * A task that has one of the excluded projects, contexts or hashtags is left out, whatever else it has.
 */
// [impl->dsn~filter~1]
data class Filter(
    val completion: Completion = Completion.OPEN,
    val text: String = "",
    val priorities: Set<Char> = emptySet(),
    val projects: Set<String> = emptySet(),
    val contexts: Set<String> = emptySet(),
    val hashtags: Set<String> = emptySet(),
    // [impl->dsn~filter-exclusion~1]
    val excludedProjects: Set<String> = emptySet(),
    val excludedContexts: Set<String> = emptySet(),
    val excludedHashtags: Set<String> = emptySet(),
    /** Hides tasks whose threshold date (`t:`) lies after today. */
    val hideFuture: Boolean = true,
) {
    fun matches(task: Task, today: LocalDate): Boolean =
        when (completion) {
            Completion.ALL -> true
            Completion.COMPLETED -> task.completed
            Completion.OPEN -> !task.completed
        } &&
            (text.isBlank() || task.toLine().contains(text.trim(), ignoreCase = true)) &&
            (priorities.isEmpty() || task.priority in priorities) &&
            (projects.isEmpty() || task.projects.any { project -> projects.any(project::isWithin) }) &&
            (contexts.isEmpty() || task.contexts.any { context -> contexts.any(context::isWithin) }) &&
            (hashtags.isEmpty() || task.hashtags.any { it in hashtags }) &&
            task.projects.none { project -> excludedProjects.any(project::isWithin) } &&
            task.contexts.none { context -> excludedContexts.any(context::isWithin) } &&
            task.hashtags.none { it in excludedHashtags } &&
            !(hideFuture && !task.completed && task.threshold.let { it != null && it > today })

    val isDefault: Boolean get() = this == Filter()
}

/**
 * The text with a project, a context or a hashtag, told by its [sign], and the labels below it renamed from [from] to [to].
 * A label is a word of its own, so that `+papers2` and `a+papers` are not `+papers`.
 */
// [impl->dsn~label-rename~1]
fun renameLabel(text: String, sign: Char, from: String, to: String): String =
    Regex("(?<![^\\s])" + Regex.escape("$sign$from") + "(?![^\\s$LEVEL_SEPARATOR])").replace(text, Regex.escapeReplacement("$sign$to"))

/**
 * The order of the task list: open before completed, overdue before the others, then by priority (none last),
 * then by due date (none last) and the time by which they are due on that day (none last), then by position in the file.
 */
// [impl->dsn~sort-order~1]
fun listOrder(today: LocalDate): Comparator<Entry> =
    compareBy<Entry> { it.task.completed }
        .thenBy { !it.task.isOverdue(today) }
        .thenBy { it.task.priority ?: Char.MAX_VALUE }
        .thenBy { it.task.due ?: LocalDate(9999, 12, 31) }
        // [impl->dsn~due-time~1]
        .thenBy { it.task.by ?: (24 * 60) }
        .thenBy { it.index }

/** The context of a task that waits for someone else. */
const val WAITING_CONTEXT = "waiting"

/** The hashtag of a task that is not decided yet. */
const val SOMEDAY_HASHTAG = "someday"

/**
 * The open tasks that can be worked on now: those that neither wait for someone else (`@waiting`) nor are for someday (`#someday`),
 * whose threshold date has come, and that do not wait for another open task (`id:` with a task of that `p:`).
 */
// [impl->dsn~next-actions~1]
fun actionable(entries: List<Entry>, today: LocalDate): Set<Entry> {
    val open = entries.filterNot { it.task.completed }
    val parts = open.flatMap { entry -> entry.task.tags.filter { it.first == Task.PARENT }.map { it.second to entry } }.groupBy({ it.first }, { it.second })
    return open.filter { entry ->
        val task = entry.task
        task.contexts.none { it.isWithin(WAITING_CONTEXT) } && SOMEDAY_HASHTAG !in task.hashtags &&
            task.threshold.let { it == null || it <= today } &&
            task.tags.none { (key, value) -> key == Task.ID && parts[value].orEmpty().any { it != entry } }
    }.toSet()
}

/**
 * The next actions among the listed tasks: those that can be worked on, and of each project the first one only.
 * A task without project is no step towards a larger outcome and stays.
 *
 * @param listed the tasks to choose from, in the order of the list
 * @param all the tasks of the file, which tell what a task waits for
 */
// [impl->dsn~next-actions~1]
fun nextActions(listed: List<Entry>, all: List<Entry>, today: LocalDate): List<Entry> {
    val actionable = actionable(all, today)
    val seen = mutableSetOf<String>()
    // Each project of the task is marked as seen, so no `any` on the projects, which would stop at the first new one.
    return listed.filter { it in actionable && (it.task.projects.isEmpty() || it.task.projects.map(seen::add).contains(true)) }
}

/**
 * The projects that are stuck: they have an open task whose threshold date has come, and none that can be worked on, in the projects below them neither.
 */
// [impl->dsn~next-actions~1]
fun projectsWithoutNextAction(entries: List<Entry>, today: LocalDate): List<String> {
    val moving = actionable(entries, today).flatMap { it.task.projects }
    return entries.filter { !it.task.completed && it.task.threshold.let { day -> day == null || day <= today } }
        .flatMap { it.task.projects }.distinct().filter { project -> moving.none { it.isWithin(project) } }.sortedWith(hierarchyOrder)
}

private val LINK = Regex("^koppter:(?://)?filter/?\\?(.*)$", RegexOption.IGNORE_CASE)
private val PERCENT_ENCODED = Regex("(?:%[0-9A-Fa-f]{2})+")

/** The keys of a link to the application. */
const val LINK_PROJECT = "project"
const val LINK_CONTEXT = "context"
const val LINK_HASHTAG = "hashtag"

/**
 * The link to the tasks of a project, a context or a hashtag, such as `koppter://filter?project=papers%3Esurvey`.
 * Everything but letters, digits and a few signs is encoded, so that a program that finds links in a text does not cut the link short.
 */
// [impl->dsn~app-link-copy~1]
fun linkTo(key: String, name: String): String = "koppter://filter?$key=${encoded(name)}"

internal fun encoded(name: String): String = name.encodeToByteArray().joinToString("") { byte ->
    val char = byte.toInt().toChar()
    if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") char.toString() else "%" + (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
}

/** The keys and decoded values of the query of a link to the application, in their order; a key without a value is left out. */
internal fun linkValues(query: String): List<Pair<String, String>> = query.split('&').mapNotNull { part ->
    val key = part.substringBefore('=', "").lowercase()
    val value = PERCENT_ENCODED.replace(part.substringAfter('=')) { encoded ->
        encoded.value.split('%').drop(1).map { it.toInt(16).toByte() }.toByteArray().decodeToString()
    }
    (key to value).takeIf { value.isNotEmpty() }
}

private const val LINK_NOT = "not-"
private const val LINK_PRIORITY = "priority"
private const val LINK_TEXT = "text"
private const val LINK_SHOW = "show"
private const val LINK_FUTURE = "future"

/**
 * The link that names the whole filter, which [filterOfLink] reads back:
 * besides projects, contexts and hashtags also the excluded ones (`not-hashtag=import`), the priorities (`priority=A`), the text (`text=mom`),
 * the completed tasks (`show=all` or `show=done`) and the tasks before their threshold date (`future=1`).
 */
// [impl->dsn~filter-remembered~1]
fun Filter.toLink(): String = "koppter://filter?" + (
    projects.map { LINK_PROJECT to it } + contexts.map { LINK_CONTEXT to it } + hashtags.map { LINK_HASHTAG to it } +
        excludedProjects.map { LINK_NOT + LINK_PROJECT to it } + excludedContexts.map { LINK_NOT + LINK_CONTEXT to it } +
        excludedHashtags.map { LINK_NOT + LINK_HASHTAG to it } + priorities.map { LINK_PRIORITY to it.toString() } +
        listOfNotNull(
            (LINK_TEXT to text).takeIf { text.isNotBlank() },
            (LINK_SHOW to "all").takeIf { completion == Completion.ALL },
            (LINK_SHOW to "done").takeIf { completion == Completion.COMPLETED },
            (LINK_FUTURE to "1").takeIf { !hideFuture },
        )
    ).joinToString("&") { (key, value) -> "$key=${encoded(value)}" }

/**
 * The filter that a link to the application names, such as `koppter://filter?project=papers>survey&context=phone&hashtag=someday`;
 * `null` if the text is no such link or names nothing.
 * A program that holds the link may have encoded characters as `%3E`, and a name may carry its sign.
 */
// [impl->dsn~app-links~1]
fun filterOfLink(link: String): Filter? {
    val query = LINK.matchEntire(link.trim())?.groupValues?.get(1) ?: return null
    val values = linkValues(query)
    fun named(key: String, sign: String) = values.filter { it.first == key }.map { it.second.removePrefix(sign) }.filter(String::isNotEmpty).toSet()
    return Filter(
        completion = when (values.lastOrNull { it.first == LINK_SHOW }?.second) {
            "all" -> Completion.ALL
            "done" -> Completion.COMPLETED
            else -> Completion.OPEN
        },
        text = values.lastOrNull { it.first == LINK_TEXT }?.second.orEmpty(),
        priorities = values.filter { it.first == LINK_PRIORITY }.mapNotNull { (_, letter) -> letter.singleOrNull()?.takeIf { it in 'A'..'Z' } }.toSet(),
        projects = named(LINK_PROJECT, "+"),
        contexts = named(LINK_CONTEXT, "@"),
        hashtags = named(LINK_HASHTAG, "#"),
        excludedProjects = named(LINK_NOT + LINK_PROJECT, "+"),
        excludedContexts = named(LINK_NOT + LINK_CONTEXT, "@"),
        excludedHashtags = named(LINK_NOT + LINK_HASHTAG, "#"),
        hideFuture = values.none { it == LINK_FUTURE to "1" },
    ).takeIf { !it.isDefault }
}
