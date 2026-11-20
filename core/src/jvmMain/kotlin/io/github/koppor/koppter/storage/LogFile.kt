package io.github.koppor.koppter.storage

import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val LOG_DATE = "yyyy-MM-dd HH:mm:ss"

/**
 * Adds a log file in the directory to the configuration of tinylog, which reads system properties in addition to
 * `tinylog.properties`. To be called before anything logs, as tinylog reads its configuration once.
 *
 * The file is named here because its directory is known at run time only, and so the tests write none.
 */
// [impl->adr~log-messages-through-tinylog~1]
fun logToFile(directory: Path) {
    mapOf(
        "writer2" to "rolling file",
        "writer2.file" to directory.resolve("koppter-{count}.log").toString(),
        "writer2.policies" to "size: 1mb",
        "writer2.backups" to "3",
        "writer2.charset" to "UTF-8",
        "writer2.format" to "{date: $LOG_DATE} {level} {class}: {message}",
    ).forEach { (key, value) -> System.setProperty("tinylog.$key", value) }
}

/**
 * The entries of the log from the given time on. A line without a date, such as one of a stack trace, belongs to the
 * entry above it.
 */
// [impl->dsn~share-log~1]
fun logSince(log: String, since: LocalDateTime): String {
    // Dates in this format sort as text, so none is parsed.
    val from = since.format(DateTimeFormatter.ofPattern(LOG_DATE))
    val first = Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""", RegexOption.MULTILINE).findAll(log).firstOrNull { it.value >= from }
    return first?.let { log.substring(it.range.first) }.orEmpty()
}
