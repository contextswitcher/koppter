package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.koppor.koppter.storage.DEFAULT_COMMITTER_EMAIL
import io.github.koppor.koppter.storage.DEFAULT_COMMITTER_NAME
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.storage.Settings
import kotlinx.coroutines.launch
import io.github.koppor.koppter.outlook.OutlookCalendar

/**
 * The message of a failure, followed by its causes where they say something else:
 * a library often reports only that a connection ended, and the cause tells why.
 */
// [impl->dsn~git-setup-dialog~1]
internal fun failureText(e: Throwable): String {
    val lines = mutableListOf(e.message ?: e.toString())
    var last = e.message
    // Not endlessly, should a cause name itself.
    generateSequence(e.cause) { it.cause }.take(8).forEach { cause ->
        if (cause.message != last) lines += "Cause: ${cause::class.simpleName}: ${cause.message}"
        last = cause.message
    }
    return lines.joinToString("\n")
}

/** Git access that the application brings along, for a repository that is not cloned yet. */
interface GitSetupActions {
    /**
     * The public SSH key of this device, to be registered with the git server;
     * `null` where the application uses the SSH configuration of the user.
     */
    fun publicKey(): String?

    /** Clones the repository and returns the location of its todo.txt file. */
    suspend fun clone(url: String): String

    /** Fails as [clone] would if the server is not reached or does not let this device in; clones nothing. */
    suspend fun check(url: String)
}

/** Whether the text names a git repository on a server rather than a file: a URL, or the form `user@host:path` of SSH. */
internal fun isGitAddress(text: String): Boolean = Regex("(ssh|git|https?)://.+|[\\w.-]+@[\\w.-]+:.+").matches(text)

/**
 * The address for SSH of a repository of GitHub that is given as the address of one of its pages, which is what the browser offers to copy;
 * any other text as it is. The application has an SSH key and no password for HTTPS.
 */
// [impl->dsn~git-setup-dialog~1]
internal fun sshAddress(text: String): String =
    Regex("https?://(?:www\\.)?github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?(?:[/?#].*)?").matchEntire(text)
        ?.let { "git@github.com:${it.groupValues[1]}/${it.groupValues[2]}.git" } ?: text

/** The page of GitHub that adds a deploy key to the repository of the address; `null` for an address of another server. */
// [impl->dsn~git-setup-dialog~1]
internal fun deployKeyPage(address: String): String? =
    Regex("(?:ssh://)?git@github\\.com[:/]([\\w.-]+)/([\\w.-]+?)(?:\\.git)?/?").matchEntire(sshAddress(address.trim()))
        ?.let { "https://github.com/${it.groupValues[1]}/${it.groupValues[2]}/settings/keys/new" }

private const val OPENED = "Opened. The fields below show the configuration of this file. Cancel returns to the file from before."

/** Scales offered for the names of the priorities: the letters of the format, numbers, and steps of the Fibonacci sequence. */
private val PRIORITY_PRESETS = listOf(
    emptyList(),
    listOf("1", "2", "3", "4", "5"),
    listOf("1", "2", "3", "5", "8"),
    listOf("1", "2", "3", "5", "8", "13", "21", "34"),
)

