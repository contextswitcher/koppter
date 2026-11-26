package io.github.koppor.koppter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MIN_APP_WIDTH = 160.dp
private const val MOST_USED = 5
private val MIN_MOST_USED_WIDTH = 120.dp

/**
 * An application of the device that the user can start.
 *
 * @param id what tells it from the others also after it was renamed, under which its starts are counted
 * @param open throws where the system does not start it
 */
class DeviceApp(val id: String, val name: String, val icon: ImageBitmap? = null, val open: () -> Unit)

/** The number of starts per application, as [io.github.koppor.koppter.storage.Settings.appStarts] holds them. */
internal fun appStarts(setting: String): Map<String, Int> =
    setting.lines().mapNotNull { line -> line.substringAfterLast('=').toIntOrNull()?.let { line.substringBeforeLast('=') to it } }.toMap()

/** [setting] with one more start of the application [id]. */
internal fun withStart(setting: String, id: String): String =
    appStarts(setting).let { it + (id to (it[id] ?: 0) + 1) }.entries.joinToString("\n") { "${it.key}=${it.value}" }

/**
 * The applications of the device: first the five that were started most often from here, then all by their names; a click starts one and closes the dialog.
 * It is the way to them on a device whose home screen is this application.
 *
 * @param starts the starts so far, see [appStarts]
 * @param onStarted called with the [DeviceApp.id] of the application that was started
 */
// [impl->dsn~android-apps~1]
@Composable
fun AppsDialog(read: suspend () -> List<DeviceApp>, starts: String, onStarted: (String) -> Unit, onDismiss: () -> Unit) {
    var apps by remember { mutableStateOf<List<DeviceApp>?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { apps = read().sortedBy { it.name.lowercase() } }
    val counts = remember(starts) { appStarts(starts) }
    // The sort keeps the order of the names for those started equally often.
    val most = apps.orEmpty().filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] }.take(MOST_USED)
    val scroll = rememberScrollState()
    fun start(app: DeviceApp) {
        runCatching(app.open).onSuccess {
            onStarted(app.id)
            onDismiss()
        }.onFailure { failure = "${app.name} was not started: ${it.message ?: it::class.simpleName}" }
    }

    GuardedDialog(changed = false, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 720.dp).testTag("apps-dialog")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The word of the button that opens the dialog.
                DialogHeading("Apps of this device") {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("apps-cancel")) { Text("Close") }
                }
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("apps-failure")) }
                when {
                    apps == null -> Text("Reading the applications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    apps.orEmpty().isEmpty() -> Text("The device has no other application.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // The lists alone scroll, so that the title and Cancel stay in view.
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    // Beside each other as far as the width holds them: a device has more applications than a column shows.
                    val width = maxWidth / (maxWidth / MIN_APP_WIDTH).toInt().coerceAtLeast(1)
                    // The most used ones in one row where the width gives each of them a few letters, so that none stands alone in a second one.
                    val mostWidth = (maxWidth / MOST_USED).takeIf { it >= MIN_MOST_USED_WIDTH } ?: width
                    Column(Modifier.verticalScroll(scroll)) {
                        if (most.isNotEmpty()) {
                            // Bold, as the names below have the size of a heading.
                            Text("Most used", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                            FlowRow { for (app in most) AppCell(app, mostWidth, "app-most") { start(app) } }
                            // All of them, so that an application has its place by its name whether it is used much or not.
                            Text("All", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                        FlowRow { for (app in apps.orEmpty()) AppCell(app, width, "app") { start(app) } }
                    }
                }
                if (scroll.canScrollForward) {
                    Text("More apps below", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("apps-more"))
                }
            }
        }
    }
}

/** An application by its icon, by which it is recognized, and its name. */
@Composable
private fun AppCell(app: DeviceApp, width: Dp, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.width(width).heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 4.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The name beside it says what the icon shows.
        app.icon?.let { Image(it, contentDescription = null, Modifier.size(32.dp)) }
        Text(app.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
