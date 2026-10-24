package us.ttyl.prescriptiontracker.data.local.reminder

/** How often a medication's schedule repeats. */
enum class FrequencyType {
    DAILY,
    WEEKLY,
}

/** Unit of measure for a medication's dosage. */
enum class MedicationUnit {
    PILLS,
    TABLETS,
    MILLS,
    ;

    /** Human-readable label for display purposes. */
    fun label(): String = when (this) {
        PILLS -> "Pills"
        TABLETS -> "Tablets"
        MILLS -> "mL"
    }
}

enum class DoseStatus {
    PENDING,
    TAKEN,
    MISSED,
    SNOOZED,
}

enum class ReminderType {
    DOSE,
    REFILL,
}

enum class ReminderState {
    SCHEDULED,
    FIRED,
    COMPLETED,
    SNOOZED,
    CANCELLED,
}

