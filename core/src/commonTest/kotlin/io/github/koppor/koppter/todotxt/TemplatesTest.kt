package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplatesTest {
    // [utest->dsn~templates~1]
    @Test
    fun aTemplateHasTasksAndTheIndentedLinesBelowATaskAreItsNote() {
        val text = """
            |Ignored before the first heading
            |# Swimming
            |Pack the bag +swim @home
            |    - [ ] towel
            |
            |    - [ ] goggles
            |      - [ ] spare pair
            |Buy a ticket +swim
            |
            |# Journey
            |(A) Book the train #rail
            |
        """.trimMargin()
        assertEquals(
            listOf(
                Template("Swimming", listOf(
                    TemplateTask("Pack the bag +swim @home", "- [ ] towel\n\n- [ ] goggles\n  - [ ] spare pair\n"),
                    TemplateTask("Buy a ticket +swim", ""),
                )),
                Template("Journey", listOf(TemplateTask("(A) Book the train #rail", ""))),
            ),
            parseTemplates(text),
        )
    }

    // [utest->dsn~templates~1]
    @Test
    fun withOneTitleTheTemplatesAreTheSectionsBelowItAndTheTasksTheirListItems() {
        val text = """
            |# Templates
            |
            |Each section is a template.
            |
            |## Swimming
            |
            |- Pack the bag +swim
            |  - [ ] towel
            |
            |  The locker needs a coin.
            |* Buy a ticket +swim
            |
            |---
            |
            |## Trip ##
            |
            |### Not a template
            |
            |- Ignored below a heading of another level
        """.trimMargin()
        assertEquals(
            listOf(
                Template("Swimming", listOf(
                    TemplateTask("Pack the bag +swim", "- [ ] towel\n\nThe locker needs a coin.\n"),
                    TemplateTask("Buy a ticket +swim", ""),
                )),
                Template("Trip", emptyList()),
            ),
            parseTemplates(text),
        )
    }

    // [utest->dsn~templates~1]
    @Test
    fun aHashtagAtTheStartOfALineIsATaskNotAHeading() {
        assertEquals(listOf(Template("T", listOf(TemplateTask("#call Mom", "")))), parseTemplates("# T\r\n#call Mom\r\n"))
    }

    // [utest->dsn~note-checklist~1]
    @Test
    fun theItemsOfANoteAreFoundAtAnyDepthAndToggledInPlace() {
        val note = "Before\n- [ ] towel\n  * [x] goggles\n- [] not an item\n"
        assertEquals(listOf(ChecklistItem(1, false, "towel"), ChecklistItem(2, true, "goggles")), checklist(note))
        assertEquals("Before\n- [x] towel\n  * [x] goggles\n- [] not an item\n", toggleItem(note, 1))
        assertEquals("Before\n- [ ] towel\n  * [ ] goggles\n- [] not an item\n", toggleItem(note, 2))
        assertEquals(note, toggleItem(note, 0))
    }
}
