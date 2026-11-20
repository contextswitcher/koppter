package io.github.koppor.koppter.android

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.TodoController
import kotlinx.datetime.toJavaLocalDate
import org.tinylog.Logger

// The paths and keys of `MainActivity.kt` in :wear. The two applications are installed apart, so a released name stays as it is.
private const val AGENDA_PATH = "/koppter/agenda"
private const val COMPLETE_PATH = "/koppter/complete"

// ponytail: the first lines only, which stay far below the 100 kB of an item of the Data Layer; pages if a watch is to show the whole list.
private const val WATCH_LINES = 50

/** Passes the plan on to what shows it outside the application: the alarm of the next block and the watch. */
internal fun Context.publishAgenda(controller: TodoController) {
    scheduleBlockAlarm(controller)
    pushAgendaToWatch(controller)
}

/** Stores the agenda in the Data Layer, which brings it to the watch now or when it connects. */
// [impl->dsn~watch-data-layer~1]
private fun Context.pushAgendaToWatch(controller: TodoController) {
    val request = PutDataMapRequest.create(AGENDA_PATH).apply {
        dataMap.putDataMapArrayList(
            "lines",
            ArrayList(controller.agenda.take(WATCH_LINES).map { line ->
                // The watch shows the texts as they are: the day is formatted here, where the language of the user is known as well.
                DataMap().apply {
                    line.day?.let { putString("day", dateText(it.toJavaLocalDate())) }
                    putString("text", line.text)
                    line.times?.let { putString("times", it) }
                    line.name?.let { putString("name", it) }
                }
            }),
        )
        // The Data Layer tells the watch of a change only: with the time, the watch also learns that a completion left the agenda as it was.
        dataMap.putLong("time", System.currentTimeMillis())
    }.asPutDataRequest().setUrgent()
    try {
        // On a phone without Google Play services or without a watch, the request fails, which changes nothing.
        Wearable.getDataClient(this).putDataItem(request)
    } catch (e: RuntimeException) {
        Logger.warn(e, "The agenda was not passed to the watch")
    }
}

/** Receives what the watch application asks for: the current agenda, or the completion of a task. */
// [impl->dsn~watch-data-layer~1]
class WatchListenerService : WearableListenerService() {
    // Called on a thread of the service, not on the main thread.
    override fun onMessageReceived(event: MessageEvent) {
        try {
            // ponytail: a controller per message, as the share target does; its push may be cut short in the same way.
            val controller = todoController(GitSetup(filesDir.toPath().resolve("git")))
            if (event.path == COMPLETE_PATH) {
                controller.entryNamed(String(event.data))?.let(controller::toggle)
                updateAgendaWidgets()
            }
            publishAgenda(controller)
        } catch (e: Exception) {
            Logger.error(e, "The request of the watch failed")
        }
    }
}
