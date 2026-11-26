package io.github.koppor.koppter

import io.github.koppor.koppter.todotxt.Backlink
import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorHubOneNoteTest {
    // [utest->dsn~onenote-backlinks~1]
    @Test
    fun theRequestHoldsTheSourceAndAllLinks() {
        val links = listOf(
            Backlink("onenote:https://d.docs.live.net/1/Work.one#Plan&page-id={A}&end", "Call \"the\" dentist", "koppter://task?onenote=A&text=Call", false),
            Backlink("onenote:https://example.org/Numbers.one", "Pay", "koppter://task?onenote=x&text=Pay", true),
        )
        assertEquals(
            """{"source":"koppter","name":"koppter","links":[""" +
                """{"note":"onenote:https://d.docs.live.net/1/Work.one#Plan&page-id={A}&end","label":"Call \"the\" dentist","url":"koppter://task?onenote=A&text=Call","done":false},""" +
                """{"note":"onenote:https://example.org/Numbers.one","label":"Pay","url":"koppter://task?onenote=x&text=Pay","done":true}]}""",
            backlinksRequest(links),
        )
        assertEquals("""{"source":"koppter","name":"koppter","links":[]}""", backlinksRequest(emptyList()))
        assertEquals("""{"source":"koppter","name":"koppter","icon":"iVBORw0KGgo=","links":[]}""", backlinksRequest(emptyList(), "iVBORw0KGgo="))
    }

    // [utest->dsn~onenote-backlinks~1]
    @Test
    fun theAnswerGivesTheNumbersAndTheFailedPages() {
        assertEquals(
            BacklinksResult(2, 1, 3, listOf("onenote:https://example.org/Numbers.one" to "page not found")),
            parseBacklinksAnswer("""{"updated":2,"removed":1,"unchanged":3,"failed":[{"note":"onenote:https://example.org/Numbers.one","error":"page not found"}]}""" + "\r\n"),
        )
        assertEquals(BacklinksResult(0, 0, 0, emptyList()), parseBacklinksAnswer("""{"updated":0}"""))
    }
}
