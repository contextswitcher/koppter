package io.github.koppor.koppter.plan

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** A part of a day in minutes from midnight; [end] is exclusive. */
data class Span(val start: Int, val end: Int) {
    val length: Int get() = end - start
}

internal const val MINUTES_PER_DAY = 24 * 60

/** The spans sorted, with overlapping and adjoining ones joined. */
internal fun List<Span>.joined(): List<Span> = filter { it.length > 0 }.sortedBy { it.start }.fold(mutableListOf()) { result, span ->
    val last = result.lastOrNull()
    if (last != null && span.start <= last.end) result[result.lastIndex] = Span(last.start, maxOf(last.end, span.end)) else result += span
    result
}

/** The parts of these spans that lie in none of the [taken] ones. */
internal fun List<Span>.without(taken: List<Span>): List<Span> = taken.joined().fold(joined()) { free, busy ->
    free.flatMap { listOf(Span(it.start, minOf(it.end, busy.start)), Span(maxOf(it.start, busy.end), it.end)) }.filter { it.length > 0 }
}

internal fun List<Span>.within(other: List<Span>): List<Span> = without(listOf(Span(0, MINUTES_PER_DAY)).without(other))

/**
 * How much a time of a time map is wanted for its tasks, as SkedPal paints it.
 * A task is planned in the green times; yellow and then red times are added for a task that would otherwise end late.
 * Black times are green for the tasks of their map and taken for the tasks of all other maps.
 */
// [impl->dsn~time-map-colors~1]
enum class Paint {
    GREEN,
    YELLOW,
    RED,
    BLACK,
    ;

    /** The word in a definition; green is the default and not written. */
    val word: String get() = name.lowercase()

    /** Black is green for the tasks of its own map. */
    internal val rank: Int get() = if (this == BLACK) 0 else ordinal
}

/**
 * The time maps: each names the times of the week in which the tasks that refer to it are planned.
 *
 * A definition is a list of rules separated by commas; a rule is days followed by times, such as `Mon-Fri 08:00-12:00 13:00-17:00`.
 * A rule without days holds on every day, and a rule without times for the whole day.
 * A rule may name its color: `yellow`, `red` or `black`; without, its times are green. A later rule paints over an earlier one.
 * A definition that starts with the name of another map is restricted to the times of that map: `General 08:00-11:00`.
 * A name followed by dates, such as `General 2026-12-24..2027-01-06 2027-04-03`, gives the map on these dates instead of its definition;
 * the later of two such entries wins, and `none` as definition leaves no time on these dates.
 */
// [impl->dsn~time-maps~1]
class TimeMaps(definitions: List<Pair<String, String>>) {
    private class Rule(val days: Set<Int>, val spans: List<Span>, val paint: Paint)

    private class Definition(val map: String, val dates: List<ClosedRange<LocalDate>>, val parent: String?, val rules: List<Rule>)

    /** What was not understood, as texts for the user. */
    val problems = mutableListOf<String>()

    /** The definitions by the names in the field, which for a map on dates carry these dates. */
    private val entries: Map<String, Definition> = definitions.associate { (key, text) ->
        val name = key.trim().substringBefore(' ')
        // [impl->dsn~time-maps-on-dates~1]
        val dates = key.trim().substringAfter(' ', "").split(WHITESPACE).filter(String::isNotEmpty).mapNotNull { token ->
            try {
                LocalDate.parse(token.substringBefore(".."))..LocalDate.parse(token.substringAfter(".."))
            } catch (_: IllegalArgumentException) {
                problems += "$key: \"$token\" is not a date"
                null
            }
        }
        val names = definitions.map { it.first.trim().substringBefore(' ') }.toSet() - name
        val first = text.trim().substringBefore(' ')
        val parent = first.takeIf { it in names }
        val none = dates.isNotEmpty() && text.trim() == "none"
        val rules = (if (none) "" else if (parent == null) text else text.trim().removePrefix(first)).split(',').map(String::trim).filter(String::isNotEmpty).map { rule ->
            val days = mutableSetOf<Int>()
            val spans = mutableListOf<Span>()
            var paint = Paint.GREEN
            for (token in rule.split(WHITESPACE)) {
                val range = TIMES.matchEntire(token)?.destructured?.let { (h1, m1, h2, m2) -> Span(h1.toInt() * 60 + m1.toInt(), h2.toInt() * 60 + m2.toInt()) }
                val from = DAYS.indexOf(token.take(3).lowercase()) + 1
                val to = DAYS.indexOf(token.substringAfter('-', token).lowercase()) + 1
                val color = Paint.entries.firstOrNull { it.word == token.lowercase() }
                when {
                    range != null && range.length > 0 && range.end <= MINUTES_PER_DAY -> spans += range
                    color != null -> paint = color
                    DAY_RANGE.matches(token) && from > 0 && to > 0 -> days += if (from <= to) from..to else (from..7) + (1..to)
                    else -> problems += "$name: \"$token\" is neither days nor times"
                }
            }
            Rule(days.ifEmpty { (1..7).toSet() }, spans.ifEmpty { listOf(Span(0, MINUTES_PER_DAY)) }, paint)
        }
        key to Definition(name, dates, parent, if (none) emptyList() else rules.ifEmpty { listOf(Rule((1..7).toSet(), listOf(Span(0, MINUTES_PER_DAY)), Paint.GREEN)) })
    }

    private val maps: Set<String> = entries.values.map { it.map }.toSet()

