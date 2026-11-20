package io.github.koppor.koppter.todotxt

private fun Task.values(key: String) = tags.filter { it.first == key }.map { it.second }

/** The entries of [others] that [task] waits for: as topydo writes it, their `p:` names an `id:` of the task. */
fun waitsFor(task: Task, others: List<Entry>): List<Entry> = task.values(Task.ID).let { ids -> others.filter { o -> o.task.values(Task.PARENT).any { it in ids } } }

/** The entries of [others] that wait for [task]. */
fun waitingFor(task: Task, others: List<Entry>): List<Entry> = task.values(Task.PARENT).let { parents -> others.filter { o -> o.task.values(Task.ID).any { it in parents } } }

/**
 * The task and the changed [others] after the task waits for exactly the entries with the line numbers [waits] and exactly those of [before] wait for it.
 * A task that needs an `id:` and has none gets the next free number, as topydo numbers them.
 */
// [impl->dsn~edit-dependencies~1]
fun withDependencies(task: Task, others: List<Entry>, waits: Set<Int>, before: Set<Int>): Pair<Task, List<Entry>> {
    var next = ((others.map { it.task } + task).flatMap { it.values(Task.ID) }.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1
    fun idOf(t: Task): Pair<Task, String> = t.values(Task.ID).firstOrNull()?.let { t to it } ?: "${next++}".let { t.edit(tags = t.tags + (Task.ID to it)) to it }
    fun Task.without(key: String, values: List<String>) = edit(tags = tags.filterNot { it.first == key && it.second in values })

    var self = task
    val changed = mutableListOf<Entry>()
    for (entry in others) {
        var other = entry.task
        if ((entry.index in waits) != other.values(Task.PARENT).any { it in self.values(Task.ID) }) {
            if (entry.index in waits) {
                val (s, id) = idOf(self)
                self = s
                other = other.edit(tags = other.tags + (Task.PARENT to id))
            } else {
                other = other.without(Task.PARENT, self.values(Task.ID))
            }
        }
        if ((entry.index in before) != self.values(Task.PARENT).any { it in other.values(Task.ID) }) {
            if (entry.index in before) {
                val (o, id) = idOf(other)
                other = o
                self = self.edit(tags = self.tags + (Task.PARENT to id))
            } else {
                self = self.without(Task.PARENT, other.values(Task.ID))
            }
        }
        if (other != entry.task) changed += Entry(entry.index, other)
    }
    return self to changed
}
