package io.github.koppor.koppter

import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.outlook.OutlookTodos
import kotlinx.datetime.LocalDate
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
internal class AnchorHubOutlook(private val ankd: Path) : OutlookTodos {
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

    private fun run(vararg arguments: String): String {
        val process = ProcessBuilder(ankd.toString(), *arguments).redirectError(ProcessBuilder.Redirect.PIPE).start()
        // Read before waiting: a full pipe would block the program.
        val output = thread { process.errorStream.readAllBytes() }.let { errors ->
            process.inputStream.bufferedReader(Charsets.UTF_8).readText().also { errors.join() }
        }
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            error("AnchorHub did not answer: ${arguments.first()}")
        }
        if (process.exitValue() != 0) error("AnchorHub ${arguments.first()} failed with exit code ${process.exitValue()}")
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
