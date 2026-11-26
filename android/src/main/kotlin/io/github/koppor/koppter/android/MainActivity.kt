package io.github.koppor.koppter.android

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.storage.logSince
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.DeviceApp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.hours

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
     * Passes the last hour of the newest log file to another application as text, which the user reads before sending it.
     * Older entries belong to another problem than the one that is reported.
     */
    // ponytail: text, which an intent carries up to some hundred kilobytes; a FileProvider if whole files are to be shared.
    // ponytail: the newest file only, so right after it rolled over at 1 MB the hour is cut short; read the backup too if that shows.
    // [impl->dsn~share-log~1]
    private fun shareLog() {
        val newest = logDirectory().toFile().listFiles()?.maxByOrNull { it.lastModified() }
        val text = newest?.readText()?.let { logSince(it, LocalDateTime.now().minusHours(1)) }?.takeLast(SHARED_LOG_LENGTH).orEmpty()
            .ifEmpty { "The log has no entry of the last hour." }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Log of koppter").putExtra(Intent.EXTRA_TEXT, text)
        // Without the application itself as a target: the log is no task.
        val chooser = Intent.createChooser(send, "Share log")
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this, ShareActivity::class.java)))
        startActivity(chooser)
    }

    // The alias of the manifest that carries the category HOME; the package manager keeps whether it is enabled, so no setting of koppter repeats it.
    private val homeAlias by lazy { ComponentName(this, "io.github.koppor.koppter.android.HomeScreen") }
    private var homeScreen by mutableStateOf(false)

    // [impl->dsn~android-home-screen~1]
    private fun offerAsHomeScreen(offer: Boolean) {
        val state = if (offer) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        // Without DONT_KILL_APP, the system ends the application that the user is looking at.
        packageManager.setComponentEnabledSetting(homeAlias, state, PackageManager.DONT_KILL_APP)
        homeScreen = offer
        // Enabling the alias makes koppter a candidate only; the user picks it on this screen, which a few devices lack.
        if (offer) runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }

    // [impl->dsn~android-apps~1]
    private suspend fun otherApps(): List<DeviceApp> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val size = (48 * resources.displayMetrics.density).toInt()
        packageManager.queryIntentActivities(launcher, 0).filter { it.activityInfo.packageName != packageName }.map { found ->
            // An icon that is not drawn leaves the name.
            val icon = runCatching {
                Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
                    found.loadIcon(packageManager).apply { setBounds(0, 0, size, size) }.draw(Canvas(bitmap))
                }.asImageBitmap()
            }.getOrNull()
            DeviceApp("${found.activityInfo.packageName}/${found.activityInfo.name}", found.loadLabel(packageManager).toString(), icon) {
                // As a home screen starts it: the application comes back as it was left, in a task of its own.
                startActivity(
                    Intent(launcher).setClassName(found.activityInfo.packageName, found.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                )
            }
        }
    }

    // [impl->dsn~android-update~1]
    /** The tag of a release newer than the running one, `null` while there is none. */
    private var newerRelease by mutableStateOf<String?>(null)
    private var installing by mutableStateOf(false)

    private lateinit var controller: TodoController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The pickers of FileKit start from this activity.
        FileKit.init(this)
        controller = todoController(gitSetup)
        homeScreen = packageManager.getComponentEnabledSetting(homeAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        publishShareTargets(controller.shareActions)
        // [impl->dsn~android-block-notification~2]
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
            val scope = rememberCoroutineScope()
            // [impl->dsn~android-update~1]
            // Only an APK of the release workflow has a version to compare; a build of a developer is never offered one.
            if (BuildConfig.VERSION_NAME != "0") {
                LaunchedEffect(Unit) {
                    while (true) {
                        newerRelease = withContext(Dispatchers.IO) { latestReleaseTag() }?.takeIf { it != "v${BuildConfig.VERSION_NAME}" }
                        delay(6.hours)
                    }
                }
            }
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
                homeScreen = homeScreen,
                onHomeScreen = ::offerAsHomeScreen,
                apps = ::otherApps,
                update = newerRelease?.let { tag ->
                    {
                        if (!installing) {
                            installing = true
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { installRelease(tag) } }.onFailure {
                                    installing = false
                                    controller.report("Update not downloaded", it)
                                    controller.error = "Update not downloaded: ${it.message}"
                                }
                            }
                        }
                    }
                },
                updateLabel = if (installing) "Downloading update" else "Update",
            )
        }
    }

    // A link that is clicked while the application is open.
    // [impl->dsn~android-links~1]
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // [impl->dsn~android-update~1]
        if (intent.action == INSTALL_STATUS) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                // The confirmation of the system, which also asks once for the permission to install from koppter.
                @Suppress("DEPRECATION")
                PackageInstaller.STATUS_PENDING_USER_ACTION -> intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let(::startActivity)
                // On success, the system ends the application to replace it; nothing is left to show.
                PackageInstaller.STATUS_SUCCESS -> {}
                // Cancel in the dialog of the system leaves no trace; the button offers the update again.
                PackageInstaller.STATUS_FAILURE_ABORTED -> installing = false
                else -> {
                    installing = false
                    controller.error = "Update not installed: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}"
                }
            }
            return
        }
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
