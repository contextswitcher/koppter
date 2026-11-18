package io.github.koppor.koppter.ui

import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.SyncState
import io.github.koppor.koppter.storage.SyncStatus
import io.github.koppor.koppter.todotxt.Entry
import io.github.koppor.koppter.todotxt.Completion
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.GroupKey
import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.OVERVIEW_KEYS
import io.github.koppor.koppter.todotxt.overviewStates
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

/** Width from which the filter panel stands beside the list instead of sliding over it. */
private val WIDE = 840.dp

private const val RELOAD_INTERVAL_MILLIS = 2_000L

/**
 * @param chooseFile lets the user pick the todo.txt file with the file dialog of the platform and returns its location,
 *   or `null` if the user canceled.
 * @param git offered in the settings on platforms where the application has to bring its own git access
 * @param keyboard whether the device has a keyboard, see [LocalKeyboard]
 * @param backHandler takes what the back gesture of the platform does; a platform without such a gesture takes nothing
 * @param leave leaves the application, after a second back gesture that has nothing left to close
 * @param chooseFolder lets the user pick a folder and returns the location that stands for its todo.txt file, which need not exist yet
 * @param chooseFileNote what the user has to know about a file chosen with [chooseFile], shown below its button
 * @param shareLog passes the log to another application, offered in the settings on platforms where the user does not reach the log file
 */
