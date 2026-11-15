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
 */
// [impl->dsn~filter~1]
data class Filter(
    val completion: Completion = Completion.OPEN,
    val text: String = "",
    val priorities: Set<Char> = emptySet(),
    val projects: Set<String> = emptySet(),
    val contexts: Set<String> = emptySet(),
    val hashtags: Set<String> = emptySet(),
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
            !(hideFuture && !task.completed && task.threshold.let { it != null && it > today })

    val isDefault: Boolean get() = this == Filter()
}

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
