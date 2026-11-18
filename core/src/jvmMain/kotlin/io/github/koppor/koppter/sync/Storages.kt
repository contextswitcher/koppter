package io.github.koppor.koppter.sync

import io.github.koppor.koppter.storage.FileStorage
import io.github.koppor.koppter.storage.TodoStorage
import org.eclipse.jgit.transport.SshSessionFactory
import java.nio.file.Path

/**
 * The storage for a todo.txt file in the file system:
 * synchronized through git if the file lies in the working tree of a repository, a plain file otherwise.
 * SSH uses the configuration of the user (`~/.ssh`).
 */
fun storageForFile(file: Path, committerName: String = "", committerEmail: String = ""): TodoStorage = storageForFile(file, null, committerName, committerEmail)

// [impl->dsn~git-detection~1]
internal fun storageForFile(file: Path, ssh: SshSessionFactory?, committerName: String = "", committerEmail: String = ""): TodoStorage =
    GitStorage.repositoryOf(file)?.let { GitStorage(file, it, ssh, committerName, committerEmail) } ?: FileStorage(file)
