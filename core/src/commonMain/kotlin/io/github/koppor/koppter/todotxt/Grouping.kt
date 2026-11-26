package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** The attribute whose values form the columns of the board. */
enum class GroupKey(val label: String) {
    PRIORITY("Priority"),
    PROJECT("Project"),
    CONTEXT("Context"),
    HASHTAG("Hashtag"),
    DUE("Due date"),
}

/**
 * One column of the board.
 *
 * @param value the value of the attribute that the tasks of the column share: a priority, a project, a context,
 *   a hashtag or a due date range; `null` for the column of the tasks without a value
 */
data class Group(val title: String, val entries: List<Entry>, val value: String? = null)

/**
 * The parts of a list without groups, in the order of [listOrder]: the overdue tasks, the other open ones and the completed ones.
 * A part without tasks is left out.
 */
// [impl->dsn~list-groups~1]
fun listParts(entries: List<Entry>, today: LocalDate): List<Group> = listOf(
    Group("Overdue", entries.filter { it.task.isOverdue(today) }),
    Group("Next", entries.filter { !it.task.completed && !it.task.isOverdue(today) }),
    Group("Done", entries.filter { it.task.completed }),
).filter { it.entries.isNotEmpty() }

/**
 * Distributes the entries over groups by the given attribute, keeping their order within each group.
 *
 * A task with several projects, contexts or hashtags appears in each of their groups.
 * Tasks without a value form the last group; empty groups are left out.
 */
// [impl->dsn~board-groups~1]
fun groupEntries(entries: List<Entry>, key: GroupKey, today: LocalDate): List<Group> {
    fun byValues(none: String, prefix: String, values: (Task) -> List<String>): List<Group> {
        val named = entries.flatMap { values(it.task) }.distinct().sorted()
            .map { value -> Group(prefix + value, entries.filter { value in values(it.task) }, value) }
        return named + Group(none, entries.filter { values(it.task).isEmpty() })
    }
    return when (key) {
        GroupKey.PRIORITY -> byValues("No priority", "") { listOfNotNull(it.priority?.toString()) }
        GroupKey.PROJECT -> byValues("No project", "+") { it.projects }
        GroupKey.CONTEXT -> byValues("No context", "@") { it.contexts }
        GroupKey.HASHTAG -> byValues("No hashtag", "#") { it.hashtags }
        // [impl->dsn~due-groups~2]
        GroupKey.DUE -> buildList {
            fun between(after: LocalDate?, until: LocalDate?) =
                entries.filter { it.task.due.let { due -> due != null && (after == null || due > after) && (until == null || due <= until) } }
            var last = today.minus(DatePeriod(days = 1))
            // A completed task is not overdue; it keeps a group of its own instead of vanishing.
            val past = between(null, last).partition { !it.task.completed }
            add(Group("Overdue", past.first, OVERDUE))
            // A group has the tasks that are due after the day of the group before it, up to its own day.
            for ((name, day) in dueChoices(today)) {
                // The group spans the days up to that Sunday, which are next week, not a weekend.
                val title = if (name == "next weekend") "next week" else name
                add(Group(title.replaceFirstChar(Char::uppercase), between(last, day), day.toString()))
                last = day
            }
            add(Group("Later", between(last, null), LATER))
            add(Group("No due date", entries.filter { it.task.due == null }))
            add(Group("Done", past.second, DONE))
        }
    }.filter { it.entries.isNotEmpty() }
}

/** A column of the board that the user defined: the tasks that match the filter [expression]. */
data class BoardColumn(val title: String, val expression: String)

// The equals sign of a comparison such as `due:<=today` does not separate title and expression.
private val TITLE_SEPARATOR = Regex("(?<![<>:])=")

/**
 * Reads the definitions of board columns: one column per line, as `title = expression`.
 * A line without title is a column titled by its expression.
 */
// [impl->dsn~board-defined-columns~1]
fun parseBoardColumns(text: String): List<BoardColumn> =
    text.lines().map(String::trim).filter(String::isNotEmpty).map { line ->
        val parts = line.split(TITLE_SEPARATOR, limit = 2).map(String::trim)
        if (parts.size == 2 && parts[0].isNotEmpty()) BoardColumn(parts[0], parts[1]) else BoardColumn(line, line)
    }

