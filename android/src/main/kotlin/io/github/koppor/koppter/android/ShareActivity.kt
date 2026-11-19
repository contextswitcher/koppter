package io.github.koppor.koppter.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import io.github.koppor.koppter.storage.Settings
import io.github.koppor.koppter.storage.SharedLists
import io.github.koppor.koppter.plan.fetchCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.koppor.koppter.storage.SettingsFile
import io.github.koppor.koppter.storage.TodoStorage
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.todotxt.ShareAction
import io.github.koppor.koppter.ui.TodoController
import io.github.koppor.koppter.ui.isGitAddress
import io.github.koppor.koppter.ui.NOTE_IMAGE_SIZE
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.tinylog.Logger

/**
 * Receives text or an image that another application shares and adds it as a task, without showing a screen.
 * A share action chosen in the share sheet arrives as the id of its shortcut; the plain entry of the application applies the first action.
 */
// [impl->dsn~android-share-target~1]
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT))
            // Some applications repeat the subject in the text.
            .let { (it.dropLast(1).filterNot { subject -> subject in it.last() } + it.takeLast(1)).joinToString(" ") }
        val action = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        // The share sheet ranks the targets by their use.
        action?.let { getSystemService(ShortcutManager::class.java)?.reportShortcutUsed(it) }
        // [impl->dsn~note-images~1]
        // The variant that names the type exists from Android 13 on.
        @Suppress("DEPRECATION")
        val shared = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        val message = if (text.isBlank() && shared == null) {
            "Nothing to add"
        } else {
            try {
                val image = shared?.let { uri ->
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw java.io.IOException("the image was not read")
                    runBlocking { noteImage(bytes) }
                }
                // ponytail: the push runs on a background thread of a process that the system may stop after this activity;
                // the commit is made before, and the next start of the application pushes it. A foreground service if that shows.
                todoController(GitSetup(filesDir.toPath().resolve("git"))).share(text, action, image)
            } catch (e: Exception) {
                "Not added: ${e.message ?: e}"
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}

/** The image as a note keeps it: scaled down, upright and as JPEG. */
// [impl->dsn~note-images~1]
internal suspend fun noteImage(bytes: ByteArray): ByteArray = FileKit.compressImage(bytes, maxWidth = NOTE_IMAGE_SIZE, maxHeight = NOTE_IMAGE_SIZE)

// [impl->dsn~android-locations~1]
// [impl->dsn~shared-lists~1]
internal fun Context.storageFor(settings: Settings, gitSetup: GitSetup): TodoStorage =
    SharedLists.of(mainStorage(settings, gitSetup), settings.sharedLists) { gitSetup.sharedListStorage(it, settings.committerName, settings.committerEmail) }

private fun Context.mainStorage(settings: Settings, gitSetup: GitSetup): TodoStorage = when {
    settings.todoFile.isBlank() -> gitSetup.storageFor(filesDir.toPath().resolve("todo.txt"), settings.committerName, settings.committerEmail)
    // [impl->dsn~clone-by-address~1]
    isGitAddress(settings.todoFile) -> gitSetup.storageForAddress(settings.todoFile, settings.committerName, settings.committerEmail)
    // A folder comes from the picker as the root of a tree of documents.
    settings.todoFile.startsWith("content:") ->
        PlatformFile(settings.todoFile).let { if (DocumentsContract.isTreeUri(Uri.parse(settings.todoFile))) FolderStorage(it) else DocumentStorage(it) }
    else -> gitSetup.storageFor(java.nio.file.Paths.get(settings.todoFile), settings.committerName, settings.committerEmail)
}

/** Settings of an earlier version name the clone instead of the address of its repository; they get the address, which survives the clone. */
// [impl->dsn~clone-by-address~1]
internal fun Settings.withAddress(gitSetup: GitSetup, file: SettingsFile): Settings =
    gitSetup.withAddresses(this).also { if (it !== this) file.save(it) }

internal fun Context.todoController(gitSetup: GitSetup): TodoController {
    val settingsFile = SettingsFile(filesDir.toPath().resolve("settings.properties"))
    return TodoController(
        settings = settingsFile.load().withAddress(gitSetup, settingsFile),
        storageFor = { storageFor(it, gitSetup) },
        saveSettings = settingsFile::save,
        today = { java.time.LocalDate.now().let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
        minuteOfDay = { java.time.LocalTime.now().toSecondOfDay() / 60 },
        fetch = { address -> withContext(Dispatchers.IO) { fetchCalendar(address) } },
        // [impl->dsn~failure-log~1]
        // The message is passed without arguments, so that tinylog does not read braces in it as placeholders.
        log = { message, cause -> if (cause == null) Logger.error(message as Any) else Logger.error(cause, message) },
    )
}

/**
 * Publishes one shortcut per share action, which the share sheet of Android 10 and later offers as a target of its own.
 * Their number is limited by the system; the first actions win.
 */
// [impl->dsn~android-share-target~1]
internal fun Context.publishShareTargets(actions: List<ShareAction>) {
    val manager = getSystemService(ShortcutManager::class.java) ?: return
    try {
        manager.dynamicShortcuts = actions.distinctBy { it.title }.take(manager.maxShortcutCountPerActivity).map { action ->
            ShortcutInfo.Builder(this, action.title)
                .setShortLabel(action.title)
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                // What the shortcut does when it is chosen in the launcher: it opens the application.
                .setIntent(Intent(Intent.ACTION_MAIN, null, this, MainActivity::class.java))
                .setCategories(setOf("io.github.koppor.koppter.android.category.SHARE"))
                .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setLongLived(true) }
                .build()
        }
    } catch (_: RuntimeException) {
        // The launcher may refuse shortcuts; sharing through the entry of the application still works.
    }
}
