package io.github.koppor.koppter.sync

import io.github.koppor.koppter.outlook.OutlookCalendar
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.TimeLog
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.todotxt.DONE_FILE
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import io.github.koppor.koppter.todotxt.TEMPLATES_FILE
import kotlinx.datetime.LocalDate
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Constants.GIT_AUTHOR_EMAIL_KEY
import org.eclipse.jgit.lib.Constants.GIT_COMMITTER_EMAIL_KEY
import org.eclipse.jgit.lib.ObjectId
import io.github.koppor.koppter.storage.defaultCommitterEmail
import io.github.koppor.koppter.storage.defaultCommitterName
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.SystemReader
import java.nio.file.Path
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.io.path.writeText

/**
 * A todo.txt file in the working tree of a git repository.
 *
 * Every change of the file is committed at once, with the change as the commit message;
 * the exchange with the remote repository runs afterwards in the background.
 * Remote changes are merged, never rebased. The todo.txt file and `ice_recur.txt` are merged line by line
 * ([mergeLines]), and of two plans the newer one is taken, so that concurrent changes on two devices do not conflict.
 */
// [impl->dsn~git-storage~1]
class GitStorage(
    private val file: Path,
    private val repository: Repository,
    private val ssh: SshSessionFactory? = null,
    /** Blank for the name of the git configuration. */
    private val committerName: String = "",
    /** Blank for the email address of the git configuration. */
    private val committerEmail: String = "",
) : SyncingStorage, CompanionFiles {
    private val git = Git(repository)
    private val files = FileStorage(file)
    private val path = repository.workTree.toPath().toRealPath().relativize(file.toAbsolutePath().normalize().let { it.parent.toRealPath().resolve(it.fileName) })
        .joinToString("/")

    /**
     * Guards the working tree and the index: commits of the user interface and merges of the background thread.
     * The storages of one repository, such as those of the lists in its directories, share both, as they share the index and the remote.
     */
    // [impl->dsn~member-lists~1]
    private val shared = SHARED.computeIfAbsent(repository.directory.canonicalFile) { Shared() }
    private val lock = shared.lock
    private val background = shared.background

    /** Companion files written since the last commit, as paths in the repository. */
    private val companions = mutableSetOf<String>()

    override var onStatus: (SyncStatus) -> Unit = {}

    // [impl->dsn~companion-files~1]
    override fun readCompanion(path: String): String? = synchronized(lock) { files.readCompanion(path) }

    override fun writeCompanion(path: String, text: String) = synchronized(lock) {
        files.writeCompanion(path, text)
        companions += inRepository(path)
        Unit
    }

    override fun readCompanionBytes(path: String): ByteArray? = synchronized(lock) { files.readCompanionBytes(path) }

    override fun writeCompanionBytes(path: String, bytes: ByteArray) = synchronized(lock) {
        files.writeCompanionBytes(path, bytes)
        companions += inRepository(path)
        Unit
    }

    override fun deleteCompanion(path: String) = synchronized(lock) {
        files.deleteCompanion(path)
        companions += inRepository(path)
        Unit
    }

    private fun inRepository(companion: String) = path.substringBeforeLast('/', "").let { if (it.isEmpty()) companion else "$it/$companion" }

    override fun read(): String = synchronized(lock) { files.read() }

    override fun version(): Any? = files.version()

    // [impl->dsn~commit-per-change~1]
    override fun write(text: String, change: String) {
        synchronized(lock) {
            files.write(text, change)
            commit(change)
        }
        sync()
    }

    override fun sync() {
        background.execute {
            onStatus(SyncStatus(SyncState.RUNNING))
            try {
                synchronize()
                onStatus(SyncStatus(SyncState.IDLE))
            } catch (e: Exception) {
                onStatus(SyncStatus(SyncState.FAILED, e.message ?: e.toString(), e))
            }
        }
    }

    /**
     * Synchronizes and waits for the result.
     * Runs on the background thread like [sync], as two synchronizations at the same time would compete for the remote.
     */
    fun synchronizeNow() {
        try {
            background.submit(::synchronize).get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    /** Commits pending changes, merges the remote branch and pushes. */
    // [impl->dsn~git-sync~1]
    private fun synchronize() {
        // A change made by another program has no description of its own.
        synchronized(lock) { commit("Update $path") }
        if (repository.remoteNames.isEmpty()) return
        val remote = if (Constants.ORIGIN in repository.remoteNames) Constants.ORIGIN else repository.remoteNames.first()

        // Twice: another device may push between our fetch and our push.
        repeat(2) {
            git.fetch().setRemote(remote).withSsh().call()
            synchronized(lock) {
                // A merge refuses a working tree with a change, and the file may have changed since the commit above.
                commit("Update $path")
                mergeRemote(remote)
            }
            if (repository.resolve("HEAD") == null) return
            val rejected = git.push().setRemote(remote).withSsh().call()
                .flatMap { it.remoteUpdates }
                .filterNot { it.status == RemoteRefUpdate.Status.OK || it.status == RemoteRefUpdate.Status.UP_TO_DATE }
            if (rejected.isEmpty()) return
            if (rejected.any { it.status != RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD }) {
                error("Push rejected: " + rejected.joinToString { it.message ?: it.status.toString() })
            }
        }
        error("Push rejected twice: the remote repository keeps changing")
    }

    // [impl->dsn~git-sync~1]
    private fun mergeRemote(remote: String) {
        val theirs = repository.resolve("refs/remotes/$remote/${repository.branch}") ?: return
        val ours = repository.resolve("HEAD")
        if (ours == null) {
            // A fresh repository without commits takes over the remote branch.
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef(theirs.name).call()
            return
        }
        val base = mergeBase(ours, theirs)
        if (base == theirs) return
        if (base == ours) {
            git.merge().include(theirs).setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call()
            return
        }

        // The files that both sides changed and that are merged here instead of by git, with their merged content.
        // A file that only one side changed is left to git, which takes that side as it is.
        val merged = mutableMapOf<String, String>()
        fun bothChanged(file: String): Triple<String?, String?, String?>? =
            Triple(base?.let { textAt(it, file) }, textAt(ours, file), textAt(theirs, file))
                .takeIf { (base, ours, theirs) -> ours != theirs && ours != base && theirs != base }
        // [impl->dsn~merge-of-companions~1]
        // [impl->dsn~time-log~1]
        // [impl->dsn~member-lists~1]
        // A merge is one for the whole repository, so it covers the lists in the other directories as well, which have storages of their own.
        val inDirectories = (filesOf(ours) + filesOf(theirs)).distinct()
        fun named(vararg names: String) = inDirectories.filter { it.substringAfterLast('/') in names }
        // [impl->dsn~archive~1]
        for (file in (listOf(path, sibling(ICE_RECUR_FILE), sibling(TimeLog.NAME), sibling(DONE_FILE)) + named(TODO_FILE, ICE_RECUR_FILE, TimeLog.NAME, DONE_FILE)).distinct()) {
            val (inBase, inOurs, inTheirs) = bothChanged(file) ?: continue
            val lines = mergeLines(inBase.toLines(), inOurs.toLines(), inTheirs.toLines())
            merged[file] = lines.joinToString("\n", postfix = if (lines.isEmpty()) "" else "\n")
        }
        // A plan is one whole: lines of two plans do not make a plan, so the plan made last is taken, and so is the mirror of Outlook written with it.
        // [impl->dsn~outlook-mirror~1]
        for (plan in (listOf(sibling(CalendarTxt.NAME)) + named(CalendarTxt.NAME)).distinct()) {
            val newerPlan = if (lastChange(theirs, plan) > lastChange(ours, plan)) theirs else ours
            for (file in listOf(plan, plan.removeSuffix(CalendarTxt.NAME) + Ics.NAME, plan.removeSuffix(CalendarTxt.NAME) + OutlookCalendar.MIRROR)) {
                if (bothChanged(file) != null) textAt(newerPlan, file)?.let { merged[file] = it }
            }
        }

        val result = git.merge().include(theirs).setCommit(false).setFastForward(MergeCommand.FastForwardMode.NO_FF).call()
        val conflicts = result.conflicts?.keys.orEmpty() - merged.keys
        if (conflicts.isNotEmpty()) {
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call()
            error("Conflicting changes in " + conflicts.joinToString())
        }
        for ((file, text) in merged) {
            repository.workTree.toPath().resolve(file).writeText(text)
            git.add().addFilepattern(file).call()
        }
        git.commit().setMessage("Merge remote changes").setIdent().call()
    }

    /** The path in the repository of a file next to the todo.txt file. */
    private fun sibling(name: String): String = path.substringBeforeLast('/', "").let { if (it.isEmpty()) name else "$it/$name" }

    /** When the file was changed last in the history of the commit, as seconds; 0 if it never was. */
    private fun lastChange(commit: ObjectId, file: String): Int =
        git.log().add(commit).addPath(file).setMaxCount(1).call().firstOrNull()?.commitTime ?: 0

    /** Commits the todo.txt file, the templates and the companion files written since the last commit, and nothing else. */
    private fun commit(message: String) {
        // [impl->dsn~templates-file~1]
        // The templates are edited in another program, so that no write of the application names them.
        val paths = listOf(path, sibling(TEMPLATES_FILE)) + companions
        git.add().apply { paths.forEach(::addFilepattern) }.call()
        // Without the update, adding does not stage a deleted file.
        git.add().setUpdate(true).apply { paths.forEach(::addFilepattern) }.call()
        val status = git.status().apply { paths.forEach(::addPath) }.call()
        val changed = status.added + status.changed + status.removed
        if (changed.isEmpty()) return
        git.commit().apply { changed.forEach(::setOnly) }.setMessage(message).setIdent().call()
        companions.clear()
    }

    // [impl->dsn~dates-from-history~1]
    override fun dates(): Map<String, TaskDates> = synchronized(lock) {
        if (repository.resolve("HEAD") == null) return emptyMap()
        val zone = ZoneId.systemDefault()
        val history = git.log().addPath(path).call().reversed().map { commit ->
            val date = commit.authorIdent.whenAsInstant.atZone(zone).toLocalDate()
            Snapshot(LocalDate(date.year, date.monthValue, date.dayOfMonth), linesAt(commit))
        }
        taskDates(history)
    }

    private fun mergeBase(a: ObjectId, b: ObjectId): RevCommit? = RevWalk(repository).use { walk ->
        walk.revFilter = RevFilter.MERGE_BASE
        walk.markStart(walk.parseCommit(a))
        walk.markStart(walk.parseCommit(b))
        walk.next()
    }

    private fun linesAt(commit: ObjectId): List<String> = textAt(commit, path).toLines()

    /** The content of a file in a commit; `null` if the commit has no such file. */
    private fun textAt(commit: ObjectId, file: String): String? = RevWalk(repository).use { walk ->
        val tree = walk.parseCommit(commit).tree
        TreeWalk.forPath(repository, file, tree)?.use { treeWalk -> repository.open(treeWalk.getObjectId(0)).bytes.decodeToString() }
    }

    /** The paths of all files of a commit. */
    private fun filesOf(commit: ObjectId): List<String> = RevWalk(repository).use { walk ->
        TreeWalk(repository).use { tree ->
            tree.addTree(walk.parseCommit(commit).tree)
            tree.isRecursive = true
            buildList { while (tree.next()) add(tree.pathString) }
        }
    }

    private fun String?.toLines(): List<String> = orEmpty().lines().dropLastWhile { it.isEmpty() }

    // [impl->dsn~committer~1]
    /** The settings come first; without them and without a configured user, as on a phone, the commits are made in the name of the application. */
    private fun org.eclipse.jgit.api.CommitCommand.setIdent() = apply {
        val config = repository.config
        val ident = PersonIdent(
            committerName.ifBlank { config.getString("user", null, "name") ?: defaultCommitterName },
            committerEmail.ifBlank { config.getString("user", null, "email") ?: defaultCommitterEmail },
        )
        setAuthor(ident).setCommitter(ident)
    }

    private fun <C : TransportCommand<C, *>> C.withSsh(): C = setTransportConfigCallback { transport ->
        if (ssh != null && transport is SshTransport) transport.sshSessionFactory = ssh
    }

    private object Constants {
        const val ORIGIN = "origin"
    }

    private class Shared {
        val lock = Any()
        val background: java.util.concurrent.ExecutorService = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "koppter-git").apply { isDaemon = true } }
    }

    companion object {
        /** By the directory of the repository; an entry stays for the life of the application, which opens few repositories. */
        private val SHARED = java.util.concurrent.ConcurrentHashMap<java.io.File, Shared>()

        init {
            // [impl->dsn~reflog-email~1]
            // Without an email address of the git user, JGit makes one up for the reflog from the name of the host,
            // which Android forbids to look up on the main thread, where a change is committed. One of the environment comes first.
            SystemReader.setInstance(object : SystemReader.Delegate(SystemReader.getInstance()) {
                override fun getenv(variable: String?): String? = super.getenv(variable)
                    ?: defaultCommitterEmail.takeIf { variable == GIT_AUTHOR_EMAIL_KEY || variable == GIT_COMMITTER_EMAIL_KEY }
            })
        }

        /** The repository whose working tree contains the file, or `null` if there is none. */
        // [impl->dsn~git-detection~1]
        fun repositoryOf(file: Path): Repository? {
            val directory = file.toAbsolutePath().normalize().parent?.toFile()?.takeIf { it.isDirectory } ?: return null
            val builder = FileRepositoryBuilder().findGitDir(directory)
            return if (builder.gitDir == null) null else builder.build().takeUnless { it.isBare }
        }
    }
}
