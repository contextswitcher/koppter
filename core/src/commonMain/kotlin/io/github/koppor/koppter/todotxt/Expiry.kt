package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate

/** @param removed the open tasks that were taken out, in the order of the file */
data class Expired(val file: TodoFile, val removed: List<Task>)

/**
 * Takes the open tasks out of the file whose last day, the tag `exp:`, lies before [today].
 * A completed task stays: it was done, and the archive moves it later.
 * A task that stands for an item of Outlook or a TODO of LaTeX stays as well, as the item would show up again without its line.
 *
 * @return `null` if no task has expired
 */
// [impl->dsn~expiry~1]
fun expire(file: TodoFile, today: LocalDate): Expired? {
    val removed = mutableListOf<Task>()
    val lines = file.lines.filter { line ->
        val task = line.takeIf { it.isNotBlank() }?.let(Task::parse) ?: return@filter true
        val expired = !task.completed && task.expires.let { it != null && it < today } &&
            task.tag(Task.OUTLOOK) == null && task.tag(Task.LATEX) == null
        if (expired) removed += task
        !expired
    }
    return if (removed.isEmpty()) null else Expired(file.copy(lines = lines), removed)
}
