package io.github.koppor.koppter.ui

import androidx.compose.ui.test.withKeysDown
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.outlook.BusyStatus
import io.github.koppor.koppter.outlook.OutlookBlock
import io.github.koppor.koppter.outlook.OutlookBridge
import io.github.koppor.koppter.outlook.OutlookBusy
import kotlinx.datetime.LocalDateTime
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.dueChoices
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryStorage(var text: String) : TodoStorage, CompanionFiles {
    val companions = mutableMapOf<String, String>()
    val images = mutableMapOf<String, ByteArray>()
    val changes = mutableListOf<String>()
    override fun readCompanionBytes(path: String) = images[path]
    override fun writeCompanionBytes(path: String, bytes: ByteArray) {
        images[path] = bytes
    }
    override fun readCompanion(path: String) = companions[path]
    override fun writeCompanion(path: String, text: String) {
        companions[path] = text
    }

    override fun read() = text
    override fun write(text: String, change: String) {
        this.text = text
        changes += change
    }
    override fun version(): Any? = text
}

/** Outlook as AnchorHub would show it, with what was asked of it. */
private class FakeOutlook(val items: List<OutlookItem> = emptyList(), val appointments: List<OutlookBusy> = emptyList(), var blocks: Int = 0) : OutlookBridge {
    val opened = mutableListOf<String>()
    val completed = mutableListOf<String>()
    var synced: Pair<ClosedRange<LocalDateTime>, List<OutlookBlock>>? = null
    var cleared = 0
    override fun items() = items
    override fun open(entryId: String) {
        opened += entryId
    }
    override fun complete(entryId: String) {
        completed += entryId
    }
    override fun syncBlocks(calendar: String?, window: ClosedRange<LocalDateTime>, blocks: List<OutlookBlock>): String {
        synced = window to blocks
        return "${blocks.size} blocks added, 0 deleted, 0 kept"
    }
    override fun countBlocks(calendar: String?) = blocks
    override fun clearBlocks(calendar: String?): Int {
        cleared++
        return blocks.also { blocks = 0 }
    }
    override fun busy(calendar: String?, from: LocalDate, to: LocalDate) = appointments
}

@OptIn(ExperimentalTestApi::class)
class AppTest {
    private val storage = MemoryStorage(
        """
        (B) Schedule pickup +GarageSale @phone
        Post signs +GarageSale
        (A) Thank Mom @phone due:2026-11-20
        x Call Mom +Family @phone
        """.trimIndent() + "\n",
        // A new task starts empty, so that a test states all that its task has.
    ).apply { companions["koppter.yml"] = "new-task:\n" }

    /** Adds a task in its dialog, which the button for a new task opens. */
    private fun ComposeUiTest.addTask(description: String) {
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-description").performTextInput(description)
        onNodeWithTag("edit-save").performScrollTo().performClick()
    }

