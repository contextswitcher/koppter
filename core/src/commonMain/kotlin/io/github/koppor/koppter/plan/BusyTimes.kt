package io.github.koppor.koppter.plan

import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/**
 * The times that the events of an iCalendar file take, by day, in the given time zone.
 *
 * Counted are events with a start time that are neither canceled nor marked as free.
 * Events that last whole days are left out, as are the blocks that [Ics] wrote itself and the [sidelined] ones,
 * by a word of their title or by their UID in [sidelinedIds].
 * Repeating events are followed for the frequencies daily, weekly, monthly and yearly with interval, end, count,
 * days of the week, days of the month and excluded dates.
 */
// ponytail: a moved occurrence of a repeating event counts at its old and its new time, and a time zone that the platform
// does not know by its name, such as those Outlook writes, is taken as the given zone. A library for iCalendar if that shows.
// [impl->dsn~busy-times~1]
fun busyTimes(ics: String, from: LocalDate, to: LocalDate, zone: TimeZone, sideline: List<String> = emptyList(), sidelinedIds: Set<String> = emptySet()): Map<LocalDate, List<Span>> =
    events(ics, from, to, zone, sideline, sidelinedIds).groupBy({ it.date }) { it.span }.mapValues { it.value.joined() }

/** The events that [busyTimes] counts, each part of a day with the title of its event. */
// [impl->dsn~absence-titles~1]
fun events(ics: String, from: LocalDate, to: LocalDate, zone: TimeZone, sideline: List<String> = emptyList(), sidelinedIds: Set<String> = emptySet()): List<TextBlock> =
    icsEvents(ics, from, to, zone)
        // A repeating event is one entry with one title and one UID, so all its occurrences are sidelined together.
        // [impl->dsn~sideline~1]
        // [impl->dsn~event-sideline~1]
        .filterNot { sidelined(it.block.text, sideline) || (it.uid.isNotEmpty() && it.uid in sidelinedIds) }
        .map { it.block }

/**
 * A part of a day that an event of an iCalendar file takes: [uid] names the event, for a repeating one all its occurrences,
 * and [recurring] tells whether it repeats.
 */
data class IcsEvent(val uid: String, val recurring: Boolean, val block: TextBlock)

/** The events that take time from [from] to [to], as [busyTimes] counts them but with those on the sideline. */
// [impl->dsn~event-sideline~1]
fun icsEvents(ics: String, from: LocalDate, to: LocalDate, zone: TimeZone): List<IcsEvent> {
    val result = mutableListOf<IcsEvent>()
    val lines = unfolded(ics)
    var event: MutableMap<String, Pair<String, String>>? = null
    for (line in lines) {
        when {
            line == "BEGIN:VEVENT" -> event = mutableMapOf()
            line == "END:VEVENT" -> {
                event?.let { addEvent(it, from, to, zone, result) }
                event = null
            }
            event != null -> {
                val name = line.substringBefore(':').substringBefore(';').uppercase()
                // The first one counts; EXDATE may occur several times.
                val value = line.substringBefore(':').substringAfter(';', "") to line.substringAfter(':')
                if (name == "EXDATE") event[name] = "" to listOfNotNull(event[name]?.second, value.second).joinToString(",") else event.getOrPut(name) { value }
            }
        }
    }
    return result
}

/** The name that the iCalendar file gives its calendar in `X-WR-CALNAME`, as Google Calendar and Outlook write it; `null` without one. */
// [impl->dsn~calendar-events~1]
fun calendarName(ics: String): String? =
    unfolded(ics).takeWhile { it != "BEGIN:VEVENT" }
        .firstOrNull { it.substringBefore(':').substringBefore(';').equals("X-WR-CALNAME", ignoreCase = true) }
        ?.substringAfter(':')?.replace(Regex("\\\\(.)"), "$1")?.trim()?.takeIf(String::isNotEmpty)

/** The lines of an iCalendar file, where a line that starts with a blank continues the line before it. */
private fun unfolded(ics: String): List<String> = ics.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "").lines()

private val DATE_TIME = Regex("^(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})(Z?)$")

