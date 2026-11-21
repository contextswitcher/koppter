package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SharedLists
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.storage.TodoStorage
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter
import org.apache.sshd.common.keyprovider.KeyPairProvider
import org.apache.sshd.common.util.io.PathUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Git access for a device without an SSH configuration of its own, such as a phone:
 * a key pair and the known hosts in a directory of the application, and a clone of the repository next to it.
 *
 * @param ownKey `false` on a device with an SSH configuration of the user: the clone then uses that one, as [storageForFile] does.
 */
class GitSetup(private val directory: Path, private val ownKey: Boolean = true) {
    private val sshDirectory = directory.resolve("ssh")
    private val privateKey = sshDirectory.resolve("id_ecdsa")
    private val publicKeyFile = sshDirectory.resolve("id_ecdsa.pub")
    val repositoryDirectory: Path = directory.resolve("repository")

    init {
        // [impl->dsn~ssh-home~1]
        // The SSH library asks for the home folder of the user when it is loaded and fails where there is none, as on Android.
        if (System.getProperty("user.home").isNullOrEmpty()) PathUtils.setUserHomeFolderResolver { directory }
    }

    /**
     * The storage for the file, as [storageForFile], but with the SSH key and the known hosts of the application where it has one.
     * A file in a clone of the application whose clone is gone cannot be read: the address to clone it again is not known.
     */
    // [impl->dsn~missing-clone~1]
    fun storageFor(file: Path, committerName: String = "", committerEmail: String = ""): TodoStorage {
        val absolute = file.toAbsolutePath().normalize()
        val inClone = listOf(repositoryDirectory, sharedDirectory).any { absolute.startsWith(it.toAbsolutePath().normalize()) }
        if (inClone && GitStorage.repositoryOf(absolute) == null) return MissingClone(absolute)
        return storageForFile(file, if (ownKey) ssh else null, committerName, committerEmail)
    }

    /**
     * The storage of the todo.txt file of the repository at the address, in the clone of the application.
     * The settings keep the address, not the location of the clone, so that a clone that is gone, as after a new installation, is cloned again:
     * in the background, with the first synchronization; until then there are no tasks, and writing fails.
     */
    // [impl->dsn~clone-by-address~1]
    fun storageForAddress(url: String, committerName: String = "", committerEmail: String = ""): TodoStorage =
        ClonedStorage(url, repositoryDirectory, { clone(url) }) { storageForFile(repositoryDirectory.resolve(TODO_FILE), if (ownKey) ssh else null, committerName, committerEmail) }

    /** As [storageForAddress], for the repository of a shared list, whose clone lies where [cloneShared] puts it. */
    // [impl->dsn~shared-list-clone~1]
    fun storageForSharedAddress(url: String, committerName: String = "", committerEmail: String = ""): TodoStorage {
        val clone = sharedDirectoryFor(url)
        return ClonedStorage(url, clone, { cloneShared(url) }) { storageForFile(clone.resolve(TODO_FILE), if (ownKey) ssh else null, committerName, committerEmail) }
    }

    /**
     * The storage of an entry of the shared lists of the settings: a todo.txt file, or else the address of a repository.
     * The rule is that of [SharedLists.name], which names the list.
     */
    // [impl->dsn~shared-list-clone~1]
    fun sharedListStorage(entry: String, committerName: String = "", committerEmail: String = ""): TodoStorage =
        if (entry.endsWith(".txt")) storageFor(Path.of(entry), committerName, committerEmail) else storageForSharedAddress(entry, committerName, committerEmail)

    /**
     * The lists of the settings together with those that need no entry there: the todo.txt files of the directories
     * next to the main file and next to the file of each shared list ([memberFiles]), each behind the list that it lies in.
     *
     * @param mainFile the main file where it is a file of the file system; `null` for a document, which has no directories to look into
     */
    // [impl->dsn~member-lists~1]
    fun listsWithMembers(sharedLists: String, mainFile: Path?): String {
        fun members(file: Path?) = file?.let(::memberFiles).orEmpty().map { it.toString() }
        // The rule of [sharedListStorage] tells a file from an address.
        fun fileOf(entry: String): Path? = if (entry.endsWith(".txt")) runCatching { Path.of(entry) }.getOrNull() else sharedDirectoryFor(entry).resolve(TODO_FILE)
        return (members(mainFile) + SharedLists.entries(sharedLists).flatMap { listOf(it) + members(fileOf(it)) }).joinToString("\n")
    }

