package us.ttyl.prescriptiontracker.data.local.reminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insertSchedule(schedule: MedicationScheduleEntity): Long

    @Insert
    suspend fun insertDoseEvent(event: DoseEventEntity): Long

    @Insert
    suspend fun insertRefillReminder(refill: RefillReminderEntity): Long

    @Insert
    suspend fun insertReminderInstance(instance: ReminderInstanceEntity): Long

    @Query("SELECT * FROM reminder_instances WHERE state = :state ORDER BY triggerAt ASC")
    fun observeByState(state: ReminderState = ReminderState.SCHEDULED): Flow<List<ReminderInstanceEntity>>

    @Query("SELECT * FROM reminder_instances ORDER BY triggerAt DESC")
    fun observeAll(): Flow<List<ReminderInstanceEntity>>

    @Query("UPDATE reminder_instances SET state = :newState WHERE id = :id")
    suspend fun updateReminderState(id: Long, newState: ReminderState)

    @Query("SELECT * FROM reminder_instances WHERE triggerAt <= :atMillis AND state = :state")
    suspend fun dueReminders(
        atMillis: Long,
        state: ReminderState = ReminderState.SCHEDULED,
    ): List<ReminderInstanceEntity>

    @Query("SELECT * FROM reminder_instances WHERE triggerAt <= :atMillis AND state = 'SCHEDULED' ORDER BY triggerAt ASC")
    suspend fun dueScheduledReminders(atMillis: Long): List<ReminderInstanceEntity>

    @Query("SELECT * FROM reminder_instances WHERE state = 'SCHEDULED' ORDER BY triggerAt ASC")
    suspend fun getAllScheduledReminders(): List<ReminderInstanceEntity>

    @Query("SELECT * FROM reminder_instances WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderInstanceEntity?

    @Query("UPDATE reminder_instances SET triggerAt = :triggerAt, state = :newState WHERE id = :id")
    suspend fun rescheduleReminder(id: Long, triggerAt: Long, newState: ReminderState)

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationScheduleEntity>

    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: Long)

    @Query(
        "SELECT * FROM dose_events WHERE scheduleId = :scheduleId AND scheduledAt >= :from ORDER BY scheduledAt DESC LIMIT 1"
    )
    suspend fun latestDoseEventForSchedule(scheduleId: Long, from: Long): DoseEventEntity?

    @Query("SELECT * FROM dose_events WHERE medicationId = :medicationId ORDER BY scheduledAt DESC")
    fun observeDoseEventsForMedication(medicationId: Long): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events ORDER BY scheduledAt DESC")
    fun observeAllDoseEvents(): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE scheduledAt >= :from AND scheduledAt <= :to ORDER BY scheduledAt ASC")
    suspend fun getDoseEventsInRange(from: Long, to: Long): List<DoseEventEntity>

    @Query("UPDATE dose_events SET status = :status, takenAt = :takenAt WHERE id = :id")
    suspend fun updateDoseEventStatus(id: Long, status: DoseStatus, takenAt: Long?)

    @Query("UPDATE dose_events SET status = 'MISSED' WHERE status = 'PENDING' AND scheduledAt < :cutoff")
    suspend fun markOverdueDosesMissed(cutoff: Long)

    @Query("SELECT * FROM dose_events WHERE id = :id LIMIT 1")
    suspend fun getDoseEventById(id: Long): DoseEventEntity?

    @Query("SELECT * FROM refill_reminders WHERE medicationId = :medicationId LIMIT 1")
    suspend fun getRefillReminderForMedication(medicationId: Long): RefillReminderEntity?


    @Query("SELECT * FROM refill_reminders WHERE id = :id LIMIT 1")
    suspend fun getRefillReminderById(id: Long): RefillReminderEntity?

    @Query("DELETE FROM refill_reminders WHERE medicationId = :medicationId")
    suspend fun deleteRefillRemindersForMedication(medicationId: Long)

    @Query("UPDATE reminder_instances SET note = :note WHERE id = :id")
    suspend fun updateReminderNote(id: Long, note: String)

    @Query("SELECT id FROM dose_events WHERE medicationId = :medicationId AND status = 'PENDING'")
    suspend fun getPendingDoseEventIds(medicationId: Long): List<Long>

    @Query("DELETE FROM dose_events WHERE medicationId = :medicationId AND status = 'PENDING'")
    suspend fun deletePendingDoseEventsForMedication(medicationId: Long)

    @Query("DELETE FROM reminder_instances WHERE type = 'DOSE' AND relatedEntityId IN (:doseEventIds)")
    suspend fun deleteDoseReminderInstances(doseEventIds: List<Long>)

    @Query("DELETE FROM reminder_instances WHERE type = 'REFILL' AND relatedEntityId IN (:refillIds)")
    suspend fun deleteRefillReminderInstances(refillIds: List<Long>)
}
