package io.github.koppor.koppter.storage

import io.github.koppor.koppter.plan.PlanConfig
import io.github.koppor.koppter.plan.TimeMaps
import io.github.koppor.koppter.todotxt.BoardColumn
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.Query
import io.github.koppor.koppter.todotxt.ShareAction
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.dueDays
import io.github.koppor.koppter.todotxt.renameLabel
import io.github.koppor.koppter.todotxt.withAncestors
import kotlinx.datetime.LocalDate

/**
 * The configuration that is kept next to the todo.txt file and synchronized with it.
 *
 * It is a YAML file. The key `board-columns` maps the title of each column the user defined to its filter expression:
 *
 * ```yaml
 * board-columns:
 *   "Now": "due:<=today -@waiting"
 *   Waiting: "@waiting"
 * ```
 *
 * The key `share-actions` maps the title of each share action to its template in the same way,
 * and so do the keys `time-maps`, `context-time-maps` and `barriers` for planning.
 * The key `sideline` lists the words, each as a key with an empty value, whose events of the calendars planning ignores.
 * The key `priorities` maps the letter of a priority to the name it is shown with.
 * The key `links` maps a project, a context or a hashtag, written with its sign, to a link.
 * The key `new-task` holds what a new task starts with, under `priority` and `due`.
 * The key `project-contexts` maps a project, written with its sign, to the contexts that a task takes with it.
 */
// ponytail: reads the subset of YAML that it writes (one line per column, plain or quoted scalars, the escapes \" and \\);
// a YAML library when the file gets nested content.
// [impl->dsn~config-file~1]
object ConfigFile {
    // ponytail: one name for every todo.txt file, so two todo.txt files in one directory share it; derive it from the file name if that matters.
    const val NAME = "koppter.yml"

    private const val BOARD_COLUMNS = "board-columns:"
    private const val SHARE_ACTIONS = "share-actions:"
    const val TIME_MAPS = "time-maps:"
    const val CONTEXT_TIME_MAPS = "context-time-maps:"
    const val BARRIERS = "barriers:"
    const val LIMITS = "limits:"

    /** The words whose events leave their time to planning, each as a key without value. */
    const val SIDELINE = "sideline:"
    const val PRIORITIES = "priorities:"
    const val LINKS = "links:"
    const val PROJECT_CONTEXTS = "project-contexts:"
    private const val NEW_TASK = "new-task:"
    private const val HEADER = "# Configuration of koppter for the todo.txt file in this directory"

    /**
     * The columns of a configuration that does not have the key, so that a new user sees what columns by filter are:
     * the lists of Getting Things Done, as far as a filter tells them.
     */
    val DEFAULT_BOARD_COLUMNS = listOf(
        BoardColumn("Today", "due:<=today -@waiting"),
        BoardColumn("Next", "-@waiting -#someday"),
        BoardColumn("Waiting", "@waiting"),
        BoardColumn("Someday", "#someday"),
    )

    /**
     * The share actions of a configuration that does not have the key: the shared text alone, which the entry of the application
     * in the share sheet applies as the first action, and an example of an action that adds something, for the user to choose.
     */
    val DEFAULT_SHARE_ACTIONS = listOf(ShareAction("inbox", "{}"), ShareAction("reading", "{} +research @reading due:2w dur:1h"))

    /** What a new task starts with in a configuration that does not have the key: a middle priority and the end of the next week. */
    val DEFAULT_NEW_TASK = listOf("priority" to "C", "due" to "next weekend")

    /** What a new task starts with, as `priority` and `due`; the default if the file does not have the key. */
    // [impl->dsn~config-defaults~1]
    fun newTaskDefaults(config: String): List<Pair<String, String>> = block(config, NEW_TASK) ?: DEFAULT_NEW_TASK

    /** Without entries, the key stays, which tells that a new task starts empty. */
    fun withNewTaskDefaults(config: String, defaults: List<Pair<String, String>>): String = withBlock(config, NEW_TASK, defaults, keepEmpty = true)

