package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.listOrder
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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

/** A calendar of a member at an address; [titles] if the family may know what its events are. */
data class MemberCalendar(val member: String, val address: String, val titles: Boolean = false) {
    /** The line of the settings. */
    val line: String get() = "$member = $address" + if (titles) " ${Family.TITLES}" else ""
}

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

    /**
     * The calendars of the members as the settings name them, one per line as `member = address`, or `member = address titles`
     * for a calendar of which the family may know what its events are.
     * A line without an equals sign or without an address is left out.
     */
    // [impl->dsn~absences~1]
    // [impl->dsn~absence-titles~1]
    fun calendars(setting: String): List<MemberCalendar> = setting.lines().mapNotNull { line ->
        val member = line.substringBefore('=', "").trim()
        val words = line.substringAfter('=', "").trim().split(Regex("\\s+"))
        if (member.isEmpty() || words[0].isEmpty()) null else MemberCalendar(member.removePrefix("@"), words[0], words.getOrNull(1) == TITLES)
    }

    /** The word behind the address of a calendar whose titles are told. */
    const val TITLES = "titles"

    /**
     * What the plan of a device tells the family about its [member], from the blocks of the plan and the tasks by their names in it:
     * a task of the list of the member is named, as that list is with the family anyway; any other block is a time that is taken, joined with those it touches, and what it is stays on the device.
     * A block of a task of another member is left out, as it is not the time of this one.
     */
    // [impl->dsn~busy-plan~1]
    fun busy(blocks: List<TextBlock>, tasks: Map<String, Task>, member: String, members: List<String>): List<TextBlock> {
        val (named, hidden) = blocks.mapNotNull { block ->
            val travel = block.text.startsWith(TRAVEL)
            val task = tasks[block.text.removePrefix(TRAVEL)]
            when (task?.let { memberOf(it, members) }) {
                null -> block.copy(text = CalendarTxt.BUSY)
                member -> block.copy(text = "${CalendarTxt.BUSY}: ${if (travel) TRAVEL else ""}${task.description}")
                else -> null
            }
        }.partition { it.text != CalendarTxt.BUSY }
        return named + hidden.groupBy { it.date }.flatMap { (day, same) -> same.map { it.span }.joined().map { TextBlock(day, it, CalendarTxt.BUSY) } }
    }

    /** The name of the time map in the configuration of the list of a member that says when the tasks of the member are done. */
    const val TIME_MAP = "Tasks"

    /**
     * When a member does their open tasks: these are planned into the time maps of [config], the first of which the family view edits,
     * around the other [blocks] of the calendar of the member, for [days] from [today] on.
     * A task with a time of the day keeps that time and has no block, as it is an appointment of the member already.
     */
    // ponytail: the plan of a day starts at midnight and not now, so that it stays as it is over the day and is not written anew every half an hour;
    // a task that was not done at its time stays there until the next day. Plan from now on if the family wants the rest of the day planned anew.
    // [impl->dsn~member-plan~1]
    fun planned(entries: List<Entry>, member: String, members: List<String>, config: PlanConfig, blocks: List<TextBlock>, today: LocalDate, days: Int): List<TextBlock> {
        val busy = blocks.filterNot { it.text.startsWith("${CalendarTxt.PLANNED}: ") }.groupBy({ it.date }) { it.span }
        val until = today.plus(DatePeriod(days = days))
        return plan(entries.filter { memberOf(it.task, members) == member }, config, busy, today, 0).blocks
            .filter { it.entry.task.at == null && it.date <= until }
            .map { TextBlock(it.date, it.span, "${CalendarTxt.PLANNED}: ${if (it.travel) TRAVEL else ""}${it.entry.task.description}") }
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
