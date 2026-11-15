package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.Finding
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import io.github.koppor.koppter.todotxt.LintFormat
import io.github.koppor.koppter.todotxt.Severity
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.iceRecurLines
import io.github.koppor.koppter.todotxt.lint
import io.github.koppor.koppter.todotxt.written
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.system.exitProcess

/**
 * What [lint] finds in the todo.txt file, with the notes, the schedules and the configuration next to it.
 * The findings name the files by their paths as the path of the todo.txt file gives them, so that an editor or a service finds them.
 */
// [impl->dsn~lint-files~1]
fun lintFiles(todo: Path): List<Finding> {
    val directory = todo.toAbsolutePath().parent
    val tasks = lint(TodoFile.parse(todo.readText())) { directory.resolve(NOTES_DIRECTORY).resolve(it).isRegularFile() }.map { it.copy(file = todo.toString()) }
    val schedule = directory.resolve(ICE_RECUR_FILE).takeIf { it.exists() }?.let { file ->
        val name = todo.resolveSibling(ICE_RECUR_FILE).toString()
        iceRecurLines(file.readText()).filter { it.entry == null }.map { Finding(it.number, Severity.ERROR, "the line is not understood", name) }
    }
    val config = directory.resolve(ConfigFile.NAME).takeIf { it.exists() }?.let { file ->
        ConfigFile.planConfig(file.readText()).problems.map { Finding(0, Severity.ERROR, it, todo.resolveSibling(ConfigFile.NAME).toString()) }
    }
    return tasks + schedule.orEmpty() + config.orEmpty()
}

/** The format and the path that the arguments of the command line give; `null` if they are not understood. */
// [impl->dsn~lint-formats~1]
fun lintOptions(args: List<String>): Pair<LintFormat, String>? {
    var format = LintFormat.ERRORFORMAT
    val paths = mutableListOf<String>()
    val rest = args.iterator()
    for (arg in rest) {
        when {
            arg == "--output-format" || arg.startsWith("--output-format=") -> {
                val name = if ('=' in arg) arg.substringAfter('=') else if (rest.hasNext()) rest.next() else return null
                format = LintFormat.entries.firstOrNull { it.option == name.lowercase() } ?: return null
            }
            arg.startsWith("-") -> return null
            else -> paths += arg
        }
    }
    return if (paths.size > 1) null else format to (paths.firstOrNull() ?: "todo.txt")
}

/**
 * The command line: prints the findings for the given todo.txt file and ends with 1 if an error is among them.
 * The count goes to the error stream, so that the output holds the findings only.
 */
// [impl->dsn~lint-files~1]
fun lintMain(args: Array<String>) {
    val (format, path) = lintOptions(args.toList()) ?: run {
        System.err.println("Usage: lint [--output-format ${LintFormat.entries.joinToString("|") { it.option }}] [<path of the todo.txt file>]")
        exitProcess(2)
    }
    val todo = Path.of(path)
    if (!Files.isRegularFile(todo)) {
        System.err.println("$todo is not a file")
        exitProcess(2)
    }
    val findings = lintFiles(todo)
    findings.written(format).forEach(::println)
    val errors = findings.count { it.severity == Severity.ERROR }
    System.err.println("$errors errors, ${findings.count { it.severity == Severity.WARNING }} warnings")
    if (errors > 0) exitProcess(1)
}
