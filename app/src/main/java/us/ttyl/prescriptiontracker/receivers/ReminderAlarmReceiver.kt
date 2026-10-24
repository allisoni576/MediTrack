package us.ttyl.prescriptiontracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.ttyl.prescriptiontracker.data.local.DatabaseProvider
import us.ttyl.prescriptiontracker.notifications.ReminderActions
import us.ttyl.prescriptiontracker.notifications.ReminderNotifier

/**
 * Fires when an exact AlarmManager alarm triggers for a single reminder instance,
 * showing its notification immediately regardless of whether the periodic
 * WorkManager job has run recently.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderActions.EXTRA_REMINDER_ID, -1L) ?: -1L
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = DatabaseProvider.getInstance(appContext)
                ReminderNotifier.notifyIfDue(appContext, database, reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

