package io.github.koppor.koppter.outlook

import io.github.koppor.koppter.plan.Block
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.plan.sidelined
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/** A block of the plan as an appointment of Outlook; AnchorHub marks it as tentative, private and koppter's. */
data class OutlookBlock(val id: String, val start: LocalDateTime, val end: LocalDateTime, val subject: String, val body: String)

/** How Outlook shows an appointment: as free, tentative, busy, out of office or working elsewhere. */
enum class BusyStatus { FREE, TENTATIVE, BUSY, OOF, ELSEWHERE }

/**
 * An appointment of Outlook as planning and the calendar views need it.
 * [entryId] names the appointment, for a recurring one its series; [sideline] is set when it carries the category `Sideline` in Outlook;
 * [color] is that of its first category in Outlook as `#RRGGBB`, `null` without one.
 */
data class OutlookBusy(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val status: BusyStatus,
    val subject: String = "",
    val entryId: String = "",
    val recurring: Boolean = false,
    val sideline: Boolean = false,
    val color: String? = null,
)

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
     * The blocks of koppter are not among them: AnchorHub leaves them out. Nor are the [sidelined] ones, by a word of their subject or by the category of Outlook.
     */
    fun busyTimes(appointments: List<OutlookBusy>, sideline: List<String> = emptyList()): Map<LocalDate, List<Span>> {
        val result = mutableMapOf<LocalDate, MutableList<Span>>()
        for (appointment in appointments) {
            if (appointment.allDay || appointment.status == BusyStatus.FREE || appointment.end <= appointment.start) continue
            // [impl->dsn~sideline~1]
            // [impl->dsn~outlook-sideline~1]
            if (isSidelined(appointment, sideline)) continue
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

    /** Whether the appointment stays in the calendar and leaves its time to planning: by the category `Sideline` or by a word of its subject. */
    // [impl->dsn~outlook-sideline~1]
    fun isSidelined(appointment: OutlookBusy, sideline: List<String>): Boolean = appointment.sideline || sidelined(appointment.subject, sideline)

    /** The appointments with a start time that take time on the day, each with its span on that day; one that goes past midnight is cut there. */
    // [impl->dsn~outlook-appointments~1]
    fun onDay(appointments: List<OutlookBusy>, day: LocalDate): List<Pair<OutlookBusy, Span>> =
        appointments.filter { !it.allDay && it.start.date <= day && it.end.date >= day && it.end > it.start }.mapNotNull { appointment ->
            val start = if (appointment.start.date == day) minuteOf(appointment.start) else 0
            val end = if (appointment.end.date == day) minuteOf(appointment.end) else 24 * 60
            (appointment to Span(start, end)).takeIf { end > start }
        }.sortedBy { it.second.start }

    /** The color of an appointment as red, green and blue from 0 to 255; `null` without one or for a text that is no `#RRGGBB`. */
    fun rgb(color: String?): Triple<Int, Int, Int>? =
        color?.takeIf(RGB::matches)?.let { Triple(it.substring(1, 3).toInt(16), it.substring(3, 5).toInt(16), it.substring(5, 7).toInt(16)) }

    private val RGB = Regex("^#[0-9A-Fa-f]{6}$")

    private fun at(date: LocalDate, minute: Int): LocalDateTime =
        if (minute >= 24 * 60) LocalDateTime(date.plus(DatePeriod(days = 1)), LocalTime(0, 0)) else LocalDateTime(date, LocalTime(minute / 60, minute % 60))

    private fun minuteOf(time: LocalDateTime) = time.hour * 60 + time.minute
}
