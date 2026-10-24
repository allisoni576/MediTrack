package us.ttyl.prescriptiontracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Total prescribed supply count (e.g. number of tablets). */
    val totalQuantity: Int = 0,
    /** Supply remaining; auto-decremented by [dosage] whenever a dose is marked taken. */
    val remainingQuantity: Int = 0,
    /** Units taken per dose. Must not exceed [totalQuantity]. */
    val dosage: Int = 1,
    /** Unit of measure for [dosage] (e.g. pills, tablets, mL). */
    val unit: MedicationUnit = MedicationUnit.PILLS,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    /** Bit flags for Sun(1) .. Sat(64); only used when [frequencyType] is WEEKLY. */
    val daysOfWeekMask: Int = 0,
    /** Number of scheduled times per day (1-5). */
    val timesPerDay: Int = 1,
    val startDate: Long = System.currentTimeMillis(),
    /** Null/absent means an ongoing/chronic medication with no end date. */
    val endDate: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A medication is considered low on supply when the remaining quantity can't
 * cover the next 3 days of scheduled doses.
 */
fun MedicationEntity.isLowSupply(): Boolean {
    val dosesPerDay = if (frequencyType == FrequencyType.DAILY) timesPerDay else timesPerDay
    val neededForThreeDays = dosage * dosesPerDay * 3
    return remainingQuantity < neededForThreeDays
}
