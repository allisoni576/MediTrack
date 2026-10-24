package us.ttyl.prescriptiontracker.data.repository

import kotlinx.coroutines.flow.Flow
import us.ttyl.prescriptiontracker.data.local.MedicationDao
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderType
import us.ttyl.prescriptiontracker.data.usecase.DoseScheduleGenerator
import us.ttyl.prescriptiontracker.data.usecase.RefillCalculator

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val reminderDao: ReminderDao? = null,
) {
    fun observeAll(): Flow<List<MedicationEntity>> = medicationDao.observeAll()

    suspend fun getAllOnce(): List<MedicationEntity> = medicationDao.getAllOnce()

    suspend fun getById(id: Long): MedicationEntity? = medicationDao.getById(id)

    /**
     * Adds a medication and generates its dose-reminder schedule and (optionally) a
     * single estimated refill reminder.
     */
    suspend fun addMedicationWithSchedule(
        name: String,
        totalQuantity: Int,
        dosage: Int,
        unit: MedicationUnit,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        startDate: Long,
        endDate: Long?,
        notes: String,
        timesOfDay: List<String>,
        refillEnabled: Boolean,
    ): Long {
        val timesPerDay = timesOfDay.size.coerceAtLeast(1)
        val medicationId = medicationDao.insert(
            MedicationEntity(
                name = name.trim(),
                totalQuantity = totalQuantity,
                remainingQuantity = totalQuantity,
                dosage = dosage,
                unit = unit,
                frequencyType = frequencyType,
                daysOfWeekMask = daysOfWeekMask,
                timesPerDay = timesPerDay,
                startDate = startDate,
                endDate = endDate,
                notes = notes.trim(),
            )
        )
        val dao = reminderDao ?: return medicationId
        if (timesOfDay.isNotEmpty()) {
            DoseScheduleGenerator(dao, medicationDao).createSchedule(medicationId, timesOfDay)
        }
        if (refillEnabled) {
            insertRefillReminder(
                dao = dao,
                medicationId = medicationId,
                totalQuantity = totalQuantity,
                dosage = dosage,
                timesPerDay = timesPerDay,
                frequencyType = frequencyType,
                daysOfWeekMask = daysOfWeekMask,
                startDate = startDate,
            )
        }
        return medicationId
    }

    /**
     * Updates a medication's core fields and, when the schedule/frequency/refill info changed,
     * regenerates pending dose events and the refill reminder to match the new configuration.
     */
    suspend fun updateMedicationWithSchedule(
        medication: MedicationEntity,
        timesOfDay: List<String>,
        refillEnabled: Boolean,
    ) {
        val timesPerDay = timesOfDay.size.coerceAtLeast(1)
        val updatedMedication = medication.copy(timesPerDay = timesPerDay)
        medicationDao.update(updatedMedication)
        val dao = reminderDao ?: return

        // Clear pending (not-yet-taken) dose events/reminders and regenerate from the new schedule.
        val pendingIds = dao.getPendingDoseEventIds(medication.id)
        if (pendingIds.isNotEmpty()) {
            dao.deleteDoseReminderInstances(pendingIds)
        }
        dao.deletePendingDoseEventsForMedication(medication.id)
        dao.deleteSchedulesForMedication(medication.id)
        if (timesOfDay.isNotEmpty()) {
            DoseScheduleGenerator(dao, medicationDao).createSchedule(medication.id, timesOfDay)
        }

        // Replace the refill reminder with a freshly computed one.
        val existingRefill = dao.getRefillReminderForMedication(medication.id)
        if (existingRefill != null) {
            dao.deleteRefillReminderInstances(listOf(existingRefill.id))
        }
        dao.deleteRefillRemindersForMedication(medication.id)
        if (refillEnabled) {
            insertRefillReminder(
                dao = dao,
                medicationId = medication.id,
                totalQuantity = updatedMedication.totalQuantity,
                dosage = updatedMedication.dosage,
                timesPerDay = timesPerDay,
                frequencyType = updatedMedication.frequencyType,
                daysOfWeekMask = updatedMedication.daysOfWeekMask,
                startDate = updatedMedication.startDate,
            )
        }
    }

    private suspend fun insertRefillReminder(
        dao: ReminderDao,
        medicationId: Long,
        totalQuantity: Int,
        dosage: Int,
        timesPerDay: Int,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        startDate: Long,
    ) {
        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = totalQuantity,
            dosage = dosage,
            timesPerDay = timesPerDay,
            frequencyType = frequencyType,
            daysOfWeekMask = daysOfWeekMask,
            startDate = startDate,
        ) ?: return
        val refillId = dao.insertRefillReminder(
            RefillReminderEntity(
                medicationId = medicationId,
                refillEnabled = true,
                refillDate = refillDate,
            )
        )
        dao.insertReminderInstance(
            ReminderInstanceEntity(
                type = ReminderType.REFILL,
                relatedEntityId = refillId,
                triggerAt = refillDate,
            )
        )
    }

    suspend fun updateMedication(medication: MedicationEntity) {
        medicationDao.update(medication)
    }

    suspend fun deleteMedication(medication: MedicationEntity) {
        medicationDao.delete(medication)
    }

    suspend fun getSchedulesForMedication(medicationId: Long) =
        reminderDao?.getSchedulesForMedication(medicationId) ?: emptyList()

    suspend fun getRefillReminderForMedication(medicationId: Long) =
        reminderDao?.getRefillReminderForMedication(medicationId)
}

