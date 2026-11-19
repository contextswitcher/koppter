package io.github.koppor.koppter.latex

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task

/**
 * A TODO of a LaTeX document: its text and where it stands, [file] as the document names it, relative to the directory of the main file,
 * and [path] as it was read.
 */
data class LatexTodo(val file: String, val line: Int, val text: String, val path: String = file)

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
     * `null` if the main file cannot be read, as on a device where the document lies elsewhere.
     */
    fun todos(path: String, read: (String) -> String?): List<LatexTodo>? {
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
                if (todo.isNotEmpty()) result += LatexTodo(name, text.substring(0, match.range.first).count { it == '\n' } + 1, todo, directory + name)
            }
        }
        val name = main.substringAfterLast('/')
        if (read(directory + name) == null) return null
        visit(name)
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

    /**
     * The lines whose TODO was changed in the document, each with the task it becomes: the open lines of a project whose tag `tex:` names no TODO
     * of its document, each paired with the TODO that no line names and whose words are most like its own, if at least half of all their words are shared.
     * The task takes the text and the tag of the TODO and keeps everything else of the line.
     * Only documents that were read count: [todos] has no project whose document is missing.
     */
    // [impl->dsn~latex-rematch~1]
    // ponytail: shared words, not an edit distance; a TODO whose words were all replaced counts as a new one.
    fun rematched(todos: Map<String, List<LatexTodo>>, file: List<Entry>): List<Pair<Entry, Task>> {
        val linked = file.mapNotNull { it.task.tag(Task.LATEX) }.toSet()
        return todos.flatMap { (project, list) ->
            val current = list.map(::tag).toSet()
            val orphans = file.filter { !it.task.completed && project in it.task.projects && it.task.tag(Task.LATEX)?.let { tag -> tag !in current } == true }
            val unmatched = list.filter { tag(it) !in linked }.distinctBy(::tag)
            val pairs = orphans.flatMap { line -> unmatched.map { todo -> Triple(line, todo, similarity(words(line.task.description), words(todo.text))) } }
                .filter { it.third >= 0.5 }.sortedByDescending { it.third }
            val taken = mutableSetOf<Any>()
            pairs.mapNotNull { (line, todo, _) ->
                if (line in taken || todo in taken) return@mapNotNull null
                taken += line
                taken += todo
                line to followed(line.task, todo, project)
            }
        }
    }

    /** The line with the text and the tag of the TODO; its labels, tags, priority and dates stay. */
    private fun followed(line: Task, todo: LatexTodo, project: String): Task {
        val kept = line.body.split(' ').filter { it.isNotEmpty() && (it[0] in "+@#" || ':' in it) && !it.startsWith("${Task.LATEX}:") }
        val fresh = task(todo, project).body.split(' ')
        return line.copy(body = (fresh + kept.filter { it !in fresh }).joinToString(" "))
    }

    private fun words(text: String): Set<String> =
        text.lowercase().split(WHITESPACE).filter { it.isNotEmpty() && it[0] !in "+@#" }.map { it.trim('.', ',', ';', ':', '!', '?') }.filter { it.isNotEmpty() }.toSet()

    private fun similarity(a: Set<String>, b: Set<String>): Double = if (a.isEmpty() || b.isEmpty()) 0.0 else (a intersect b).size.toDouble() / (a union b).size

    /**
     * The program that opens a TODO, from a template of the settings such as `code --goto {file}:{line}`: its words, with `{file}` and `{line}` replaced in each.
     * Split before the replacement, so that a path with blanks stays one argument.
     */
    // [impl->dsn~latex-open~1]
    fun command(template: String, path: String, line: Int): List<String> =
        template.trim().split(WHITESPACE).filter { it.isNotEmpty() }.map { it.replace("{file}", path).replace("{line}", line.toString()) }

    private val LEADING = Regex("^(\\([A-Z]\\) |\\d{4}-\\d{2}-\\d{2} )")
}
