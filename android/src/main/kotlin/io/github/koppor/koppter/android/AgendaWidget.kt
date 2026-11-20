package io.github.koppor.koppter.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.sync.GitSetup
import kotlinx.datetime.toJavaLocalDate
import java.time.ZoneId

private fun Context.dateText(date: java.time.LocalDate): String = DateUtils.formatDateTime(
    this,
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_ALL,
)

/** Redraws all widgets, after the application changed the file or the plan. */
internal fun Context.updateAgendaWidgets() {
    val manager = AppWidgetManager.getInstance(this)
    manager.notifyAppWidgetViewDataChanged(manager.getAppWidgetIds(ComponentName(this, AgendaWidget::class.java)), R.id.widget_list)
}

/** A widget for the home screen: the plan from now on, by day; without a plan ahead, the list of tasks as the application shows it. */
// [impl->dsn~android-widget~1]
class AgendaWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget).apply {
                setTextViewText(R.id.widget_title, context.dateText(java.time.LocalDate.now()))
                setOnClickPendingIntent(R.id.widget_title, open)
                // The activity passes the link to the controller, which opens the dialog for a new task.
                val add = Intent(Intent.ACTION_VIEW, Uri.parse("koppter://new"), context, MainActivity::class.java)
                setOnClickPendingIntent(R.id.widget_add, PendingIntent.getActivity(context, 1, add, PendingIntent.FLAG_IMMUTABLE))
                setRemoteAdapter(R.id.widget_list, Intent(context, AgendaWidgetService::class.java))
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                // A row of a list takes its click from this template.
                setPendingIntentTemplate(R.id.widget_list, open)
            }
            manager.updateAppWidget(id, views)
        }
        // The periodic update also moves the plan on: blocks that ended drop out.
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }
}

/** A row of the widget: a day, or a task with the times of its block if it has one. */
private data class Row(val day: java.time.LocalDate? = null, val text: String = "", val times: String? = null)

// [impl->dsn~android-widget~1]
class AgendaWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = object : RemoteViewsFactory {
        private var rows = emptyList<Row>()

        // Runs off the main thread, where reading the file is allowed.
        override fun onDataSetChanged() {
            rows = try {
                rows(this@AgendaWidgetService)
            } catch (_: Exception) {
                emptyList()
            }
        }

        override fun getCount() = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val row = rows[position]
            if (row.day != null) {
                return RemoteViews(packageName, R.layout.widget_day).apply { setTextViewText(R.id.widget_day, dateText(row.day)) }
            }
            return RemoteViews(packageName, R.layout.widget_item).apply {
                setTextViewText(R.id.widget_text, row.text)
                setTextViewText(R.id.widget_times, row.times.orEmpty())
                setViewVisibility(R.id.widget_times, if (row.times == null) View.GONE else View.VISIBLE)
                setOnClickFillInIntent(R.id.widget_item, Intent())
            }
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount() = 2

        override fun getItemId(position: Int) = position.toLong()

        override fun hasStableIds() = false

        override fun onCreate() {}

        override fun onDestroy() {}
    }

    private fun rows(context: Context): List<Row> {
        // ponytail: a controller per refresh, as the share target does; it also starts a synchronization of a git repository.
        val controller = context.todoController(GitSetup(filesDir.toPath().resolve("git")))
        val now = java.time.LocalTime.now().toSecondOfDay() / 60
        val ahead = CalendarTxt.ahead(controller.planBlocks.orEmpty(), controller.today(), now)
        if (ahead.isEmpty()) return controller.visible.filterNot { it.task.completed }.map { Row(text = it.task.description) }
        return ahead.groupBy { it.date }.flatMap { (date, blocks) ->
            listOf(Row(day = date.toJavaLocalDate())) +
                blocks.map { Row(text = controller.entryOf(it)?.task?.description ?: it.text, times = CalendarTxt.timesText(it.span)) }
        }
    }
}
