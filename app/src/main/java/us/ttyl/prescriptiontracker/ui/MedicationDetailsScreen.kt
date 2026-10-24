package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.isLowSupply
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.usecase.AdherenceCalculator

/** Filter options for the Dose History section of [MedicationDetailsScreen]. */
private enum class DoseHistoryFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    TAKEN("Taken"),
    PENDING("Pending"),
}

private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
fun MedicationDetailsScreen(
    medication: MedicationEntity,
    doseEvents: List<DoseEventEntity>,
    schedules: List<MedicationScheduleEntity>,
    refillReminder: RefillReminderEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val adherence = remember(doseEvents) { AdherenceCalculator.calculate(doseEvents) }
    val daysOfWeek = remember(medication.daysOfWeekMask) {
        DAY_LABELS.filterIndexed { index, _ -> medication.daysOfWeekMask and (1 shl index) != 0 }
    }

    var doseHistoryFilter by remember { mutableStateOf(DoseHistoryFilter.ALL) }
    var doseHistoryFilterMenuExpanded by remember { mutableStateOf(false) }

    val missedDoses = remember(doseEvents) {
        doseEvents.filter { it.status == DoseStatus.MISSED }.sortedBy { it.scheduledAt }
    }
    val takenDoses = remember(doseEvents) {
        doseEvents.filter { it.status == DoseStatus.TAKEN }.sortedBy { it.scheduledAt }
    }
    val pendingDoses = remember(doseEvents) {
        doseEvents.filter {
            it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED
        }.sortedBy { it.scheduledAt }
    }
    val dosage = "${medication.dosage} ${medication.unit.label()}"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("< Back")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            Text("Medication Details")
        }

        if (medication.isLowSupply()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Low supply warning: remaining quantity may not cover upcoming doses",
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(medication.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Total quantity: ${medication.totalQuantity}")
                    Text("Remaining quantity: ${medication.remainingQuantity}")
                    Text("Dosage: ${medication.dosage} ${medication.unit.label()} per dose")
                    Text("Start date: ${formatDate(medication.startDate)}")
                    Text("End date: ${formatDate(medication.endDate)}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (medication.frequencyType) {
                        FrequencyType.DAILY -> {
                            Text("Schedule: Daily", fontWeight = FontWeight.Bold)
                            Text("Times per day: ${medication.timesPerDay}")
                        }
                        FrequencyType.WEEKLY -> {
                            Text("Schedule: Weekly", fontWeight = FontWeight.Bold)
                            Text("Days: ${daysOfWeek.joinToString().ifEmpty { "None selected" }}")
                            Text("Times per day: ${medication.timesPerDay}")
                        }
                    }
                    Text("Times: ${schedules.joinToString { it.timeOfDay }.ifEmpty { "None" }}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Refill information", fontWeight = FontWeight.Bold)
                    if (refillReminder == null || !refillReminder.refillEnabled) {
                        Text("No refill reminder configured")
                    } else {
                        Text("Refill reminder: Enabled")
                        Text("Estimated refill date: ${formatDate(refillReminder.refillDate)}")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notes", fontWeight = FontWeight.Bold)
                    Text(medication.notes.ifBlank { "No notes" })
                }
            }
        }

        item {
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Medication")
            }
        }

//        item {
//            Card(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                    Text("Adherence")
//                    Text("${adherence.adherencePercent}% taken on time")
//                    Text("Taken: ${adherence.takenDoses}  Missed: ${adherence.missedDoses}  Pending: ${adherence.pendingDoses}")
//                }
//            }
//        }

//        item {
//            Text("Dose History", fontWeight = FontWeight.Bold)
//        }
//        item {
//            Column {
//                TextButton(onClick = { doseHistoryFilterMenuExpanded = true }) {
//                    Text("Filter: ${doseHistoryFilter.label}")
//                }
//                DropdownMenu(
//                    expanded = doseHistoryFilterMenuExpanded,
//                    onDismissRequest = { doseHistoryFilterMenuExpanded = false },
//                ) {
//                    DoseHistoryFilter.entries.forEach { filter ->
//                        DropdownMenuItem(
//                            text = { Text(filter.label) },
//                            onClick = {
//                                doseHistoryFilter = filter
//                                doseHistoryFilterMenuExpanded = false
//                            },
//                        )
//                    }
//                }
//            }
//        }
//
//        if (doseEvents.isEmpty()) {
//            item {
//                Text("No dose history yet")
//            }
//        } else {
//            if (doseHistoryFilter == DoseHistoryFilter.ALL || doseHistoryFilter == DoseHistoryFilter.MISSED) {
//                item {
//                    Text(
//                        "Missed",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp,
//                    )
//                }
//                if (missedDoses.isEmpty()) {
//                    item { Text("No missed doses.") }
//                } else {
//                    item { MedicationTableHeader() }
//                    items(missedDoses, key = { "detail-missed-${it.id}" }) { event ->
//                        MedicationTableRow(
//                            medicationName = medication.name,
//                            dosage = dosage,
//                            scheduledAt = event.scheduledAt,
//                            checked = false,
//                            enabled = false,
//                            onCheckedChange = {},
//                        )
//                    }
//                }
//            }
//
//            if (doseHistoryFilter == DoseHistoryFilter.ALL || doseHistoryFilter == DoseHistoryFilter.TAKEN) {
//                item {
//                    Text(
//                        "Taken",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp,
//                    )
//                }
//                if (takenDoses.isEmpty()) {
//                    item { Text("No taken doses.") }
//                } else {
//                    item { TakenMedicationTableHeader() }
//                    items(takenDoses, key = { "detail-taken-${it.id}" }) { event ->
//                        TakenMedicationTableRow(
//                            medicationName = medication.name,
//                            dosage = dosage,
//                            scheduledAt = event.scheduledAt,
//                            takenAt = event.takenAt,
//                        )
//                    }
//                }
//            }
//
//            if (doseHistoryFilter == DoseHistoryFilter.ALL || doseHistoryFilter == DoseHistoryFilter.PENDING) {
//                item {
//                    Text(
//                        "Pending",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp,
//                    )
//                }
//                if (pendingDoses.isEmpty()) {
//                    item { Text("No pending doses.") }
//                } else {
//                    item { MedicationTableHeader() }
//                    items(pendingDoses, key = { "detail-pending-${it.id}" }) { event ->
//                        MedicationTableRow(
//                            medicationName = medication.name,
//                            dosage = dosage,
//                            scheduledAt = event.scheduledAt,
//                            checked = false,
//                            enabled = false,
//                            onCheckedChange = {},
//                        )
//                    }
//                }
//            }
//        }

//        item {
//            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
//                Text("Delete Medication")
//            }
//        }

//        item {
//            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
//                Text("Back")
//            }
//        }
        }
    }
}

