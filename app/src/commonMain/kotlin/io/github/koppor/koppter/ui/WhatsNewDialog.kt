package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The changes that a restart brings, as `(heading, bullet)` pairs of `CHANGELOG.md`, under their headings such as *Added*.
 * *Restart* is the restart itself; *Later*, Esc and a click beside the dialog keep the running version.
 */
// [impl->dsn~whats-new~1]
@Composable
fun WhatsNewDialog(news: List<Pair<String, String>>, onRestart: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The count tells whether the list holds more than is in view.
        title = { Text("What's new: ${news.size} " + if (news.size == 1) "change" else "changes", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("whats-new-title")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("whats-new-list")) {
                news.groupBy({ it.first }, { it.second }).forEach { (heading, bullets) ->
                    Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    // The Markdown marks of the changelog would stand as characters in a plain text.
                    bullets.forEach { Text("• " + it.replace("*", "").replace("`", ""), modifier = Modifier.padding(bottom = 4.dp)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onRestart, modifier = Modifier.testTag("whats-new-restart")) { Text("Restart") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("whats-new-later")) { Text("Later") } },
        modifier = Modifier.testTag("whats-new-dialog"),
    )
}
