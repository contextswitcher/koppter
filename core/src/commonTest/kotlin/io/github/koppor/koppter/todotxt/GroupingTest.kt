package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupingTest {
    private val today = LocalDate(2026, 11, 12)
    private val entries = TodoFile.parse(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale +Family
        (A) Thank Mom @phone due:2026-11-20
        Pay bill due:2026-11-12
        Return book due:2026-11-01
        """.trimIndent(),
    ).entries

    private fun groups(key: GroupKey) =
        groupEntries(entries, key, today).associate { group -> group.title to group.entries.map { it.task.description } }

    // [utest->dsn~list-groups~1]
    @Test
    fun aListWithoutGroupsHasThePartsThatHaveTasks() {
        fun parts(text: String) = listParts(TodoFile.parse(text).entries, today).associate { part -> part.title to part.entries.map { it.task.description } }
        assertEquals(
            mapOf("Overdue" to listOf("Return book"), "Next" to listOf("Pay bill", "Post signs"), "Done" to listOf("Call Mom")),
            parts("Pay bill due:2026-11-12\nReturn book due:2026-11-01\nx Call Mom due:2026-11-01\nPost signs"),
        )
        assertEquals(mapOf("Next" to listOf("Post signs")), parts("Post signs"))
        assertEquals(emptyMap(), parts(""))
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun groupsByPriorityWithUnprioritizedTasksLast() {
        assertEquals(
            mapOf("A" to listOf("Thank Mom"), "B" to listOf("Schedule pickup"), "No priority" to listOf("Post signs", "Pay bill", "Return book")),
            groups(GroupKey.PRIORITY),
        )
        assertEquals(listOf("A", "B", "No priority"), groupEntries(entries, GroupKey.PRIORITY, today).map { it.title })
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun aTaskWithTwoProjectsAppearsInBothGroups() {
        val groups = groups(GroupKey.PROJECT)
        assertEquals(listOf("Post signs"), groups["+Family"])
        assertEquals(listOf("Schedule pickup", "Post signs"), groups["+GarageSale"])
        assertEquals(listOf("Thank Mom", "Pay bill", "Return book"), groups["No project"])
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun groupsByContext() {
        assertEquals(listOf("Schedule pickup", "Thank Mom"), groups(GroupKey.CONTEXT)["@phone"])
    }

    // [utest->dsn~board-groups~1]
    // [utest->dsn~due-groups~1]
    @Test
    fun groupsByDueDateRelativeToToday() {
        assertEquals(
            mapOf(
                "Overdue" to listOf("Return book"),
                "Today" to listOf("Pay bill"),
                "Next weekend" to listOf("Thank Mom"),
                "No due date" to listOf("Schedule pickup", "Post signs"),
            ),
            groups(GroupKey.DUE),
        )
        // 2026-11-12 is a Thursday: a group has the days after the group before it, up to the day it is named after.
        val days = listOf("11-13", "11-14", "11-15", "11-16", "11-22", "11-23", "11-26", "11-27", "12-12", "12-13", "2027-03-01")
        val dated = TodoFile.parse(days.joinToString("\n") { "Water the plants due:${if (it.length == 5) "2026-$it" else it}" }).entries
        assertEquals(
            listOf("Tomorrow" to 1, "This weekend" to 2, "Next weekend" to 2, "In two weeks" to 2, "In a month" to 2, "Later" to 2),
            groupEntries(dated, GroupKey.DUE, today).map { it.title to it.entries.size },
        )
        // On a Monday, three days from now come before the weekend.
        assertEquals(
            listOf("In three days", "This weekend"),
            groupEntries(TodoFile.parse("Water the plants due:2026-11-18\nMow the lawn due:2026-11-21").entries, GroupKey.DUE, LocalDate(2026, 11, 16)).map { it.title },
        )
    }

    // [utest->dsn~board-groups~1]
    @Test
    fun emptyGroupsAreLeftOut() {
        assertEquals(listOf("No context"), groupEntries(entries.filter { it.task.contexts.isEmpty() }, GroupKey.CONTEXT, today).map { it.title })
    }

    private fun move(line: String, key: GroupKey, from: String, to: String): String? {
        val task = Task.parse(line)
        val groups = groupEntries(entries + Entry(99, task), key, today)
        return moveToGroup(task, key, groups.first { it.title == from }, groups.first { it.title == to }, today)?.toLine()
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenPriorityColumnsSetsThePriority() {
        assertEquals("(A) Call Mom", move("(B) Call Mom", GroupKey.PRIORITY, "B", "A"))
        assertEquals("Call Mom", move("(B) Call Mom", GroupKey.PRIORITY, "B", "No priority"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenProjectColumnsReplacesTheProjectOfTheSourceColumn() {
        assertEquals("Call Mom +Other +Family", move("Call Mom +GarageSale +Other", GroupKey.PROJECT, "+GarageSale", "+Family"))
        assertEquals("Call Mom", move("Call Mom +GarageSale", GroupKey.PROJECT, "+GarageSale", "No project"))
        assertEquals("Call Mom @phone", move("Call Mom", GroupKey.CONTEXT, "No context", "@phone"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movingBetweenDueDateColumnsSetsOrRemovesTheDueDate() {
        assertEquals("Call Mom due:2026-11-12", move("Call Mom", GroupKey.DUE, "No due date", "Today"))
        // A column sets the day it is named after, and the last one the day after the month.
        assertEquals("Call Mom due:2026-11-22", move("Call Mom due:2026-11-12", GroupKey.DUE, "Today", "Next weekend"))
        val groups = groupEntries(TodoFile.parse("Call Mom due:2026-11-12\nRenew the passport due:2027-03-01").entries, GroupKey.DUE, today)
        assertEquals(listOf("Today", "Later"), groups.map { it.title })
        assertEquals("Call Mom due:2026-12-13", moveToGroup(groups[0].entries[0].task, GroupKey.DUE, groups[0], groups[1], today)?.toLine())
        assertEquals("Call Mom", move("Call Mom due:2026-11-12", GroupKey.DUE, "Today", "No due date"))
    }

    // [utest->dsn~board-move~1]
    @Test
    fun movesWithoutMeaningAreRefused() {
        assertEquals(null, move("(B) Call Mom", GroupKey.PRIORITY, "B", "B"))
        assertEquals(null, move("Call Mom", GroupKey.DUE, "No due date", "Overdue"))
    }

    // [utest->dsn~board-groups~1]
    // [utest->dsn~board-move~1]
    @Test
    fun hashtagsFormColumnsAndAMoveReplacesTheHashtagOfTheSourceColumn() {
        val tagged = TodoFile.parse("Call Mom #todo #family\nPay bill #doing\nPost signs").entries
        val groups = groupEntries(tagged, GroupKey.HASHTAG, today)

        assertEquals(listOf("#doing", "#family", "#todo", "No hashtag"), groups.map { it.title })
        val moved = moveToGroup(tagged[0].task, GroupKey.HASHTAG, groups.first { it.title == "#todo" }, groups.first { it.title == "#doing" }, today)
        assertEquals("Call Mom #family #doing", moved?.toLine())
    }

    // [utest->dsn~deadline-menu~1]
    @Test
    fun theDeadlineMenuOffersWeeksAndTheQuarterAfterTheNext() {
        assertEquals(
            listOf(
                "in 2 weeks" to LocalDate(2026, 12, 8),
                "in 4 weeks" to LocalDate(2026, 12, 22),
                "in 8 weeks" to LocalDate(2027, 1, 19),
                "Q2 2027" to LocalDate(2027, 6, 30),
                "Q3 2027" to LocalDate(2027, 9, 30),
                "Q4 2027" to LocalDate(2027, 12, 31),
            ),
            deadlineChoices(LocalDate(2026, 11, 24)),
        )
        // In the first quarter, the quarters reach into the next year.
        assertEquals(listOf("Q3 2027", "Q4 2027", "Q1 2028"), deadlineChoices(LocalDate(2027, 3, 31)).drop(3).map { it.first })
        assertEquals(LocalDate(2028, 3, 31), deadlineChoices(LocalDate(2027, 3, 31)).last().second)
    }
}
