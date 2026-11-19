package io.github.koppor.koppter.storage

import io.github.koppor.koppter.sync.TaskDates

/** The place where the todo.txt file is kept. */
interface TodoStorage {
    /** The content of the file; empty if the file does not exist yet. */
    fun read(): String

    /**
     * @param change what the user did, as one line, such as `Add task: Call Mom`;
     *   a storage with a history records it there
     */
    fun write(text: String, change: String)

    /**
     * A value that changes whenever the file changes, used to notice changes made by other programs.
     * `null` if the file does not exist.
     */
    fun version(): Any?
}

/** The directory of the notes of tasks, next to the todo.txt file. */
const val NOTES_DIRECTORY = "notes"

/**
 * A storage that keeps further files next to the todo.txt file: the notes of tasks and the schedule of recurring tasks.
 * Paths are relative to the directory of the todo.txt file and use `/`.
 */
interface CompanionFiles {
    /** The content of the file, or `null` if it does not exist. */
    fun readCompanion(path: String): String?

    /** A storage with a history records the file together with the next [TodoStorage.write]. */
    fun writeCompanion(path: String, text: String)

    /** The content of a file that is no text, such as an image of a note, or `null` if it does not exist. */
    fun readCompanionBytes(path: String): ByteArray?

    /** As [writeCompanion], for a file that is no text. */
    fun writeCompanionBytes(path: String, bytes: ByteArray)

    /** Deletes the file if it exists; a storage with a history records that together with the next [TodoStorage.write]. */
    fun deleteCompanion(path: String)
}

enum class SyncState { IDLE, RUNNING, FAILED }

/** @param cause what made the synchronization fail, for the log */
data class SyncStatus(val state: SyncState, val message: String = "", val cause: Throwable? = null)

/** A storage that exchanges the file with a remote copy. */
interface SyncingStorage : TodoStorage {
    /** Called with every change of the synchronization status, from any thread. */
    var onStatus: (SyncStatus) -> Unit

    /** Starts a synchronization in the background. */
    fun sync()

    /** When the tasks were added and completed according to the history, by line of the file. */
    fun dates(): Map<String, TaskDates>
}
