package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import java.util.Locale

private val EDIT_DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private const val EDIT_MAX_TIMES_PER_DAY = 5

/** All valid 30-minute-aligned time strings from 00:00 to 23:30. */
private val EDIT_ALL_TIME_OPTIONS: List<String> = buildList {
    for (hour in 0..23) {
        for (minute in intArrayOf(0, 30)) {
            add(String.format(Locale.US, "%02d:%02d", hour, minute))
        }
    }
}

/**
 * Dedicated "Edit Medication" screen, mirroring the Add Medication layout but
 * pre-filled with the existing medication's details. Allows editing of name,
 * quantities, schedule (frequency/days/times), dates, refill info, and notes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditMedicationScreen(
    medication: MedicationEntity,
    schedules: List<MedicationScheduleEntity>,
    refillReminder: RefillReminderEntity?,
    onSave: (
        medication: MedicationEntity,
        timesOfDay: List<String>,
        refillEnabled: Boolean,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(medication.id) { mutableStateOf(medication.name) }
    var totalQuantityText by remember(medication.id) { mutableStateOf(medication.totalQuantity.toString()) }
    var dosageText by remember(medication.id) { mutableStateOf(medication.dosage.toString()) }
    var unit by remember(medication.id) { mutableStateOf(medication.unit) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var notes by remember(medication.id) { mutableStateOf(medication.notes) }
    var frequencyType by remember(medication.id) { mutableStateOf(medication.frequencyType) }
    val selectedDays = remember(medication.id) {
        mutableStateListOf<Int>().apply {
            EDIT_DAY_LABELS.indices.forEach { index ->
                val bit = 1 shl index
                if (medication.daysOfWeekMask and bit != 0) add(bit)
            }
        }
    }
    val selectedTimes = remember(medication.id) {
        mutableStateListOf<String>().apply { addAll(schedules.map { it.timeOfDay }) }
    }
    var timeMenuExpanded by remember { mutableStateOf(false) }
    var startDateText by remember(medication.id) { mutableStateOf(formatDate(medication.startDate)) }
    var endDateText by remember(medication.id) {
        mutableStateOf(if (medication.endDate == null) "" else formatDate(medication.endDate))
    }
    var refillEnabled by remember(medication.id) { mutableStateOf(refillReminder?.refillEnabled ?: false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Edit Medication")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Medication name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = totalQuantityText,
            onValueChange = { totalQuantityText = it.filter { c -> c.isDigit() } },
            label = { Text("Total quantity") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dosageText,
                onValueChange = { dosageText = it.filter { c -> c.isDigit() } },
                label = { Text("Dosage") },
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(3f)) {
                OutlinedTextField(
                    value = unit.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Units") },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparent overlay on top of the (read-only) text field so taps are
                // reliably captured here instead of being consumed by the field itself.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { unitMenuExpanded = true },
                )
                DropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }) {
                    MedicationUnit.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = {
                                unit = option
                                unitMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = startDateText,
            onValueChange = { startDateText = it },
            label = { Text("Start date (yyyy-MM-dd)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = endDateText,
            onValueChange = { endDateText = it },
            label = { Text("End date (yyyy-MM-dd, optional — leave blank for ongoing)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Frequency")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = frequencyType == FrequencyType.DAILY,
                onClick = { frequencyType = FrequencyType.DAILY },
            )
            Text("Daily")
            RadioButton(
                selected = frequencyType == FrequencyType.WEEKLY,
                onClick = { frequencyType = FrequencyType.WEEKLY },
            )
            Text("Weekly")
        }

        if (frequencyType == FrequencyType.WEEKLY) {
            Text("Days of week")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EDIT_DAY_LABELS.forEachIndexed { index, label ->
                    val bit = 1 shl index
                    val checked = selectedDays.contains(bit)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (!selectedDays.contains(bit)) selectedDays.add(bit)
                                } else {
                                    selectedDays.remove(bit)
                                }
                            },
                        )
                        Text(label)
                    }
                }
            }
        }

        Text("Reminder times (up to $EDIT_MAX_TIMES_PER_DAY)")
        selectedTimes.forEachIndexed { index, time ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(time)
                TextButton(onClick = { selectedTimes.removeAt(index) }) {
                    Text("Remove")
                }
            }
        }
        if (selectedTimes.size < EDIT_MAX_TIMES_PER_DAY) {
            TextButton(onClick = { timeMenuExpanded = true }) {
                Text("Add time")
            }
            DropdownMenu(expanded = timeMenuExpanded, onDismissRequest = { timeMenuExpanded = false }) {
                EDIT_ALL_TIME_OPTIONS.filterNot { selectedTimes.contains(it) }.forEach { time ->
                    DropdownMenuItem(
                        text = { Text(time) },
                        onClick = {
                            selectedTimes.add(time)
                            timeMenuExpanded = false
                        },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = refillEnabled,
                onCheckedChange = { refillEnabled = it },
            )
            Text("Enable refill reminder")
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (error.isNotEmpty()) {
            Text(error)
        }

        Button(
            onClick = {
                val totalQuantity = totalQuantityText.toIntOrNull()
                val dosage = dosageText.toIntOrNull()
                val startDate = parseDate(startDateText)
                val endDate = if (endDateText.isBlank()) null else parseDate(endDateText)

                when {
                    name.isBlank() -> error = "Medication name is required"
                    totalQuantity == null || totalQuantity <= 0 -> error = "Total quantity must be a positive number"
                    dosage == null || dosage <= 0 -> error = "Dosage must be a positive number"
                    dosage > totalQuantity -> error = "Dosage cannot exceed total quantity"
                    startDate == null -> error = "Start date is required (yyyy-MM-dd)"
                    endDateText.isNotBlank() && endDate == null -> error = "End date is invalid (yyyy-MM-dd)"
                    frequencyType == FrequencyType.WEEKLY && selectedDays.isEmpty() ->
                        error = "Select at least one day of the week"
                    selectedTimes.isEmpty() -> error = "Add at least one reminder time"
                    selectedTimes.size > EDIT_MAX_TIMES_PER_DAY -> error = "Up to $EDIT_MAX_TIMES_PER_DAY times allowed"
                    selectedTimes.toSet().size != selectedTimes.size -> error = "Reminder times must be unique"
                    else -> {
                        error = ""
                        val daysMask = if (frequencyType == FrequencyType.WEEKLY) {
                            selectedDays.fold(0) { acc, bit -> acc or bit }
                        } else 0
                        val newRemaining = medication.remainingQuantity.coerceAtMost(totalQuantity)
                        val updated = medication.copy(
                            name = name.trim(),
                            totalQuantity = totalQuantity,
                            remainingQuantity = newRemaining,
                            dosage = dosage,
                            unit = unit,
                            frequencyType = frequencyType,
                            daysOfWeekMask = daysMask,
                            timesPerDay = selectedTimes.size,
                            startDate = startDate,
                            endDate = endDate,
                            notes = notes.trim(),
                        )
                        onSave(
                            updated,
                            selectedTimes.toList(),
                            refillEnabled,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

