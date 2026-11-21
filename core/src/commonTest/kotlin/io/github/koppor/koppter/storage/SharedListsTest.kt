package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

// [utest->dsn~shared-lists~1]
class SharedListsTest {
    private class Memory(var text: String = "") : TodoStorage, CompanionFiles {
        val files = mutableMapOf<String, Any>()
        var writes = 0

        override fun read() = text
        override fun write(text: String, change: String) {
            this.text = text
            writes++
        }

        override fun version(): Any? = text
        override fun readCompanion(path: String) = files[path] as? String
        override fun writeCompanion(path: String, text: String) {
            files[path] = text
        }

        override fun readCompanionBytes(path: String) = files[path] as? ByteArray
        override fun writeCompanionBytes(path: String, bytes: ByteArray) {
            files[path] = bytes
        }

        override fun deleteCompanion(path: String) {
            files -= path
        }
    }

    /** A list whose clone is gone. */
    private class Missing : TodoStorage {
        var written = false
        override fun read(): String = error("clone missing")
        override fun write(text: String, change: String) {
            written = true
        }
        override fun version(): Any? = null
    }

    private val main = Memory("Call Mom @phone\n")
    private val household = Memory("Buy milk\r\nClean @kitchen\r\n")
    private val lists = SharedLists(main, listOf("household" to household))

    // [utest->dsn~family-day~1]
    @Test
    fun aFileNextToAListIsReadByTheNameOfTheList() {
        household.files["calendar.txt"] = "2026-11-21 w47 Sat  08:00-12:00 Absent\n"
        main.files["calendar.txt"] = "2026-11-21 w47 Sat  09:00-10:00 Call Mom\n"
        assertEquals(listOf("household"), lists.names)
        assertEquals("2026-11-21 w47 Sat  08:00-12:00 Absent\n", lists.readCompanionOf("household", "calendar.txt"))
        assertNull(lists.readCompanionOf("household", "timelog.txt"))
        assertNull(lists.readCompanionOf("elsewhere", "calendar.txt"))
        // The main file is no list.
        assertNull(lists.readCompanionOf("", "calendar.txt"))
    }

    @Test
    fun theTasksOfASharedListAreShownWithItsContext() {
        assertEquals("Call Mom @phone\nBuy milk @household\nClean @kitchen @household\n", lists.read())
    }

    @Test
    fun linesThatWereNotEditedAreWrittenBackUnchanged() {
        lists.write(lists.read(), "Nothing")

        assertEquals("Call Mom @phone\n", main.text)
        assertEquals("Buy milk\r\nClean @kitchen\r\n", household.text)
        assertEquals(0, main.writes + household.writes)
    }

    @Test
    fun aNewTaskGoesToTheListOfItsContext() {
        lists.write(lists.read() + "Pay rent @household>money\nRead @home\n", "Add")

        assertEquals("Call Mom @phone\nRead @home\n", main.text)
        assertEquals("Buy milk\r\nClean @kitchen\r\nPay rent @household>money\r\n", household.text)
    }

    @Test
    fun removingTheContextMovesTheTaskWithItsNoteAndItsImagesToTheMainFile() {
        household.text = "Buy milk note:abc.md\n"
        household.files["notes/abc.md"] = "Oat milk ![](abc-1.jpg)"
        household.files["notes/abc-1.jpg"] = byteArrayOf(7)
        lists.read()

        lists.write("Call Mom @phone\nBuy milk note:abc.md\n", "Edit")

        assertEquals("Call Mom @phone\nBuy milk note:abc.md\n", main.text)
        assertEquals("", household.text)
        assertEquals("Oat milk ![](abc-1.jpg)", main.files["notes/abc.md"])
        assertEquals(7, main.readCompanionBytes("notes/abc-1.jpg")?.single())
        assertEquals(emptyMap(), household.files)
        assertEquals(1, household.writes)
    }

    @Test
    fun aNoteThatAnotherLineOfItsFileStillNamesIsCopiedAndNotDeleted() {
        household.text = "Buy milk note:abc.md\nBuy more milk note:abc.md\n"
        household.files["notes/abc.md"] = "Oat milk"
        lists.read()

        lists.write("Call Mom @phone\nBuy milk note:abc.md\nBuy more milk note:abc.md @household\n", "Edit")

        assertEquals("Oat milk", main.files["notes/abc.md"])
        assertEquals("Oat milk", household.files["notes/abc.md"])
    }

