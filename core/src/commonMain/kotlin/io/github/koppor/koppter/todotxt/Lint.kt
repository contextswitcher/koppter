package io.github.koppor.koppter.todotxt

enum class Severity { ERROR, WARNING, INFO }

/**
 * Something in a file that the application does not read the way it was probably meant.
 *
 * @param line the number of the line, counted from 1; 0 for what belongs to no single line
 */
data class Finding(val line: Int, val severity: Severity, val message: String, val file: String = "todo.txt") {
    /** As compilers write it, so that editors and CI services find the line. */
    override fun toString(): String = file + (if (line > 0) ":$line" else "") + ": ${severity.name.lowercase()}: $message"
}

/** The forms in which findings are written, each with the name that the command line has for it. */
enum class LintFormat(val option: String) { ERRORFORMAT("errorformat"), GITHUB_ACTIONS("github-actions"), CSV("csv") }

/** The lines that tell the findings in the format. */
// [impl->dsn~lint-formats~1]
fun List<Finding>.written(format: LintFormat): List<String> = when (format) {
    LintFormat.ERRORFORMAT -> map(Finding::toString)
    LintFormat.GITHUB_ACTIONS -> map {
        val command = when (it.severity) {
            Severity.ERROR -> "error"
            Severity.WARNING -> "warning"
            Severity.INFO -> "notice"
        }
        // The service takes "%", line ends and, in a property, ":" and "," as its own syntax.
        val file = it.file.replace('\\', '/').escaped().replace(":", "%3A").replace(",", "%2C")
        "::$command file=$file" + (if (it.line > 0) ",line=${it.line}" else "") + "::" + it.message.escaped()
    }
    LintFormat.CSV -> listOf("file,line,severity,message") + map {
        listOf(it.file, if (it.line > 0) "${it.line}" else "", it.severity.name.lowercase(), it.message).joinToString(",") { field ->
            if (field.any { c -> c in ",\"\r\n" }) "\"" + field.replace("\"", "\"\"") + "\"" else field
        }
    }
}

private fun String.escaped() = replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")

/** The tags that the application reads, each with what its value has to be. */
private val VALUES: Map<String, Pair<String, (String) -> Any?>> = mapOf(
    Task.DUE to ("a date as YYYY-MM-DD" to Task::parseDate),
    Task.THRESHOLD to ("a date as YYYY-MM-DD" to Task::parseDate),
    Task.DEADLINE to ("a date as YYYY-MM-DD" to Task::parseDate),
    Task.RECURRENCE to ("a recurrence such as 2w or +1m" to Recurrence::parse),
    Task.DURATION to ("a duration such as 30m or 1h30m" to Task::parseDuration),
    Task.TRAVEL to ("a duration such as 30m or 1h30m" to Task::parseDuration),
    Task.AT to ("a time as HH:MM" to Task::parseTime),
    Task.BY to ("a time as HH:MM" to Task::parseTime),
)

private val KEYS = VALUES.keys + setOf(Task.NOTE, Task.ID, Task.PARENT, Task.CONTEXT_SWITCHER)

/**
 * What the application does not read from the tasks the way it was probably meant.
 * Only the lines of open tasks are checked: a completed task is planned and shown no more.
 *
 * @param noteExists whether the directory of the notes has a file of that name
 */
// [impl->dsn~lint~1]
fun lint(file: TodoFile, noteExists: (String) -> Boolean = { true }): List<Finding> {
    val findings = mutableListOf<Finding>()
    val open = file.entries.filterNot { it.task.completed }

    fun Entry.report(severity: Severity, message: String) {
        findings += Finding(index + 1, severity, message)
    }

    val ids = mutableMapOf<String, Int>()
    for (entry in file.entries) {
        for ((_, id) in entry.task.tags.filter { it.first == Task.ID }) {
            val first = ids.getOrPut(id) { entry.index + 1 }
            if (first != entry.index + 1) entry.report(Severity.ERROR, "id:$id is the id of line $first as well")
        }
    }

    for (entry in open) {
        val task = entry.task
        for ((key, value) in task.tags) {
            val (kind, parse) = VALUES[key] ?: continue
            if (parse(value) == null) entry.report(Severity.ERROR, "$key:$value is not $kind")
        }
        for (key in VALUES.keys) {
            if (task.tags.count { it.first == key } > 1) entry.report(Severity.WARNING, "$key: is given more than once; the first one counts")
        }
        // "due: 2026-11-16" is the word "due:" and a date in the description.
        for (key in KEYS) {
            if ("$key:" in task.body.split(' ')) entry.report(Severity.WARNING, "$key: has no value; a tag has no space after the colon")
        }
        for ((_, parent) in task.tags.filter { it.first == Task.PARENT }) {
            if (parent !in ids) entry.report(Severity.ERROR, "p:$parent names no task: no line has id:$parent")
        }
        task.tag(Task.NOTE)?.let { if (!noteExists(it)) entry.report(Severity.ERROR, "the note $it does not exist") }
        if (task.by != null && task.due == null) entry.report(Severity.WARNING, "by: has no effect without due:")
        val (threshold, due) = task.threshold to task.due
        if (threshold != null && due != null && threshold > due) entry.report(Severity.WARNING, "the threshold date $threshold is after the due date $due")
        val line = file.lines[entry.index]
        if (task.toLine() != line) entry.report(Severity.WARNING, "a change of this task writes the line as: ${task.toLine()}")
    }

    // "@Paper" and "@paper" are two contexts.
    for ((sign, names) in listOf<Pair<String, (Task) -> List<String>>>("@" to Task::contexts, "+" to Task::projects, "#" to Task::hashtags)) {
        val uses = open.flatMap { entry -> names(entry.task).map { it to entry } }
        for ((_, spellings) in uses.groupBy { it.first.lowercase() }) {
            val usual = spellings.groupingBy { it.first }.eachCount().maxBy { it.value }.key
            for ((name, entry) in spellings) {
                if (name != usual) entry.report(Severity.WARNING, "$sign$name is written $sign$usual elsewhere")
            }
        }
    }

    val inbox = open.filter { it.task.contexts.isEmpty() }
    if (inbox.isNotEmpty()) {
        findings += Finding(0, Severity.INFO, "open tasks without context, which are in the inbox: lines ${inbox.joinToString { "${it.index + 1}" }}")
    }
    return findings.sortedBy { it.line }
}
