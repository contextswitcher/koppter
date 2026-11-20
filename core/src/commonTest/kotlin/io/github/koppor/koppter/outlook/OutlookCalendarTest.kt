package io.github.koppor.koppter.outlook

import io.github.koppor.koppter.plan.Block
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutlookCalendarTest {
    private val today = LocalDate(2026, 11, 18)
    private fun block(date: LocalDate, start: Int, end: Int, text: String) = Block(date, Span(start, end), Entry(0, Task(body = text)))

    // [utest->dsn~outlook-blocks~1]
    @Test
    fun theBlocksInTheWindowBecomeAppointmentsMarkedAsKoppters() {
        val now = LocalDateTime(2026, 11, 18, 10, 0)
        val window = OutlookCalendar.window(now, 3)
        assertEquals(LocalDateTime(2026, 12, 9, 0, 0), window.endInclusive)
        val plan = listOf(
            block(today, 9 * 60, 10 * 60, "Already started"),
            block(today, 14 * 60, 15 * 60 + 30, "Write the review"),
            block(LocalDate(2026, 12, 8), 23 * 60, 24 * 60, "Last day"),
            block(LocalDate(2026, 12, 9), 9 * 60, 10 * 60, "Beyond the window"),
        )
        val blocks = OutlookCalendar.blocks(plan, window)
        assertEquals(listOf("Write the review [koppter]", "Last day [koppter]"), blocks.map { it.subject })
        assertEquals(LocalDateTime(2026, 11, 18, 14, 0), blocks[0].start)
        assertEquals(LocalDateTime(2026, 11, 18, 15, 30), blocks[0].end)
        assertEquals(LocalDateTime(2026, 12, 9, 0, 0), blocks[1].end)
        assertEquals(Ics.id(plan[1]), blocks[0].id)
        assertTrue(blocks.all { it.body.startsWith(OutlookCalendar.MARK) })
        assertEquals(emptyList(), OutlookCalendar.blocks(plan, OutlookCalendar.window(now, 0)))
    }

    // [utest->dsn~outlook-sideline~1]
    // [utest->dsn~outlook-appointments~1]
    @Test
    fun anAppointmentOnTheSidelineStaysInTheDayButNotInTheBusyTimes() {
        val sidelined = OutlookBusy(LocalDateTime(2026, 11, 18, 9, 0), LocalDateTime(2026, 11, 18, 10, 0), false, BusyStatus.BUSY, "Office hours", "AA", sideline = true)
        val byWord = OutlookBusy(LocalDateTime(2026, 11, 18, 11, 0), LocalDateTime(2026, 11, 18, 12, 0), false, BusyStatus.BUSY, "Optional: talk", "BB")
        val night = OutlookBusy(LocalDateTime(2026, 11, 17, 23, 0), LocalDateTime(2026, 11, 18, 1, 0), false, BusyStatus.BUSY, "Flight", "CC")
        assertEquals(mapOf(LocalDate(2026, 11, 17) to listOf(Span(1380, 1440)), today to listOf(Span(0, 60))), OutlookCalendar.busyTimes(listOf(sidelined, byWord, night), listOf("optional")))
        assertTrue(OutlookCalendar.isSidelined(sidelined, emptyList()))
        assertTrue(OutlookCalendar.isSidelined(byWord, listOf("optional")))
        assertEquals(listOf(Span(0, 60), Span(540, 600), Span(660, 720)), OutlookCalendar.onDay(listOf(sidelined, byWord, night), today).map { it.second })
        assertEquals(Triple(157, 183, 232), OutlookCalendar.rgb("#9DB7E8"))
        assertEquals(null, OutlookCalendar.rgb("blue"))
    }

    // [utest->dsn~outlook-busy~1]
    @Test
    fun theAppointmentsThatAreNotFreeAreBusyTimesByDay() {
        val busy = OutlookCalendar.busyTimes(
            listOf(
                OutlookBusy(LocalDateTime(2026, 11, 18, 9, 0), LocalDateTime(2026, 11, 18, 10, 30), false, BusyStatus.BUSY),
                OutlookBusy(LocalDateTime(2026, 11, 18, 12, 0), LocalDateTime(2026, 11, 18, 13, 0), false, BusyStatus.FREE),
                OutlookBusy(LocalDateTime(2026, 11, 18, 0, 0), LocalDateTime(2026, 11, 19, 0, 0), true, BusyStatus.OOF),
                OutlookBusy(LocalDateTime(2026, 11, 18, 22, 0), LocalDateTime(2026, 11, 19, 1, 0), false, BusyStatus.TENTATIVE),
            ),
        )
        assertEquals(mapOf(today to listOf(Span(540, 630), Span(1320, 1440)), LocalDate(2026, 11, 19) to listOf(Span(0, 60))), busy)
    }
}
