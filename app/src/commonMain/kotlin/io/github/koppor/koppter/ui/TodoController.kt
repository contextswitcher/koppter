package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.plan.TimeLog
import io.github.koppor.koppter.latex.Latex
import io.github.koppor.koppter.latex.LatexTodo
import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.outlook.OutlookBridge
import io.github.koppor.koppter.outlook.OutlookBusy
import io.github.koppor.koppter.outlook.OutlookCalendar
import io.github.koppor.koppter.plan.Block
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.Family
import io.github.koppor.koppter.plan.RankingPeriod
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.PlanState
import io.github.koppor.koppter.plan.Planned
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.plan.busyTimes
import io.github.koppor.koppter.plan.events
import io.github.koppor.koppter.plan.plan
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.storage.NOTES_DIRECTORY
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SharedLists
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.bareAddresses
import io.github.koppor.koppter.todotxt.pageTitle
import io.github.koppor.koppter.todotxt.withLinkTitle
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Completion
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.Group
import io.github.koppor.koppter.todotxt.BoardColumn
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.todotxt.OVERVIEW_KEYS
import io.github.koppor.koppter.todotxt.listParts
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.renameLabel
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.DONE_FILE
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
import io.github.koppor.koppter.todotxt.archive
import io.github.koppor.koppter.todotxt.SUGGESTED_ICE_RECUR
import io.github.koppor.koppter.todotxt.Query
import io.github.koppor.koppter.todotxt.ShareAction
import io.github.koppor.koppter.todotxt.groupByColumns
import io.github.koppor.koppter.todotxt.parseBoardColumns
import io.github.koppor.koppter.todotxt.groupEntries
import io.github.koppor.koppter.todotxt.iceRecurOccurrences
import io.github.koppor.koppter.todotxt.iceRecurTasks
import io.github.koppor.koppter.todotxt.imageName
import io.github.koppor.koppter.todotxt.noteImages
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.hierarchyOrder
import io.github.koppor.koppter.todotxt.withAncestors
import io.github.koppor.koppter.todotxt.TodoFile
import io.github.koppor.koppter.todotxt.filterOfLink
import io.github.koppor.koppter.todotxt.SyncGuard
import io.github.koppor.koppter.todotxt.contextSwitcherLink
import io.github.koppor.koppter.todotxt.selection
import io.github.koppor.koppter.todotxt.syncOfLink
import io.github.koppor.koppter.todotxt.with
import io.github.koppor.koppter.todotxt.listOrder
import io.github.koppor.koppter.todotxt.nextActions
import io.github.koppor.koppter.todotxt.toLink
import io.github.koppor.koppter.todotxt.moveToGroup
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.isoDayNumber
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

/**
 * The state of the application and the operations on it.
 *
 * Every change is written to the storage at once; there is no unsaved state.
 */
// ponytail: file access runs on the caller's thread; move it off the UI thread when a storage is slow (network).
/** What the main screen shows of the tasks. */
// From the views that show what is up to those that change the tasks.
// Today and the list come first, as they show what to do next; the overview, which tells how things stand, comes last.
/** For how many days from today the absences of the members are read: what a family plans ahead on its calendar. */
private const val ABSENCE_DAYS = 28

enum class View(val title: String) { TODAY("Today"), LIST("List"), AGENDA("Agenda"), WEEK("Week"), BOARD("Board"), INBOX("Inbox"), OVERVIEW("Overview"), FAMILY("Family") }

/** The link that opens the dialog for a new task, in the forms that programs make of it. */
/**
 * A line of [TodoController.agenda]: a day, or a task with the times of its block if it has one.
 * [name] is what [TodoController.entryNamed] finds the task by; a line without it has no open task.
 */
data class AgendaLine(val day: LocalDate? = null, val text: String = "", val times: String? = null, val name: String? = null)

private val NEW_TASK_LINK = Regex("koppter:(//)?new/?")

