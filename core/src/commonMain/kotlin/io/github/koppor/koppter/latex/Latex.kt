package io.github.koppor.koppter.latex

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task

/** A TODO of a LaTeX document: its text and where it stands, [file] as the document names it, relative to the directory of the main file. */
data class LatexTodo(val file: String, val line: Int, val text: String)

/**
 * The TODOs of LaTeX documents as tasks next to those of the file, as the items of Outlook are.
 * A line of the file names a TODO with the tag `tex:`, whose value is derived from its file and text, so that it survives moved lines.
 */
// [impl->dsn~latex-todos~1]
object Latex {
    /** The hashtag of a TODO; it stays in the line that is written for it. */
    const val HASHTAG = "latex"

    /**
     * The commands of the packages compared in gadr-latex-packages, by name, with the mandatory argument that holds the text of the TODO.
     * Generic names such as `\note` and `\comment` are left out: other packages define them for other purposes.
     */
    private val COMMANDS = mapOf(
        "todo" to 0, "Todo" to 0, "TODO" to 0, "FIXME" to 0, "missingfigure" to 0, "zebratodo" to 0, "snaptodo" to 0,
        "fxnote" to 0, "fxwarning" to 0, "fxerror" to 0, "fxfatal" to 0, "draftnote" to 0,
        "pdfcomment" to 0, "commentatside" to 0, "commentontext" to 1,
    )
    private val INCLUDES = setOf("input", "include", "subfile")
    private val COMMAND = Regex("\\\\([A-Za-z]+)(\\*?)")
    private val WHITESPACE = Regex("\\s+")

    /**
     * The TODOs of the document [main] and of the files it takes in with `\input`, `\include` and `\subfile`, in the order of the text.
     * [read] gives the content of a file, or `null` if there is none; a file that is taken in twice, or in a loop, is read once.
     */
    fun todos(path: String, read: (String) -> String?): List<LatexTodo> {
        // A path of Windows, which its file API also reads with slashes.
        val main = path.trim().replace('\\', '/')
        val directory = main.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
        val seen = mutableSetOf<String>()
        val result = mutableListOf<LatexTodo>()
        fun visit(name: String) {
            if (!seen.add(name)) return
            val text = read(directory + name)?.let(::withoutComments) ?: return
            var at = 0
            while (true) {
                val match = COMMAND.find(text, at) ?: break
                at = match.range.last + 1
                val command = match.groupValues[1]
                val starred = match.groupValues[2].isNotEmpty()
                if (command in INCLUDES) {
                    arguments(text, at, 1)?.let { (args, end) ->
                        at = end
                        // LaTeX adds the extension to a name without one, and resolves the name against the directory of the main file.
                        args[0].trim().takeIf { it.isNotEmpty() }?.let { visit(if (it.endsWith(".tex")) it else "$it.tex") }
                    }
                    continue
                }
                // `\fxnote*{note}{text}` marks a range of text; the note stays the first argument.
                val index = COMMANDS[command] ?: continue
                val (args, end) = arguments(text, at, index + 1 + if (starred && command.startsWith("fx")) 1 else 0) ?: continue
                at = end
                val todo = args[index].replace(WHITESPACE, " ").trim()
                if (todo.isNotEmpty()) result += LatexTodo(name, text.substring(0, match.range.first).count { it == '\n' } + 1, todo)
            }
        }
        visit(main.substringAfterLast('/'))
        return result
    }

    /** The text with each comment replaced by blanks, so that lines keep their numbers. */
    private fun withoutComments(text: String): String = text.lines().joinToString("\n") { line ->
        var i = 0
        while (i < line.length) {
            when (line[i]) {
                '\\' -> i++
                '%' -> return@joinToString line.substring(0, i)
            }
            i++
        }
        line
    }

    /** The first [count] mandatory arguments after [start], skipping optional ones in brackets; with the position after the last. */
    private fun arguments(text: String, start: Int, count: Int): Pair<List<String>, Int>? {
        val args = mutableListOf<String>()
        var i = start
        while (args.size < count) {
            while (i < text.length && text[i].isWhitespace()) i++
            if (i >= text.length) return null
            val close = when (text[i]) {
                '{' -> '}'
                '[' -> ']'
                else -> return null
            }
            val open = text[i]
            var depth = 0
            val from = i + 1
            while (i < text.length) {
                when (text[i]) {
                    '\\' -> i++
                    open -> depth++
                    close -> if (--depth == 0) break
                }
                i++
            }
            if (i >= text.length) return null
            if (open == '{') args += text.substring(from, i)
            i++
        }
        return args to i
    }

    /** The value of the tag `tex:` for a TODO: a hash of its file and its text, so that it stays when the lines around it change. */
    // ponytail: two TODOs with the same text in one file share the tag; number them when that occurs.
    fun tag(todo: LatexTodo): String = "${todo.file}\n${todo.text}".hashCode().toUInt().toString(36)

    /** The TODO as a task: its text, the project, the hashtag [HASHTAG] and the tag `tex:`. */
    fun task(todo: LatexTodo, project: String): Task {
        var text = todo.text
        // As for an item of Outlook: a text that would read as the completion mark, a priority or a date stays text.
        if (text.startsWith("x ") || LEADING.containsMatchIn(text)) text = "- $text"
        return Task(body = "$text +$project #$HASHTAG ${Task.LATEX}:${tag(todo)}")
    }

    /**
     * The TODOs of the projects as entries, with negative indexes from [firstIndex] down, which no line of the file has.
     * A TODO that a line of the file names in its tag `tex:`, open or completed, is left out: that line stands for it.
     */
    fun entries(todos: Map<String, List<LatexTodo>>, file: List<Entry>, firstIndex: Int): List<Entry> {
        val linked = file.mapNotNull { it.task.tag(Task.LATEX) }.toSet()
        return todos.flatMap { (project, list) -> list.map { task(it, project) } }
            .filter { it.tag(Task.LATEX) !in linked }
            .distinctBy { it.tag(Task.LATEX) }
            .mapIndexed { i, task -> Entry(firstIndex - i, task) }
    }

    private val LEADING = Regex("^(\\([A-Z]\\) |\\d{4}-\\d{2}-\\d{2} )")
}
