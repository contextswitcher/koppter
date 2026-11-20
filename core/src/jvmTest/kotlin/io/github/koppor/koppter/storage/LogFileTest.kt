package io.github.koppor.koppter.storage

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LogFileTest {
    private val log = """
        2026-11-19 08:00:00 ERROR Sync: failed the day before
        2026-11-20 09:59:59 ERROR Sync: failed more than an hour ago
        java.io.IOException: unreachable
        2026-11-20 10:00:00 ERROR Sync: failed within the hour
        java.io.IOException: 2026-11-01 refused
        	at Sync.run(Sync.kt:1)
        2026-11-20 10:30:00 WARN Sync: slow
    """.trimIndent() + "\n"

    // [utest->dsn~share-log~1]
    @Test
    fun theLogIsCutAtTheFirstEntryFromTheTimeOnWithItsStackTrace() {
        assertEquals(
            "2026-11-20 10:00:00 ERROR Sync: failed within the hour\njava.io.IOException: 2026-11-01 refused\n\tat Sync.run(Sync.kt:1)\n" +
                "2026-11-20 10:30:00 WARN Sync: slow\n",
            logSince(log, LocalDateTime.of(2026, 11, 20, 10, 0)),
        )
    }

    // [utest->dsn~share-log~1]
    @Test
    fun aLogWithoutAnEntryFromTheTimeOnGivesNothing() {
        assertEquals("", logSince(log, LocalDateTime.of(2026, 11, 20, 11, 0)))
        assertEquals("", logSince("", LocalDateTime.of(2026, 11, 20, 11, 0)))
    }
}
