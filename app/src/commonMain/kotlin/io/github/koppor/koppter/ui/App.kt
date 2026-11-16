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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
 * @param chooseFileNote what the user has to know about a file chosen with [chooseFile], shown below its button
 * @param shareLog passes the log to another application, offered in the settings on platforms where the user does not reach the log file
 */
@Composable
fun App(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)? = null,
    git: GitSetupActions? = null,
    keyboard: Boolean = true,
    shareLog: (() -> Unit)? = null,
    chooseFileNote: String? = null,
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
                platform.openUri(uri)
            } catch (e: Exception) {
                controller.error = "Not opened: $uri"
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
            MainScreen(controller, defaultTodoFile, chooseFile, chooseFileNote, git, shareLog, backHandler, leave)
        }
    }
}

internal const val LEAVE_WARNING = "Back again leaves koppter."

/** The task whose edit dialog is open; `entry` is null for a task that is not in the file yet. */
/**
 * Whether the device has a keyboard. With one, the field that is typed into next gets the focus when it appears;
 * without one, that would only cover the screen with the keyboard of the touch screen.
 */
val LocalKeyboard = staticCompositionLocalOf { true }

private data class Editing(val entry: Entry?)

/** Ctrl with the n-th of these keys shows the n-th view. */
private val VIEW_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six)

