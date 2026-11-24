package io.github.koppor.koppter.todotxt

// The address ends before a mark that ends a sentence or closes a bracket around it.
private val WEB_ADDRESS = Regex("https?://\\S*[^\\s.,;:!?)\\]]")

private val MARKDOWN_LINK = Regex("\\[([^\\]]*)]\\((https?://[^)\\s]+)\\)")

/** The parts of a text that are addresses of the web, also those inside a Markdown link. */
// [impl->dsn~links~1]
fun webAddresses(text: String): List<IntRange> = WEB_ADDRESS.findAll(text).map { it.range }.toList()

/** A Markdown link `[label](address)` of a text: where it stands, what it shows and where it leads. */
data class MarkdownLink(val range: IntRange, val label: String, val address: String)

// [impl->dsn~link-titles~1]
fun markdownLinks(text: String): List<MarkdownLink> =
    MARKDOWN_LINK.findAll(text).map { MarkdownLink(it.range, it.groupValues[1], it.groupValues[2]) }.toList()

/** The addresses of the web that are not part of a Markdown link. */
// [impl->dsn~link-titles~1]
fun bareAddresses(text: String): List<IntRange> {
    val links = markdownLinks(text).map { it.range }
    return webAddresses(text).filter { address -> links.none { address.first in it } }
}

private val TITLE = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)

/**
 * The title of an HTML page as a task can keep it: on one line, without brackets that would end the Markdown link,
 * and without words that the task would read as a project, context, hashtag or tag.
 * `null` for a page without a title.
 */
// [impl->dsn~link-titles~1]
fun pageTitle(html: String): String? {
    val raw = TITLE.find(html)?.groupValues?.get(1) ?: return null
    val title = decodeEntities(raw).replace('[', '(').replace(']', ')').trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word -> if (Task(body = word).description.isNotEmpty()) word else word.trimStart('+', '@', '#').replaceFirst(":", ": ").trim() }
    return title.ifBlank { null }
}

private fun decodeEntities(text: String): String =
    Regex("&(#x[0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos|nbsp);").replace(text) { match ->
        when (val name = match.groupValues[1]) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> " "
            else -> (if (name[1] == 'x') name.drop(2).toIntOrNull(16) else name.drop(1).toIntOrNull())
                ?.let { code -> if (code in 0..0x10FFFF) buildString { appendCodePoint(code) } else null } ?: match.value
        }
    }

private fun StringBuilder.appendCodePoint(code: Int) {
    if (code < 0x10000) append(code.toChar()) else {
        append((0xD800 + ((code - 0x10000) shr 10)).toChar())
        append((0xDC00 + ((code - 0x10000) and 0x3FF)).toChar())
    }
}

/** The text with each bare occurrence of [address] as the Markdown link `[title](address)`. */
// [impl->dsn~link-titles~1]
fun withLinkTitle(text: String, address: String, title: String): String =
    bareAddresses(text).filter { text.substring(it) == address }.reversed()
        .fold(text) { result, range -> result.replaceRange(range, "[$title]($address)") }
