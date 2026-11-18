package io.github.koppor.koppter.outlook

import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.io.encoding.Base64

/** An open item of the To-Do list of Outlook: a flagged mail or a task. [entryId] is the EntryID in hexadecimal digits, as Outlook gives it. */
data class OutlookItem(val entryId: String, val subject: String, val due: LocalDate?)

/**
 * The classic desktop Outlook, read and changed through another program: its To-Do list and a calendar.
 * A [calendar] names the account whose main calendar is meant; `null` for the main calendar of the default account.
 * The functions that return something block while Outlook answers.
 */
interface OutlookBridge {
    /** The open items of the To-Do lists of all accounts. */
    fun items(): List<OutlookItem>

    /** Shows the item in a window of Outlook; returns at once. */
    fun open(entryId: String)

    /** Marks the item done in Outlook; returns at once. */
    fun complete(entryId: String)

    /** Makes the blocks of koppter that start in the window those given, and leaves every other appointment alone; returns what changed, for the user. */
    fun syncBlocks(calendar: String?, window: ClosedRange<LocalDateTime>, blocks: List<OutlookBlock>): String

    /** The number of the blocks of koppter in the calendar. */
    fun countBlocks(calendar: String?): Int

    /** Deletes all blocks of koppter in the calendar, and nothing else; returns their number. */
    fun clearBlocks(calendar: String?): Int

    /** The appointments of the calendar on the days from [from] to before [to], without the blocks of koppter. */
    fun busy(calendar: String?, from: LocalDate, to: LocalDate): List<OutlookBusy>
}

// [impl->dsn~outlook-tag~1]
object Outlook {
    private val BASE64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    private val HEX = Regex("^(?:[0-9A-Fa-f]{2})+$")
    private const val ANCHOR_PREFIX = "anchor://outlook/message/"

    /** The value of the tag `ol:` for an EntryID: its bytes in Base64 for URLs, two thirds as long as the hexadecimal digits. */
    @OptIn(ExperimentalStdlibApi::class)
    fun tag(entryId: String): String = BASE64.encode(entryId.hexToByteArray())

    /** The EntryID in upper-case hexadecimal digits, as Outlook writes it, for the value of a tag `ol:`; `null` if the value is none. */
    @OptIn(ExperimentalStdlibApi::class)
    fun entryId(tag: String): String? =
        runCatching { BASE64.decode(tag) }.getOrNull()?.takeIf { it.isNotEmpty() }?.toHexString()?.uppercase()

    /** The link of AnchorHub for the item; without the store, which AnchorHub then looks up. */
    fun anchor(entryId: String): String = "$ANCHOR_PREFIX$entryId?v=1"

    /** The EntryID of a link of [anchor]; `null` for another text. */
    fun entryIdOfAnchor(link: String): String? =
        link.takeIf { it.startsWith(ANCHOR_PREFIX) }?.removePrefix(ANCHOR_PREFIX)?.substringBefore('?')?.takeIf(HEX::matches)

    /**
     * The item as a task: its subject, its due date and the tag `ol:`.
     * Line breaks and runs of blanks in the subject become single blanks, and a subject that would read as a completed task or a priority keeps that as text.
     */
    // [impl->dsn~outlook-items~1]
    fun task(item: OutlookItem): Task {
        var subject = item.subject.trim().replace(WHITESPACE, " ").ifEmpty { "(no subject)" }
        if (subject.startsWith("x ") || PRIORITY.containsMatchIn(subject) || DATE.containsMatchIn(subject)) subject = "- $subject"
        val due = item.due?.let { " ${Task.DUE}:$it" }.orEmpty()
        return Task(body = "$subject$due ${Task.OUTLOOK}:${tag(item.entryId)}")
    }

    /**
     * The items as entries next to those of the file, each with a negative index, which no line of the file has.
     * An item that a line of the file names in its tag `ol:`, open or completed, is left out: that line stands for it.
     */
    // [impl->dsn~outlook-items~1]
    fun entries(items: List<OutlookItem>, file: List<Entry>): List<Entry> {
        val linked = file.mapNotNull { it.task.tag(Task.OUTLOOK)?.let(::entryId) }.toSet()
        return items.filter { it.entryId.uppercase() !in linked }.mapIndexed { i, item -> Entry(-1 - i, task(item)) }
    }

    /** Whether the entry is an item of Outlook rather than a line of the file. */
    fun isItem(entry: Entry): Boolean = entry.index < 0

    private val WHITESPACE = Regex("\\s+")
    private val PRIORITY = Regex("^\\([A-Z]\\) ")
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2} ")
}
