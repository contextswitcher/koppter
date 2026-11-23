package io.github.koppor.koppter.ui

import io.github.koppor.koppter.outlook.OutlookCalendar
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.storage.ConfigFile
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
import kotlinx.datetime.LocalDateTime

/** Width from which the filter panel stands beside the list instead of sliding over it. */
private val WIDE = 840.dp

/** The room below the end of a view, so that the buttons that float at the bottom cover nothing that is scrolled to its end. */
internal val FLOATING_BUTTONS_ROOM = 88.dp

private const val RELOAD_INTERVAL_MILLIS = 2_000L

/** Short enough that a device left open sees the changes of others before the user edits the same task. */
private val SYNC_INTERVAL = 5.minutes

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
    /** Lets the user choose images for a note and gives their bytes, scaled down, none if none was chosen; `null` on a platform without that. */
    pickImage: (suspend () -> List<ByteArray>)? = null,
    /** As [pickImage], with the camera of the device. */
    takePhoto: (suspend () -> List<ByteArray>)? = null,
    /** Ends the application so that a newer version of it starts; `null` unless there is one and something starts it. */
    update: (() -> Unit)? = null,
    /** The words of the button of [update]. */
    updateLabel: String = "Restart to update",
    /** Gives the parts of the window that the platform draws, such as the title bar on Windows, the dark or the light variant. */
    windowVariant: (dark: Boolean) -> Unit = {},
    backHandler: @Composable (onBack: () -> Unit) -> Unit = {},
    leave: () -> Unit = {},
    /** Whether the system offers the application as its home screen; `null` on a platform without a home screen to replace. */
    homeScreen: Boolean? = null,
    /** Offers the application as the home screen of the device or stops offering it. */
    onHomeScreen: (Boolean) -> Unit = {},
    /** Reads the other applications of the device, which the user starts from here while this one is the home screen; `null` on a platform without [homeScreen]. */
    apps: (suspend () -> List<DeviceApp>)? = null,
) {
    val dark = controller.settings.theme.dark ?: when (controller.settings.appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    // [impl->dsn~title-bar-theme~1]
    LaunchedEffect(dark) { windowVariant(dark) }
    // A link without a program that handles it, such as one of ContextSwitcher on a device without it, is reported.
    val platform = LocalUriHandler.current
    val uriHandler = remember(platform) {
        object : UriHandler {
            override fun openUri(uri: String) = try {
                // [impl->dsn~outlook-tag~1]
                if (controller.openOutlook(uri)) Unit
                // [impl->dsn~latex-open~1]
                else if (uri.startsWith(LATEX_LINK)) controller.openLatex(uri.removePrefix(LATEX_LINK))
                else platform.openUri(uri)
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
                // [impl->dsn~outlook-appointments~1]
                controller.refreshAppointments()
                delay(OUTLOOK_REFRESH)
            }
        }
    }
    // [impl->dsn~absences~1]
    // Whatever view is shown: the device of a member that knows the calendars keeps the absences current for the others.
    val memberCalendars = controller.memberCalendars
    LaunchedEffect(controller, memberCalendars) {
        while (!memberCalendars.isNullOrBlank()) {
            controller.updateAbsences()
            delay(ABSENCE_REFRESH)
        }
    }
    val projectEditor = remember(controller) {
        object : ProjectEditor {
            override fun properties(project: String) = controller.projectProperties(project)
            override fun save(project: String, properties: ConfigFile.ProjectProperties) = controller.updateProjectProperties(project, properties)
        }
    }
    // [impl->dsn~latex-todos~1]
    // Again at once when a project gets another document.
    if (controller.readsLatex) {
        LaunchedEffect(controller, controller.latexDocuments) {
            while (true) {
                controller.refreshLatex()
                delay(LATEX_REFRESH)
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
            LocalProjectEditor provides projectEditor,
            LocalSetDue provides controller::setDue,
        ) {
            MainScreen(controller, defaultTodoFile, chooseFile, chooseFolder, chooseFileNote, git, shareLog, version, pickImage, takePhoto, update, updateLabel, backHandler, leave, homeScreen, onHomeScreen, apps)
        }
    }
}

/** The link of the label *LaTeX*, which the application handles itself, as it handles the links of Outlook. */
internal const val LATEX_LINK = "koppter-latex:"

internal const val LEAVE_WARNING = "Back again leaves koppter."

/** How often the To-Do list of Outlook is read again: each reading starts a process that asks Outlook. */
private val OUTLOOK_REFRESH = 2.minutes

/** How often the LaTeX documents are read again, so that a TODO written in the editor shows up while koppter is open. */
private val LATEX_REFRESH = 1.minutes

/** What the list is without groups. */
internal const val NO_GROUPS = "no groups: by priority, then due date"

/** The task whose edit dialog is open; `entry` is null for a task that is not in the file yet. */
/**
 * Whether the device has a keyboard. With one, the field that is typed into next gets the focus when it appears;
 * without one, that would only cover the screen with the keyboard of the touch screen.
 */
val LocalKeyboard = staticCompositionLocalOf { true }

/** The task whose dialog is open; without one, a new task, which starts with the [context] if there is one. */
private data class Editing(val entry: Entry?, val context: String? = null)

/** The longer side of an image of a note in pixels: enough to read a photographed page, and a file of some hundred kilobytes for the repository. */
const val NOTE_IMAGE_SIZE = 1600

/** Ctrl with the n-th of these keys shows the n-th view. */
/** How often the calendars of the members are read: a calendar changes a few times a day, and each change is a commit. */
private const val ABSENCE_REFRESH = 30 * 60_000L

private val VIEW_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight)

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
    pickImage: (suspend () -> List<ByteArray>)?,
    takePhoto: (suspend () -> List<ByteArray>)?,
    update: (() -> Unit)?,
    updateLabel: String,
    backHandler: @Composable (onBack: () -> Unit) -> Unit,
    leave: () -> Unit,
    homeScreen: Boolean?,
    onHomeScreen: (Boolean) -> Unit,
    apps: (suspend () -> List<DeviceApp>)?,
) {
    var showApps by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    // [impl->dsn~main-screen~1]
    // The floating buttons would cover the row at the bottom edge; they give way while the user scrolls down.
    // A scroll that moves nothing is at the end or the start of a list, or in one too short to scroll: it brings them back.
    var floatingShown by remember { mutableStateOf(true) }
    val floatingScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y != 0f || available.y != 0f) floatingShown = consumed.y >= 0f
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(controller.view) { floatingShown = true }
    var draft by remember { mutableStateOf<Draft?>(null) }
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
    // [impl->dsn~app-links~1]
    LaunchedEffect(controller.newTaskRequested) {
        if (controller.newTaskRequested) {
            editing = Editing(null)
            controller.newTaskRequested = false
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
            focusSearch()
        }
        when {
            !event.isShiftPressed && event.key == Key.N -> editing = Editing(null)
            // The key of OneNote for its search.
            !event.isShiftPressed && event.key == Key.E -> search(all = true)
            // The key of browsers and editors for their search; with Shift, it is the key of SkedPal.
            event.key == Key.F -> search(all = false)
            !event.isShiftPressed && view >= 0 -> View.entries[view].takeIf { it in controller.views }?.let { controller.view = it }
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
    // [impl->dsn~periodic-sync~1]
    LaunchedEffect(controller) {
        while (true) {
            delay(SYNC_INTERVAL)
            controller.sync()
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
{ controller.filter = controller.filter.copy(text = it) },
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
            search = if (onClose != null) ({
                searchField(Modifier.padding(bottom = 8.dp))
                // [impl->dsn~filter-matches~1]
                // The drawer covers the list, and the strip beside it that closes the drawer is narrow.
                if (controller.filter.text.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        val count = controller.visible.size
                        Text(
                            if (count == 1) "1 match" else "$count matches",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).testTag("filter-matches"),
                        )
                        TextButton(onClick = onClose, modifier = Modifier.testTag("filter-show-results")) { Text("Go to results") }
                    }
                }
            }) else null,
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
            // [impl->dsn~android-apps~1]
            // Only as the home screen: elsewhere, the home screen of the system lists the applications.
            val startsApps = homeScreen == true && apps != null
            // A wide window has no top bar: the title of the window names the application, and its buttons end the row of the views.
            val barActions: @Composable RowScope.() -> Unit = {
                // [impl->dsn~restart-to-update~1]
                // Words, as no icon tells that the application ends and comes back.
                // [impl->dsn~android-update~1]
                update?.let { TextButton(onClick = it, modifier = Modifier.testTag("restart-to-update")) { Text(updateLabel) } }
                controller.syncStatus?.let { SyncIndicator(it, controller.lastSynchronized, onSync = controller::sync) }
                if (controller.canSchedule) {
                    // [impl->dsn~plan-dialog~1]
                    // A word: the play icon starts the clock of a task.
                    TextButton(onClick = { planning = controller.planSettings }, modifier = Modifier.testTag("open-plan")) { Text("Plan") }
                    BarButton(Icons.Default.DateRange, "Scheduled tasks", "open-schedule") { schedule = controller.schedule() }
                }
                if (startsApps) TextButton(onClick = { showApps = true }, modifier = Modifier.testTag("open-apps")) { Text("Apps") }
                BarButton(Icons.Default.Settings, "Settings", "open-settings") { showSettings = true }
            }
            Scaffold(
                topBar = {
                    if (!wideNow) TopAppBar(
                        // A phone has no room for the name beside the button Apps or the one of an update; as the home screen, the application needs no name.
                        title = { if (!startsApps && update == null) Text("koppter") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.testTag("open-filter")) {
                                Icon(Icons.Default.Menu, contentDescription = "Filter")
                            }
                        },
                        actions = barActions,
                    )
                },
                floatingActionButton = {
                    // [impl->dsn~family-view~1]
                    // The family view has a button for a new task per member and nothing to search; buttons that float would cover its calendar.
                    if (controller.view != View.FAMILY) AnimatedVisibility(floatingShown, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!wideNow) {
                                SmallFloatingActionButton(onClick = { focusSearch() }, modifier = Modifier.testTag("open-search")) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                            SmallFloatingActionButton(onClick = { editing = Editing(null) }, modifier = Modifier.testTag("new-task")) {
                                Icon(Icons.Default.Add, contentDescription = "New task")
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Row(Modifier.padding(padding).fillMaxSize().nestedScroll(floatingScroll)) {
                    // [impl->dsn~family-view~1]
                    // The family view shows the day of everybody, which no filter narrows, and its columns take the width.
                    val filtered = controller.view != View.FAMILY
                    if (wideNow && filtered) {
                        filterPanel(Modifier.width(280.dp).fillMaxHeight(), null)
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        // The line of the current time and the clock of the running task move on once a minute.
                        val minute by androidx.compose.runtime.produceState(controller.minuteOfDay()) {
                            while (true) {
                                delay(60_000)
                                value = controller.minuteOfDay()
                            }
                        }
                        val running = controller.running
                        val clock: (Entry) -> Unit = { if (it == running) controller.stop(it) else controller.start(it) }
                        // [impl->dsn~outlook-appointments~1]
                        val appointmentScope = rememberCoroutineScope()
                        val sidelineWords = controller.sidelineWords
                        val appointments = if (!controller.readsOutlook) null else CalendarAppointments(
                            controller.appointments,
                            onShow = controller::showAppointments,
                            sidelined = { OutlookCalendar.isSidelined(it, sidelineWords) },
                            onOpen = controller::openAppointment,
                            onSideline = { appointment, on -> appointmentScope.launch { controller.setSideline(appointment, on) } },
                        )
                        val now = kotlinx.datetime.LocalDateTime(controller.today(), kotlinx.datetime.LocalTime(minute / 60, minute % 60))
                        // [impl->dsn~time-log~1]
                        val away = controller::goAway.takeIf { controller.canSchedule && controller.awaySince == null }
                        running?.let { RunningClock(it, now, onOpen = { editing = Editing(it) }, onStop = { controller.stop(it) }, onAway = away) }
                        controller.awaySince?.let { AwayBar(it, now, onBack = controller::back) }
                        ViewOptions(controller, if (wideNow) barActions else null)
                        // A narrow screen gives the field no room above the list: there, it lies in the drawer, opened by the search button.
                        if (wideNow && filtered) searchField(Modifier.padding(8.dp))
                        val emptyText = if (controller.file.entries.isEmpty()) "No tasks" else "No task matches the filter"
                        val open: (Entry) -> Unit = { editing = Editing(it) }
                        if (controller.view == View.OVERVIEW) {
                            // An opened tile belongs to the attribute that was chosen: another one starts with the tiles.
                            androidx.compose.runtime.key(controller.overviewKey) {
                                Overview(overviewStates(controller.visible, controller.today(), controller.overviewKey, controller.file.entries), controller.today(), controller::toggle, open, emptyText)
                            }
                        } else if (controller.view == View.AGENDA) {
                            Agenda(controller.shownBlocks, controller.dueUnplanned, controller::entryOf, controller.today(), controller::toggle, open, controller::worked, running = running, onClock = clock)
                        } else if (controller.view == View.TODAY) {
                            Today(controller.shownBlocks, controller.dueUnplanned, controller::entryOf, controller::planSuffix, controller.today(), minute, controller::toggle, open, controller::worked, running, clock,
                                onAway = away,
                                calendarWidth = controller.settings.todayCalendarWidth,
                                onCalendarWidth = { controller.updateSettings(controller.settings.copy(todayCalendarWidth = it)) },
                                appointments = appointments,
                            )
                        } else if (controller.view == View.WEEK) {
                            Week(controller.shownBlocks, controller::entryOf, controller::planSuffix, controller.today(), open, appointments)
                        } else if (controller.view == View.FAMILY) {
                            FamilyCalendar(
                                controller.file.entries, controller.members, controller::blocksOf, controller.today(), minute,
                                controller.rankingPeriod, { controller.rankingPeriod = it }, controller::toggle, open,
                                onAdd = { editing = Editing(null, context = it) },
                                problems = controller.absenceProblems,
                            )
                        } else if (controller.view == View.INBOX) {
                            Inbox(controller.inbox, controller.contexts, controller::assignContext, controller::toggle, open)
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

    editing?.let { (entry, context) ->
        EditDialog(
            // [impl->dsn~family-view~1]
            original = entry?.task ?: remember { controller.newTask().let { task -> if (context == null) task else task.edit(contexts = task.contexts + context) } },
            isNew = entry == null,
            dates = remember(entry) { entry?.let { controller.datesOf(it.task) } },
            originalNote = remember(entry) { controller.noteOf(entry?.task ?: Task(body = "")) },
            newNoteName = remember(entry) { controller.newNoteName() },
            knownProjects = controller.projects,
            knownContexts = controller.contexts,
            knownHashtags = controller.hashtags,
            nameUses = remember(entry) { controller.nameUses },
            contextsOf = controller::contextsOf,
            today = controller.today(),
            width = controller.settings.editWidth,
            onWidth = { controller.updateSettings(controller.settings.copy(editWidth = it)) },
            pickImage = pickImage,
            takePhoto = takePhoto,
            imageOf = controller::imageOf,
            others = remember(entry, controller.file) { controller.file.entries.filter { it != entry } },
            onSave = { task, note, images, changed ->
                if (entry == null) {
                    controller.add(task, note, images, changed)
                    draft = null
                } else {
                    controller.replace(entry, task, note, images, changed)
                }
                editing = null
            },
            onDelete = {
                entry?.let(controller::delete)
                editing = null
            },
            onDismiss = { editing = null },
            // A new task of a member starts with the context of the member, not with what was entered for another task.
            draft = draft.takeIf { entry == null && context == null },
            onDraft = { draft = it },
        )
    }
    if (showApps && apps != null) {
        AppsDialog(
            apps, controller.settings.appStarts,
            onStarted = { controller.updateSettings(controller.settings.copy(appStarts = withStart(controller.settings.appStarts, it))) },
            onDismiss = { showApps = false },
        )
    }
    planning?.let {
        PlanDialog(settings = it, calendars = controller.calendars, onPlan = controller::planWithCalendars, onDismiss = { planning = null }, today = controller.today())
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
            memberCalendars = controller.memberCalendars,
            members = controller.members,
            // [impl->dsn~config-completion~1]
            knownProjects = controller.projects,
            knownContexts = controller.contexts,
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
            opensLatex = controller.opensLatex,
            syncsContextSwitcher = controller.syncsContextSwitcher,
            homeScreen = homeScreen,
            onHomeScreen = onHomeScreen,
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
private fun ViewOptions(controller: TodoController, actions: (@Composable RowScope.() -> Unit)?) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        // The buttons of the top bar, in a wide window, go to a line of their own where all switches would not fit next to them.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, itemVerticalAlignment = Alignment.CenterVertically) {
            // Each row is one line that scrolls sideways: in a narrow window, wrapped rows would take the place of the tasks.
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (view in controller.views) {
                    FilterChip(
                        selected = controller.view == view,
                        onClick = { controller.view = view },
                        label = { Text(if (view == View.INBOX && controller.inbox.isNotEmpty()) "${view.title} (${controller.inbox.size})" else view.title) },
                        modifier = Modifier.testTag("${view.name.lowercase()}-switch"),
                    )
                }
            }
            if (actions != null) Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        // [impl->dsn~sync-status~1]
        // On a line of its own: among the buttons, the words take their room, so that a phone loses the last buttons and a wide window moves them to the next line until the next success.
        // In a wide window, the line ends below the button.
        SyncFailure(controller.syncStatus, Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 4.dp), if (actions != null) TextAlign.End else TextAlign.Start)
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
private fun BarButton(icon: ImageVector, description: String, tag: String, enabled: Boolean = true, tint: Color? = null, iconModifier: Modifier = Modifier, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        // The color of the content is read inside the button, which fades it while it is disabled.
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) { Icon(icon, contentDescription = description, modifier = iconModifier, tint = tint ?: LocalContentColor.current) }
    }
}

/** Shows whether the file is synchronized, and since when; a click synchronizes now. */
// [impl->dsn~sync-status~1]
@Composable
private fun SyncIndicator(status: SyncStatus, last: LocalDateTime?, onSync: () -> Unit) {
    when (status.state) {
        SyncState.IDLE -> BarButton(Synchronized, "Synchronized${last?.let { " at ${it.date} ${it.time}" }.orEmpty()}. Synchronize now", "sync-now", onClick = onSync)
        SyncState.RUNNING -> {
            // Against the clock, as the arrows of the icon point.
            val angle by rememberInfiniteTransition().animateFloat(0f, -360f, infiniteRepeatable(tween(1500, easing = LinearEasing)))
            // In the color of the other buttons: faded as a disabled button is, the arrows are hardly seen, and their turning tells that a click does nothing now.
            BarButton(Synchronizing, "Synchronizing", "sync-now", enabled = false, tint = LocalContentColor.current, iconModifier = Modifier.rotate(angle), onClick = onSync)
        }
        // With the cause, as the words beside the button are cut where the bar is narrow.
        SyncState.FAILED -> BarButton(
            NotSynchronized,
            "Synchronize now. Not synchronized: ${status.message}${last?.let { ". Last synchronized at ${it.date} ${it.time}" }.orEmpty()}",
            "sync-now",
            tint = MaterialTheme.colorScheme.error,
            onClick = onSync,
        )
    }
}

/**
 * Why the file is not synchronized, and nothing while it is.
 * Words only for a failure, which has a cause to tell: beside a status in words, a phone has no room for the name of the application.
 */
@Composable
private fun SyncFailure(status: SyncStatus?, modifier: Modifier, textAlign: TextAlign) {
    if (status?.state == SyncState.FAILED) Text(
        "Not synchronized: ${status.message}",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelMedium,
        textAlign = textAlign,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.testTag("sync-status"),
    )
}

// The icons published_with_changes, sync and sync_problem of the Material icons (Apache License 2.0), which the core set of the icons does not have.
// All three are the arrows of a synchronization, so that the button reads as the one that synchronizes in every status.
private fun syncIcon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(addPathNodes(path), fill = SolidColor(Color.Black)).build()
private val Synchronized = syncIcon("Synchronized", "M17.66 9.53l-7.07 7.07-4.24-4.24 1.41-1.41 2.83 2.83 5.66-5.66 1.41 1.41zM4 12c0-2.33 1.02-4.42 2.62-5.88L9 8.5v-6H3l2.2 2.2C3.24 6.52 2 9.11 2 12c0 5.19 3.95 9.45 9 9.95v-2.02c-3.94-.49-7-3.86-7-7.93zm18 0c0-5.19-3.95-9.45-9-9.95v2.02c3.94.49 7 3.86 7 7.93 0 2.33-1.02 4.42-2.62 5.88L15 15.5v6h6l-2.2-2.2c1.96-1.82 3.2-4.41 3.2-7.3z")
private val Synchronizing = syncIcon("Synchronizing", "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z")
private val NotSynchronized = syncIcon("NotSynchronized", "M3 12c0 2.21.91 4.2 2.36 5.64L3 20h6v-6l-2.24 2.24C5.68 15.15 5 13.66 5 12c0-2.61 1.67-4.83 4-5.65V4.26C5.55 5.15 3 8.27 3 12zm8 5h2v-2h-2v2zM21 4h-6v6l2.24-2.24C18.32 8.85 19 10.34 19 12c0 2.61-1.67 4.83-4 5.65v2.09c3.45-.89 6-4.01 6-7.74 0-2.21-.91-4.2-2.36-5.64L21 4zm-10 9h2V7h-2v6z")
