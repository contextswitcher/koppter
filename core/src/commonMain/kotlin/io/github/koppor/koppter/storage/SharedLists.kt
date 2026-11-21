package io.github.koppor.koppter.storage

import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.noteImages

/**
 * The main todo.txt file together with shared lists: the todo.txt files of other repositories, such as one of a household,
 * which other people work on as well.
 *
 * They are shown as one file. A task of a shared list carries the context named after the list, which is not written into its file;
 * a task with that context, or with a level below it, is written into the file of the list, any other task into the main file.
 * So a new task with the context goes to the shared list, and changing the context moves a task from one file to the other.
 * A note goes to the list of its task, and moves with its task together with its images.
 * The schedule of `ice_recur.txt` is that of all files, its entries assigned as the tasks are;
 * the other files next to the todo.txt file, such as the configuration, are those of the main file.
 *
 * A list that cannot be read, such as one whose clone is gone, shows no tasks and reports why;
 * a change that would write into it fails, and nothing is written.
 *
 * @param lists the name of each list, which is its context, and its storage
 */
// [impl->dsn~shared-lists~1]
class SharedLists(main: TodoStorage, lists: List<Pair<String, TodoStorage>>) : SyncingStorage, CompanionFiles {
    /** The main file first, without a name: it has no context of its own. */
    private val sources = listOf("" to main) + lists
    private val separators = MutableList(sources.size) { "\n" }

    /** The lines of each file as they were read or written last, to tell which lines are new in a file. */
    private val known = MutableList(sources.size) { emptyList<String>() }

    /** Why a file could not be read at the last read; `null` for a file that was read. */
    private val broken = MutableList<Exception?>(sources.size) { null }

    /** The files that have companion files written since their last write. */
    private val touched = mutableSetOf<Int>()

    /** Notes and their images whose task is not written yet, and with it the file that they belong to. */
    private val pending = mutableMapOf<String, Any>()

    private fun files(i: Int) = sources[i].second as? CompanionFiles

    /** The names of the lists, which are their contexts, in their order. */
    val names: List<String> get() = sources.drop(1).map { it.first }

    /** A file next to the todo.txt file of a list, such as its `calendar.txt`; `null` if the list has none or cannot be read. */
    // [impl->dsn~family-day~1]
    fun readCompanionOf(list: String, path: String): String? = sources.indexOfFirst { it.first == list }.takeIf { it > 0 }?.let { readable(it)?.readCompanion(path) }

    /**
     * Writes a file next to the todo.txt file of a list, which a storage with a history records as [change].
     * Fails for a list that there is not, that cannot be read or that keeps no such files.
     */
    // [impl->dsn~absences~1]
    fun writeCompanionOf(list: String, path: String, text: String, change: String) {
        val i = sources.indexOfFirst { it.first == list }.takeIf { it > 0 } ?: error("There is no list @$list")
        failIfBroken(i)
        (files(i) ?: error("The list @$list keeps no files next to its todo.txt file")).writeCompanion(path, text)
        // The storage records such a file with the next write of the todo.txt file, which stays as it is.
        sources[i].second.let { it.write(it.read(), change) }
    }

    // ponytail: the statuses arrive from the threads of the storages unsynchronized; a lost update shows a stale state until the next one.
    private val statuses = MutableList(sources.size) { SyncStatus(SyncState.IDLE) }

    override var onStatus: (SyncStatus) -> Unit = {}

    init {
        sources.forEachIndexed { i, (_, storage) -> (storage as? SyncingStorage)?.onStatus = { report(i, it) } }
    }

    private fun report(i: Int, status: SyncStatus) {
        val name = sources[i].first
        statuses[i] = if (name.isEmpty() || status.message.isEmpty()) status else status.copy(message = "@$name: ${status.message}")
        onStatus(statuses.firstOrNull { it.state == SyncState.FAILED } ?: statuses.firstOrNull { it.state == SyncState.RUNNING } ?: statuses[i])
    }

    override fun read(): String {
        val lines = sources.flatMapIndexed { i, (name, storage) ->
            val file = try {
                TodoFile.parse(storage.read()).also { broken[i] = null }
            } catch (e: Exception) {
                // The main file has no other file to show instead.
                if (i == 0) throw e
                broken[i] = e
                report(i, SyncStatus(SyncState.FAILED, e.message ?: e.toString(), e))
                TodoFile()
            }
            separators[i] = file.lineSeparator
            known[i] = file.lines
            if (i == 0) file.lines else file.lines.map { shown(name, it) }
        }
        return TodoFile(lines).serialize()
    }

    override fun write(text: String, change: String) {
        val parts = split(TodoFile.parse(text).lines) { it }
        checkWritable(parts)
        parts.forEachIndexed { i, lines -> moveNotes(i, lines.filterNot { it in known[i] }, parts) }
        pending.keys.toList().forEach { put(0, it, pending.remove(it)!!) }
        parts.forEachIndexed { i, lines ->
            if (lines != known[i] || i in touched) {
                sources[i].second.write(TodoFile(lines, separators[i]).serialize(), change)
                known[i] = lines
            }
        }
        touched.clear()
    }

