package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FamilyTest {
    private val members = listOf("anna", "ben", "carl")

    // A Saturday, in week 47 and in the fourth quarter.
    private val today = LocalDate(2026, 11, 21)

    private fun entries(vararg lines: String) = TodoFile(lines.toList()).entries

    // [utest->dsn~stars~1]
    @Test
    fun aTaskIsWorthOneStarUnlessItsTagSaysOtherwise() {
        assertEquals(1, Task.parse("Feed the cat @anna").stars)
        assertEquals(3, Task.parse("Mow the lawn stars:3 @anna").stars)
        assertEquals(0, Task.parse("Sleep stars:0").stars)
        assertEquals(1, Task.parse("Sleep stars:many").stars)
        assertEquals(1, Task.parse("Sleep stars:-2").stars)
    }

    // [utest->dsn~stars~1]
    @Test
    fun theNextOccurrenceOfATaskIsWorthTheSameStars() {
        val next = Task.parse("Mow the lawn stars:3 rec:1w due:2026-11-21 @anna").nextOccurrence(today)
        assertEquals(3, next?.stars)
    }

    // [utest->dsn~stars~1]
    @Test
    fun aPeriodStartsWithTheDayTheWeekTheMonthTheQuarterOrTheYear() {
        assertEquals(
            listOf("2026-11-21", "2026-11-16", "2026-11-01", "2026-10-01", "2026-01-01"),
            RankingPeriod.entries.map { it.start(today).toString() },
        )
        assertEquals(LocalDate(2026, 1, 1), RankingPeriod.QUARTER.start(LocalDate(2026, 3, 31)))
        assertEquals(LocalDate(2026, 4, 1), RankingPeriod.QUARTER.start(LocalDate(2026, 4, 1)))
    }

    // [utest->dsn~stars~1]
    @Test
    fun theRankingAddsUpTheStarsOfTheTasksCompletedInThePeriod() {
        val tasks = entries(
            "x 2026-11-21 Feed the cat @anna",
            "x 2026-11-21 Mow the lawn stars:3 @ben",
            "x 2026-11-17 Tidy the room stars:2 @anna",
            "x 2026-11-02 Wash the car stars:5 @carl",
            "x 2025-12-31 Clear the snow stars:9 @carl",
            // Neither an open task, nor one without the day of its completion, nor one of nobody counts.
            "Do the homework stars:7 @anna",
            "x Empty the bin stars:7 @anna",
            "x 2026-11-21 Buy milk stars:7",
        ).map { it.task }
        fun ranking(period: RankingPeriod) = Family.ranking(tasks, members, period, today).joinToString { "${it.place}. ${it.member} ${it.stars}" }

        assertEquals("1. ben 3, 2. anna 1, 3. carl 0", ranking(RankingPeriod.DAY))
        // The same stars share a place, in the order of the members.
        assertEquals("1. anna 3, 1. ben 3, 3. carl 0", ranking(RankingPeriod.WEEK))
        assertEquals("1. carl 5, 2. anna 3, 2. ben 3", ranking(RankingPeriod.MONTH))
        assertEquals("1. carl 5, 2. anna 3, 2. ben 3", ranking(RankingPeriod.YEAR))
    }

    // [utest->dsn~family-day~1]
    @Test
    fun aTaskBelongsToTheFirstMemberWhoseContextItHas() {
        assertEquals("ben", Family.memberOf(Task.parse("Mow the lawn @garden @ben>chores"), members))
        assertNull(Family.memberOf(Task.parse("Buy milk @benjamin"), members))
    }

    // [utest->dsn~family-day~1]
    @Test
    fun theTasksWithATimeOnTheDayAreTheAppointmentsOfTheirMember() {
        val entries = entries(
            "Football due:2026-11-21 at:17:00 dur:90m @ben",
            "Dentist due:2026-11-21 at:08:30 @ben",
            "x 2026-11-21 Swimming due:2026-11-21 at:23:45 dur:1h @ben",
            "Piano due:2026-11-22 at:10:00 @ben",
            "Choir due:2026-11-21 at:19:00 @anna",
        )
        assertEquals(
            listOf("Dentist 510-540", "Football 1020-1110", "Swimming 1425-1440"),
            Family.appointments(entries, "ben", members, today).map { "${it.entry.task.description} ${it.span.start}-${it.span.end}" },
        )
    }

    // [utest->dsn~family-day~1]
    @Test
    fun theTasksOfADayAreThoseDueAndThoseCompletedOnIt() {
        val entries = entries(
            "Feed the cat @anna",
            "Tidy the room due:2026-11-20 @anna",
            "Do the homework due:2026-11-21 @anna",
            "Pack the bag due:2026-11-22 @anna",
            "Buy a present t:2026-11-25 @anna",
            "x 2026-11-21 Empty the bin @anna",
            "x 2026-11-20 Water the plants @anna",
            "Choir due:2026-11-21 at:19:00 @anna",
            "Mow the lawn @ben",
        )
        fun todos(day: LocalDate) = Family.todos(entries, "anna", members, day, today).map { it.task.description }

        // Today also what is overdue and what has no day, the overdue first as in the list; the completed last.
        assertEquals(listOf("Tidy the room", "Do the homework", "Feed the cat", "Empty the bin"), todos(today))
        assertEquals(listOf("Pack the bag"), todos(LocalDate(2026, 11, 22)))
        assertEquals(listOf("Tidy the room", "Water the plants"), todos(LocalDate(2026, 11, 20)))
    }

    // [utest->dsn~absences~1]
    @Test
    fun theCalendarsOfTheMembersAreLinesOfAMemberAndAnAddress() {
        assertEquals(
            listOf("Anna" to "https://example.org/a.ics?key=1", "Ben" to "https://example.org/b.ics", "Anna" to "https://example.org/school.ics"),
            Family.calendars("Anna = https://example.org/a.ics?key=1\n\n@Ben=https://example.org/b.ics\nAnna = https://example.org/school.ics\nno address\nCarl =\n"),
        )
    }

    // [utest->dsn~absences~1]
    @Test
    fun theAbsencesFromTodayOnAreReplacedAndEverythingElseStays() {
        val existing = """
            # Calendar of Anna
            2026-11-20 w47 Fri  08:00-13:00 Absent
            2026-11-21 w47 Sat  Birthday of Grandma. 08:00-09:00 Absent. 10:00-11:00 Swimming
            2026-11-22 w47 Sun  09:00-10:00 Absent
            2026-11-23 w48 Mon
        """.trimIndent() + "\n"
        val absent = mapOf(
            // Two calendars that touch are one absence.
            today to listOf(Span(13 * 60, 15 * 60), Span(9 * 60, 10 * 60), Span(15 * 60, 16 * 60)),
            LocalDate(2026, 11, 24) to listOf(Span(8 * 60, 12 * 60)),
            // What was is not written anew.
            LocalDate(2026, 11, 19) to listOf(Span(8 * 60, 12 * 60)),
        )
        val written = CalendarTxt.withAbsences(existing, absent, today)
        assertEquals(
            """
            # Calendar of Anna
            2026-11-20 w47 Fri  08:00-13:00 Absent
            2026-11-21 w47 Sat  Birthday of Grandma. 09:00-10:00 Absent. 10:00-11:00 Swimming. 13:00-16:00 Absent
            2026-11-24 w48 Tue  08:00-12:00 Absent
            """.trimIndent() + "\n",
            written,
        )
        // The same absences change nothing, so that nothing is committed.
        assertEquals(written, CalendarTxt.withAbsences(written, absent, today))
        assertEquals("", CalendarTxt.withAbsences("", emptyMap(), today))
    }
}
