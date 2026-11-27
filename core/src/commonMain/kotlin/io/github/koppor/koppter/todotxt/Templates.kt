package io.github.koppor.koppter.todotxt

/** The file next to todo.txt that holds the templates of tasks. */
const val TEMPLATES_FILE = "templates.txt"

/** A task of a template: its line and the text of its note, empty if it has none. */
data class TemplateTask(val line: String, val note: String)

/** A set of tasks that are added together, such as everything to do before a journey. */
data class Template(val name: String, val tasks: List<TemplateTask>)

private val HEADING = Regex("^(#{1,6}) +(.+?)(?: +#+)?$")
private val BREAK = Regex("^(?:-{3,}|\\*{3,}|_{3,})$")
private val ITEM_MARK = Regex("^[-*+] +")

/**
 * Reads `templates.txt`, which is Markdown as well: a heading `## name` starts a template, and a file without such a heading
 * takes `# name` instead, so that a file of one title `# …` and its templates `## …` passes a linter that allows one title.
 * Each further line that does not start with a blank is a task of it in the notation of todo.txt, as a list item or as a plain line,
 * and the indented lines below a task are its note, without the indentation of its first line.
 * Lines outside of a template, blank lines between tasks and separators such as `---` are ignored.
 */
// [impl->dsn~templates~1]
fun parseTemplates(text: String): List<Template> {
    val lines = text.replace("\r\n", "\n").lines()
    val level = if (lines.any { HEADING.matchEntire(it.trimEnd())?.groupValues?.get(1) == "##" }) 2 else 1
    val templates = mutableListOf<Pair<String, MutableList<Pair<String, MutableList<String>>>>>()
    // Whether the lines belong to the last template: a heading of another level, such as the title, ends it.
    var inTemplate = false
    for (line in lines) {
        val heading = HEADING.matchEntire(line.trimEnd())
        when {
            heading != null -> {
                inTemplate = heading.groupValues[1].length == level
                if (inTemplate) templates += heading.groupValues[2].trim() to mutableListOf()
            }
            !inTemplate || BREAK.matches(line.trim()) -> {}
            line.isBlank() -> templates.last().second.lastOrNull()?.second?.add("")
            line[0].isWhitespace() -> templates.last().second.lastOrNull()?.second?.add(line)
            else -> templates.last().second += line.trim().replaceFirst(ITEM_MARK, "") to mutableListOf()
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