// [impl->dsn~settings-dialog~1]
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: Settings,
    defaultTodoFile: String,
    chooseFile: (suspend () -> String?)?,
    /** Lets the user pick a folder whose todo.txt file is opened. */
    chooseFolder: (suspend () -> String?)?,
    /** What the user has to know about a file chosen with [chooseFile]; `null` if there is nothing. */
    chooseFileNote: String?,
    git: GitSetupActions?,
    /** Passes the log to another application; `null` on a platform where the user reaches the log file. */
    shareLog: (() -> Unit)?,
    /** The definitions of the board columns; `null` if the storage cannot keep them. */
    boardColumns: String?,
    /** The share actions; `null` if the storage cannot keep them. */
    shareActions: String?,
    /** The names of the priorities; `null` if the storage cannot keep them. */
    priorityNames: String?,
    /** The links of projects, contexts and hashtags; `null` if the storage cannot keep them. */
    labelLinks: String?,
    /** The contexts of the projects; `null` if the storage cannot keep them. */
    projectContexts: String?,
    /** What a new task starts with; `null` if the storage cannot keep it. */
    newTask: String?,
    /** Called when the user chooses colors: they apply at once, so that the user sees them behind the dialog. */
    onColors: (ColorTheme, Appearance) -> Unit,
    /**
     * Called with a file that the user chose or cloned: it is opened at once, so that the fields for its configuration
     * show what the file brings along instead of what belonged to the file before.
     */
    onOpenFile: (String) -> Unit,
    /** Called with the todo.txt file of a repository that was cloned: it becomes the file of the application, and the dialog is left. */
    onCloned: (String) -> Unit = onOpenFile,
    /** Called with what failed with the repository, a clone or a check of the connection, for the log. */
    onGitFailed: (String, Exception) -> Unit = { _, _ -> },
    onSave: (Settings, boardColumns: String?, shareActions: String?, priorityNames: String?, labelLinks: String?, projectContexts: String?, newTask: String?) -> Unit,
    onDismiss: () -> Unit,
    /** Counts and deletes the blocks of koppter in the calendar of Outlook of an account; `null` where Outlook is not reached. */
    outlookBlocks: OutlookBlockActions? = null,
) {
    val scope = rememberCoroutineScope()
    // ponytail: the clipboard of text, as in Labels.kt. Taken outside the dialog, which is a layer with a clipboard of its own that a test cannot replace.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    // That of the application, which reports a link that is not opened; taken outside the dialog as the clipboard is.
    val uriHandler = LocalUriHandler.current
    // [impl->dsn~settings-open-file~1]
    // What depends on the file starts anew when another file is opened from the dialog.
    val openFile = settings.todoFile
    var todoFile by remember(openFile) { mutableStateOf(openFile) }
    var writeDates by remember { mutableStateOf(settings.writeDates) }
    var topCount by remember { mutableStateOf(settings.topCount.toString()) }
    val topCountValue = topCount.trim().toIntOrNull()?.takeIf { it > 0 }
    var columns by remember(openFile) { mutableStateOf(boardColumns) }
    var actions by remember(openFile) { mutableStateOf(shareActions) }
    var priorities by remember(openFile) { mutableStateOf(priorityNames) }
    var links by remember(openFile) { mutableStateOf(labelLinks) }
    var contexts by remember(openFile) { mutableStateOf(projectContexts) }
    var newTaskDefaults by remember(openFile) { mutableStateOf(newTask) }
    var committerName by remember { mutableStateOf(settings.committerName) }
    var committerEmail by remember { mutableStateOf(settings.committerEmail) }
    var outlookCalendar by remember { mutableStateOf(settings.outlookCalendar) }
    var outlookWeeks by remember { mutableStateOf(settings.outlookWeeks.toString()) }
    val outlookWeeksValue = outlookWeeks.trim().toIntOrNull()?.takeIf { it >= 0 }
    var cloning by remember { mutableStateOf(false) }
    var fileMessage by remember { mutableStateOf("") }
    var fileFailed by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    /** Whether the field holds the address of a repository, which is cloned before anything else is done with it. */
    val address = git != null && isGitAddress(todoFile.trim())

    /** Shows why the repository was not reached, with what most often is the cause on a device with a key of its own. */
    fun gitFailed(shown: String, logged: String, e: Exception) {
        fileFailed = true
        onGitFailed(logged, e)
        // A server that does not know the key ends the connection without saying why.
        val ownKey = git?.let { runCatching(it::publicKey).getOrNull() } != null
        fileMessage = "$shown: ${failureText(e)}" + if (ownKey) "\nIs the public SSH key of this device, shown below, registered with the server?" else ""
    }

    val state = listOf(todoFile, writeDates, topCount, columns, actions, priorities, links, contexts, newTaskDefaults, committerName, committerEmail, outlookCalendar, outlookWeeks)
    val initial = remember(openFile) { state }
    val fileBefore = remember { openFile }

    // Another file that was opened is a change as well: leaving the dialog without Save returns to the file from before.
    GuardedDialog(changed = state != initial || openFile != fileBefore, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("settings-dialog")) {
            Column(Modifier.verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = todoFile,
                    // A pasted address, which arrives at once, is converted; one that is typed is left alone until it is cloned.
                    onValueChange = { todoFile = if (git != null && it.length > todoFile.length + 1) sshAddress(it) else it },
                    label = { Text(if (git == null) "todo.txt file" else "todo.txt file or address of a git repository") },
                    placeholder = { Text(defaultTodoFile) },
                    supportingText = {
                        Text(
                            "Empty: $defaultTodoFile" +
                                (if (git == null) "" else "\nAn address such as git@github.com:user/todo.git is cloned with the button Clone, which then shows the tasks of the clone. The address of a page of a repository of GitHub becomes that form.") +
                                "\nA file in a git repository is committed after every change and synchronized with the remote repository." +
                                "\nThe files next to the todo.txt file belong to it: notes, ice_recur.txt, ${ConfigFile.NAME} and calendar.txt.",
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-file"),
                )
                if (fileMessage.isNotEmpty()) {
                    Text(
                        fileMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fileFailed) MaterialTheme.colorScheme.error else Color.Unspecified,
                        modifier = Modifier.testTag("settings-file-message"),
                    )
                }
                if (chooseFile != null || git != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((title, tag, choose) in listOfNotNull(
                            chooseFile?.let { Triple("Choose file", "settings-choose-file", it) },
                            chooseFolder?.let { Triple("Choose folder", "settings-choose-folder", it) },
                        )) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            choose()?.let {
                                                onOpenFile(it)
                                                fileFailed = false
                                                fileMessage = OPENED
                                            }
                                        } catch (e: Exception) {
                                            // A platform without a program that shows the dialog, or one that refuses it.
                                            fileFailed = true
                                            fileMessage = "Not chosen: ${e.message ?: e}"
                                        }
                                    }
                                },
                                modifier = Modifier.testTag(tag),
                            ) {
                                Text(title)
                            }
                        }
                        if (git != null) {
                            // [impl->dsn~git-setup-dialog~1]
                            OutlinedButton(
                                onClick = {
                                    cloning = true
                                    fileFailed = false
                                    fileMessage = "Checking"
                                    scope.launch {
                                        try {
                                            git.check(sshAddress(todoFile.trim()))
                                            fileMessage = "Connected"
                                        } catch (e: Exception) {
                                            gitFailed("Not connected", "Connection check failed", e)
                                        }
                                        cloning = false
                                        // The message stands above the buttons and may be out of view.
                                        scroll.animateScrollTo(0)
                                    }
                                },
                                enabled = address && !cloning,
                                modifier = Modifier.testTag("settings-check-connection"),
                            ) {
                                Text("Check connection")
                            }
                            // At the address and not at the end of the dialog, where Save stands: a clone saves nothing of the dialog.
                            Button(
                                onClick = {
                                    cloning = true
                                    fileFailed = false
                                    fileMessage = "Cloning"
                                    scope.launch {
                                        try {
                                            onCloned(git.clone(sshAddress(todoFile.trim())))
                                        } catch (e: Exception) {
                                            gitFailed("Not cloned", "Clone failed", e)
                                        }
                                        cloning = false
                                        scroll.animateScrollTo(0)
                                    }
                                },
                                enabled = address && !cloning,
                                modifier = Modifier.testTag("settings-clone"),
                            ) {
                                Text("Clone")
                            }
                        }
                    }
                    if (chooseFileNote != null) Text(chooseFileNote, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("settings-choose-file-note"))
                }
                Row(
                    Modifier.fillMaxWidth().toggleable(value = writeDates, onValueChange = { writeDates = it }, role = Role.Checkbox)
                        .testTag("settings-write-dates"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = writeDates, onCheckedChange = null, modifier = Modifier.padding(6.dp))
                    Text("Write creation and completion dates into the file")
                }
                OutlinedTextField(
                    value = topCount,
                    onValueChange = { topCount = it },
                    label = { Text("Number of tasks for the filter option of the first tasks") },
                    isError = topCountValue == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-top-count"),
                )
                columns?.let { text ->
                    ConfigField(
                        "Board columns by filter", text, { columns = it }, "Next = -@waiting -#someday\nWaiting = @waiting",
                        "One column per line as title = filter.", "settings-board-columns",
                    )
                }
                // [impl->dsn~priority-names~1]
                priorities?.let { text ->
                    ConfigField(
                        "Names of the priorities", text, { priorities = it }, "A = 1\nB = 2\nC = 3",
                        "One priority per line as letter = name. The tasks show the names, and a task is given one of these priorities; the file keeps the letters.",
                        "settings-priorities",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRIORITY_PRESETS.forEachIndexed { index, names ->
                            AssistChip(
                                onClick = { priorities = PriorityNames.lines(names) },
                                label = { Text(if (names.isEmpty()) "Letters" else names.joinToString(", ")) },
                                modifier = Modifier.testTag("settings-priorities-preset-$index"),
                            )
                        }
                    }
                }
                // [impl->dsn~label-links~1]
                links?.let { text ->
                    ConfigField(
                        "Links of projects and contexts", text, { links = it }, "+Fair = https://example.org/fair\n@phone = https://example.org/numbers",
                        "One link per line as label = link, the label with its sign: + for a project, @ for a context, # for a hashtag. The menu of the label opens the link.",
                        "settings-links",
                    )
                }
                // [impl->dsn~new-task-defaults~1]
                newTaskDefaults?.let { text ->
                    ConfigField(
                        "New task", text, { newTaskDefaults = it }, "priority = C\ndue = next weekend",
                        "What the dialog of a new task starts with: priority = a priority, and due = a day of the menu of the due date, such as tomorrow or next weekend, or an interval such as 10d. Without lines, a new task starts empty.",
                        "settings-new-task",
                    )
                }
                // [impl->dsn~project-contexts~1]
                contexts?.let { text ->
                    ConfigField(
                        "Contexts of projects", text, { contexts = it }, "+Fair = @errands\n+Garden = @home @weekend",
                        "One project per line as +project = @context. A project that is added to a task in its dialog brings these contexts along.",
                        "settings-project-contexts",
                    )
                }
                // [impl->dsn~share-actions~1]
                actions?.let { text ->
                    ConfigField(
                        "Share actions", text, { actions = it }, "inbox = {}\nreading = {} +research @reading due:2w dur:1h",
                        "What sharing a text to koppter on Android adds: one action per line as title = task; {} stands for the shared text.",
                        "settings-share-actions",
                    )
                }
                Text("Colors", style = MaterialTheme.typography.titleMedium)
                RadioRow(ColorTheme.entries.map { it to it.title }, settings.theme, tag = "settings-theme", onSelect = { onColors(it, settings.appearance) })
                RadioRow(Appearance.entries.map { it to it.title }, settings.appearance, tag = "settings-appearance", onSelect = { onColors(settings.theme, it) })
                if (git != null) {
                    Text("Git repository", style = MaterialTheme.typography.titleMedium)
                    // [impl->dsn~committer~1]
                    CommitterField("Name of the commits", committerName, { committerName = it }, DEFAULT_COMMITTER_NAME, "settings-committer-name")
                    CommitterField("Email address of the commits", committerEmail, { committerEmail = it }, DEFAULT_COMMITTER_EMAIL, "settings-committer-email")
                    GitSection(git, clipboard, uriHandler, deployKeyPage(todoFile))
                }
                if (outlookBlocks != null) {
                    // [impl->dsn~outlook-blocks~1]
                    Text("Outlook", style = MaterialTheme.typography.titleMedium)
                    CommitterField(
                        "Account of the calendar", outlookCalendar, { outlookCalendar = it }, "Default account", "settings-outlook-calendar",
                        "The name of the account in Outlook, such as its email address. Its main calendar takes the plan as private, tentative appointments whose subject ends with ${OutlookCalendar.MARK}.",
                    )
                    OutlinedTextField(
                        value = outlookWeeks,
                        onValueChange = { outlookWeeks = it },
                        label = { Text("Weeks of the plan in Outlook") },
                        supportingText = { Text("Planning writes the blocks of so many weeks from today to Outlook and replaces those it wrote before; 0 writes nothing.") },
                        isError = outlookWeeksValue == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings-outlook-weeks"),
                    )
                    OutlookClearButton(outlookBlocks, outlookCalendar)
                }
                Row(Modifier.fillMaxWidth()) {
                    // [impl->dsn~share-log~1]
                    if (shareLog != null) {
                        TextButton(onClick = shareLog, modifier = Modifier.testTag("settings-share-log")) { Text("Share log") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    fun save(file: String) = onSave(
                        settings.copy(
                            todoFile = file, writeDates = writeDates, topCount = topCountValue ?: settings.topCount,
                            committerName = committerName.trim(), committerEmail = committerEmail.trim(),
                            outlookCalendar = outlookCalendar.trim(), outlookWeeks = outlookWeeksValue ?: settings.outlookWeeks,
                        ),
                        columns?.trim(),
                        actions?.trim(),
                        priorities?.trim(),
                        links?.trim(),
                        contexts?.trim(),
                        newTaskDefaults?.trim(),
                    )
                    Button(
                        onClick = { save(todoFile.trim()) },
                        // An address is no file to save: it is cloned first.
                        enabled = topCountValue != null && outlookWeeksValue != null && !cloning && !address,
                        modifier = Modifier.testTag("settings-save"),
                    ) { Text("Save") }
                }
            }
        }
    }
}

