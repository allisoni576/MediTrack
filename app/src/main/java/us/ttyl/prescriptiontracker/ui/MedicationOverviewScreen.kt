package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import java.util.Calendar


/**
 * Screen 6.1 (combined): Today's Medication & Missed Doses Screen.
 *
 * Merges the former standalone Missed Medication History screen and Today's Medication screen
 * into a single screen with three clearly labeled sections, in order:
 *  1. Missed Medications — doses that were scheduled but not taken in time.
 *  2. Taken Medications — doses scheduled for today that have already been marked taken.
 *  3. Pending Medications — doses scheduled for today that are still awaiting action.
 * Each entry is a simple detail card (name, dosage, scheduled time) with a "Mark as Taken"
 * checkbox (disabled/pre-checked for entries already taken). Checkbox state from all sections is
 * held locally and committed (via [onMarkTaken]) only when the user navigates back to the
 * Dashboard.
 */
@Composable
fun MedicationOverviewScreen(
    medications: List<MedicationEntity>,
    doseEvents: List<DoseEventEntity>,
    onMarkTaken: (List<Long>) -> Unit,
    onBack: () -> Unit,
) {

    var selectedMissedMedicationId by remember { mutableStateOf<Long?>(null) }
    var missedMedicationMenuExpanded by remember { mutableStateOf(false) }

    var selectedTakenMedicationId by remember { mutableStateOf<Long?>(null) }
    var takenMedicationMenuExpanded by remember { mutableStateOf(false) }

    var selectedPendingMedicationId by remember { mutableStateOf<Long?>(null) }
    var pendingMedicationMenuExpanded by remember { mutableStateOf(false) }

    var checkedDoseEventIds by remember { mutableStateOf(setOf<Long>()) }

    fun commitAndBack() {
        if (checkedDoseEventIds.isNotEmpty()) {
            onMarkTaken(checkedDoseEventIds.toList())
        }
        onBack()
    }

    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfDay = remember(startOfDay) { startOfDay + 24L * 60 * 60 * 1000 }

    val missedDoses = remember(doseEvents, selectedMissedMedicationId, startOfDay, endOfDay) {
        doseEvents.filter { event ->
            event.status == DoseStatus.MISSED &&
                event.scheduledAt in startOfDay until endOfDay &&
                (selectedMissedMedicationId == null || event.medicationId == selectedMissedMedicationId)
        }.sortedByDescending { it.scheduledAt }
    }

    val takenDosesToday = remember(doseEvents, selectedTakenMedicationId, startOfDay, endOfDay) {
        doseEvents.filter { event ->
            event.scheduledAt in startOfDay until endOfDay &&
                event.status == DoseStatus.TAKEN &&
                (selectedTakenMedicationId == null || event.medicationId == selectedTakenMedicationId)
        }.sortedBy { it.scheduledAt }
    }
    val pendingDosesToday = remember(doseEvents, selectedPendingMedicationId, startOfDay, endOfDay) {
        doseEvents.filter { event ->
            event.scheduledAt in startOfDay until endOfDay &&
                (event.status == DoseStatus.PENDING || event.status == DoseStatus.SNOOZED) &&
                (selectedPendingMedicationId == null || event.medicationId == selectedPendingMedicationId)
        }.sortedBy { it.scheduledAt }
    }

    val medicationNameById = remember(medications) { medications.associateBy({ it.id }, { it.name }) }
    val medicationDosageById = remember(medications) { medications.associateBy({ it.id }, { it.dosage }) }
    val medicationUnitsById = remember(medications) { medications.associateBy({ it.id }, { it.unit.label() }) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { commitAndBack() }) {
                Text("< Back")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Missed Medications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            item {
                MedicationFilterDropdown(
                    expanded = missedMedicationMenuExpanded,
                    onExpandedChange = { missedMedicationMenuExpanded = it },
                    selectedMedicationId = selectedMissedMedicationId,
                    onMedicationSelected = { selectedMissedMedicationId = it },
                    medications = medications,
                    medicationNameById = medicationNameById,
                )
            }
            if (missedDoses.isEmpty()) {
                item { Text("No Medications missed for today.") }
            } else {
                item { MedicationTableHeader() }
                items(missedDoses, key = { "missed-${it.id}" }) { event ->
                    MedicationTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitsById[event.medicationId]}",
                        scheduledAt = event.scheduledAt,
                        checked = checkedDoseEventIds.contains(event.id),
                        enabled = true,
                        onCheckedChange = { checked ->
                            checkedDoseEventIds = if (checked) {
                                checkedDoseEventIds + event.id
                            } else {
                                checkedDoseEventIds - event.id
                            }
                        },
                    )
                }
            }

            item {
                Text(
                    "Taken Medications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            item {
                MedicationFilterDropdown(
                    expanded = takenMedicationMenuExpanded,
                    onExpandedChange = { takenMedicationMenuExpanded = it },
                    selectedMedicationId = selectedTakenMedicationId,
                    onMedicationSelected = { selectedTakenMedicationId = it },
                    medications = medications,
                    medicationNameById = medicationNameById,
                )
            }
            if (takenDosesToday.isEmpty()) {
                item { Text("No Medications taken for today.") }
            } else {
                item { TakenMedicationTableHeader() }
                items(takenDosesToday, key = { "taken-${it.id}" }) { event ->
                    TakenMedicationTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitsById[event.medicationId]}",
                        scheduledAt = event.scheduledAt,
                        takenAt = event.takenAt,
                    )
                }
            }

            item {
                Text(
                    "Pending Medications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            item {
                MedicationFilterDropdown(
                    expanded = pendingMedicationMenuExpanded,
                    onExpandedChange = { pendingMedicationMenuExpanded = it },
                    selectedMedicationId = selectedPendingMedicationId,
                    onMedicationSelected = { selectedPendingMedicationId = it },
                    medications = medications,
                    medicationNameById = medicationNameById,
                )
            }
            if (pendingDosesToday.isEmpty()) {
                item { Text("No Medications pending for today.") }
            } else {
                item { MedicationTableHeader() }
                items(pendingDosesToday, key = { "pending-${it.id}" }) { event ->
                    MedicationTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitsById[event.medicationId]}",
                        scheduledAt = event.scheduledAt,
                        checked = checkedDoseEventIds.contains(event.id),
                        enabled = true,
                        onCheckedChange = { checked ->
                            checkedDoseEventIds = if (checked) {
                                checkedDoseEventIds + event.id
                            } else {
                                checkedDoseEventIds - event.id
                            }
                        },
                    )
                }
            }
        }

        Button(onClick = { commitAndBack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Dashboard")
        }
    }
}

