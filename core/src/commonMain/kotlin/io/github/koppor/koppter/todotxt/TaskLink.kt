package io.github.koppor.koppter.todotxt

/**
 * The link to one task that refers to a page of OneNote: [oneNote] is the key of the page (see [oneNoteKey]),
 * [text] the description of the task when the link was made.
 */
data class TaskLink(val oneNote: String, val text: String) {
    /** Whether the task has a tag `onenote:` for the page of this link. */
    fun refersTo(task: Task): Boolean = task.oneNoteLinks().any { oneNoteKey(it).equals(oneNote, ignoreCase = true) }

    /**
     * The items whose task the link names: of those that refer to its page, the ones with its text,
     * or all of them when none has the text, as the task may have been edited since the link was made.
     */
    // [impl->dsn~task-link~1]
    fun <T> find(items: List<T>, task: (T) -> Task): List<T> {
        val candidates = items.filter { refersTo(task(it)) }
        return candidates.filter { task(it).description == text }.ifEmpty { candidates }
    }
}

private val TASK_LINK = Regex("^koppter:(?://)?task/?\\?(.*)$", RegexOption.IGNORE_CASE)

// OneNote writes the braces as they are; a program that passed the link on may have encoded them.
private val PAGE_ID = Regex("[?&]page-id=(?:\\{|%7B)([0-9A-Fa-f-]+)(?:\\}|%7D)", RegexOption.IGNORE_CASE)

private const val LINK_ONENOTE = "onenote"
private const val LINK_TEXT = "text"

/**
 * The key of the page that a link of OneNote leads to: the GUID of its `page-id`, which stays when the page or its section is renamed or moved,
 * or the whole link for one without it.
 */
// [impl->dsn~task-link~1]
fun oneNoteKey(link: String): String = PAGE_ID.find(link)?.groupValues?.get(1) ?: link

/** The values of the tags `onenote:` of the task, the links without their scheme, each once. */
fun Task.oneNoteLinks(): List<String> = tags.filter { it.first == Task.ONENOTE && it.second.isNotEmpty() }.map { it.second }.distinct()

/** The link to the task from the page of OneNote that [oneNote], the value of one of its tags `onenote:`, leads to. */
// [impl->dsn~task-link~1]
fun taskLink(task: Task, oneNote: String): String =
    "koppter://task?$LINK_ONENOTE=${encoded(oneNoteKey(oneNote))}&$LINK_TEXT=${encoded(task.description)}"

/** The link to a task that a text is, such as `koppter://task?onenote=…&text=…`; `null` if it is none or names no page. */
// [impl->dsn~task-link~1]
fun taskLinkOf(link: String): TaskLink? {
    val query = TASK_LINK.matchEntire(link.trim())?.groupValues?.get(1) ?: return null
    val values = linkValues(query)
    val page = values.lastOrNull { it.first == LINK_ONENOTE }?.second ?: return null
    return TaskLink(page, values.lastOrNull { it.first == LINK_TEXT }?.second.orEmpty())
}

/** A link on a page of OneNote back to a task that refers to the page: [note] is the link of OneNote with its scheme, [url] that of [taskLink]. */
data class Backlink(val note: String, val label: String, val url: String, val done: Boolean)

/**
 * The links back from the pages of OneNote to the tasks that refer to them, one per task and page: the tasks of the file,
 * and the completed tasks of [archived], the lines of `done.txt`, so that archiving a task does not take its link away.
 * A task that is in both, after an archiving that failed halfway, counts once.
 */
// [impl->dsn~onenote-backlinks~1]
fun backlinks(tasks: List<Task>, archived: List<Task>): List<Backlink> =
    (tasks + archived.filter { it.completed }).flatMap { task ->
        task.oneNoteLinks().map { link ->
            Backlink("${Task.ONENOTE}:$link", task.description.ifEmpty { Task.oneNotePage(link) ?: "Task" }, taskLink(task, link), task.completed)
        }
    }.distinctBy { it.note to it.url }
