package io.github.koppor.koppter.android

import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.TodoStorage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import java.io.FileNotFoundException
import kotlinx.coroutines.runBlocking

/** The name of the todo.txt file in a folder that the user chose. */
private const val TODO_FILE = "todo.txt"

private fun PlatformFile.readText(): String = source().buffered().use { it.readString() }

// The sink of FileKit truncates, otherwise a shorter file keeps the tail of the longer one.
private fun PlatformFile.writeText(text: String) = sink().buffered().use { it.writeString(text) }

/** The modification time together with the size, as reported by the document provider. */
private fun PlatformFile.version(): Any? = if (exists()) lastModified() to size() else null

/**
 * A todo.txt file behind a document provider, such as a cloud storage app.
 * The application never sees a path, only the `content:` URI the user picked.
 */
// [impl->dsn~document-storage~1]
class DocumentStorage(private val file: PlatformFile) : TodoStorage {
    override fun read(): String = file.readText()

    override fun write(text: String, change: String) = file.writeText(text)

    override fun version(): Any? = file.version()
}

/**
 * The todo.txt file of a folder behind a document provider, together with the files next to it.
 * The user picked the folder, which gives access to all that is in it.
 */
// [impl->dsn~folder-storage~1]
class FolderStorage(private val folder: PlatformFile) : TodoStorage, CompanionFiles {
    // Resolved by name every time: with some providers, the address of a document says nothing about its name,
    // so that of a document that was created since is not known in advance.
    private fun resolve(path: String): PlatformFile = path.split('/').fold(folder) { base, name -> PlatformFile(base, name) }

    // A folder that is gone, or whose permission was withdrawn, is no empty list of tasks.
    override fun read(): String = readCompanion(TODO_FILE) ?: if (folder.exists()) "" else throw FileNotFoundException(folder.path)

    override fun write(text: String, change: String) = writeCompanion(TODO_FILE, text)

    override fun version(): Any? = resolve(TODO_FILE).version()

    override fun readCompanion(path: String): String? = resolve(path).takeIf { it.exists() }?.readText()

    override fun writeCompanion(path: String, text: String) {
        path.substringBeforeLast('/', "").ifEmpty { null }?.let { resolve(it).createDirectories() }
        resolve(path).writeText(text)
    }

    override fun readCompanionBytes(path: String): ByteArray? = resolve(path).takeIf { it.exists() }?.source()?.buffered()?.use { it.readByteArray() }

    override fun writeCompanionBytes(path: String, bytes: ByteArray) {
        path.substringBeforeLast('/', "").ifEmpty { null }?.let { resolve(it).createDirectories() }
        resolve(path).sink().buffered().use { it.write(bytes) }
    }

    override fun deleteCompanion(path: String) {
        // The deletion of FileKit suspends; the other operations of a storage block the caller as well.
        resolve(path).takeIf { it.exists() }?.let { runBlocking { it.delete() } }
    }
}
