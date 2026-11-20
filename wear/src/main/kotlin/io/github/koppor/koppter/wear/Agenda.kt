package io.github.koppor.koppter.wear

import android.net.Uri
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem

// The path and keys of `Watch.kt` in :android. The two applications are installed apart, so a released name stays as it is.
internal const val AGENDA_PATH = "/koppter/agenda"

/** The agenda of whichever device stored one: the phone is the only one that does. */
internal val AGENDA_URI: Uri = Uri.parse("wear://*$AGENDA_PATH")

/** A line as the phone sends it: a day, or a task with the times of its block if it has one; [name] is what the phone completes the task by. */
internal data class Line(val day: String?, val text: String, val times: String?, val name: String?)

// [impl->dsn~watch-data-layer~1]
internal fun linesOf(item: DataItem): List<Line> = DataMapItem.fromDataItem(item).dataMap.getDataMapArrayList("lines").orEmpty().map {
    Line(it.getString("day"), it.getString("text").orEmpty(), it.getString("times"), it.getString("name"))
}
