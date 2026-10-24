package us.ttyl.prescriptiontracker.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import us.ttyl.prescriptiontracker.data.local.DatabaseProvider
import us.ttyl.prescriptiontracker.data.repository.MedicationRepository
import us.ttyl.prescriptiontracker.data.repository.ReminderRepository
import us.ttyl.prescriptiontracker.data.usecase.DoseScheduleGenerator
import us.ttyl.prescriptiontracker.notifications.NotificationPreferencesStore
import us.ttyl.prescriptiontracker.notifications.ReminderNotificationChannels
import us.ttyl.prescriptiontracker.notifications.ReminderNotifier

class ReminderSchedulerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        ReminderNotificationChannels.ensureChannels(applicationContext)

        val database = DatabaseProvider.getInstance(applicationContext)
        val reminderRepository = ReminderRepository(database.reminderDao(), database.medicationDao())
        val medicationRepository = MedicationRepository(database.medicationDao())
        val prefs = NotificationPreferencesStore(applicationContext).preferences()

        // Top up the rolling schedule window and expire stale pending doses.
        val generator = DoseScheduleGenerator(database.reminderDao(), database.medicationDao())
        val medications = medicationRepository.getAllOnce()
        medications.forEach { medication ->
            generator.extendSchedule(medication.id)
        }
        reminderRepository.markOverdueDosesMissed(System.currentTimeMillis())

        val due = reminderRepository.getDueScheduledReminders(System.currentTimeMillis())
        due.forEach { reminder ->
            val doseAndName = reminderRepository.getDoseAndNameForReminder(reminder)
            ReminderNotifier.show(
                applicationContext,
                prefs.soundEnabled,
                prefs.vibrationEnabled,
                reminder.id,
                reminder.type,
                doseAndName,
            )
            ReminderAlarmScheduler.cancel(applicationContext, reminder.id)
        }
        reminderRepository.markBatchFired(due)

        // Re-arm exact alarms for every reminder still pending, so precise timing is
        // restored after a reboot (which clears AlarmManager alarms) or if any alarm
        // was otherwise missed while the app/process was not running.
        val stillScheduled = reminderRepository.getAllScheduledReminders()
        stillScheduled.forEach { reminder ->
            ReminderAlarmScheduler.schedule(applicationContext, reminder.id, reminder.triggerAt)
        }

        return Result.success()
    }
}


