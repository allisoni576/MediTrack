package us.ttyl.prescriptiontracker.data.local.reminder

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import us.ttyl.prescriptiontracker.data.local.MedicationEntity

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("medicationId")],
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val timeOfDay: String,
    val daysOfWeekMask: Int = 0,
)

@Entity(
    tableName = "dose_events",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("medicationId"), Index("scheduledAt")],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long? = null,
    val scheduledAt: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    val takenAt: Long? = null,
    val snoozedUntil: Long? = null,
)

@Entity(
    tableName = "refill_reminders",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("medicationId")],
)
data class RefillReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    /** Whether the user has enabled a refill reminder for this medication. */
    val refillEnabled: Boolean = false,
    /**
     * Estimated refill date (epoch millis), computed once from totalQuantity, dosage,
     * schedule, and start date at create/edit time. Not user-editable.
     */
    val refillDate: Long? = null,
)

@Entity(
    tableName = "reminder_instances",
    indices = [Index("triggerAt"), Index("state")],
)
data class ReminderInstanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: ReminderType,
    val relatedEntityId: Long,
    val triggerAt: Long,
    val state: ReminderState = ReminderState.SCHEDULED,
    /** Per-reminder note tied to the specific dose/medication. */
    val note: String = "",
)

