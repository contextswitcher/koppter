package io.github.koppor.koppter.todotxt

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/**
 * How far an open task is on its way to its due date.
 *
 * @param daysLeft the days until the due date; negative if it has passed
 * @param elapsed the part of the time from the start of the task to its due date that has passed, from 0 to 1
 * @param deadlineLeft the days until the deadline of a task that has one apart from its due date; negative if it has passed
 */
data class Urgency(val daysLeft: Int, val elapsed: Float, val deadlineLeft: Int? = null) {
    /** A due date that has passed is no failure while the deadline lies ahead: the task is then due, as on its due date. */
    // [impl->dsn~deadline~1]
    val state: UrgencyState
        get() = when {
            (deadlineLeft ?: daysLeft) < 0 -> UrgencyState.OVERDUE
            daysLeft <= 0 -> UrgencyState.DUE
            else -> UrgencyState.FINE
        }

    val text: String
        get() = when {
            daysLeft < -1 -> "${-daysLeft} days overdue"
            daysLeft == -1 -> "1 day overdue"
            daysLeft == 0 -> "Due today"
            daysLeft == 1 -> "Due tomorrow"
            else -> "Due in $daysLeft days"
        } + when (deadlineLeft) {
            null -> ""
            -1 -> ", deadline yesterday"
            0 -> ", deadline today"
            1 -> ", deadline tomorrow"
            else -> if (deadlineLeft < 0) ", deadline ${-deadlineLeft} days ago" else ", deadline in $deadlineLeft days"
        }
}

/** Ordered from the best to the worst state; [DUE] is due today, or due earlier with the deadline still ahead. */
enum class UrgencyState { FINE, DUE, OVERDUE }

/**
 * The urgency of the task; `null` if it is completed or has no due date.
 * The task starts on its threshold date, else on its creation date.
 */
// ponytail: a task without either starts two weeks before its due date; the date from the git history if the bar should be exact.
// [impl->dsn~overview~1]
fun Task.urgency(today: LocalDate): Urgency? {
    val due = due?.takeIf { !completed } ?: return null
    val start = (threshold ?: creationDate)?.takeIf { it < due } ?: due.minus(DatePeriod(days = 14))
    return Urgency(today.daysUntil(due), (start.daysUntil(today).toFloat() / start.daysUntil(due)).coerceIn(0f, 1f), deadline?.takeIf { it != due }?.let(today::daysUntil))
}

/**
 * The state of a tile of the overview: a context or a project of the outermost level with those below it, or a hashtag.
 *
 * @param name `null` for the tasks that have none
 * @param title the name with its sign, as the tile shows it
 * @param entries the open tasks, the most urgent first and those without due date last
 */
data class OverviewState(val name: String?, val title: String, val entries: List<Entry>, val dueToday: Int, val overdue: Int) {
    val state: UrgencyState
        get() = when {
            overdue > 0 -> UrgencyState.OVERDUE
            dueToday > 0 -> UrgencyState.DUE
            else -> UrgencyState.FINE
        }
}

/** The attributes that the overview has tiles for. */
val OVERVIEW_KEYS = listOf(GroupKey.CONTEXT, GroupKey.PROJECT, GroupKey.HASHTAG)

/**
 * The contexts, projects or hashtags that have open tasks, sorted by name; the tasks that have none last.
 * Contexts and projects are those of the outermost level, each with what lies below it.
 */
// [impl->dsn~overview~1]
fun overviewStates(entries: List<Entry>, today: LocalDate, key: GroupKey = GroupKey.CONTEXT): List<OverviewState> {
    val (sign, values) = when (key) {
        GroupKey.PROJECT -> "+" to Task::projects
        GroupKey.HASHTAG -> "#" to Task::hashtags
        else -> "@" to Task::contexts
    }
    val open = entries.filterNot { it.task.completed }
    val names = open.flatMap { values(it.task) }.map { it.substringBefore(LEVEL_SEPARATOR) }.distinct().sorted()
    return (names + null).mapNotNull { name ->
        val own = open.filter { entry ->
            if (name == null) values(entry.task).isEmpty() else values(entry.task).any { it.isWithin(name) }
        }.sortedWith(compareBy<Entry> { it.task.urgency(today)?.daysLeft ?: Int.MAX_VALUE }.then(listOrder(today)))
        if (own.isEmpty()) return@mapNotNull null
        val states = own.mapNotNull { it.task.urgency(today)?.state }
        val title = name?.let { sign + it } ?: "No ${key.label.lowercase()}"
        OverviewState(name, title, own, states.count { it == UrgencyState.DUE }, states.count { it == UrgencyState.OVERDUE })
    }
}