    private fun ComposeUiTest.start(settings: Settings = Settings()) {
        val controller = TodoController(settings, { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
    }

    private fun ComposeUiTest.openTask(description: String) {
        // On the description itself: the middle of the card may be a label, which has a click of its own.
        onNodeWithText(description, useUnmergedTree = true).performClick()
        onNodeWithTag("edit-dialog").assertExists()
    }

    // [utest->dsn~visible-tasks~1]
    // [utest->dsn~task-card~1]
    @Test
    fun showsOpenTasksWithDescriptionAndLabels() = runComposeUiTest {
        start()
        onAllNodesWithTag("task").assertCountEquals(3)
        onNodeWithText("Thank Mom").assertExists()
        onNodeWithText("Call Mom").assertDoesNotExist()
        // The description carries no metadata; the metadata is in the labels.
        onNodeWithText("Schedule pickup").assertExists()
        // A card merges the semantics of its content; the labels are found in the unmerged tree only.
        onAllNodes(hasText("+GarageSale") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertCountEquals(2)
        onNode(hasText("2026-11-20") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertExists()
        onNodeWithTag("overdue-separator").assertDoesNotExist()
    }

    // [utest->dsn~overdue-separator~1]
    // [utest->dsn~list-groups~1]
    // [utest->dsn~list-step~1]
    @Test
    fun overdueTasksStandFirstAboveABorderThatNoStepCrosses() = runComposeUiTest {
        storage.text = "(A) Write the report +Work\n(C) Return the books +Work due:2026-11-10\nx Sharpen the pencils +Work\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        fun bounds(text: String) = onNodeWithText(text).fetchSemanticsNode().boundsInRoot

        // Within a group, a line is the border.
        controller.listGroupKey = GroupKey.PROJECT
        val line = onNodeWithTag("overdue-separator").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds("Return the books").bottom <= line.top)
        assertTrue(line.bottom <= bounds("Write the report").top)

        // Without groups, the two kinds stand below a heading each, and the completed tasks below a third one.
        controller.listGroupKey = null
        onNodeWithTag("overdue-separator").assertDoesNotExist()
        assertTrue(bounds("Overdue (1)").bottom <= bounds("Return the books").top)
        assertTrue(bounds("Return the books").bottom <= bounds("Next (1)").top)
        assertTrue(bounds("Next (1)").bottom <= bounds("Write the report").top)
        onNodeWithText("Done (1)").assertDoesNotExist()
        onNodeWithTag("filter-done").performScrollTo().performClick()
        assertTrue(bounds("Write the report").bottom <= bounds("Done (1)").top)
        assertTrue(bounds("Done (1)").bottom <= bounds("Sharpen the pencils").top)
        // The due date decides on which side of the line a task stands, not its priority.
        onAllNodesWithTag("task-down")[0].assertIsNotEnabled()
        onAllNodesWithTag("task-up")[1].assertIsNotEnabled()
    }

    // [utest->dsn~due-menu~1]
    @Test
    fun theMenuOfATaskMovesItsDueDate() = runComposeUiTest {
        start()
        onNodeWithTag("due-menu").assertDoesNotExist()
        onNodeWithText("Post signs").performMouseInput { rightClick() }
        onNodeWithTag("due-2026-11-13").performClick()
        assertEquals("Post signs +GarageSale due:2026-11-13", storage.text.lines()[1])
        onNodeWithTag("due-menu").assertDoesNotExist()
        // The label of the due date opens the menu as well: the first task of the list is the one that is due on 2026-11-20.
        onAllNodesWithTag("label-due", useUnmergedTree = true)[0].performClick()
        // This weekend ends on Sunday.
        onNodeWithTag("due-2026-11-15").performClick()
        assertEquals("(A) Thank Mom @phone due:2026-11-15", storage.text.lines()[2])
        // The due date is removed with the menu as well; a task without one is not offered that.
        onAllNodesWithTag("label-due", useUnmergedTree = true)[0].performClick()
        onNodeWithTag("due-none").performClick()
        assertEquals("(A) Thank Mom @phone", storage.text.lines()[2])
        onNodeWithText("Thank Mom").performMouseInput { rightClick() }
        onNodeWithTag("due-2026-12-12").assertExists()
        onNodeWithTag("due-none").assertDoesNotExist()
    }

    // [utest->dsn~deadline~1]
    @Test
    fun aDeadlineStandsNextToTheDueDateAndIsNotMoved() = runComposeUiTest {
        storage.text += "File the tax return @home deadline:2026-11-30\n"
        start()
        onNodeWithText("deadline:", substring = true, useUnmergedTree = true).assertExists()
        // The task is due on its deadline and has no due date that the menu could take away.
        onNodeWithText("File the tax return").performMouseInput { rightClick() }
        onNodeWithTag("due-none").assertDoesNotExist()
        onNodeWithTag("due-2026-11-13").performClick()
        assertEquals("File the tax return @home deadline:2026-11-30 due:2026-11-13", storage.text.lines()[4])

        openTask("File the tax return")
        onNodeWithTag("edit-deadline").assertTextContains("2026-11-30")
        onNodeWithTag("edit-deadline").performTextReplacement("soon")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        // The deadline may state a time.
        onNodeWithTag("edit-deadline").performTextReplacement("2026-12-04T12:00")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("File the tax return @home deadline:2026-12-04T12:00 due:2026-11-13", storage.text.lines()[4])
        onNodeWithText("2026-12-04T12:00", substring = true, useUnmergedTree = true).assertExists()

        // A deadline with an offset from UTC is shown as it counts on this device: that is another text than the one written.
        openTask("File the tax return")
        onNodeWithTag("edit-deadline").performTextReplacement("2026-12-04T12:00Z")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("File the tax return @home deadline:2026-12-04T12:00Z due:2026-11-13", storage.text.lines()[4])
        onNodeWithText(Task.localDeadline("2026-12-04T12:00Z")!!, substring = true, useUnmergedTree = true).assertExists()
        onNodeWithText("2026-12-04T12:00Z", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    // [utest->dsn~due-chips~1]
    @Test
    fun theEditDialogOffersTheDaysOfTheMenuForTheDueDate() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-due-2026-11-22").performScrollTo().performClick()
        onNodeWithTag("edit-due").assertTextContains("2026-11-22")
        onNodeWithTag("edit-due-2026-11-22").assertIsSelected()
        onNodeWithTag("edit-line").assertTextEquals("Post signs +GarageSale due:2026-11-22")
        // A second click on the chosen day takes the due date away again.
        onNodeWithTag("edit-due-2026-11-22").performClick()
        onNodeWithTag("edit-line").assertTextEquals("Post signs +GarageSale")
        onNodeWithTag("edit-due-2026-11-26").performClick()
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale due:2026-11-26", storage.text.lines()[1])
    }

    // [utest->dsn~due-menu~1]
    @Test
    fun theMenuOffersEachDayOnce() {
        // 2026-11-12 is a Thursday.
        assertEquals(
            listOf("2026-11-12", "2026-11-13", "2026-11-15", "2026-11-22", "2026-11-26", "2026-12-12"),
            dueChoices(LocalDate(2026, 11, 12)).map { it.second.toString() },
        )
        // On that Thursday, three days from now are this weekend.
        assertEquals(listOf("today", "tomorrow", "this weekend", "next weekend", "in two weeks", "in a month"), dueChoices(LocalDate(2026, 11, 12)).map { it.first })
        // On a Sunday, this weekend is today, and on a Saturday, it is tomorrow.
        assertEquals(listOf("today", "tomorrow", "in three days", "next weekend", "in two weeks", "in a month"), dueChoices(LocalDate(2026, 11, 15)).map { it.first })
        assertEquals(listOf("today", "tomorrow", "in three days", "next weekend", "in two weeks", "in a month"), dueChoices(LocalDate(2026, 11, 14)).map { it.first })
        // The choices stand in the order of their days: on a Friday, the weekend comes before the third day, on a Monday after it.
        assertEquals(listOf("today", "tomorrow", "this weekend", "in three days"), dueChoices(LocalDate(2026, 11, 13)).map { it.first }.take(4))
        assertEquals(listOf("today", "tomorrow", "in three days", "this weekend"), dueChoices(LocalDate(2026, 11, 16)).map { it.first }.take(4))
        // The end of January has no day a month later.
        assertEquals(LocalDate(2027, 2, 28), dueChoices(LocalDate(2027, 1, 31)).last().second)
    }

    // [utest->dsn~list-step~1]
    @Test
    fun theArrowsOfATaskMoveItsLineAndGiveItThePriorityOfTheTaskItMeets() = runComposeUiTest {
        start()
        // The list shows: Thank Mom (A, due), Schedule pickup (B), Post signs.
        onAllNodesWithTag("task-up")[0].assertIsNotEnabled()
        onAllNodesWithTag("task-down")[2].assertIsNotEnabled()
        // At the border, the task takes the priority and stays below the task it met.
        onAllNodesWithTag("task-up")[2].performClick()
        assertEquals(listOf("(B) Schedule pickup +GarageSale @phone", "(B) Post signs +GarageSale", "(A) Thank Mom @phone due:2026-11-20"), storage.text.lines().take(3))
        // Within the priority, the lines change places.
        onAllNodesWithTag("task-up")[2].performClick()
        assertEquals(listOf("(B) Post signs +GarageSale", "(B) Schedule pickup +GarageSale @phone"), storage.text.lines().take(2))
        assertEquals("Move task up: Post signs", storage.changes.last())
        // In the next priority, the due date orders the tasks, so the one without due date cannot pass.
        onAllNodesWithTag("task-up")[1].performClick()
        assertEquals("(A) Post signs +GarageSale", storage.text.lines()[0])
        onAllNodesWithTag("task-up")[1].assertIsNotEnabled()
        onAllNodesWithTag("task-down")[1].performClick()
        assertEquals("(B) Post signs +GarageSale", storage.text.lines()[0])
    }

    /** Drags the task with the mouse to the upper or lower part of the card of another task, and holds it there. */
    private fun ComposeUiTest.dragTo(description: String, target: String, before: Boolean) {
        val from = onNodeWithText(description).fetchSemanticsNode().boundsInRoot
        val to = onNodeWithText(target).fetchSemanticsNode().boundsInRoot
        onNodeWithText(description).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-10f, 0f))
            moveBy(Offset(to.center.x - from.center.x, (if (before) to.top + 5 else to.bottom - 5) - from.center.y))
        }
        onNodeWithTag("task-dragged").assertExists()
    }

    // [utest->dsn~list-drag~1]
    @Test
    fun draggingATaskInTheListPutsItNextToAnotherOneWithThePriorityOfThatOne() = runComposeUiTest {
        storage.text += "Paint the fence\n"
        start()
        // The list shows: Thank Mom (A, due), Schedule pickup (B), Post signs, Paint the fence.
        dragTo("Paint the fence", "Schedule pickup", before = true)
        onNodeWithTag("drop-line").assertExists()
        onNodeWithTag("task-list").performMouseInput { release() }
        assertEquals(listOf("(B) Paint the fence", "(B) Schedule pickup +GarageSale @phone", "Post signs +GarageSale"), storage.text.lines().take(3))
        assertEquals("Move task: Paint the fence", storage.changes.last())
        onNodeWithTag("task-dragged").assertDoesNotExist()

        // Behind a task of the same priority: only the line moves, as it is.
        dragTo("Paint the fence", "Schedule pickup", before = false)
        onNodeWithTag("task-list").performMouseInput { release() }
        assertEquals(listOf("(B) Schedule pickup +GarageSale @phone", "(B) Paint the fence", "Post signs +GarageSale"), storage.text.lines().take(3))

        // A task that is due on another day cannot be passed within its priority: there is no place to drop.
        storage.text = "(A) Thank Mom @phone due:2026-11-20\n(A) Paint the fence\n"
        mainClock.advanceTimeBy(5_000)
        dragTo("Paint the fence", "Thank Mom", before = true)
        onNodeWithTag("drop-line").assertDoesNotExist()
        onNodeWithTag("task-list").performMouseInput { release() }
        assertEquals("(A) Thank Mom @phone due:2026-11-20\n(A) Paint the fence\n", storage.text)
    }

    // [utest->dsn~drag-scroll~1]
    @Test
    fun theListScrollsWhileATaskIsHeldAtItsEdge() = runComposeUiTest {
        storage.text = (1..40).joinToString("") { "Task $it\n" }
        start()
        onNodeWithText("Task 40").assertDoesNotExist()
        val list = onNodeWithTag("task-list").fetchSemanticsNode().boundsInRoot
        val card = onNodeWithText("Task 1").fetchSemanticsNode().boundsInRoot
        onNodeWithText("Task 1").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-10f, 0f))
            moveBy(Offset(0f, list.bottom - 5 - card.center.y))
        }
        // Held at the lower edge, the list scrolls to its end; the dragged task has left the window and is dropped all the same.
        mainClock.advanceTimeBy(6_000)
        onNodeWithText("Task 40").assertExists()
        onNodeWithTag("task-list").performMouseInput { release() }
        assertEquals(listOf("Task 39", "Task 40", "Task 1"), storage.text.trim().lines().takeLast(3))

    }

    // [utest->dsn~drag-scroll~1]
    @Test
    fun theListScrollsAlsoWhileATaskIsHeldWithTheFinger() = runComposeUiTest {
        storage.text = (1..40).joinToString("") { "Task $it\n" }
        start()
        val list = onNodeWithTag("task-list").fetchSemanticsNode().boundsInRoot
        val card = onNodeWithText("Task 1").fetchSemanticsNode().boundsInRoot
        // The finger takes the task after a long press.
        onNodeWithText("Task 1").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveBy(Offset(0f, 20f))
            moveBy(Offset(0f, list.bottom - 5 - card.center.y - 20))
        }
        onNodeWithTag("task-dragged").assertExists()
        mainClock.advanceTimeBy(6_000)
        onNodeWithText("Task 40").assertExists()
        onNodeWithTag("task-list").performTouchInput { up() }
        assertEquals(listOf("Task 39", "Task 40", "Task 1"), storage.text.trim().lines().takeLast(3))
    }

    // [utest->dsn~drag-scroll~1]
    @Test
    fun theBoardScrollsSidewaysWhileACardIsHeldAtItsEdge() = runComposeUiTest {
        storage.text = "(A) Alpha\n(B) Beta\n(C) Gamma\n(D) Delta\n(E) Epsilon\n(F) Zeta\nEta\n"
        start()
        onNodeWithTag("board-switch").performClick()
        val board = onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val card = onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        onNodeWithText("Alpha").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(10f, 0f))
            moveBy(Offset(board.right - 30 - card.center.x, 0f))
        }
        // Held at the right edge, the board scrolls to its last column, which takes the card.
        mainClock.advanceTimeBy(8_000)
        onNodeWithTag("board").performMouseInput { release() }
        assertEquals(listOf("Eta", "Alpha"), storage.text.trim().lines().takeLast(2))
        assertEquals("Move task to No priority: Alpha", storage.changes.last())
    }

    // [utest->dsn~board-drag-position~1]
    @Test
    fun draggingACardOntoAnotherCardOfTheBoardPutsItThere() = runComposeUiTest {
        storage.text += "(B) Paint the fence\n(A) Mow the lawn\n"
        start()
        onNodeWithTag("board-switch").performClick()
        // Column A holds Thank Mom and Mow the lawn, column B Schedule pickup and Paint the fence.
        dragTo("Paint the fence", "Schedule pickup", before = true)
        onNodeWithTag("drop-line").assertExists()
        onNodeWithTag("board").performMouseInput { release() }
        assertEquals(listOf("(B) Paint the fence", "(B) Schedule pickup +GarageSale @phone"), storage.text.lines().take(2))
        assertEquals("Move task: Paint the fence", storage.changes.last())

        // To a place in another column, which gives the task its priority.
        dragTo("Mow the lawn", "Paint the fence", before = false)
        onNodeWithTag("drop-line").assertExists()
        onNodeWithTag("board").performMouseInput { release() }
        assertEquals(listOf("(B) Paint the fence", "(B) Mow the lawn", "(B) Schedule pickup +GarageSale @phone"), storage.text.lines().take(3))
        assertEquals("Move task to B: Mow the lawn", storage.changes.last())

        // In a board by project, a card that changes its column keeps its priority: it has no place next to a card of another priority.
        onNodeWithTag("group-PROJECT").performClick()
        dragTo("Thank Mom", "Schedule pickup", before = true)
        onNodeWithTag("drop-line").assertDoesNotExist()
        onNodeWithTag("board").performMouseInput { release() }
        assertEquals("(A) Thank Mom +GarageSale @phone due:2026-11-20", storage.text.lines()[4])
    }

    // [utest->dsn~task-operations~1]
    @Test
    fun completingWritesNoDateByDefault() = runComposeUiTest {
        start()
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
        onAllNodesWithTag("task").assertCountEquals(2)
    }

    // [utest->dsn~task-operations~1]
    @Test
    fun addingAndCompletingWriteDatesWhenSwitchedOn() = runComposeUiTest {
        start(Settings(writeDates = true))
        addTask("Buy milk")
        assertEquals("2026-11-12 Buy milk", storage.text.trim().lines().last())
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x 2026-11-12 Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun theDialogShowsTheDescriptionWithoutMetadataAndTheResultingLine() = runComposeUiTest {
        start()
        openTask("Thank Mom")
        onNodeWithTag("edit-description").assertTextEquals("Description", "Thank Mom")
        onNodeWithTag("edit-line").assertTextEquals("(A) Thank Mom @phone due:2026-11-20")

        onNodeWithTag("edit-description").performTextReplacement("Thank Mom for the meatballs")
        onNodeWithTag("edit-priority-B").performClick()
        onNodeWithTag("edit-project-suggest-Family").performClick()
        onNodeWithTag("edit-line").assertTextEquals("(B) Thank Mom for the meatballs +Family @phone due:2026-11-20")

        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("(B) Thank Mom for the meatballs +Family @phone due:2026-11-20", storage.text.lines()[2])
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun savingWithoutChangeLeavesTheFileUntouched() = runComposeUiTest {
        storage.text = "Call  +Family Mom   @phone\n"
        start()
        openTask("Call Mom")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Call  +Family Mom   @phone\n", storage.text)
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun anInvalidDateBlocksSaving() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-due").performTextInput("tomorrow")
        onNodeWithTag("edit-save").assertIsNotEnabled()
        onNodeWithTag("edit-due").performTextReplacement("2026-11-30")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale due:2026-11-30", storage.text.lines()[1])
    }

    // [utest->dsn~duration~1]
    @Test
    fun theDialogSetsTheDurationAndRejectsWhatIsNone() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-duration").performTextInput("long")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-duration").performTextReplacement("1h30m")
        onNodeWithTag("edit-save").performClick()
        assertEquals("Post signs +GarageSale dur:1h30m", storage.text.lines()[1])
    }

    // [utest->dsn~edit-line~1]
    @Test
    fun theLineOfTheDialogIsEditedAsTextAndTheFieldsFollow() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-line").performScrollTo().performTextReplacement("(B) Post the signs at the corner +GarageSale @errands due:2026-11-20 dur:45m custom:kept")
        onNodeWithTag("edit-description").assertTextContains("Post the signs at the corner")
        onNodeWithTag("edit-due").assertTextContains("2026-11-20")
        onNodeWithTag("edit-duration").assertTextContains("45m")
        onNodeWithTag("edit-context-errands").assertExists()
        // A field that is edited afterwards changes the line that was typed.
        onNodeWithTag("edit-duration").performTextReplacement("1h")
        onNodeWithTag("edit-line").assertTextEquals("(B) Post the signs at the corner +GarageSale @errands due:2026-11-20 dur:1h custom:kept")
        // What is no date keeps the task from being saved, as in the field.
        onNodeWithTag("edit-line").performTextReplacement("Post signs due:soon")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-line").performTextReplacement("(B) Post the signs +GarageSale custom:kept")
        onNodeWithTag("edit-save").performClick()
        assertEquals("(B) Post the signs +GarageSale custom:kept", storage.text.lines()[1])
    }

    // [utest->dsn~travel-time~1]
    @Test
    fun theDialogSetsTheTravelTime() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-travel").performScrollTo().performTextInput("far")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-travel").performScrollTo().performTextReplacement("30m")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale tt:30m", storage.text.lines()[1])
    }

    // [utest->dsn~time-spent~1]
    @Test
    fun theDialogSetsTheTimeSpent() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-spent").performScrollTo().performTextInput("long")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-spent").performScrollTo().performTextReplacement("1h30m")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale spent:1h30m", storage.text.lines()[1])
    }

    // [utest->dsn~time-spent~1]
    @Test
    fun aBlockOfTodayAddsItsLengthToTheTimeSpentOnItsTask() = runComposeUiTest {
        storage.companions["calendar.txt"] = """
            2026-11-12 w46 Thu  09:00-09:45 Schedule pickup +GarageSale @phone. 10:00-11:30 Post signs +GarageSale. 12:00-12:30 A task that is gone
            2026-11-13 w46 Fri  09:00-10:00 Thank Mom @phone
        """.trimIndent() + "\n"
        start()
        onNodeWithTag("agenda-switch").performClick()
        // Only the blocks of today whose task is there offer it.
        onAllNodesWithTag("agenda-worked").assertCountEquals(2)
        onAllNodesWithTag("agenda-worked")[0].assertTextEquals("+45m spent")
        onAllNodesWithTag("agenda-worked")[1].assertTextEquals("+1h30m spent").performClick()
        assertEquals("Post signs +GarageSale spent:1h30m", storage.text.lines()[1])
        // A block is counted once.
        onAllNodesWithTag("agenda-worked").assertCountEquals(1)
        onNodeWithTag("agenda-worked").performClick()
        assertEquals(true, storage.text.lines().any { "Schedule pickup" in it && it.endsWith("spent:45m") })
        onAllNodesWithTag("agenda-worked").assertCountEquals(0)
    }

    // [utest->dsn~due-time~1]
    @Test
    fun theDialogSetsTheTimeByWhichATaskIsDue() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-due").performTextInput("2026-11-16")
        onNodeWithTag("edit-by").performTextInput("2 pm")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-by").performScrollTo().performTextReplacement("14:00")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Post signs +GarageSale due:2026-11-16 dueby:14:00", storage.text.lines()[1])
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun deletesATaskFromTheDialog() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-delete").performScrollTo().performClick()
        assertEquals(3, storage.text.trim().lines().size)
        onNodeWithText("Post signs").assertDoesNotExist()
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun theNewTaskButtonOpensAnEmptyDialog() = runComposeUiTest {
        start()
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-save").assertIsNotEnabled()
        onNodeWithTag("edit-description").performTextInput("Water plants")
        onNodeWithTag("edit-context-add").performTextInput("home ")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Water plants @home", storage.text.trim().lines().last())
    }

    // [utest->dsn~view-remembered~1]
    // [utest->dsn~settings-file~1]
    @Test
    fun theViewAndItsGroupsAreThereAgainAfterARestart() = runComposeUiTest {
        var saved = Settings(view = "BOARD", boardGroup = "PROJECT", listGroup = "CONTEXT")
        val controller = TodoController(saved, { storage }, saveSettings = { saved = it }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("board-switch").assertIsSelected()
        onNodeWithTag("group-PROJECT").assertIsSelected()

        onNodeWithTag("list-switch").performClick()
        onNodeWithTag("group-CONTEXT").assertIsSelected().performClick()
        onNodeWithTag("overview-switch").performClick()
        onNodeWithTag("group-HASHTAG").performClick()
        assertEquals(Settings(view = "OVERVIEW", boardGroup = "PROJECT", listGroup = "", overviewGroup = "HASHTAG"), saved)
        assertEquals(saved, Settings.fromProperties(saved.toProperties()))
        // A settings file of an earlier version, or one with a name that this version does not know, gives the list.
        assertEquals(Settings(), Settings.fromProperties(emptyMap()))
        assertEquals(View.LIST, TodoController(Settings(view = "TIMELINE"), { storage }, today = { LocalDate(2026, 11, 12) }).view)
    }

    // [utest->dsn~view-options~1]
    @Test
    fun aListWithoutGroupsSaysHowItIsOrdered() = runComposeUiTest {
        start()
        onNodeWithTag("group-none").assertTextEquals(NO_GROUPS)
        onNodeWithTag("group-PROJECT").performClick()
        onNodeWithTag("group-none").assertDoesNotExist()
        onNodeWithTag("group-PROJECT").performClick()
        onNodeWithTag("group-none").assertExists()
        // The board always has columns.
        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-none").assertDoesNotExist()
    }

    // [utest->dsn~edit-pickers~1]
    @Test
    fun aTimeAndADurationOfTheDialogAreChosenInADialogOfTheirOwn() = runComposeUiTest {
        start()
        onNodeWithTag("new-task").performClick()

        // The clock face starts at noon and is left with the time it shows.
        onNodeWithContentDescription("Choose Due time").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithTag("edit-by").assertTextContains("12:00")
        onNodeWithTag("edit-at").performTextInput("9:30")
        onNodeWithContentDescription("Choose Fixed time").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithTag("edit-at").assertTextContains("09:30")
        onNodeWithContentDescription("Choose Fixed time").performClick()
        onNodeWithText("No time").performClick()
        onNodeWithTag("edit-at").assertTextEquals("Fixed time", "")

        // The wheels start at half an hour, as planning does for a task without duration, or at the duration of the field.
        onNodeWithContentDescription("Choose Duration").performClick()
        onNodeWithTag("duration-picker").assertExists()
        onNodeWithText("OK").performClick()
        onNodeWithTag("edit-duration").assertTextContains("30m")
        onNodeWithTag("edit-travel").performTextInput("1h17m")
        onNodeWithContentDescription("Choose Travel time").performClick()
        onNodeWithText("OK").performClick()
        // The wheels have steps of five minutes.
        onNodeWithTag("edit-travel").assertTextContains("1h15m")
        onNodeWithContentDescription("Choose Travel time").performClick()
        onNodeWithText("No duration").performClick()
        onNodeWithTag("edit-travel").assertTextEquals("Travel time", "")

        // The wheels of the recurrence start at one week, or at the recurrence of the field.
        onNodeWithContentDescription("Choose Recurrence").performClick()
        onNodeWithTag("recurrence-picker").assertExists()
        onNodeWithTag("recurrence-from-the completion").assertIsSelected()
        onNodeWithText("OK").performClick()
        onNodeWithTag("edit-recurrence").assertTextContains("1w")
        onNodeWithTag("edit-recurrence").performTextReplacement("2m")
        onNodeWithContentDescription("Choose Recurrence").performClick()
        onNodeWithText("months").assertExists()
        onNodeWithTag("recurrence-from-the due date").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithTag("edit-recurrence").assertTextContains("+2m")
        onNodeWithContentDescription("Choose Recurrence").performClick()
        onNodeWithTag("recurrence-from-the due date").assertIsSelected()
        onNodeWithText("No recurrence").performClick()
        onNodeWithTag("edit-recurrence").assertTextEquals("Recurrence", "")

        // The calendar of the deadline takes the due date with one click, once the task has one.
        onNodeWithContentDescription("Choose Deadline").performClick()
        onNodeWithText("Use due date").assertDoesNotExist()
        onNodeWithText("No date").performClick()
        onNodeWithTag("edit-due").performTextInput("2026-11-20")
        onNodeWithContentDescription("Choose Deadline").performClick()
        onNodeWithText("Use due date").performClick()
        onNodeWithTag("edit-deadline").assertTextContains("2026-11-20")
    }

    // [utest->dsn~edit-dialog~1]
    // [utest->dsn~edit-resize~1]
    @Test
    fun theDialogHasAHeadingAllKnownNamesAndAWidthThatIsDraggedAndKept() = runComposeUiTest {
        storage.text += (1..12).joinToString("") { "Sort the box +Box$it\n" }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-heading").assertTextEquals("New task")
        // More names than a row or two hold.
        onNodeWithTag("edit-project-suggest-Box12").assertExists()

        fun width() = onNodeWithTag("edit-dialog").fetchSemanticsNode().size.width
        val wide = width()
        onNodeWithTag("edit-resize").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-100f, 0f))
            release()
        }
        // The dialog stays in the middle, so it loses the way of the pointer on both sides.
        val narrow = width()
        assertTrue(narrow < wide - 100, "$narrow < $wide - 100")
        assertEquals(true, controller.settings.editWidth < 760)
        onNodeWithTag("edit-cancel").performScrollTo().performClick()

        openTask("Post signs")
        onNodeWithTag("edit-heading").assertTextEquals("Edit task")
        assertEquals(narrow, width())
    }

    // [utest->dsn~restart-to-update~1]
    @Test
    fun anUpdateThatWaitsIsOfferedInTheTopBar() = runComposeUiTest {
        var update by mutableStateOf<(() -> Unit)?>(null)
        var restarted = false
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", update = update) }
        onNodeWithTag("restart-to-update").assertDoesNotExist()

        update = { restarted = true }
        onNodeWithTag("restart-to-update").assertTextEquals("Restart to update").performClick()
        assertEquals(true, restarted)
    }

    // [utest->dsn~note-images~1]
    @Test
    fun anImageIsAttachedToTheNoteOfATaskAndShownInItsDialog() = runComposeUiTest {
        // One transparent pixel.
        val png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
        storage.text = "Post signs +GarageSale note:abc.md\nSchedule pickup\n"
        storage.companions["notes/abc.md"] = "Bring tape\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        var picked: () -> ByteArray? = { png }
        setContent { App(controller, "todo.txt", pickImage = { picked() }) }

        openTask("Post signs")
        // Without a camera, as on the desktop.
        onNodeWithTag("edit-take-photo").assertDoesNotExist()
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        onNodeWithTag("edit-image-abc-1.jpg").assertExists()
        onNodeWithTag("edit-note").assertTextContains("Bring tape\n\n![](abc-1.jpg)\n")
        // A chooser that is left without an image, and one that fails.
        picked = { null }
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        picked = { error("the file is no image") }
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        onNodeWithTag("edit-image-message").assertTextEquals("Not attached: the file is no image")
        picked = { png }
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        onNodeWithTag("edit-image-message").assertDoesNotExist()
        onNodeWithTag("edit-image-abc-2.jpg").assertExists()
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(setOf("notes/abc-1.jpg", "notes/abc-2.jpg"), storage.images.keys)
        assertEquals(png.toList(), storage.images.getValue("notes/abc-1.jpg").toList())
        assertEquals("Bring tape\n\n![](abc-1.jpg)\n\n![](abc-2.jpg)\n", storage.companions["notes/abc.md"])

        // The dialog shows the images that the storage has, and names one that it has not.
        openTask("Post signs")
        onNodeWithTag("edit-image-abc-2.jpg").assertExists()
        onNodeWithTag("edit-note").performTextReplacement("![](abc-1.jpg) ![](gone.jpg)")
        onNodeWithTag("edit-image-abc-2.jpg").assertDoesNotExist()
        onNodeWithTag("edit-image-missing-gone.jpg").assertTextEquals("gone.jpg is not shown")
        onNodeWithTag("edit-cancel").performScrollTo().performClick()

        // An image whose line was removed from the note again is not written; a task without a note gets one for its image.
        openTask("Schedule pickup")
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        onNodeWithTag("edit-note").performTextReplacement("Ask for a box")
        onNodeWithTag("edit-add-image").performScrollTo().performClick()
        onNodeWithTag("edit-save").performScrollTo().performClick()
        val note = Regex("Schedule pickup note:(\\w+)\\.md").find(storage.text)!!.groupValues[1]
        assertEquals("Ask for a box\n\n![]($note-2.jpg)\n", storage.companions["notes/$note.md"])
        assertEquals(setOf("notes/abc-1.jpg", "notes/abc-2.jpg", "notes/$note-2.jpg"), storage.images.keys)

        // An image that is shared becomes a task of its own.
        assertEquals("Added (inbox): Image", runOnUiThread { controller.share("", null, png) })
        val shared = Regex("Image note:(\\w+)\\.md").find(storage.text)!!.groupValues[1]
        assertEquals("![]($shared-1.jpg)\n", storage.companions["notes/$shared.md"])
        assertEquals(png.toList(), storage.images.getValue("notes/$shared-1.jpg").toList())
    }

    // [utest->dsn~new-task-defaults~1]
    // [utest->dsn~config-defaults~1]
    @Test
    fun aNewTaskStartsWithThePriorityAndTheDueDateOfTheSettings() = runComposeUiTest {
        storage.companions.remove("koppter.yml")
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        // Without the key, a middle priority and the Sunday of the next week, which alone are no task yet.
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-line").performScrollTo().assertTextEquals("(C) due:2026-11-22")
        onNodeWithTag("edit-save").assertIsNotEnabled()
        onNodeWithTag("edit-description").performTextInput("Buy milk")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("(C) Buy milk due:2026-11-22", storage.text.trim().lines().last())

        // A priority is given by its name.
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-new-task").performScrollTo().assertTextContains("priority = C\ndue = next weekend")
        onNodeWithTag("settings-new-task").performTextReplacement("priority = 5\ndue = tomorrow")
        onNodeWithTag("settings-priorities").performScrollTo().performTextReplacement("A = 1\nB = 2\nC = 3\nD = 5")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        addTask("Call the plumber")
        assertEquals("(D) Call the plumber due:2026-11-13", storage.text.trim().lines().last())

        // Without lines, a new task starts empty, and the default does not come back.
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-new-task").performScrollTo().performTextReplacement("")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("new-task:"))
        addTask("Sweep the yard")
        assertEquals("Sweep the yard", storage.text.trim().lines().last())
    }

    // [utest->dsn~project-contexts~1]
    @Test
    fun aProjectThatIsAddedInTheDialogBringsTheContextsOfTheSettings() = runComposeUiTest {
        storage.text += "Order the tables +Fair\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-project-contexts").performScrollTo().performTextReplacement("+Fair = @errands")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("project-contexts:\n  \"+Fair\": \"@errands\""))

        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-context-errands").assertDoesNotExist()
        onNodeWithTag("edit-project-suggest-Fair").performScrollTo().performClick()
        onNodeWithTag("edit-context-errands").assertExists()
        // A context that is removed stays removed, and comes back with the project.
        onNodeWithTag("edit-context-errands").performClick()
        onNodeWithTag("edit-context-errands").assertDoesNotExist()
        onNodeWithTag("edit-project-Fair").performClick()
        onNodeWithTag("edit-project-suggest-Fair").performClick()
        onNodeWithTag("edit-description").performTextInput("Rent a van")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(true, storage.text.contains("Rent a van +Fair @errands"))

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-project-contexts").performScrollTo().assertTextContains("+Fair = @errands")
    }

    // [utest->dsn~edit-help~1]
    @Test
    fun aFieldOfTheDialogShowsItsHelpWhileThePointerRestsOnIt() = runComposeUiTest {
        start()
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-threshold").assertTextContains("Start date")
        onNodeWithText("The task is hidden and not planned before this day (t:).").assertDoesNotExist()
        onNodeWithTag("edit-threshold").performMouseInput { moveTo(center) }
        // Not at once: the pointer may be on its way to another field.
        onNodeWithText("The task is hidden and not planned before this day (t:).").assertDoesNotExist()
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("The task is hidden and not planned before this day (t:).").assertExists()
        onNodeWithTag("edit-duration").performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("The task is hidden and not planned before this day (t:).").assertDoesNotExist()
        onNodeWithText("How long the task takes, such as 30m or 2h (dur:).").assertExists()
    }

    // [utest->dsn~edit-dates~1]
    @Test
    fun theDescriptionOfTheDialogReadsDatesInWordsOnceItIsChanged() = runComposeUiTest {
        storage.text += "Plan next week\n"
        start()
        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-description").performTextInput("Water plants tomorrow")
        onNodeWithTag("edit-dates-hint", useUnmergedTree = true).assertExists()
        onNodeWithTag("edit-line").performScrollTo().assertTextEquals("Water plants due:2026-11-13")
        // A date of the fields stays.
        onNodeWithTag("edit-due-2026-11-12").performScrollTo().performClick()
        onNodeWithTag("edit-line").performScrollTo().assertTextEquals("Water plants due:2026-11-12")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Water plants due:2026-11-12", storage.text.trim().lines().last())

        // The words of a task that exists stay until its description is typed in, which starts at its end.
        openTask("Plan next week")
        onNodeWithTag("edit-dates-hint", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag("edit-line").performScrollTo().assertTextEquals("Plan next week")
        onNodeWithTag("edit-description").performTextInput(" by Fri")
        onNodeWithTag("edit-line").performScrollTo().assertTextEquals("Plan next week due:2026-11-13")
    }

    @Test
    fun aFieldOfOneLineKeepsItsHeightWhenTextIsTyped() = runComposeUiTest {
        start()
        val empty = onNodeWithTag("filter-text").fetchSemanticsNode().size.height
        onNodeWithTag("filter-text").performTextInput("mom")
        assertEquals(empty, onNodeWithTag("filter-text").fetchSemanticsNode().size.height)
    }

    // [utest->dsn~filter-label-search~1]
    @Test
    fun theSearchForLabelsNarrowsWhatTheFilterPanelOffers() = runComposeUiTest {
        val labelled = MemoryStorage("Read the survey +papers>survey @research>reading #someday\nBuy paint +House @errands #money\n")
        val controller = TodoController(Settings(), { labelled }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("filter-labels-clear").assertDoesNotExist()
        // A found project keeps the project above it; the case of the letters does not count.
        onNodeWithTag("filter-labels").performScrollTo().performTextInput("SURV")
        onNodeWithTag("filter-project-papers").assertExists()
        onNodeWithTag("filter-project-papers>survey").assertExists()
        onNodeWithTag("filter-project-House").assertDoesNotExist()
        onNodeWithTag("filter-context-errands").assertDoesNotExist()
        onNodeWithTag("filter-hashtag-money").assertDoesNotExist()
        // The tasks stay as they are.
        onAllNodesWithTag("task").assertCountEquals(2)

        // The sign of a kind of label restricts the search to that kind.
        onNodeWithTag("filter-labels").performTextReplacement("@e")
        onNodeWithTag("filter-context-errands").assertExists()
        onNodeWithTag("filter-context-research").assertExists()
        onNodeWithTag("filter-context-research>reading").assertExists()
        onNodeWithTag("filter-project-House").assertDoesNotExist()
        onNodeWithTag("filter-hashtag-money").assertDoesNotExist()

        // A selected label stays whatever is searched.
        onNodeWithTag("filter-context-errands").performClick()
        onNodeWithTag("filter-labels").performTextReplacement("#some")
        onNodeWithTag("filter-context-errands").assertIsOn()
        onNodeWithTag("filter-context-research").assertDoesNotExist()
        onNodeWithTag("filter-hashtag-someday").assertExists()

        onNodeWithTag("filter-labels-clear").performClick()
        onNodeWithTag("filter-labels").assertTextEquals("Search labels", "")
        onNodeWithTag("filter-project-House").assertExists()
        onNodeWithTag("filter-context-research").assertExists()
        onNodeWithTag("filter-hashtag-money").assertExists()
    }

    // [utest->dsn~filter-panel~1]
    @Test
    fun theTextOfTheFilterOfANarrowWindowLiesInTheDrawerThatTheSearchButtonOpensAndLeadsFromTheWeekToTheList() = runDesktopComposeUiTest(width = 412, height = 760) {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        // The panel with the other criteria and the field are closed.
        onNodeWithTag("filter-heading").assertIsNotDisplayed()
        onNodeWithTag("filter-text").assertIsNotDisplayed()
        controller.view = View.WEEK
        onNodeWithTag("week").assertExists()
        onNodeWithTag("open-search").performClick()
        onNodeWithTag("filter-heading").assertIsDisplayed()
        onNodeWithTag("filter-text").assertIsDisplayed().assertIsFocused().performTextInput("mom")
        onNodeWithTag("week").assertDoesNotExist()
        onAllNodesWithTag("task").assertCountEquals(1)
    }

    // [utest->dsn~filter-panel~1]
    @Test
    fun theFilterPanelRestrictsTheList() = runComposeUiTest {
        start()
        onNodeWithTag("filter-heading").assertTextEquals("Filter")
        onNodeWithTag("filter-project-GarageSale").assertIsOff().performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-project-GarageSale").assertIsOn()
        onNodeWithTag("filter-priority-B").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        // A click on a selected label takes it out of the filter again.
        onNodeWithTag("filter-priority-B").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-clear").performClick()
        onAllNodesWithTag("task").assertCountEquals(3)
        // Open and done are two switches; switching off the only selected one selects the other.
        onNodeWithTag("filter-done").performScrollTo().performClick()
        onAllNodesWithTag("task").assertCountEquals(4)
        onNodeWithTag("filter-open").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        onNodeWithText("Call Mom").assertExists()
        onNodeWithTag("filter-done").performClick()
        onNodeWithTag("filter-open").assertIsSelected()
        onAllNodesWithTag("task").assertCountEquals(3)
    }

    // [utest->dsn~reload-on-external-change~1]
    @Test
    fun aChangeByAnotherProgramIsPickedUp() = runComposeUiTest {
        start()
        storage.text += "Added elsewhere\n"
        mainClock.advanceTimeBy(2_500)
        onNodeWithText("Added elsewhere").assertExists()
    }

    // [utest->dsn~settings-dialog~1]
    @Test
    fun theSettingsDialogSwitchesTheWritingOfDatesOn() = runComposeUiTest {
        start()
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-write-dates").performClick()
        onNodeWithTag("settings-save").performScrollTo().performClick()
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals("x 2026-11-12 Thank Mom @phone due:2026-11-20 pri:A", storage.text.lines()[2])
    }

    // [utest->dsn~outlook-items~1]
    // [utest->dsn~outlook-tag~1]
    // [utest->dsn~outlook-complete~1]
    @Test
    fun theItemsOfOutlookAreShownAndBecomeALineWhenOneGetsAContext() = runComposeUiTest {
        val mail = "00000000A1B2C3D4"
        val task = "00000000E5F60718"
        val outlook = FakeOutlook(listOf(OutlookItem(mail, "Answer the invitation", LocalDate(2026, 11, 12)), OutlookItem(task, "Book the hotel", null)))
        val opened = outlook.opened
        val completed = outlook.completed
        val before = storage.text
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) }, outlook = outlook)
        setContent { App(controller, "todo.txt") }
        waitUntil { onAllNodesWithText("Book the hotel", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Answer the invitation", useUnmergedTree = true).assertExists()
        assertEquals(before, storage.text)

        // The label opens the item in Outlook, not the task.
        onAllNodesWithTag("label-outlook", useUnmergedTree = true)[0].performClick()
        assertEquals(1, opened.size)
        assertTrue(opened.single() in setOf(mail, task))
        onNodeWithTag("edit-dialog").assertDoesNotExist()

        // In the inbox, the item shows where it comes from, and the contexts below it are choices.
        controller.view = View.INBOX
        onAllNodesWithTag("inbox-hashtag-outlook", useUnmergedTree = true).assertCountEquals(2)
        // The task "Post signs" of the file has no context either.
        onAllNodesWithText("Give it:", useUnmergedTree = true).assertCountEquals(3)
        controller.view = View.LIST

        // A context writes the item as a line, which then stands for it.
        runOnIdle { controller.assignContext(controller.inbox.first { it.task.description == "Answer the invitation" }, "phone") }
        assertEquals("Answer the invitation @phone #outlook due:2026-11-12 ol:${Outlook.tag(mail)}", storage.text.trim().lines().last())
        assertEquals(listOf(false), controller.visible.filter { it.task.description == "Answer the invitation" }.map(Outlook::isItem))

        // Completing an item or a line that names one marks it done in Outlook.
        runOnIdle { controller.toggle(controller.visible.first { it.task.description == "Book the hotel" }) }
        assertEquals(listOf(task), completed)
        assertTrue(controller.visible.none { it.task.description == "Book the hotel" })
        runOnIdle { controller.toggle(controller.visible.first { it.task.description == "Answer the invitation" }) }
        assertEquals(listOf(task, mail), completed)
        assertTrue(storage.text.trim().lines().last().startsWith("x "))
    }

    // [utest->dsn~outlook-blocks~1]
    // [utest->dsn~outlook-busy~1]
    @Test
    fun planningAvoidsTheAppointmentsOfOutlookAndWritesTheBlocksOfTheWeeksToIt() {
        val meeting = OutlookBusy(LocalDateTime(2026, 11, 12, 0, 0), LocalDateTime(2026, 11, 26, 0, 0), false, BusyStatus.BUSY)
        val outlook = FakeOutlook(appointments = listOf(meeting))
        val controller = TodoController(Settings(outlookWeeks = 4), { storage }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 8 * 60 }, outlook = outlook)
        val result = runBlocking { controller.planWithCalendars(controller.planSettings!!, null) }!!
        val (window, blocks) = outlook.synced!!
        assertEquals(LocalDateTime(2026, 11, 12, 8, 0)..LocalDateTime(2026, 12, 10, 0, 0), window)
        assertTrue(blocks.isNotEmpty(), result.summary)
        assertTrue(blocks.all { it.subject.endsWith(" [koppter]") && it.start >= window.start && it.start < window.endInclusive })
        // The busy weeks of Outlook stay free of blocks.
        assertTrue(blocks.all { it.start >= meeting.end }, blocks.toString())
        assertTrue(result.summary.endsWith("Outlook: ${blocks.size} blocks added, 0 deleted, 0 kept."), result.summary)

        // With 0 weeks, nothing is written.
        outlook.synced = null
        controller.updateSettings(controller.settings.copy(outlookWeeks = 0))
        runBlocking { controller.planWithCalendars(controller.planSettings!!, null) }
        assertNull(outlook.synced)
    }

    // [utest->dsn~outlook-clear~1]
    @Test
    fun theBlocksOfKoppterAreRemovedFromOutlookAfterTheUserConfirmedTheirNumber() = runComposeUiTest {
        val outlook = FakeOutlook(blocks = 7)
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) }, outlook = outlook)
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-outlook-clear").performScrollTo().performClick()
        waitUntil { onAllNodesWithTag("settings-outlook-clear-confirm").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Remove 7 blocks?").assertExists()
        assertEquals(0, outlook.cleared)
        onNodeWithTag("settings-outlook-clear-confirm").performClick()
        waitUntil { onAllNodesWithTag("settings-outlook-message").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, outlook.cleared)
        onNodeWithTag("settings-outlook-message").assertTextEquals("Removed 7 blocks from the calendar of the default account.")

        // Without blocks, there is nothing to confirm.
        onNodeWithTag("settings-outlook-clear").performScrollTo().performClick()
        waitUntil { onAllNodesWithText("There are no blocks of koppter in the calendar of the default account.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, outlook.cleared)
    }

    // [utest->dsn~context-switcher-tag~1]    @Test
    fun theLabelOfAContextSwitcherTaskSwitchesToItAndAPastedLinkBecomesTheTag() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                if ("missing" in uri) error("no handler")
                opened += uri
            }
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(LocalUriHandler provides links) { App(controller, "todo.txt") } }

        // A link in the description of a new task becomes the tag.
        addTask("Review the comments contextswitcher://task/jabref/fix-npe")
        assertEquals("Review the comments cs:jabref/fix-npe", storage.text.trim().lines().last())

        onNodeWithTag("label-cs", useUnmergedTree = true).performClick()
        assertEquals(listOf("contextswitcher://switch/jabref/fix-npe"), opened)
        // The click on the label does not open the task.
        onNodeWithTag("edit-dialog").assertDoesNotExist()

        // The edit dialog has a field for the task; it takes the id or a link and removes the tag when emptied.
        openTask("Review the comments")
        onNodeWithTag("edit-cs").assertTextContains("jabref/fix-npe")
        onNodeWithTag("edit-cs").performTextReplacement("contextswitcher://task/jabref/review")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Review the comments cs:jabref/review", storage.text.trim().lines().last())
        openTask("Review the comments")
        onNodeWithTag("edit-cs").performTextReplacement("")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Review the comments", storage.text.trim().lines().last())
        openTask("Review the comments")
        onNodeWithTag("edit-cs").performTextReplacement("jabref/fix-npe")
        onNodeWithTag("edit-save").performScrollTo().performClick()

        controller.add(Task.parse("Call cs:missing"))
        onAllNodesWithTag("label-cs", useUnmergedTree = true)[1].performClick()
        onNodeWithText("Not opened: contextswitcher://switch/missing").assertExists()
    }

    // [utest->dsn~label-links~1]
    @Test
    fun theMenuOfALabelOpensTheLinkThatTheSettingsGiveIt() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(LocalUriHandler provides links) { App(controller, "todo.txt") } }

        // Without a link, the menu does not offer to open one.
        onAllNodesWithTag("label-project-GarageSale", useUnmergedTree = true)[0].performMouseInput { rightClick() }
        onNodeWithTag("copy-link").assertExists()
        onNodeWithTag("open-link").assertDoesNotExist()
        onNodeWithTag("copy-link").performClick()

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-links").performScrollTo().performTextReplacement("+GarageSale = contextswitcher://switch-category/garage-sale\n@phone = https://example.org/numbers")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("links:\n  \"+GarageSale\": \"contextswitcher://switch-category/garage-sale\""))

        // In the filter panel and at a task.
        onNodeWithTag("filter-project-GarageSale").performMouseInput { rightClick() }
        onNodeWithTag("open-link").assertTextEquals("Open link").performClick()
        onNodeWithTag("link-menu").assertDoesNotExist()
        onAllNodesWithTag("label-context-phone", useUnmergedTree = true)[0].performMouseInput { rightClick() }
        onNodeWithTag("open-link").performClick()
        assertEquals(listOf("contextswitcher://switch-category/garage-sale", "https://example.org/numbers"), opened)
        // Opening the link selects nothing.
        onNodeWithTag("filter-clear").assertIsNotEnabled()

        // The settings show what was entered.
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-links").performScrollTo().assertTextContains("+GarageSale = contextswitcher://switch-category/garage-sale\n@phone = https://example.org/numbers")
    }

    // [utest->dsn~links~1]
    @Test
    fun anAddressOfTheWebInATaskOpensInTheBrowser() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        storage.text += "Read https://example.org/adr/issues/95. +Work\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(LocalUriHandler provides links) { App(controller, "todo.txt") } }

        onNode(hasTestTag("task-description") and hasText("Read https://example.org/adr/issues/95."), useUnmergedTree = true).performFirstLinkClick()
        // The full stop ends the sentence, not the address.
        assertEquals(listOf("https://example.org/adr/issues/95"), opened)
        // The click on the address does not open the task.
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~links~1]
    @Test
    fun aLongAddressIsCutAndShownAsAWholeWhenThePointerRestsOnIt() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        val address = "https://example.org/structure/java/blob/master/docs/decisions/0001-use-records.md"
        val cut = "https://example.org/structure/java/blob/master/do\u2026"
        storage.text += "$address\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(LocalUriHandler provides links) { App(controller, "todo.txt") } }

        onNodeWithText(cut).assertExists()
        onNodeWithText(address).assertDoesNotExist()
        onNodeWithText(cut).performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(address).assertExists()
        // The pointer beside the address shows nothing.
        onNodeWithText(cut).performMouseInput { moveTo(Offset(width + 40f, centerY)) }
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(address).assertDoesNotExist()
        // The link leads to the whole address, and the file keeps it.
        onNodeWithText(cut).performFirstLinkClick()
        assertEquals(listOf(address), opened)
        assertEquals(address, storage.text.trim().lines().last())

        assertEquals("https://example.org/a", shortened("https://example.org/a"))
    }

    // [utest->dsn~links~1]
    @Test
    fun anAddressEndsBeforeTheMarkThatFollowsIt() {
        fun addresses(text: String) = webAddresses(text).map { text.substring(it) }
        assertEquals(listOf("https://example.org/a?b=1#c", "http://example.org"), addresses("See https://example.org/a?b=1#c, then (http://example.org)."))
        assertEquals(emptyList(), addresses("Call Mom at 10:00 about ftp://example.org"))
    }

    // [utest->dsn~edit-dialog~1]
    @Test
    fun aKnownNameIsSuggestedByAnyPartOfIt() = runComposeUiTest {
        storage.text += "Read the survey @research>reading\n"
        start()
        openTask("Thank Mom")
        onNodeWithTag("edit-context-add").performScrollTo().performTextInput("rea")
        onNodeWithTag("edit-context-suggest-research>reading").performClick()
        onNodeWithTag("edit-line").assertTextEquals("(A) Thank Mom @phone @research>reading due:2026-11-20")
    }

    // [utest->dsn~context-hierarchy~1]
    @Test
    fun aContextBelowAnotherShowsItsLastLevelAndFiltersByAClick() = runComposeUiTest {
        storage.text += "Read the survey @research>reading\nReview a paper @research\n"
        start()
        onAllNodesWithTag("task").assertCountEquals(5)
        // The label shows the last level; the path appears when the pointer rests on it.
        onNode(hasText("@reading") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertExists()
        onNodeWithText("@research>reading").assertDoesNotExist()
        onNodeWithTag("label-context-research>reading", useUnmergedTree = true).performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(2_000)
        onNodeWithText("@research>reading").assertExists()

        // The filter panel shows the contexts as a tree; the upper context includes the lower one.
        onNodeWithTag("filter-context-research").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-clear").performScrollTo().performClick()

        // A click on the label of a task shows the tasks of its context and does not open the task.
        onNodeWithTag("label-context-research>reading", useUnmergedTree = true).performClick()
        onNodeWithTag("edit-dialog").assertDoesNotExist()
        onAllNodesWithTag("task").assertCountEquals(1)
        onNodeWithTag("filter-context-research>reading").assertIsOn()
    }

    // [utest->dsn~project-hierarchy~1]
    @Test
    fun aProjectBelowAnotherShowsItsLastLevelAndTheFilterPanelShowsATree() = runComposeUiTest {
        storage.text += "Outline the related work +papers>survey\nPick a venue +papers\n"
        start()
        onAllNodesWithTag("task").assertCountEquals(5)
        // The label shows the last level.
        onNodeWithTag("label-project-papers>survey", useUnmergedTree = true).assertExists()
        onAllNodesWithText("+survey").assertCountEquals(2)
        // The upper project includes the lower one; selected, a project shows its path.
        onNodeWithTag("filter-project-papers").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-project-papers").performClick()
        onNodeWithTag("filter-project-papers>survey").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        onNodeWithText("+papers>survey \u00d7").assertExists()
        onNodeWithTag("filter-clear").performScrollTo().performClick()

        // A click on the label of a task shows the tasks of its project and does not open the task.
        onNodeWithTag("label-project-papers", useUnmergedTree = true).performClick()
        onNodeWithTag("edit-dialog").assertDoesNotExist()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-project-papers").assertIsOn()
    }

    // [utest->dsn~priority-names~1]
    @Test
    fun prioritiesAreShownAndTypedWithTheNamesFromTheSettingsAndTheFileKeepsTheLetters() = runComposeUiTest {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-board-columns").performScrollTo().performTextReplacement("High = (>3)\nTop = (A)")
        // The third scale offered: the first steps of the Fibonacci sequence.
        onNodeWithTag("settings-priorities-preset-2").performScrollTo().performClick()
        onNodeWithTag("settings-priorities").assertTextContains("A = 1\nB = 2\nC = 3\nD = 5\nE = 8")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        // A filter names a priority by its name or by its letter; the settings keep what was typed.
        assertEquals("High = (>3)\nTop = (A)", controller.boardColumnsText)
        controller.groupKey = null
        assertEquals(
            listOf("High" to listOf("Thank Mom", "Schedule pickup"), "Top" to listOf("Thank Mom")),
            controller.groups.map { group -> group.title to group.entries.map { it.task.description } },
        )
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("priorities:\n  \"A\": \"1\""))

        controller.add(Task.parse("(D) Buy milk @shop"))
        onNode(hasText("5") and hasAnyAncestor(hasTestTag("task-labels")), useUnmergedTree = true).assertExists()

        openTask("Buy milk")
        onNodeWithTag("edit-priority-D").assertTextEquals("5")
        onNodeWithTag("edit-priority-F").assertDoesNotExist()
        onNodeWithTag("edit-priority-E").performClick()
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("(E) Buy milk @shop", storage.text.trim().lines().last())
    }

    // [utest->dsn~share-actions~1]
    @Test
    fun aShareActionFromTheSettingsMakesATaskOfSharedText() = runComposeUiTest {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        // [utest->dsn~config-defaults~1]
        // Without actions in the configuration, the default ones are there, and the settings show them.
        // The first one takes the shared text as it is, and the other one is there to choose.
        assertEquals("Added (inbox): An article", controller.share("An article"))
        assertEquals("An article", storage.text.trim().lines().last())
        assertEquals("Added (reading): A paper", controller.share("A paper", "reading"))
        assertEquals("A paper +research @reading due:2026-11-26 dur:1h", storage.text.trim().lines().last())
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-share-actions").performScrollTo().assertTextContains("inbox = {}\nreading = {} +research @reading due:2w dur:1h")
        // With the field cleared, there is no action, and the shared text is the task.
        onNodeWithTag("settings-share-actions").performTextReplacement("")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals("Added: A book", controller.share("A book"))

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-share-actions").performScrollTo().performTextInput("inbox = @inbox\nreading = {} @research>reading due:2w dur:1h")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("share-actions:"))

        assertEquals("Added (reading): https://example.org/article", controller.share("https://example.org/article", "reading"))
        assertEquals("https://example.org/article @research>reading due:2026-11-26 dur:1h", storage.text.trim().lines().last())
        // A title that no action has, and no title, apply the first action.
        assertEquals("Added (inbox): Milk", controller.share("Milk", "gone"))
        assertEquals("Milk @inbox", storage.text.trim().lines().last())
    }

    // [utest->dsn~overview~1]
    @Test
    fun theOverviewShowsTheStateOfEachContextAndItsTasksByUrgency() = runComposeUiTest {
        storage.text += "Sweep the floor @home due:2026-11-07\nWater the plants @home\n"
        start()
        onNodeWithTag("overview-switch").performClick()
        onNodeWithTag("overview-home").assertExists()
        onNodeWithTag("overview-phone").assertExists()
        onNodeWithTag("overview-none").assertExists()
        onNodeWithText("1 overdue").assertExists()

        onNodeWithTag("overview-home").performClick()
        onAllNodesWithTag("overview-task").assertCountEquals(2)
        onNodeWithText("5 days overdue").assertExists()
        // A click on a task opens it.
        onNodeWithText("Sweep the floor").performClick()
        onNodeWithTag("edit-dialog").assertExists()
        onNodeWithTag("edit-cancel").performScrollTo().performClick()
        // A task is completed where it is shown.
        onAllNodesWithTag("task-done")[1].performClick()
        assertEquals(true, storage.text.lines().any { it.startsWith("x ") && "Water the plants" in it })
        onAllNodesWithTag("overview-task").assertCountEquals(1)
        onNodeWithTag("overview-back").performClick()
        onNodeWithTag("overview-phone").assertExists()

        // The tiles stand for contexts, projects or hashtags, and one of the three is always chosen.
        onNodeWithTag("group-CONTEXT").assertIsSelected()
        onNodeWithTag("group-PRIORITY").assertDoesNotExist()
        onNodeWithTag("overview-home").performClick()
        onNodeWithTag("group-PROJECT").performClick()
        // The tiles again, not the tasks of the tile that was open.
        onNodeWithTag("overview-GarageSale").assertExists()
        onNodeWithTag("overview-home").assertDoesNotExist()
        onNodeWithText("No project").assertExists()
        onNodeWithTag("group-PROJECT").performClick()
        onNodeWithTag("group-PROJECT").assertIsSelected()
        onNodeWithTag("group-HASHTAG").performClick()
        onNodeWithText("No hashtag").assertExists()
    }

    // [utest->dsn~plan-dialog~1]
    @Test
    fun thePlanDialogShowsWhatTheConfigurationHoldsAndWhatWasSaved() = runComposeUiTest {
        storage.companions["koppter.yml"] = """
            time-maps:
              "General": "Mon-Fri 08:00-12:00 13:00-17:00"
            context-time-maps:
              "@home": "General"
            barriers:
              "default": "year"
            limits:
              "+GarageSale": "block 45m"
        """.trimIndent() + "\n"
        start()
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-time-maps").assertTextContains("General = Mon-Fri 08:00-12:00 13:00-17:00")
        onNodeWithTag("plan-time-maps").assertTextContains("Time maps (optional)")
        onNodeWithTag("plan-context-maps").assertTextContains("@home = General")
        onNodeWithTag("plan-barriers").assertTextContains("default = year")
        onNodeWithTag("plan-limits").assertTextContains("+GarageSale = block 45m")

        // What is saved is there when the dialog is opened again.
        onNodeWithTag("plan-barriers").performScrollTo().performTextReplacement("default = month")
        onNodeWithTag("plan-update").performScrollTo().performClick()
        onNodeWithTag("plan-close").performClick()
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-barriers").assertTextContains("default = month")
        onNodeWithTag("plan-time-maps").assertTextContains("General = Mon-Fri 08:00-12:00 13:00-17:00")
    }

    // [utest->dsn~plan-dialog~1]
    // [utest->dsn~plan-names~1]
    @Test
    fun twoTasksWithTheSameTextHaveTheirOwnBlocksAndStates() = runComposeUiTest {
        // The second lecture is due tomorrow and is planned in time; the first one was due yesterday.
        storage.text = "Prepare the lecture @home dur:1h due:2026-11-11\nPrepare the lecture @home dur:1h due:2026-11-13\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 9 * 60 + 5 })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-time-maps").performTextInput("General = Mon-Fri 08:00-12:00 13:00-17:00")
        onNodeWithTag("plan-update").performScrollTo().performClick()
        onNodeWithTag("plan-summary").performScrollTo().assertTextEquals("Planned 2 tasks until 2026-11-12.")
        assertEquals(
            "2026-11-12 w46 Thu  09:15-10:15 Prepare the lecture @home (due 2026-11-11). 10:15-11:15 Prepare the lecture @home (due 2026-11-13)\n",
            storage.companions.getValue("calendar.txt"),
        )
        // What tells the tasks apart is in the plan only.
        assertEquals("Prepare the lecture @home dur:1h due:2026-11-11\nPrepare the lecture @home dur:1h due:2026-11-13\n", storage.text)
        onNodeWithTag("plan-close").performClick()
        onAllNodesWithTag("plan-LATE", useUnmergedTree = true).assertCountEquals(1)
        onAllNodesWithTag("plan-FITS", useUnmergedTree = true).assertCountEquals(1)

        // The week names the blocks as the plan does, and each block opens its own task.
        onNodeWithTag("week-switch").performClick()
        onNodeWithText("09:15 Prepare the lecture (due 2026-11-11)").assertExists()
        onNodeWithText("10:15 Prepare the lecture (due 2026-11-13)").performClick()
        onNodeWithTag("edit-due").assertTextContains("2026-11-13")
    }

    // [utest->dsn~plan-state~1]
    @Test
    fun planningWritesTheCalendarAndMarksTheTasksWithTheStateOfTheirPlan() = runComposeUiTest {
        storage.text = "(A) Thank Mom @phone dur:1h due:2026-11-12\nPost signs +GarageSale dur:2h\nPaint the fence @home dur:200h\nLearn to sail t:2027-06-01\n"
        // A Thursday, at 09:05.
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 9 * 60 + 5 })
        setContent { App(controller, "todo.txt") }
        // Nothing was planned yet: no task carries a mark.
        onAllNodesWithTag("plan-LATE", useUnmergedTree = true).assertCountEquals(0)

        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-time-maps").performTextInput("General = Mon-Fri 08:00-12:00 13:00-17:00\nEvenings = General 15:00-17:00 Noday")
        onNodeWithTag("plan-context-maps").performTextInput("@home = Evenings")
        onNodeWithTag("plan-barriers").performScrollTo().performTextInput("default = year\n@home = week")
        onNodeWithTag("plan-limits").performScrollTo().performTextInput("+GarageSale = block 45m, max 1h/day")
        onNodeWithTag("plan-update").performScrollTo().performClick()

        onNodeWithTag("plan-summary").performScrollTo().assertTextEquals("Planned 2 tasks until 2026-11-13.")
        onNodeWithTag("plan-problem").assertTextEquals("Evenings: \"Noday\" is neither days nor times")
        onNodeWithTag("plan-unplaced").assertTextEquals("Paint the fence @home (barrier 2026-11-15)")
        assertEquals(
            // An hour of the garage sale a day.
            "2026-11-12 w46 Thu  09:15-10:15 Thank Mom @phone. 10:15-11:15 Post signs +GarageSale\n" +
                "2026-11-13 w46 Fri  08:00-09:00 Post signs +GarageSale\n",
            storage.companions.getValue("calendar.txt"),
        )
        assertEquals("Update the plan", storage.changes.last())
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("time-maps:\n  \"General\": \"Mon-Fri 08:00-12:00 13:00-17:00\""))
        assertEquals(true, storage.companions.getValue("koppter.yml").contains("limits:\n  \"+GarageSale\": \"block 45m, max 1h/day\""))
        // The todo.txt file is not changed by planning.
        assertEquals(4, storage.text.trim().lines().size)
        onNodeWithTag("plan-close").performClick()
        // A task whose threshold date lies behind its barrier is neither planned nor reported, and carries no mark.
        onNodeWithTag("filter-future").performClick()
        onNodeWithText("Learn to sail").assertExists()
        onAllNodesWithTag("plan-LATE", useUnmergedTree = true).assertCountEquals(1)

        // Due today and planned for today: tight. Without due date: fits. Not planned: late.
        onNodeWithTag("plan-TIGHT", useUnmergedTree = true).assertExists()
        onNodeWithTag("plan-FITS", useUnmergedTree = true).assertExists()
        onNodeWithTag("plan-LATE", useUnmergedTree = true).assertExists()
        // The dialog shows the saved settings again.
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-context-maps").assertTextContains("@home = Evenings")
    }

    // [utest->dsn~agenda~1]
    @Test
    fun theAgendaShowsTheBlocksOfTheNextDaysAndWhatIsDueWithoutABlock() = runComposeUiTest {
        storage.text += "Sweep the floor due:2026-11-12\n"
        storage.companions["calendar.txt"] = """
            2026-11-11 w46 Wed  09:00-09:30 Yesterday
            2026-11-12 w46 Thu  09:00-09:30 Schedule pickup +GarageSale @phone. 10:00-10:30 A task that is gone
            2026-11-13 w46 Fri  09:00-10:00 Thank Mom @phone
            2026-11-25 w48 Wed  09:00-10:00 Post signs +GarageSale
        """.trimIndent() + "\n"
        start()
        onNodeWithTag("agenda-switch").performClick()

        onNodeWithTag("agenda-day-2026-11-12").assertTextEquals("Today · Thu 2026-11-12")
        onNodeWithTag("agenda-day-2026-11-13").assertTextEquals("Tomorrow · Fri 2026-11-13")
        // The day after tomorrow is shown without a block; a later day without a block is left out, as are the days after the week.
        onNodeWithTag("agenda-day-2026-11-14").assertExists()
        onNodeWithTag("agenda-day-2026-11-15").assertDoesNotExist()
        onAllNodesWithTag("agenda-block").assertCountEquals(3)
        // A block whose task is gone still tells what was planned.
        onNodeWithText("A task that is gone").assertExists()
        // Due today, but planned for no time of today.
        onNodeWithTag("agenda-due").assertExists()
        onNodeWithText("Sweep the floor").assertExists()

        // The tasks are completed where they are shown.
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals(true, storage.text.lines().any { it.startsWith("x ") && "Schedule pickup" in it })
    }

    // [utest->dsn~agenda~1]
    @Test
    fun theWeekShowsTheBlocksAsACalendar() = runComposeUiTest {
        storage.companions["calendar.txt"] = """
            2026-11-11 w46 Wed  09:00-09:30 Yesterday
            2026-11-12 w46 Thu  09:00-09:30 Schedule pickup +GarageSale @phone
            2026-11-13 w46 Fri  09:00-10:00 Thank Mom @phone
            2026-11-25 w48 Wed  09:00-10:00 Post signs +GarageSale
        """.trimIndent() + "\n"
        start()
        onNodeWithTag("week-switch").performClick()

        onNodeWithTag("week-title").assertTextEquals("Week 46 · 2026-11-09 to 2026-11-15")
        onAllNodesWithTag("week-block").assertCountEquals(3)
        // The hours share the height of the window.
        val window = onNodeWithTag("main-screen").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(onNodeWithTag("week-hours").fetchSemanticsNode().boundsInRoot.bottom > window - 24f)
        onNodeWithTag("week-next").performClick()
        onAllNodesWithTag("week-block").assertCountEquals(0)
        onNodeWithTag("week-next").performClick()
        onNodeWithTag("week-title").assertTextEquals("Week 48 · 2026-11-23 to 2026-11-29")
        onAllNodesWithTag("week-block").assertCountEquals(1)
        onNodeWithTag("week-today").performClick()

        // A click on a block opens its task.
        onNodeWithText("09:00 Thank Mom").performClick()
        onNodeWithTag("edit-dialog").assertExists()
    }

    // [utest->dsn~inbox~1]
    @Test
    fun theInboxGivesATaskItsContextWithOneClickOrByTyping() = runComposeUiTest {
        storage.text += "Sweep the floor\nMow the lawn\nRead the survey @research>reading\n"
        start()
        onNodeWithTag("inbox-switch").assertTextEquals("Inbox (3)").performClick()
        onAllNodesWithTag("inbox-task").assertCountEquals(2)
        onNodeWithTag("inbox-marked").assertExists()

        // With the pointer: a click on a context of a task.
        onAllNodesWithTag("inbox-context-phone")[0].performClick()
        assertEquals(1, storage.text.lines().count { it.endsWith(" @phone") && "Mom" !in it && "pickup" !in it })
        assertEquals(true, storage.changes.last().startsWith("Edit task: "))
        onNodeWithTag("inbox-switch").assertTextEquals("Inbox (2)")

        // With the keyboard: the field has the focus, typing narrows the contexts of the marked task, Enter gives the first.
        onNodeWithTag("inbox-typed").assertIsFocused().performTextInput("rea")
        onAllNodesWithTag("inbox-context-research").assertCountEquals(1)
        onNodeWithTag("inbox-typed").performKeyInput { pressKey(Key.Enter) }
        assertEquals(2, storage.text.lines().count { it.endsWith(" @research>reading") })
        onNodeWithTag("inbox-typed").assertTextContains("")

        // Without a context that matches, the typed text is the new context; the arrow keys do not leave the list.
        onNodeWithTag("inbox-typed").performTextInput("@garden")
        onNodeWithTag("inbox-typed").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }
        assertEquals(1, storage.text.lines().count { it.endsWith(" @garden") })
        onNodeWithText("Every task has a context.").assertExists()
    }

    // [utest->dsn~inbox~1]
    @Test
    fun theInboxCompletesATask() = runComposeUiTest {
        storage.text += "Sweep the floor\n"
        start()
        onNodeWithTag("inbox-switch").assertTextEquals("Inbox (2)").performClick()
        val done = storage.text.lines().count { it.startsWith("x ") }
        onAllNodesWithTag("task-done")[0].performClick()
        assertEquals(done + 1, storage.text.lines().count { it.startsWith("x ") })
        onNodeWithTag("inbox-switch").assertTextEquals("Inbox (1)")
    }

    // [utest->dsn~unsaved-changes~1]
    @Test
    fun escAsksBeforeChangesAreLost() = runComposeUiTest {
        start()
        fun esc(tag: String) = onNodeWithTag(tag).performKeyInput { pressKey(Key.Escape) }

        // Without a change, the dialog closes.
        openTask("Post signs")
        esc("edit-description")
        onNodeWithTag("edit-dialog").assertDoesNotExist()

        // With a change, the user may go back to the dialog, which still holds the change.
        openTask("Post signs")
        onNodeWithTag("edit-description").performTextInput(" not wanted")
        esc("edit-description")
        onNodeWithTag("discard-dialog").assertExists()
        onNodeWithTag("discard-keep").performClick()
        onNodeWithTag("edit-description").assertTextContains("Post signs not wanted")

        esc("edit-description")
        onNodeWithTag("discard-confirm").performClick()
        onNodeWithTag("edit-dialog").assertDoesNotExist()
        assertEquals(false, "Not wanted" in storage.text)

        // The other dialogs ask as well; what was planned is saved and does not count as a change.
        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-barriers").performScrollTo().performTextInput("default = year")
        esc("plan-barriers")
        onNodeWithTag("discard-keep").performClick()
        onNodeWithTag("plan-update").performScrollTo().performClick()
        waitForIdle()
        esc("plan-barriers")
        onNodeWithTag("plan-dialog").assertDoesNotExist()
    }

    // [utest->dsn~keyboard-shortcuts~1]
    @Test
    fun keysOpenANewTaskShowTheViewsAndSaveTheDialog() = runComposeUiTest {
        start()
        fun ctrl(key: Key, shift: Boolean = false) = onNodeWithTag("main-screen").performKeyInput {
            withKeysDown(listOfNotNull(Key.CtrlLeft, Key.ShiftLeft.takeIf { shift })) { pressKey(key) }
        }

        ctrl(Key.Two)
        onNodeWithTag("agenda").assertExists()
        ctrl(Key.C, shift = true)
        onNodeWithTag("week").assertExists()
        ctrl(Key.Six)
        onNodeWithTag("inbox").assertExists()
        ctrl(Key.Five)
        onNodeWithTag("inbox").assertDoesNotExist()

        ctrl(Key.F, shift = true)
        onNodeWithTag("filter-text").assertIsFocused()
        // The key of SkedPal for a new task.
        ctrl(Key.A, shift = true)
        onNodeWithTag("edit-dialog").assertExists()
        onNodeWithTag("edit-cancel").performScrollTo().performClick()
        // The key of browsers and editors for their search; the field offers to empty it once it has a text.
        ctrl(Key.F)
        onNodeWithTag("filter-text-clear").assertDoesNotExist()
        val all = onAllNodesWithTag("task").fetchSemanticsNodes().size
        onNodeWithTag("filter-text").assertIsFocused().performTextInput("mom")
        onAllNodesWithTag("task").assertCountEquals(1)
        onNodeWithTag("filter-text-clear").performClick()
        onAllNodesWithTag("task").assertCountEquals(all)
        onNodeWithTag("filter-text-clear").assertDoesNotExist()
        ctrl(Key.L, shift = true)
        onNodeWithTag("plan-dialog").assertExists()
        onNodeWithTag("plan-close").performScrollTo().performClick()

        // The filter text is reached from a view that the filter does not apply to, and keeps the other filters.
        onNodeWithTag("filter-context-phone").performClick()
        ctrl(Key.Five)
        ctrl(Key.F, shift = true)
        onNodeWithTag("week").assertDoesNotExist()
        onNodeWithTag("filter-text").assertIsFocused().performTextInput("mom")
        onAllNodesWithTag("task").assertCountEquals(1)

        // The search looks through every task: the other filters are reset, and completed tasks are found.
        onNodeWithTag("top").performClick()
        ctrl(Key.Three)
        ctrl(Key.E)
        onNodeWithTag("filter-text").assertIsFocused().performTextInput("mom")
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-clear").performClick()

        // A new task without the pointer: the description has the focus, Ctrl+Enter saves.
        ctrl(Key.N)
        onNodeWithTag("edit-description").assertIsFocused().performTextInput("Buy milk")
        onNodeWithTag("edit-description").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Enter) } }
        onNodeWithTag("edit-dialog").assertDoesNotExist()
        assertEquals(true, "Buy milk" in storage.text)
    }

    // [utest->dsn~fixed-time~1]
    @Test
    fun aTaskWithATimeIsPlannedAtThatTimeAndOnTheLaterDaysOfItsSchedule() = runComposeUiTest {
        storage.text = "Post signs dur:4h\nCall the dentist\n"
        storage.companions["ice_recur.txt"] = "weekly ; day Thursday, Friday - Lunch at:12:15 dur:45m\n"
        // A Thursday, at 09:00.
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 9 * 60 })
        setContent { App(controller, "todo.txt") }
        mainClock.advanceTimeBy(2_500)
        assertEquals("Lunch at:12:15 dur:45m", storage.text.trim().lines().last())

        openTask("Call the dentist")
        onNodeWithTag("edit-at").performScrollTo().performTextInput("noon")
        onNodeWithTag("edit-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("edit-at").performScrollTo().performTextReplacement("10:00")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals("Call the dentist at:10:00", storage.text.lines()[1])

        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-update").performScrollTo().performClick()
        waitForIdle()
        onNodeWithTag("plan-summary").performScrollTo().assertTextEquals("Planned 3 tasks until 2026-12-04.")
        val calendar = storage.companions.getValue("calendar.txt").trim().lines()
        // The signs are posted around the call and the lunch of today.
        assertEquals("2026-11-12 w46 Thu  09:00-10:00 Post signs. 10:00-10:30 Call the dentist. 10:30-12:15 Post signs. 12:15-13:00 Lunch. 13:00-14:15 Post signs", calendar[0])
        // The lunch of the next days is in no task yet: the schedule tells it, for four weeks.
        assertEquals("2026-11-13 w46 Fri  12:15-13:00 Lunch", calendar[1])
        assertEquals("2026-12-04 w49 Fri  12:15-13:00 Lunch", calendar.last())
        assertEquals(8, calendar.count { "Lunch" in it })
    }

    // [utest->dsn~busy-times~1]
    @Test
    fun nothingIsPlannedIntoTheBusyTimesOfACalendar() = runComposeUiTest {
        storage.text = "Post signs dur:1h\n"
        val ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART:20261112T090000\nDTEND:20261112T103000\nEND:VEVENT\nEND:VCALENDAR\n"
        var settings = Settings()
        val controller = TodoController(
            settings, { storage }, saveSettings = { settings = it }, today = { LocalDate(2026, 11, 12) }, minuteOfDay = { 9 * 60 },
            fetch = { address -> if ("gone" in address) error("no such calendar") else ics },
        )
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("open-plan").performClick()
        onNodeWithTag("plan-calendars").performScrollTo().performTextInput("https://calendar.example.org/secret/calendar.ics\nhttps://gone.example.org/secret.ics")
        onNodeWithTag("plan-update").performScrollTo().performClick()
        // Planning runs in a coroutine, as reading the calendars takes time.
        waitForIdle()

        // Office hours without time maps; the meeting takes the time from nine to half past ten.
        assertEquals("2026-11-12 w46 Thu  10:30-11:30 Post signs\n", storage.companions.getValue("calendar.txt"))
        onNodeWithTag("plan-problem").performScrollTo().assertTextEquals("Calendar of gone.example.org was not read: no such calendar")
        // The addresses are a setting of the device and not written next to the todo.txt file.
        assertEquals(true, settings.calendars.startsWith("https://calendar.example.org/secret/calendar.ics"))
        assertEquals(false, storage.companions.values.any { "example.org" in it })
    }

    // [utest->dsn~color-themes~1]
    @Test
    fun theSettingsDialogChoosesThePaletteAndItsVariant() = runComposeUiTest {
        var saved = Settings()
        val controller = TodoController(Settings(), { storage }, saveSettings = { saved = it }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-theme-Nord").assertIsSelected()
        onNodeWithTag("settings-theme-Everforest").performScrollTo().performClick()
        onNodeWithTag("settings-appearance-Dark").performScrollTo().performClick()

        // The colors apply at once, without the button that saves the rest.
        assertEquals(ColorTheme.EVERFOREST to Appearance.DARK, saved.theme to saved.appearance)
        onNodeWithTag("settings-theme-Everforest").assertIsSelected()
        assertEquals(saved, Settings.fromProperties(saved.toProperties()))
    }

    // [utest->dsn~share-log~1]
    @Test
    fun theSettingsShareTheLogWhereThePlatformOffersIt() = runComposeUiTest {
        var shared = 0
        var offered by mutableStateOf(false)
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", shareLog = if (offered) ({ shared++ }) else null) }
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-share-log").assertDoesNotExist()
        offered = true
        onNodeWithTag("settings-share-log").performScrollTo().performClick()
        assertEquals(1, shared)
        // The dialog stays open: sharing changes no setting.
        onNodeWithTag("settings-dialog").assertExists()
    }

    // [utest->dsn~failure-log~1]
    @Test
    fun whatFailsIsShownAndPassedToTheLog() {
        val logged = mutableListOf<Pair<String, Throwable?>>()
        val failing = object : SyncingStorage, CompanionFiles by storage {
            override var onStatus: (SyncStatus) -> Unit = {}
            override fun read() = storage.read()
            override fun version(): Any? = storage.version()
            override fun write(text: String, change: String) = error("The disk is full")
            override fun sync() {}
            override fun dates() = emptyMap<String, TaskDates>()
        }
        val controller = TodoController(
            Settings(), { failing }, today = { LocalDate(2026, 11, 12) },
            fetch = { error("No such calendar at https://example.org/secret") },
            log = { message, cause -> logged += message to cause },
        )
        controller.add(Task.parse("Buy milk"))
        assertEquals("The disk is full", controller.error)
        assertEquals("The disk is full", logged.single().first)
        assertEquals("The disk is full", logged.single().second?.message)

        val cause = IllegalStateException("no network")
        failing.onStatus(SyncStatus(SyncState.FAILED, "no network", cause))
        assertEquals("Synchronization failed: no network" to cause, logged.last())
        failing.onStatus(SyncStatus(SyncState.IDLE))
        assertEquals(2, logged.size)

        // The address of a calendar is a secret: the log names its host and the kind of failure only.
        runBlocking { controller.planWithCalendars(controller.planSettings!!, "https://example.org/secret") }
        assertEquals(true, ("Calendar of example.org was not read: IllegalStateException" to null) in logged)
        assertEquals(false, logged.any { "secret" in it.first })
    }

    // [utest->dsn~sync-status~1]
    // [utest->dsn~dates-in-dialog~1]
    @Test
    fun aSynchronizingStorageShowsItsStatusAndTheDatesOfATask() = runComposeUiTest {
        val syncing = object : SyncingStorage, TodoStorage by storage {
            override var onStatus: (SyncStatus) -> Unit = {}
            var synchronizations = 0
            override fun sync() {
                synchronizations++
            }
            override fun dates() = mapOf("Post signs +GarageSale" to TaskDates(added = LocalDate(2026, 11, 1)))
        }
        val controller = TodoController(Settings(), { syncing }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("sync-status").assertTextEquals("Synchronized")
        assertEquals(1, syncing.synchronizations)
        onNodeWithTag("sync-now").performClick()
        assertEquals(2, syncing.synchronizations)

        syncing.onStatus(SyncStatus(SyncState.FAILED, "no network"))
        onNodeWithTag("sync-status").assertTextEquals("Not synchronized: no network")

        openTask("Post signs")
        onNodeWithTag("edit-dates").assertTextEquals("Added 2026-11-01 (from the git history)")
    }

    // [utest->dsn~sync-status~1]
    @Test
    fun aPlainFileHasNoSynchronizationStatus() = runComposeUiTest {
        start()
        onNodeWithTag("sync-status").assertDoesNotExist()
    }

    // [utest->dsn~committer~1]
    @Test
    fun theCommitterIsSavedAndOpensTheStorageAgain() = runComposeUiTest {
        val git = object : GitSetupActions {
            override fun publicKey(): String? = null
            override suspend fun clone(url: String) = url
            override suspend fun check(url: String) = Unit
        }
        val opened = mutableListOf<Settings>()
        val controller = TodoController(Settings(), { opened += it; storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", git = git) }

        onNodeWithTag("open-settings").performClick()
        // The default stands in the empty field in gray, whether or not it has the focus.
        onNodeWithTag("settings-committer-name").performScrollTo().assertTextEquals("koppter", "Empty: that of the git configuration, otherwise the value in gray.", "")
        onNodeWithTag("settings-committer-name").performTextInput("Ada ")
        onNodeWithTag("settings-committer-email").performScrollTo().performTextInput("ada@example.org")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals("Ada" to "ada@example.org", controller.settings.committerName to controller.settings.committerEmail)
        // The storage commits in the new name from now on.
        assertEquals(2, opened.size)
        assertEquals("Ada", opened.last().committerName)
    }

    // [utest->dsn~git-setup-dialog~1]
    @Test
    fun theSettingsShowThePublicKeyAndCloneARepository() = runComposeUiTest {
        // The configuration is compared as a whole.
        storage.companions.remove("koppter.yml")
        val git = object : GitSetupActions {
            override fun publicKey() = "ecdsa-sha2-nistp256 AAAA koppter"
            override suspend fun clone(url: String) = if (url == "git@example.org:todo.git") {
                "/data/repository/todo.txt"
            } else {
                // As a library reports it: the same message twice, and the reason below them.
                throw IllegalStateException("remote hung up unexpectedly", IllegalStateException("remote hung up unexpectedly", NoSuchMethodError("no such repository")))
            }

            override suspend fun check(url: String) {
                clone(url)
            }
        }
        val logged = mutableListOf<String>()
        val cloned = MemoryStorage("Read the paper\n").apply { companions["koppter.yml"] = "board-columns:\n  Phone: \"@phone\"\n" }
        val controller = TodoController(
            Settings(), { if (it.todoFile == "/data/repository/todo.txt") cloned else storage }, today = { LocalDate(2026, 11, 12) },
            log = { message, cause -> logged += "$message: ${cause?.message}" },
        )
        setContent { App(controller, "todo.txt", git = git) }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-public-key").assertTextEquals("ecdsa-sha2-nistp256 AAAA koppter")
        // Without an address, there is no connection to check.
        onNodeWithTag("settings-check-connection").assertIsNotEnabled()
        onNodeWithTag("settings-file").performTextInput("git@example.org:todo.git")
        onNodeWithTag("settings-check-connection").performClick()
        onNodeWithTag("settings-file-message").assertTextEquals("Connected")
        // Nothing is cloned or opened by the check.
        onNodeWithText("Read the paper").assertDoesNotExist()
        onNodeWithTag("settings-file").performTextReplacement("git@example.org:other.git")
        onNodeWithTag("settings-check-connection").performClick()
        onNodeWithTag("settings-file-message").assertTextEquals(
            "Not connected: remote hung up unexpectedly\nCause: NoSuchMethodError: no such repository\n" +
                "Is the public SSH key of this device, shown below, registered with the server?",
        )
        // An address is cloned, not saved as a file.
        onNodeWithTag("settings-save").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("settings-clone").performScrollTo().performClick()
        // The reason, with what most often is the cause on a device with a key of its own.
        onNodeWithTag("settings-file-message").assertIsDisplayed()
            .assertTextEquals(
                "Not cloned: remote hung up unexpectedly\nCause: NoSuchMethodError: no such repository\n" +
                    "Is the public SSH key of this device, shown below, registered with the server?",
            )
        // [utest->dsn~failure-log~1]
        assertEquals(listOf("Connection check failed: remote hung up unexpectedly", "Clone failed: remote hung up unexpectedly"), logged)
        assertEquals("", controller.settings.todoFile)

        // The clone shows its tasks at once: the dialog is left, and what stood in it is written nowhere.
        onNodeWithTag("settings-board-columns").performScrollTo().performTextReplacement("Home = @home")
        onNodeWithTag("settings-file").performScrollTo().performTextReplacement("git@example.org:todo.git")
        onNodeWithTag("settings-clone").performClick()
        onNodeWithTag("settings-dialog").assertDoesNotExist()
        onNodeWithText("Read the paper").assertExists()
        assertEquals("/data/repository/todo.txt", controller.settings.todoFile)
        assertEquals("board-columns:\n  Phone: \"@phone\"\n", cloned.companions["koppter.yml"])
        assertNull(storage.companions["koppter.yml"])

        // The settings then show the configuration that the repository brings along.
        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-file").assertTextContains("/data/repository/todo.txt")
        onNodeWithTag("settings-board-columns").assertTextContains("Phone = @phone")
    }

    // [utest->dsn~settings-open-file~1]
    @Test
    fun aChosenFileIsOpenedAtOnceWithItsConfiguration() = runComposeUiTest {
        val chosen = MemoryStorage("Read the paper\n").apply { companions["koppter.yml"] = "share-actions:\n  later: \"{} due:1w\"\n" }
        val controller = TodoController(Settings(), { if (it.todoFile == "/data/other/todo.txt") chosen else storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", chooseFile = { "/data/other/todo.txt" }) }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-share-actions").performScrollTo().assertTextContains("inbox = {}\nreading = {} +research @reading due:2w dur:1h")
        onNodeWithTag("settings-choose-file").performScrollTo().performClick()
        onNodeWithTag("settings-share-actions").performScrollTo().assertTextContains("later = {} due:1w")
        assertEquals("/data/other/todo.txt", controller.settings.todoFile)
        onNodeWithTag("settings-file-message").assertExists()
        // Without Save, the file from before is open again.
        onNodeWithText("Cancel").performScrollTo().performClick()
        onNodeWithTag("settings-dialog").assertDoesNotExist()
        assertEquals("", controller.settings.todoFile)
        onNodeWithText("Thank Mom").assertExists()
        onNodeWithText("Read the paper").assertDoesNotExist()
    }

    // [utest->dsn~settings-open-file~1]
    @Test
    fun aChosenFolderIsOpenedAtOnceAndAChooserThatFailsIsReported() = runComposeUiTest {
        val chosen = MemoryStorage("Read the paper\n")
        val controller = TodoController(Settings(), { if (it.todoFile == "content://documents/tree/todo") chosen else storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", chooseFile = { error("No program shows the dialog") }, chooseFolder = { "content://documents/tree/todo" }) }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-choose-file").performScrollTo().performClick()
        onNodeWithTag("settings-file-message").assertTextEquals("Not chosen: No program shows the dialog")
        assertEquals("", controller.settings.todoFile)

        onNodeWithTag("settings-choose-folder").performScrollTo().performClick()
        onNodeWithTag("settings-file-message").assertTextContains("Opened.", substring = true)
        assertEquals("content://documents/tree/todo", controller.settings.todoFile)
        onNodeWithTag("settings-save").performScrollTo().performClick()
        onNodeWithText("Read the paper").assertExists()
    }

    // [utest->dsn~git-setup-dialog~1]
    @Test
    fun aClickOnThePublicKeyCopiesItAndAPastedPageOfGitHubBecomesTheAddressForSsh() = runComposeUiTest {
        var copied: String? = null
        val clipboard = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText() = copied?.let(::AnnotatedString)
            override fun setText(annotatedString: AnnotatedString) {
                copied = annotatedString.text
            }
        }
        val git = object : GitSetupActions {
            override fun publicKey() = "ecdsa-sha2-nistp256 AAAA koppter"
            override suspend fun clone(url: String) = error("not cloned in this test")
            override suspend fun check(url: String) = error("not checked in this test")
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        val opened = mutableListOf<String>()
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        setContent {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalClipboardManager provides clipboard, LocalUriHandler provides links) { App(controller, "todo.txt", git = git) }
        }

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-public-key").performScrollTo().performClick()
        assertEquals("ecdsa-sha2-nistp256 AAAA koppter", copied)
        onNodeWithTag("settings-public-key-help").assertTextContains("Copied to the clipboard.", substring = true)
        // Without the address of a repository of GitHub, there is no page to open.
        onNodeWithTag("settings-add-key").assertDoesNotExist()

        // Pasted text arrives at once.
        onNodeWithTag("settings-file").performScrollTo().performTextInput("https://github.com/demo-user/todo/tree/main")
        onNodeWithTag("settings-file").assertTextContains("git@github.com:demo-user/todo.git")
        onNodeWithTag("settings-clone").assertIsEnabled()

        // The page for a deploy key of that repository, with the key in the clipboard.
        copied = null
        onNodeWithTag("settings-add-key").performScrollTo().performClick()
        assertEquals("ecdsa-sha2-nistp256 AAAA koppter", copied)
        assertEquals(listOf("https://github.com/demo-user/todo/settings/keys/new"), opened)
        assertEquals("https://github.com/demo-user/my.todo/settings/keys/new", deployKeyPage(" ssh://git@github.com/demo-user/my.todo.git "))
        for (other in listOf("git@example.org:demo-user/todo.git", "/home/user/todo.txt", "")) assertEquals(null, deployKeyPage(other))

        assertEquals("git@github.com:demo-user/todo.git", sshAddress("https://github.com/demo-user/todo"))
        assertEquals("git@github.com:demo-user/todo.git", sshAddress("https://github.com/demo-user/todo.git"))
        assertEquals("git@github.com:demo-user/my.todo.git", sshAddress("http://www.github.com/demo-user/my.todo/pull/10?diff=split"))
        // Another server may offer no SSH, and an address for SSH stays as it is.
        for (other in listOf("https://example.org/demo-user/todo.git", "git@github.com:demo-user/todo.git", "https://github.com/demo-user", "/home/user/todo.txt")) assertEquals(other, sshAddress(other))
    }

    // [utest->dsn~settings-dialog~1]
    @Test
    fun theSettingsTellThatTheFilesNextToTheFileBelongToItAndWhatThePlatformSaysAboutAChosenFile() = runComposeUiTest {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", chooseFile = { null }, chooseFileNote = "A chosen file is opened alone.") }

        onNodeWithTag("open-settings").performClick()
        onNodeWithText("The files next to the todo.txt file belong to it", substring = true).assertExists()
        onNodeWithTag("settings-choose-file-note").assertTextEquals("A chosen file is opened alone.")
    }

    // [utest->dsn~keyboard-shortcuts~1]
    @Test
    fun withoutAKeyboardTheDialogOfANewTaskHasTheFocusInItsDescription() = runComposeUiTest {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", keyboard = false) }

        onNodeWithTag("new-task").performClick()
        onNodeWithTag("edit-description").assertIsFocused()
        onNodeWithTag("edit-cancel").performScrollTo().performClick()
        // An existing task is read first: the keyboard of the screen would hide half of it.
        openTask("Post signs")
        onNodeWithTag("edit-description").assertIsNotFocused()
    }

    // [utest->dsn~back~1]
    // [utest->dsn~filter-panel~1]
    @Test
    fun backClosesTheFilterPanelThenReturnsToTheListAndWarnsBeforeItLeaves() = runDesktopComposeUiTest(width = 412, height = 760) {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        // What the platform calls for its back gesture.
        var back: (() -> Unit)? = null
        var left = 0
        setContent { App(controller, "todo.txt", backHandler = { back = it }, leave = { left++ }) }

        // The panel above the list has a button that closes it.
        onNodeWithTag("open-filter").performClick()
        onNodeWithTag("filter-close").assertIsDisplayed().performClick()
        onNodeWithTag("filter-close").assertIsNotDisplayed()

        onNodeWithTag("open-filter").performClick()
        onNodeWithTag("filter-close").assertIsDisplayed()
        back!!()
        onNodeWithTag("filter-close").assertIsNotDisplayed()
        onNodeWithText(LEAVE_WARNING).assertDoesNotExist()

        controller.view = View.WEEK
        onNodeWithTag("week").assertExists()
        back!!()
        assertEquals(View.LIST, controller.view)
        onNodeWithTag("week").assertDoesNotExist()
        onNodeWithText(LEAVE_WARNING).assertDoesNotExist()

        // From the list with nothing above it, back warns, and leaves when it follows while the warning is shown.
        mainClock.autoAdvance = false
        back!!()
        mainClock.advanceTimeBy(500)
        onNodeWithText(LEAVE_WARNING).assertExists()
        assertEquals(0, left)
        back!!()
        assertEquals(1, left)
        mainClock.advanceTimeBy(500)
        onNodeWithText(LEAVE_WARNING).assertDoesNotExist()

        // A warning that went away counts no longer.
        back!!()
        mainClock.advanceTimeBy(60_000)
        onNodeWithText(LEAVE_WARNING).assertDoesNotExist()
        back!!()
        assertEquals(1, left)
    }

    // [utest->dsn~agenda~1]
    @Test
    fun theWeekShowsAWholeDayWithoutScrollingInAWindowOfAPhone() = runDesktopComposeUiTest(width = 412, height = 760) {
        storage.companions["calendar.txt"] = """
            2026-11-12 w46 Thu  06:00-06:30 Schedule pickup +GarageSale @phone
            2026-11-12 w46 Thu  22:00-23:00 Thank Mom @phone
        """.trimIndent() + "\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt", keyboard = false) }
        controller.view = View.WEEK

        onNodeWithText("06:00").assertExists()
        val window = onNodeWithTag("main-screen").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(onNodeWithTag("week-hours").fetchSemanticsNode().boundsInRoot.bottom <= window)
    }

    // [utest->dsn~git-setup-dialog~1]
    @Test
    fun onlyAnAddressOfARepositoryIsCloned() {
        for (address in listOf("git@github.com:user/todo.git", "ssh://git@example.org/todo.git", "https://example.org/todo.git")) assertTrue(isGitAddress(address), address)
        for (file in listOf("", "todo.txt", "/home/user/todo.txt", "C:\\Users\\user\\todo.txt", "content://documents/todo.txt")) assertFalse(isGitAddress(file), file)
    }

    // [utest->dsn~recurrence-on-completion~1]
    @Test
    fun completingARecurringTaskAddsItsNextOccurrence() = runComposeUiTest {
        storage.text = "Water plants due:2026-11-10 rec:1w\n"
        start()
        onNodeWithTag("task-done").performClick()
        assertEquals("x Water plants due:2026-11-10 rec:1w\nWater plants due:2026-11-19 rec:1w\n", storage.text)
        onAllNodesWithTag("task").assertCountEquals(1)
    }

    // [utest->dsn~next-actions~1]
    @Test
    fun theListShowsTheNextActionOfEachProjectAndTheOverviewTheProjectsWithoutOne() = runComposeUiTest {
        storage.text = "Book the stand +Fair @phone\nPrint the flyers +Fair\nAsk for an offer +Fair @waiting\nOutline the survey +papers>survey #someday\nFix the bike @home\n"
        start()
        onAllNodesWithTag("task").assertCountEquals(5)
        onNodeWithTag("next-actions").assertTextEquals("Only the next action of each project").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Book the stand").assertExists()
        onNodeWithText("Fix the bike").assertExists()
        // It is a criterion of the filter panel and is cleared with the others.
        onNodeWithTag("filter-clear").performClick()
        onNodeWithTag("next-actions").assertIsOff()
        onAllNodesWithTag("task").assertCountEquals(5)

        onNodeWithTag("overview-switch").performClick()
        onAllNodesWithTag("overview-stuck", useUnmergedTree = true).assertCountEquals(0)
        onNodeWithTag("group-PROJECT").performClick()
        onAllNodesWithTag("overview-stuck", useUnmergedTree = true).assertCountEquals(1)
        onNodeWithText("No next action: survey", useUnmergedTree = true).assertExists()
    }

    // [utest->dsn~app-links~1]
    @Test
    fun aLinkShowsTheTasksItNamesInTheList() = runComposeUiTest {
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("filter-context-phone").performClick()
        onNodeWithTag("next-actions").performClick()

        // The link replaces what was filtered and shows the list.
        runOnUiThread { controller.openLink("koppter://filter?project=GarageSale") }
        onNodeWithTag("board").assertDoesNotExist()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Post signs").assertExists()
        onNodeWithTag("next-actions").assertIsOff()

        // A text that is no link of the application changes nothing.
        runOnUiThread { controller.openLink("https://example.org/?project=Family") }
        onAllNodesWithTag("task").assertCountEquals(2)
    }

    // [utest->dsn~app-link-copy~1]
    @Test
    @Suppress("DEPRECATION")
    fun theMenuOfALabelCopiesTheLinkToItsTasks() = runComposeUiTest {
        storage.text = "Post signs +GarageSale @errands>city #quick\n"
        var copied: String? = null
        val clipboard = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText() = copied?.let(::AnnotatedString)
            override fun setText(annotatedString: AnnotatedString) {
                copied = annotatedString.text
            }
        }
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { CompositionLocalProvider(androidx.compose.ui.platform.LocalClipboardManager provides clipboard) { App(controller, "todo.txt") } }

        // A label of the task: the menu of the label opens, not that of the card.
        onNodeWithTag("label-project-GarageSale", useUnmergedTree = true).performMouseInput { rightClick() }
        onNodeWithTag("due-menu").assertDoesNotExist()
        onNodeWithTag("copy-link").assertTextEquals("Copy link").performClick()
        assertEquals("koppter://filter?project=GarageSale", copied)
        onNodeWithTag("link-menu").assertDoesNotExist()
        onNodeWithTag("label-hashtag-quick", useUnmergedTree = true).performMouseInput { rightClick() }
        onNodeWithTag("copy-link").performClick()
        assertEquals("koppter://filter?hashtag=quick", copied)

        // A label of the filter panel has a menu without the link and is not selected by that click.
        onNodeWithTag("filter-context-errands>city").performMouseInput { rightClick() }
        onNodeWithTag("copy-link").assertDoesNotExist()
        onNodeWithTag("rename-label").performClick()
        onNodeWithTag("rename-cancel").performClick()
        onNodeWithTag("filter-clear").assertIsNotEnabled()
        // The card still has its menu.
        onNodeWithText("Post signs").performMouseInput { rightClick() }
        onNodeWithTag("due-menu").assertExists()
    }

    // [utest->dsn~app-link-copy~1]
    @Test
    fun aLongPressOnALabelOfTheFilterPanelOpensItsMenuOnATouchScreen() = runComposeUiTest {
        storage.text = "Post signs +GarageSale @errands>city #quick\nCall Mom @phone\n"
        start()

        // The long press on a label of the filter panel opens the menu and does not select the label.
        onNodeWithTag("filter-context-phone").performTouchInput { longClick() }
        onNodeWithTag("exclude-label").assertExists()
        onNodeWithTag("rename-label").performClick()
        onNodeWithTag("rename-cancel").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)

        // A short touch selects the label as before.
        onNodeWithTag("filter-context-phone").performTouchInput { click() }
        onNodeWithTag("link-menu").assertDoesNotExist()
        onAllNodesWithTag("task").assertCountEquals(1)

        // The label of a task has no menu for a finger: a long press there picks up the card.
        onNodeWithTag("label-context-phone", useUnmergedTree = true).performTouchInput { longClick() }
        onNodeWithTag("link-menu").assertDoesNotExist()
    }

    // [utest->dsn~label-rename~1]
    @Test
    fun aLabelOfTheFilterPanelIsRenamedInAllTasks() = runComposeUiTest {
        storage.text = "Read the survey +papers>survey @desk\nx Write the paper +papers\nSort the shelf +papers2\n"
        storage.companions["ice_recur.txt"] = "weekly ; day Friday - Review the notes +papers\n"
        storage.companions["koppter.yml"] = "project-contexts:\n  \"+papers\": \"@desk\"\n"
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }

        onNodeWithTag("filter-project-papers").performClick()
        onNodeWithTag("filter-project-papers").performMouseInput { rightClick() }
        onNodeWithTag("rename-label").assertTextEquals("Rename\u2026").performClick()
        // The same name, two words and nothing are no new name.
        onNodeWithTag("rename-ok").assertIsNotEnabled()
        onNodeWithTag("rename-name").performTextReplacement("two words")
        onNodeWithTag("rename-ok").assertIsNotEnabled()
        onNodeWithTag("rename-name").performTextReplacement("articles")
        onNodeWithTag("rename-ok").performClick()

        // The project below it follows, and so do the completed task, the scheduled tasks and the filter.
        assertEquals("Read the survey +articles>survey @desk\nx Write the paper +articles\nSort the shelf +papers2\n", storage.text)
        assertEquals("weekly ; day Friday - Review the notes +articles\n", storage.companions["ice_recur.txt"])
        assertEquals("Rename +papers to +articles", storage.changes.last())
        // The contexts of the project are those of its new name.
        assertEquals(listOf("desk"), controller.contextsOf("articles"))
        assertEquals(emptyList(), controller.contextsOf("papers"))
        assertEquals(setOf("articles"), controller.filter.projects)
        onNodeWithTag("filter-project-articles").assertIsOn()
        onNodeWithTag("filter-project-papers").assertDoesNotExist()

        // A hashtag does not start with a digit.
        storage.text += "Buy paint #home\n"
        runOnUiThread { controller.load() }
        onNodeWithTag("filter-hashtag-home").performScrollTo().performMouseInput { rightClick() }
        onNodeWithTag("rename-label").performClick()
        onNodeWithTag("rename-name").performTextReplacement("2026")
        onNodeWithTag("rename-ok").assertIsNotEnabled()
        onNodeWithTag("rename-name").performTextReplacement("house")
        onNodeWithTag("rename-ok").performClick()
        assertEquals("Buy paint #house", storage.text.trim().lines().last())
    }

    // [utest->dsn~filter-exclusion~1]
    @Test
    fun theMenuOfALabelOfTheFilterPanelHidesItsTasks() = runComposeUiTest {
        storage.text = "Book the stand +Fair @phone #import\nPrint the flyers +Fair #import\nFix the bike @home\nCall Mom\n"
        start()
        onAllNodesWithTag("task").assertCountEquals(4)
        onNodeWithTag("filter-hashtag-import").performMouseInput { rightClick() }
        onNodeWithTag("exclude-label").assertTextEquals("Hide these tasks").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Fix the bike").assertExists()
        // The hashtag stands beside the title as excluded and is not offered below it.
        onNodeWithTag("filter-hashtag-import").assertDoesNotExist()
        onNodeWithTag("filter-hashtag-import-excluded").assertExists()
        // The inbox and the overview follow the filter.
        onNodeWithText("Inbox (1)").assertExists()

        // A click on the excluded label shows its tasks again.
        onNodeWithTag("filter-hashtag-import-excluded").performClick()
        onAllNodesWithTag("task").assertCountEquals(4)
        onNodeWithTag("filter-hashtag-import").assertExists()

        // Excluding a selected label ends its selection, and the exclusion is cleared with the other filters.
        onNodeWithTag("filter-project-Fair").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithTag("filter-project-Fair").performTouchInput { longClick() }
        onNodeWithTag("exclude-label").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Call Mom").assertExists()
        onNodeWithTag("filter-clear").performClick()
        onAllNodesWithTag("task").assertCountEquals(4)
    }

    // [utest->dsn~filter-remembered~1]
    @Test
    fun theFilterIsThereAgainAfterARestart() = runComposeUiTest {
        var saved = Settings()
        val controller = TodoController(Settings(), { storage }, saveSettings = { saved = it }, today = { LocalDate(2026, 11, 12) })
        setContent { App(controller, "todo.txt") }
        onNodeWithTag("filter-context-phone").performMouseInput { rightClick() }
        onNodeWithTag("exclude-label").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)
        assertEquals("koppter://filter?not-context=phone", saved.filter)

        // The application starts again with the settings it saved.
        val restarted = TodoController(saved, { storage }, today = { LocalDate(2026, 11, 12) })
        assertEquals(listOf("Post signs"), restarted.visible.map { it.task.description })

        // The filter that restricts nothing is not kept, and another file starts without the filter of this one.
        onNodeWithTag("filter-clear").performClick()
        assertEquals("", saved.filter)
        onNodeWithTag("filter-project-GarageSale").performClick()
        runOnUiThread { controller.updateSettings(controller.settings.copy(todoFile = "other.txt")) }
        assertEquals("", saved.filter)
        assertEquals(true, controller.filter.isDefault)
    }

    // [utest->dsn~top-tasks~1]
    @Test
    fun theTopViewShowsTheFirstTasksOnly() = runComposeUiTest {
        start(Settings(topCount = 2))
        onNodeWithTag("top").assertTextEquals("Only the first 2 tasks").performClick()
        onAllNodesWithTag("task").assertCountEquals(2)
        onNodeWithText("Thank Mom").assertExists()
        onNodeWithText("Schedule pickup").assertExists()
        // It is a criterion of the filter panel and is cleared with the others.
        onNodeWithTag("filter-clear").performClick()
        onNodeWithTag("top").assertIsOff()
        onAllNodesWithTag("task").assertCountEquals(3)
    }

    // [utest->dsn~view-options~1]
    @Test
    fun oneViewIsAlwaysSelected() = runComposeUiTest {
        start()
        onNodeWithTag("list-switch").assertIsSelected()
        onNodeWithTag("overview-switch").performClick()
        onNodeWithTag("overview-switch").assertIsSelected()
        // A click on the selected view leaves it selected.
        onNodeWithTag("overview-switch").performClick()
        onNodeWithTag("overview-switch").assertIsSelected()
        // The overview has tiles for contexts, projects or hashtags; the agenda has nothing to choose.
        onNodeWithTag("group-PROJECT").assertExists()
        onNodeWithTag("group-DUE").assertDoesNotExist()
        onNodeWithTag("agenda-switch").performClick()
        onNodeWithTag("group-PROJECT").assertDoesNotExist()
        onNodeWithTag("list-switch").performClick()
        onAllNodesWithTag("task").assertCountEquals(3)
    }

    // [utest->dsn~list-groups~1]
    @Test
    fun theHeadingOfAGroupStaysWhileItsTasksAreScrolled() = runComposeUiTest {
        storage.text = (1..40).joinToString("") { "Sort box $it\n" }
        start()
        val top = onNodeWithTag("list-group").fetchSemanticsNode().boundsInRoot.top
        onNodeWithTag("task-list").performScrollToIndex(30)
        onNodeWithText("Sort box 1").assertDoesNotExist()
        onNodeWithTag("list-group").assertIsDisplayed().assertTextEquals("Next (40)")
        assertEquals(top, onNodeWithTag("list-group").fetchSemanticsNode().boundsInRoot.top)
    }

    // [utest->dsn~list-groups~1]
    // [utest->dsn~view-options~1]
    @Test
    fun theListIsDividedIntoGroupsWithHeadings() = runComposeUiTest {
        storage.text += "Order the cake +Family +GarageSale\n"
        start()
        // Without groups, the open tasks that are not overdue are the only part.
        onNodeWithTag("list-group").assertTextEquals("Next (4)")
        onNodeWithTag("group-PROJECT").performClick()
        onAllNodesWithTag("list-group").assertCountEquals(3)
        onNodeWithText("+Family (1)").assertExists()
        onNodeWithText("+GarageSale (3)").assertExists()
        onNodeWithText("No project (1)").assertExists()
        // A task with two projects stands in both groups.
        onAllNodesWithText("Order the cake").assertCountEquals(2)
        // A step stays within the group: the last task of a group has no way down.
        onAllNodesWithTag("task-down")[0].assertIsNotEnabled()
        // A second click on the attribute shows the list without groups again.
        onNodeWithTag("group-PROJECT").performClick()
        onNodeWithTag("list-group").assertTextEquals("Next (4)")
        onAllNodesWithTag("task").assertCountEquals(4)
    }

    // [utest->dsn~board~1]
    // [utest->dsn~view-options~1]
    @Test
    fun theBoardShowsOneColumnPerGroup() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        onAllNodesWithTag("board-column").assertCountEquals(3)
        // A priority is drawn as at the tasks, next to the number of its tasks.
        onAllNodesWithTag("board-column-title")[0].assertTextEquals("A", "(1)")
        onNodeWithText("No priority (1)").assertExists()

        onNodeWithTag("group-PROJECT").performClick()
        onAllNodesWithTag("board-column").assertCountEquals(2)
        onNodeWithText("+GarageSale (2)").assertExists()
        onNodeWithText("No project (1)").assertExists()

        // A task on the board is completed and edited like a task in the list.
        onNodeWithText("Thank Mom").performClick()
        onNodeWithTag("edit-dialog").assertExists()
    }

    // [utest->dsn~onenote-links~1]
    @Test
    fun aLinkOfOneNotePastedIntoTheNoteBecomesTwoMarkdownLinks() = runComposeUiTest {
        start()
        openTask("Post signs")
        onNodeWithTag("edit-note").performScrollTo().performTextInput("https://example.org/view?page=7\nonenote:https://example.org/Notes.one#Signs&end")
        // The cursor stands behind the links.
        onNodeWithTag("edit-note").performTextInput("\nAsk there")
        val links = "- [OneNote Desktop](onenote:https://example.org/Notes.one#Signs&end)\n- [OneNote Web](https://example.org/view?page=7)\nAsk there"
        onNodeWithTag("edit-note").assertTextEquals("Note (Markdown)", links)
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(links, storage.companions.entries.single { it.key.startsWith("notes/") }.value)
    }

    // [utest->dsn~notes~1]
    @Test
    fun aNoteIsKeptInAFileNamedByATag() = runComposeUiTest {
        storage.text += "Water the plants note:abc.txt\n"
        storage.companions["notes/abc.txt"] = "Not the cactus"
        start()
        openTask("Post signs")
        onNodeWithTag("edit-note").performScrollTo().performTextInput("Ask the neighbors first")
        onNodeWithTag("edit-save").performScrollTo().performClick()

        val line = storage.text.lines()[1]
        val name = Regex("^Post signs \\+GarageSale note:([0-9a-z]{3}\\.md)$").matchEntire(line)!!.groupValues[1]
        assertEquals("Ask the neighbors first", storage.companions["notes/$name"])

        openTask("Post signs")
        onNodeWithTag("edit-note").assertTextEquals("Note (Markdown)", "Ask the neighbors first")
        onNodeWithTag("edit-note").performScrollTo().performTextReplacement("Ask the neighbors first, then the school")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(line, storage.text.lines()[1])
        assertEquals("Ask the neighbors first, then the school", storage.companions["notes/$name"])
        assertEquals("Edit note: Post signs", storage.changes.last())

        // A note of the add-on keeps its file with the extension it has.
        openTask("Water the plants")
        onNodeWithTag("edit-note").performScrollTo().performTextReplacement("Not the cactus, twice the fern")
        onNodeWithTag("edit-save").performScrollTo().performClick()
        assertEquals(true, "Water the plants note:abc.txt" in storage.text)
        assertEquals("Not the cactus, twice the fern", storage.companions["notes/abc.txt"])
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun scheduledTasksAreAddedOncePerDay() = runComposeUiTest {
        storage.companions["ice_recur.txt"] = "daily - (A) Check mail\nweekly ; day Friday - Take out trash\n"
        start()
        mainClock.advanceTimeBy(2_500)
        assertEquals("(A) Check mail", storage.text.trim().lines().last())
        assertEquals("Add recurring tasks", storage.changes.last())

        onNodeWithText("Check mail").assertExists()
        onAllNodesWithTag("task-done")[0].performClick()
        mainClock.advanceTimeBy(2_500)
        assertEquals(1, storage.text.lines().count { "Check mail" in it })
    }

    // [utest->dsn~ice-recur-suggestion~1]
    @Test
    fun withoutAScheduleFileTheEditorSuggestsTheWeeklyReview() = runComposeUiTest {
        // A Friday.
        val controller = TodoController(Settings(), { storage }, today = { LocalDate(2026, 11, 13) })
        setContent { App(controller, "todo.txt") }
        mainClock.advanceTimeBy(2_500)
        // Nothing is scheduled without a file.
        assertEquals(0, storage.text.lines().count { "Weekly review" in it })

        onNodeWithTag("open-schedule").performClick()
        onNodeWithTag("schedule-text").assertTextContains("weekly ; day Friday - Weekly review @review dur:1h", substring = true)
        onNodeWithTag("schedule-suggestion").assertExists()
        assertEquals(null, storage.companions["ice_recur.txt"])
        // Saving writes the entry to the file, and from then on it is a schedule like any other.
        onNodeWithTag("schedule-save").performScrollTo().performClick()
        assertEquals("weekly ; day Friday - Weekly review @review dur:1h\n", storage.companions["ice_recur.txt"])
        assertEquals("Weekly review @review dur:1h", storage.text.trim().lines().last())

        onNodeWithTag("open-schedule").performClick()
        onNodeWithTag("schedule-suggestion").assertDoesNotExist()
    }

    // [utest->dsn~ice-recur-suggestion~1]
    @Test
    fun anEmptyScheduleFileGetsNoSuggestion() = runComposeUiTest {
        storage.companions["ice_recur.txt"] = ""
        start()
        onNodeWithTag("open-schedule").performClick()
        onNodeWithTag("schedule-suggestion").assertDoesNotExist()
        onNodeWithTag("schedule-text").assertTextContains("")
    }

    // [utest->dsn~ice-recur-run~1]
    @Test
    fun scheduledTasksOfDaysWithoutARunAreAdded() = runComposeUiTest {
        // Today is Thursday; the last run was on Sunday.
        storage.companions["ice_recur.txt"] = "weekly ; day Monday - Plan the week\nweekly ; day Saturday - Clean the kitchen\n"
        start(Settings(iceRecurLastRun = "2026-11-08", writeDates = true))
        mainClock.advanceTimeBy(2_500)

        assertEquals("2026-11-09 Plan the week", storage.text.trim().lines().last())
        assertEquals(0, storage.text.lines().count { "kitchen" in it })
    }

    // [utest->dsn~ice-recur-editor~1]
    @Test
    fun theScheduleIsEditedAndANewEntryAddsItsTaskOfToday() = runComposeUiTest {
        storage.companions["ice_recur.txt"] = "weekly ; day Thursday - Put out the bin\n"
        start(Settings(iceRecurLastRun = "2026-11-12"))
        onNodeWithTag("open-schedule").performClick()
        onNodeWithTag("schedule-line-1").assertTextEquals("Line 1: next on Thu 2026-11-12")

        onNodeWithTag("schedule-text").performTextReplacement("weekly ; day Thursday - Put out the bin\ndaily - Check the mail\nsometimes - Dream\nweekly ; day Monday - Plan the week")
        onNodeWithTag("schedule-line-3").assertTextEquals("Line 3 is not understood")
        onNodeWithTag("schedule-line-4").assertTextEquals("Line 4: next on Mon 2026-11-16")
        onNodeWithTag("schedule-save").performScrollTo().performClick()

        assertEquals("weekly ; day Thursday - Put out the bin\ndaily - Check the mail\nsometimes - Dream\nweekly ; day Monday - Plan the week\n", storage.companions["ice_recur.txt"])
        assertEquals("Edit scheduled tasks", storage.changes.last())
        // Only the new entry that occurs today adds its task; the entry that was there ran already.
        assertEquals("Check the mail", storage.text.trim().lines().last())
        assertEquals(0, storage.text.lines().count { "bin" in it || "Plan" in it })
        onNodeWithText("Check the mail").assertExists()
    }

    // [utest->dsn~board-drag~1]
    @Test
    fun draggingACardToAnotherColumnChangesTheTask() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        // The columns are A, B and "No priority", 328 pixels apart; the card of column B goes to column A.
        onNodeWithText("Schedule pickup").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-30f, 0f))
            moveBy(Offset(-300f, 0f))
        }
        onNodeWithTag("task-dragged").assertExists()
        // The description now exists twice, in the card and at the pointer, so the board takes the release.
        onNodeWithTag("board").performMouseInput { release() }

        assertEquals("(A) Schedule pickup +GarageSale @phone", storage.text.lines()[0])
        assertEquals("Move task to A: Schedule pickup", storage.changes.last())
        onAllNodesWithTag("board-column-title")[0].assertTextEquals("A", "(2)")
        onNodeWithTag("task-dragged").assertDoesNotExist()
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~board-drag~1]
    @Test
    fun aCardIsDraggedWithTheFingerAfterALongPress() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        onNodeWithText("Schedule pickup").performTouchInput {
            down(center)
            advanceEventTime(1000)
            moveBy(Offset(-330f, 0f))
            up()
        }

        assertEquals("(A) Schedule pickup +GarageSale @phone", storage.text.lines()[0])
        onNodeWithTag("edit-dialog").assertDoesNotExist()
    }

    // [utest->dsn~board-defined-columns~1]
    // [utest->dsn~settings-dialog~1]
    @Test
    fun columnsDefinedInTheSettingsFormTheBoard() = runComposeUiTest {
        // The configuration is compared as a whole.
        storage.companions.remove("koppter.yml")
        start()
        onNodeWithTag("board-switch").performClick()
        // [utest->dsn~config-defaults~1]
        // Without columns in the configuration, the default ones are there, and the settings show them.
        onNodeWithTag("group-FILTER").performClick()
        onNodeWithText("Today (0)").assertExists()
        onNodeWithText("Next (3)").assertExists()
        onNodeWithText("Waiting (0)").assertExists()
        onNodeWithText("Someday (0)").assertExists()

        onNodeWithTag("open-settings").performClick()
        onNodeWithTag("settings-board-columns").assertTextContains("Today = due:<=today -@waiting\nNext = -@waiting -#someday\nWaiting = @waiting\nSomeday = #someday")
        onNodeWithTag("settings-board-columns").performTextReplacement("Phone = @phone\nSoon = due:<2w\nNothing = +Nowhere")
        onNodeWithTag("settings-save").performScrollTo().performClick()
        assertEquals(
            "# Configuration of koppter for the todo.txt file in this directory\n" +
                "board-columns:\n  \"Phone\": \"@phone\"\n  \"Soon\": \"due:<2w\"\n  \"Nothing\": \"+Nowhere\"\n",
            storage.companions["koppter.yml"],
        )
        assertEquals("Edit board columns", storage.changes.last())

        onNodeWithText("Phone (2)").assertExists()
        onNodeWithText("Soon (1)").assertExists()
        onNodeWithText("Nothing (0)").assertExists()
        onNodeWithText("A (1)").assertDoesNotExist()
    }

    // [utest->dsn~board-defined-columns~1]
    @Test
    fun columnsThatAnotherDeviceDefinedAppear() = runComposeUiTest {
        start()
        onNodeWithTag("board-switch").performClick()
        storage.companions["koppter.yml"] = "board-columns:\n  Phone: \"@phone\"\n"
        mainClock.advanceTimeBy(5_000)

        onNodeWithTag("group-FILTER").performClick()
        onNodeWithText("Phone (2)").assertExists()
    }

    // [utest->dsn~board-move-to-filter~1]
    @Test
    fun draggingACardToAColumnDefinedByAFilterMakesTheTaskMatchTheFilter() = runComposeUiTest {
        storage.companions["koppter.yml"] = "board-columns:\n  Phone: \"@phone\"\n  Garage: +GarageSale\n"
        start()
        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-FILTER").performClick()
        onNodeWithText("Garage (2)").assertExists()

        onNodeWithText("Thank Mom").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(30f, 0f))
            moveBy(Offset(300f, 0f))
        }
        onNodeWithTag("board").performMouseInput { release() }

        assertEquals("(A) Thank Mom +GarageSale @phone due:2026-11-20", storage.text.lines()[2])
        assertEquals("Move task to Garage: Thank Mom", storage.changes.last())
        onNodeWithText("Garage (3)").assertExists()
        // The task matches the filter of its former column as before.
        onNodeWithText("Phone (2)").assertExists()
    }

    // [utest->dsn~hashtags-in-ui~1]
    @Test
    fun hashtagsAreLabelsFilterCriteriaAndEditableInTheDialog() = runComposeUiTest {
        start()
        onNodeWithTag("filter-hashtag-urgent").assertDoesNotExist()

        openTask("Post signs")
        onNodeWithTag("edit-hashtag-add").performTextInput("123 ")
        onNodeWithTag("edit-hashtag-add").performTextInput("#urgent ")
        // A name is taken with or without its sign, and a currency sign starts a name as a letter does.
        onNodeWithTag("edit-hashtag-add").performTextInput("$$$ ")
        onNodeWithTag("edit-line").assertTextEquals("Post signs +GarageSale #urgent #$$$")
        // The dialog is higher than the window of the test.
        onNodeWithTag("edit-save").performScrollTo().performClick()

        assertEquals("Post signs +GarageSale #urgent #$$$", storage.text.lines()[1])
        // On the card and in the filter panel.
        onAllNodesWithText("#urgent", useUnmergedTree = true).assertCountEquals(2)
        onNodeWithTag("filter-hashtag-urgent").performClick()
        onAllNodesWithTag("task").assertCountEquals(1)

        onNodeWithTag("board-switch").performClick()
        onNodeWithTag("group-HASHTAG").performClick()
        onNodeWithText("#urgent (1)").assertExists()
    }
}
