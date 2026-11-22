package io.github.koppor.koppter.android

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES = "https://github.com/contextswitcher/koppter/releases"

/** The action of the intent that brings the result of an installer session back to [MainActivity]. */
internal const val INSTALL_STATUS = "io.github.koppor.koppter.INSTALL_STATUS"

/**
 * The tag of the latest release on GitHub, such as `v2026-11-20`; `null` when it cannot be read.
 * GitHub answers `/releases/latest` with a redirect to the page of the tag, which saves the API and its JSON.
 */
// [impl->dsn~android-update~1]
internal fun latestReleaseTag(): String? = runCatching {
    val connection = URL("$RELEASES/latest").openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    connection.requestMethod = "HEAD"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    try {
        connection.getHeaderField("Location")?.substringAfter("/releases/tag/", "")?.ifEmpty { null }
    } finally {
        connection.disconnect()
    }
}.getOrNull()

/**
 * Downloads the APK of the release [tag] into an installer session and commits it.
 * The system asks the user, through [MainActivity], and installs it over the running application, which has the same key.
 */
// [impl->dsn~android-update~1]
internal fun Context.installRelease(tag: String) {
    val installer = packageManager.packageInstaller
    val id = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
    try {
        installer.openSession(id).use { session ->
            URL("$RELEASES/download/$tag/koppter-${tag.removePrefix("v")}.apk").openStream().use { input ->
                session.openWrite("koppter.apk", 0, -1).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            // Mutable: the system adds the status to the intent.
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val status = Intent(this, MainActivity::class.java).setAction(INSTALL_STATUS)
            session.commit(PendingIntent.getActivity(this, 0, status, flags or PendingIntent.FLAG_UPDATE_CURRENT).intentSender)
        }
    } catch (e: Exception) {
        installer.abandonSession(id)
        throw e
    }
}
