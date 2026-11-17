package io.github.koppor.koppter.sync

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitStorageTest {
    private val root = Files.createTempDirectory("koppter-git")
    private val remote = root.resolve("remote.git").also {
        Git.init().setBare(true).setInitialBranch("main").setDirectory(it.toFile()).call().close()
    }

    /** A device: a repository of its own with the shared remote. */
    private fun device(name: String): GitStorage {
        val directory = root.resolve(name)
        val git = Git.init().setInitialBranch("main").setDirectory(directory.toFile()).call()
        git.remoteAdd().setName("origin").setUri(URIish(remote.toUri().toString())).call()
        return storage(directory)
    }

    private fun storage(directory: Path): GitStorage {
        val file = directory.resolve("todo.txt")
        return GitStorage(file, assertNotNull(GitStorage.repositoryOf(file)))
    }

    private fun messages(directory: String): List<String> =
        Git.open(root.resolve(directory).toFile()).use { git -> git.log().call().map { it.shortMessage } }

    // [utest->dsn~commit-per-change~1]
    @Test
    fun everyChangeIsACommitWithTheChangeAsMessage() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        phone.write("x Call Mom\n", "Complete task: Call Mom")
        phone.write("x Call Mom\n", "Nothing changed")
        assertEquals(listOf("Complete task: Call Mom", "Add task: Call Mom"), messages("phone"))
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun aSecondDeviceReceivesTheChanges() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        phone.synchronizeNow()

        val desktop = device("desktop")
        desktop.synchronizeNow()
        assertEquals("Call Mom\n", desktop.read())

        desktop.write("x Call Mom\n", "Complete task: Call Mom")
        desktop.synchronizeNow()
        phone.synchronizeNow()
        assertEquals("x Call Mom\n", phone.read())
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun concurrentChangesOnTwoDevicesAreMergedWithoutConflict() {
        val phone = device("phone")
        phone.write("Call Mom\nBuy milk\n", "Add tasks")
        phone.synchronizeNow()
        val desktop = device("desktop")
        desktop.synchronizeNow()

        // Both change adjacent lines, which a textual merge reports as a conflict.
        phone.write("x Call Mom\nBuy milk\n", "Complete task: Call Mom")
        desktop.write("Call Mom\nBuy milk +shop\nWater plants\n", "Edit and add")
        phone.synchronizeNow()
        desktop.synchronizeNow()
        phone.synchronizeNow()

        assertEquals("x Call Mom\nBuy milk +shop\nWater plants\n", desktop.read())
        assertEquals(desktop.read(), phone.read())
        assertEquals("Merge remote changes", messages("desktop").first())
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun aChangeByAnotherProgramIsCommittedOnSynchronization() {
        val phone = device("phone")
        phone.write("Call Mom\n", "Add task: Call Mom")
        root.resolve("phone/todo.txt").appendText("Typed in an editor\n")
        phone.synchronizeNow()
        assertEquals("Update todo.txt", messages("phone").first())
        assertEquals("Call Mom\nTyped in an editor\n", root.resolve("phone/todo.txt").readText())
    }

    // [utest->dsn~dates-from-history~1]
    @Test
    fun theDatesOfATaskComeFromTheCommits() {
        val phone = device("phone")
        assertEquals(emptyMap(), phone.dates())
        phone.write("Call Mom\nBuy milk\n", "Add tasks")
        phone.write("x Call Mom\nBuy milk\n", "Complete task: Call Mom")
        val today = java.time.LocalDate.now().toString()
        val dates = phone.dates()
        assertEquals(today, dates.getValue("x Call Mom").added.toString())
        assertEquals(today, dates.getValue("x Call Mom").completed.toString())
        assertNull(dates.getValue("Buy milk").completed)
    }

    // [utest->dsn~git-detection~1]
    @Test
    fun aFileOutsideARepositoryHasNone() {
        // The temporary directory itself could lie in a repository only on an unusual machine.
        assertNull(GitStorage.repositoryOf(Files.createTempDirectory("koppter-plain").resolve("todo.txt")))
        assertNotNull(GitStorage.repositoryOf(root.resolve("phone").also { Git.init().setDirectory(it.toFile()).call() }.resolve("sub/../todo.txt")))
    }

    /** Two devices with the same files, of which each then changes its own. */
    private fun twoDevices(companions: Map<String, String>): Pair<GitStorage, GitStorage> {
        val phone = device("phone")
        companions.forEach(phone::writeCompanion)
        phone.write("Call Mom\nBuy milk\n", "Add tasks")
        phone.synchronizeNow()
        val desktop = device("desktop")
        desktop.synchronizeNow()
        return phone to desktop
    }

    // [utest->dsn~companion-files~1]
    @Test
    fun anImageOfANoteReachesTheOtherDeviceByteForByte() {
        val (phone, desktop) = twoDevices(emptyMap())
        // With the bytes of line ends, which a conversion of text would change.
        val image = byteArrayOf(-1, -40, -1, 0, 13, 10, 26, 10, -128)

        phone.writeCompanionBytes("notes/a1b-1.jpg", image)
        phone.writeCompanion("notes/a1b.md", "![](a1b-1.jpg)\n")
        phone.write("Call Mom note:a1b.md\nBuy milk\n", "Edit task: Call Mom")
        phone.synchronizeNow()
        desktop.synchronizeNow()

        assertEquals(image.toList(), desktop.readCompanionBytes("notes/a1b-1.jpg")?.toList())
    }

    // [utest->dsn~merge-of-companions~1]
    @Test
    fun theScheduleIsMergedLineByLine() {
        val (phone, desktop) = twoDevices(mapOf("ice_recur.txt" to "daily - Check the mail\nweekly ; day Monday - Plan the week\n"))

        // Both change adjacent lines, which a textual merge reports as a conflict.
        phone.writeCompanion("ice_recur.txt", "daily - (A) Check the mail\nweekly ; day Monday - Plan the week\n")
        phone.write("Call Mom\nBuy milk\nWater plants\n", "Add task: Water plants")
        desktop.writeCompanion("ice_recur.txt", "daily - Check the mail\nweekly ; day Friday - Plan the week\nmonthly - Check the account\n")
        desktop.write("x Call Mom\nBuy milk\n", "Complete task: Call Mom")
        phone.synchronizeNow()
        desktop.synchronizeNow()
        phone.synchronizeNow()

        assertEquals("daily - (A) Check the mail\nweekly ; day Friday - Plan the week\nmonthly - Check the account\n", desktop.readCompanion("ice_recur.txt"))
        assertEquals(desktop.readCompanion("ice_recur.txt"), phone.readCompanion("ice_recur.txt"))
        assertEquals("x Call Mom\nBuy milk\nWater plants\n", phone.read())
    }

    // [utest->dsn~merge-of-companions~1]
    @Test
    fun ofTwoPlansTheNewerOneIsTaken() {
        val (phone, desktop) = twoDevices(mapOf("calendar.txt" to "2026-11-12 09:00-10:00 Call Mom\n", "calendar.ics" to "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"))

        phone.writeCompanion("calendar.txt", "2026-11-12 11:00-12:00 Call Mom\n")
        phone.writeCompanion("calendar.ics", "BEGIN:VCALENDAR\r\nX-PLAN:phone\r\nEND:VCALENDAR\r\n")
        phone.write("Call Mom\nBuy milk\nWater plants\n", "Plan")
        // The time of a commit counts in seconds.
        Thread.sleep(1100)
        desktop.writeCompanion("calendar.txt", "2026-11-12 14:00-15:00 Call Mom\n")
        desktop.writeCompanion("calendar.ics", "BEGIN:VCALENDAR\r\nX-PLAN:desktop\r\nEND:VCALENDAR\r\n")
        desktop.write("x Call Mom\nBuy milk\n", "Plan")
        // The device with the older plan merges as well as the one with the newer plan.
        desktop.synchronizeNow()
        phone.synchronizeNow()
        desktop.synchronizeNow()

        for (device in listOf(phone, desktop)) {
            assertEquals("2026-11-12 14:00-15:00 Call Mom\n", device.readCompanion("calendar.txt"))
            assertEquals("BEGIN:VCALENDAR\r\nX-PLAN:desktop\r\nEND:VCALENDAR\r\n", device.readCompanion("calendar.ics"))
            assertEquals("x Call Mom\nBuy milk\nWater plants\n", device.read())
        }
    }

    // [utest->dsn~git-sync~1]
    @Test
    fun anOrderChangedOnOneDeviceIsKeptWhenTheOtherChangedAnotherFile() {
        val (phone, desktop) = twoDevices(mapOf("notes/abc.txt" to "Ask about the recipe\n"))

        phone.write("Buy milk\nCall Mom\n", "Move task up: Buy milk")
        desktop.writeCompanion("notes/abc.txt", "Ask about the recipe and the photos\n")
        desktop.write("Call Mom\nBuy milk\n", "Edit note")
        desktop.synchronizeNow()
        phone.synchronizeNow()

        assertEquals("Buy milk\nCall Mom\n", phone.read())
        assertEquals("Ask about the recipe and the photos\n", phone.readCompanion("notes/abc.txt"))
    }

    // [utest->dsn~companion-files~1]
    @Test
    fun aCompanionFileIsCommittedWithTheNextChange() {
        val phone = device("phone")
        assertNull(phone.readCompanion("notes/abc.txt"))
        phone.writeCompanion("notes/abc.txt", "Ask about the recipe\n")
        phone.write("Call Mom note:abc.txt\n", "Add task: Call Mom")
        assertEquals("Ask about the recipe\n", phone.readCompanion("notes/abc.txt"))
        phone.synchronizeNow()

        val desktop = device("desktop")
        desktop.synchronizeNow()
        assertEquals("Ask about the recipe\n", desktop.readCompanion("notes/abc.txt"))
        assertEquals(listOf("Add task: Call Mom"), messages("desktop"))
    }
}