/** The local date and time in [zone] of a value such as `20261116T090000`, with `Z` for UTC or the zone that the parameters name. */
private fun dateTime(parameters: String, value: String, zone: TimeZone): LocalDateTime? {
    val (year, month, day, hour, minute, _, utc) = DATE_TIME.matchEntire(value.trim())?.destructured ?: return null
    val local = try {
        LocalDateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
    } catch (_: IllegalArgumentException) {
        return null
    }
    val source = when {
        utc.isNotEmpty() -> TimeZone.UTC
        else -> Regex("TZID=\"?([^;\"]+)").find(parameters)?.groupValues?.get(1)?.let {
            try {
                TimeZone.of(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        } ?: zone
    }
    return if (source == zone) local else local.toInstant(source).toLocalDateTime(zone)
}

/**
 * Whether an event with the title stays in the calendar but leaves its time to planning:
 * the title contains one of the words of [sideline], ignoring case.
 */
// [impl->dsn~sideline~1]
fun sidelined(title: String, sideline: List<String>): Boolean = sideline.any { it.isNotBlank() && title.contains(it.trim(), ignoreCase = true) }

private fun addEvent(event: Map<String, Pair<String, String>>, from: LocalDate, to: LocalDate, zone: TimeZone, result: MutableList<IcsEvent>) {
    fun value(name: String) = event[name]?.second?.trim()
    if (value("STATUS") == "CANCELLED" || value("TRANSP") == "TRANSPARENT" || value("X-MICROSOFT-CDO-BUSYSTATUS") == "FREE") return
    if (value("UID").orEmpty().endsWith("@koppter")) return
    val start = event["DTSTART"]?.let { dateTime(it.first, it.second, zone) } ?: return
    val minutes = event["DTEND"]?.let { dateTime(it.first, it.second, zone) }?.let { end ->
        start.date.daysUntil(end.date) * MINUTES_PER_DAY + end.time.toSecondOfDay() / 60 - start.time.toSecondOfDay() / 60
    } ?: value("DURATION")?.let { Duration.parseIsoStringOrNull(it)?.inWholeMinutes?.toInt() } ?: return
    if (minutes <= 0) return
    // A title is one line without the escapes of iCalendar.
    val title = value("SUMMARY").orEmpty().replace(Regex("\\\\[nN]"), " ").replace(Regex("\\\\(.)"), "$1").trim()
    val uid = value("UID").orEmpty()
    val rule = value("RRULE")?.split(';')?.associate { it.substringBefore('=').uppercase() to it.substringAfter('=').uppercase() }

    fun add(date: LocalDate) {
        // An event that runs over midnight takes time of the following days.
        var day = date
        var first = start.time.toSecondOfDay() / 60
        var left = minutes
        while (left > 0 && day <= to) {
            val length = minOf(left, MINUTES_PER_DAY - first)
            if (day >= from) result += IcsEvent(uid, rule != null, TextBlock(day, Span(first, first + length), title))
            left -= length
            first = 0
            day = day.plus(DatePeriod(days = 1))
        }
    }

    if (rule == null) {
        add(start.date)
        return
    }
    val interval = rule["INTERVAL"]?.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val until = rule["UNTIL"]?.let { Task.parseDate("${it.take(4)}-${it.substring(4, 6)}-${it.substring(6, 8)}") }
    val count = rule["COUNT"]?.toIntOrNull()
    val byDay = rule["BYDAY"]?.split(',')?.mapNotNull { DAY.matchEntire(it) }?.map { it.groupValues[1].toIntOrNull() to WEEKDAYS.indexOf(it.groupValues[2]) + 1 }
    val byMonthDay = rule["BYMONTHDAY"]?.split(',')?.mapNotNull { it.toIntOrNull() }
    val excluded = value("EXDATE").orEmpty().split(',').mapNotNull { dateTime("", it, zone)?.date }.toSet()

    fun occursOn(date: LocalDate): Boolean = when (rule["FREQ"]) {
        "DAILY" -> start.date.daysUntil(date) % interval == 0
        "WEEKLY" -> {
            val weeks = start.date.minus(DatePeriod(days = start.date.dayOfWeek.isoDayNumber - 1)).daysUntil(date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))) / 7
            weeks % interval == 0 && (byDay?.any { it.second == date.dayOfWeek.isoDayNumber } ?: (date.dayOfWeek == start.date.dayOfWeek))
        }
        "MONTHLY" -> {
            val months = (date.year - start.date.year) * 12 + date.month.ordinal - start.date.month.ordinal
            val fromEnd = date.day - (LocalDate(date.year, date.month, 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day + 1)
            months % interval == 0 && when {
                byMonthDay != null -> date.day in byMonthDay || fromEnd in byMonthDay
                // 2TU is the second Tuesday, -1FR the last Friday of the month.
                byDay != null -> byDay.any { (ordinal, day) ->
                    day == date.dayOfWeek.isoDayNumber && (ordinal == null || ordinal == (date.day - 1) / 7 + 1 || ordinal == (fromEnd + 1) / 7 - 1)
                }
                else -> date.day == start.date.day
            }
        }
        "YEARLY" -> (date.year - start.date.year) % interval == 0 && date.month == start.date.month && date.day == start.date.day
        else -> false
    }

    // ponytail: walks every day from the first occurrence; compute the occurrences from the rule if calendars get slow.
    var date = start.date
    var occurrences = 0
    val last = listOfNotNull(to, until).min()
    while (date <= last && (count == null || occurrences < count)) {
        if (occursOn(date)) {
            occurrences++
            if (date !in excluded && date.plus(DatePeriod(days = minutes / MINUTES_PER_DAY + 1)) >= from) add(date)
        }
        date = date.plus(DatePeriod(days = 1))
    }
}

private val WEEKDAYS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
private val DAY = Regex("^([+-]?\\d+)?(MO|TU|WE|TH|FR|SA|SU)$")