    @Test
    fun aListThatCannotBeReadShowsNoTasksAndIsNotWritten() {
        val missing = Missing()
        val withMissing = SharedLists(main, listOf("flat" to missing))
        val reported = mutableListOf<SyncStatus>()
        withMissing.onStatus = { reported += it }

        assertEquals("Call Mom @phone\n", withMissing.read())
        assertEquals("@flat: clone missing", reported.single().message)

        withMissing.write("Call Mom @phone\nRead @home\n", "Add")
        assertEquals("Call Mom @phone\nRead @home\n", main.text)
        val failure = assertFailsWith<IllegalStateException> { withMissing.write("Call Mom @phone\nRead @home\nClean @flat\n", "Add") }
        assertEquals("Not written: the list @flat cannot be read (clone missing)", failure.message)
        assertFalse(missing.written)
        assertEquals("Call Mom @phone\nRead @home\n", main.text)
    }

    // [utest->dsn~shared-ice-recur~1]
    @Test
    fun theScheduleIsThatOfAllFilesWithTheContextOfTheList() {
        main.files[ICE_RECUR_FILE] = "weekly ; day Friday - Weekly review @review\n"
        household.files[ICE_RECUR_FILE] = "# Shared\r\nweekly ; day Monday - Take out the trash\r\n"
        lists.read()

        assertEquals(
            "weekly ; day Friday - Weekly review @review\n# Shared\nweekly ; day Monday - Take out the trash @household\n",
            lists.readCompanion(ICE_RECUR_FILE),
        )

        lists.writeCompanion(
            ICE_RECUR_FILE,
            "weekly ; day Friday - Weekly review @review\n# Shared\nweekly ; day Monday - Take out the trash @household\nmonthly ; day_of_month 1 - Pay rent @household\n",
        )
        assertEquals("weekly ; day Friday - Weekly review @review\n", main.files[ICE_RECUR_FILE])
        assertEquals("# Shared\nweekly ; day Monday - Take out the trash\nmonthly ; day_of_month 1 - Pay rent\n", household.files[ICE_RECUR_FILE])
    }

    // [utest->dsn~shared-ice-recur~1]
    @Test
    fun aListGetsAScheduleOnlyForAnEntryOfItsOwn() {
        main.files[ICE_RECUR_FILE] = "weekly ; day Friday - Weekly review\n"
        lists.read()

        lists.writeCompanion(ICE_RECUR_FILE, "daily - Water the plants\n")

        assertEquals("daily - Water the plants\n", main.files[ICE_RECUR_FILE])
        assertNull(household.files[ICE_RECUR_FILE])
    }

    @Test
    fun aNewNoteAndItsImageGoToTheFileOfTheirTask() {
        lists.read()
        lists.writeCompanionBytes("notes/abc-1.jpg", byteArrayOf(1))
        lists.writeCompanion("notes/abc.md", "![](abc-1.jpg)")
        assertEquals("![](abc-1.jpg)", lists.readCompanion("notes/abc.md"))

        lists.write(lists.read() + "Fix tap @household note:abc.md\n", "Add")

        assertEquals("![](abc-1.jpg)", household.files["notes/abc.md"])
        assertEquals(1, household.readCompanionBytes("notes/abc-1.jpg")?.single())
        assertNull(main.files["notes/abc.md"])
    }

    @Test
    fun theConfigurationIsThatOfTheMainFile() {
        lists.writeCompanion(ConfigFile.NAME, "x")
        lists.write(lists.read(), "Config")

        assertEquals("x", main.files[ConfigFile.NAME])
        assertEquals(1, main.writes)
        assertEquals(0, household.writes)
    }

    @Test
    fun theContextOfTheListIsRemovedWhereverItStands() {
        assertEquals("Buy milk +food", SharedLists.stripped("household", "Buy milk @household +food"))
        assertEquals("Buy milk", SharedLists.stripped("household", "Buy milk @household"))
        assertEquals("Buy milk @household>food", SharedLists.stripped("household", "Buy milk @household>food"))
    }

    @Test
    fun aListIsNamedAfterItsRepositoryOrTheDirectoryOfItsFile() {
        assertEquals("household", SharedLists.name("/home/me/household/todo.txt"))
        assertEquals("shared-flat", SharedLists.name("C:\\clones\\shared flat\\todo.txt"))
        assertEquals("household", SharedLists.name("git@github.com:me/household.git"))
        assertEquals("flat", SharedLists.name("https://example.org/we/flat/"))
    }

    @Test
    fun withoutSharedListsTheMainStorageIsUsed() {
        assertSame(main, SharedLists.of(main, " \n") { error("not used") })
    }
}
