package io.github.koppor.koppter.plan

/** The place of an entry of a day in a calendar view: the column [index] of [count] columns that share the width of the day. */
data class Lane(val index: Int, val count: Int)

/**
 * The lanes of the spans of one day, in the order of [spans], so that entries that overlap stand side by side.
 * Spans that overlap, directly or through others, form a group whose entries share the width; an entry takes the first column that is free at its start.
 * An entry that overlaps nothing takes the whole width.
 */
// [impl->dsn~calendar-lanes~1]
fun lanes(spans: List<Span>): List<Lane> {
    val result = arrayOfNulls<Lane>(spans.size)
    // Earlier first, and of those the longer one, so that it stands to the left.
    val order = spans.indices.sortedWith(compareBy<Int> { spans[it].start }.thenByDescending { spans[it].end })
    var group = mutableListOf<Pair<Int, Int>>()
    // The ends of the entries in each column of the group.
    val columnEnds = mutableListOf<Int>()
    var groupEnd = Int.MIN_VALUE

    fun close() {
        for ((index, column) in group) result[index] = Lane(column, columnEnds.size)
        group = mutableListOf()
        columnEnds.clear()
    }

    for (index in order) {
        val span = spans[index]
        if (span.start >= groupEnd) close()
        val column = columnEnds.indexOfFirst { it <= span.start }.takeIf { it >= 0 } ?: columnEnds.size.also { columnEnds += 0 }
        columnEnds[column] = span.end
        group += index to column
        groupEnd = if (group.size == 1) span.end else maxOf(groupEnd, span.end)
    }
    close()
    return result.map { it!! }
}