/** A field for a block of the configuration file: lines of the form `key = value`, with an example shown while it is empty. */
@Composable
private fun ConfigField(title: String, value: String, onChange: (String) -> Unit, example: String, help: String, tag: String) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    Column {
        // The title stands above the field: as a label in it, it would hide the example until the field has the focus.
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            // Paler than the placeholder of Material, which is hard to tell from lines that were typed.
            placeholder = { Text(example, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            supportingText = {
                Text("$help ${if (value.isEmpty()) "The pale lines are an example. " else ""}Kept in ${ConfigFile.NAME} next to the todo.txt file.")
            },
            textStyle = style,
            minLines = maxOf(2, example.lines().size),
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

/** Counts and deletes the blocks of koppter in the main calendar of an account of Outlook, named as in Outlook; blank for the default account. */
interface OutlookBlockActions {
    suspend fun count(calendar: String): Int
    suspend fun clear(calendar: String): Int
}

/**
 * Deletes the blocks of koppter from the calendar of Outlook after the user confirmed their number.
 * Which appointments are koppter's is decided by AnchorHub, by marks that only it sets.
 */
// [impl->dsn~outlook-clear~1]
@Composable
private fun OutlookClearButton(actions: OutlookBlockActions, calendar: String) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val name = calendar.trim().ifEmpty { "the default account" }
    fun run(action: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                action()
            } catch (e: Exception) {
                message = "Outlook did not answer: ${e.message ?: e}"
            }
            busy = false
        }
    }
    OutlinedButton(
        onClick = {
            message = ""
            run {
                val count = actions.count(calendar)
                if (count == 0) message = "There are no blocks of koppter in the calendar of $name." else confirm = count
            }
        },
        enabled = !busy,
        modifier = Modifier.testTag("settings-outlook-clear"),
    ) { Text("Remove koppter's blocks from Outlook") }
    if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("settings-outlook-message"))
    confirm?.let { count ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Remove $count blocks?") },
            text = { Text("The calendar of $name has $count appointments that koppter wrote. They go to the deleted items of Outlook; other appointments are not touched.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = null
                        run { message = "Removed ${actions.clear(calendar)} blocks from the calendar of $name." }
                    },
                    modifier = Modifier.testTag("settings-outlook-clear-confirm"),
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

/** A field whose [default], which applies while it is empty, stands in it in gray. */
@Composable
private fun CommitterField(
    title: String,
    value: String,
    onChange: (String) -> Unit,
    default: String,
    tag: String,
    help: String = "Empty: that of the git configuration, otherwise the value in gray.",
) {
    Column {
        // The title stands above the field: as a label in it, it would hide the default until the field has the focus.
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(default, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            supportingText = { Text(help) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

/** The public key of the device, where the application has a key of its own. */
// [impl->dsn~git-setup-dialog~1]
@Composable
private fun GitSection(git: GitSetupActions, clipboard: ClipboardManager, uriHandler: UriHandler, deployKeyPage: String?) {
    val publicKey = remember { runCatching(git::publicKey).getOrElse { "No key: ${it.message}" } } ?: return

    var copied by remember { mutableStateOf(false) }
    Text(
        "Public SSH key of this device. Register it with the git server, for GitHub as a deploy key with write access. " +
            if (copied) "Copied to the clipboard." else "A tap on the key copies it.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("settings-public-key-help"),
    )
    // A tap copies the whole key: selecting a line of some hundred characters on a touch screen does not work well.
    Surface(
        onClick = {
            clipboard.setText(AnnotatedString(publicKey))
            copied = true
        },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().testTag("settings-public-key"),
    ) {
        Text(publicKey, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
    }
    // The API of GitHub for deploy keys needs a login with the rights of an administrator; the page in the browser has that login already.
    if (deployKeyPage != null) {
        OutlinedButton(
            onClick = {
                clipboard.setText(AnnotatedString(publicKey))
                copied = true
                uriHandler.openUri(deployKeyPage)
            },
            modifier = Modifier.testTag("settings-add-key"),
        ) { Text("Copy the key and open GitHub to add it") }
        Text("On the page of GitHub, paste the key, tick \"Allow write access\" and add the key.", style = MaterialTheme.typography.bodySmall)
    }
}
