package io.github.koppor.koppter.todotxt

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// [utest->dsn~expiry~1]
class ExpiryTest {
    private val today = LocalDate(2026, 11, 26)

    private fun file(vararg lines: String) = TodoFile(lines.toList())

    @Test
    fun anOpenTaskIsRemovedAfterItsLastDay() {
        val expired = expire(file("(A) Put out the bins @home exp:2026-11-25", "", "Water the plants"), today)!!
        assertEquals(listOf("", "Water the plants"), expired.file.lines)
        assertEquals(listOf("Put out the bins"), expired.removed.map { it.description })
    }

    @Test
    fun aTaskStaysOnItsLastDay() {
        assertNull(expire(file("Put out the bins exp:2026-11-26"), today))
    }

    @Test
    fun completedTasksAndTasksWithoutTheTagStay() {
        assertNull(expire(file("x 2026-11-24 Put out the bins exp:2026-11-25", "Water the plants due:2026-11-01"), today))
    }

    @Test
    fun tasksOfOutlookAndLatexStay() {
        assertNull(expire(file("Answer the mail #outlook ol:AAAA exp:2026-11-01", "Fix the figure #latex tex:1x2 exp:2026-11-01"), today))
    }

    @Test
    fun aTaskWithoutDueDateIsDueOnItsLastDay() {
        assertEquals(LocalDate(2026, 11, 25), Task.parse("Put out the bins exp:2026-11-25").due)
        assertEquals(LocalDate(2026, 11, 24), Task.parse("Put out the bins due:2026-11-24 exp:2026-11-25").due)
    }

    @Test
    fun theNextOccurrenceMovesTheLastDayAsFarAsTheDueDate() {
        val next = Task.parse("Put out the bins exp:2026-11-25 rec:+2w").nextOccurrence(today)!!
        assertEquals("Put out the bins exp:2026-12-09 rec:+2w", next.toLine())
        val withDue = Task.parse("Put out the bins due:2026-11-24 exp:2026-11-25 rec:+2w").nextOccurrence(today)!!
        assertEquals("Put out the bins due:2026-12-08 exp:2026-12-09 rec:+2w", withDue.toLine())
    }
}
