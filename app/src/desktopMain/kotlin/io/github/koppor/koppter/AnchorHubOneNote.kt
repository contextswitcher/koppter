package io.github.koppor.koppter

import io.github.koppor.koppter.todotxt.Backlink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tinylog.Logger

/** The command of AnchorHub that writes the links back to the tasks onto the pages of OneNote. */
private const val BACKLINKS_COMMAND = "onenote-backlinks-sync"

/**
 * Writes the links back to the tasks of koppter onto the pages of OneNote, through `ankd.exe` of AnchorHub, which talks to OneNote over COM.
 * The call blocks while OneNote answers.
 */
// [impl->dsn~onenote-backlinks~1]
internal class AnchorHubOneNote(private val anchorHub: AnchorHubOutlook) {
    /** Set once AnchorHub turned out to lack the command: until the next start, which an update of AnchorHub needs anyway, it is not asked again. */
    @Volatile
    private var lacksCommand = false

    /**
     * Sends the complete set of links; AnchorHub removes those of koppter that are no longer in it.
     * Returns whether the set needs not be sent again while it stays the same: `false` when AnchorHub failed or a page was not updated.
     */
    fun sync(links: List<Backlink>): Boolean {
        if (lacksCommand) return true
        val answer = try {
            anchorHub.run(BACKLINKS_COMMAND, input = backlinksRequest(links, ICON))
        } catch (e: MissingCommand) {
            Logger.info("The pages of OneNote get no links to the tasks: {}", e.message)
            lacksCommand = true
            return true
        } catch (e: Exception) {
            Logger.warn(e, "AnchorHub did not update the links on the pages of OneNote")
            return false
        }
        val result = parseBacklinksAnswer(answer)
        Logger.info("Links on the pages of OneNote: {} updated, {} removed, {} unchanged, {} failed", result.updated, result.removed, result.unchanged, result.failed.size)
        result.failed.forEach { (note, error) -> Logger.warn("The links on the page of OneNote {} were not updated: {}", note, error) }
        return result.failed.isEmpty()
    }

    companion object {
        /** The icon of the application, which the page shows in front of the links of koppter; 8 KB, below the 64 KB that AnchorHub takes. */
        private val ICON: String? by lazy {
            AnchorHubOneNote::class.java.getResourceAsStream("/koppter.png")?.use { java.util.Base64.getEncoder().encodeToString(it.readBytes()) }
        }

        fun installed(): AnchorHubOneNote? = AnchorHubOutlook.installed()?.let(::AnchorHubOneNote)
    }
}

/** What `ankd onenote-backlinks-sync` reads: the source, its name and its icon (a PNG in Base64) on the page, and the complete set of its links. */
// [impl->dsn~onenote-backlinks~1]
internal fun backlinksRequest(links: List<Backlink>, icon: String? = null): String =
    JsonObject(
        mapOf(
            "source" to JsonPrimitive("koppter"),
            "name" to JsonPrimitive("koppter"),
        ) + listOfNotNull(icon?.let { "icon" to JsonPrimitive(it) }) + mapOf(
            "links" to JsonArray(
                links.map {
                    JsonObject(
                        mapOf(
                            "note" to JsonPrimitive(it.note),
                            "label" to JsonPrimitive(it.label),
                            "url" to JsonPrimitive(it.url),
                            "done" to JsonPrimitive(it.done),
                        ),
                    )
                },
            ),
        ),
    ).toString()

/** What `ankd onenote-backlinks-sync` answered: the numbers of pages and, for each page that failed, its link and the reason. */
internal data class BacklinksResult(val updated: Int, val removed: Int, val unchanged: Int, val failed: List<Pair<String, String>>)

// [impl->dsn~onenote-backlinks~1]
internal fun parseBacklinksAnswer(json: String): BacklinksResult {
    val answer = Json.parseToJsonElement(json.trim().lines().first()).jsonObject
    fun count(key: String) = answer[key]?.jsonPrimitive?.intOrNull ?: 0
    val failed = answer["failed"]?.let { runCatching { it.jsonArray }.getOrNull() }.orEmpty().mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        (item["note"]?.jsonPrimitive?.contentOrNull.orEmpty()) to (item["error"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    return BacklinksResult(count("updated"), count("removed"), count("unchanged"), failed)
}
