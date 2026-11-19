package io.github.koppor.koppter.android

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import io.github.koppor.koppter.storage.defaultCommitterEmail
import io.github.koppor.koppter.storage.defaultCommitterName
import io.github.koppor.koppter.storage.logToFile
import org.tinylog.Logger
import java.nio.file.Path

/** The directory of the application in the shared storage, which the user reaches from a computer; without one, the private directory. */
internal fun Context.logDirectory(): Path = (getExternalFilesDir(null) ?: filesDir).toPath().resolve("logs")

/** Sets up the log once per process, before an activity creates anything that logs. */
// [impl->dsn~failure-log~1]
class KoppterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        logToFile(logDirectory())
        // [impl->dsn~committer~1]
        defaultCommitterName = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() } ?: Build.MODEL
        defaultCommitterEmail = "koppter@android${Build.VERSION.RELEASE}"
        // Android ends the application after an exception that no code caught: the handler from before does that.
        val ending = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            Logger.error(e, "The application failed")
            ending?.uncaughtException(thread, e)
        }
    }
}