    /** The todo.txt file of the clone of the main repository. */
    val repositoryFile: Path get() = repositoryDirectory.resolve(TODO_FILE)

    /**
     * Settings of an earlier version name clones instead of the addresses of their repositories;
     * they get the addresses, which survive the clones. The same settings if there is nothing to change.
     */
    // [impl->dsn~clone-by-address~1]
    fun withAddresses(settings: Settings): Settings = settings.copy(
        todoFile = addressOf(settings.todoFile) ?: settings.todoFile,
        sharedLists = SharedLists.entries(settings.sharedLists).joinToString("\n") { sharedAddressOf(it) ?: it },
    ).takeIf { it.todoFile != settings.todoFile || SharedLists.entries(it.sharedLists) != SharedLists.entries(settings.sharedLists) } ?: settings

    /** The address of the repository whose clone of a shared list holds the file; `null` for any other file. */
    // [impl->dsn~shared-list-clone~1]
    fun sharedAddressOf(file: String): String? {
        val clone = runCatching { Path.of(file).toAbsolutePath().normalize().parent }.getOrNull() ?: return null
        return clone.takeIf { it.parent == sharedDirectory.toAbsolutePath().normalize() }?.let(::remoteOf)
    }

    /** The address of the repository whose clone of the application holds the file; `null` for any other file. */
    // [impl->dsn~clone-by-address~1]
    fun addressOf(file: String): String? =
        file.takeIf { it.isNotBlank() && Path.of(it).toAbsolutePath().normalize() == repositoryDirectory.resolve(TODO_FILE).toAbsolutePath().normalize() }
            ?.let { remoteOf(repositoryDirectory) }

    /** SSH with the key and the known hosts of the application instead of those in `~/.ssh`. */
    internal val ssh: SshSessionFactory by lazy {
        prepare()
        SshdSessionFactoryBuilder()
            .setPreferredAuthentications("publickey")
            .setHomeDirectory(directory.toFile())
            .setSshDirectory(sshDirectory.toFile())
            .build(null)
    }

    /**
     * The public key in the form `ecdsa-sha2-nistp256 AAAA… koppter`, to be registered with the git server.
     * The key pair is created on first use.
     */
    // [impl->dsn~ssh-key~1]
    fun publicKey(): String {
        prepare()
        return publicKeyFile.readText().trim()
    }

    /** SSH of the application where it has a key of its own, and the SSH configuration of the user otherwise. */
    private val transport = TransportConfigCallback { if (ownKey && it is SshTransport) it.sshSessionFactory = ssh }

    /**
     * Asks the server for the branches of the repository and fails as a clone would: the server is not reached, or it does not let this device in.
     * Nothing is cloned.
     */
    // ponytail: reading only; a key without write access passes here and fails at the first push. Add a push of nothing if that is seen.
    // [impl->dsn~clone~1]
    fun check(url: String) {
        Git.lsRemoteRepository().setRemote(url).setTransportConfigCallback(transport).call()
    }

    private fun prepare() {
        if (privateKey.exists()) return
        Files.createDirectories(sshDirectory)
        // ECDSA rather than Ed25519: every supported Android version can create it without an additional library.
        val pair = KeyUtils.generateKeyPair(KeyPairProvider.ECDSA_SHA2_NISTP256, 256)
        privateKey.outputStream().use { OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(pair, COMMENT, null, it) }
        publicKeyFile.outputStream().use { OpenSSHKeyPairResourceWriter.INSTANCE.writePublicKey(pair, COMMENT, it) }
        // [impl->dsn~host-keys~1]
        sshDirectory.resolve("config").writeText("Host *\n    StrictHostKeyChecking accept-new\n")
        // JGit records a new host key only in an existing file; it would ask before creating one.
        sshDirectory.resolve("known_hosts").writeText("")
    }

    /**
     * Clones the repository and returns the location of its todo.txt file.
     * A previous clone is kept if it is of the same repository and replaced otherwise.
     */
    // [impl->dsn~clone~1]
    fun clone(url: String, fileName: String = TODO_FILE): Path = synchronized(cloning) {
        if (remoteOf(repositoryDirectory) != url) {
            if (repositoryDirectory.exists() && repositoryDirectory.listDirectoryEntries().isNotEmpty()) {
                repositoryDirectory.toFile().deleteRecursively()
            }
            cloneInto(repositoryDirectory, url)
        }
        repositoryDirectory.resolve(fileName)
    }

