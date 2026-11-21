package io.github.koppor.koppter.plan

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~time-log~1]
class TimeLogTest {
    private fun at(day: Int, hour: Int, minute: Int) = LocalDateTime(2026, 11, day, hour, minute)

    @Test
    fun aTimeIsAppendedAsALinePerDay() {
        val log = TimeLog.append("", at(21, 16, 50), at(21, 17, 20), "Write the report +Work")
        assertEquals("2026-11-21 16:50-17:20 Write the report +Work\n", log)
        assertEquals(
            log + "2026-11-21 23:30-24:00 Read\n2026-11-22 00:00-00:15 Read\n",
            TimeLog.append(log.trimEnd('\n'), at(21, 23, 30), at(22, 0, 15), "Read"),
        )
        // A clock of the device that was set back adds nothing.
        assertEquals(log, TimeLog.append(log, at(21, 18, 0), at(21, 17, 0), "Read"))
    }

    @Test
    fun theTimeAwayIsAnOpenLineUntilTheUserIsBack() {
        val log = "2026-11-21 16:50-17:20 Write the report +Work\n"
        val away = TimeLog.goAway(log, at(21, 17, 20))
        assertEquals(log + "2026-11-21 17:20- Away\n", away)
        assertEquals(at(21, 17, 20), TimeLog.awaySince(away))
        val back = TimeLog.back(away, at(21, 18, 5))
        assertEquals(log + "2026-11-21 17:20-18:05 Away\n", back)
        assertEquals(null, TimeLog.awaySince(back))
        assertEquals(back, TimeLog.back(back, at(21, 19, 0)))
    }
}
