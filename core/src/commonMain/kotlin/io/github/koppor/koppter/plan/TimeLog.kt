package io.github.koppor.koppter.plan

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

/**
 * The times worked as a file next to todo.txt: one line per time, such as `2026-11-21 16:50-17:20 Write the report +Work`.
 * The time away that still goes on is a line without end, such as `2026-11-21 16:50- Away`.
 * Lines are appended at the end, so that the changes of two devices merge line by line; only the line of the time away that goes on is replaced when it ends.
 */
// [impl->dsn~time-log~1]
object TimeLog {
    const val NAME = "timelog.txt"
    const val AWAY = "Away"

    private val OPEN_AWAY = Regex("^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2})- $AWAY$")

    private fun time(at: LocalDateTime) = at.time.toString().take(5)

    /** The log with the time from [from] to [to] added, as one line per day; a time that does not go forward adds nothing. */
    fun append(log: String, from: LocalDateTime, to: LocalDateTime, text: String): String {
        val lines = mutableListOf<String>()
        var start = from
        while (start < to) {
            val midnight = start.date.plus(DatePeriod(days = 1)).atTime(0, 0)
            val end = minOf(to, midnight)
            // A time that ends at midnight is written as 24:00, so that it stays on its day.
            lines += "${start.date} ${time(start)}-${if (end == midnight) "24:00" else time(end)} $text"
            start = end
        }
        return lines.fold(log, ::withLine)
    }

    private fun withLine(log: String, line: String) = log + (if (log.isEmpty() || log.endsWith("\n")) "" else "\n") + line + "\n"

    /** Since when the user is away; `null` if they are not. */
    fun awaySince(log: String): LocalDateTime? = log.lines().asReversed().firstNotNullOfOrNull { line ->
        OPEN_AWAY.matchEntire(line.trim())?.destructured?.let { (date, time) -> LocalDateTime(LocalDate.parse(date), LocalTime.parse(time)) }
    }

    /** The log with an open time away from [now]. */
    fun goAway(log: String, now: LocalDateTime): String = withLine(back(log, now), "${now.date} ${time(now)}- $AWAY")

    /** The log with the open time away ended at [now]. */
    fun back(log: String, now: LocalDateTime): String {
        val since = awaySince(log) ?: return log
        val kept = log.lines().filterNot { OPEN_AWAY.matches(it.trim()) }.joinToString("\n").trimEnd('\n')
        return append(kept, since, now, AWAY)
    }
}
