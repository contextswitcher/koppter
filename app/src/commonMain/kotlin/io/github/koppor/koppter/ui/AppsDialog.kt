package io.github.koppor.koppter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val MIN_APP_WIDTH = 160.dp

/** An application of the device that the user can start; [open] throws where the system does not start it. */
class DeviceApp(val name: String, val open: () -> Unit)

/**
 * The applications of the device by their names; a click starts one and closes the dialog.
 * It is the way to them on a device whose home screen is this application.
 */
// [impl->dsn~android-apps~1]
@Composable
fun AppsDialog(read: suspend () -> List<DeviceApp>, onDismiss: () -> Unit) {
    var apps by remember { mutableStateOf<List<DeviceApp>?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { apps = read().sortedBy { it.name.lowercase() } }
    val scroll = rememberScrollState()

    GuardedDialog(changed = false, onDismiss = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, modifier = Modifier.widthIn(max = 720.dp).testTag("apps-dialog")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The word of the button that opens the dialog.
                Text("Apps of this device", style = MaterialTheme.typography.titleLarge)
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("apps-failure")) }
                when {
                    apps == null -> Text("Reading the applications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    apps.orEmpty().isEmpty() -> Text("The device has no other application.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // The list alone scrolls, so that the title and Cancel stay in view.
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    // Beside each other as far as the width holds them: a device has more applications than a column shows.
                    val width = maxWidth / (maxWidth / MIN_APP_WIDTH).toInt().coerceAtLeast(1)
                    FlowRow(Modifier.verticalScroll(scroll)) {
                        for (app in apps.orEmpty()) {
                            Box(
                                Modifier.width(width).heightIn(min = 48.dp).clickable {
                                    runCatching(app.open).onSuccess { onDismiss() }.onFailure { failure = "${app.name} was not started: ${it.message ?: it::class.simpleName}" }
                                }.padding(horizontal = 4.dp).testTag("app"),
                                contentAlignment = Alignment.CenterStart,
                            ) { Text(app.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                if (scroll.canScrollForward) {
                    Text("More apps below", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("apps-more"))
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("apps-cancel")) { Text("Cancel") }
                }
            }
        }
    }
}
