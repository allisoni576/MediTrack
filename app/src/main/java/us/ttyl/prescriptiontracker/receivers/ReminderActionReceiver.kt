package us.ttyl.prescriptiontracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.ttyl.prescriptiontracker.data.local.DatabaseProvider
import us.ttyl.prescriptiontracker.data.repository.ReminderRepository
import us.ttyl.prescriptiontracker.notifications.ReminderActionHandler
import us.ttyl.prescriptiontracker.notifications.ReminderActions
import us.ttyl.prescriptiontracker.workers.ReminderAlarmScheduler

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val reminderId = intent?.getLongExtra(ReminderActions.EXTRA_REMINDER_ID, -1L) ?: -1L
        if (action == null || reminderId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = DatabaseProvider.getInstance(appContext)
                val repository = ReminderRepository(db.reminderDao(), db.medicationDao())
                val handler = ReminderActionHandler(repository)
                val result = handler.handle(action, reminderId)

                if (result.handled) {
                    NotificationManagerCompat.from(appContext).cancel(reminderId.toInt())
                    val newTriggerAt = result.newTriggerAt
                    if (newTriggerAt != null) {
                        ReminderAlarmScheduler.schedule(appContext, reminderId, newTriggerAt)
                    } else {
                        ReminderAlarmScheduler.cancel(appContext, reminderId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
