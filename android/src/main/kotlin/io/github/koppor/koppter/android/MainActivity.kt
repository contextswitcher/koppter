package io.github.koppor.koppter.android

import android.Manifest
import android.content.ComponentName
import android.content.res.Configuration
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.GitSetupActions
import io.github.koppor.koppter.ui.TodoController
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openCameraPicker
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The number of characters of the log that are shared: what an intent carries safely, and more than a report needs. */
private const val SHARED_LOG_LENGTH = 60_000

class MainActivity : ComponentActivity() {
    // The bookmark persists the permission, as the file is read again after every restart of the application.
    // Any type of file is offered: a todo.txt file is not always reported as text/plain by a document provider.
    // [impl->dsn~choose-file~1]
    private suspend fun chooseFile(): String? = FileKit.openFilePicker()?.also { it.bookmarkData() }?.path

    // [impl->dsn~choose-folder~1]
    private suspend fun chooseFolder(): String? = FileKit.openDirectoryPicker()?.also { it.bookmarkData() }?.path

    // [impl->dsn~note-images~1]
    private suspend fun pickImage(): List<ByteArray> =
        FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Multiple()).orEmpty().map { noteImage(it.readBytes()) }

    // The camera writes into the cache of the application, where the photo is not needed once it is read.
    private suspend fun takePhoto(): List<ByteArray> = listOfNotNull(FileKit.openCameraPicker()?.let { photo -> noteImage(photo.readBytes()).also { photo.delete() } })

    private val gitSetup by lazy { GitSetup(filesDir.toPath().resolve("git")) }

    // [impl->dsn~git-setup-dialog~1]
    private val gitActions = object : GitSetupActions {
        override fun publicKey(): String = gitSetup.publicKey()

        override suspend fun clone(url: String): String = withContext(Dispatchers.IO) { gitSetup.clone(url).toString() }

        override suspend fun cloneShared(url: String): String = withContext(Dispatchers.IO) { gitSetup.cloneShared(url).toString() }

        override suspend fun check(url: String) = withContext(Dispatchers.IO) { gitSetup.check(url) }
    }

    /**
     * Passes the end of the newest log file to another application as text, which the user reads before sending it.
     */
    // ponytail: text, which an intent carries up to some hundred kilobytes; a FileProvider if whole files are to be shared.
    // [impl->dsn~share-log~1]
    private fun shareLog() {
        val newest = logDirectory().toFile().listFiles()?.maxByOrNull { it.lastModified() }
        val text = newest?.readText()?.takeLast(SHARED_LOG_LENGTH).orEmpty().ifEmpty { "The log is empty." }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Log of koppter").putExtra(Intent.EXTRA_TEXT, text)
        // Without the application itself as a target: the log is no task.
        val chooser = Intent.createChooser(send, "Share log")
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this, ShareActivity::class.java)))
        startActivity(chooser)
    }

    private lateinit var controller: TodoController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The pickers of FileKit start from this activity.
        FileKit.init(this)
        controller = todoController(gitSetup)
        publishShareTargets(controller.shareActions)
        // [impl->dsn~android-block-notification~1]
        // Asked where there is a plan to notify of; the system stops asking after the second refusal.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null && !controller.planBlocks.isNullOrEmpty()) {
            // The answer changes nothing here, so no result is taken.
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        // [impl->dsn~android-links~1]
        // Not after a change of the configuration, such as a rotation: the link would undo what the user filtered since.
        if (savedInstanceState == null) intent?.dataString?.let(controller::openLink)
        val keyboard = resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
        setContent {
            App(
                controller, "todo.txt in the storage of the application", chooseFile = ::chooseFile, chooseFolder = ::chooseFolder, git = gitActions, keyboard = keyboard,
                shareLog = ::shareLog,
                // [impl->dsn~build-version~1]
                // A build outside the release workflow has the version name "0", which tells nothing.
                version = listOf(BuildConfig.VERSION_NAME.takeIf { it != "0" }, BuildConfig.GIT_COMMIT_LINE).filterNot { it.isNullOrEmpty() }.joinToString(" · ").ifEmpty { null },
                pickImage = ::pickImage,
                takePhoto = ::takePhoto,
                // [impl->dsn~choose-file~1]
                chooseFileNote = "Android gives access to a chosen file alone, not to the files next to it: " +
                    "notes, scheduled tasks, board columns and the plan need a chosen folder, whose file todo.txt is opened, or the todo.txt file of a git repository.",
                // [impl->dsn~back~1]
                backHandler = { onBack -> BackHandler(onBack = onBack) },
                // To the background, as back does it for the first activity of an application since Android 12: the application keeps what it shows.
                leave = { moveTaskToBack(true) },
            )
        }
    }

    // A link that is clicked while the application is open.
    // [impl->dsn~android-links~1]
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(controller::openLink)
    }

    // The share actions may have been edited in the settings, and the tasks or the plan changed.
    override fun onStop() {
        super.onStop()
        publishShareTargets(controller.shareActions)
        updateAgendaWidgets()
        publishAgenda(controller)
    }
}
