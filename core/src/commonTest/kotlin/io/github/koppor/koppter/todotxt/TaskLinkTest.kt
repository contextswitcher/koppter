package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskLinkTest {
    private val page = "https://d.docs.live.net/0123456789ABCDEF/Documents/Work.one#%C3%84rger%20-%20Review&section-id={8D0158A8-979F-4ABA-9DC2-77F6F43A00C8}&page-id={666AC41B-1C96-448D-AF38-3DEF10279206}&end"
    private val other = "https://d.docs.live.net/0123456789ABCDEF/Documents/Work.one#Plan&section-id={8D0158A8-979F-4ABA-9DC2-77F6F43A00C8}&page-id={11111111-2222-3333-4444-555555555555}&end"

    // [utest->dsn~task-link~1]
    @Test
    fun theKeyOfAPageIsItsPageIdOrTheWholeLink() {
        assertEquals("666AC41B-1C96-448D-AF38-3DEF10279206", oneNoteKey(page))
        assertEquals("666AC41B-1C96-448D-AF38-3DEF10279206", oneNoteKey(page.replace("{", "%7B").replace("}", "%7D")))
        assertEquals("https://example.org/Notebook/Numbers.one", oneNoteKey("https://example.org/Notebook/Numbers.one"))
    }

    // [utest->dsn~task-link~1]
    @Test
    fun theLinkNamesThePageAndTheDescription() {
        val task = Task.parse("Call the dentist +Health @phone onenote:$page")
        assertEquals("koppter://task?onenote=666AC41B-1C96-448D-AF38-3DEF10279206&text=Call%20the%20dentist", taskLink(task, page))
        assertEquals(TaskLink("666AC41B-1C96-448D-AF38-3DEF10279206", "Call the dentist"), taskLinkOf(taskLink(task, page)))
        assertEquals(TaskLink("x", "Ärger"), taskLinkOf("koppter:task?onenote=x&text=%C3%84rger"))
        assertEquals(TaskLink("x", ""), taskLinkOf("koppter://task/?onenote=x"))
        assertNull(taskLinkOf("koppter://task?text=Call"))
        assertNull(taskLinkOf("koppter://filter?onenote=x"))
    }

    // [utest->dsn~task-link~1]
    @Test
    fun theTaskWithTheTextIsPreferredAndAnyOfThePageIsTheFallback() {
        val tasks = listOf(
            Task.parse("Call the dentist onenote:$page"),
            Task.parse("Book the appointment onenote:$page"),
            Task.parse("Call the dentist onenote:$other"),
        )
        val link = TaskLink("666ac41b-1c96-448d-af38-3def10279206", "Call the dentist")
        assertEquals(listOf(tasks[0]), link.find(tasks) { it })
        assertEquals(tasks.take(2), link.copy(text = "Call the dentist again").find(tasks) { it })
        assertEquals(listOf(tasks[1]), link.copy(text = "Call").find(tasks.drop(1)) { it })
        assertEquals(emptyList(), TaskLink("00000000-0000-0000-0000-000000000000", "Call the dentist").find(tasks) { it })
    }

    // [utest->dsn~onenote-backlinks~1]
    @Test
    fun everyTaskWithAPageAndEveryArchivedOneGivesABacklink() {
        val tasks = listOf(
            Task.parse("Call the dentist +Health @phone onenote:$page"),
            Task.parse("x 2026-11-20 Send the slides onenote:$page onenote:$other"),
            Task.parse("Buy milk @shop"),
        )
        val archived = listOf(Task.parse("x 2026-10-01 Write the review onenote:$page"), Task.parse("x 2026-10-02 Pay the bill"))
        assertEquals(
            listOf(
                Backlink("onenote:$page", "Call the dentist", "koppter://task?onenote=666AC41B-1C96-448D-AF38-3DEF10279206&text=Call%20the%20dentist", false),
                Backlink("onenote:$page", "Send the slides", "koppter://task?onenote=666AC41B-1C96-448D-AF38-3DEF10279206&text=Send%20the%20slides", true),
                Backlink("onenote:$other", "Send the slides", "koppter://task?onenote=11111111-2222-3333-4444-555555555555&text=Send%20the%20slides", true),
                Backlink("onenote:$page", "Write the review", "koppter://task?onenote=666AC41B-1C96-448D-AF38-3DEF10279206&text=Write%20the%20review", true),
            ),
            backlinks(tasks, archived),
        )
        // A task that is in the file and in the archive, after an archiving that failed halfway, counts once.
        assertEquals(1, backlinks(tasks.drop(1).take(1), tasks.drop(1).take(1)).count { it.note == "onenote:$page" })
        assertEquals(emptyList(), backlinks(listOf(Task.parse("Buy milk")), emptyList()))
    }
}
