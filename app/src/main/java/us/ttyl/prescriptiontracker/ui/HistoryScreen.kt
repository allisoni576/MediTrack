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
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus

/** Time-range filter options shared by all three history sections. Defaults to the widest range. */
internal enum class HistoryRange(val label: String, val days: Int) {
    LAST_7("Last 7 days", 7),
    LAST_30("Last 30 days", 30),
    LAST_90("Last 90 days", 90),
}

internal enum class HistoryRangeNext(val label: String, val days: Int) {
    NEXT_7("Next 7 days", 7),
    NEXT_30("Next 30 days", 30),
    NEXT_90("Next 90 days", 90),
}

/**
 * Dose History Screen.
 *
 * Mirrors the look & feel of [MedicationOverviewScreen]: three clearly labeled, independently
 * filterable tables (Missed, Taken, Pending), each with its own medication-name filter and
 * time-range filter (Last 7 / 30 / 90 days). Defaults to showing the last 90 days of history.
 */
@Composable
fun HistoryScreen(
    medications: List<MedicationEntity>,
    doseEvents: List<DoseEventEntity>,
    onMarkTaken: (List<Long>) -> Unit,
    onBack: () -> Unit,
) {
    var selectedMissedMedicationId by remember { mutableStateOf<Long?>(null) }
    var missedMedicationMenuExpanded by remember { mutableStateOf(false) }
    var selectedMissedRange by remember { mutableStateOf(HistoryRange.LAST_90) }
    var missedRangeMenuExpanded by remember { mutableStateOf(false) }

    var selectedTakenMedicationId by remember { mutableStateOf<Long?>(null) }
    var takenMedicationMenuExpanded by remember { mutableStateOf(false) }
    var selectedTakenRange by remember { mutableStateOf(HistoryRange.LAST_90) }
    var takenRangeMenuExpanded by remember { mutableStateOf(false) }

    var selectedPendingMedicationId by remember { mutableStateOf<Long?>(null) }
    var pendingMedicationMenuExpanded by remember { mutableStateOf(false) }
    var selectedPendingRange by remember { mutableStateOf(HistoryRangeNext.NEXT_90) }
    var pendingRangeMenuExpanded by remember { mutableStateOf(false) }

    // Tracks dose events whose "taken" checkbox has been toggled on by the user but not yet
    // committed to the repository. Committed (via [onMarkTaken]) only when the user navigates
    // back to the previous screen.
    var checkedDoseEventIds by remember { mutableStateOf(setOf<Long>()) }

    fun commitAndBack() {
        if (checkedDoseEventIds.isNotEmpty()) {
            onMarkTaken(checkedDoseEventIds.toList())
        }
        onBack()
    }

    val medicationNameById = remember(medications) { medications.associateBy({ it.id }, { it.name }) }
    val medicationDosageById = remember(medications) { medications.associateBy({ it.id }, { it.dosage }) }
    val medicationUnitById = remember(medications) { medications.associateBy({ it.id }, { it.unit.label() }) }

    val missedDoses = remember(doseEvents, selectedMissedMedicationId, selectedMissedRange) {
        val cutoff = System.currentTimeMillis() - selectedMissedRange.days * 24L * 60 * 60 * 1000
        doseEvents.filter { event ->
            event.status == DoseStatus.MISSED &&
                event.scheduledAt >= cutoff &&
                (selectedMissedMedicationId == null || event.medicationId == selectedMissedMedicationId)
        }.sortedByDescending { it.scheduledAt }
    }

    val takenDoses = remember(doseEvents, selectedTakenMedicationId, selectedTakenRange) {
        val cutoff = System.currentTimeMillis() - selectedTakenRange.days * 24L * 60 * 60 * 1000
        doseEvents.filter { event ->
            event.status == DoseStatus.TAKEN &&
                event.scheduledAt >= cutoff &&
                (selectedTakenMedicationId == null || event.medicationId == selectedTakenMedicationId)
        }.sortedByDescending { it.scheduledAt }
    }

    val pendingDoses = remember(doseEvents, selectedPendingMedicationId, selectedPendingRange) {
        val cutoff = System.currentTimeMillis() - selectedPendingRange.days * 24L * 60 * 60 * 1000
        doseEvents.filter { event ->
            (event.status == DoseStatus.PENDING || event.status == DoseStatus.SNOOZED) &&
                event.scheduledAt >= cutoff &&
                (selectedPendingMedicationId == null || event.medicationId == selectedPendingMedicationId)
        }.sortedByDescending { it.scheduledAt }
    }

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Missed Medications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                HistoryFilterRow(
                    medications = medications,
                    medicationNameById = medicationNameById,
                    selectedMedicationId = selectedMissedMedicationId,
                    onMedicationSelected = { selectedMissedMedicationId = it },
                    medicationMenuExpanded = missedMedicationMenuExpanded,
                    onMedicationMenuExpandedChange = { missedMedicationMenuExpanded = it },
                    selectedRange = selectedMissedRange,
                    rangeOptions = HistoryRange.entries,
                    rangeLabel = { it.label },
                    onRangeSelected = { selectedMissedRange = it },
                    rangeMenuExpanded = missedRangeMenuExpanded,
                    onRangeMenuExpandedChange = { missedRangeMenuExpanded = it },
                )
            }
            if (missedDoses.isEmpty()) {
                item { Text("No missed medications in this range.") }
            } else {
                item { HistoryTableHeader() }
                items(missedDoses, key = { "missed-${it.id}" }) { event ->
                    HistoryTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitById[event.medicationId]}",
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                HistoryFilterRow(
                    medications = medications,
                    medicationNameById = medicationNameById,
                    selectedMedicationId = selectedTakenMedicationId,
                    onMedicationSelected = { selectedTakenMedicationId = it },
                    medicationMenuExpanded = takenMedicationMenuExpanded,
                    onMedicationMenuExpandedChange = { takenMedicationMenuExpanded = it },
                    selectedRange = selectedTakenRange,
                    rangeOptions = HistoryRange.entries,
                    rangeLabel = { it.label },
                    onRangeSelected = { selectedTakenRange = it },
                    rangeMenuExpanded = takenRangeMenuExpanded,
                    onRangeMenuExpandedChange = { takenRangeMenuExpanded = it },
                )
            }
            if (takenDoses.isEmpty()) {
                item { Text("No taken medications in this range.") }
            } else {
                item { HistoryTableHeader() }
                items(takenDoses, key = { "taken-${it.id}" }) { event ->
                    HistoryTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitById[event.medicationId]}",
                        scheduledAt = event.scheduledAt,
                        checked = true,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
            }

            item {
                Text(
                    "Pending Medications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                HistoryFilterRow(
                    medications = medications,
                    medicationNameById = medicationNameById,
                    selectedMedicationId = selectedPendingMedicationId,
                    onMedicationSelected = { selectedPendingMedicationId = it },
                    medicationMenuExpanded = pendingMedicationMenuExpanded,
                    onMedicationMenuExpandedChange = { pendingMedicationMenuExpanded = it },
                    selectedRange = selectedPendingRange,
                    rangeOptions = HistoryRangeNext.entries,
                    rangeLabel = { it.label },
                    onRangeSelected = { selectedPendingRange = it },
                    rangeMenuExpanded = pendingRangeMenuExpanded,
                    onRangeMenuExpandedChange = { pendingRangeMenuExpanded = it },
                )
            }
            if (pendingDoses.isEmpty()) {
                item { Text("No pending medications in this range.") }
            } else {
                item { HistoryTableHeader() }
                items(pendingDoses, key = { "pending-${it.id}" }) { event ->
                    HistoryTableRow(
                        medicationName = medicationNameById[event.medicationId] ?: "Unknown medication",
                        dosage = "${medicationDosageById[event.medicationId]} ${medicationUnitById[event.medicationId]}",
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

/** Combined medication-name + time-range filter row shared by all three history sections. */
@Composable
private fun <T> HistoryFilterRow(
    medications: List<MedicationEntity>,
    medicationNameById: Map<Long, String>,
    selectedMedicationId: Long?,
    onMedicationSelected: (Long?) -> Unit,
    medicationMenuExpanded: Boolean,
    onMedicationMenuExpandedChange: (Boolean) -> Unit,
    selectedRange: T,
    rangeOptions: List<T>,
    rangeLabel: (T) -> String,
    onRangeSelected: (T) -> Unit,
    rangeMenuExpanded: Boolean,
    onRangeMenuExpandedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            TextButton(onClick = { onMedicationMenuExpandedChange(true) }) {
                Text(selectedMedicationId?.let { medicationNameById[it] } ?: "All medications")
            }
            DropdownMenu(
                expanded = medicationMenuExpanded,
                onDismissRequest = { onMedicationMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text("All medications") },
                    onClick = {
                        onMedicationSelected(null)
                        onMedicationMenuExpandedChange(false)
                    },
                )
                medications.forEach { medication ->
                    DropdownMenuItem(
                        text = { Text(medication.name) },
                        onClick = {
                            onMedicationSelected(medication.id)
                            onMedicationMenuExpandedChange(false)
                        },
                    )
                }
            }
        }
        Column {
            TextButton(onClick = { onRangeMenuExpandedChange(true) }) {
                Text(rangeLabel(selectedRange))
            }
            DropdownMenu(
                expanded = rangeMenuExpanded,
                onDismissRequest = { onRangeMenuExpandedChange(false) },
            ) {
                rangeOptions.forEach { range ->
                    DropdownMenuItem(
                        text = { Text(rangeLabel(range)) },
                        onClick = {
                            onRangeSelected(range)
                            onRangeMenuExpandedChange(false)
                        },
                    )
                }
            }
        }
    }
}

/** Column header row for a history table: Name, Dosage, Date, Status. */
@Composable
private fun HistoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Yellow)
            .border(BorderStroke(1.dp, Color.Gray))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Dosage", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Date", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold)
        Text("Status", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
    }
}

/** Single data row of a history table, matching the columns from [HistoryTableHeader]. */
@Composable
private fun HistoryTableRow(
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
        Text(medicationName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text(dosage, modifier = Modifier.weight(1f))
        Text(formatReminderTime(scheduledAt), modifier = Modifier.weight(1.3f))
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.weight(0.7f),
        )
    }
}


