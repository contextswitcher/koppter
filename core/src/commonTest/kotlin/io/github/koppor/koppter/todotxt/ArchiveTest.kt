package io.github.koppor.koppter.todotxt

import io.github.koppor.koppter.sync.TaskDates
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// [utest->dsn~archive~1]
class ArchiveTest {
    private val today = LocalDate(2026, 12, 1)

    private fun file(vararg lines: String) = TodoFile(lines.toList())

    @Test
    fun tasksCompletedBeforeTheLastMonthAreArchived() {
        val archived = archive(file("x 2026-10-31 Call Mom", "x 2026-11-01 Buy milk", "Water the plants"), emptyMap(), today)!!
        assertEquals(listOf("x 2026-11-01 Buy milk", "Water the plants"), archived.file.lines)
        assertEquals(listOf("x 2026-10-31 Call Mom"), archived.done)
    }

    @Test
    fun nothingIsArchivedDuringTheMonth() {
        val file = file("x 2026-11-01 Buy milk")
        assertNull(archive(file, emptyMap(), LocalDate(2026, 12, 31)))
        assertEquals(listOf("x 2026-11-01 Buy milk"), archive(file, emptyMap(), LocalDate(2027, 1, 1))!!.done)
    }

    @Test
    fun theDatesOfTheHistoryAreWrittenIntoTheArchivedLine() {
        val dates = mapOf("x Call Mom pri:A" to TaskDates(added = LocalDate(2026, 9, 1), completed = LocalDate(2026, 10, 5)))
        val archived = archive(file("x Call Mom pri:A", "x Buy milk"), dates, today)!!
        assertEquals(listOf("x 2026-10-05 2026-09-01 Call Mom pri:A"), archived.done)
        // The day on which the other task was completed is not known.
        assertEquals(listOf("x Buy milk"), archived.file.lines)
    }

    @Test
    fun tasksThatStillHaveAUseStay() {
        val file = file(
            "x 2026-10-01 Reply to the mail ol:AAAA",
            "x 2026-10-01 Cite the survey tex:abc",
            "x 2026-10-01 Book the room id:1",
            "Invite the guests p:1",
            "x 2026-10-01 Tidy the room @anna",
        )
        assertNull(archive(file, emptyMap(), today) { "anna" in it.contexts })
    }
}
