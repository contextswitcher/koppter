package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.Finding
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import io.github.koppor.koppter.todotxt.Severity
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.iceRecurLines
import io.github.koppor.koppter.todotxt.lint
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.system.exitProcess

/** What [lint] finds in the todo.txt file, with the notes, the schedules and the configuration next to it. */
// [impl->dsn~lint-files~1]
fun lintFiles(todo: Path): List<Finding> {
    val directory = todo.toAbsolutePath().parent
    val name = todo.fileName.toString()
    val tasks = lint(TodoFile.parse(todo.readText())) { directory.resolve(NOTES_DIRECTORY).resolve(it).isRegularFile() }.map { it.copy(file = name) }
    val schedule = directory.resolve(ICE_RECUR_FILE).takeIf { it.exists() }?.let { file ->
        iceRecurLines(file.readText()).filter { it.entry == null }.map { Finding(it.number, Severity.ERROR, "the line is not understood", ICE_RECUR_FILE) }
    }
    val config = directory.resolve(ConfigFile.NAME).takeIf { it.exists() }?.let { file ->
        ConfigFile.planConfig(file.readText()).problems.map { Finding(0, Severity.ERROR, it, ConfigFile.NAME) }
    }
    return tasks + schedule.orEmpty() + config.orEmpty()
}

/** The command line: prints the findings for the given todo.txt file and ends with 1 if an error is among them. */
// [impl->dsn~lint-files~1]
fun lintMain(args: Array<String>) {
    val todo = Path.of(args.singleOrNull() ?: "todo.txt")
    if (!Files.isRegularFile(todo)) {
        System.err.println("$todo is not a file. Usage: lint [<path of the todo.txt file>]")
        exitProcess(2)
    }
    val findings = lintFiles(todo)
    findings.forEach(::println)
    val errors = findings.count { it.severity == Severity.ERROR }
    println("$errors errors, ${findings.count { it.severity == Severity.WARNING }} warnings")
    if (errors > 0) exitProcess(1)
}
