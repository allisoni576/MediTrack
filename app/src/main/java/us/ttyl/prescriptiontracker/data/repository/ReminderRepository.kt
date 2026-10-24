package us.ttyl.prescriptiontracker.data.repository

import kotlinx.coroutines.flow.Flow
import us.ttyl.prescriptiontracker.data.local.MedicationDao
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderType

/** Fixed snooze duration applied whenever a reminder is snoozed. */
const val DEFAULT_SNOOZE_MINUTES = 15

/**
 * How long a dose may remain PENDING past its scheduled time before it is automatically marked
 * MISSED. Kept short (rather than e.g. an hour) so the Dashboard's "Missed Doses" card and the
 * Missed Medication History screen reflect overdue doses promptly, including in manual/automated
 * test scenarios where a medication's schedule time is set in the past to exercise this flow.
 */
const val MISSED_GRACE_PERIOD_MILLIS = 5 * 60 * 1000L

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val medicationDao: MedicationDao? = null,
) {
    fun observeScheduledReminders(): Flow<List<ReminderInstanceEntity>> {
        return reminderDao.observeByState(ReminderState.SCHEDULED)
    }

    fun observeAllReminders(): Flow<List<ReminderInstanceEntity>> {
        return reminderDao.observeAll()
    }

    fun observeDoseEventsForMedication(medicationId: Long): Flow<List<DoseEventEntity>> {
        return reminderDao.observeDoseEventsForMedication(medicationId)
    }

    fun observeAllDoseEvents(): Flow<List<DoseEventEntity>> {
        return reminderDao.observeAllDoseEvents()
    }

    suspend fun getDoseEventsInRange(from: Long, to: Long): List<DoseEventEntity> {
        return reminderDao.getDoseEventsInRange(from, to)
    }

    suspend fun scheduleDoseReminder(relatedDoseEventId: Long, triggerAt: Long): Long {
        return reminderDao.insertReminderInstance(
            ReminderInstanceEntity(
                type = ReminderType.DOSE,
                relatedEntityId = relatedDoseEventId,
                triggerAt = triggerAt,
                state = ReminderState.SCHEDULED,
            )
        )
    }

    suspend fun scheduleRefillReminder(relatedRefillId: Long, triggerAt: Long): Long {
        return reminderDao.insertReminderInstance(
            ReminderInstanceEntity(
                type = ReminderType.REFILL,
                relatedEntityId = relatedRefillId,
                triggerAt = triggerAt,
                state = ReminderState.SCHEDULED,
            )
        )
    }

    suspend fun markFired(reminderId: Long) {
        reminderDao.updateReminderState(reminderId, ReminderState.FIRED)
    }

    /** Marks a reminder completed and propagates the action to its underlying dose/refill record. */
    suspend fun markCompleted(reminderId: Long) {
        val reminder = reminderDao.getById(reminderId) ?: return
        reminderDao.updateReminderState(reminderId, ReminderState.COMPLETED)
        when (reminder.type) {
            ReminderType.DOSE -> {
                val doseEvent = reminderDao.getDoseEventById(reminder.relatedEntityId)
                reminderDao.updateDoseEventStatus(
                    reminder.relatedEntityId,
                    DoseStatus.TAKEN,
                    System.currentTimeMillis(),
                )
                if (doseEvent != null) {
                    val medication = medicationDao?.getById(doseEvent.medicationId)
                    if (medication != null) {
                        val newRemaining = (medication.remainingQuantity - medication.dosage).coerceAtLeast(0)
                        medicationDao.updateRemainingQuantity(medication.id, newRemaining)
                    }
                }
            }
            ReminderType.REFILL -> {
                // Refill reminders use a fixed set of user-entered dates; no auto-rescheduling.
            }
        }
    }

    suspend fun markSnoozed(reminderId: Long) {
        reminderDao.updateReminderState(reminderId, ReminderState.SNOOZED)
    }

    /**
     * Retroactively marks a dose event (e.g. a previously MISSED dose) as TAKEN directly by its
     * id, decrementing the medication's remaining quantity accordingly. Used by the Missed
     * Medication History screen's "Mark as Taken" checkboxes.
     */
    suspend fun markDoseEventTaken(doseEventId: Long) {
        val doseEvent = reminderDao.getDoseEventById(doseEventId) ?: return
        reminderDao.updateDoseEventStatus(doseEventId, DoseStatus.TAKEN, System.currentTimeMillis())
        val medication = medicationDao?.getById(doseEvent.medicationId)
        if (medication != null) {
            val newRemaining = (medication.remainingQuantity - medication.dosage).coerceAtLeast(0)
            medicationDao.updateRemainingQuantity(medication.id, newRemaining)
        }
    }

    suspend fun markOverdueDosesMissed(atMillis: Long) {
        reminderDao.markOverdueDosesMissed(atMillis - MISSED_GRACE_PERIOD_MILLIS)
    }

    suspend fun getDueScheduledReminders(atMillis: Long): List<ReminderInstanceEntity> {
        return reminderDao.dueScheduledReminders(atMillis)
    }

    /** All reminders still awaiting their trigger time; used to (re)arm exact alarms. */
    suspend fun getAllScheduledReminders(): List<ReminderInstanceEntity> {
        return reminderDao.getAllScheduledReminders()
    }

    suspend fun markBatchFired(reminders: List<ReminderInstanceEntity>) {
        reminders.forEach { reminder ->
            reminderDao.updateReminderState(reminder.id, ReminderState.FIRED)
        }
    }

    suspend fun getReminderById(id: Long): ReminderInstanceEntity? {
        return reminderDao.getById(id)
    }

    /**
     * Resolves the dose quantity, unit, and medication name associated with a reminder instance,
     * regardless of whether it's a DOSE reminder (via its underlying dose event) or a REFILL
     * reminder (via its underlying refill reminder record). Returns null if the reminder, its
     * related record, or the medication itself cannot be found.
     */
    suspend fun getDoseAndNameForReminder(reminder: ReminderInstanceEntity): Triple<Int, us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit, String>? {
        val medicationId = when (reminder.type) {
            ReminderType.DOSE -> reminderDao.getDoseEventById(reminder.relatedEntityId)?.medicationId
            ReminderType.REFILL -> reminderDao.getRefillReminderById(reminder.relatedEntityId)?.medicationId
        } ?: return null
        val medication = medicationDao?.getById(medicationId) ?: return null
        return Triple(medication.dosage, medication.unit, medication.name)
    }

    suspend fun updateNote(reminderId: Long, note: String) {
        reminderDao.updateReminderNote(reminderId, note)
    }

    suspend fun snoozeReminder(reminderId: Long, minutes: Int = DEFAULT_SNOOZE_MINUTES): Long? {
        val reminder = reminderDao.getById(reminderId) ?: return null
        val newTrigger = System.currentTimeMillis() + minutes * 60_000L
        reminderDao.rescheduleReminder(reminder.id, newTrigger, ReminderState.SCHEDULED)
        if (reminder.type == ReminderType.DOSE) {
            reminderDao.updateDoseEventStatus(reminder.relatedEntityId, DoseStatus.SNOOZED, null)
        }
        return newTrigger
    }
}

