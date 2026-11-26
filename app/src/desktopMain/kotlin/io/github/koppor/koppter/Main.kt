package io.github.koppor.koppter

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.koppor.koppter.plan.fetchCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.storage.SharedLists
import io.github.koppor.koppter.storage.logToFile
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.GitSetupActions
import io.github.koppor.koppter.ui.isGitAddress
import io.github.koppor.koppter.ui.NOTE_IMAGE_SIZE
import io.github.koppor.koppter.todotxt.Filter
import io.github.koppor.koppter.todotxt.filterOfLink
import io.github.koppor.koppter.todotxt.syncOfLink
import io.github.koppor.koppter.todotxt.taskLinkOf
import io.github.koppor.koppter.todotxt.toLink
import io.github.koppor.koppter.todotxt.with
import io.github.koppor.koppter.ui.TodoController
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import net.harawata.appdirs.AppDirsFactory
import org.tinylog.Logger
import java.io.File
import io.github.koppor.koppter.latex.Latex
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

// [impl->adr~desktop-locations-from-appdirs~1]
private val directories = AppDirsFactory.getInstance()

/** The directory for the settings: `%APPDATA%\koppter` on Windows, `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` on Linux. */
// [impl->dsn~desktop-locations~1]
private fun configDirectory(): Path = Path(directories.getUserConfigDir("koppter", null, null, true))

/** The directory for files that the application creates: `%APPDATA%\koppter` on Windows, `$XDG_DATA_HOME/koppter` or `~/.local/share/koppter` on Linux. */
private fun dataDirectory(): Path = Path(directories.getUserDataDir("koppter", null, null, true))

/** Shows the file dialog of the operating system, which FileKit opens from a thread of its own. */
// [impl->dsn~choose-file~1]
private suspend fun chooseFile(owner: java.awt.Window): String? =
    FileKit.openFilePicker(dialogSettings = FileKitDialogSettings(title = "todo.txt file", parent = FileKitDialogParent.awt(owner)))?.absolutePath()

/** The todo.txt file of a folder that the user picks, such as the clone of a git repository; the file need not exist yet. */
// [impl->dsn~choose-folder~1]
private suspend fun chooseFolder(owner: java.awt.Window): String? =
    FileKit.openDirectoryPicker(dialogSettings = FileKitDialogSettings(title = "Folder of the todo.txt file", parent = FileKitDialogParent.awt(owner)))
        ?.let { Path(it.absolutePath(), "todo.txt").toString() }

/** The images that the user picks for a note, scaled down and as JPEG. */
// [impl->dsn~note-images~1]
private suspend fun pickImage(owner: java.awt.Window): List<ByteArray> =
    FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Multiple(), dialogSettings = FileKitDialogSettings(title = "Images for the note", parent = FileKitDialogParent.awt(owner)))
        .orEmpty().map { FileKit.compressImage(it.readBytes(), maxWidth = NOTE_IMAGE_SIZE, maxHeight = NOTE_IMAGE_SIZE) }

/** The exit code that `scripts/run-loop` answers by pulling, building and starting the application again; far from the codes of a JVM or a shell. */
private const val RESTART_EXIT_CODE = 55

/** What git prints for [arguments] in [directory], or `null` when it fails or is missing. */
// ponytail: no timeout, a git that hangs holds one thread and the update is not offered; add one if that is seen.
internal fun git(directory: File, vararg arguments: String): String? = runCatching {
    val process = ProcessBuilder("git", *arguments).directory(directory).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    output.takeIf { process.waitFor() == 0 }
}.getOrNull()

/**
 * How many commits the upstream branch of [checkout] has that the commit [running] lacks, after a fetch.
 * Counted from the commit that runs and not from `HEAD`, which a pull outside the application moves while the old build keeps running.
 * 0 whenever git cannot tell: the update is an offer, not an error.
 */
// [impl->dsn~restart-to-update~1]
internal fun commitsBehind(checkout: File, running: String): Int {
    git(checkout, "fetch", "--quiet") ?: return 0
    return git(checkout, "rev-list", "--count", "$running..@{u}")?.toIntOrNull() ?: 0
}

/**
 * The bullets of the changelog [newer] that the changelog [older] lacks, each with the `###` heading it stands under, such as `Added`.
 * Compared by their text, as a release moves the bullets of *Unreleased* under a date without changing them.
 */
// [impl->dsn~whats-new~1]
internal fun changelogNews(older: String, newer: String): List<Pair<String, String>> {
    val known = older.lines().toSet()
    var heading = ""
    return newer.lines().mapNotNull { line ->
        if (line.startsWith("### ")) heading = line.removePrefix("### ")
        (heading to line.removePrefix("- ")).takeIf { line.startsWith("- ") && line !in known }
    }
}