/**
 * Distributes the entries over the columns that the user defined, see [Query] for the expressions.
 * A task appears in each column whose expression it matches; a column without tasks stays.
 */
// [impl->dsn~board-defined-columns~1]
fun groupByColumns(entries: List<Entry>, columns: List<BoardColumn>, today: LocalDate): List<Group> =
    columns.map { column ->
        val query = Query.parse(column.expression)
        Group(column.title, entries.filter { query.matches(it.task, today) })
    }

private const val OVERDUE = "overdue"
private const val LATER = "later"
private const val DONE = "done"

/**
 * The days that are offered for a due date, each with what it is called, in the order of the days; each day once.
 * A weekend and a week end on their Sunday, as in the dates that are typed in words.
 */
// [impl->dsn~due-menu~1]
fun dueChoices(today: LocalDate): List<Pair<String, LocalDate>> = dueDays(today).distinctBy { it.second }.sortedBy { it.second }

/** The days of [dueChoices], each under its name on every day. */
fun dueDays(today: LocalDate): List<Pair<String, LocalDate>> = listOf(
    "today" to today,
    "tomorrow" to today.plus(DatePeriod(days = 1)),
    "this weekend" to today.plus(DatePeriod(days = 7 - today.dayOfWeek.isoDayNumber)),
    "in three days" to today.plus(DatePeriod(days = 3)),
    // Named after the day it sets, the Sunday: as "next week", it read like a day a week from now.
    "next weekend" to today.plus(DatePeriod(days = 14 - today.dayOfWeek.isoDayNumber)),
    "in two weeks" to today.plus(DatePeriod(days = 14)),
    "in a month" to today.plus(DatePeriod(months = 1)),
    // In the choices, a day keeps its first name: on a Sunday, this weekend is today, and on a Thursday, three days from now are this weekend.
)

/**
 * The days that the context menu of the deadline offers: in two, four and eight weeks, then the last day of each of three quarters.
 * The quarters start with the one after the next, since a deadline in the current or the next quarter is reached by the weeks.
 */
// [impl->dsn~deadline-menu~1]
fun deadlineChoices(today: LocalDate): List<Pair<String, LocalDate>> {
    val weeks = listOf(2, 4, 8).map { "in $it weeks" to today.plus(DatePeriod(days = 7 * it)) }
    val current = today.year * 4 + (today.month.number - 1) / 3
    val quarters = (current + 2..current + 4).map { q ->
        val year = q / 4
        val number = q % 4 + 1
        "Q$number $year" to LocalDate(year, number * 3, 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    }
    return weeks + quarters
}

/**
 * The task as it is after the user moved it from one column of the board to another.
 *
 * The value of the source column is replaced by that of the target column; other projects, contexts and hashtags stay.
 * For due date ranges, a column sets its day, such as the Sunday for *this weekend*, *later* the day after the last of them,
 * and the column without due date removes the date.
 *
 * @return `null` if the move makes no sense: within a column, or into the column of overdue or of done tasks
 */
// [impl->dsn~board-move~1]
fun moveToGroup(task: Task, key: GroupKey, from: Group, to: Group, today: LocalDate): Task? {
    if (from.value == to.value) return null
    return when (key) {
        GroupKey.PRIORITY -> task.edit(priority = to.value?.single())
        GroupKey.PROJECT -> task.edit(projects = (task.projects - listOfNotNull(from.value).toSet() + listOfNotNull(to.value)).distinct())
        GroupKey.CONTEXT -> task.edit(contexts = (task.contexts - listOfNotNull(from.value).toSet() + listOfNotNull(to.value)).distinct())
        GroupKey.HASHTAG -> task.edit(hashtags = (task.hashtags - listOfNotNull(from.value).toSet() + listOfNotNull(to.value)).distinct())
        GroupKey.DUE -> when (to.value) {
            null -> task.withTag(Task.DUE, null)
            OVERDUE, DONE -> null
            LATER -> task.withTag(Task.DUE, dueChoices(today).last().second.plus(DatePeriod(days = 1)).toString())
            else -> task.withTag(Task.DUE, to.value)
        }
    }
}