    /** The map for tasks whose contexts name none: the first one. */
    val default: String? = entries.values.firstOrNull()?.map

    operator fun contains(name: String): Boolean = name in maps

    /** The map whose times restrict those of the entry of the field, if its definition names one. */
    fun parentOf(name: String): String? = entries[name]?.parent

    /** The definition of the map on the date: that of the last entry with the date, else the one without dates; `null` if neither exists. */
    // [impl->dsn~time-maps-on-dates~1]
    private fun on(name: String, date: LocalDate): Definition? =
        entries.values.lastOrNull { it.map == name && it.dates.any { range -> date in range } } ?: entries[name]?.takeIf { it.dates.isEmpty() }

    /**
     * The times of the map on the day; those of the [default] map for `null` or a name that no map has.
     * Up to [paint], the times of that color and the better ones; without the black times of the other maps.
     */
    fun spans(name: String?, date: LocalDate, paint: Paint = Paint.GREEN): List<Span> {
        val map = name?.takeIf { it in maps } ?: default
        val own = generateSequence(map) { on(it, date)?.parent }.take(maps.size + 1).toSet()
        val exclusive = (maps - own).flatMap { other -> on(other, date)?.let { painted(it, date) }.orEmpty().filter { it.second == Paint.BLACK }.map { it.first } }
        return spans(map, date, paint, emptySet()).without(exclusive)
    }

    private fun spans(name: String?, date: LocalDate, paint: Paint, seen: Set<String>): List<Span> {
        // Without any map, the working hours of an office.
        if (name == null) return if (date.dayOfWeek.isoDayNumber <= 5) listOf(Span(9 * 60, 17 * 60)) else emptyList()
        val map = on(name, date) ?: return emptyList()
        val own = painted(map, date).filter { it.second.rank <= paint.rank }.map { it.first }.joined()
        // A map that is restricted to itself over other maps is not restricted; the times of the map it names count in all colors.
        return if (map.parent == null || map.parent in seen) own else own.within(spans(map.parent, date, Paint.RED, seen + name))
    }

    private fun painted(map: Definition, date: LocalDate) = painted(map, date.dayOfWeek.isoDayNumber)

    /** The times that the rules of the map paint on the day, a later rule over an earlier one. */
    private fun painted(map: Definition, day: Int): List<Pair<Span, Paint>> =
        map.rules.filter { day in it.days }.fold(emptyList()) { result, rule ->
            result.flatMap { (span, paint) -> listOf(span).without(rule.spans).map { it to paint } } + rule.spans.map { it to rule.paint }
        }

    /**
     * The colors of the map in cells of [CELL] minutes from Monday 00:00 on, `null` where the map has no time; its own rules only.
     * A time that does not start or end on a cell is rounded to the cell in which most of it lies.
     */
    // [impl->dsn~time-map-editor~1]
    fun cells(name: String): List<Paint?> = (1..7).flatMap { day ->
        val painted = entries[name]?.let { painted(it, day) }.orEmpty()
        (0 until CELLS_PER_DAY).map { cell ->
            val middle = cell * CELL + CELL / 2
            painted.lastOrNull { (span, _) -> middle in span.start until span.end }?.second
        }
    }

    companion object {
        /** The minutes of a cell of the editor. */
        const val CELL = 15
        const val CELLS_PER_DAY = MINUTES_PER_DAY / CELL

        private val DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        private val DAY_RANGE = Regex("^[A-Za-z]{3}(-[A-Za-z]{3})?$")
        private val TIMES = Regex("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$")
        private val WHITESPACE = Regex("\\s+")

        /**
         * The definition of the cells of [cells]: per color, the days with the same times in one rule.
         * The cells without color give no definition that the map can have, as a definition without rules holds all the time: such cells give `null`.
         */
        // [impl->dsn~time-map-editor~1]
        fun definition(parent: String?, cells: List<Paint?>): String? {
            if (parent == null && cells.all { it == null }) return null
            val rules = Paint.entries.flatMap { paint ->
                (1..7).map { day ->
                    day to cells.subList((day - 1) * CELLS_PER_DAY, day * CELLS_PER_DAY).withIndex()
                        .fold(mutableListOf<Span>()) { spans, (cell, value) ->
                            if (value == paint) spans += Span(cell * CELL, cell * CELL + CELL)
                            spans
                        }.joined()
                }.filter { it.second.isNotEmpty() }.groupBy({ it.second }, { it.first }).map { (spans, days) ->
                    listOfNotNull(dayText(days), spans.joinToString(" ") { "${time(it.start)}-${time(it.end)}" }, paint.word.takeIf { paint != Paint.GREEN }).joinToString(" ")
                }
            }
            return (listOfNotNull(parent) + rules.joinToString(", ").ifEmpty { null }.let(::listOfNotNull)).joinToString(" ")
        }

        /** The days as ranges such as `Mon-Fri Sun`; `null` for all days, which a rule without days means. */
        private fun dayText(days: List<Int>): String? {
            if (days.size == 7) return null
            val runs = days.fold(mutableListOf<IntRange>()) { runs, day ->
                val last = runs.lastOrNull()
                if (last != null && last.last == day - 1) runs[runs.lastIndex] = last.first..day else runs += day..day
                runs
            }
            fun name(day: Int) = DAYS[day - 1].replaceFirstChar(Char::uppercase)
            return runs.joinToString(" ") { if (it.first == it.last) name(it.first) else "${name(it.first)}-${name(it.last)}" }
        }

        private fun time(minutes: Int) = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
    }
}
