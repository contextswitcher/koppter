package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.listOrder
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** The times over which the stars of the members are added up. */
enum class RankingPeriod(val title: String) {
    DAY("Day"), WEEK("Week"), MONTH("Month"), QUARTER("Quarter"), YEAR("Year");

    /** The first day of the period that holds the day; a week starts on Monday. */
    fun start(day: LocalDate): LocalDate = when (this) {
        DAY -> day
        WEEK -> day.minus(DatePeriod(days = day.dayOfWeek.isoDayNumber - 1))
        MONTH -> LocalDate(day.year, day.month, 1)
        QUARTER -> LocalDate(day.year, day.month.ordinal / 3 * 3 + 1, 1)
        YEAR -> LocalDate(day.year, 1, 1)
    }
}

/** A member in a ranking; members with the same stars share a place. */
data class Rank(val member: String, val stars: Int, val place: Int)

/** A task of a member that has a time on a day. */
data class Appointment(val span: Span, val entry: Entry)

/**
 * The tasks of a family: each member is a context, that of the list of the member, and a task belongs to the member whose context it has.
 * Completing a task gives its member the stars of the task.
 */
object Family {
    /** The member that a task belongs to: the first one whose context, or a level below it, the task has. */
    fun memberOf(task: Task, members: List<String>): String? = members.firstOrNull { member -> task.contexts.any { it.isWithin(member) } }

    /**
     * The members by the stars of the tasks that they completed in the period that holds [day], the most stars first.
     * A completed task without a completion date counts for no period, as its day is not known.
     */
    // [impl->dsn~stars~1]
    fun ranking(tasks: List<Task>, members: List<String>, period: RankingPeriod, day: LocalDate): List<Rank> {
        val start = period.start(day)
        val stars = members.associateWith { member ->
            tasks.filter { task -> task.completed && task.completionDate?.let(period::start) == start && memberOf(task, members) == member }.sumOf { it.stars }
        }
        return members.sortedByDescending { stars.getValue(it) }.map { member ->
            Rank(member, stars.getValue(member), 1 + stars.values.count { it > stars.getValue(member) })
        }
    }

    /** The tasks of a member that have a time on the day, completed or not, each for its duration or for [DEFAULT_DURATION]. */
    // [impl->dsn~family-day~1]
    fun appointments(entries: List<Entry>, member: String, members: List<String>, day: LocalDate): List<Appointment> =
        entries.filter { isAppointment(it.task, day) && memberOf(it.task, members) == member }.map { entry ->
            val start = entry.task.at!!
            Appointment(Span(start, minOf(start + (entry.task.duration ?: DEFAULT_DURATION).inWholeMinutes.toInt(), MINUTES_PER_DAY)), entry)
        }.sortedBy { it.span.start }

    private fun isAppointment(task: Task, day: LocalDate) = task.at != null && task.due == day

    /**
     * The tasks of a member to do on the day, in the order of the list: the open ones that are due on that day, and on [today] also
     * those that are overdue or have no due date; behind them, those that were completed on that day.
     * A task before its threshold date and an appointment of the day are left out.
     */
    // [impl->dsn~family-day~1]
    fun todos(entries: List<Entry>, member: String, members: List<String>, day: LocalDate, today: LocalDate): List<Entry> =
        entries.filter { entry ->
            val task = entry.task
            when {
                memberOf(task, members) != member || isAppointment(task, day) -> false
                task.completed -> task.completionDate == day
                task.threshold.let { it != null && it > day } -> false
                else -> task.due == day || day == today && task.due.let { it == null || it < day }
            }
        }.sortedWith(listOrder(today))
}
