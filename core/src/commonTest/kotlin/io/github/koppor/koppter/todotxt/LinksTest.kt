package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinksTest {
    // [utest->dsn~link-titles~1]
    @Test
    fun theTitleOfAPageStaysOneLinkAndAddsNoLabel() {
        assertEquals("Tom & Jerry: (S1) home x: y #42 home", pageTitle("<head><TITLE lang=en>\n Tom &amp; Jerry: [S1] @home x:y #42 &#x2B;home</TITLE>"))
        assertEquals("Café", pageTitle("<title>Caf&#233;</title>"))
        assertNull(pageTitle("<title> </title>"))
        assertNull(pageTitle("<p>no title</p>"))
    }

    // [utest->dsn~link-titles~1]
    @Test
    fun onlyABareAddressBecomesALink() {
        val text = "See [Old](https://example.org/a) https://example.org/a. https://example.org/ab"
        assertEquals(listOf("https://example.org/a", "https://example.org/ab"), bareAddresses(text).map(text::substring))
        assertEquals(
            "See [Old](https://example.org/a) [New](https://example.org/a). https://example.org/ab",
            withLinkTitle(text, "https://example.org/a", "New"),
        )
        assertEquals(listOf(MarkdownLink(4..31, "Old", "https://example.org/a")), markdownLinks(text))
    }
}
