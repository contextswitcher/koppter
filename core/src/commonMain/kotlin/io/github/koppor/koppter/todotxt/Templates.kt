package io.github.koppor.koppter.todotxt

/** The file next to todo.txt that holds the templates of tasks. */
const val TEMPLATES_FILE = "templates.txt"

/** A task of a template: its line and the text of its note, empty if it has none. */
data class TemplateTask(val line: String, val note: String)

/** A set of tasks that are added together, such as everything to do before a journey. */
data class Template(val name: String, val tasks: List<TemplateTask>)

private val HEADING = Regex("^# +(.+)$")

/**
 * Reads `templates.txt`: a line `# name` starts a template, each further line that does not start with a blank is a task of it,
 * and the indented lines below a task are its note, without the indentation of its first line.
 * Lines before the first heading, and blank lines between tasks, are ignored.
 */
// [impl->dsn~templates~1]
fun parseTemplates(text: String): List<Template> {
    val templates = mutableListOf<Pair<String, MutableList<Pair<String, MutableList<String>>>>>()
    for (line in text.replace("\r\n", "\n").lines()) {
        val heading = HEADING.matchEntire(line.trimEnd())
        when {
            heading != null -> templates += heading.groupValues[1].trim() to mutableListOf()
            templates.isEmpty() -> {}
            line.isBlank() -> templates.last().second.lastOrNull()?.second?.add("")
            line[0].isWhitespace() -> templates.last().second.lastOrNull()?.second?.add(line)
            else -> templates.last().second += line.trim() to mutableListOf()
        }
    }
    return templates.map { (name, tasks) ->
        Template(name, tasks.map { (line, note) ->
            val indent = note.firstOrNull { it.isNotBlank() }?.takeWhile { it.isWhitespace() }.orEmpty()
            TemplateTask(line, note.joinToString("\n") { it.removePrefix(indent) }.trim('\n').let { if (it.isEmpty()) it else "$it\n" })
        })
    }
}

/** An item of a Markdown task list in a note: its line, whether it is checked, and its text. */
data class ChecklistItem(val line: Int, val checked: Boolean, val text: String)

private val ITEM = Regex("^(\\s*[-*+] \\[)([ xX])(] ?)(.*)$")

/** The items `- [ ] …` and `- [x] …` of the note, at any depth. */
// [impl->dsn~note-checklist~1]
fun checklist(note: String): List<ChecklistItem> = note.lines().mapIndexedNotNull { i, line ->
    ITEM.matchEntire(line)?.let { ChecklistItem(i, it.groupValues[2] != " ", it.groupValues[4]) }
}

/** The note with the item on [line] checked or unchecked; every other character stays as it is. */
// [impl->dsn~note-checklist~1]
fun toggleItem(note: String, line: Int): String = note.split("\n").mapIndexed { i, text ->
    if (i != line) text else ITEM.matchEntire(text)?.destructured?.let { (start, mark, end, rest) -> start + (if (mark == " ") "x" else " ") + end + rest } ?: text
}.joinToString("\n")