class TodoController(
    settings: Settings,
    private val storageFor: (Settings) -> TodoStorage,
    private val saveSettings: (Settings) -> Unit = {},
    val today: () -> LocalDate,
    /** The minutes of today that have passed. */
    val minuteOfDay: () -> Int = { 0 },
    /** Reads the file at an address of the web; `null` if the platform cannot. */
    private val fetch: (suspend (String) -> String)? = null,
    /** Takes what failed, with its cause if there is one, for the log of the platform. */
    private val log: (message: String, cause: Throwable?) -> Unit = { _, _ -> },
    /** The To-Do list and the calendar of Outlook; `null` where the platform has none. */
    private val outlook: OutlookBridge? = null,
    /** Reads a file of the device by its path, `null` if there is none; `null` where the platform cannot, as on Android. */
    private val readFile: ((String) -> String?)? = null,
    /** Opens a file of the device at a line, with the program of the template of [Latex.command], or as the system opens it for a blank one; `null` where the platform cannot. */
    private val openFile: ((template: String, path: String, line: Int) -> Unit)? = null,
    /** Opens a link of ContextSwitcher, which takes the projects and contexts of the filter; `null` where the platform has none, as on Android. */
    private val openContextSwitcher: ((String) -> Unit)? = null,
) {
    var settings by mutableStateOf(settings)
        private set
    var file by mutableStateOf(TodoFile())
        private set
    private var shownFilter by mutableStateOf(filterOfLink(settings.filter) ?: Filter())

    /** What ContextSwitcher knows of the projects and contexts, so that a selection is neither sent twice nor sent back. */
    // [impl->dsn~contextswitcher-sync~1]
    private val syncGuard = SyncGuard(shownFilter.selection)

    /** The filter of the task list, which the settings keep so that it is there again after a restart. */
    // ponytail: the settings are saved with every change of the filter, also with every typed character of its text; delay the saving if that is felt.
    // [impl->dsn~filter-remembered~1]
    var filter: Filter
        get() = shownFilter
        set(value) {
            shownFilter = value
            updateSettings(settings.copy(filter = if (value.isDefault) "" else value.toLink()))
            // [impl->dsn~contextswitcher-sync~1]
            val send = openContextSwitcher.takeIf { settings.contextSwitcherSync }
            if (send != null && syncGuard.send(value.selection)) send(contextSwitcherLink(value.selection))
        }

    /** Whether the platform can send the filter to ContextSwitcher, which the settings then offer. */
    val syncsContextSwitcher: Boolean get() = openContextSwitcher != null

    /**
     * Takes the projects and contexts that ContextSwitcher sent with a link such as `koppter://sync?project=Fair&context=desk`;
     * the other criteria of the filter, the view and the window stay, and nothing is sent back.
     * A text that is no such link changes nothing, and so does the link while the settings do not ask for it.
     *
     * @return whether the text is such a link
     */
    // [impl->dsn~contextswitcher-sync~1]
    fun applySync(link: String): Boolean {
        val selection = syncOfLink(link) ?: return false
        if (!settings.contextSwitcherSync) return true
        syncGuard.received(selection)
        filter = filter.with(selection)
        return true
    }
    var error by mutableStateOf<String?>(null)

    /**
     * Restricts the list to its first tasks, the ones to do next.
     * With priority A and a due date, these are the most important tasks; there is no notation of its own for them.
     */
    // [impl->dsn~most-important-tasks~1]
    var topOnly by mutableStateOf(false)

    /** Restricts the list to the next actions: of each project, the first task that can be worked on. */
    // [impl->dsn~next-actions~1]
    var nextOnly by mutableStateOf(false)

    /** Set by the link `koppter://new`; the screen opens the dialog for a new task and resets it. */
    var newTaskRequested by mutableStateOf(false)

    /**
     * Shows the tasks that a link to the application names, in the list, or opens a new task for `koppter://new`;
     * a text that is no such link changes nothing.
     * The link replaces the filter, so that it shows the same tasks whatever was filtered before.
     */
    // [impl->dsn~app-links~1]
    fun openLink(link: String) {
        if (NEW_TASK_LINK.matches(link.trim())) {
            newTaskRequested = true
            return
        }
        filter = filterOfLink(link) ?: return
        topOnly = false
        nextOnly = false
        view = View.LIST
    }

    /**
     * What the main screen shows of the tasks.
     * The settings keep it, and the groups of each view with it, so that a restart shows what was shown last.
     */
    // [impl->dsn~view-remembered~1]
    var view: View
        get() = views.firstOrNull { it.name == settings.view } ?: View.LIST
        set(value) = updateSettings(settings.copy(view = value.name))

    /** The views that there are switches for: the family view only where the tasks have members. */
    // [impl->dsn~family-view~1]
    val views: List<View> get() = View.entries.filter { it != View.FAMILY || members.isNotEmpty() }

    /**
     * The members of the family, each the context of a list, in the order of the lists; none without lists.
     * A task belongs to the member whose context it has.
     */
    // [impl->dsn~family-day~1]
    var members by mutableStateOf(emptyList<String>())
        private set

    private var memberBlocks by mutableStateOf(emptyMap<String, List<TextBlock>>())

    /** The `koppter.yml` of the list of each member that has one. */
    private var memberConfigs by mutableStateOf(emptyMap<String, String>())

    /** The time maps of the list of a member, the first of which says when the tasks of the member are done; none if the tasks are not planned. */
    // [impl->dsn~member-plan~1]
    fun timeMapsOf(member: String): List<Pair<String, String>> = ConfigFile.entries(memberConfigs[member].orEmpty(), ConfigFile.TIME_MAPS)

    /** Writes the time maps of the list of a member, for every device of the family. */
    // [impl->dsn~member-plan~1]
    fun setTimeMaps(member: String, maps: List<Pair<String, String>>) = guarded {
        val lists = storage as? SharedLists ?: return@guarded
        lists.writeCompanionOf(member, ConfigFile.NAME, ConfigFile.withEntries(memberConfigs[member].orEmpty(), ConfigFile.TIME_MAPS, maps), "Set the times for the tasks of @$member")
        loadConfig()
    }

    /** What the plans of the members are made from: a change of it means planning anew. */
    val memberPlanKey: Any
        get() = members.filter { timeMapsOf(it).isNotEmpty() }.map { member ->
            Triple(member, memberConfigs[member], file.entries.map { it.task }.filter { !it.completed && Family.memberOf(it, members) == member })
        } to today()

    /**
     * Plans the open tasks of each member whose list has a time map and writes the blocks, for four weeks, into the `calendar.txt` of the list.
     * Every device does so and comes to the same plan, so that the file changes only when the tasks, the time map or the other blocks did.
     */
    // [impl->dsn~member-plan~1]
    fun updateMemberPlans() {
        val lists = storage as? SharedLists ?: return
        var written = false
        for (member in members.filter { timeMapsOf(it).isNotEmpty() }) {
            try {
                val existing = lists.readCompanionOf(member, CalendarTxt.NAME).orEmpty()
                val planned = Family.planned(file.entries, member, members, ConfigFile.planConfig(memberConfigs[member].orEmpty()), CalendarTxt.blocks(existing), today(), ABSENCE_DAYS)
                val new = CalendarTxt.withAbsences(existing, null, today(), planned = planned)
                if (new != existing) {
                    lists.writeCompanionOf(member, CalendarTxt.NAME, new, "Plan the tasks of @$member")
                    written = true
                }
            } catch (e: Exception) {
                log("The tasks of $member were not planned", e)
            }
        }
        if (written) loadConfig()
    }

    /** The blocks of the `calendar.txt` of the list of a member, such as the times at which the member is absent. */
    fun blocksOf(member: String): List<TextBlock> = memberBlocks[member].orEmpty()

    /** The period of the ranking of the family view, which the settings keep. */
    // [impl->dsn~family-view~1]
    var rankingPeriod: RankingPeriod
        get() = RankingPeriod.entries.firstOrNull { it.name == settings.rankingPeriod } ?: RankingPeriod.WEEK
        set(value) = updateSettings(settings.copy(rankingPeriod = value.name))

    val board: Boolean get() = view == View.BOARD

    /** The attribute whose values are the columns of the board; `null` for the columns the user defined. */
    var groupKey: GroupKey?
        get() = GroupKey.entries.firstOrNull { it.name == settings.boardGroup }
        set(value) = updateSettings(settings.copy(boardGroup = value?.name.orEmpty()))

    /** The attribute by whose values the list is divided into groups; `null` for a list without groups. */
    var listGroupKey: GroupKey?
        get() = GroupKey.entries.firstOrNull { it.name == settings.listGroup }
        set(value) = updateSettings(settings.copy(listGroup = value?.name.orEmpty()))

    /** What the overview has its tiles for: one of [OVERVIEW_KEYS]. */
    var overviewKey: GroupKey
        get() = OVERVIEW_KEYS.firstOrNull { it.name == settings.overviewGroup } ?: GroupKey.CONTEXT
        set(value) = updateSettings(settings.copy(overviewGroup = value.name))

    /** The configuration file next to the todo.txt file; `null` if the storage keeps no files next to the todo.txt file. */
    private var config by mutableStateOf<String?>(null)

    /** The blocks of `calendar.txt`; `null` without that file. */
    /** Since when the user is away, as `timelog.txt` says; `null` if they are not. */
    // [impl->dsn~time-log~1]
    var awaySince by mutableStateOf<LocalDateTime?>(null)
        private set

    var planBlocks by mutableStateOf<List<TextBlock>?>(null)
        private set

    /** When the tasks are planned from today on, by the text of their blocks. */
    private val planned: Map<String, Planned>? get() = planBlocks?.let { CalendarTxt.planned(it, today()) }

    /** What the plan calls the open tasks: tasks with the same text are told apart there. */
    // [impl->dsn~plan-names~1]
    private val planNames: Map<Entry, String> by derivedStateOf { CalendarTxt.names(file.entries) }

    /** The open task that a block of the plan is for; `null` if the task was completed, deleted or edited since. */
    fun entryOf(block: TextBlock): Entry? = entryNamed(block.text)

    /** The open task that the plan calls [name]; what is shown outside the application, such as a notification, names its task that way. */
    fun entryNamed(name: String): Entry? = planNames.entries.firstOrNull { it.value == name }?.key

    /**
     * What the widget and the watch show: the blocks of the plan that have not ended, under their days;
     * without such a block, the open tasks of the filtered list.
     */
    // [impl->dsn~android-widget~1]
    // [impl->dsn~watch-agenda~1]
    val agenda: List<AgendaLine>
        get() {
            val ahead = CalendarTxt.ahead(planBlocks.orEmpty(), today(), minuteOfDay())
            if (ahead.isEmpty()) return visible.filterNot { it.task.completed }.map { AgendaLine(text = it.task.description, name = planNames[it]) }
            return ahead.groupBy { it.date }.flatMap { (date, blocks) ->
                listOf(AgendaLine(day = date)) + blocks.map { block ->
                    val entry = entryOf(block)
                    AgendaLine(text = entry?.task?.description ?: block.text, times = CalendarTxt.timesText(block.span), name = block.text.takeIf { entry != null })
                }
            }
        }

    /** What the plan adds to the text of the task to tell it from another task with the same text; empty for most tasks. */
    fun planSuffix(entry: Entry): String = planNames[entry]?.removePrefix(CalendarTxt.text(entry.task)).orEmpty()

    /**
     * The blocks of the plan whose task the filter shows; all blocks without a filter,
     * as a block whose task is no longer open has no task to filter.
     */
    // [impl->dsn~plan-filter~1]
    val shownBlocks: List<TextBlock>?
        get() = if (filter.isDefault) planBlocks else planBlocks?.filter(::blockMatches)

    /** Whether the filter shows a block: without a filter every block, with one the blocks whose task matches it. */
    fun blockMatches(block: TextBlock): Boolean = filter.isDefault || entryOf(block)?.let { filter.matches(it.task, today()) } == true

    /** The open tasks that the filter shows, that are due today or earlier and have no block today. */
    // [impl->dsn~agenda~1]
    // [impl->dsn~plan-filter~1]
    val dueUnplanned: List<Entry>
        get() {
            val today = today()
            val plannedToday = planBlocks.orEmpty().filter { it.date == today }.map { it.text }.toSet()
            return tasks.filter { entry ->
                !entry.task.completed && entry.task.due.let { it != null && it <= today } && planNames[entry] !in plannedToday && filter.matches(entry.task, today)
            }.sortedWith(listOrder(today))
        }

    /** The visible open tasks without context, which wait for one. */
    // [impl->dsn~inbox~1]
    val inbox: List<Entry> get() = visible.filter { !it.task.completed && it.task.contexts.isEmpty() }

    /**
     * How many tasks the text of the filter finds in the file, done and before their start date included, and how many of them the view shows;
     * `null` without a text and in the family view, which has no search.
     */
    // [impl->dsn~filter-hidden-matches~1]
    val textMatches: Pair<Int, Int>?
        get() {
            if (filter.text.isBlank() || view == View.FAMILY) return null
            val today = today()
            val all = tasks.count { Filter(completion = Completion.ALL, text = filter.text, hideFuture = false).matches(it.task, today) }
            fun planned(days: Set<LocalDate>) = shownBlocks.orEmpty().filter { it.date in days }.mapNotNull(::entryOf).toSet()
            // ponytail: the week of today, not the week that the Week view was paged to.
            val monday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
            val shown = when (view) {
                View.TODAY -> (planned(setOf(today)) + dueUnplanned).size
                View.AGENDA -> (planned((0..6).map { today.plus(DatePeriod(days = it)) }.toSet()) + dueUnplanned).size
                View.WEEK -> planned((0..6).map { monday.plus(DatePeriod(days = it)) }.toSet()).size
                View.INBOX -> inbox.size
                else -> visible.size
            }
            return all to shown
        }

    /** Shows every task that has [text] in the list: also the completed ones, those before their start date, and not only the top or next ones. */
    fun showAll(text: String = "") {
        filter = Filter(completion = Completion.ALL, text = text, hideFuture = false)
        topOnly = false
        nextOnly = false
        view = View.LIST
    }

    /** Gives a task of the inbox its context. */
    fun assignContext(entry: Entry, context: String) = replace(entry, entry.task.edit(contexts = listOf(context)))

    /** The columns the user defined, as the configuration states them; none if the storage keeps no configuration file. */
    private val definedColumns: List<BoardColumn> get() = config?.let(ConfigFile::boardColumns).orEmpty()

    /** The columns the user defined; their filters name a priority by its letter, also where the configuration uses its name. */
    // [impl->dsn~priority-names~1]
    val boardColumns: List<BoardColumn> get() = definedColumns.map { it.copy(expression = priorityNames.toLettersInFilter(it.expression)) }

    /** The columns the user defined, one per line as `title = filter expression`; `null` if the storage cannot keep them. */
    val boardColumnsText: String? get() = config?.let { definedColumns.joinToString("\n") { "${it.title} = ${it.expression}" } }

    /** The state of the synchronization; `null` if the storage does not synchronize. */
    var syncStatus by mutableStateOf<SyncStatus?>(null)
        private set

    /** When a synchronization last succeeded; `null` before the first one. */
    var lastSynchronized by mutableStateOf<LocalDateTime?>(null)
        private set

    private var storage = storageFor(settings)
    private var loadedVersion: Any? = null

    init {
        open()
    }

    /** Loads the file and, for a synchronizing storage, starts the first synchronization. */
    // [impl->dsn~sync-status~1]
    private fun open() {
        load()
        val syncing = storage as? SyncingStorage
        syncStatus = syncing?.let { SyncStatus(SyncState.IDLE) }
        lastSynchronized = null
        syncing?.onStatus = { status ->
            // [impl->dsn~failure-log~1]
            if (status.state == SyncState.FAILED) log("Synchronization failed: ${status.message}", status.cause)
            // A storage that was replaced in the settings may still report.
            if (storage === syncing) {
                syncStatus = status
                if (status.state == SyncState.IDLE) lastSynchronized = now()
            }
        }
        sync()
    }

    fun sync() = guarded { (storage as? SyncingStorage)?.sync() }

    /** When the task was added and completed according to the history of the storage. */
    fun datesOf(task: Task): TaskDates? = try {
        (storage as? SyncingStorage)?.dates()?.get(task.toLine())
    } catch (_: Exception) {
        null
    }

    /** The open items of the To-Do list of Outlook as it was read last. */
    private var outlookItems by mutableStateOf<List<OutlookItem>>(emptyList())

    /** Whether the To-Do list of Outlook is read. */
    val readsOutlook: Boolean get() = outlook != null

    /**
     * Reads the To-Do list of Outlook again; what fails is logged and leaves the list as it was,
     * as Outlook may just not be running.
     */
    // [impl->dsn~outlook-items~1]
    suspend fun refreshOutlook() {
        val source = outlook ?: return
        try {
            outlookItems = withContext(Dispatchers.Default) { source.items() }
        } catch (e: Exception) {
            log("The To-Do list of Outlook was not read: ${e.message}", e)
        }
    }

    /** The appointments of the calendar of Outlook on the days that a calendar view shows, as they were read last. */
    var appointments by mutableStateOf<List<OutlookBusy>>(emptyList())
        private set

    /** The days, from the first to before the last, whose appointments a calendar view shows; `null` while none does. */
    private var shownDays: Pair<LocalDate, LocalDate>? = null

    /** The words by which an event of a calendar is on the sideline, as the configuration lists them. */
    val sidelineWords: List<String> get() = ConfigFile.entries(config.orEmpty(), ConfigFile.SIDELINE).map { it.first.trim() }.filter(String::isNotEmpty)

    /** Reads the appointments of Outlook on the days from [from] to before [to], which a calendar view shows from now on. */
    // [impl->dsn~outlook-appointments~1]
    suspend fun showAppointments(from: LocalDate, to: LocalDate) {
        if (shownDays != from to to) appointments = emptyList()
        shownDays = from to to
        refreshAppointments()
    }

    /** Reads the appointments of the days that a calendar view shows again; what fails is logged and leaves them as they were. */
    // [impl->dsn~outlook-appointments~1]
    suspend fun refreshAppointments() {
        val source = outlook ?: return
        val (from, to) = shownDays ?: return
        try {
            val read = withContext(Dispatchers.Default) { source.busy(outlookCalendar, from, to) }
            // Another view may have asked for other days meanwhile.
            if (shownDays == from to to) appointments = read
        } catch (e: Exception) {
            log("The calendar of Outlook was not read: ${e.message}", e)
        }
    }

    /**
     * Puts the appointment, for a recurring one its series, on the sideline in Outlook, or takes it off: its time then counts as free for the next plan, or as taken.
     * A failure is shown to the user.
     */
    // [impl->dsn~outlook-sideline~1]
    suspend fun setSideline(appointment: OutlookBusy, on: Boolean) {
        val source = outlook ?: return
        try {
            withContext(Dispatchers.Default) { source.sideline(appointment.entryId, on) }
            appointments = appointments.map { if (it.entryId == appointment.entryId) it.copy(sideline = on) else it }
        } catch (e: Exception) {
            error = "Not changed in Outlook: ${e.message ?: e}"
            log("The sideline of an appointment was not changed in Outlook", e)
        }
    }

    /** Shows the appointment, for a recurring one its series, in Outlook. */
    // [impl->dsn~outlook-appointments~1]
    fun openAppointment(appointment: OutlookBusy) {
        if (appointment.entryId.isNotEmpty()) openOutlook(Outlook.anchor(appointment.entryId))
    }

    /** The TODOs of the LaTeX documents of the projects as they were read last, by project. */
    private var latexTodos by mutableStateOf<Map<String, List<LatexTodo>>>(emptyMap())

    /** The projects with their LaTeX document, with sign, as the configuration names them. */
    val latexDocuments: List<Pair<String, String>>
        get() = ConfigFile.entries(config.orEmpty(), ConfigFile.LATEX).filter { it.first.startsWith("+") && it.second.isNotBlank() }

    /** Whether the LaTeX documents of the projects are read. */
    val readsLatex: Boolean get() = readFile != null

    /**
     * Reads the LaTeX documents that the configuration names for projects again.
     * A document that is missing, as on a device where it lies elsewhere, has no TODOs; what fails is logged.
     */
    // [impl->dsn~latex-todos~1]
    suspend fun refreshLatex() {
        val read = readFile ?: return
        val documents = latexDocuments
        latexTodos = withContext(Dispatchers.Default) {
            documents.mapNotNull { (project, path) ->
                try {
                    Latex.todos(path) { read(it) }?.let { project.drop(1) to it }
                } catch (e: Exception) {
                    log("The LaTeX document $path was not read: ${e.message}", e)
                    null
                }
            }.toMap()
        }
        // [impl->dsn~latex-rematch~1]
        val rematched = Latex.rematched(latexTodos, file.entries)
        if (rematched.isNotEmpty()) {
            val new = rematched.fold(file) { text, (line, task) -> text.replace(line.index, task) }
            write(new, "Follow changed LaTeX TODOs: ${rematched.joinToString { it.second.description }}")
        }
    }

    /** Whether the label *LaTeX* opens its TODO. */
    val opensLatex: Boolean get() = openFile != null

    /** Opens the TODO that the tag `tex:` names where it stands in its document, with the program of the settings. */
    // [impl->dsn~latex-open~1]
    fun openLatex(tag: String) {
        val open = openFile ?: return
        val todo = latexTodos.values.flatten().firstOrNull { Latex.tag(it) == tag } ?: run {
            error = "The TODO is no longer in the LaTeX document of its project."
            return
        }
        guarded { open(settings.latexEditor, todo.path, todo.line) }
    }

    /** Opens the item of Outlook that a link of [Outlook.anchor] names; returns whether the link was one. */
    // [impl->dsn~outlook-tag~1]
    fun openOutlook(link: String): Boolean {
        val entryId = Outlook.entryIdOfAnchor(link) ?: return false
        val source = outlook ?: run {
            error = "Outlook is not reachable on this device."
            return true
        }
        guarded { source.open(entryId) }
        return true
    }

    /** The tasks to show, filtered and in list order: the lines of the file and the items of Outlook that no line names. */
    // [impl->dsn~visible-tasks~1]
    // [impl->dsn~outlook-items~1]
    /** The filter that [visible] applies: a search in the list or the inbox also finds the tasks before their start date, to add a note to one of them. */
    private val searched: Filter
        get() = if (filter.text.isNotBlank() && (view == View.LIST || view == View.INBOX)) filter.copy(hideFuture = false) else filter

    /** The lines of the file and the items of Outlook and TODOs of LaTeX documents that no line names, unfiltered. */
    // [impl->dsn~outlook-items~1]
    // [impl->dsn~latex-todos~1]
    private val tasks: List<Entry>
        get() = Outlook.entries(outlookItems, file.entries).let { items -> file.entries + items + Latex.entries(latexTodos, file.entries, -1 - items.size) }

    val visible: List<Entry>
        get() = tasks.filter { searched.matches(it.task, today()) }.sortedWith(listOrder(today()))
            // [impl->dsn~next-actions~1]
            .let { if (nextOnly) nextActions(it, file.entries, today()) else it }
            // [impl->dsn~top-tasks~1]
            .let { if (topOnly) it.take(settings.topCount) else it }

    /** The visible tasks as the columns of the board. */
    // [impl->dsn~board~1]
    // [impl->dsn~board-defined-columns~1]
    val groups: List<Group>
        get() = groupKey?.let(::groupsBy) ?: groupByColumns(visible, boardColumns, today())

    /** The visible tasks as the groups of the list: the overdue, the next and the completed ones if no attribute divides the list. */
    // [impl->dsn~list-groups~1]
    val listGroups: List<Group>
        get() = listGroupKey?.let(::groupsBy) ?: listParts(visible, today())

    /** [entries] divided as the list is, for the tasks of `Today`; `null` while the list has no groups. */
    // [impl->dsn~today-groups~1]
    fun listGroupsOf(entries: List<Entry>): List<Group>? = listGroupKey?.let { groupsBy(it, entries) }

    private fun groupsBy(key: GroupKey, entries: List<Entry> = visible): List<Group> {
        val groups = groupEntries(entries, key, today())
        return if (key != GroupKey.PRIORITY) groups
        else groups.map { group -> group.value?.singleOrNull()?.let { group.copy(title = priorityNames.name(it)) } ?: group }
    }

    /** The projects of the file and the projects above them, a project directly before those below it. */
    val projects: List<String> get() = file.entries.flatMap { it.task.projects }.flatMap { it.withAncestors() }.distinct().sortedWith(hierarchyOrder)
    /** The contexts of the file and the contexts above them, a context directly before those below it. */
    val contexts: List<String> get() = file.entries.flatMap { it.task.contexts }.flatMap { it.withAncestors() }.distinct().sortedWith(hierarchyOrder)
    val hashtags: List<String> get() = file.entries.flatMap { it.task.hashtags }.distinct().sorted()
    val priorities: List<Char> get() = file.entries.mapNotNull { it.task.priority }.distinct().sorted()
    /** How many open tasks carry each project, context and hashtag, keyed with its sign (`+`, `@`, `#`); a name counts for the names above it as well. */
    val nameUses: Map<String, Int>
        get() = file.entries.filter { !it.task.completed }.flatMap { entry ->
            entry.task.projects.flatMap { it.withAncestors() }.distinct().map { "+$it" } +
                entry.task.contexts.flatMap { it.withAncestors() }.distinct().map { "@$it" } +
                entry.task.hashtags.map { "#$it" }
        }.groupingBy { it }.eachCount()

    fun load() {
        guarded {
            file = TodoFile.parse(storage.read())
            loadedVersion = storage.version()
        }
        loadConfig()
    }

    /** Reads the configuration file; a synchronization may change it while the todo.txt file stays the same. */
    // [impl->dsn~board-defined-columns~1]
    private fun loadConfig() {
        config = try {
            (storage as? CompanionFiles)?.let { it.readCompanion(ConfigFile.NAME).orEmpty() }
        } catch (_: Exception) {
            config
        }
        if (boardColumns.isEmpty() && groupKey == null) groupKey = GroupKey.PRIORITY
        awaySince = try {
            (storage as? CompanionFiles)?.readCompanion(TimeLog.NAME)?.let(TimeLog::awaySince)
        } catch (_: Exception) {
            awaySince
        }
        planBlocks = try {
            (storage as? CompanionFiles)?.readCompanion(CalendarTxt.NAME)?.let(CalendarTxt::blocks)
        } catch (_: Exception) {
            planBlocks
        }
        // [impl->dsn~family-day~1]
        val lists = storage as? SharedLists
        members = lists?.names.orEmpty()
        memberBlocks = members.associateWith { member ->
            try {
                lists?.readCompanionOf(member, CalendarTxt.NAME)?.let(CalendarTxt::blocks).orEmpty()
            } catch (_: Exception) {
                blocksOf(member)
            }
        }
        memberConfigs = members.mapNotNull { member ->
            try {
                lists?.readCompanionOf(member, ConfigFile.NAME)?.let { member to it }
            } catch (_: Exception) {
                memberConfigs[member]?.let { member to it }
            }
        }.toMap()
    }

    /** Replaces the columns the user defined by those in the text, one per line as `title = filter expression`. */
    fun updateBoardColumns(text: String) {
        val columns = parseBoardColumns(text)
        if (columns == definedColumns) return
        writeConfig(ConfigFile.withBoardColumns(config.orEmpty(), columns), "Edit board columns")
    }

    /** Saves the time maps, lines of the form `name = definition`, without planning. */
    // [impl->dsn~time-map-editor~1]
    fun saveTimeMaps(lines: String) {
        val text = ConfigFile.withEntries(config.orEmpty(), ConfigFile.TIME_MAPS, ConfigFile.parseLines(lines))
        if (text != config.orEmpty()) writeConfig(text, "Edit time maps")
    }

    private fun writeConfig(text: String, message: String) {
        guarded {
            (storage as? CompanionFiles)?.writeCompanion(ConfigFile.NAME, text) ?: return@guarded
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(file.serialize(), message)
            loadedVersion = storage.version()
        }
        loadConfig()
    }

    /** The names under which the priorities are shown. */
    // [impl->dsn~priority-names~1]
    val priorityNames: PriorityNames get() = PriorityNames.of(ConfigFile.entries(config.orEmpty(), ConfigFile.PRIORITIES))

    /** The names of the priorities, one per line as `letter = name`; `null` if the storage keeps no configuration file. */
    val priorityNamesText: String? get() = config?.let { ConfigFile.toLines(ConfigFile.entries(it, ConfigFile.PRIORITIES)) }

    fun updatePriorityNames(text: String) {
        val names = ConfigFile.parseLines(text)
        if (names == ConfigFile.entries(config.orEmpty(), ConfigFile.PRIORITIES)) return
        writeConfig(ConfigFile.withEntries(config.orEmpty(), ConfigFile.PRIORITIES, names), "Edit names of priorities")
    }

    /** The links of projects, contexts and hashtags, each under its name with its sign. */
    // [impl->dsn~label-links~1]
    val labelLinks: Map<String, String> get() = ConfigFile.entries(config.orEmpty(), ConfigFile.LINKS).toMap()

    /** The links, one per line as `label = link`; `null` if the storage keeps no configuration file. */
    val labelLinksText: String? get() = config?.let { ConfigFile.toLines(ConfigFile.entries(it, ConfigFile.LINKS)) }

    fun updateLabelLinks(text: String) {
        val links = ConfigFile.parseLines(text)
        if (links == ConfigFile.entries(config.orEmpty(), ConfigFile.LINKS)) return
        writeConfig(ConfigFile.withEntries(config.orEmpty(), ConfigFile.LINKS, links), "Edit links of projects and contexts")
    }

    /** The task that the dialog of a new task starts with; an empty one if the storage keeps no configuration file, where it could not be changed. */
    // [impl->dsn~new-task-defaults~1]
    fun newTask(): Task = config?.let { ConfigFile.newTask(it, today()) } ?: Task(body = "")

    /** What a new task starts with, one per line as `name = value`; `null` if the storage keeps no configuration file. */
    val newTaskText: String? get() = config?.let { ConfigFile.toLines(ConfigFile.newTaskDefaults(it)) }

    fun updateNewTask(text: String) {
        val defaults = ConfigFile.parseLines(text)
        if (defaults == ConfigFile.newTaskDefaults(config.orEmpty())) return
        writeConfig(ConfigFile.withNewTaskDefaults(config.orEmpty(), defaults), "Edit what a new task starts with")
    }

    /** Records a failure that a dialog shows to the user itself. */
    // [impl->dsn~failure-log~1]
    fun report(message: String, cause: Throwable) = log(message, cause)

    /** The contexts that a task takes with the project. */
    // [impl->dsn~project-contexts~1]
    fun contextsOf(project: String): List<String> =
        ConfigFile.contextsOf(ConfigFile.entries(config.orEmpty(), ConfigFile.PROJECT_CONTEXTS).toMap(), project)

    /** The contexts of the projects, one project per line as `+project = @context`; `null` if the storage keeps no configuration file. */
    val projectContextsText: String? get() = config?.let { ConfigFile.toLines(ConfigFile.entries(it, ConfigFile.PROJECT_CONTEXTS)) }

    fun updateProjectContexts(text: String) {
        val contexts = ConfigFile.parseLines(text)
        if (contexts == ConfigFile.entries(config.orEmpty(), ConfigFile.PROJECT_CONTEXTS)) return
        writeConfig(ConfigFile.withEntries(config.orEmpty(), ConfigFile.PROJECT_CONTEXTS, contexts), "Edit contexts of projects")
    }

    /** What the configuration keeps of the project; `null` if the storage keeps no configuration file. */
    // [impl->dsn~project-properties~1]
    fun projectProperties(project: String): ConfigFile.ProjectProperties? = config?.let { ConfigFile.projectProperties(it, project) }

    // [impl->dsn~project-properties~1]
    fun updateProjectProperties(project: String, properties: ConfigFile.ProjectProperties) {
        val text = ConfigFile.withProjectProperties(config.orEmpty(), project, properties)
        if (text != config.orEmpty()) writeConfig(text, "Edit project +$project")
    }

    /** The state of the plan of an open task and a text that tells its start; `null` if nothing was planned yet. */
    // [impl->dsn~plan-state~1]
    fun planOf(task: Task): Pair<PlanState, String>? {
        val plan = planned?.takeIf { !task.completed } ?: return null
        // ponytail: by the task, as the labels know no line; of two lines that are the same, both show the plan of the first.
        val own = plan[planNames.entries.firstOrNull { it.key.task == task }?.value]
        // A task that is not due for work yet is not late for not being planned.
        if (own == null && task.threshold.let { it != null && it > today() }) return null
        return CalendarTxt.state(task, own) to (own?.let { "Planned for ${CalendarTxt.startText(it)}" } ?: "Not planned")
    }

    /** The configuration for planning as the dialog shows it; `null` if the storage keeps no files next to the todo.txt file. */
    val planSettings: PlanSettings?
        get() = config?.let { text ->
            PlanSettings(
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.TIME_MAPS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.CONTEXT_TIME_MAPS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.BARRIERS)),
                ConfigFile.toLines(ConfigFile.entries(text, ConfigFile.LIMITS)),
                ConfigFile.entries(text, ConfigFile.SIDELINE).joinToString("\n") { it.first },
            )
        }

    /** The calendars of the members as the settings name them; `null` if the platform cannot read calendars or the tasks have no members. */
    val memberCalendars: String? get() = settings.memberCalendars.takeIf { fetch != null && members.isNotEmpty() }

    /** What kept the absences of members from being updated at the last attempt, as texts for the user. */
    var absenceProblems by mutableStateOf(emptyList<String>())
        private set

    /** The member whose plan this device keeps, as the settings name it; `null` if the tasks have no members. */
    val planMember: String? get() = settings.planMember.takeIf { members.isNotEmpty() }

    /** The member whose busy times this device wrote and has to take back, as the settings name another one now. */
    private var formerPlanMember by mutableStateOf<String?>(null)

    /** Whether this device has absences or busy times of members to write, see [updateAbsences]. */
    val writesAbsences: Boolean get() = !memberCalendars.isNullOrBlank() || !planMember.isNullOrBlank() || formerPlanMember != null

    /**
     * Reads the calendars of the members that the settings of this device name and writes the times of their events, for the next four weeks,
     * as absences into the `calendar.txt` of the list of each member, where every device of the family reads them.
     * A member one of whose calendars cannot be read keeps the absences that are there, and the failure is reported.
     * The times of the plan of this device are written the same way for the member whose plan it is, see [Family.busy].
     */
    // [impl->dsn~absences~1]
    // [impl->dsn~busy-plan~1]
    suspend fun updateAbsences() {
        val lists = storage as? SharedLists ?: return
        val problems = mutableListOf<String>()
        val calendars = if (fetch == null) emptyMap() else Family.calendars(settings.memberCalendars).groupBy { it.member }
        val planOf = settings.planMember.trim().ifEmpty { null }
        val until = today().plus(DatePeriod(days = ABSENCE_DAYS))
        val former = formerPlanMember?.takeUnless { it.equals(planOf, ignoreCase = true) }
        formerPlanMember = null
        for (named in (calendars.keys + listOfNotNull(planOf, former)).distinctBy { it.lowercase() }) {
            val member = members.firstOrNull { it.equals(named, ignoreCase = true) }
            if (member == null) {
                // A list that is gone has no busy times to take back.
                if (named == former) continue
                problems += "${if (named in calendars) "Calendar" else "Plan"} of $named: there is no list of that name"
                continue
            }
            try {
                val ofMember = calendars.entries.filter { it.key.equals(named, ignoreCase = true) }.flatMap { it.value }
                val absent = mutableMapOf<LocalDate, List<Span>>()
                val titled = mutableListOf<TextBlock>()
                for (calendar in ofMember) {
                    try {
                        val events = events(fetch!!(calendar.address), today(), until, TimeZone.currentSystemDefault())
                        // [impl->dsn~absence-titles~1]
                        if (calendar.titles) titled += events else events.forEach { absent[it.date] = absent[it.date].orEmpty() + it.span }
                    } catch (e: Exception) {
                        // The address is a secret: only its host is shown, and the message of the exception, which may name it, stays out of the log.
                        val host = calendar.address.substringAfter("://").substringBefore('/')
                        log("Calendar of $member at $host was not read: ${e::class.simpleName}", null)
                        error("Calendar of $member at $host was not read: ${e.message ?: e}")
                    }
                }
                val busy = if (!member.equals(planOf, ignoreCase = true)) emptyList<TextBlock>().takeIf { member.equals(former, ignoreCase = true) } else Family.busy(
                    planBlocks.orEmpty().filter { it.date <= until },
                    planNames.entries.associate { (entry, name) -> name to entry.task },
                    member,
                    members,
                )
                val existing = lists.readCompanionOf(member, CalendarTxt.NAME).orEmpty()
                // Absences stay without a calendar on this device, as another device of the member may know one.
                val new = CalendarTxt.withAbsences(existing, absent.takeIf { ofMember.isNotEmpty() }, today(), busy, titled)
                if (new != existing) lists.writeCompanionOf(member, CalendarTxt.NAME, new, "Update the absences of @$member")
            } catch (e: Exception) {
                problems += e.message ?: e.toString()
            }
        }
        absenceProblems = problems
        loadConfig()
        // The tasks of a member are planned around the absences.
        updateMemberPlans()
    }

    /** The addresses of the calendars with busy times; `null` if the platform cannot read them. */
    val calendars: String? get() = settings.calendars.takeIf { fetch != null }

    /**
     * Saves the addresses of the calendars, reads their busy times for a year from today and plans with them, see [plan].
     * A calendar that cannot be read is reported and left out.
     */
    // [impl->dsn~busy-times~1]
    suspend fun planWithCalendars(settings: PlanSettings, calendars: String?): PlanResult? {
        val busy = mutableMapOf<LocalDate, List<Span>>()
        val problems = mutableListOf<String>()
        // [impl->dsn~sideline~1]
        val sideline = settings.sideline.lines().map(String::trim).filter(String::isNotEmpty)
        val fetch = fetch
        if (calendars != null && fetch != null) updateSettings(this.settings.copy(calendars = calendars.trim()))
        if (fetch != null) for (address in calendars.orEmpty().lines().map(String::trim).filter(String::isNotEmpty)) {
            try {
                busyTimes(fetch(address), today(), today().plus(DatePeriod(years = 1)), TimeZone.currentSystemDefault(), sideline)
                    .forEach { (date, spans) -> busy[date] = busy[date].orEmpty() + spans }
            } catch (e: Exception) {
                // The address is a secret: only its host is shown.
                val host = address.substringAfter("://").substringBefore('/')
                problems += "Calendar of $host was not read: ${e.message ?: e}"
                // The message of the exception may name the address, and a log is passed on.
                log("Calendar of $host was not read: ${e::class.simpleName}", null)
            }
        }
        val bridge = outlook
        // [impl->dsn~outlook-busy~1]
        if (bridge != null) {
            try {
                val appointments = withContext(Dispatchers.Default) { bridge.busy(outlookCalendar, today(), today().plus(DatePeriod(years = 1))) }
                OutlookCalendar.busyTimes(appointments, sideline).forEach { (date, spans) -> busy[date] = busy[date].orEmpty() + spans }
            } catch (e: Exception) {
                problems += "The calendar of Outlook was not read: ${e.message ?: e}"
                log("The calendar of Outlook was not read", e)
            }
        }
        val result = plan(settings, busy, problems) ?: return null
        // [impl->dsn~busy-plan~1]
        if (!planMember.isNullOrBlank()) updateAbsences()
        // [impl->dsn~outlook-blocks~1]
        val weeks = this.settings.outlookWeeks
        if (bridge == null || weeks == 0) return result
        val minute = minuteOfDay()
        val window = OutlookCalendar.window(LocalDateTime(today(), LocalTime(minute / 60, minute % 60)), weeks)
        return try {
            val change = withContext(Dispatchers.Default) { bridge.syncBlocks(outlookCalendar, window, OutlookCalendar.blocks(plannedBlocks, window)) }
            result.copy(summary = "${result.summary} Outlook: $change.")
        } catch (e: Exception) {
            log("The plan was not written to Outlook", e)
            result.copy(problems = result.problems + "The plan was not written to Outlook: ${e.message ?: e}")
        }
    }

    /** The blocks of the last plan of this run of the application, for the calendar of Outlook. */
    private var plannedBlocks: List<Block> = emptyList()

    /** The account whose calendar of Outlook is meant; `null` for the default account. */
    private val outlookCalendar: String? get() = settings.outlookCalendar.trim().ifEmpty { null }

    /** The number of the blocks of koppter in the main calendar of the account, by its name in Outlook; blank for the default account. */
    // [impl->dsn~outlook-clear~1]
    suspend fun countOutlookBlocks(calendar: String): Int =
        withContext(Dispatchers.Default) { outlook?.countBlocks(calendar.trim().ifEmpty { null }) ?: 0 }

    /** Deletes the blocks of koppter, and only those, from the main calendar of the account; returns their number. */
    // [impl->dsn~outlook-clear~1]
    suspend fun clearOutlookBlocks(calendar: String): Int =
        withContext(Dispatchers.Default) { outlook?.clearBlocks(calendar.trim().ifEmpty { null }) ?: 0 }

    /**
     * Saves the configuration for planning, plans the open tasks from now on and writes the plan to `calendar.txt`.
     * The todo.txt file is not changed: the plan is derived from it.
     *
     * @param busy the times that are taken, by day
     */
    // [impl->dsn~plan-dialog~1]
    fun plan(settings: PlanSettings, busy: Map<LocalDate, List<Span>> = emptyMap(), problems: List<String> = emptyList()): PlanResult? {
        var result: PlanResult? = null
        guarded {
            val files = storage as? CompanionFiles ?: return@guarded
            val newConfig = listOf(
                ConfigFile.TIME_MAPS to settings.timeMaps,
                ConfigFile.CONTEXT_TIME_MAPS to settings.contextMaps,
                ConfigFile.BARRIERS to settings.barriers,
                ConfigFile.LIMITS to settings.limits,
            ).fold(config.orEmpty()) { text, (key, lines) -> ConfigFile.withEntries(text, key, ConfigFile.parseLines(lines)) }.let { text ->
                ConfigFile.withEntries(text, ConfigFile.SIDELINE, settings.sideline.lines().map(String::trim).filter(String::isNotEmpty).map { it to "" })
            }
            if (newConfig != config.orEmpty()) files.writeCompanion(ConfigFile.NAME, newConfig)
            val planConfig = ConfigFile.planConfig(newConfig)
            // A block does not start at an odd minute.
            // ponytail: a year ahead, as far as the widest named barrier; longer when a barrier with a date lies behind.
            val upcoming = iceRecurOccurrences(files.readCompanion(ICE_RECUR_FILE).orEmpty(), today().plus(DatePeriod(days = 1))..today().plus(DatePeriod(days = 366)))
            val plan = plan(file.entries, planConfig, busy, today(), (minuteOfDay() + 14) / 15 * 15, upcoming)
            files.writeCompanion(CalendarTxt.NAME, CalendarTxt.write(files.readCompanion(CalendarTxt.NAME).orEmpty(), plan.blocks, today()))
            // [impl->dsn~ics-export~1]
            files.writeCompanion(Ics.NAME, Ics.write(plan.blocks, today()))
            plannedBlocks = plan.blocks
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(file.serialize(), "Update the plan")
            loadedVersion = storage.version()
            // By the text, as each day of a schedule is a task of its own.
            val tasks = plan.blocks.filterNot { it.travel }.map { it.text }.distinct().size
            result = PlanResult(
                if (plan.blocks.isEmpty()) "Nothing was planned." else "Planned $tasks tasks until ${plan.blocks.last().date}.",
                problems + planConfig.problems + plan.problems,
                plan.unplaced.map { entry ->
                    "${entry.task.description}${entry.task.contexts.firstOrNull()?.let { " @$it" }.orEmpty()} (barrier ${planConfig.barrierOf(entry.task, today())})"
                },
            )
        }
        loadConfig()
        return result
    }

    // [impl->dsn~share-actions~1]
    // Without a configuration file, the user could not change the default actions, so there are none.
    val shareActions: List<ShareAction> get() = config?.let(ConfigFile::shareActions).orEmpty()

    /** The share actions as the settings show them; `null` if the storage keeps no configuration file. */
    val shareActionsText: String? get() = config?.let { shareActions.joinToString("\n") { "${it.title} = ${it.template}" } }

    /** Replaces the share actions by those in the text, one per line as `title = template`. */
    fun updateShareActions(text: String) {
        val actions = ShareAction.parse(text)
        if (actions == shareActions) return
        writeConfig(ConfigFile.withShareActions(config.orEmpty(), actions), "Edit share actions")
    }

    /**
     * Adds the text that another application shared as a task.
     * [action] is the title of the share action to apply; without it or if there is none with this title, the first one is applied.
     * Shared [images] become the note of the task; without a text, the task is called after them.
     * Returns what to tell the user.
     */
    // [impl->dsn~note-images~1]
    fun share(text: String, action: String? = null, images: List<ByteArray> = emptyList()): String {
        if (images.isNotEmpty() && storage !is CompanionFiles) return "Not added: this todo.txt file has no notes, which an image needs"
        val chosen = shareActions.firstOrNull { it.title == action } ?: shareActions.firstOrNull()
        val shared = text.ifBlank { if (images.isEmpty()) text else "Image" }
        val plain = chosen?.task(shared, today()) ?: Task(body = shared.trim().replace(Regex("\\s+"), " "))
        val noteName = newNoteName()
        val files = buildMap { images.forEach { put(imageName(noteName) { name -> name in this }, it) } }
        val task = if (images.isEmpty()) plain else plain.withTag(Task.NOTE, noteName)
        error = null
        if (images.isEmpty()) add(task) else add(task, files.keys.joinToString("\n") { "![]($it)\n" }, files)
        return error ?: "Added${chosen?.let { " (${it.title})" }.orEmpty()}: ${task.description}"
    }

    /** Reloads if another program changed the file. */
    // [impl->dsn~reload-on-external-change~1]
    fun reloadIfChanged() {
        val version = try {
            storage.version()
        } catch (_: Exception) {
            return
        }
        if (version != loadedVersion) load() else loadConfig()
        addScheduledTasks()
        archiveCompleted()
    }

    /** The day of the last look for tasks to archive: the history is read for it, which is too slow for every reload. */
    private var archiveChecked: LocalDate? = null

    /**
     * Moves the tasks that were completed before the last month to `done.txt`.
     * It waits for a running synchronization, as the scheduled tasks do, so that the devices archive the same file.
     */
    // [impl->dsn~archive~1]
    private fun archiveCompleted() {
        val today = today()
        val files = storage as? CompanionFiles ?: return
        if (archiveChecked == today || syncStatus?.state == SyncState.RUNNING) return
        guarded {
            val dates = if (file.entries.any { it.task.completed && it.task.completionDate == null }) (storage as? SyncingStorage)?.dates().orEmpty() else emptyMap()
            // The task of a list stays in its file: the ranking of its member counts it, and `done.txt` is that of the main file.
            val archived = archive(file, dates, today) { Family.memberOf(it, members) != null }
            if (archived != null) {
                val done = files.readCompanion(DONE_FILE).orEmpty().lines().filter { it.isNotBlank() }
                // Before the todo.txt file, so that a failure in between leaves a task in both files instead of in none;
                // the lines that are there already are those of such a failure.
                files.writeCompanion(DONE_FILE, (done + archived.done.filterNot { it in done }).joinToString("\n", postfix = "\n"))
                storage.write(archived.file.serialize(), "Archive completed tasks")
                file = archived.file
                loadedVersion = storage.version()
            }
            archiveChecked = today
        }
    }

    /**
     * Adds the tasks that `ice_recur.txt` schedules for today and for the days since the last run, once per day and device.
     * It waits for a running synchronization, so that tasks another device added in the meantime are known.
     */
    // [impl->dsn~ice-recur-run~1]
    private fun addScheduledTasks() {
        val today = today()
        if (settings.iceRecurLastRun == today.toString() || syncStatus?.state == SyncState.RUNNING) return
        guarded {
            val schedule = (storage as? CompanionFiles)?.readCompanion(ICE_RECUR_FILE) ?: return@guarded
            // The days since the last run, so that the tasks of a day on which the application was not open are not lost.
            val from = Task.parseDate(settings.iceRecurLastRun)?.plus(DatePeriod(days = 1))?.takeIf { it <= today } ?: today
            // A task that was added on these days, by any device, counts as present even if it is completed already.
            val addedSince = (storage as? SyncingStorage)?.dates().orEmpty().filterValues { it.added.let { added -> added != null && added >= from } }.keys
            val existing = file.lines.filter { it.isNotBlank() }.map { line -> Task.parse(line).let { if (line in addedSince) it.reopen() else it } }
            val tasks = iceRecurTasks(schedule, from..today, existing)
            if (tasks.isNotEmpty()) {
                write(tasks.fold(file) { current, (day, task) -> current.add(task.created(day, settings.writeDates)) }, "Add recurring tasks")
            }
            updateSettings(settings.copy(iceRecurLastRun = today.toString()))
        }
    }

    /** Whether the storage keeps files next to the todo.txt file, of which the schedule is one. */
    val canSchedule: Boolean get() = config != null

    /** The schedule of `ice_recur.txt`; empty if the file does not exist, `null` if it cannot be read. */
    fun schedule(): String? = try {
        (storage as? CompanionFiles)?.let { it.readCompanion(ICE_RECUR_FILE).orEmpty().replace("\r\n", "\n") }
    } catch (e: Exception) {
        failed(e)
        null
    }

    /** What the editor offers in place of an empty schedule: an entry to start with, as long as the file does not exist. */
    // [impl->dsn~ice-recur-suggestion~1]
    fun scheduleSuggestion(): String? = try {
        SUGGESTED_ICE_RECUR.takeIf { (storage as? CompanionFiles)?.readCompanion(ICE_RECUR_FILE) == null }
    } catch (_: Exception) {
        null
    }

    /** Writes the schedule and adds the tasks that its new and changed entries have for today. */
    // [impl->dsn~ice-recur-editor~1]
    fun updateSchedule(text: String) {
        val old = schedule() ?: return
        val new = text.trimEnd().let { if (it.isEmpty()) "" else "$it\n" }
        if (new == old) return
        guarded {
            (storage as? CompanionFiles)?.writeCompanion(ICE_RECUR_FILE, new) ?: return@guarded
            // The daily run does not come back to today, so an entry that was not there takes effect now.
            val today = today()
            val known = old.lines().toSet()
            val tasks = iceRecurTasks(new.lines().filterNot { it in known }.joinToString("\n"), today..today, file.entries.map { it.task })
            val added = tasks.fold(file) { current, (day, task) -> current.add(task.created(day, settings.writeDates)) }
            // The storage records the files next to the todo.txt file with a change of that file.
            storage.write(added.serialize(), "Edit scheduled tasks")
            file = added
            loadedVersion = storage.version()
        }
    }

    /** The text of the note of the task; empty if it has none, `null` if the storage keeps no notes. */
    // [impl->dsn~notes~1]
    fun noteOf(task: Task): String? = try {
        (storage as? CompanionFiles)?.let { files -> task.tag(Task.NOTE)?.let { files.readCompanion("$NOTES_DIRECTORY/$it") }.orEmpty() }
    } catch (_: Exception) {
        null
    }

    /** The image of a note, by its file name in the directory of the notes; `null` if it is not there or cannot be read. */
    // [impl->dsn~note-images~1]
    fun imageOf(name: String): ByteArray? = try {
        (storage as? CompanionFiles)?.readCompanionBytes("$NOTES_DIRECTORY/$name")
    } catch (_: Exception) {
        null
    }

    /** A file name for the note of a task that has none yet; Markdown, so that programs that show the file format it. */
    fun newNoteName(): String = Random.nextInt(36 * 36 * 36).toString(36).padStart(3, '0') + ".md"

    /**
     * Writes the note into the file that the `note:` tag of the task names; returns whether the text changed.
     * The [images] that the note shows are written next to it, each under its file name.
     */
    // [impl->dsn~note-images~1]
    private fun saveNote(task: Task, note: String?, images: Map<String, ByteArray>): Boolean {
        val name = task.tag(Task.NOTE) ?: return false
        if (note == null || note == noteOf(task)) return false
        val files = storage as? CompanionFiles ?: return false
        // Before the note, so that the note never shows an image that is not there.
        noteImages(note).forEach { image -> images[image]?.let { files.writeCompanionBytes("$NOTES_DIRECTORY/$image", it) } }
        files.writeCompanion("$NOTES_DIRECTORY/$name", note)
        return true
    }

    // [impl->dsn~task-operations~1]
    /** Adds the task; [changed] are other tasks whose dependencies on it changed. */
    fun add(task: Task, note: String? = null, images: Map<String, ByteArray> = emptyMap(), changed: List<Entry> = emptyList()) {
        if (task.body.isBlank()) return
        guarded { saveNote(task, note, images) }
        write(withChanged(file, changed).add(task.created(today(), settings.writeDates)), "Add task: ${task.description}")
    }

    fun toggle(entry: Entry) {
        // [impl->dsn~latex-todos~1]
        // The document is not changed: the completed line hides the TODO until its text changes.
        if (Outlook.isItem(entry) && entry.task.tag(Task.LATEX) != null) {
            write(file.add(entry.task.complete(today(), settings.writeDates)), "Complete task: ${entry.task.description}")
            return
        }
        // [impl->dsn~outlook-complete~1]
        if (Outlook.isItem(entry)) {
            entry.task.tag(Task.OUTLOOK)?.let(Outlook::entryId)?.let { entryId ->
                guarded { outlook?.complete(entryId) }
                outlookItems = outlookItems.filterNot { it.entryId.equals(entryId, ignoreCase = true) }
            }
            return
        }
        val index = locate(entry) ?: return
        if (entry.task.completed) {
            write(file.replace(index, entry.task.reopen()), "Reopen task: ${entry.task.description}")
        } else {
            // [impl->dsn~time-tracking~1]
            // A task that is completed while its clock runs gets the time of the clock.
            val task = if (entry.task.started != null) stopped(entry.task) else entry.task
            // [impl->dsn~stars~1]
            // The ranking needs the day on which a member completed a task.
            var new = file.replace(index, task.complete(today(), settings.writeDates || Family.memberOf(task, members) != null))
            // [impl->dsn~recurrence-on-completion~1]
            entry.task.nextOccurrence(today())?.let { new = new.add(it.created(today(), settings.writeDates)) }
            write(new, "Complete task: ${entry.task.description}")
            // [impl->dsn~outlook-complete~1]
            entry.task.tag(Task.OUTLOOK)?.let(Outlook::entryId)?.let { entryId -> guarded { outlook?.complete(entryId) } }
        }
    }

    /**
     * Turns each bare address of the web in the task with [body] into a Markdown link with the title of its page, once the page is read.
     * An address whose page cannot be read or has no title stays as it is; so does a task that was changed meanwhile.
     */
    // [impl->dsn~link-titles~1]
    suspend fun addLinkTitles(body: String) {
        val fetch = fetch ?: return
        var current = body
        for (address in bareAddresses(body).map(body::substring).distinct()) {
            val title = try {
                pageTitle(fetch(address))
            } catch (e: Exception) {
                log("No title for a link: ${e::class.simpleName}", null)
                null
            } ?: continue
            val entry = file.entries.firstOrNull { it.task.body == current } ?: return
            current = withLinkTitle(current, address, title)
            replace(entry, entry.task.copy(body = current))
        }
    }

    fun replace(entry: Entry, task: Task, note: String? = null, images: Map<String, ByteArray> = emptyMap(), changed: List<Entry> = emptyList()) {
        // [impl->dsn~outlook-items~1]
        if (Outlook.isItem(entry)) {
            if (task != entry.task) add(task, note, images)
            return
        }
        val index = locate(entry) ?: return
        var noteChanged = false
        guarded { noteChanged = saveNote(task, note, images) }
        val new = withChanged(file.replace(index, task), changed)
        // A changed note alone is a change to record, although the todo.txt file stays the same.
        if (new == file && noteChanged) guarded { storage.write(file.serialize(), "Edit note: ${task.description}") }
        write(new, "Edit task: ${task.description}")
    }

    /**
     * The task as it is after a move from one column of the board to another; `null` if that move is not possible.
     * For a column the user defined, the task is changed as little as possible so that it matches the filter of the column.
     */
    // [impl->dsn~board-move~1]
    // [impl->dsn~board-move-to-filter~1]
    fun moved(entry: Entry, from: Group, to: Group): Task? {
        val key = groupKey
        val task = when {
            key != null -> moveToGroup(entry.task, key, from, to, today())
            from.title == to.title -> null
            else -> boardColumns.firstOrNull { it.title == to.title }?.let { Query.parse(it.expression).applyTo(entry.task, today()) }
        }
        return task?.takeIf { it != entry.task }
    }

    /**
     * Applies the move of a task from one column of the board to another, or within its column.
     * With [at], the task is also put before or behind that task of the column.
     * Within its column, it takes the priority of that task, as [drop] does it.
     */
    // [impl->dsn~board-drag-position~1]
    fun move(entry: Entry, from: Group, to: Group, at: Entry? = null, before: Boolean = false) {
        val task = moved(entry, from, to)
        // [impl->dsn~outlook-items~1]
        if (Outlook.isItem(entry)) {
            task?.let(::add)
            return
        }
        if (task == null && (from.title != to.title || at == null)) return
        val index = locate(entry) ?: return
        var new = task?.let { file.replace(index, it) } ?: file
        // A move to another column changes what the column stands for and nothing else: the task gets its place only among tasks of its priority.
        if (at != null && (task == null || task.priority == at.task.priority)) locate(at)?.let { other -> new.moveTo(index, other, before, today())?.let { new = it } }
        write(new, if (task == null) "Move task: ${entry.task.description}" else "Move task to ${to.title}: ${task.description}")
    }

    /**
     * Renames a project, a context or a hashtag, told by its sign, with those below it: in all tasks, in the scheduled tasks, which would bring
     * the old name back, and in the filter.
     */
    // [impl->dsn~label-rename~1]
    // ponytail: the limits and board columns of koppter.yml keep the old name; rename them there when that is missed.
    fun rename(sign: Char, from: String, to: String) {
        guarded {
            val files = storage as? CompanionFiles ?: return@guarded
            config?.let { before -> ConfigFile.renamed(before, sign, from, to).takeIf { it != before }?.let { files.writeCompanion(ConfigFile.NAME, it) } }
            val schedule = files.readCompanion(ICE_RECUR_FILE) ?: return@guarded
            renameLabel(schedule, sign, from, to).takeIf { it != schedule }?.let { files.writeCompanion(ICE_RECUR_FILE, it) }
        }
        write(file.rename(sign, from, to), "Rename $sign$from to $sign$to")
        loadConfig()
        fun Set<String>.renamed() = map { if (it.isWithin(from)) to + it.substring(from.length) else it }.toSet()
        filter = when (sign) {
            '+' -> filter.copy(projects = filter.projects.renamed(), excludedProjects = filter.excludedProjects.renamed())
            '@' -> filter.copy(contexts = filter.contexts.renamed(), excludedContexts = filter.excludedContexts.renamed())
            else -> filter.copy(hashtags = filter.hashtags.renamed(), excludedHashtags = filter.excludedHashtags.renamed())
        }
    }

    /** Moves the due date of the task to the given day; without a day, the task has no due date anymore. */
    // [impl->dsn~due-menu~1]
    fun setDue(entry: Entry, date: LocalDate?) = replace(entry, entry.task.withTag(Task.DUE, date?.toString()))

    /** Puts the task before or behind another one of the list: see [TodoFile.moveTo]. */
    // [impl->dsn~list-drag~1]
    fun drop(entry: Entry, target: Entry, before: Boolean) {
        val index = locate(entry) ?: return
        val other = locate(target) ?: return
        write(file.moveTo(index, other, before, today()) ?: return, "Move task: ${entry.task.description}")
    }

    /** Lets the task step over its neighbor in the list: see [TodoFile.step]. */
    // [impl->dsn~list-step~1]
    fun step(entry: Entry, neighbor: Entry, up: Boolean) {
        val index = locate(entry) ?: return
        val other = locate(neighbor) ?: return
        write(file.step(index, other, up, today()) ?: return, "Move task ${if (up) "up" else "down"}: ${entry.task.description}")
    }

    fun delete(entry: Entry) {
        val index = locate(entry) ?: return
        write(file.remove(index), "Delete task: ${entry.task.description}")
    }

    /** Adds the minutes of a block of the plan to the time spent on its task. */
    // [impl->dsn~time-spent~1]
    /** The current minute, as the clocks of tasks are written. */
    fun now(): LocalDateTime = LocalDateTime(today(), LocalTime(minuteOfDay() / 60, minuteOfDay() % 60))

    /** The open task whose clock runs; one at a time. */
    // [impl->dsn~time-tracking~1]
    val running: Entry? get() = file.entries.firstOrNull { !it.task.completed && it.task.started != null }

    /** Changes `timelog.txt`; without files next to todo.txt, there is no log. */
    private fun changeLog(change: (String) -> String) = guarded {
        val files = storage as? CompanionFiles ?: return@guarded
        val log = change(files.readCompanion(TimeLog.NAME).orEmpty())
        files.writeCompanion(TimeLog.NAME, log)
        awaySince = TimeLog.awaySince(log)
    }

    /** The task with its clock stopped, after the time of the clock was added to the log. */
    private fun stopped(task: Task): Task {
        task.started?.let { started -> changeLog { TimeLog.append(it, started, now(), CalendarTxt.text(task)) } }
        return task.stop(now())
    }

    /** Starts the clock of the task, stops the one that ran, which gets its time, and ends the time away. */
    fun start(entry: Entry) {
        val index = locate(entry) ?: return
        var new = file
        running?.takeIf { it.index != index }?.let { new = new.replace(it.index, stopped(it.task)) }
        if (awaySince != null) changeLog { TimeLog.back(it, now()) }
        write(new.replace(index, entry.task.start(now())), "Start working on: ${entry.task.description}")
    }

    /** Stops the clock of the task and adds its time to the time spent and to the log. */
    fun stop(entry: Entry) {
        val index = locate(entry) ?: return
        write(file.replace(index, stopped(entry.task)), "Stop working on: ${entry.task.description}")
    }

    /** Stops the clock that runs and starts the time away. */
    // [impl->dsn~time-log~1]
    fun goAway() {
        running?.let(::stop)
        changeLog { TimeLog.goAway(it, now()) }
    }

    /** Ends the time away. */
    fun back() = changeLog { TimeLog.back(it, now()) }

    fun worked(entry: Entry, minutes: Int) {
        val index = locate(entry) ?: return
        write(file.replace(index, entry.task.withSpent(minutes.minutes)), "Note the time spent: ${entry.task.description}")
    }

    fun updateSettings(new: Settings) {
        if (new == settings) return
        // [impl->dsn~shared-lists-setting~1]
        val fileChanged = new.todoFile != settings.todoFile || new.sharedLists != settings.sharedLists
        // [impl->dsn~committer~1]
        val committerChanged = new.committerName != settings.committerName || new.committerEmail != settings.committerEmail
        // [impl->dsn~busy-plan~1]
        if (new.planMember != settings.planMember && settings.planMember.isNotBlank()) formerPlanMember = settings.planMember
        settings = new
        guarded { saveSettings(new) }
        if (fileChanged || committerChanged) {
            // The filter names projects and contexts of the file that was open.
            if (fileChanged) filter = Filter()
            storage = storageFor(new)
            open()
        }
    }

    /**
     * The current line index of the task the user acted on.
     * The file may have been reloaded since the entry was shown, so the index is checked against the content.
     */
    private fun locate(entry: Entry): Int? {
        // An item of Outlook has no line to change: the operations that write one handle it before.
        if (Outlook.isItem(entry)) return null
        val current = file.entries
        val found = current.firstOrNull { it == entry } ?: current.firstOrNull { it.task == entry.task }
        if (found == null) error = "The task was changed by another program. Nothing was written."
        return found?.index
    }

    // [impl->dsn~edit-dependencies~1]
    private fun withChanged(start: TodoFile, changed: List<Entry>): TodoFile =
        changed.fold(start) { new, entry -> new.replace(entry.index, entry.task) }

    private fun write(new: TodoFile, change: String) {
        if (new == file) return
        guarded {
            storage.write(new.serialize(), change)
            file = new
            loadedVersion = storage.version()
        }
    }

    private companion object {
    }

    private fun guarded(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            failed(e)
        }
    }

    /** Shows the user what failed and passes it to the log. */
    // [impl->dsn~failure-log~1]
    private fun failed(e: Exception) {
        val message = e.message ?: e.toString()
        error = message
        log(message, e)
    }
}
