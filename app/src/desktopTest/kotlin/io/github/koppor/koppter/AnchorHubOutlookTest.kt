package io.github.koppor.koppter

import io.github.koppor.koppter.outlook.OutlookItem
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
}
