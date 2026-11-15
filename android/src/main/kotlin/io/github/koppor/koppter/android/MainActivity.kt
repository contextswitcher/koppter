package io.github.koppor.koppter.android

import android.content.ComponentName
import android.content.res.Configuration
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.App
import io.github.koppor.koppter.ui.GitSetupActions
import io.github.koppor.koppter.ui.TodoController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The number of characters of the log that are shared: what an intent carries safely, and more than a report needs. */
private const val SHARED_LOG_LENGTH = 60_000

class MainActivity : ComponentActivity() {
    private var picked: CompletableDeferred<String?>? = null

    // The permission is persisted, as the file is read again after every restart of the application.
    // [impl->dsn~choose-file~1]
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        picked?.complete(uri?.toString())
    }

    private suspend fun chooseFile(): String? {
        val result = CompletableDeferred<String?>()
        picked = result
        // A todo.txt file is not always reported as text/plain by a document provider.
        picker.launch(arrayOf("*/*"))
        return result.await()
    }

    private val gitSetup by lazy { GitSetup(filesDir.toPath().resolve("git")) }

    // [impl->dsn~git-setup-dialog~1]
    private val gitActions = object : GitSetupActions {
        override fun publicKey(): String = gitSetup.publicKey()

        override suspend fun clone(url: String): String = withContext(Dispatchers.IO) { gitSetup.clone(url).toString() }
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
        controller = todoController(gitSetup)
        publishShareTargets(controller.shareActions)
        // [impl->dsn~android-links~1]
        // Not after a change of the configuration, such as a rotation: the link would undo what the user filtered since.
        if (savedInstanceState == null) intent?.dataString?.let(controller::openLink)
        val keyboard = resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
        setContent { App(controller, "todo.txt in the storage of the application", chooseFile = ::chooseFile, git = gitActions, keyboard = keyboard, shareLog = ::shareLog) }
    }

    // A link that is clicked while the application is open.
    // [impl->dsn~android-links~1]
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(controller::openLink)
    }

    // The share actions may have been edited in the settings.
    override fun onStop() {
        super.onStop()
        publishShareTargets(controller.shareActions)
    }
}