    /** Fails before anything is written if a change would go into a file that could not be read. */
    private fun checkWritable(parts: List<List<String>>) = parts.forEachIndexed { i, lines -> if (lines != known[i]) failIfBroken(i) }

    private fun failIfBroken(i: Int) {
        broken[i]?.let { error("Not written: the list @${sources[i].first} cannot be read (${it.message ?: it})") }
    }

    /**
     * The lines of each file: a line that has the context of a list, or a level below it, without that context, every other line as it is.
     * A line without a task, such as a blank line or a comment, goes with the next line that has one, as it usually heads it, and at the end with the line above.
     *
     * @param task the part of a line that is the task; blank for a line without one
     */
    private fun split(lines: List<String>, task: (String) -> String): List<List<String>> {
        val owners = lines.map { line -> task(line).takeIf { it.isNotBlank() }?.let(::owner) }
        val parts = List(sources.size) { mutableListOf<String>() }
        lines.forEachIndexed { index, line ->
            val owner = owners[index] ?: owners.drop(index).firstNotNullOfOrNull { it } ?: owners.take(index).lastOrNull { it != null } ?: 0
            parts[owner] += if (owner == 0 || owners[index] == null) line else stripped(sources[owner].first, line)
        }
        return parts
    }

    /**
     * Puts the notes of lines that are new in a file into that file: those not written yet, and those of another file, of a task that moved.
     * A note that moved is deleted where it was, with its images, unless a line of that file still names it.
     */
    private fun moveNotes(i: Int, lines: List<String>, parts: List<List<String>>) {
        for (line in lines) {
            val path = Task.parse(line).tag(Task.NOTE)?.let { "$NOTES_DIRECTORY/$it" } ?: continue
            val note = pending.remove(path)
            if (note is String) {
                put(i, path, note)
                imagesOf(note).forEach { image -> pending.remove(image)?.let { put(i, image, it) } }
                continue
            }
            if (files(i)?.readCompanion(path) != null) continue
            val from = sources.indices.firstOrNull { it != i && files(it)?.readCompanion(path) != null } ?: continue
            val text = files(from)!!.readCompanion(path)!!
            put(i, path, text)
            val images = imagesOf(text).mapNotNull { image -> files(from)!!.readCompanionBytes(image)?.let { image to it } }
            images.forEach { (image, bytes) -> put(i, image, bytes) }
            if (parts[from].none { Task.parse(it).tag(Task.NOTE) == path.removePrefix("$NOTES_DIRECTORY/") }) {
                (listOf(path) + images.map { it.first }).forEach { files(from)!!.deleteCompanion(it) }
                touched += from
            }
        }
    }

    private fun imagesOf(note: String) = noteImages(note).map { "$NOTES_DIRECTORY/$it" }

    private fun put(i: Int, path: String, content: Any) {
        val files = files(i) ?: return
        if (content is String) files.writeCompanion(path, content) else files.writeCompanionBytes(path, content as ByteArray)
        touched += i
    }

    /** The file that a task belongs to: the first shared list whose context it has, or the main file. */
    private fun owner(task: String): Int {
        val contexts = Task.parse(task).contexts
        return sources.indices.drop(1).firstOrNull { i -> contexts.any { it.isWithin(sources[i].first) } } ?: 0
    }

    override fun version(): Any? = sources.map { it.second.version() }

    // [impl->dsn~shared-ice-recur~1]
    override fun readCompanion(path: String): String? = when {
        path == ICE_RECUR_FILE -> schedules().takeIf { it.any { schedule -> schedule != null } }?.let { schedules ->
            schedules.flatMapIndexed { i, schedule ->
                schedule.orEmpty().lines().dropLastWhile(String::isEmpty).map { if (i == 0) it else shownEntry(sources[i].first, it) }
            }.joinToString("\n", postfix = "\n")
        }
        path.startsWith("$NOTES_DIRECTORY/") -> pending[path] as? String ?: sources.indices.firstNotNullOfOrNull { readable(it)?.readCompanion(path) }
        else -> files(0)?.readCompanion(path)
    }

    /** The schedule of each file, without line separators of the form `\r\n`; `null` for a file without one. */
    private fun schedules(): List<String?> = sources.indices.map { readable(it)?.readCompanion(ICE_RECUR_FILE)?.replace("\r\n", "\n") }

    /** The companion files of a file that could be read. */
    private fun readable(i: Int) = files(i).takeIf { broken[i] == null }

    override fun readCompanionBytes(path: String): ByteArray? =
        if (!path.startsWith("$NOTES_DIRECTORY/")) files(0)?.readCompanionBytes(path)
        else pending[path] as? ByteArray ?: sources.indices.firstNotNullOfOrNull { readable(it)?.readCompanionBytes(path) }

