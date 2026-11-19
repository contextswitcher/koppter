package io.github.koppor.koppter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import io.github.koppor.koppter.storage.CompanionFiles
import io.github.koppor.koppter.outlook.Outlook
import io.github.koppor.koppter.outlook.OutlookItem
import io.github.koppor.koppter.outlook.OutlookBridge
import io.github.koppor.koppter.outlook.OutlookCalendar
import io.github.koppor.koppter.plan.Block
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.plan.Ics
import io.github.koppor.koppter.plan.PlanState
import io.github.koppor.koppter.plan.Planned
import io.github.koppor.koppter.plan.Span
import io.github.koppor.koppter.plan.TextBlock
import io.github.koppor.koppter.plan.busyTimes
import io.github.koppor.koppter.plan.plan
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.storage.NOTES_DIRECTORY
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.storage.SyncingStorage
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.TaskDates
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.Group
import io.github.koppor.koppter.todotxt.BoardColumn
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.todotxt.OVERVIEW_KEYS
import io.github.koppor.koppter.todotxt.listParts
import io.github.koppor.koppter.todotxt.isWithin
import io.github.koppor.koppter.todotxt.renameLabel
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.todotxt.ICE_RECUR_FILE
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
import io.github.koppor.koppter.todotxt.listOrder
import io.github.koppor.koppter.todotxt.nextActions
import io.github.koppor.koppter.todotxt.toLink
import io.github.koppor.koppter.todotxt.moveToGroup
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
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
enum class View(val title: String) { OVERVIEW("Overview"), AGENDA("Agenda"), WEEK("Week"), BOARD("Board"), LIST("List"), INBOX("Inbox"), TODAY("Today") }

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
) {
    var settings by mutableStateOf(settings)
        private set
    var file by mutableStateOf(TodoFile())
        private set
    private var shownFilter by mutableStateOf(filterOfLink(settings.filter) ?: Filter())

    /** The filter of the task list, which the settings keep so that it is there again after a restart. */
    // ponytail: the settings are saved with every change of the filter, also with every typed character of its text; delay the saving if that is felt.
    // [impl->dsn~filter-remembered~1]
    var filter: Filter
        get() = shownFilter
        set(value) {
            shownFilter = value
            updateSettings(settings.copy(filter = if (value.isDefault) "" else value.toLink()))
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

    /**
     * Shows the tasks that a link to the application names, in the list; a text that is no such link changes nothing.
     * The link replaces the filter, so that it shows the same tasks whatever was filtered before.
     */
    // [impl->dsn~app-links~1]
    fun openLink(link: String) {
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
        get() = View.entries.firstOrNull { it.name == settings.view } ?: View.LIST
        set(value) = updateSettings(settings.copy(view = value.name))

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
    var planBlocks by mutableStateOf<List<TextBlock>?>(null)
        private set

    /** When the tasks are planned from today on, by the text of their blocks. */
    private val planned: Map<String, Planned>? get() = planBlocks?.let { CalendarTxt.planned(it, today()) }

    /** What the plan calls the open tasks: tasks with the same text are told apart there. */
    // [impl->dsn~plan-names~1]
    private val planNames: Map<Entry, String> by derivedStateOf { CalendarTxt.names(file.entries) }

    /** The open task that a block of the plan is for; `null` if the task was completed, deleted or edited since. */
    fun entryOf(block: TextBlock): Entry? = planNames.entries.firstOrNull { it.value == block.text }?.key

    /** What the plan adds to the text of the task to tell it from another task with the same text; empty for most tasks. */
    fun planSuffix(entry: Entry): String = planNames[entry]?.removePrefix(CalendarTxt.text(entry.task)).orEmpty()

    /** The open tasks that are due today or earlier and have no block today. */
    // [impl->dsn~agenda~1]
    val dueUnplanned: List<Entry>
        get() {
            val today = today()
            val plannedToday = planBlocks.orEmpty().filter { it.date == today }.map { it.text }.toSet()
            return file.entries.filter { entry ->
                !entry.task.completed && entry.task.due.let { it != null && it <= today } && planNames[entry] !in plannedToday
            }.sortedWith(listOrder(today))
        }

    /** The visible open tasks without context, which wait for one. */
    // [impl->dsn~inbox~1]
    val inbox: List<Entry> get() = visible.filter { !it.task.completed && it.task.contexts.isEmpty() }

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
        syncing?.onStatus = { status ->
            // [impl->dsn~failure-log~1]
            if (status.state == SyncState.FAILED) log("Synchronization failed: ${status.message}", status.cause)
            // A storage that was replaced in the settings may still report.
            if (storage === syncing) syncStatus = status
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

    val visible: List<Entry>
        get() = (file.entries + Outlook.entries(outlookItems, file.entries)).filter { searched.matches(it.task, today()) }.sortedWith(listOrder(today()))
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

    private fun groupsBy(key: GroupKey): List<Group> {
        val groups = groupEntries(visible, key, today())
        return if (key != GroupKey.PRIORITY) groups
        else groups.map { group -> group.value?.singleOrNull()?.let { group.copy(title = priorityNames.name(it)) } ?: group }
    }

    /** The projects of the file and the projects above them, a project directly before those below it. */
    val projects: List<String> get() = file.entries.flatMap { it.task.projects }.flatMap { it.withAncestors() }.distinct().sortedWith(hierarchyOrder)
    /** The contexts of the file and the contexts above them, a context directly before those below it. */
    val contexts: List<String> get() = file.entries.flatMap { it.task.contexts }.flatMap { it.withAncestors() }.distinct().sortedWith(hierarchyOrder)
    val hashtags: List<String> get() = file.entries.flatMap { it.task.hashtags }.distinct().sorted()
    val priorities: List<Char> get() = file.entries.mapNotNull { it.task.priority }.distinct().sorted()

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
        planBlocks = try {
            (storage as? CompanionFiles)?.readCompanion(CalendarTxt.NAME)?.let(CalendarTxt::blocks)
        } catch (_: Exception) {
            planBlocks
        }
    }

    /** Replaces the columns the user defined by those in the text, one per line as `title = filter expression`. */
    fun updateBoardColumns(text: String) {
        val columns = parseBoardColumns(text)
        if (columns == definedColumns) return
        writeConfig(ConfigFile.withBoardColumns(config.orEmpty(), columns), "Edit board columns")
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
     * A shared [image] becomes the note of the task; without a text, the task is called after it.
     * Returns what to tell the user.
     */
    // [impl->dsn~note-images~1]
    fun share(text: String, action: String? = null, image: ByteArray? = null): String {
        if (image != null && storage !is CompanionFiles) return "Not added: this todo.txt file has no notes, which an image needs"
        val chosen = shareActions.firstOrNull { it.title == action } ?: shareActions.firstOrNull()
        val shared = text.ifBlank { if (image == null) text else "Image" }
        val plain = chosen?.task(shared, today()) ?: Task(body = shared.trim().replace(Regex("\\s+"), " "))
        val noteName = newNoteName()
        val imageFile = imageName(noteName) { false }
        val task = if (image == null) plain else plain.withTag(Task.NOTE, noteName)
        error = null
        if (image == null) add(task) else add(task, "![]($imageFile)\n", mapOf(imageFile to image))
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
    fun add(task: Task, note: String? = null, images: Map<String, ByteArray> = emptyMap()) {
        if (task.body.isBlank()) return
        guarded { saveNote(task, note, images) }
        write(file.add(task.created(today(), settings.writeDates)), "Add task: ${task.description}")
    }

    fun toggle(entry: Entry) {
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
            var new = file.replace(index, entry.task.complete(today(), settings.writeDates))
            // [impl->dsn~recurrence-on-completion~1]
            entry.task.nextOccurrence(today())?.let { new = new.add(it.created(today(), settings.writeDates)) }
            write(new, "Complete task: ${entry.task.description}")
            // [impl->dsn~outlook-complete~1]
            entry.task.tag(Task.OUTLOOK)?.let(Outlook::entryId)?.let { entryId -> guarded { outlook?.complete(entryId) } }
        }
    }

    fun replace(entry: Entry, task: Task, note: String? = null, images: Map<String, ByteArray> = emptyMap()) {
        // [impl->dsn~outlook-items~1]
        if (Outlook.isItem(entry)) {
            if (task != entry.task) add(task, note, images)
            return
        }
        val index = locate(entry) ?: return
        var noteChanged = false
        guarded { noteChanged = saveNote(task, note, images) }
        val new = file.replace(index, task)
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
