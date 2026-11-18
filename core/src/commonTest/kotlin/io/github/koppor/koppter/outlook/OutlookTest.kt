package io.github.koppor.koppter.outlook

import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutlookTest {
    private val entryId = "00000000A1B2C3D4E5F60718293A4B5C6D7E8F900700F1E2D3C4B5A69788"

    // [utest->dsn~outlook-tag~1]
    @Test
    fun theTagHoldsTheEntryIdInBase64ForUrls() {
        val tag = Outlook.tag(entryId)
        assertTrue(tag.length * 3 <= entryId.length * 2 + 3)
        assertTrue(tag.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertEquals(entryId, Outlook.entryId(tag))
        assertEquals(entryId, Outlook.entryId(Outlook.tag(entryId.lowercase())))
        assertNull(Outlook.entryId("not base64!"))
        assertNull(Outlook.entryId(""))
    }

    // [utest->dsn~outlook-tag~1]
    @Test
    fun theAnchorNamesTheEntryIdWithoutTheStore() {
        assertEquals("anchor://outlook/message/$entryId?v=1", Outlook.anchor(entryId))
        assertEquals(entryId, Outlook.entryIdOfAnchor(Outlook.anchor(entryId)))
        assertNull(Outlook.entryIdOfAnchor("https://example.org/$entryId"))
    }

    // [utest->dsn~outlook-items~1]
    @Test
    fun anItemIsATaskWithItsSubjectDueDateAndTag() {
        val task = Outlook.task(OutlookItem(entryId, "Material zu\r\n  Literaturrecherche", LocalDate(2026, 11, 18)))
        assertEquals("Material zu Literaturrecherche", task.description)
        assertEquals(LocalDate(2026, 11, 18), task.due)
        assertEquals(listOf("outlook"), task.hashtags)
        assertEquals(entryId, Outlook.entryId(task.tag(Task.OUTLOOK)!!))
        assertNull(Outlook.task(OutlookItem(entryId, "No date", null)).due)
        assertEquals("(no subject)", Outlook.task(OutlookItem(entryId, " ", null)).description)
    }

    // [utest->dsn~outlook-items~1]
    @Test
    fun aSubjectThatLooksLikeTheHeaderOfALineStaysText() {
        for (subject in listOf("x marks the spot", "(A) Review", "2026-11-18 Meeting")) {
            val line = Outlook.task(OutlookItem(entryId, subject, null)).toLine()
            val task = Task.parse(line)
            assertFalse(task.completed, line)
            assertNull(task.priority, line)
            assertNull(task.creationDate, line)
        }
    }

    // [utest->dsn~outlook-items~1]
    @Test
    fun anItemThatALineNamesIsLeftOut() {
        val other = "00000000FFEE"
        val file = TodoFile.parse("x Answer the mail ol:${Outlook.tag(entryId)}\nCall Mom\n")
        val entries = Outlook.entries(listOf(OutlookItem(entryId, "Mail", null), OutlookItem(other, "Other", null)), file.entries)
        assertEquals(listOf("Other"), entries.map { it.task.description })
        assertTrue(entries.all(Outlook::isItem))
        assertFalse(file.entries.any(Outlook::isItem))
    }
}
