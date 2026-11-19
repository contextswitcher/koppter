package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitSetupTest {
    private val root = Files.createTempDirectory("koppter-setup")
    private val setup = GitSetup(root.resolve("app"))

    // [utest->dsn~ssh-key~1]
    @Test
    fun theKeyPairIsCreatedOnceAndThePublicKeyIsInOpenSshForm() {
        val key = setup.publicKey()
        assertTrue(key.startsWith("ecdsa-sha2-nistp256 AAAA"), key)
        assertTrue(key.endsWith(" koppter"), key)
        assertEquals(key, GitSetup(root.resolve("app")).publicKey())
        assertTrue(root.resolve("app/ssh/id_ecdsa").readText().startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
    }

    // [utest->dsn~host-keys~1]
    @Test
    fun unknownHostsAreAcceptedOnFirstContact() {
        setup.publicKey()
        assertTrue("StrictHostKeyChecking accept-new" in root.resolve("app/ssh/config").readText())
    }

    // [utest->dsn~clone~1]
    @Test
    fun clonesTheRepositoryAndKeepsAnExistingCloneOfTheSameRepository() {
        val origin = root.resolve("origin")
        Git.init().setInitialBranch("main").setDirectory(origin.toFile()).call().use { git ->
            origin.resolve("todo.txt").writeText("Call Mom\n")
            git.add().addFilepattern("todo.txt").call()
            git.commit().setMessage("Add task: Call Mom").call()
        }
        val url = origin.toUri().toString()

        val file = setup.clone(url)
        assertEquals("Call Mom\n", file.readText())

        file.writeText("Call Mom\nNot yet committed\n")
        assertEquals(file, setup.clone(url))
        assertEquals("Call Mom\nNot yet committed\n", file.readText())

        // With the SSH configuration of the user, the application creates no key.
        assertEquals("Call Mom\n", GitSetup(root.resolve("desktop"), ownKey = false).clone(url).readText())
        assertFalse(root.resolve("desktop/ssh").exists())
    }

    private fun origin(name: String, files: Map<String, String> = mapOf("todo.txt" to "Call Mom\n")) = root.resolve(name).also { origin ->
        Git.init().setInitialBranch("main").setDirectory(origin.toFile()).call().use { git ->
            files.forEach { (file, text) -> origin.resolve(file).writeText(text) }
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Add files").call()
        }
    }.toUri().toString()

    /** Synchronizes and waits until the storage reports that it is done. */
    private fun syncAndWait(storage: SyncingStorage): SyncStatus {
        val done = java.util.concurrent.LinkedBlockingQueue<SyncStatus>()
        storage.onStatus = { if (it.state != SyncState.RUNNING) done += it }
        storage.sync()
        return done.poll(30, java.util.concurrent.TimeUnit.SECONDS)!!
    }

    // [utest->dsn~clone-by-address~1]
    @Test
    fun aRepositoryGivenByItsAddressIsClonedAgainWhenItsCloneIsGone() {
        val url = origin("by-address")
        setup.clone(url)
        assertEquals(url, setup.addressOf(setup.repositoryDirectory.resolve("todo.txt").toString()))
        assertNull(setup.addressOf(root.resolve("todo.txt").toString()))

        // As after a new installation that restored the settings, but not the clone.
        setup.repositoryDirectory.toFile().deleteRecursively()
        val storage = setup.storageForAddress(url) as SyncingStorage
        assertEquals("", storage.read())
        assertFailsWith<IllegalStateException> { storage.write("Call Dad\n", "Add task: Call Dad") }
        assertEquals(SyncState.IDLE, syncAndWait(storage).state)
        assertEquals("Call Mom\n", storage.read())
        assertTrue(storage.version() != null)

        // An existing clone is used at once.
        assertEquals("Call Mom\n", setup.storageForAddress(url).read())
    }

    // [utest->dsn~missing-clone~1]
    @Test
    fun aMissingCloneOrTodoFileFailsInsteadOfShowingNoTasks() {
        val file = setup.repositoryDirectory.resolve("todo.txt")
        assertFailsWith<IllegalStateException> { setup.storageFor(file).read() }
        // A file outside of the clones is a new file.
        assertEquals("", setup.storageFor(root.resolve("todo.txt")).read())

        val url = origin("no-todo", mapOf("README.md" to "Notes\n"))
        val storage = setup.storageForAddress(url) as SyncingStorage
        assertEquals(SyncState.IDLE, syncAndWait(storage).state)
        assertFailsWith<IllegalStateException> { storage.read() }
    }

    // [utest->dsn~shared-list-clone~1]
    @Test
    fun clonesASharedListIntoADirectoryNamedAfterTheRepository() {
        fun origin(name: String) = root.resolve(name).resolve("household.git").also { origin ->
            Git.init().setInitialBranch("main").setDirectory(origin.toFile()).call().use { git ->
                origin.resolve("todo.txt").writeText("Buy milk\n")
                git.add().addFilepattern("todo.txt").call()
                git.commit().setMessage("Add task: Buy milk").call()
            }
        }
        val first = origin("a").toUri().toString()
        val second = origin("b").toUri().toString()

        val file = setup.cloneShared(first)
        assertEquals(setup.sharedDirectory.resolve("household/todo.txt"), file)
        file.writeText("Buy milk\nNot yet committed\n")
        assertEquals(file, setup.cloneShared(first))
        assertEquals("Buy milk\nNot yet committed\n", file.readText())

        // Another repository of the same name does not replace the clone.
        assertEquals(setup.sharedDirectory.resolve("household-2/todo.txt"), setup.cloneShared(second))
        assertEquals("Buy milk\nNot yet committed\n", file.readText())
        // The main clone is another directory.
        assertEquals("Buy milk\n", setup.clone(first).readText())
        assertEquals("Buy milk\nNot yet committed\n", file.readText())
    }
}
