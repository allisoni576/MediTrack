package us.ttyl.prescriptiontracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderTypeConverters

@Database(
    entities = [
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        DoseEventEntity::class,
        RefillReminderEntity::class,
        ReminderInstanceEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(ReminderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun reminderDao(): ReminderDao
}
