package us.ttyl.prescriptiontracker.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import us.ttyl.prescriptiontracker.R
import us.ttyl.prescriptiontracker.data.local.AppDatabase
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderType
import us.ttyl.prescriptiontracker.data.repository.ReminderRepository
import us.ttyl.prescriptiontracker.receivers.ReminderActionReceiver

/**
 * Builds and shows a single reminder's notification. Shared by both the periodic
 * WorkManager top-up job and the exact AlarmManager-triggered receiver so a reminder
 * fires precisely on time even if the app process/worker hasn't run recently.
 */
object ReminderNotifier {
    /** Looks up the reminder and shows its notification only if it's still pending. */
    suspend fun notifyIfDue(context: Context, database: AppDatabase, reminderId: Long) {
        ReminderNotificationChannels.ensureChannels(context)
        val reminderRepository = ReminderRepository(database.reminderDao(), database.medicationDao())
        val reminder = reminderRepository.getReminderById(reminderId) ?: return
        // If it was already marked taken/snoozed/cancelled through another path, skip.
        if (reminder.state != ReminderState.SCHEDULED) return

        val doseAndName = reminderRepository.getDoseAndNameForReminder(reminder)
        val prefs = NotificationPreferencesStore(context).preferences()
        show(context, prefs.soundEnabled, prefs.vibrationEnabled, reminder.id, reminder.type, doseAndName)
        reminderRepository.markFired(reminder.id)
    }

    fun show(
        context: Context,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        reminderId: Long,
        type: ReminderType,
        doseAndName: Triple<Int, MedicationUnit, String>? = null,
    ) {
        val label = doseAndName?.third?.takeIf { it.isNotBlank() } ?: "Medication"
        val dose = doseAndName?.first
        val unit = doseAndName?.second
        val (title, channelId) = when (type) {
            ReminderType.DOSE -> "Dose Reminder: $label" to ReminderNotificationChannels.DOSE_CHANNEL_ID
            ReminderType.REFILL -> "Refill Reminder: $label" to ReminderNotificationChannels.REFILL_CHANNEL_ID
        }
        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActions.ACTION_MARK_TAKEN
            putExtra(ReminderActions.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActions.ACTION_SNOOZE
            putExtra(ReminderActions.EXTRA_REMINDER_ID, reminderId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(
                if (dose != null && unit != null) "It's time for $dose ${unit.label()} of $label" else "It's time for $label",
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        when (type) {
            ReminderType.DOSE -> builder
                .addAction(0, "Taken", takenPendingIntent)
                .addAction(0, "Snooze 15m", snoozePendingIntent)
            ReminderType.REFILL -> builder.addAction(0, "Acknowledge", takenPendingIntent)
        }

        if (!soundEnabled) builder.setSilent(true)
        if (!vibrationEnabled) builder.setVibrate(longArrayOf(0L))

        NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
    }
}

