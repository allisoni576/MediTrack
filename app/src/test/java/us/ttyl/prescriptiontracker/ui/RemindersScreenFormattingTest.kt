package us.ttyl.prescriptiontracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class RemindersScreenFormattingTest {
    @Test
    fun formatReminderTimeUsesExpectedPattern() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val formatted = formatReminderTime(0L)
            assertEquals("1970-01-01 00:00", formatted)
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}

