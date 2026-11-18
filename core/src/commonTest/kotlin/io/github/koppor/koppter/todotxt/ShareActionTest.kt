package io.github.koppor.koppter.todotxt

import io.github.koppor.koppter.storage.ConfigFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~share-actions~1]
class ShareActionTest {
    private val today = LocalDate(2026, 11, 12)

    @Test
    fun theTemplateMakesATaskOfTheSharedTextWithDatesFromTheDayOfSharing() {
        val reading = ShareAction("reading", "{} @research>reading due:2w dur:1h")

        assertEquals(
            "https://example.org/article @research>reading due:2026-11-26 dur:1h",
            reading.task(" https://example.org/article\n", today).toLine(),
        )
        // Without the placeholder, the shared text stands in front; a fixed date stays.
        assertEquals(
            "An article https://example.org/a +Reading t:2026-11-13 due:2026-12-24",
            ShareAction("later", "+Reading t:tomorrow due:2026-12-24").task("An article\nhttps://example.org/a", today).toLine(),
        )
        // Shared text that looks like the start of a todo.txt line stays text.
        assertEquals("(A) x 2026-01-01 list @inbox", ShareAction("inbox", "@inbox").task("(A) x 2026-01-01 list", today).toLine())
    }

    @Test
    fun actionsAreReadFromLinesAndKeptInTheConfigurationNextToTheColumns() {
        val actions = ShareAction.parse("reading = {} @research>reading due:2w\n\nno title\ninbox = @inbox")
        assertEquals(listOf(ShareAction("reading", "{} @research>reading due:2w"), ShareAction("inbox", "@inbox")), actions)

        val config = ConfigFile.withShareActions("board-columns:\n  Waiting: \"@waiting\"\n", actions)
        assertEquals(actions, ConfigFile.shareActions(config))
        assertEquals(listOf(BoardColumn("Waiting", "@waiting")), ConfigFile.boardColumns(config))
        assertEquals(emptyList(), ConfigFile.shareActions(ConfigFile.withShareActions(config, emptyList())))
    }

    // [utest->dsn~note-images~1]
    @Test
    fun theImagesOfANoteAreItsMarkdownImagesThatNameAFile() {
        val note = "Receipt:\n\n![](a1b-1.jpg)\n![the stand](<a1b-2.jpg>) ![](https://example.org/x.png) ![](../todo.txt) ![](a1b-1.jpg) [no image](a1b-3.jpg)"
        assertEquals(listOf("a1b-1.jpg", "a1b-2.jpg"), noteImages(note))
        assertEquals("a1b-3.jpg", imageName("a1b.md") { it in noteImages(note) })
        assertEquals("a1b-1.jpg", imageName("a1b.md") { false })
    }

    // [utest->dsn~onenote-links~1]
    @Test
    fun theTwoLinesThatOneNoteCopiesBecomeTwoMarkdownLinks() {
        val web = "https://onedrive.example.org/view.aspx?resid=ABC%21101&wd=target%28Notes.one%7C1111%2FShopping%20list%7C2222%2F%29&wdpartid={3333}{1}&end"
        val desktop = "onenote:https://d.docs.example.org/abc/Documents/Notes.one#Shopping%20list&section-id={1111}&page-id={2222}&end"
        val links = "- [OneNote Desktop]($desktop)\n- [OneNote Web]($web)"

        assertEquals(links, oneNoteLinks("$web\r\n$desktop"))
        // The list starts on a line of its own, and what is a list already stays.
        assertEquals("See:\n$links\nand buy", oneNoteLinks("See:$web\n$desktop\nand buy"))
        assertEquals(links, oneNoteLinks(links))
        // A parenthesis would end a Markdown link.
        assertEquals(
            "- [OneNote Desktop](<onenote:https://example.org/Notes.one#Plan%20(old)&end>)\n- [OneNote Web](https://example.org/view)",
            oneNoteLinks("https://example.org/view\nonenote:https://example.org/Notes.one#Plan%20(old)&end"),
        )
        // Other addresses stay as they are.
        assertEquals("https://example.org/a\nhttps://example.org/b", oneNoteLinks("https://example.org/a\nhttps://example.org/b"))
    }
}
