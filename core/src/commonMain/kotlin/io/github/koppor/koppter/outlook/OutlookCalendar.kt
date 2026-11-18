package io.github.koppor.koppter.outlook

import io.github.koppor.koppter.plan.Block
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.Span
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** A block of the plan as an appointment of Outlook; AnchorHub marks it as tentative, private and koppter's. */
data class OutlookBlock(val id: String, val start: LocalDateTime, val end: LocalDateTime, val subject: String, val body: String)

/** How Outlook shows an appointment: as free, tentative, busy, out of office or working elsewhere. */
enum class BusyStatus { FREE, TENTATIVE, BUSY, OOF, ELSEWHERE }

/** An appointment of Outlook as far as planning needs it. */
data class OutlookBusy(val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean, val status: BusyStatus)

// [impl->dsn~outlook-blocks~1]
object OutlookCalendar {
    /** What the subject and the text of a block of koppter carry, so that the user sees whose block it is. */
    const val MARK = "[koppter]"

    /** The window into which the blocks are written: from [now] to the end of the day before today plus [weeks] weeks. */
    fun window(now: LocalDateTime, weeks: Int): ClosedRange<LocalDateTime> =
        now..LocalDateTime(now.date.plus(DatePeriod(days = 7 * weeks)), LocalTime(0, 0))

    /** The blocks of the plan that start in the window, as appointments; the subject ends with [MARK]. */
    fun blocks(plan: List<Block>, window: ClosedRange<LocalDateTime>): List<OutlookBlock> =
        plan.map { block ->
            OutlookBlock(
                Ics.id(block),
                at(block.date, block.span.start),
                at(block.date, block.span.end),
                "${block.text} $MARK",
                "$MARK\nPlanned by koppter. The next plan replaces this appointment.",
            )
        }.filter { it.start >= window.start && it.start < window.endInclusive }

    /**
     * The times that the appointments take, by day: those that are not free and have a start time.
     * Whole-day appointments are left out, as [io.github.koppor.koppter.plan.busyTimes] does for an iCalendar file.
     * The blocks of koppter are not among them: AnchorHub leaves them out.
     */
    fun busyTimes(appointments: List<OutlookBusy>): Map<LocalDate, List<Span>> {
        val result = mutableMapOf<LocalDate, MutableList<Span>>()
        for (appointment in appointments) {
            if (appointment.allDay || appointment.status == BusyStatus.FREE || appointment.end <= appointment.start) continue
            var date = appointment.start.date
            while (date <= appointment.end.date) {
                val start = if (date == appointment.start.date) minuteOf(appointment.start) else 0
                val end = if (date == appointment.end.date) minuteOf(appointment.end) else 24 * 60
                if (end > start) result.getOrPut(date) { mutableListOf() } += Span(start, end)
                date = date.plus(DatePeriod(days = 1))
            }
        }
        return result
    }

    private fun at(date: LocalDate, minute: Int): LocalDateTime =
        if (minute >= 24 * 60) LocalDateTime(date.plus(DatePeriod(days = 1)), LocalTime(0, 0)) else LocalDateTime(date, LocalTime(minute / 60, minute % 60))

    private fun minuteOf(time: LocalDateTime) = time.hour * 60 + time.minute
}
