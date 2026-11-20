package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals

class DependenciesTest {
    private val others = listOf(Entry(0, Task.parse("Collect sources id:3")), Entry(1, Task.parse("Write outline")), Entry(2, Task.parse("Submit")))

    // [utest->dsn~edit-dependencies~1]
    @Test
    fun waitingAddsTheTagsAndAMissingIdGetsTheNextNumber() {
        val (task, changed) = withDependencies(Task.parse("Write abstract"), others, waits = setOf(1), before = setOf(0, 2))
        assertEquals("Write abstract p:3 id:4 p:5", task.toLine())
        assertEquals(listOf(Entry(1, Task.parse("Write outline p:4")), Entry(2, Task.parse("Submit id:5"))), changed)
        assertEquals(listOf(1), waitsFor(task, others + changed).map { it.index }.distinct())
        assertEquals(listOf(0, 2), waitingFor(task, changed + others[0]).map { it.index }.sorted())
    }

    // [utest->dsn~edit-dependencies~1]
    @Test
    fun removingADependencyRemovesOnlyItsTag() {
        val all = listOf(Entry(0, Task.parse("Collect sources id:3")), Entry(1, Task.parse("Write outline p:4")))
        val (task, changed) = withDependencies(Task.parse("Write abstract id:4 p:3"), all, waits = emptySet(), before = setOf(0))
        assertEquals("Write abstract id:4 p:3", task.toLine())
        assertEquals(listOf(Entry(1, Task.parse("Write outline"))), changed)
    }
}
