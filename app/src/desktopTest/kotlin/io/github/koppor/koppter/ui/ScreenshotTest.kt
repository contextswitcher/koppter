package io.github.koppor.koppter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.koppor.koppter.outlook.OutlookBlock
import io.github.koppor.koppter.outlook.OutlookBridge
import io.github.koppor.koppter.outlook.OutlookBusy
import io.github.koppor.koppter.outlook.OutlookCalendar
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.storage.SharedLists
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.sync.TaskDates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the screens to `build/screenshots`, for a look at layout changes and for the documentation:
 * `docs/screenshots` holds copies, see `CLAUDE.md`.
 *
 * The tasks are invented; the storage stands in for a git repository so that the screens show synchronization and dates.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {
    private val storage = object : SyncingStorage, CompanionFiles {
        /** Whether writes are kept, for the screens that show what an action wrote; the other screens start from the same file. */
        var keepsWrites = false

        /** Lines that one screen adds to the file, so that the other screens and the images of the README stay as they are. */
        var extra = ""
        val companions = mutableMapOf(
            "koppter.yml" to """
                board-columns:
                  Now: due:<=today -@waiting
                  Waiting: "@waiting"
                  This week: due:<1w
                  Important: (>C)
                time-maps:
                  General: Mon-Fri 08:00-12:00 13:00-17:00
                  Focus: General 08:00-10:00
                  Evenings: Mon-Thu 19:00-21:00, Sat 10:00-12:00
                context-time-maps:
                  "@computer": Focus
                  "@home": Evenings
                barriers:
                  default: quarter
                  "+Vacation": 2026-11-30
                limits:
                  "@errands": max 2h/week
                  "+Work": min 1h/week, block 90m
            """.trimIndent(),
            // As "Save and plan" writes it on the day of the screenshots at noon; the bike and the present were added afterwards.
            "calendar.txt" to """
                2026-11-12 w46 Thu  13:00-13:30 Call the landlord about the heating @phone. 13:30-14:00 Schedule Goodwill pickup +GarageSale @phone. 14:00-14:30 Post signs around the neighborhood +GarageSale @errands. 14:30-15:00 Renew the library card @errands>city. 19:00-19:30 Water the plants @home
                2026-11-13 w46 Fri  08:00-08:30 Submit the travel expense report +Work @computer. 08:30-10:00 Prepare the slides for the team meeting +Work @computer
                2026-11-14 w46 Sat
                2026-11-15 w46 Sun
                2026-11-16 w47 Mon  08:00-08:30 Prepare the slides for the team meeting +Work @computer. 08:30-09:00 Book the train to Hamburg +Vacation @computer @waiting. 09:00-09:30 Back up the photos @computer>backup
            """.trimIndent() + "\n",
            // No entry occurs on the day of the screenshots, so the list stays as the file has it.
            "ice_recur.txt" to """
                # Work
                weekly ; day Monday - (B) Plan the week +Work
                monthly ; day_of_month -1 - Send the time sheet +Work @computer

                # Household
                @2026-11-05 weekly 2 ; day Thursday - Put out the paper bin @home
                monthly ; day_of_week Saturday => 1 - Clean the bike @home
                @2026-04-15 yearly - (A) Renew the insurance #money
            """.trimIndent() + "\n",
            "notes/k3f.md" to "## Agenda\n\n- Results of the survey\n- Plan for December\n\nAsk for the room with the **projector**.",
        )

        override var onStatus: (SyncStatus) -> Unit = {}
        override fun sync() = Unit
        override fun dates() = mapOf(
            "(B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 dur:2h cs:work/team-meeting note:k3f.md" to TaskDates(added = LocalDate(2026, 11, 5)),
        )

        override fun readCompanion(path: String) = companions[path]
        override fun writeCompanion(path: String, text: String) {
            if (keepsWrites) companions[path] = text
        }
        override fun readCompanionBytes(path: String): ByteArray? = null
        override fun writeCompanionBytes(path: String, bytes: ByteArray) = Unit
        override fun deleteCompanion(path: String) = Unit

        private var text = """
            (A) Call the landlord about the heating @phone due:2026-11-12
            (A) Submit the travel expense report +Work @computer #money due:2026-11-10 onenote:https://d.docs.live.net/0123456789ABCDEF/Documents/Work.one#Travel%20expenses&section-id={8D0158A8-979F-4ABA-9DC2-77F6F43A00C8}&page-id={666AC41B-1C96-448D-AF38-3DEF10279206}&end
            (B) Schedule Goodwill pickup +GarageSale @phone due:2026-11-14
            (B) Prepare the slides for the team meeting +Work @computer #meeting due:2026-11-17 dur:2h cs:work/team-meeting note:k3f.md
            (C) Post signs around the neighborhood +GarageSale @errands
            (C) Renew the library card @errands>city #someday due:2026-11-20 deadline:2026-11-27
            Water the plants @home due:2026-11-12 rec:1w
            Book the train to Hamburg +Vacation @computer @waiting
            Compare prices for a new bike, see [Bike test 2026](https://example.org/bikes) +Vacation #money #someday
            Find a present for Dad +Family due:2026-11-12
            Back up the photos @computer>backup due:2026-12-01 rec:+1m
            x Thank Mom for the meatballs +Family @phone
        """.trimIndent() + "\n"

        override fun read() = text + extra
        override fun write(text: String, change: String) {
            if (keepsWrites) this.text = text
        }
        override fun version(): Any? = null
    }

    private val git = object : GitSetupActions {
        override fun publicKey() = "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBExampleKeyOfTheDemo= koppter"
        override suspend fun clone(url: String) = "todo.txt"
        override suspend fun cloneShared(url: String) = error("not cloned in this test")
        override suspend fun check(url: String) = Unit
    }

    /** A list of a member of a family, with what its `calendar.txt` says of the day. */
    private class Member(private val text: String, private val calendar: String) : TodoStorage, CompanionFiles {
        override fun read() = text.trimIndent() + "\n"
        override fun write(text: String, change: String) = Unit
        override fun version(): Any? = null
        override fun readCompanion(path: String) = "2026-11-12 w46 Thu  $calendar\n".takeIf { path == "calendar.txt" }
        override fun writeCompanion(path: String, text: String) = Unit
        override fun readCompanionBytes(path: String): ByteArray? = null
        override fun writeCompanionBytes(path: String, bytes: ByteArray) = Unit
        override fun deleteCompanion(path: String) = Unit
    }

    private val family = listOf(
        "Anna" to Member(
            """
            Feed the cat due:2026-11-12 rec:1d
            Tidy the room stars:2 due:2026-11-11
            Practice the piano stars:2
            Choir due:2026-11-12 at:18:30 dur:90m
            x 2026-11-12 Empty the dishwasher
            x 2026-11-10 Do the homework stars:2
            """,
            "08:00-13:00 School. 15:00-16:00 Planned: Tidy the room. 16:00-16:30 Planned: Feed the cat",
        ),
        "Ben" to Member(
            """
            Take out the trash due:2026-11-12
            Mow the lawn stars:3
            Football due:2026-11-12 at:16:00 dur:2h
            x 2026-11-11 Wash the car stars:5
            x 2026-11-12 Water the plants
            """,
            "08:00-13:00 Absent. 14:30-15:30 Absent: Dentist",
        ),
        "Carl" to Member(
            """
            Buy the groceries stars:2 due:2026-11-12
            Parents' evening due:2026-11-12 at:19:30 dur:1h
            x 2026-11-09 Repair the bike stars:3
            """,
            "09:00-12:00 Absent. 13:00-15:00 Busy. 15:00-16:00 Busy: Buy the groceries",
        ),
    )

    /** A calendar with busy times whose events overlap each other and the blocks of the plan. */
    private val LECTURES = listOf(
        "20261112T100000" to "20261112T113000" to "Programming",
        "20261112T103000" to "20261112T120000" to "Office hour",
        "20261112T140000" to "20261112T153000" to "Seminar",
        "20261113T090000" to "20261113T110000" to "Software engineering",
        "20261110T130000" to "20261110T140000" to "Lab",
    ).joinToString("", "BEGIN:VCALENDAR\nX-WR-CALNAME:Lectures\n", "END:VCALENDAR\n") { (times, title) ->
        "BEGIN:VEVENT\nDTSTART:${times.first}\nDTEND:${times.second}\nSUMMARY:$title\nEND:VEVENT\n"
    }

    /** Outlook without items and appointments, so that the settings show its fields. */
    private val outlook = object : OutlookBridge {
        override fun items() = emptyList<OutlookItem>()
        override fun open(entryId: String) = Unit
        override fun complete(entryId: String) = Unit
        override fun syncBlocks(calendar: String?, window: ClosedRange<LocalDateTime>, blocks: List<OutlookBlock>) = ""
        override fun countBlocks(calendar: String?) = 0
        override fun clearBlocks(calendar: String?) = 0
        override fun busy(calendar: String?, from: LocalDate, to: LocalDate) = emptyList<OutlookBusy>()
        override fun sideline(entryId: String, on: Boolean) = Unit
    }

    private fun DesktopComposeUiTest.start(settings: Settings = Settings(), homeScreen: Boolean? = null, members: Boolean = false, update: (() -> Unit)? = null, updateLabel: String = "Restart to update", updateNews: List<Pair<String, String>> = emptyList(), withOutlook: Boolean = false, link: String? = null) {
        val shown = if (members) SharedLists(storage, family) else storage
        // With members, the settings have the field for their calendars, which a platform that reads calendars shows.
        val controller = TodoController(settings, { shown }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 14 * 60 + 10 }, fetch = if (members) ({ "" }) else if (settings.calendars.isNotBlank()) ({ LECTURES }) else null, outlook = outlook.takeIf { withOutlook })
        // As many as a device has, so that the list is cut, and a name that is too long for its place.
        val apps = (
            "Books Calculator Calendar Camera Chrome Clock Contacts Drive evcc Files Gallery Gmail Health Hue Keep Maps Messages Music News Notes Phone Photos Podcasts Radio " +
                "Recorder Settings Sheets Signal Spotify Translate Wallet Weather YouTube"
            ).split(" ").plus(listOf("Home Assistant Companion for Android", "Play Store", "Smart Life", "Voice Access")).map { name ->
                // A disk in a color of its own stands for the icon.
                val icon = ImageBitmap(64, 64).also { Canvas(it).drawCircle(Offset(32f, 32f), 30f, Paint().apply { color = Color.hsv((name.hashCode() and 0xffff) % 360f, 0.5f, 0.8f) }) }
                DeviceApp(name, name, icon) {}
            }
        setContent { App(controller, "todo.txt", git = git, homeScreen = homeScreen, apps = { apps }, update = update, updateLabel = updateLabel, updateNews = updateNews) }
        link?.let { runOnUiThread { controller.openLink(it) } }
    }

    @Test
    fun rendersListAndDialogs() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        save("list.png")
        onNodeWithTag("filter-project-GarageSale").performClick()
        onNodeWithTag("filter-project-Work").performClick()
        onNodeWithTag("filter-context-phone").performClick()
        save("filter.png")
        onNodeWithTag("filter-clear").performClick()
        onNodeWithText("Prepare the slides for the team meeting").performClick()
        save("edit.png")
        onNodeWithTag("edit-cancel").performClick()
        onNodeWithTag("open-settings").performClick()
        save("settings.png")
    }

    @Test
    fun rendersOverview() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start()
        onNodeWithTag("overview-switch").performClick()
        save("overview.png")
        onNodeWithTag("overview-computer").performClick()
        save("overview-context.png")
    }

    @Test
    fun rendersPlan() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-update").performScrollTo().performClick()
        save("plan.png")
        onNodeWithTag("plan-map-General").performScrollTo().performClick()
        // Early mornings and late afternoons only if needed, Friday afternoon as the last resort.
        onNodeWithTag("time-map-paint-yellow").performClick()
        for (day in 0..4) {
            onNodeWithTag("time-map-grid").performTouchInput { swipe(Offset(width * (2 * day + 1) / 14f, height * 28.5f / 96), Offset(width * (2 * day + 1) / 14f, height * 31.5f / 96)) }
            onNodeWithTag("time-map-grid").performTouchInput { swipe(Offset(width * (2 * day + 1) / 14f, height * 68.5f / 96), Offset(width * (2 * day + 1) / 14f, height * 71.5f / 96)) }
        }
        onNodeWithTag("time-map-paint-red").performClick()
        onNodeWithTag("time-map-grid").performTouchInput { swipe(Offset(width * 9 / 14f, height * 52.5f / 96), Offset(width * 9 / 14f, height * 67.5f / 96)) }
        save("time-map.png")
    }

    @Test
    fun rendersAgendaWeekAndInbox() = runDesktopComposeUiTest(width = 1100, height = 860) {
        start()
        onNodeWithTag("agenda-switch").performClick()
        save("agenda.png")
        onNodeWithTag("week-switch").performClick()
        save("week.png")
        onNodeWithTag("today-switch").performClick()
        save("today.png")
        onNodeWithTag("inbox-switch").performClick()
        save("inbox.png")
    }

    @Test
    fun rendersSchedule() = runDesktopComposeUiTest(width = 1100, height = 1060) {
        start()
        onNodeWithTag("open-schedule").performClick()
        save("schedule.png")
    }

    @Test
    fun rendersBoard() = runDesktopComposeUiTest(width = 1400, height = 620) {
        start()
        onNodeWithTag("board-switch").performClick()
        // A card on its way from column B to column A.
        onNodeWithText("Schedule Goodwill pickup").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-30f, 0f))
            moveBy(Offset(-200f, 60f))
        }
        save("board-priority.png")
        // Back to its column, so that the task stays as it is for the other boards.
        onNodeWithTag("board").performMouseInput {
            moveBy(Offset(230f, -60f))
            release()
        }
        onNodeWithTag("group-PROJECT").performClick()
        save("board-project.png")
        onNodeWithTag("group-FILTER").performClick()
        save("board-filter.png")
    }

    @Test
    fun rendersNordDark() = runDesktopComposeUiTest(width = 1100, height = 620) {
        start(Settings(appearance = Appearance.DARK))
        save("nord-dark.png")
    }

    @Test
    fun rendersListInGroups() = runDesktopComposeUiTest(width = 1100, height = 860) {
        start()
        onNodeWithTag("group-PROJECT").performClick()
        save("list-project.png")
    }

    @Test
    fun rendersPickers() = runDesktopComposeUiTest(width = 1100, height = 760) {
        start()
        onNodeWithText("Prepare the slides for the team meeting").performClick()
        onNodeWithContentDescription("Choose Due time").performClick()
        save("time-picker.png")
        onNodeWithText("OK").performClick()
        onNodeWithContentDescription("Choose Duration").performClick()
        save("duration-picker.png")
        onNodeWithText("OK").performClick()
        onNodeWithContentDescription("Choose Recurrence").performClick()
        save("recurrence-picker.png")
    }

    @Test
    fun rendersDueMenu() = runDesktopComposeUiTest(width = 1100, height = 520) {
        start()
        onAllNodesWithTag("task-description", useUnmergedTree = true)[0].performMouseInput { rightClick() }
        save("due-menu.png")
    }

    @Test
    fun rendersNarrowWindow() = runDesktopComposeUiTest(width = 412, height = 760) {
        start()
        save("narrow.png")
    }

    // The screens of the README are rendered taller than a window in use, which hides what falls out of view.
    // These two sizes are the ones a change of the user interface is reviewed at (`.claude/skills/ux-review`); the README does not show them.
    @Test
    fun rendersPhoneForReview() = reviewScreens("phone", width = 412, height = 760)

    @Test
    fun rendersLaptopForReview() = reviewScreens("laptop", width = 1280, height = 720)

    // The one screen that differs in a window wider than high and still as narrow as a phone.
    @Test
    fun rendersFamilyOnAPhoneHeldAcrossForReview() = runDesktopComposeUiTest(width = 760, height = 412) {
        start(members = true)
        onNodeWithTag("family-switch").performScrollTo().performClick()
        save("review/across-family.png")
    }

    private fun reviewScreens(size: String, width: Int, height: Int) {
        // Each dialog in an application of its own: a dialog taller than the window is not closed reliably by a click in a test.
        fun screen(name: String, homeScreen: Boolean? = null, members: Boolean = false, settings: Settings = Settings(), updateLabel: String? = null, updateNews: List<Pair<String, String>> = emptyList(), extra: String = "", withOutlook: Boolean = false, link: String? = null, open: DesktopComposeUiTest.() -> Unit) = runDesktopComposeUiTest(width = width, height = height) {
            storage.extra = extra
            start(settings, homeScreen = homeScreen, members = members, update = updateLabel?.let { {} }, updateLabel = updateLabel.orEmpty(), updateNews = updateNews, withOutlook = withOutlook, link = link)
            open()
            save("review/$size-$name.png")
        }
        screen("list") {}
        // [utest->dsn~templates~1]
        screen("templates") {
            storage.companions["templates.txt"] = "# Swimming course\nPack the swimming bag +Sport @home\n  - [ ] towel\n  - [ ] goggles\nBuy a ten-visit card +Sport\n\n# Business trip\nBook the train +Travel\nSubmit the travel request +Travel\nAsk Dana to cover the Friday meeting @office\n"
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("new-from-template").performClick()
            onNodeWithTag("template-0").performClick()
        }
        // [utest->dsn~note-checklist~1]
        screen("edit-checklist", extra = "Pack the swimming bag +Sport @home note:swm.md\n") {
            storage.companions["notes/swm.md"] = "- [x] towel\n- [ ] goggles\n- [ ] shampoo\n\nThe locker needs a coin.\n"
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("task-list").performScrollToNode(hasText("Pack the swimming bag", substring = true))
            onNodeWithText("Pack the swimming bag", substring = true, useUnmergedTree = true).performClick()
            onNodeWithTag("edit-dialog").assertExists()
        }
        // [utest->dsn~latex-todos~1]
        screen("latex-todo", extra = "Cite the survey on remote work +Paper #latex tex:1x2y3z\n") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("task-list").performScrollToNode(hasText("Cite the survey", substring = true))
        }
        screen("latex-hint", extra = "Cite the survey on remote work +Paper #latex tex:1x2y3z\n") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("task-list").performScrollToNode(hasText("Cite the survey", substring = true))
            onNodeWithTag("task-done-latex", useUnmergedTree = true).performClick()
            mainClock.advanceTimeBy(1_000)
        }
        // [utest->dsn~outlook-busy~1]
        screen("settings-outlook", withOutlook = true, settings = Settings(outlookBusyCalendars = "me@uni.example")) {
            onNodeWithTag("open-settings").performClick()
            onNodeWithTag("settings-tab-integrations").performClick()
            onNodeWithTag("settings-outlook-mirror").performScrollTo()
        }
        screen("overdue-tooltip") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("group-PRIORITY").performScrollTo().performClick()
            onNodeWithTag("overdue-separator", useUnmergedTree = true).performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(1_000)
        }
        // [utest->dsn~link-titles~1]
        screen("link-title") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithTag("task-list").performScrollToNode(hasText("Bike test 2026", substring = true))
        }
        screen("synchronizing") { storage.onStatus(SyncStatus(SyncState.RUNNING)) }
        screen("not-synchronized") { storage.onStatus(SyncStatus(SyncState.FAILED, "git@example.org:family/tasks.git: connection refused")) }
        screen("week") { onNodeWithTag("week-switch").performClick() }
        val lectures = Settings(calendars = "# Lectures\nhttps://calendar.example.org/lectures.ics")
        screen("week-calendars", settings = lectures) { onNodeWithTag("week-switch").performClick() }
        screen("week-calendars-menu", settings = lectures) {
            onNodeWithTag("week-switch").performClick()
            onNodeWithTag("week-calendars").performClick()
        }
        screen("today") { onNodeWithTag("today-switch").performClick() }
        // A phone without Outlook, with the appointments that the desktop copied; only these screens have them.
        val mirror = "2026-11-12 w46 Thu  09:00-10:30 Faculty board. 13:00-14:00 Interview with a candidate\n2026-11-13 w46 Fri  10:00-11:00 Review of the project\n"
        screen("week-mirror") {
            storage.companions[OutlookCalendar.MIRROR] = mirror
            onNodeWithTag("week-switch").performClick()
            waitUntil { onAllNodesWithTag("calendar-appointment", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }
        screen("today-mirror") {
            onNodeWithTag("today-switch").performClick()
            waitUntil { onAllNodesWithTag("calendar-appointment", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() || size == "phone" }
        }
        screen("today-mirror-day") {
            onNodeWithTag("today-switch").performClick()
            if (size == "phone") onNodeWithTag("today-calendar").performClick()
            waitUntil { onAllNodesWithTag("calendar-appointment", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }
        screen("today-calendars-menu") {
            onNodeWithTag("today-switch").performClick()
            if (size == "phone") onNodeWithTag("today-calendar").performClick()
            waitUntil { onAllNodesWithTag("calendar-appointment", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("week-calendars").performClick()
        }
        screen("today-appointments-only", settings = Settings(hiddenCalendars = Settings.PLAN_SOURCE)) {
            onNodeWithTag("today-switch").performClick()
            if (size == "phone") onNodeWithTag("today-calendar").performClick()
            waitUntil { onAllNodesWithTag("calendar-appointment", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            storage.companions -= OutlookCalendar.MIRROR
        }
        screen("today-hidden-matches") {
            onNodeWithTag("today-switch").performClick()
            if (width < 840) onNodeWithTag("open-search").performClick()
            onNodeWithTag("filter-text").performTextInput("photos")
        }
        screen("week-filtered") {
            onNodeWithTag("week-switch").performClick()
            if (width < 840) onNodeWithTag("open-search").performClick()
            onNodeWithTag("filter-text").performTextInput("photos")
        }
        screen("filter-search") {
            if (width < 840) onNodeWithTag("open-search").performClick()
            onNodeWithTag("filter-contexts-search").performScrollTo().performTextInput("o")
        }
        screen("today-groups", settings = Settings(listGroup = "PROJECT")) { onNodeWithTag("today-switch").performClick() }
        screen("edit") {
            // First a click that changes nothing: a dialog opened by a touch as the first input is missing in the image.
            onNodeWithTag("list-switch").performClick()
            // The description itself: on a phone, the middle of this card is a label that opens a link.
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithTag("edit-dialog").assertExists()
        }
        // [utest->dsn~task-link~1]
        screen("task-link", link = "koppter://task?onenote=666AC41B-1C96-448D-AF38-3DEF10279206&text=Submit%20the%20travel%20expense%20report") {
            onNodeWithTag("edit-dialog").assertExists()
        }
        screen("task-link-missing", link = "koppter://task?onenote=00000000-0000-0000-0000-000000000000&text=Call%20the%20dentist") {
            waitUntil { onAllNodesWithText("No task refers", substring = true).fetchSemanticsNodes().isNotEmpty() }
        }
        screen("edit-more") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithTag("edit-dialog").assertExists()
            onNodeWithTag("edit-context-more").performScrollTo().performClick()
            onNodeWithTag("edit-context-rest").performScrollTo()
        }
        screen("edit-deadline-menu") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithTag("edit-deadline").performScrollTo().performMouseInput { rightClick() }
        }
        screen("edit-start-menu") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithTag("edit-threshold").performScrollTo().performTextReplacement("1w")
            onNodeWithTag("edit-threshold").performMouseInput { rightClick() }
        }
        screen("edit-start-calendar") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithContentDescription("Choose Start date").performScrollTo().performClick()
        }
        screen("edit-names") {
            onNodeWithTag("list-switch").performClick()
            onNodeWithText("Prepare the slides for the team meeting", useUnmergedTree = true).performClick()
            onNodeWithTag("edit-dialog").assertExists()
            onNodeWithTag("edit-before-add").performScrollTo()
        }
        // On a phone, the switch lies outside of the row of the switches until it is scrolled there.
        screen("family", members = true) { onNodeWithTag("family-switch").performScrollTo().performClick() }
        screen("family-times", members = true) {
            onNodeWithTag("family-switch").performScrollTo().performClick()
            onNodeWithTag("family-times-Anna").performScrollTo().performClick()
        }
        screen("settings-members", members = true) {
            onNodeWithTag("open-settings").performClick()
            onNodeWithTag("settings-member-calendar-Carl").performScrollTo()
        }
        screen("settings-plan-member", members = true, settings = Settings(planMember = "Carl", memberCalendars = "Ben = https://calendar.example.org/club.ics titles\nCarl = https://calendar.example.org/work.ics")) {
            onNodeWithTag("open-settings").performClick()
            onNodeWithTag("settings-plan-member-nobody").performScrollTo()
        }
        screen("plan") { onNodeWithTag("open-plan").performClick() }
        screen("time-map") {
            onNodeWithTag("open-plan").performClick()
            onNodeWithTag("plan-add-map").performScrollTo().performClick()
        }
        screen("time-map-block") {
            onNodeWithTag("open-plan").performClick()
            onNodeWithTag("plan-map-General").performScrollTo().performClick()
            // The width and height of the mouse input are not those of the grid in this window: its own size places the pointer.
            val grid = onNodeWithTag("time-map-grid").fetchSemanticsNode().size
            onNodeWithTag("time-map-grid").performMouseInput { moveTo(Offset(grid.width * 9 / 14f, grid.height * 40.5f / 96)) }
            onNodeWithTag("time-map-block").assertExists()
        }
        screen("settings") { onNodeWithTag("open-settings").performClick() }
        screen("list-matrix", settings = Settings(theme = ColorTheme.MATRIX)) {}
        screen("settings-colors", settings = Settings(theme = ColorTheme.DAWN)) {
            onNodeWithTag("open-settings").performClick()
            onNodeWithTag("settings-tab-colors").performClick()
        }
        // The tab that Android alone has.
        screen("settings-device", homeScreen = false) {
            onNodeWithTag("open-settings").performClick()
            onNodeWithTag("settings-tab-device").performClick()
        }
        // What a device shows whose home screen is the application.
        screen("home", homeScreen = true) {}
        // [utest->dsn~android-update~1]
        screen("update", updateLabel = "Update") {}
        // [utest->dsn~whats-new~1]
        screen(
            "whats-new", updateLabel = "Restart to update",
            updateNews = List(12) { "Added" to "A feature number ${it + 1} of the release, described in one sentence that is long enough to wrap on a phone." } + ("Fixed" to "A fix."),
        ) { onNodeWithTag("restart-to-update").performClick() }
        screen("apps", homeScreen = true, settings = Settings(appStarts = "Hue=31\nevcc=12\nChrome=5\nSettings=2\nCamera=2\nClock=1")) { onNodeWithTag("open-apps").performClick() }
        // The last ones: the clock that runs and the time away stay in the file.
        storage.keepsWrites = true
        screen("clock") {
            onNodeWithTag("today-switch").performClick()
            onAllNodesWithTag("clock-start")[0].performClick()
        }
        screen("away") { onNodeWithTag("go-away").performClick() }
    }

    private fun DesktopComposeUiTest.save(name: String) {
        waitForIdle()
        val file = File("build/screenshots/$name")
        file.parentFile.mkdirs()
        ImageIO.write(captureToImage().toAwtImage(), "png", file)
        assertTrue(file.length() > 0)
    }
}
