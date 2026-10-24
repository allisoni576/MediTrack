package us.ttyl.prescriptiontracker.data.local.reminder

import androidx.room.TypeConverter

class ReminderTypeConverters {
    @TypeConverter
    fun doseStatusToString(value: DoseStatus): String = value.name

    @TypeConverter
    fun stringToDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)

    @TypeConverter
    fun reminderTypeToString(value: ReminderType): String = value.name

    @TypeConverter
    fun stringToReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun reminderStateToString(value: ReminderState): String = value.name

    @TypeConverter
    fun stringToReminderState(value: String): ReminderState = ReminderState.valueOf(value)

    @TypeConverter
    fun frequencyTypeToString(value: FrequencyType): String = value.name

    @TypeConverter
    fun stringToFrequencyType(value: String): FrequencyType = FrequencyType.valueOf(value)

    @TypeConverter
    fun medicationUnitToString(value: MedicationUnit): String = value.name

    @TypeConverter
    fun stringToMedicationUnit(value: String): MedicationUnit = MedicationUnit.valueOf(value)

    @TypeConverter
    fun longListToString(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun stringToLongList(value: String): List<Long> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}