@Composable
fun App(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)? = null,
    chooseFolder: (suspend () -> String?)? = null,
    git: GitSetupActions? = null,
    keyboard: Boolean = true,
    shareLog: (() -> Unit)? = null,
    /** The version and commit of the build, shown at the bottom of the settings; `null` where the platform does not tell it. */
    version: String? = null,
    chooseFileNote: String? = null,
    /** Lets the user choose an image for a note and gives its bytes, scaled down; `null` on a platform without that. */
    pickImage: (suspend () -> ByteArray?)? = null,
    /** As [pickImage], with the camera of the device. */
    takePhoto: (suspend () -> ByteArray?)? = null,
    /** Ends the application so that a newer version of it starts; `null` unless there is one and something starts it. */
    update: (() -> Unit)? = null,
    backHandler: @Composable (onBack: () -> Unit) -> Unit = {},
    leave: () -> Unit = {},
) {
    val dark = when (controller.settings.appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    // A link without a program that handles it, such as one of ContextSwitcher on a device without it, is reported.
    val platform = LocalUriHandler.current
    val uriHandler = remember(platform) {
        object : UriHandler {
            override fun openUri(uri: String) = try {
                // [impl->dsn~outlook-tag~1]
                if (!controller.openOutlook(uri)) platform.openUri(uri) else Unit
            } catch (e: Exception) {
                controller.error = "Not opened: $uri"
            }
        }
    }
    // [impl->dsn~outlook-items~1]
    if (controller.readsOutlook) {
        LaunchedEffect(controller) {
            while (true) {
                controller.refreshOutlook()
                delay(OUTLOOK_REFRESH)
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme(controller.settings.theme, dark), typography = compactTypography) {
        CompositionLocalProvider(
            LocalUriHandler provides uriHandler,
            LocalShowContext provides { controller.filter = controller.filter.copy(contexts = setOf(it)) },
            LocalShowProject provides { controller.filter = controller.filter.copy(projects = setOf(it)) },
            LocalFineColor provides fineColor(controller.settings.theme, dark),
            LocalPlanOf provides controller::planOf,
            LocalKeyboard provides keyboard,
            LocalPriorityNames provides controller.priorityNames,
            LocalLabelLinks provides controller.labelLinks,
            LocalSetDue provides controller::setDue,
        ) {
            MainScreen(controller, defaultTodoFile, chooseFile, chooseFolder, chooseFileNote, git, shareLog, version, pickImage, takePhoto, update, backHandler, leave)
        }
    }
}

internal const val LEAVE_WARNING = "Back again leaves koppter."

/** How often the To-Do list of Outlook is read again: each reading starts a process that asks Outlook. */
private val OUTLOOK_REFRESH = 2.minutes

/** What the list is without groups. */
internal const val NO_GROUPS = "no groups: by priority, then due date"

/** The task whose edit dialog is open; `entry` is null for a task that is not in the file yet. */
/**
 * Whether the device has a keyboard. With one, the field that is typed into next gets the focus when it appears;
 * without one, that would only cover the screen with the keyboard of the touch screen.
 */
val LocalKeyboard = staticCompositionLocalOf { true }

private data class Editing(val entry: Entry?)

/** The longer side of an image of a note in pixels: enough to read a photographed page, and a file of some hundred kilobytes for the repository. */
const val NOTE_IMAGE_SIZE = 1600

/** Ctrl with the n-th of these keys shows the n-th view. */
private val VIEW_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six)

// [impl->dsn~main-screen~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    chooseFolder: (suspend () -> String?)?,
    chooseFileNote: String?,
    git: GitSetupActions?,
    shareLog: (() -> Unit)?,
    version: String?,
    pickImage: (suspend () -> ByteArray?)?,
    takePhoto: (suspend () -> ByteArray?)?,
    update: (() -> Unit)?,
    backHandler: @Composable (onBack: () -> Unit) -> Unit,
    leave: () -> Unit,
) {
    var editing by remember { mutableStateOf<Editing?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var schedule by remember { mutableStateOf<String?>(null) }
    var planning by remember { mutableStateOf<PlanSettings?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val rootFocus = remember { FocusRequester() }
    val filterFocus = remember { FocusRequester() }
    // Keys reach the shortcuts only through a focused element, so the screen takes the focus whenever none of its elements has it:
    // at the start, after the element with the focus went away with its view, after a dialog closed, and after a touch took the focus away.
    var focused by remember { mutableStateOf(false) }
    // Set by the layout: below its width, the field for the text of the filter lies in the drawer, which has to open first.
    var wide by remember { mutableStateOf(true) }
    fun focusSearch() {
        if (wide) filterFocus.requestFocus() else scope.launch {
            drawer.open()
            filterFocus.requestFocus()
        }
    }
    val dialog = editing != null || showSettings || schedule != null || planning != null
    LaunchedEffect(focused, dialog) { if (!focused && !dialog) rootFocus.requestFocus() }

    // [impl->dsn~keyboard-shortcuts~1]
    fun shortcut(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || !(event.isCtrlPressed || event.isMetaPressed) || event.isAltPressed) return false
        val view = VIEW_KEYS.indexOf(event.key)
        fun search(all: Boolean) {
            if (all) {
                // Every task: also the completed ones and those before their threshold date, and not only the top ones.
                controller.filter = Filter(completion = Completion.ALL, hideFuture = false)
                controller.topOnly = false
                controller.nextOnly = false
                controller.view = View.LIST
            }
            // The agenda and the week show the plan, which the filter does not apply to.
            if (controller.view == View.AGENDA || controller.view == View.WEEK) controller.view = View.LIST
            focusSearch()
        }
        when {
            !event.isShiftPressed && event.key == Key.N -> editing = Editing(null)
            // The key of OneNote for its search.
            !event.isShiftPressed && event.key == Key.E -> search(all = true)
            // The key of browsers and editors for their search; with Shift, it is the key of SkedPal.
            event.key == Key.F -> search(all = false)
            !event.isShiftPressed && view >= 0 -> controller.view = View.entries[view]
            !event.isShiftPressed -> return false
            // The keys of SkedPal for a new task and for its outline, today list, calendar and plan.
            event.key == Key.A -> editing = Editing(null)
            event.key == Key.O -> controller.view = View.LIST
            event.key == Key.Y -> controller.view = View.AGENDA
            event.key == Key.C -> controller.view = View.WEEK
            event.key == Key.L && controller.canSchedule -> planning = controller.planSettings
            else -> return false
        }
        return true
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(RELOAD_INTERVAL_MILLIS)
            controller.reloadIfChanged()
        }
    }
    LaunchedEffect(controller.error) {
        controller.error?.let {
            snackbar.showSnackbar(it)
            controller.error = null
        }
    }

    // [impl->dsn~filter-panel~1]
    val searchField: @Composable (Modifier) -> Unit = { modifier ->
        SearchField(
            controller.filter.text,
            { text ->
                controller.filter = controller.filter.copy(text = text)
                // The agenda and the week show the plan, which the filter does not apply to.
                if (text.isNotEmpty() && (controller.view == View.AGENDA || controller.view == View.WEEK)) controller.view = View.LIST
            },
            "Search text", "filter-text", modifier.focusRequester(filterFocus),
        )
    }
    val filterPanel: @Composable (Modifier, (() -> Unit)?) -> Unit = { modifier, onClose ->
        FilterPanel(
            filter = controller.filter,
            onChange = { controller.filter = it },
            onRename = controller::rename,
            priorities = controller.priorities,
            projects = controller.projects,
            contexts = controller.contexts,
            hashtags = controller.hashtags,
            topCount = controller.settings.topCount,
            topOnly = controller.topOnly,
            onTopOnly = { controller.topOnly = it },
            nextOnly = controller.nextOnly,
            onNextOnly = { controller.nextOnly = it },
            modifier = modifier,
            onClose = onClose,
            search = if (onClose != null) ({ searchField(Modifier.padding(bottom = 8.dp)) }) else null,
        )
    }

    // [impl->dsn~back~1]
    // Back first closes the filter panel and then returns to the list; from the list, it warns, and leaves the application while the warning is shown.
    var leaving by remember { mutableStateOf(false) }
    backHandler {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            controller.view != View.LIST -> controller.view = View.LIST
            leaving -> {
                leaving = false
                leave()
            }
            else -> leaving = true
        }
    }
    LaunchedEffect(leaving) {
        if (leaving) {
            snackbar.showSnackbar(LEAVE_WARNING)
            leaving = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().focusRequester(rootFocus).onFocusChanged { focused = it.hasFocus }.focusable().onPreviewKeyEvent(::shortcut).testTag("main-screen")) {
        val wideNow = maxWidth >= WIDE
        SideEffect { wide = wideNow }
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = !wideNow,
            drawerContent = { if (!wideNow) ModalDrawerSheet { filterPanel(Modifier) { scope.launch { drawer.close() } } } },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("koppter") },
                        navigationIcon = {
                            if (!wideNow) {
                                IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.testTag("open-filter")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Filter")
                                }
                            }
                        },
                        actions = {
                            // [impl->dsn~restart-to-update~1]
                            // Words, as no icon tells that the application ends and comes back.
                            update?.let { TextButton(onClick = it, modifier = Modifier.testTag("restart-to-update")) { Text("Restart to update") } }
                            controller.syncStatus?.let { SyncIndicator(it, onSync = controller::sync) }
                            if (controller.canSchedule) {
                                // [impl->dsn~plan-dialog~1]
                                BarButton(Icons.Default.PlayArrow, "Plan", "open-plan") { planning = controller.planSettings }
                                BarButton(Icons.Default.DateRange, "Scheduled tasks", "open-schedule") { schedule = controller.schedule() }
                            }
                            BarButton(Icons.Default.Settings, "Settings", "open-settings") { showSettings = true }
                        },
                    )
                },
                floatingActionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!wideNow) {
                            SmallFloatingActionButton(onClick = { focusSearch() }, modifier = Modifier.testTag("open-search")) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        FloatingActionButton(onClick = { editing = Editing(null) }, modifier = Modifier.testTag("new-task")) {
                            Icon(Icons.Default.Add, contentDescription = "New task")
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Row(Modifier.padding(padding).fillMaxSize()) {
                    if (wideNow) {
                        filterPanel(Modifier.width(280.dp).fillMaxHeight(), null)
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        ViewOptions(controller)
                        // A narrow screen gives the field no room above the list: there, it lies in the drawer, opened by the search button.
                        if (wideNow) searchField(Modifier.padding(8.dp))
                        val emptyText = if (controller.file.entries.isEmpty()) "No tasks" else "No task matches the filter"
                        val open: (Entry) -> Unit = { editing = Editing(it) }
                        if (controller.view == View.OVERVIEW) {
                            // An opened tile belongs to the attribute that was chosen: another one starts with the tiles.
                            androidx.compose.runtime.key(controller.overviewKey) {
                                Overview(overviewStates(controller.visible, controller.today(), controller.overviewKey, controller.file.entries), controller.today(), open, emptyText)
                            }
                        } else if (controller.view == View.AGENDA) {
                            Agenda(controller.planBlocks, controller.dueUnplanned, controller::entryOf, controller.today(), controller::toggle, open, controller::worked)
                        } else if (controller.view == View.WEEK) {
                            Week(controller.planBlocks, controller::entryOf, controller::planSuffix, controller.today(), open)
                        } else if (controller.view == View.INBOX) {
                            Inbox(controller.inbox, controller.contexts, controller::assignContext, open)
                        } else if (controller.board) {
                            Board(
                                controller.groups, controller.today(), controller::toggle, onOpen = { editing = Editing(it) },
                                moved = controller::moved,
                                onMove = controller::move,
                                emptyText = emptyText,
                                priorities = controller.groupKey == GroupKey.PRIORITY,
                            )
                        } else {
                            TaskList(
                                controller.listGroups, controller.today(),
                                controller::toggle, { editing = Editing(it) }, controller::step, controller::drop, emptyText,
                                priorities = controller.listGroupKey == GroupKey.PRIORITY,
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { (entry) ->
        EditDialog(
            original = entry?.task ?: remember { controller.newTask() },
            isNew = entry == null,
            dates = remember(entry) { entry?.let { controller.datesOf(it.task) } },
            originalNote = remember(entry) { controller.noteOf(entry?.task ?: Task(body = "")) },
            newNoteName = remember(entry) { controller.newNoteName() },
            knownProjects = controller.projects,
            knownContexts = controller.contexts,
            knownHashtags = controller.hashtags,
            contextsOf = controller::contextsOf,
            today = controller.today(),
            width = controller.settings.editWidth,
            onWidth = { controller.updateSettings(controller.settings.copy(editWidth = it)) },
            pickImage = pickImage,
            takePhoto = takePhoto,
            imageOf = controller::imageOf,
            onSave = { task, note, images ->
                if (entry == null) controller.add(task, note, images) else controller.replace(entry, task, note, images)
                editing = null
            },
            onDelete = {
                entry?.let(controller::delete)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    planning?.let {
        PlanDialog(settings = it, calendars = controller.calendars, onPlan = controller::planWithCalendars, onDismiss = { planning = null })
    }
    schedule?.let {
        ScheduleDialog(
            schedule = it,
            suggestion = remember { controller.scheduleSuggestion() },
            today = controller.today(),
            onSave = { text ->
                controller.updateSchedule(text)
                schedule = null
            },
            onDismiss = { schedule = null },
        )
    }
    if (showSettings) {
        SettingsDialog(
            settings = controller.settings,
            defaultTodoFile = defaultTodoFile,
            chooseFile = chooseFile,
            chooseFolder = chooseFolder,
            chooseFileNote = chooseFileNote,
            git = git,
            shareLog = shareLog,
            version = version,
            boardColumns = controller.boardColumnsText,
            shareActions = controller.shareActionsText,
            priorityNames = controller.priorityNamesText,
            labelLinks = controller.labelLinksText,
            projectContexts = controller.projectContextsText,
            // [impl->dsn~failure-log~1]
            onGitFailed = controller::report,
            newTask = controller.newTaskText,
            // [impl->dsn~color-themes~1]
            onColors = { theme, appearance -> controller.updateSettings(controller.settings.copy(theme = theme, appearance = appearance)) },
            // [impl->dsn~settings-open-file~1]
            // [impl->dsn~git-setup-dialog~1]
            // The user wants the tasks of the file or repository; nothing of the dialog is written to it.
            onOpenFile = {
                controller.updateSettings(controller.settings.copy(todoFile = it))
                showSettings = false
            },
            onSave = { settings, boardColumns, shareActions, priorityNames, labelLinks, projectContexts, newTask ->
                // The columns, share actions, names and links belong to the file that is open, not to one whose path was typed into the field.
                boardColumns?.let(controller::updateBoardColumns)
                shareActions?.let(controller::updateShareActions)
                priorityNames?.let(controller::updatePriorityNames)
                labelLinks?.let(controller::updateLabelLinks)
                projectContexts?.let(controller::updateProjectContexts)
                newTask?.let(controller::updateNewTask)
                controller.updateSettings(settings)
                showSettings = false
            },
            // [impl->dsn~outlook-clear~1]
            outlookBlocks = if (!controller.readsOutlook) null else object : OutlookBlockActions {
                override suspend fun count(calendar: String) = controller.countOutlookBlocks(calendar)
                override suspend fun clear(calendar: String) = controller.clearOutlookBlocks(calendar)
            },
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * The switches above the tasks: one row for the view, of which one is always shown,
 * and below it what forms the groups of the list or the columns of the board.
 */
// [impl->dsn~view-options~1]
@Composable
private fun ViewOptions(controller: TodoController) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        // Each row is one line that scrolls sideways: in a narrow window, wrapped rows would take the place of the tasks.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (view in View.entries) {
                FilterChip(
                    selected = controller.view == view,
                    onClick = { controller.view = view },
                    label = { Text(if (view == View.INBOX && controller.inbox.isNotEmpty()) "${view.title} (${controller.inbox.size})" else view.title) },
                    modifier = Modifier.testTag("${view.name.lowercase()}-switch"),
                )
            }
        }
        val list = controller.view == View.LIST
        val overview = controller.view == View.OVERVIEW
        if (!list && !overview && !controller.board) return
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (key in if (overview) OVERVIEW_KEYS else GroupKey.entries) {
                FilterChip(
                    selected = when {
                        list -> controller.listGroupKey
                        overview -> controller.overviewKey
                        else -> controller.groupKey
                    } == key,
                    // The board always has columns and the overview always has tiles; the list is divided or not.
                    onClick = {
                        when {
                            list -> controller.listGroupKey = key.takeIf { it != controller.listGroupKey }
                            overview -> controller.overviewKey = key
                            else -> controller.groupKey = key
                        }
                    },
                    label = { Text("by ${key.label.lowercase()}") },
                    modifier = Modifier.testTag("group-${key.name}"),
                )
            }
            if (list && controller.listGroupKey == null) {
                // No switch is selected then, which alone does not tell how the tasks are ordered.
                Text(
                    NO_GROUPS,
                    Modifier.testTag("group-none"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (controller.board && controller.boardColumns.isNotEmpty()) {
                FilterChip(
                    selected = controller.groupKey == null,
                    onClick = { controller.groupKey = null },
                    label = { Text("by filter") },
                    modifier = Modifier.testTag("group-FILTER"),
                )
            }
        }
    }
}

/** A button of the top bar. Its description is its tooltip as well, as an icon alone does not tell what it does. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarButton(icon: ImageVector, description: String, tag: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Icon(icon, contentDescription = description) }
    }
}

/** Shows whether the file is synchronized; a click synchronizes now. */
// [impl->dsn~sync-status~1]
@Composable
private fun SyncIndicator(status: SyncStatus, onSync: () -> Unit) {
    val (text, color) = when (status.state) {
        SyncState.IDLE -> "Synchronized" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.RUNNING -> "Synchronizing" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.FAILED -> "Not synchronized: ${status.message}" to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 360.dp).testTag("sync-status"),
    )
    IconButton(onClick = onSync, enabled = status.state != SyncState.RUNNING, modifier = Modifier.testTag("sync-now")) {
        Icon(Icons.Default.Refresh, contentDescription = "Synchronize now")
    }
}
