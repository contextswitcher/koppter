package io.github.koppor.koppter

import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.outlook.BusyStatus
import io.github.koppor.koppter.outlook.OutlookBlock
import io.github.koppor.koppter.outlook.OutlookBridge
import io.github.koppor.koppter.outlook.OutlookBusy
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.tinylog.Logger
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile

/**
 * The To-Do list of the classic Outlook through `ankd.exe` of AnchorHub, which talks to Outlook over COM.
 * Each call starts the program once.
 */
// [impl->adr~outlook-through-anchorhub~1]
// [impl->dsn~outlook-items~1]
internal class AnchorHubOutlook(private val ankd: Path) : OutlookBridge {
    override fun items(): List<OutlookItem> = parseOutlookTodos(run("outlook-todos"))

    override fun open(entryId: String) = inBackground("outlook-open", entryId)

    // [impl->dsn~outlook-complete~1]
    override fun complete(entryId: String) = inBackground("outlook-complete", entryId)

    /** Runs the command for the item on a thread of its own, so that the window does not wait for Outlook; a failure is logged. */
    private fun inBackground(command: String, entryId: String) {
        thread(isDaemon = true, name = command) {
            try {
                run(command, Outlook.anchor(entryId))
            } catch (e: Exception) {
                Logger.error(e, "AnchorHub $command failed")
            }
        }
    }

    // [impl->dsn~outlook-blocks~1]
    override fun syncBlocks(calendar: String?, window: ClosedRange<LocalDateTime>, blocks: List<OutlookBlock>): String {
        val answer = Json.parseToJsonElement(run("outlook-blocks-sync", input = blocksRequest(calendar, window, blocks))).jsonObject
        fun count(key: String) = answer[key]?.jsonPrimitive?.intOrNull ?: 0
        return "${count("added")} blocks added, ${count("deleted")} deleted, ${count("kept")} kept"
    }

    // [impl->dsn~outlook-clear~1]
    override fun countBlocks(calendar: String?): Int =
        Json.parseToJsonElement(run("outlook-blocks-clear", "--dry-run", *calendarArguments(calendar))).jsonObject["count"]!!.jsonPrimitive.intOrNull!!

    // [impl->dsn~outlook-clear~1]
    override fun clearBlocks(calendar: String?): Int =
        Json.parseToJsonElement(run("outlook-blocks-clear", *calendarArguments(calendar))).jsonObject["deleted"]!!.jsonPrimitive.intOrNull!!

    // [impl->dsn~outlook-busy~1]
    override fun busy(calendar: String?, from: LocalDate, to: LocalDate): List<OutlookBusy> =
        parseOutlookBusy(run("outlook-busy", "--from", from.toString(), "--to", to.toString(), *calendarArguments(calendar)))

    // [impl->dsn~outlook-sideline~1]
    override fun sideline(entryId: String, on: Boolean) {
        run("outlook-sideline", Outlook.anchor(entryId), if (on) "on" else "off")
    }

    private fun calendarArguments(calendar: String?) = calendar?.let { arrayOf("--calendar", it) } ?: emptyArray()

    /** Runs `ankd` with the arguments and gives what it prints; [input], if given, is written to it as UTF-8. */
    private fun run(vararg arguments: String, input: String? = null): String {
        val process = ProcessBuilder(ankd.toString(), *arguments).redirectError(ProcessBuilder.Redirect.PIPE).start()
        process.outputStream.use { stream -> input?.let { stream.write(it.toByteArray(Charsets.UTF_8)) } }
        // Read before waiting: a full pipe would block the program.
        var errors = ""
        val output = thread { errors = process.errorStream.readAllBytes().toString(Charsets.UTF_8).trim() }.let { reader ->
            process.inputStream.bufferedReader(Charsets.UTF_8).readText().also { reader.join() }
        }
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            error("AnchorHub did not answer: ${arguments.first()}")
        }
        if (process.exitValue() != 0) error(errors.removePrefix("Error: ").ifEmpty { "AnchorHub ${arguments.first()} failed with exit code ${process.exitValue()}" })
        return output
    }

    companion object {
        /** AnchorHub where its installer puts it; `null` on another system or without it. */
        fun installed(): AnchorHubOutlook? {
            if (!System.getProperty("os.name").startsWith("Windows")) return null
            val local = System.getenv("LOCALAPPDATA") ?: return null
            return Path(local, "Programs", "AnchorHub", "ankd.exe").takeIf { it.isRegularFile() }?.let(::AnchorHubOutlook)
        }
    }
}

/** The items that `ankd outlook-todos` prints as a JSON array; the fields that koppter does not show are ignored. */
// [impl->dsn~outlook-items~1]
internal fun parseOutlookTodos(json: String): List<OutlookItem> =
    Json.parseToJsonElement(json.trim()).jsonArray.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        fun text(key: String) = item[key]?.jsonPrimitive?.contentOrNull
        val entryId = text("entryId")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        OutlookItem(entryId, text("subject").orEmpty(), text("due")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() })
    }

/** What `ankd outlook-blocks-sync` reads: the calendar, the window and the blocks, with local times to the minute. */
// [impl->dsn~outlook-blocks~1]
internal fun blocksRequest(calendar: String?, window: ClosedRange<LocalDateTime>, blocks: List<OutlookBlock>): String =
    JsonObject(
        mapOf(
            "calendar" to JsonPrimitive(calendar),
            "from" to JsonPrimitive(minutes(window.start)),
            "to" to JsonPrimitive(minutes(window.endInclusive)),
            "blocks" to JsonArray(
                blocks.map {
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(it.id),
                            "start" to JsonPrimitive(minutes(it.start)),
                            "end" to JsonPrimitive(minutes(it.end)),
                            "subject" to JsonPrimitive(it.subject),
                            "body" to JsonPrimitive(it.body),
                        ),
                    )
                },
            ),
        ),
    ).toString()

private fun minutes(time: LocalDateTime) = "${time.date}T${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

/** The appointments that `ankd outlook-busy` prints as a JSON array; one that cannot be read is left out. */
// [impl->dsn~outlook-busy~1]
internal fun parseOutlookBusy(json: String): List<OutlookBusy> =
    Json.parseToJsonElement(json.trim()).jsonArray.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        fun time(key: String) = item[key]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDateTime.parse(it.take(16)) }.getOrNull() }
        val status = when (item["busyStatus"]?.jsonPrimitive?.contentOrNull) {
            "free" -> BusyStatus.FREE
            "tentative" -> BusyStatus.TENTATIVE
            "oof" -> BusyStatus.OOF
            "elsewhere" -> BusyStatus.ELSEWHERE
            else -> BusyStatus.BUSY
        }
        fun flag(key: String) = item[key]?.jsonPrimitive?.booleanOrNull ?: false
        OutlookBusy(
            time("start") ?: return@mapNotNull null,
            time("end") ?: return@mapNotNull null,
            flag("allDay"),
            status,
            item["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            item["entryId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            flag("recurring"),
            flag("sideline"),
            item["color"]?.jsonPrimitive?.contentOrNull,
        )
    }