    /**
     * The task that the dialog of a new task starts with.
     * The priority is given by its name or its letter; the due date by a name of [dueDays], such as `next weekend`,
     * or as a filter gives a date: `tomorrow` or an interval from today, such as `10d`.
     */
    // [impl->dsn~new-task-defaults~1]
    // ponytail: a value that is not understood is left out without a message; report it in the settings when that confuses.
    fun newTask(config: String, today: LocalDate): Task {
        val defaults = newTaskDefaults(config).toMap()
        val priority = defaults["priority"]?.let { name ->
            PriorityNames.of(entries(config, PRIORITIES)).letter(name) ?: name.singleOrNull()?.takeIf { it in 'A'..'Z' }
        }
        val due = defaults["due"]?.ifBlank { null }?.let { day -> dueDays(today).firstOrNull { it.first == day }?.second ?: Query.date(day, today) }
        return Task(body = "").edit(priority = priority).withTag(Task.DUE, due?.toString())
    }

    /** The columns in the order of the file; the default ones if the file does not have the key. */
    // [impl->dsn~config-defaults~1]
    fun boardColumns(config: String): List<BoardColumn> =
        block(config, BOARD_COLUMNS)?.map { (title, expression) -> BoardColumn(title, expression) } ?: DEFAULT_BOARD_COLUMNS

    /**
     * The configuration with the columns replaced; everything else in it stays, except comments between the columns.
     * Without columns, the key stays without entries, which tells that the user wants none instead of the default ones.
     */
    fun withBoardColumns(config: String, columns: List<BoardColumn>): String =
        withBlock(config, BOARD_COLUMNS, columns.map { it.title to it.expression }, keepEmpty = true)

    /** The share actions in the order of the file; the default ones if the file does not have the key. */
    // [impl->dsn~share-actions~1]
    // [impl->dsn~config-defaults~1]
    fun shareActions(config: String): List<ShareAction> =
        block(config, SHARE_ACTIONS)?.map { (title, template) -> ShareAction(title, template) } ?: DEFAULT_SHARE_ACTIONS

    fun withShareActions(config: String, actions: List<ShareAction>): String =
        withBlock(config, SHARE_ACTIONS, actions.map { it.title to it.template }, keepEmpty = true)

    /** The time maps, the time maps of the contexts and the barriers; see [PlanConfig]. */
    // [impl->dsn~plan-configuration~1]
    fun planConfig(config: String): PlanConfig =
        PlanConfig(TimeMaps(entries(config, TIME_MAPS)), entries(config, CONTEXT_TIME_MAPS).toMap(), entries(config, BARRIERS), entries(config, LIMITS))

    /** The entries below the key, in the order of the file. */
    fun entries(config: String, key: String): List<Pair<String, String>> = block(config, key).orEmpty()

    fun withEntries(config: String, key: String, entries: List<Pair<String, String>>): String = withBlock(config, key, entries)

    /**
     * The link of a label, which is written with its sign, such as `+Fair` or `@phone`: its own, or that of the nearest level above it.
     *
     * @param links the entries of the key `links`
     */
    // [impl->dsn~label-links~1]
    fun link(links: Map<String, String>, label: String): String? =
        label.drop(1).withAncestors().asReversed().firstNotNullOfOrNull { links[label.take(1) + it]?.ifBlank { null } }

    /**
     * The contexts that a task takes with the project: those of the project and of each level above it, without their sign.
     *
     * @param entries the entries of the key `project-contexts`
     */
    // [impl->dsn~project-contexts~1]
    fun contextsOf(entries: Map<String, String>, project: String): List<String> =
        project.withAncestors().flatMap { entries["+$it"].orEmpty().split(' ', ',') }.map { it.removePrefix("@") }.filter { it.isNotEmpty() }.distinct()

    /**
     * The configuration with the label renamed in the links and in the contexts of projects, as [renameLabel] renames it in a task;
     * the configuration itself if it has the label in neither.
     */
    // [impl->dsn~label-rename~1]
    fun renamed(config: String, sign: Char, from: String, to: String): String {
        fun String.renamed() = renameLabel(this, sign, from, to)
        fun String.with(key: String, rename: (Pair<String, String>) -> Pair<String, String>) =
            entries(this, key).let { before -> before.map(rename).let { if (it == before) this else withEntries(this, key, it) } }
        return config.with(LINKS) { (label, link) -> label.renamed() to link }.with(PROJECT_CONTEXTS) { (project, contexts) ->
            // The contexts with their sign and separated by blanks, which is how a task has them; as they were typed if none is renamed.
            val written = contexts.split(' ', ',').filter { it.isNotEmpty() }.joinToString(" ") { "@" + it.removePrefix("@") }
            project.renamed() to written.renamed().let { if (it == written) contexts else it }
        }
    }

