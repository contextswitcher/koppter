package io.github.koppor.koppter.todotxt

import io.github.koppor.koppter.sync.TaskDates
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The file next to the todo.txt file that takes the completed tasks, as the todo.txt command line names it. */
const val DONE_FILE = "done.txt"

/** @param done the lines for `done.txt`, each with its completion date */
data class Archived(val file: TodoFile, val done: List<String>)

/**
 * Takes the tasks out of the file that were completed before the month before [today], so that the file keeps
 * those of the last month and of the current one. As the day of the month does not count, this finds tasks
 * once a month, and every device finds the same ones.
 *
 * A task stays if its day of completion is not known, if it stands for an item of Outlook or a TODO of LaTeX,
 * which would show up again without its line, if an open task waits for it, or if [keep] says so.
 *
 * @param dates the dates of the history by line, for the tasks whose line has none
 * @return `null` if there is nothing to archive
 */
// [impl->dsn~archive~1]
fun archive(file: TodoFile, dates: Map<String, TaskDates>, today: LocalDate, keep: (Task) -> Boolean = { false }): Archived? {
    val before = LocalDate(today.year, today.month, 1).minus(DatePeriod(months = 1))
    val awaited = file.entries.filterNot { it.task.completed }.flatMap { open -> open.task.tags.filter { it.first == Task.PARENT }.map { it.second } }.toSet()
    val done = mutableListOf<String>()
    val lines = file.lines.filter { line ->
        val task = line.takeIf { it.isNotBlank() }?.let(Task::parse)?.takeIf { it.completed } ?: return@filter true
        val completed = task.completionDate ?: dates[line]?.completed
        val stays = completed == null || completed >= before || keep(task) ||
            task.tag(Task.OUTLOOK) != null || task.tag(Task.LATEX) != null ||
            task.tags.any { it.first == Task.ID && it.second in awaited }
        // Outside the todo.txt file, the history no longer tells the dates of the task, so the line carries them.
        if (!stays) done += task.copy(completionDate = completed, creationDate = task.creationDate ?: dates[line]?.added).toLine()
        stays
    }
    return if (done.isEmpty()) null else Archived(file.copy(lines = lines), done)
}