/** Medication-name filter dropdown shared by all three sections. */
@Composable
private fun MedicationFilterDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedMedicationId: Long?,
    onMedicationSelected: (Long?) -> Unit,
    medications: List<MedicationEntity>,
    medicationNameById: Map<Long, String>,
) {
    Column {
        TextButton(onClick = { onExpandedChange(true) }) {
            Text(selectedMedicationId?.let { medicationNameById[it] } ?: "All medications")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("All medications") },
                onClick = { onMedicationSelected(null); onExpandedChange(false) },
            )
            medications.forEach { medication ->
                DropdownMenuItem(
                    text = { Text(medication.name) },
                    onClick = {
                        onMedicationSelected(medication.id)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

/** Column header row for a medication table: Name, Dosage, Date, Taken. */
@Composable
internal fun MedicationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Dosage", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Date", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold)
        Text("Taken", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
    }
}

/** Single data row of a medication table, matching the columns from [MedicationTableHeader]. */
@Composable
internal fun MedicationTableRow(
    medicationName: String,
    dosage: String,
    scheduledAt: Long,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Yellow)
            .border(BorderStroke(1.dp, Color.LightGray))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(medicationName, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(dosage, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(formatReminderTime(scheduledAt), modifier = Modifier.weight(1.3f), fontSize = 14.sp)
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.weight(0.7f),
        )
    }
}

/** Column header row for the Taken table: Name, Dosage, Scheduled, Taken At. */
@Composable
internal fun TakenMedicationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Dosage", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Scheduled", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Taken At", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Single data row for the Taken table, showing both the scheduled time and the actual taken time. */
@Composable
internal fun TakenMedicationTableRow(
    medicationName: String,
    dosage: String,
    scheduledAt: Long,
    takenAt: Long?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Yellow)
            .border(BorderStroke(1.dp, Color.LightGray))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(medicationName, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(dosage, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(formatReminderTime(scheduledAt), modifier = Modifier.weight(1.3f), fontSize = 14.sp)
        Text(takenAt?.let { formatReminderTime(it) } ?: "-", modifier = Modifier.weight(1.3f), fontSize = 14.sp)
    }
}

