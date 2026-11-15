package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~overview~1]
class UrgencyTest {
    private val today = LocalDate(2026, 11, 12)

    @Test
    fun theUrgencyTellsTheDaysLeftAndThePartOfTheTimeThatHasPassed() {
        val half = Task.parse("2026-11-02 Vacuum due:2026-11-22").urgency(today)!!
        assertEquals(Urgency(daysLeft = 10, elapsed = 0.5f), half)
        assertEquals("Due in 10 days", half.text)
        assertEquals(UrgencyState.FINE, half.state)

        // The threshold date is the start if the task has one; without a start, the task starts two weeks before it is due.
        assertEquals(0.5f, Task.parse("2026-01-01 Vacuum t:2026-11-10 due:2026-11-14").urgency(today)!!.elapsed)
        assertEquals(0.5f, Task.parse("Vacuum due:2026-11-19").urgency(today)!!.elapsed)

        val overdue = Task.parse("Vacuum due:2026-11-07").urgency(today)!!
        assertEquals(Urgency(daysLeft = -5, elapsed = 1f), overdue)
        assertEquals("5 days overdue", overdue.text)
        assertEquals(UrgencyState.OVERDUE, overdue.state)
        assertEquals("Due today", Task.parse("Vacuum due:2026-11-12").urgency(today)!!.text)

        assertEquals(null, Task.parse("Vacuum").urgency(today))
        assertEquals(null, Task.parse("x Vacuum due:2026-11-07").urgency(today))
    }

    @Test
    fun aContextOfTheOutermostLevelHasTheStateOfItsMostUrgentTask() {
        val file = TodoFile.parse(
            """
            Read the survey @research>reading due:2026-11-30
            Review a paper @research due:2026-11-10
            (A) Plan the experiment @research
            Call the landlord @phone due:2026-11-12
            Water the plants @home
            x Sweep the floor @home due:2026-11-01
            Buy a present due:2026-12-24
            """.trimIndent(),
        )
        val states = overviewStates(file.entries, today)

        assertEquals(listOf("home", "phone", "research", null), states.map { it.name })
        assertEquals(listOf("@home", "@phone", "@research", "No context"), states.map { it.title })
        assertEquals(listOf(UrgencyState.FINE, UrgencyState.DUE, UrgencyState.OVERDUE, UrgencyState.FINE), states.map { it.state })
        // The most urgent task first, a task without due date last.
        assertEquals(listOf("Review a paper", "Read the survey", "Plan the experiment"), states[2].entries.map { it.task.description })
        assertEquals(1, states[2].overdue)
    }

    // [utest->dsn~overview~1]
    @Test
    fun theOverviewHasTilesForProjectsAndHashtagsAsWell() {
        val file = TodoFile.parse(
            """
            Outline the related work +papers>survey #writing due:2026-11-10
            Pick a venue +papers due:2026-11-12
            Print the form +paperwork #quick
            Call Mom
            """.trimIndent(),
        )
        // A project of the outermost level includes those below it, and a name that only starts alike is another one.
        val projects = overviewStates(file.entries, today, GroupKey.PROJECT)
        assertEquals(listOf("+papers", "+paperwork", "No project"), projects.map { it.title })
        assertEquals(listOf(2, 1, 1), projects.map { it.entries.size })
        assertEquals(UrgencyState.OVERDUE, projects[0].state)

        val hashtags = overviewStates(file.entries, today, GroupKey.HASHTAG)
        assertEquals(listOf("#quick", "#writing", "No hashtag"), hashtags.map { it.title })
        assertEquals(listOf(1, 1, 2), hashtags.map { it.entries.size })
    }

    // [utest->dsn~deadline~1]
    @Test
    fun aPassedDueDateIsNoFailureBeforeTheDeadline() {
        val buffered = Task.parse("File the tax return due:2026-11-10 deadline:2026-11-20").urgency(today)!!
        assertEquals(UrgencyState.DUE, buffered.state)
        assertEquals("2 days overdue, deadline in 8 days", buffered.text)
        // A deadline that has passed is a failure, wherever the due date was moved to.
        val missed = Task.parse("File the tax return due:2026-11-20 deadline:2026-11-11").urgency(today)!!
        assertEquals(UrgencyState.OVERDUE, missed.state)
        assertEquals("Due in 8 days, deadline yesterday", missed.text)
        // Without a due date, the deadline is the due date.
        val official = Task.parse("File the tax return deadline:2026-11-20").urgency(today)!!
        assertEquals(UrgencyState.FINE, official.state)
        assertEquals("Due in 8 days", official.text)
        assertEquals(UrgencyState.OVERDUE, Task.parse("File the tax return deadline:2026-11-11").urgency(today)!!.state)
    }
}
