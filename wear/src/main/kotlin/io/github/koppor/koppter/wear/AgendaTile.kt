package io.github.koppor.koppter.wear

import android.content.ComponentName
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.common.util.concurrent.ListenableFuture

// As many tasks as a tile holds with a text of two lines each.
private const val TILE_TASKS = 2

/** A tile with the next tasks of the agenda that the phone sent last; a tap opens the application. */
// [impl->dsn~watch-tile~1]
class AgendaTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            Wearable.getDataClient(this).getDataItems(AGENDA_URI).addOnCompleteListener { task ->
                // Without an answer of the Data Layer, the tile says the same as without an agenda.
                val lines = if (task.isSuccessful) task.result.flatMap(::linesOf).also { task.result.release() } else emptyList()
                completer.set(
                    TileBuilders.Tile.Builder()
                        .setResourcesVersion("1")
                        .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(lines, requestParams.deviceConfiguration)))
                        .build(),
                )
            }
            "agenda"
        }

    // The tile shows texts only.
    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { completer ->
            completer.set(ResourceBuilders.Resources.Builder().setVersion("1").build())
            "resources"
        }

    private fun layout(lines: List<Line>, device: DeviceParameters): LayoutElementBuilders.LayoutElement =
        materialScope(this, device) {
            val tasks = lines.filter { it.day == null }.take(TILE_TASKS)
            primaryLayout(
                titleSlot = { text((lines.firstOrNull()?.day ?: "koppter").layoutString) },
                mainSlot = {
                    // As wide as the slot, so that a long text breaks into its two lines.
                    LayoutElementBuilders.Column.Builder().setWidth(DimensionBuilders.expand()).apply {
                        if (tasks.isEmpty()) addContent(text("No open task".layoutString))
                        for (task in tasks) {
                            task.times?.let { addContent(text(it.layoutString, typography = Typography.LABEL_SMALL)) }
                            addContent(text(task.text.layoutString, typography = Typography.BODY_MEDIUM, maxLines = 2))
                        }
                    }.build()
                },
                onClick = clickable(
                    ActionBuilders.launchAction(ComponentName(this@AgendaTileService, MainActivity::class.java)),
                ),
            )
        }
}

/** Has the tile drawn again when the phone stored an agenda, which it does when a block starts as well. */
// [impl->dsn~watch-tile~1]
class AgendaListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        TileService.getUpdater(this).requestUpdate(AgendaTileService::class.java)
    }
}