    /**
     * Clones the repository of a shared list and returns the location of its todo.txt file.
     * The clone lies in a directory named after the repository, as that name is the context of the list;
     * a clone of the same repository is kept, and a directory that holds something else gets a number appended instead of being replaced.
     */
    // [impl->dsn~shared-list-clone~1]
    fun cloneShared(url: String): Path = synchronized(cloning) {
        val target = sharedDirectoryFor(url)
        if (remoteOf(target) != url) cloneInto(target, url)
        target.resolve(TODO_FILE)
    }

    private fun sharedDirectoryFor(url: String): Path {
        val name = url.trimEnd('/').substringAfterLast('/').substringAfterLast(':').removeSuffix(".git").ifEmpty { "list" }
        return generateSequence(1) { it + 1 }.map { sharedDirectory.resolve(if (it == 1) name else "$name-$it") }
            .first { !it.exists() || it.listDirectoryEntries().isEmpty() || remoteOf(it) == url }
    }

    /** The directory of the clones of shared lists, next to that of the main repository. */
    val sharedDirectory: Path = directory.resolve("shared")

    private fun remoteOf(clone: Path): String? = GitStorage.repositoryOf(clone.resolve("todo.txt"))?.use { it.config.getString("remote", "origin", "url") }

    private fun cloneInto(target: Path, url: String) {
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(target.toFile())
            .setTransportConfigCallback(transport)
            .call()
            .close()
    }

    /**
     * The clone of the repository at the address, cloned first if it is not there.
     * The file of a clone that has no todo.txt file is not read as empty: the repository is then not that of a list of tasks.
     */
    private inner class ClonedStorage(
        private val url: String,
        private val directory: Path,
        private val cloneIt: () -> Unit,
        private val open: () -> TodoStorage,
    ) : SyncingStorage, CompanionFiles {
        @Volatile private var clone: TodoStorage? = if (remoteOf(directory) == url) open() else null
        private val background = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "koppter-clone").apply { isDaemon = true } }

        override var onStatus: (SyncStatus) -> Unit = {}
            set(value) {
                field = value
                (clone as? SyncingStorage)?.onStatus = value
            }

        private fun cloned(): TodoStorage = clone ?: error("The repository $url is not cloned yet.")

        private fun files(): CompanionFiles? = clone as? CompanionFiles

        // [impl->dsn~missing-clone~1]
        override fun read(): String {
            val storage = clone ?: return ""
            if (!directory.resolve(TODO_FILE).exists()) error("The repository $url has no file $TODO_FILE.")
            return storage.read()
        }

        override fun write(text: String, change: String) = cloned().write(text, change)

        override fun version(): Any? = clone?.version()

        override fun sync() {
            (clone as? SyncingStorage)?.let { return it.sync() }
            background.execute {
                // A second synchronization that waited for the clone synchronizes it.
                clone?.let { return@execute (it as? SyncingStorage)?.sync() ?: Unit }
                onStatus(SyncStatus(SyncState.RUNNING, "Cloning $url"))
                try {
                    cloneIt()
                    clone = open().also { (it as? SyncingStorage)?.onStatus = onStatus }
                    onStatus(SyncStatus(SyncState.IDLE))
                } catch (e: Exception) {
                    onStatus(SyncStatus(SyncState.FAILED, "Not cloned: ${e.message ?: e}", e))
                }
            }
        }

        override fun dates() = (clone as? SyncingStorage)?.dates().orEmpty()

        override fun readCompanion(path: String) = files()?.readCompanion(path)

        override fun writeCompanion(path: String, text: String) = (cloned() as CompanionFiles).writeCompanion(path, text)

        override fun readCompanionBytes(path: String) = files()?.readCompanionBytes(path)

        override fun writeCompanionBytes(path: String, bytes: ByteArray) = (cloned() as CompanionFiles).writeCompanionBytes(path, bytes)

        override fun deleteCompanion(path: String) = (cloned() as CompanionFiles).deleteCompanion(path)
    }

    /** The file of a clone that is gone: reading it fails instead of showing no tasks, and writing does not start a file outside of a repository. */
    // [impl->dsn~missing-clone~1]
    private class MissingClone(private val file: Path) : TodoStorage {
        private fun missing(): Nothing = error("The clone of the repository of $file is missing: enter the address of the repository in the settings and clone it.")

        override fun read(): String = missing()

        override fun write(text: String, change: String) = missing()

        override fun version(): Any? = null
    }

    private companion object {
        const val COMMENT = "koppter"

        /** Clones in the same process, such as those of the application and of its widget, would replace one another's directory. */
        val cloning = Any()
    }
}
