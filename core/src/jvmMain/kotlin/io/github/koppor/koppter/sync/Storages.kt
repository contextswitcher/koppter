package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.TodoStorage
import org.eclipse.jgit.transport.SshSessionFactory
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * The storage for a todo.txt file in the file system:
 * synchronized through git if the file lies in the working tree of a repository, a plain file otherwise.
 * SSH uses the configuration of the user (`~/.ssh`).
 */
fun storageForFile(file: Path, committerName: String = "", committerEmail: String = ""): TodoStorage = storageForFile(file, null, committerName, committerEmail)

// [impl->dsn~git-detection~1]
internal fun storageForFile(file: Path, ssh: SshSessionFactory?, committerName: String = "", committerEmail: String = ""): TodoStorage =
    GitStorage.repositoryOf(file)?.let { GitStorage(file, it, ssh, committerName, committerEmail) } ?: FileStorage(file)

/** The name of the todo.txt file in a clone and in the directory of a member. */
internal const val TODO_FILE = "todo.txt"

/**
 * The todo.txt files of the directories next to the given todo.txt file, in the order of their names:
 * the lists of the members of a family or of a team, which keep them in one repository.
 * Directories whose name starts with a full stop, such as `.git`, are left out.
 */
// ponytail: read when the storage is built; a directory that arrives later, with a synchronization or a clone in the background, shows after the next start.
// [impl->dsn~member-lists~1]
fun memberFiles(todoFile: Path): List<Path> {
    val directory = todoFile.toAbsolutePath().normalize().parent?.takeIf { it.isDirectory() } ?: return emptyList()
    return directory.listDirectoryEntries().filter { it.isDirectory() && !it.name.startsWith(".") }
        .map { it.resolve(TODO_FILE) }.filter { it.isRegularFile() }.sortedBy { it.parent.name }
}