    /** Reads entries as the dialogs show them: one per line as `name = value`; lines without a name are skipped. */
    fun parseLines(text: String): List<Pair<String, String>> = text.lines().mapNotNull { line ->
        val name = line.substringBefore('=', "").trim()
        if (name.isEmpty()) null else name to line.substringAfter('=').trim()
    }

    fun toLines(entries: List<Pair<String, String>>): String = entries.joinToString("\n") { "${it.first} = ${it.second}" }

    /** The entries below the key; `null` if the configuration does not have the key. */
    private fun block(config: String, key: String): List<Pair<String, String>>? {
        val lines = config.lines()
        val start = lines.indexOfFirst { it.trimEnd() == key }
        if (start < 0) return null
        return lines.drop(start + 1).takeWhile(::inBlock).mapNotNull(::entry)
    }

    /** @param keepEmpty keeps the key if there are no entries, instead of removing it */
    private fun withBlock(config: String, key: String, entries: List<Pair<String, String>>, keepEmpty: Boolean = false): String {
        // A title is a key and therefore unique.
        val block = entries.distinctBy { it.first }.map { "  ${quoted(it.first)}: ${quoted(it.second)}" }
            .let { if (it.isEmpty() && !keepEmpty) it else listOf(key) + it }
        val lines = config.lines()
        val start = lines.indexOfFirst { it.trimEnd() == key }
        if (start < 0) {
            if (block.isEmpty()) return config
            return (if (config.isBlank()) "$HEADER\n" else config.trimEnd() + "\n\n") + block.joinToString("\n") + "\n"
        }
        val end = start + 1 + lines.drop(start + 1).takeWhile(::inBlock).dropLastWhile { !it.startsWith(" ") }.size
        return (lines.take(start) + block + lines.drop(end)).joinToString("\n")
    }

    private fun inBlock(line: String) = line.isBlank() || line.startsWith(" ") || line.startsWith("#")

    private fun entry(line: String): Pair<String, String>? {
        val text = line.trim()
        if (text.isEmpty() || text.startsWith("#")) return null
        val (title, rest) = scalar(text, key = true) ?: return null
        if (!rest.startsWith(":")) return null
        val (expression, _) = scalar(rest.substring(1).trim(), key = false) ?: return null
        return title to expression
    }

    private val DOUBLE_QUOTED = Regex("^\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val SINGLE_QUOTED = Regex("^'((?:[^']|'')*)'")
    private val ESCAPE = Regex("\\\\(.)")
    private val END_OF_PLAIN_KEY = Regex(":(\\s|$)|\\s#")
    private val END_OF_PLAIN_VALUE = Regex("\\s#")

    /** Reads the scalar at the start of the text; returns its value and the text after it. */
    private fun scalar(text: String, key: Boolean): Pair<String, String>? = when {
        text.startsWith("\"") -> DOUBLE_QUOTED.find(text)?.let { match ->
            match.groupValues[1].replace(ESCAPE) { it.groupValues[1] } to text.substring(match.value.length).trimStart()
        }
        text.startsWith("'") -> SINGLE_QUOTED.find(text)?.let { match ->
            match.groupValues[1].replace("''", "'") to text.substring(match.value.length).trimStart()
        }
        // In YAML, a number sign at the start is a comment: a filter that starts with a hashtag has to be quoted.
        text.startsWith("#") -> "" to ""
        else -> {
            // A plain scalar ends before a comment and, as a key, before the colon that separates it from the value.
            val end = (if (key) END_OF_PLAIN_KEY else END_OF_PLAIN_VALUE).find(text)?.range?.first ?: text.length
            text.substring(0, end).trimEnd() to text.substring(end).trimStart()
        }
    }

    // Always quoted, as a filter may start with a character that YAML reserves, such as `@`.
    private fun quoted(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