// [impl->dsn~main-screen~1]
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    controller: TodoController,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    chooseFileNote: String?,
    git: GitSetupActions?,
    shareLog: (() -> Unit)?,
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
    val quickAddFocus = remember { FocusRequester() }
    val filterFocus = remember { FocusRequester() }
    // Keys reach the shortcuts only through a focused element, so the screen takes the focus whenever none of its elements has it:
    // at the start, after the element with the focus went away with its view, after a dialog closed, and after a touch took the focus away.
    var focused by remember { mutableStateOf(false) }
    val dialog = editing != null || showSettings || schedule != null || planning != null
    LaunchedEffect(focused, dialog) { if (!focused && !dialog) rootFocus.requestFocus() }

    // [impl->dsn~keyboard-shortcuts~1]
    fun shortcut(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || !(event.isCtrlPressed || event.isMetaPressed) || event.isAltPressed) return false
        val view = VIEW_KEYS.indexOf(event.key)
        // In a narrow window, the filter panel is in the drawer and has no field to focus.
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
            try {
                filterFocus.requestFocus()
            } catch (_: IllegalStateException) {
                scope.launch { drawer.open() }
            }
        }
        when {
            !event.isShiftPressed && event.key == Key.N -> editing = Editing(null)
            // The key of OneNote for its search.
            !event.isShiftPressed && event.key == Key.E -> search(all = true)
            !event.isShiftPressed && view >= 0 -> controller.view = View.entries[view]
            !event.isShiftPressed -> return false
            event.key == Key.A -> quickAddFocus.requestFocus()
            event.key == Key.F -> search(all = false)
            // The keys of SkedPal for its outline, today list, calendar and plan.
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

    val filterPanel: @Composable (Modifier, (() -> Unit)?) -> Unit = { modifier, onClose ->
        FilterPanel(
            filter = controller.filter,
            onChange = { controller.filter = it },
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
            textModifier = Modifier.focusRequester(filterFocus),
            onClose = onClose,
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
        val wide = maxWidth >= WIDE
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = !wide,
            drawerContent = { if (!wide) ModalDrawerSheet { filterPanel(Modifier) { scope.launch { drawer.close() } } } },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("koppter") },
                        navigationIcon = {
                            if (!wide) {
                                IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.testTag("open-filter")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Filter")
                                }
                            }
                        },
                        actions = {
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
                    FloatingActionButton(onClick = { editing = Editing(null) }, modifier = Modifier.testTag("new-task")) {
                        Icon(Icons.Default.Add, contentDescription = "New task")
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Row(Modifier.padding(padding).fillMaxSize()) {
                    if (wide) {
                        filterPanel(Modifier.width(280.dp).fillMaxHeight(), null)
                        VerticalDivider()
                    }
                    Column(Modifier.weight(1f)) {
                        // The views stand above the field for a new task: a task is looked up more often than added.
                        ViewOptions(controller)
                        // A line to type into is for a keyboard; without one, the button for a new task opens the dialog.
                        if (LocalKeyboard.current) QuickAdd(onAdd = controller::add, preview = controller::datesPreview, completions = controller::completions, focus = quickAddFocus)
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
                            )
                        } else {
                            TaskList(
                                controller.listGroups, controller.listGroupKey != null, controller.today(),
                                controller::toggle, { editing = Editing(it) }, controller::step, controller::drop, emptyText,
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { (entry) ->
        EditDialog(
            original = entry?.task ?: Task(body = ""),
            isNew = entry == null,
            dates = remember(entry) { entry?.let { controller.datesOf(it.task) } },
            originalNote = remember(entry) { controller.noteOf(entry?.task ?: Task(body = "")) },
            newNoteName = remember(entry) { controller.newNoteName() },
            knownProjects = controller.projects,
            knownContexts = controller.contexts,
            knownHashtags = controller.hashtags,
            today = controller.today(),
            onSave = { task, note ->
                if (entry == null) controller.add(task, note) else controller.replace(entry, task, note)
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
        val fileBefore = remember { controller.settings.todoFile }
        SettingsDialog(
            settings = controller.settings,
            defaultTodoFile = defaultTodoFile,
            chooseFile = chooseFile,
            chooseFileNote = chooseFileNote,
            git = git,
            shareLog = shareLog,
            boardColumns = controller.boardColumnsText,
            shareActions = controller.shareActionsText,
            priorityNames = controller.priorityNamesText,
            labelLinks = controller.labelLinksText,
            // [impl->dsn~color-themes~1]
            onColors = { theme, appearance -> controller.updateSettings(controller.settings.copy(theme = theme, appearance = appearance)) },
            // [impl->dsn~settings-open-file~1]
            onOpenFile = { controller.updateSettings(controller.settings.copy(todoFile = it)) },
            onSave = { settings, boardColumns, shareActions, priorityNames, labelLinks ->
                // The columns, share actions, names and links belong to the file that is open, not to one whose path was typed into the field.
                boardColumns?.let(controller::updateBoardColumns)
                shareActions?.let(controller::updateShareActions)
                priorityNames?.let(controller::updatePriorityNames)
                labelLinks?.let(controller::updateLabelLinks)
                controller.updateSettings(settings)
                showSettings = false
            },
            onDismiss = {
                // A file opened from the dialog was a look at it: without Save, the file from before is open again.
                controller.updateSettings(controller.settings.copy(todoFile = fileBefore))
                showSettings = false
            },
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
                    label = { Text(if (view == View.INBOX && controller.inbox.isNotEmpty()) "${view.title} ${controller.inbox.size}" else view.title) },
                    modifier = Modifier.testTag("${view.name.lowercase()}-switch"),
                )
            }
        }
        val list = controller.view == View.LIST
        val overview = controller.view == View.OVERVIEW
        if (!list && !overview && !controller.board) return
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

/**
 * A line that adds a task from text in todo.txt syntax, for input without opening the dialog.
 * A project, a context or a hashtag that is being typed is completed from the known ones: Tab takes the first one offered.
 */
// [impl->dsn~quick-add~1]
@Composable
private fun QuickAdd(onAdd: (String) -> Unit, preview: (String) -> String?, completions: (String) -> List<String>, focus: FocusRequester) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    val text = value.text
    fun submit() {
        onAdd(text)
        value = TextFieldValue("")
    }
    // [impl->dsn~quick-add-completion~1]
    // The text for which the user closed the offers; they come back with the next character.
    var closedFor by remember { mutableStateOf<String?>(null) }
    // ponytail: completes the word at the end of the line, not the one at the cursor; look at the selection if that is missed.
    val offers = if (closedFor == text) emptyList() else completions(text)
    fun complete(name: String) {
        val completed = text.substringBeforeLast(' ', "").let { if (it.isEmpty()) "" else "$it " } + name + " "
        value = TextFieldValue(completed, TextRange(completed.length))
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
            placeholder = { Text("Add a task, such as: (A) Call Mom +Family @phone next week") },
            // [impl->dsn~quick-add-dates~1]
            // Shows the line before it is written, as words that were not meant as a date become one as well.
            supportingText = preview(text)?.let { line -> { Text(line, Modifier.testTag("quick-add-preview")) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().padding(8.dp).focusRequester(focus).testTag("quick-add")
                .onPreviewKeyEvent {
                    when {
                        it.type != KeyEventType.KeyDown -> false
                        // The Enter key of a hardware keyboard does not trigger the IME action on the desktop.
                        it.key == Key.Enter -> true.also { submit() }
                        it.key == Key.Tab && offers.isNotEmpty() -> true.also { complete(offers.first()) }
                        it.key == Key.Escape && offers.isNotEmpty() -> true.also { closedFor = text }
                        else -> false
                    }
                },
        )
        // Not focusable, so that the keys keep going to the field.
        DropdownMenu(
            expanded = offers.isNotEmpty(),
            onDismissRequest = { closedFor = text },
            offset = DpOffset(8.dp, (-4).dp),
            properties = PopupProperties(focusable = false),
            modifier = Modifier.testTag("quick-add-completions"),
        ) {
            for (offer in offers) {
                DropdownMenuItem(text = { Text(offer) }, onClick = { complete(offer) }, modifier = Modifier.testTag("quick-add-complete-$offer"))
            }
        }
    }
}
