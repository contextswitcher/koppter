package io.github.koppor.koppter.plan

/**
 * The events of the calendars with busy times that the user put on the sideline one by one, kept next to the todo.txt file.
 *
 * A line holds the UID of an event, for a repeating one of all its occurrences, and after ` # ` its title for the reader:
 *
 * ```text
 * 040000008200E00074C5B7101A82E008 # Office hours
 * ```
 *
 * Empty lines and lines that start with `#` are comments.
 */
// ponytail: a UID that contains " #" is cut there; no calendar seen so far writes one.
// [impl->dsn~event-sideline~1]
object SidelinedEvents {
    const val NAME = "calendar-sideline.txt"

    private const val HEADER = "# Events of the calendars whose time counts as free for planning: the UID of the event, then its title"

    /** The UIDs of the events on the sideline. */
    fun ids(text: String): Set<String> =
        text.lines().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.map { it.substringBefore(" #").trim() }.toSet()

    /** The text with the event put on the sideline or taken off; the lines of other events stay as they are. */
    fun with(text: String, uid: String, title: String, on: Boolean): String {
        val lines = text.replace("\r\n", "\n").lines().dropLastWhile(String::isEmpty)
        val kept = lines.filterNot { line -> line.trim().let { !it.startsWith("#") && it.substringBefore(" #").trim() == uid } }
        val added = if (on) listOf(if (title.isBlank()) uid else "$uid # ${title.replace('\n', ' ')}") else emptyList()
        val result = (if (kept.isEmpty() && on) listOf(HEADER) else kept) + added
        return if (result.isEmpty()) "" else result.joinToString("\n", postfix = "\n")
    }
}
