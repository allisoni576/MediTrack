package us.ttyl.prescriptiontracker.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderType

class ReminderRepositoryTest {
    @Test
    fun marksReminderCompleted() = runBlocking {
        val dao = FakeReminderDao()
        val repository = ReminderRepository(dao)
        val id = dao.insertReminderInstance(
            ReminderInstanceEntity(
                id = 0,
                type = ReminderType.DOSE,
                relatedEntityId = 10,
                triggerAt = System.currentTimeMillis(),
                state = ReminderState.SCHEDULED,
            )
        )

        repository.markCompleted(id)

        val updated = dao.getById(id)
        assertNotNull(updated)
        assertEquals(ReminderState.COMPLETED, updated?.state)
    }

    @Test
    fun snoozeMovesReminderToScheduledFutureTime() = runBlocking {
        val dao = FakeReminderDao()
        val repository = ReminderRepository(dao)
        val now = System.currentTimeMillis()
        val id = dao.insertReminderInstance(
            ReminderInstanceEntity(
                id = 0,
                type = ReminderType.REFILL,
                relatedEntityId = 20,
                triggerAt = now,
                state = ReminderState.FIRED,
            )
        )

        repository.snoozeReminder(id, 10)

        val updated = dao.getById(id)
        assertNotNull(updated)
        assertEquals(ReminderState.SCHEDULED, updated?.state)
        assertTrue((updated?.triggerAt ?: 0L) > now)
    }

    @Test
    fun markDoseEventTakenUpdatesStatusDirectly() = runBlocking {
        val dao = FakeReminderDao()
        val repository = ReminderRepository(dao)
        val doseEventId = dao.insertDoseEvent(
            DoseEventEntity(
                id = 0,
                medicationId = 1,
                scheduledAt = System.currentTimeMillis(),
                status = DoseStatus.MISSED,
            )
        )

        repository.markDoseEventTaken(doseEventId)

        val updated = dao.getDoseEventById(doseEventId)
        assertNotNull(updated)
        assertEquals(DoseStatus.TAKEN, updated?.status)
        assertNotNull(updated?.takenAt)
    }

    @Test
    fun markOverdueDosesMissedUsesFiveMinuteGracePeriod() = runBlocking {
        val dao = FakeReminderDao()
        val repository = ReminderRepository(dao)
        val now = System.currentTimeMillis()

        // 10 minutes overdue -> past the 5 minute grace period -> should become MISSED.
        val overdueId = dao.insertDoseEvent(
            DoseEventEntity(
                id = 0,
                medicationId = 1,
                scheduledAt = now - 10 * 60 * 1000L,
                status = DoseStatus.PENDING,
            )
        )
        // 1 minute overdue -> still within the 5 minute grace period -> should remain PENDING.
        val recentId = dao.insertDoseEvent(
            DoseEventEntity(
                id = 0,
                medicationId = 1,
                scheduledAt = now - 60 * 1000L,
                status = DoseStatus.PENDING,
            )
        )

        repository.markOverdueDosesMissed(now)

        assertEquals(DoseStatus.MISSED, dao.getDoseEventById(overdueId)?.status)
        assertEquals(DoseStatus.PENDING, dao.getDoseEventById(recentId)?.status)
    }
}

private class FakeReminderDao : ReminderDao {
    private val reminders = linkedMapOf<Long, ReminderInstanceEntity>()
    private val doseEvents = linkedMapOf<Long, DoseEventEntity>()
    private var nextId = 1L
    private var nextDoseEventId = 1L

    override suspend fun insertSchedule(schedule: MedicationScheduleEntity): Long = 1L

    override suspend fun insertDoseEvent(event: DoseEventEntity): Long {
        val id = if (event.id == 0L) nextDoseEventId++ else event.id
        doseEvents[id] = event.copy(id = id)
        return id
    }

    override suspend fun insertRefillReminder(refill: RefillReminderEntity): Long = 1L

    override suspend fun insertReminderInstance(instance: ReminderInstanceEntity): Long {
        val id = if (instance.id == 0L) nextId++ else instance.id
        reminders[id] = instance.copy(id = id)
        return id
    }

    override fun observeByState(state: ReminderState) = throw UnsupportedOperationException("Not needed in unit test")

    override fun observeAll() = throw UnsupportedOperationException("Not needed in unit test")

    override suspend fun updateReminderState(id: Long, newState: ReminderState) {
        val existing = reminders[id] ?: return
        reminders[id] = existing.copy(state = newState)
    }

    override suspend fun dueReminders(atMillis: Long, state: ReminderState): List<ReminderInstanceEntity> {
        return reminders.values.filter { it.triggerAt <= atMillis && it.state == state }
    }

    override suspend fun dueScheduledReminders(atMillis: Long): List<ReminderInstanceEntity> {
        return reminders.values.filter { it.triggerAt <= atMillis && it.state == ReminderState.SCHEDULED }
    }

    override suspend fun getAllScheduledReminders(): List<ReminderInstanceEntity> {
        return reminders.values.filter { it.state == ReminderState.SCHEDULED }
    }

    override suspend fun getById(id: Long): ReminderInstanceEntity? = reminders[id]

    override suspend fun rescheduleReminder(id: Long, triggerAt: Long, newState: ReminderState) {
        val existing = reminders[id] ?: return
        reminders[id] = existing.copy(triggerAt = triggerAt, state = newState)
    }

    override suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationScheduleEntity> = emptyList()

    override suspend fun deleteSchedulesForMedication(medicationId: Long) {}

    override suspend fun latestDoseEventForSchedule(scheduleId: Long, from: Long): DoseEventEntity? = null

    override fun observeDoseEventsForMedication(medicationId: Long) =
        throw UnsupportedOperationException("Not needed in unit test")

    override fun observeAllDoseEvents() = throw UnsupportedOperationException("Not needed in unit test")

    override suspend fun getDoseEventsInRange(from: Long, to: Long): List<DoseEventEntity> = emptyList()

    override suspend fun updateDoseEventStatus(id: Long, status: DoseStatus, takenAt: Long?) {
        val existing = doseEvents[id] ?: return
        doseEvents[id] = existing.copy(status = status, takenAt = takenAt)
    }

    override suspend fun markOverdueDosesMissed(cutoff: Long) {
        doseEvents.keys.toList().forEach { id ->
            val existing = doseEvents[id] ?: return@forEach
            if (existing.status == DoseStatus.PENDING && existing.scheduledAt < cutoff) {
                doseEvents[id] = existing.copy(status = DoseStatus.MISSED)
            }
        }
    }

    override suspend fun getDoseEventById(id: Long): DoseEventEntity? = doseEvents[id]

    override suspend fun getRefillReminderForMedication(medicationId: Long): RefillReminderEntity? = null


    override suspend fun getRefillReminderById(id: Long): RefillReminderEntity? = null

    override suspend fun deleteRefillRemindersForMedication(medicationId: Long) {}

    override suspend fun updateReminderNote(id: Long, note: String) {}

    override suspend fun getPendingDoseEventIds(medicationId: Long): List<Long> = emptyList()

    override suspend fun deletePendingDoseEventsForMedication(medicationId: Long) {}

    override suspend fun deleteDoseReminderInstances(doseEventIds: List<Long>) {}

    override suspend fun deleteRefillReminderInstances(refillIds: List<Long>) {}
}