    override fun writeCompanion(path: String, text: String) = writeCompanion(path, text as Any)

    override fun writeCompanionBytes(path: String, bytes: ByteArray) = writeCompanion(path, bytes as Any)

    /**
     * A note is written where it is already; a new note waits for the line of its task, which tells its file.
     * The schedule is split as the tasks are, and only the schedules that change are written.
     * The note names are random, so that a note of one file is not mistaken for a note of another.
     */
    private fun writeCompanion(path: String, content: Any) {
        if (path == ICE_RECUR_FILE) return writeSchedule(content as String)
        if (!path.startsWith("$NOTES_DIRECTORY/")) return put(0, path, content)
        val holder = sources.indices.firstOrNull { readable(it)?.let { files -> files.readCompanion(path) != null || files.readCompanionBytes(path) != null } == true }
        if (holder == null) pending[path] = content else put(holder, path, content)
    }

    // [impl->dsn~shared-ice-recur~1]
    private fun writeSchedule(text: String) {
        val before = schedules()
        val parts = split(text.replace("\r\n", "\n").lines().dropLastWhile(String::isEmpty)) { line -> line.substringAfter(" - ", "").takeUnless { line.trimStart().startsWith("#") }.orEmpty() }
        // A list that cannot be read shows no entries, so any entry for it is a change.
        parts.forEachIndexed { i, lines -> if (lines.isNotEmpty()) failIfBroken(i) }
        parts.forEachIndexed { i, lines ->
            val old = before[i]
            val new = if (lines.isEmpty()) "" else lines.joinToString("\n", postfix = "\n")
            // A file without a schedule gets none for an empty part.
            if (new != old.orEmpty() && (old != null || new.isNotEmpty())) put(i, ICE_RECUR_FILE, new)
        }
    }

    override fun deleteCompanion(path: String) {
        sources.indices.filter { readable(it)?.let { files -> files.readCompanion(path) != null || files.readCompanionBytes(path) != null } == true }
            .forEach { files(it)!!.deleteCompanion(path); touched += it }
        pending -= path
    }

    override fun sync() = sources.forEach { (it.second as? SyncingStorage)?.sync() }

    override fun dates(): Map<String, TaskDates> = sources.flatMapIndexed { i, (name, storage) ->
        val dates = if (broken[i] == null) (storage as? SyncingStorage)?.dates().orEmpty() else emptyMap()
        dates.map { (line, taskDates) -> (if (name.isEmpty()) line else shown(name, line)) to taskDates }
    }.toMap()

    companion object {
        /** The line of a shared list as it is shown: with the context of the list, unless it has that context or a level below it. */
        fun shown(name: String, line: String): String =
            if (line.isBlank() || Task.parse(line).contexts.any { it.isWithin(name) }) line else "$line @$name"

        /** An entry of the schedule of a shared list as it is shown: its task, which ends the line, with the context of the list. */
        private fun shownEntry(name: String, line: String): String {
            val task = line.substringAfter(" - ", "")
            return if (task.isBlank() || line.trimStart().startsWith("#") || Task.parse(task).contexts.any { it.isWithin(name) }) line else "$line @$name"
        }

        /** The line as it is written into the file of the list: without the context of the list, as far as the line has it. */
        fun stripped(name: String, line: String): String {
            if (line.endsWith(" @$name")) return line.dropLast(name.length + 2)
            val tokens = line.split(' ')
            val at = tokens.indexOf("@$name")
            return if (at < 0) line else (tokens.take(at) + tokens.drop(at + 1)).joinToString(" ")
        }

        /**
         * The name of a shared list, which is its context: that of the repository for its address,
         * and that of the directory of the file for a todo.txt file, which is the name of the repository for a clone.
         */
        fun name(entry: String): String {
            val text = entry.trim().replace('\\', '/').trimEnd('/')
            val name = if (text.endsWith(".txt")) text.substringBeforeLast('/').substringAfterLast('/')
            else text.substringAfterLast('/').substringAfterLast(':').removeSuffix(".git")
            return name.replace(' ', '-')
        }

        /** The entries of the setting of the shared lists: a todo.txt file or the address of a repository per line. */
        fun entries(sharedLists: String): List<String> = sharedLists.lines().map(String::trim).filter(String::isNotEmpty)

        /**
         * The main storage with the shared lists of the settings; the main storage alone without any.
         * Without files next to the main file, which a document on Android lacks, there is no place for the notes, so the lists are left out.
         *
         * @param storageOf the storage of an entry of the setting
         */
        fun of(main: TodoStorage, sharedLists: String, storageOf: (String) -> TodoStorage): TodoStorage {
            val entries = entries(sharedLists)
            if (entries.isEmpty() || main !is CompanionFiles) return main
            return SharedLists(main, entries.map { name(it) to storageOf(it) })
        }
    }
}
