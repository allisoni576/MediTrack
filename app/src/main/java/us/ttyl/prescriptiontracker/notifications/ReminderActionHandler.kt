package us.ttyl.prescriptiontracker.notifications

import us.ttyl.prescriptiontracker.data.repository.ReminderRepository

/** Result of handling a notification action: whether it was handled, and a new alarm trigger time if rescheduled (snooze). */
data class ReminderActionResult(val handled: Boolean, val newTriggerAt: Long? = null)

class ReminderActionHandler(
    private val repository: ReminderRepository,
) {
    suspend fun handle(action: String?, reminderId: Long): ReminderActionResult {
        if (action == null || reminderId <= 0L) return ReminderActionResult(handled = false)

        return when (action) {
            ReminderActions.ACTION_MARK_TAKEN -> {
                repository.markCompleted(reminderId)
                ReminderActionResult(handled = true)
            }

            ReminderActions.ACTION_SNOOZE -> {
                val newTrigger = repository.snoozeReminder(reminderId)
                ReminderActionResult(handled = true, newTriggerAt = newTrigger)
            }

            else -> ReminderActionResult(handled = false)
        }
    }
}

