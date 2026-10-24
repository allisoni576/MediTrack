package us.ttyl.prescriptiontracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReminderNotificationChannels {
    const val DOSE_CHANNEL_ID = "dose_reminders"
    const val REFILL_CHANNEL_ID = "refill_reminders"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val doseChannel = NotificationChannel(
            DOSE_CHANNEL_ID,
            "Dose Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for scheduled medication doses"
        }

        val refillChannel = NotificationChannel(
            REFILL_CHANNEL_ID,
            "Refill Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications for medication refills"
        }

        manager.createNotificationChannel(doseChannel)
        manager.createNotificationChannel(refillChannel)
    }
}