/**
 * Gives the drawing surface of [window] its size again after the scale of its display changed, which on Windows it keeps from the old scale.
 * Restoring and maximizing, or resizing, is what the user would do by hand.
 */
// [impl->dsn~display-change~1]
private fun refitAfterDisplayChange(window: java.awt.Frame) {
    // A display change arrives as several events; act once after they settled.
    val refit = javax.swing.Timer(500) {
        val state = window.extendedState
        if (state and java.awt.Frame.MAXIMIZED_BOTH == java.awt.Frame.MAXIMIZED_BOTH) {
            window.extendedState = state and java.awt.Frame.MAXIMIZED_BOTH.inv()
            window.extendedState = state
        } else {
            window.setSize(window.width + 1, window.height)
            window.setSize(window.width - 1, window.height)
        }
    }.apply { isRepeats = false }
    window.addPropertyChangeListener("graphicsConfiguration") { refit.restart() }
}

/** Lets Windows and macOS draw the title bar of [window] dark or light; other systems keep theirs. */
// [impl->dsn~title-bar-theme~1]
private fun titleBarVariant(window: java.awt.Window, dark: Boolean) {
    val os = System.getProperty("os.name")
    if (os.startsWith("Mac")) {
        // The JDK applies a change of this property of the root pane to the open window as well.
        (window as? javax.swing.RootPaneContainer)?.rootPane
            ?.putClientProperty("apple.awt.windowAppearance", if (dark) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua")
        return
    }
    // The window is displayable once Compose draws into it, which is when this is called.
    if (!os.startsWith("Windows") || !window.isDisplayable) return
    runCatching {
        // 20 is DWMWA_USE_IMMERSIVE_DARK_MODE, known to Windows 10 since 20H1; older versions return an error, which is ignored.
        com.sun.jna.Function.getFunction("dwmapi", "DwmSetWindowAttribute")
            .invokeInt(arrayOf(com.sun.jna.Native.getWindowPointer(window), 20, com.sun.jna.ptr.IntByReference(if (dark) 1 else 0), 4))
    }.onFailure { Logger.warn(it, "The title bar keeps its colors") }
}

fun main(args: Array<String>) {
    // [impl->dsn~link-single-instance~1]
    // Before anything else: a start that only hands its link on ends at once.
    val port = LinkPort(dataDirectory().resolve("link.port"))
    val link = args.firstOrNull { filterOfLink(it) != null || syncOfLink(it) != null || taskLinkOf(it) != null }
    if (link != null && port.pass(link)) return
    // [impl->dsn~contextswitcher-sync~1]
    // ContextSwitcher sends its selection at every change; with no window open, the next start shows it instead of a window opening now.
    val sync = link?.let(::syncOfLink)
    if (sync != null) {
        val file = SettingsFile(configDirectory().resolve("settings.properties"))
        val settings = file.load()
        if (!settings.contextSwitcherSync) return
        val filter = (filterOfLink(settings.filter) ?: Filter()).with(sync)
        file.save(settings.copy(filter = if (filter.isDefault) "" else filter.toLink()))
        return
    }
    logToFile(dataDirectory().resolve("logs"))
    // [impl->dsn~failure-log~1]
    Thread.setDefaultUncaughtExceptionHandler { _, e -> Logger.error(e, "The application failed") }
    // A file in the home directory, where earlier versions kept it, stays in use.
    val defaultTodoFile = Path(System.getProperty("user.home")).resolve("todo.txt").takeIf { it.exists() } ?: dataDirectory().resolve("todo.txt")
    // [impl->dsn~git-setup-dialog~1]
    val git = object : GitSetupActions {
        override fun publicKey(): String? = null
        override suspend fun clone(url: String) = withContext(Dispatchers.IO) { GitSetup(dataDirectory(), ownKey = false).clone(url).toString() }
        override suspend fun cloneShared(url: String) = withContext(Dispatchers.IO) { GitSetup(dataDirectory(), ownKey = false).cloneShared(url).toString() }
        override suspend fun check(url: String) = withContext(Dispatchers.IO) { GitSetup(dataDirectory(), ownKey = false).check(url) }
    }
    val settingsFile = SettingsFile(configDirectory().resolve("settings.properties"))
    val gitSetup = GitSetup(dataDirectory(), ownKey = false)
    val controller = TodoController(
        // [impl->dsn~clone-by-address~1]
        // Settings of an earlier version name the clone instead of the address of its repository.
        settings = settingsFile.load().let { settings -> gitSetup.withAddresses(settings).also { if (it !== settings) settingsFile.save(it) } },
        storageFor = { settings ->
            fun storageOf(file: Path) = gitSetup.storageFor(file, settings.committerName, settings.committerEmail)
            val main = when {
                settings.todoFile.isBlank() -> storageOf(defaultTodoFile)
                // [impl->dsn~clone-by-address~1]
                isGitAddress(settings.todoFile) -> gitSetup.storageForAddress(settings.todoFile, settings.committerName, settings.committerEmail)
                else -> storageOf(Path(settings.todoFile))
            }
            // [impl->dsn~shared-lists~1]
            // [impl->dsn~member-lists~1]
            val mainFile = when {
                settings.todoFile.isBlank() -> defaultTodoFile
                isGitAddress(settings.todoFile) -> gitSetup.repositoryFile
                else -> Path(settings.todoFile)
            }
            SharedLists.of(main, gitSetup.listsWithMembers(settings.sharedLists, mainFile)) { gitSetup.sharedListStorage(it, settings.committerName, settings.committerEmail) }
        },
        saveSettings = settingsFile::save,
        today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
        minuteOfDay = { java.time.LocalTime.now().toSecondOfDay() / 60 },
        fetch = { address -> withContext(Dispatchers.IO) { fetchCalendar(address) } },
        // The message is passed without arguments, so that tinylog does not read braces in it as placeholders.
        log = { message, cause -> if (cause == null) Logger.error(message as Any) else Logger.error(cause, message) },
        outlook = AnchorHubOutlook.installed(),
        // [impl->dsn~onenote-backlinks~1]
        oneNoteBacklinks = AnchorHubOneNote.installed()?.let { anchorHub -> anchorHub::sync },
        // [impl->dsn~latex-todos~1]
        readFile = { path -> File(withHome(path)).takeIf { it.isFile }?.readText() },
        // [impl->dsn~latex-open~1]
        openFile = { template, path, line ->
            val file = File(withHome(path))
            if (template.isBlank()) thread(isDaemon = true) { java.awt.Desktop.getDesktop().open(file) }
            else ProcessBuilder(Latex.command(template, file.path, line)).start()
        },
        // [impl->dsn~contextswitcher-sync~1]
        openContextSwitcher = { url ->
            thread(isDaemon = true) {
                runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
                    .onFailure { Logger.warn(it, "ContextSwitcher did not take the filter") }
            }
        },
    )
    val links = Channel<String>(Channel.UNLIMITED)
    link?.let(links::trySend)
    port.listen { links.trySend(it) }
    thread(isDaemon = true) { registerLinkScheme() }
    // [impl->dsn~application-icon~1]
    val icon = BitmapPainter(object {}.javaClass.getResourceAsStream("/koppter.png")!!.readBytes().decodeToImageBitmap())
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "koppter",
            icon = icon,
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
        ) {
            LaunchedEffect(Unit) { refitAfterDisplayChange(window) }
            // [impl->dsn~restart-to-update~1]
            var updateWaits by remember { mutableStateOf(false) }
            var updateNews by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
            LaunchedEffect(Unit) {
                // Set by `scripts/run-loop` to the commit it built, with the checkout as the working directory.
                val running = System.getenv("KOPPTER_RUN_LOOP")?.ifBlank { null } ?: return@LaunchedEffect
                withContext(Dispatchers.IO) {
                    // Goes on after an update was found, so that the news include what was pushed since.
                    while (true) {
                        if (commitsBehind(File("."), running) > 0) {
                            // [impl->dsn~whats-new~1]
                            updateNews = changelogNews(git(File("."), "show", "$running:CHANGELOG.md").orEmpty(), git(File("."), "show", "@{u}:CHANGELOG.md").orEmpty())
                            updateWaits = true
                        }
                        delay(30.minutes)
                    }
                }
            }
            App(
                controller,
                defaultTodoFile.toString(),
                chooseFile = { chooseFile(window) },
                chooseFolder = { chooseFolder(window) },
                git = git,
                pickImage = { pickImage(window) },
                // [impl->dsn~build-version~1]
                version = remember { ClassLoader.getSystemResource("koppter-version.txt")?.readText()?.ifBlank { null } },
                // Every change is in the file already, as on closing the window.
                update = if (updateWaits) ({ exitProcess(RESTART_EXIT_CODE) }) else null,
                updateNews = updateNews,
                windowVariant = { titleBarVariant(window, it) },
            )
            // [impl->dsn~link-single-instance~1]
            LaunchedEffect(Unit) {
                for (received in links) {
                    // [impl->dsn~contextswitcher-sync~1]
                    // The selection of ContextSwitcher changes the filter only: the user works in ContextSwitcher.
                    if (controller.applySync(received)) continue
                    controller.openLink(received)
                    // The window may be minimized or behind the program in which the link was clicked.
                    window.extendedState = window.extendedState and java.awt.Frame.ICONIFIED.inv()
                    window.toFront()
                    window.requestFocus()
                }
            }
        }
    }
}

/** The path with `~` at its start as the home directory, as a shell reads it. */
private fun withHome(path: String): String = path.replaceFirst(Regex("^~(?=/|$)"), Regex.escapeReplacement(System.getProperty("user.home")))
