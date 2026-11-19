package io.github.koppor.koppter.storage

import io.github.koppor.koppter.sync.TaskDates
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
 * A note goes to the list of its task, and with its task when the task moves.
 * The files next to the todo.txt file other than the notes, such as the configuration, are those of the main file.
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

    /** The files that have companion files written since their last write. */
    private val touched = mutableSetOf<Int>()

    /** Notes and their images whose task is not written yet, and with it the file that they belong to. */
    private val pending = mutableMapOf<String, Any>()

    private fun files(i: Int) = sources[i].second as? CompanionFiles

    // ponytail: the statuses arrive from the threads of the storages unsynchronized; a lost update shows a stale state until the next one.
    private val statuses = MutableList(sources.size) { SyncStatus(SyncState.IDLE) }

    override var onStatus: (SyncStatus) -> Unit = {}

    init {
        sources.forEachIndexed { i, (name, storage) ->
            (storage as? SyncingStorage)?.onStatus = { status ->
                statuses[i] = if (name.isEmpty() || status.message.isEmpty()) status else status.copy(message = "$name: ${status.message}")
                onStatus(statuses.firstOrNull { it.state == SyncState.FAILED } ?: statuses.firstOrNull { it.state == SyncState.RUNNING } ?: statuses[i])
            }
        }
    }

    override fun read(): String {
        val lines = sources.flatMapIndexed { i, (name, storage) ->
            val file = TodoFile.parse(storage.read())
            separators[i] = file.lineSeparator
            known[i] = file.lines
            if (i == 0) file.lines else file.lines.map { shown(name, it) }
        }
        return TodoFile(lines).serialize()
    }

    override fun write(text: String, change: String) {
        val parts = List(sources.size) { mutableListOf<String>() }
        // A blank line stays in the file of the line above it.
        var current = 0
        for (line in TodoFile.parse(text).lines) {
            if (line.isNotBlank()) current = owner(line)
            parts[current] += if (current == 0 || line.isBlank()) line else stripped(sources[current].first, line)
        }
        parts.forEachIndexed { i, lines -> moveNotes(i, lines.filterNot { it in known[i] }) }
        pending.keys.toList().forEach { put(0, it, pending.remove(it)!!) }
        parts.forEachIndexed { i, lines ->
            if (lines != known[i] || i in touched) {
                sources[i].second.write(TodoFile(lines, separators[i]).serialize(), change)
                known[i] = lines
            }
        }
        touched.clear()
    }

    /** Puts the notes of lines that are new in a file into that file: those not written yet, and those of another file, of a task that moved. */
    private fun moveNotes(i: Int, lines: List<String>) {
        for (line in lines) {
            val path = Task.parse(line).tag(Task.NOTE)?.let { "$NOTES_DIRECTORY/$it" } ?: continue
            val note = pending.remove(path)
            if (note is String) {
                put(i, path, note)
                noteImages(note).map { "$NOTES_DIRECTORY/$it" }.forEach { image -> pending.remove(image)?.let { put(i, image, it) } }
            } else if (files(i)?.readCompanion(path) == null) {
                // ponytail: the images of a moved note stay where they are, which the reading of an image from any file covers on this device.
                sources.indices.firstNotNullOfOrNull { files(it)?.readCompanion(path) }?.let { put(i, path, it) }
            }
        }
    }

    private fun put(i: Int, path: String, content: Any) {
        val files = files(i) ?: return
        if (content is String) files.writeCompanion(path, content) else files.writeCompanionBytes(path, content as ByteArray)
        touched += i
    }

    /** The file that a line belongs to: the first shared list whose context it has, or the main file. */
    private fun owner(line: String): Int {
        val contexts = Task.parse(line).contexts
        return sources.indices.drop(1).firstOrNull { i -> contexts.any { it.isWithin(sources[i].first) } } ?: 0
    }

    override fun version(): Any? = sources.map { it.second.version() }

    override fun readCompanion(path: String): String? =
        if (!path.startsWith("$NOTES_DIRECTORY/")) files(0)?.readCompanion(path)
        else pending[path] as? String ?: sources.indices.firstNotNullOfOrNull { files(it)?.readCompanion(path) }

    override fun readCompanionBytes(path: String): ByteArray? =
        if (!path.startsWith("$NOTES_DIRECTORY/")) files(0)?.readCompanionBytes(path)
        else pending[path] as? ByteArray ?: sources.indices.firstNotNullOfOrNull { files(it)?.readCompanionBytes(path) }

    override fun writeCompanion(path: String, text: String) = writeCompanion(path, text as Any)

    override fun writeCompanionBytes(path: String, bytes: ByteArray) = writeCompanion(path, bytes as Any)

    /**
     * A note is written where it is already; a new note waits for the line of its task, which tells its file.
     * The note names are random, so that a note of one file is not mistaken for a note of another.
     */
    private fun writeCompanion(path: String, content: Any) {
        if (!path.startsWith("$NOTES_DIRECTORY/")) return put(0, path, content)
        val holder = sources.indices.firstOrNull { files(it)?.let { files -> files.readCompanion(path) != null || files.readCompanionBytes(path) != null } == true }
        if (holder == null) pending[path] = content else put(holder, path, content)
    }

    override fun sync() = sources.forEach { (it.second as? SyncingStorage)?.sync() }

    override fun dates(): Map<String, TaskDates> = sources.flatMap { (name, storage) ->
        (storage as? SyncingStorage)?.dates().orEmpty().map { (line, dates) -> (if (name.isEmpty()) line else shown(name, line)) to dates }
    }.toMap()

    companion object {
        /** The line of a shared list as it is shown: with the context of the list, unless it has that context or a level below it. */
        fun shown(name: String, line: String): String =
            if (line.isBlank() || Task.parse(line).contexts.any { it.isWithin(name) }) line else "$line @$name"

        /** The line as it is written into the file of the list: without the context of the list, as far as the line has it. */
        fun stripped(name: String, line: String): String {
            if (line.endsWith(" @$name")) return line.dropLast(name.length + 2)
            val tokens = line.split(' ')
            val at = tokens.indexOf("@$name")
            return if (at < 0) line else (tokens.take(at) + tokens.drop(at + 1)).joinToString(" ")
        }

        /** The name of the list of a todo.txt file: the name of its directory, which is that of the repository for a clone. */
        fun name(todoFile: String): String =
            todoFile.replace('\\', '/').trimEnd('/').substringBeforeLast('/').substringAfterLast('/').replace(' ', '-')

        /**
         * The main storage with the shared lists of the settings, one todo.txt file per line; the main storage alone without any.
         * Without files next to the main file, which a document on Android lacks, there is no place for the notes, so the lists are left out.
         */
        fun of(main: TodoStorage, sharedLists: String, storageOf: (String) -> TodoStorage): TodoStorage {
            val files = sharedLists.lines().map(String::trim).filter(String::isNotEmpty)
            if (files.isEmpty() || main !is CompanionFiles) return main
            return SharedLists(main, files.map { name(it) to storageOf(it) })
        }
    }
}
