package us.ttyl.prescriptiontracker.notifications

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.repository.ReminderRepository

class ReminderActionHandlerTest {
    @Test
    fun markTakenRoutesToCompleted() = runBlocking {
        val dao = TrackingReminderDao().apply {
            seedReminder(
                ReminderInstanceEntity(
                    id = 44L,
                    type = us.ttyl.prescriptiontracker.data.local.reminder.ReminderType.DOSE,
                    relatedEntityId = 1L,
                    triggerAt = 1000L,
                    state = ReminderState.FIRED,
                )
            )
        }
        val repository = ReminderRepository(dao)
        val handler = ReminderActionHandler(repository)

        val handled = handler.handle(ReminderActions.ACTION_MARK_TAKEN, 44L)

        assertTrue(handled.handled)
        assertEquals(ReminderState.COMPLETED, dao.lastState)
        assertEquals(44L, dao.lastReminderId)
    }

    @Test
    fun snoozeRoutesToScheduledReschedule() = runBlocking {
        val dao = TrackingReminderDao().apply {
            seedReminder(
                ReminderInstanceEntity(
                    id = 55L,
                    type = us.ttyl.prescriptiontracker.data.local.reminder.ReminderType.DOSE,
                    relatedEntityId = 1L,
                    triggerAt = 1000L,
                    state = ReminderState.FIRED,
                )
            )
        }
        val repository = ReminderRepository(dao)
        val handler = ReminderActionHandler(repository)

        val handled = handler.handle(ReminderActions.ACTION_SNOOZE, 55L)

        assertTrue(handled.handled)
        assertEquals(ReminderState.SCHEDULED, dao.lastState)
        assertEquals(55L, dao.lastReminderId)
    }

    @Test
    fun invalidActionReturnsFalse() = runBlocking {
        val dao = TrackingReminderDao()
        val repository = ReminderRepository(dao)
        val handler = ReminderActionHandler(repository)

        val handled = handler.handle("invalid.action", 22L)

        assertFalse(handled.handled)
    }

    @Test
    fun invalidIdReturnsFalse() = runBlocking {
        val dao = TrackingReminderDao()
        val repository = ReminderRepository(dao)
        val handler = ReminderActionHandler(repository)

        val handled = handler.handle(ReminderActions.ACTION_MARK_TAKEN, -1L)

        assertFalse(handled.handled)
    }
}

private class TrackingReminderDao : ReminderDao {
    var lastReminderId: Long = -1L
    var lastState: ReminderState? = null
    private val reminders = linkedMapOf<Long, ReminderInstanceEntity>()

    fun seedReminder(reminder: ReminderInstanceEntity) {
        reminders[reminder.id] = reminder
    }

    override suspend fun insertSchedule(schedule: MedicationScheduleEntity): Long = 1L

    override suspend fun insertDoseEvent(event: DoseEventEntity): Long = 1L

    override suspend fun insertRefillReminder(refill: RefillReminderEntity): Long = 1L

    override suspend fun insertReminderInstance(instance: ReminderInstanceEntity): Long {
        reminders[instance.id] = instance
        return instance.id
    }

    override fun observeAll() = throw UnsupportedOperationException()

    override fun observeByState(state: ReminderState) = throw UnsupportedOperationException()

    override suspend fun updateReminderState(id: Long, newState: ReminderState) {
        lastReminderId = id
        lastState = newState
        reminders[id]?.let { reminders[id] = it.copy(state = newState) }
    }

    override suspend fun dueReminders(atMillis: Long, state: ReminderState): List<ReminderInstanceEntity> = emptyList()

    override suspend fun dueScheduledReminders(atMillis: Long): List<ReminderInstanceEntity> = emptyList()

    override suspend fun getAllScheduledReminders(): List<ReminderInstanceEntity> =
        reminders.values.filter { it.state == ReminderState.SCHEDULED }

    override suspend fun getById(id: Long): ReminderInstanceEntity? = reminders[id]

    override suspend fun rescheduleReminder(id: Long, triggerAt: Long, newState: ReminderState) {
        lastReminderId = id
        lastState = newState
        reminders[id]?.let { reminders[id] = it.copy(triggerAt = triggerAt, state = newState) }
    }

    override suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationScheduleEntity> = emptyList()

    override suspend fun deleteSchedulesForMedication(medicationId: Long) {}

    override suspend fun latestDoseEventForSchedule(scheduleId: Long, from: Long): DoseEventEntity? = null

    override fun observeDoseEventsForMedication(medicationId: Long) = throw UnsupportedOperationException()

    override fun observeAllDoseEvents() = throw UnsupportedOperationException()

    override suspend fun getDoseEventsInRange(from: Long, to: Long): List<DoseEventEntity> = emptyList()

    override suspend fun updateDoseEventStatus(id: Long, status: DoseStatus, takenAt: Long?) {}

    override suspend fun markOverdueDosesMissed(cutoff: Long) {}

    override suspend fun getDoseEventById(id: Long): DoseEventEntity? = null

    override suspend fun getRefillReminderForMedication(medicationId: Long): RefillReminderEntity? = null


    override suspend fun getRefillReminderById(id: Long): RefillReminderEntity? = null

    override suspend fun deleteRefillRemindersForMedication(medicationId: Long) {}

    override suspend fun updateReminderNote(id: Long, note: String) {}

    override suspend fun getPendingDoseEventIds(medicationId: Long): List<Long> = emptyList()

    override suspend fun deletePendingDoseEventsForMedication(medicationId: Long) {}

    override suspend fun deleteDoseReminderInstances(doseEventIds: List<Long>) {}

    override suspend fun deleteRefillReminderInstances(refillIds: List<Long>) {}
}
