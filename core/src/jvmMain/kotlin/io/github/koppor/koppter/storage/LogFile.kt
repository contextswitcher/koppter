package io.github.koppor.koppter.storage

import java.nio.file.Path

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
        "writer2.format" to "{date: yyyy-MM-dd HH:mm:ss} {level} {class}: {message}",
    ).forEach { (key, value) -> System.setProperty("tinylog.$key", value) }
}
