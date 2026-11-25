package io.github.koppor.koppter.plan

import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~calendar-lanes~1]
class LanesTest {
    private fun lanesOf(vararg spans: Pair<Int, Int>): List<String> = lanes(spans.map { Span(it.first * 60, it.second * 60) }).map { "${it.index}/${it.count}" }

    @Test
    fun anEntryThatOverlapsNothingTakesTheWholeWidth() {
        assertEquals(listOf("0/1", "0/1"), lanesOf(9 to 10, 10 to 11))
    }

    @Test
    fun overlappingEntriesStandSideBySideInTheOrderOfTheirStart() {
        // Given in another order than their start.
        assertEquals(listOf("1/2", "0/2"), lanesOf(10 to 12, 9 to 11))
    }

    @Test
    fun anEntryTakesTheFirstColumnThatIsFreeAtItsStart() {
        // The long one keeps the first column; the third one starts after the second one and takes its column.
        assertEquals(listOf("0/2", "1/2", "1/2", "0/1"), lanesOf(9 to 13, 9 to 10, 11 to 12, 14 to 15))
    }

    @Test
    fun theLongerOfTwoEntriesWithTheSameStartStandsLeft() {
        assertEquals(listOf("1/2", "0/2"), lanesOf(9 to 10, 9 to 12))
    }

    @Test
    fun entriesThatOverlapThroughAnotherOneShareTheWidth() {
        // The first and the third do not overlap, but both overlap the second one.
        assertEquals(listOf("0/2", "1/2", "0/2"), lanesOf(9 to 11, 10 to 13, 12 to 14))
    }

    @Test
    fun threeAtOnceTakeAThirdEach() {
        assertEquals(listOf("0/3", "1/3", "2/3"), lanesOf(9 to 12, 10 to 12, 11 to 12))
    }
}
