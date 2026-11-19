package io.github.koppor.koppter.storage

import kotlin.test.Test
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
    }

    private val main = Memory("Call Mom @phone\n")
    private val household = Memory("Buy milk\r\nClean @kitchen\r\n")
    private val lists = SharedLists(main, listOf("household" to household))

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
    fun removingTheContextMovesTheTaskWithItsNoteToTheMainFile() {
        household.text = "Buy milk note:abc.md\n"
        household.files["notes/abc.md"] = "Oat milk"
        lists.read()

        lists.write("Call Mom @phone\nBuy milk note:abc.md\n", "Edit")

        assertEquals("Call Mom @phone\nBuy milk note:abc.md\n", main.text)
        assertEquals("", household.text)
        assertEquals("Oat milk", main.files["notes/abc.md"])
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
    fun aListIsNamedAfterTheDirectoryOfItsFile() {
        assertEquals("household", SharedLists.name("/home/me/household/todo.txt"))
        assertEquals("shared-flat", SharedLists.name("C:\\clones\\shared flat\\todo.txt"))
    }

    @Test
    fun withoutSharedListsTheMainStorageIsUsed() {
        assertSame(main, SharedLists.of(main, " \n") { error("not used") })
    }
}
