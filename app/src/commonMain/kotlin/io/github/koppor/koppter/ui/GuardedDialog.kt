package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A dialog that asks before it closes with changes that are not saved: on Esc, on a click beside it and on the back button.
 * The buttons of the dialog close it themselves, as they say what they do.
 */
// [impl->dsn~unsaved-changes~1]
@Composable
fun GuardedDialog(changed: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    val dismiss = { if (changed) asking = true else onDismiss() }

    // The content states its own width, which may be more than what the platform gives a dialog; the padding keeps it off the edges of a narrow window.
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.padding(horizontal = 16.dp).onPreviewKeyEvent {
                // On the release: the question that the key opens would get the release of the key otherwise and close again.
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) dismiss()
                it.key == Key.Escape
            },
        ) {
            content()
        }
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("Discard the changes?") },
            text = { Text("The changes in this dialog are not saved.") },
            confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("discard-confirm")) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { asking = false }, modifier = Modifier.testTag("discard-keep")) { Text("Keep editing") } },
            modifier = Modifier.testTag("discard-dialog"),
        )
    }
}

/**
 * The heading of a dialog: its title and, at the end, its buttons.
 * The dialog does not scroll the heading, so that the buttons stay in view and no keyboard of a phone covers them.
 */
// [impl->dsn~dialog-header~1]
@Composable
fun DialogHeading(title: String, modifier: Modifier = Modifier, buttons: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        buttons()
    }
}
