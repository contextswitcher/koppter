package io.github.koppor.koppter.android

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import io.github.koppor.koppter.plan.CalendarTxt
import io.github.koppor.koppter.sync.GitSetup
import io.github.koppor.koppter.ui.TodoController
import kotlinx.datetime.toJavaLocalDate
import org.tinylog.Logger
import java.time.ZoneId
import kotlin.concurrent.thread

private const val ACTION_START = "io.github.koppor.koppter.android.BLOCK_START"
private const val ACTION_DONE = "io.github.koppor.koppter.android.BLOCK_DONE"
private const val EXTRA_NAME = "name"
private const val CHANNEL = "plan"

// One notification: that of a block replaces that of the block before.
private const val NOTIFICATION = 1

/**
 * Sets the alarm for the start of the next block of the plan, in place of the one set before.
 * One alarm is enough, as [PlanReceiver] sets the following one.
 */
// [impl->dsn~android-block-notification~1]
internal fun Context.scheduleBlockAlarm(controller: TodoController) {
    val alarms = getSystemService(AlarmManager::class.java)
    val alarm = PendingIntent.getBroadcast(this, 0, Intent(this, PlanReceiver::class.java).setAction(ACTION_START), PendingIntent.FLAG_IMMUTABLE)
    val next = CalendarTxt.next(controller.planBlocks.orEmpty(), controller.today(), controller.minuteOfDay())
    if (next == null) {
        alarms.cancel(alarm)
        return
    }
    val start = next.date.toJavaLocalDate().atStartOfDay(ZoneId.systemDefault()).plusMinutes(next.span.start.toLong())
    // ponytail: an inexact alarm, which needs no permission; the system may deliver it some minutes late, more so while the device idles.
    // USE_EXACT_ALARM, which is meant for calendars, if that shows.
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start.toInstant().toEpochMilli(), alarm)
}

/** Shows the block that runs now, if its task is open. */
private fun Context.notifyBlock(controller: TodoController) {
    val minute = controller.minuteOfDay()
    val block = CalendarTxt.current(controller.planBlocks.orEmpty(), controller.today(), minute) ?: return
    // Not for travel time, and not for a task that was completed, deleted or edited since it was planned.
    val entry = controller.entryOf(block) ?: return
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(CHANNEL, "Plan", NotificationManager.IMPORTANCE_DEFAULT))
    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    // The name of the task in the plan, not its line: the file may change until the action is chosen.
    val done = PendingIntent.getBroadcast(
        this, 0, Intent(this, PlanReceiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_NAME, block.text),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val icon = Icon.createWithResource(this, R.drawable.ic_notification)
    manager.notify(
        NOTIFICATION,
        Notification.Builder(this, CHANNEL)
            .setSmallIcon(icon)
            .setContentTitle(entry.task.description)
            .setContentText(CalendarTxt.timesText(block.span))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, "Done", done).build())
            // The notification leaves with its block.
            .setTimeoutAfter((block.span.end - minute) * 60_000L)
            .build(),
    )
}

/**
 * Receives the alarm of a block and shows the block, and completes the task when the user chooses *Done* in the notification.
 * It also runs after the device started and after the application was updated, as the system drops the alarm then.
 */
// [impl->dsn~android-block-notification~1]
class PlanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // Not on the main thread, as the widget does it: the controller reads the file.
        thread {
            try {
                // ponytail: a controller per broadcast, as the share target does; its push may be cut short in the same way.
                val controller = context.todoController(GitSetup(context.filesDir.toPath().resolve("git")))
                if (intent.action == ACTION_DONE) {
                    intent.getStringExtra(EXTRA_NAME)?.let(controller::entryNamed)?.let(controller::toggle)
                    // After a failure, the notification stays for another try.
                    if (controller.error == null) context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION)
                    context.updateAgendaWidgets()
                } else {
                    context.notifyBlock(controller)
                }
                context.publishAgenda(controller)
            } catch (e: Exception) {
                Logger.error(e, "The notification of the plan failed")
            } finally {
                pending.finish()
            }
        }
    }
}
