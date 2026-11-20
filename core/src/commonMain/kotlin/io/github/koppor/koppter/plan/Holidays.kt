package io.github.koppor.koppter.plan

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The public holidays of the German states, as far as they hold in the whole state.
 * Holidays of single communities, such as the Assumption in parts of Bavaria or Corpus Christi in parts of Saxony and Thuringia, are left out.
 */
// [impl->dsn~holidays~1]
object Holidays {
    /** The states by their abbreviations. */
    val STATES = linkedMapOf(
        "BW" to "Baden-Württemberg", "BY" to "Bayern", "BE" to "Berlin", "BB" to "Brandenburg", "HB" to "Bremen", "HH" to "Hamburg",
        "HE" to "Hessen", "MV" to "Mecklenburg-Vorpommern", "NI" to "Niedersachsen", "NW" to "Nordrhein-Westfalen", "RP" to "Rheinland-Pfalz",
        "SL" to "Saarland", "SN" to "Sachsen", "ST" to "Sachsen-Anhalt", "SH" to "Schleswig-Holstein", "TH" to "Thüringen",
    )

    fun of(state: String, year: Int): List<LocalDate> {
        val easter = easter(year)
        fun easter(days: Int) = easter.plus(DatePeriod(days = days))
        fun date(month: Int, day: Int) = LocalDate(year, month, day)
        fun only(vararg states: String, date: () -> LocalDate) = if (state in states) listOf(date()) else emptyList()
        val november22 = date(11, 22)
        val repentance = november22.minus(DatePeriod(days = (november22.dayOfWeek.ordinal - DayOfWeek.WEDNESDAY.ordinal + 7) % 7))
        return (
            listOf(date(1, 1), easter(-2), easter(1), date(5, 1), easter(39), easter(50), date(10, 3), date(12, 25), date(12, 26)) +
                only("BW", "BY", "ST") { date(1, 6) } +
                only("BE", "MV") { date(3, 8) } +
                only("BB") { easter } +
                only("BB") { easter(49) } +
                only("BW", "BY", "HE", "NW", "RP", "SL") { easter(60) } +
                only("SL") { date(8, 15) } +
                only("TH") { date(9, 20) } +
                only("BB", "HB", "HH", "MV", "NI", "SN", "ST", "SH", "TH") { date(10, 31) } +
                only("BW", "BY", "NW", "RP", "SL") { date(11, 1) } +
                only("SN") { repentance }
            ).sorted()
    }

    /** The entry of the time maps that leaves the map without time on the holidays of the state from [today] to the end of the next year. */
    fun entry(map: String, state: String, today: LocalDate): Pair<String, String> =
        "$map ${(of(state, today.year) + of(state, today.year + 1)).filter { it >= today }.joinToString(" ")}" to "none"

    /** Easter Sunday in the Gregorian calendar, by the algorithm of Meeus, Jones and Butcher. */
    private fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val g = (b - (b + 8) / 25 + 1) / 3
        val h = (19 * a + b - b / 4 - g + 15) % 30
        val l = (32 + 2 * (b % 4) + 2 * (c / 4) - h - c % 4) % 7
        val n = h + l - 7 * ((a + 11 * h + 22 * l) / 451) + 114
        return LocalDate(year, n / 31, n % 31 + 1)
    }
}
