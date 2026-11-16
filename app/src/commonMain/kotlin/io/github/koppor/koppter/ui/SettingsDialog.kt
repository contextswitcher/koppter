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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
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
import io.github.koppor.koppter.storage.Appearance
import io.github.koppor.koppter.storage.ColorTheme
import io.github.koppor.koppter.storage.ConfigFile
import io.github.koppor.koppter.todotxt.PriorityNames
import io.github.koppor.koppter.storage.Settings
import kotlinx.coroutines.launch

/** Git access that the application brings along, for a repository that is not cloned yet. */
interface GitSetupActions {
    /**
     * The public SSH key of this device, to be registered with the git server;
     * `null` where the application uses the SSH configuration of the user.
     */
    fun publicKey(): String?

    /** Clones the repository and returns the location of its todo.txt file. */
    suspend fun clone(url: String): String
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
    /** Called when the user chooses colors: they apply at once, so that the user sees them behind the dialog. */
    onColors: (ColorTheme, Appearance) -> Unit,
    /**
     * Called with a file that the user chose or cloned: it is opened at once, so that the fields for its configuration
     * show what the file brings along instead of what belonged to the file before.
     */
    onOpenFile: (String) -> Unit,
    onSave: (Settings, boardColumns: String?, shareActions: String?, priorityNames: String?, labelLinks: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // ponytail: the clipboard of text, as in Labels.kt. Taken outside the dialog, which is a layer with a clipboard of its own that a test cannot replace.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
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
    var cloning by remember { mutableStateOf(false) }
    var fileMessage by remember { mutableStateOf("") }
    /** Whether the field holds the address of a repository, which is cloned before anything else is done with it. */
    val address = git != null && isGitAddress(todoFile.trim())

    val state = listOf(todoFile, writeDates, topCount, columns, actions, priorities, links)
    val initial = remember(openFile) { state }
    val fileBefore = remember { openFile }

    // Another file that was opened is a change as well: leaving the dialog without Save returns to the file from before.
    GuardedDialog(changed = state != initial || openFile != fileBefore, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 560.dp).testTag("settings-dialog")) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                (if (git == null) "" else "\nAn address such as git@github.com:user/todo.git is cloned with the button Clone, and the todo.txt file of the clone is opened. The address of a page of a repository of GitHub becomes that form.") +
                                "\nA file in a git repository is committed after every change and synchronized with the remote repository." +
                                "\nThe files next to the todo.txt file belong to it: notes, ice_recur.txt, ${ConfigFile.NAME} and calendar.txt.",
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-file"),
                )
                if (fileMessage.isNotEmpty()) {
                    Text(fileMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("settings-file-message"))
                }
                if (chooseFile != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                chooseFile()?.let {
                                    onOpenFile(it)
                                    fileMessage = OPENED
                                }
                            }
                        },
                        modifier = Modifier.testTag("settings-choose-file"),
                    ) {
                        Text("Choose file")
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
                    GitSection(git, clipboard)
                }
                Row(Modifier.fillMaxWidth()) {
                    // [impl->dsn~share-log~1]
                    if (shareLog != null) {
                        TextButton(onClick = shareLog, modifier = Modifier.testTag("settings-share-log")) { Text("Share log") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    fun save(file: String) = onSave(
                        settings.copy(todoFile = file, writeDates = writeDates, topCount = topCountValue ?: settings.topCount),
                        columns?.trim(),
                        actions?.trim(),
                        priorities?.trim(),
                        links?.trim(),
                    )
                    Button(
                        onClick = {
                            val file = todoFile.trim().let { if (git != null) sshAddress(it) else it }
                            if (git != null && address) {
                                cloning = true
                                fileMessage = "Cloning"
                                scope.launch {
                                    try {
                                        onOpenFile(git.clone(file))
                                        fileMessage = OPENED
                                    } catch (e: Exception) {
                                        fileMessage = "Not cloned: ${e.message ?: e}"
                                    }
                                    cloning = false
                                }
                            } else {
                                save(file)
                            }
                        },
                        enabled = topCountValue != null && !cloning,
                        modifier = Modifier.testTag("settings-save"),
                        // The dialog stays open after cloning, so the button says what it does.
                    ) { Text(if (address) "Clone" else "Save") }
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

/** The public key of the device, where the application has a key of its own. */
// [impl->dsn~git-setup-dialog~1]
@Composable
private fun GitSection(git: GitSetupActions, clipboard: ClipboardManager) {
    val publicKey = remember { runCatching(git::publicKey).getOrElse { "No key: ${it.message}" } } ?: return

    Text("Git repository", style = MaterialTheme.typography.titleMedium)
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
}
