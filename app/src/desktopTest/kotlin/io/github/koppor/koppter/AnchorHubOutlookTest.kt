package io.github.koppor.koppter

import io.github.koppor.koppter.outlook.BusyStatus
import io.github.koppor.koppter.outlook.OutlookBlock
import io.github.koppor.koppter.outlook.OutlookBusy
import io.github.koppor.koppter.outlook.OutlookItem
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorHubOutlookTest {
    // [utest->dsn~outlook-items~1]
    @Test
    fun theItemsAreReadFromTheJsonOfAnchorHub() {
        val json = """
            [{"entryId":"00000000AB","storeId":"0000FF","store":"Mailbox","kind":"mail","subject":"Answer the invitation","due":"2026-11-18","start":null,"received":"2026-11-10T09:00:00+01:00","sender":"Someone","anchor":"anchor://outlook/message/00000000AB?v=1"},
             {"entryId":"00000000CD","kind":"task","subject":"Book the hotel","due":null},
             {"subject":"Without an EntryID"}]
        """
        assertEquals(
            listOf(
                OutlookItem("00000000AB", "Answer the invitation", LocalDate(2026, 11, 18)),
                OutlookItem("00000000CD", "Book the hotel", null),
            ),
            parseOutlookTodos(json),
        )
        assertEquals(emptyList(), parseOutlookTodos("[]\r\n"))
    }

    // [utest->dsn~outlook-busy~1]
    @Test
    fun theAppointmentsAreReadFromTheJsonOfAnchorHub() {
        val json = """[{"start":"2026-11-18T09:00","end":"2026-11-18T10:30","allDay":false,"busyStatus":"tentative"},{"start":"2026-11-19T00:00:00","end":"2026-11-20T00:00:00","allDay":true,"busyStatus":"oof"},{"start":"broken"}]"""
        assertEquals(
            listOf(
                OutlookBusy(LocalDateTime(2026, 11, 18, 9, 0), LocalDateTime(2026, 11, 18, 10, 30), false, BusyStatus.TENTATIVE),
                OutlookBusy(LocalDateTime(2026, 11, 19, 0, 0), LocalDateTime(2026, 11, 20, 0, 0), true, BusyStatus.OOF),
            ),
            parseOutlookBusy(json),
        )
    }

    // [utest->dsn~outlook-blocks~1]
    @Test
    fun theRequestNamesTheWindowAndTheBlocksToTheMinute() {
        val block = OutlookBlock("20261118-840-1f", LocalDateTime(2026, 11, 18, 14, 0), LocalDateTime(2026, 11, 18, 15, 30), "Write \"the\" review [koppter]", "[koppter]\nPlanned")
        assertEquals(
            """{"calendar":null,"from":"2026-11-18T10:05","to":"2026-12-09T00:00","blocks":[{"id":"20261118-840-1f","start":"2026-11-18T14:00","end":"2026-11-18T15:30","subject":"Write \"the\" review [koppter]","body":"[koppter]\nPlanned"}]}""",
            blocksRequest(null, LocalDateTime(2026, 11, 18, 10, 5)..LocalDateTime(2026, 12, 9, 0, 0), listOf(block)),
        )
    }
}