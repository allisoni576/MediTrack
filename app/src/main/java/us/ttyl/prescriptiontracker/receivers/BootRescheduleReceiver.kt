package us.ttyl.prescriptiontracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import us.ttyl.prescriptiontracker.workers.ReminderSchedulerWorker

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val work = OneTimeWorkRequestBuilder<ReminderSchedulerWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule_reminders_on_time_or_boot",
                ExistingWorkPolicy.REPLACE,
                work,
            )
        }
    }
}

