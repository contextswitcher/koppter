package io.github.koppor.koppter.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable

private const val REFRESH_PATH = "/koppter/refresh"
private const val COMPLETE_PATH = "/koppter/complete"

private const val NOT_CONNECTED = "The phone is not connected"

/** Shows the agenda that the phone application sends and asks it to complete a task. */
// [impl->dsn~watch-data-layer~1]
class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    /** `null` until the phone sent an agenda. */
    private var lines by mutableStateOf<List<Line>?>(null)

    /** The tasks whose completion was sent and that the phone still has to drop from the agenda. */
    private var sent by mutableStateOf(emptySet<String>())
    private var problem by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Agenda(lines, sent, problem, ::complete) }
    }

    override fun onResume() {
        super.onResume()
        val data = Wearable.getDataClient(this)
        data.addListener(this)
        // What the phone sent last is shown until it answers the request: blocks may have ended since.
        data.getDataItems(AGENDA_URI).addOnSuccessListener { items ->
            items.forEach(::show)
            items.release()
        }
        send(REFRESH_PATH)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == AGENDA_PATH }.forEach { show(it.dataItem) }
    }

    private fun show(item: DataItem) {
        lines = linesOf(item)
        // The agenda that follows a completion has the task no more.
        sent = emptySet()
    }

    private fun complete(name: String) {
        sent += name
        send(COMPLETE_PATH, name) { sent -= name }
    }

    /** Sends to the phone, which is the only other device that has this application. */
    private fun send(path: String, text: String = "", onFailure: () -> Unit = {}) {
        val failed = {
            problem = NOT_CONNECTED
            onFailure()
        }
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) failed() else problem = null
                nodes.forEach { Wearable.getMessageClient(this).sendMessage(it.id, path, text.toByteArray()).addOnFailureListener { failed() } }
            }
            .addOnFailureListener { failed() }
    }
}

@Composable
private fun Agenda(lines: List<Line>?, sent: Set<String>, problem: String?, onComplete: (String) -> Unit) {
    // The task stays while the dialog closes, so that its text does not vanish before the dialog does.
    var asked by remember { mutableStateOf<Line?>(null) }
    var asking by remember { mutableStateOf(false) }
    MaterialTheme {
        AppScaffold {
            val state = rememberScalingLazyListState()
            ScreenScaffold(scrollState = state) { padding ->
                ScalingLazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = padding) {
                    if (problem != null) item { Text(problem, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                    when {
                        lines == null -> item { Text("No agenda from the phone yet", textAlign = TextAlign.Center) }
                        lines.isEmpty() -> item { Text("No open task", textAlign = TextAlign.Center) }
                    }
                    items(lines.orEmpty()) { line ->
                        if (line.day != null) {
                            ListHeader { Text(line.day) }
                        } else {
                            Button(
                                onClick = {
                                    asked = line
                                    asking = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                // A block whose task was completed, deleted or edited since has no task to complete.
                                enabled = line.name != null && line.name !in sent,
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                secondaryLabel = line.times?.let { times -> { Text(times) } },
                                label = { Text(line.text, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
            // A tap alone does not complete: on a watch, it happens while scrolling.
            AlertDialog(
                visible = asking,
                onDismissRequest = { asking = false },
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(onClick = {
                        asked?.name?.let(onComplete)
                        asking = false
                    })
                },
                title = { Text("Complete?") },
                dismissButton = { AlertDialogDefaults.DismissButton(onClick = { asking = false }) },
                text = { Text(asked?.text.orEmpty(), textAlign = TextAlign.Center) },
            )
        }
    }
}
