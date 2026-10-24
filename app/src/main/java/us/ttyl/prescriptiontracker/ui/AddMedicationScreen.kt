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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private const val MAX_TIMES_PER_DAY = 5

/** All valid 30-minute-aligned time strings from 00:00 to 23:30. */
private val ALL_TIME_OPTIONS: List<String> = buildList {
    for (hour in 0..23) {
        for (minute in intArrayOf(0, 30)) {
            add(String.format(Locale.US, "%02d:%02d", hour, minute))
        }
    }
}

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    // DatePickerState.selectedDateMillis is always UTC-midnight for the selected day.
    // Formatting/parsing with UTC here keeps the displayed date in sync with what the
    // user actually picked, regardless of the device's local timezone offset.
    timeZone = java.util.TimeZone.getTimeZone("UTC")
}

/**
 * Returns UTC-midnight millis for "today" (device-local today), matching the convention used
 * by [dateFormatter] and [androidx.compose.material3.DatePickerState.selectedDateMillis].
 *
 * Using `Calendar.getInstance().timeInMillis` directly returns the current instant (including
 * time-of-day). When the DatePicker converts that instant to its own UTC calendar date, a local
 * time late in the day can already correspond to UTC *tomorrow* — causing the picker to open on
 * the wrong (next) day. Formatting today's local date and re-parsing it as UTC-midnight avoids
 * that off-by-one bug.
 */
private fun todayUtcMidnightMillis(): Long {
    val localNow = Calendar.getInstance().time
    val localDateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(localNow)
    return parseDate(localDateText) ?: Calendar.getInstance().timeInMillis
}

/**
 * Dedicated "Add Medication" screen per spec 8.5: name, total quantity, dosage,
 * daily/weekly schedule with up to 5 unique times, optional end date, refill info, notes.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onSave: (
        name: String,
        totalQuantity: Int,
        dosage: Int,
        unit: MedicationUnit,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        startDate: Long,
        endDate: Long?,
        notes: String,
        timesOfDay: List<String>,
        refillEnabled: Boolean,
    ) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var totalQuantityText by remember { mutableStateOf("") }
    var dosageText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(MedicationUnit.PILLS) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var frequencyType by remember { mutableStateOf(FrequencyType.DAILY) }
    val selectedDays = remember { mutableStateListOf<Int>() }
    val selectedTimes = remember { mutableStateListOf<String>() }
    var timeMenuExpanded by remember { mutableStateOf(false) }
    var startDateText by remember {
        mutableStateOf(dateFormatter.format(Calendar.getInstance(java.util.TimeZone.getDefault()).time))
    }
    var endDateText by remember { mutableStateOf("") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var refillEnabled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add Medication")

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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dosageText,
                onValueChange = { dosageText = it.filter { c -> c.isDigit() } },
                label = { Text("Dosage") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(2f),
            )
            Box(modifier = Modifier.weight(2f)) {
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
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = startDateText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Start date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showStartDatePicker = true },
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = endDateText,
                onValueChange = {},
                readOnly = true,
                label = { Text("End date (yyyy-MM-dd, optional — leave blank for ongoing)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showEndDatePicker = true },
            )
        }

        if (showStartDatePicker) {
            val initialMillis = parseDate(startDateText) ?:Calendar.getInstance(java.util.TimeZone.getDefault()).timeInMillis - 24 * 60 * 60 * 1000 // default to yesterday if parsing fails
            val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            startDateText = dateFormatter.format(java.util.Date(millis))
                        }
                        showStartDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) {
                        Text("Cancel")
                    }
                },
            ) {
                DatePicker(state = startDatePickerState)
            }
        }

        if (showEndDatePicker) {
            val initialMillis = parseDate(endDateText) ?: todayUtcMidnightMillis()
            val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            endDateText = dateFormatter.format(java.util.Date(millis))
                        }
                        showEndDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        endDateText = ""
                        showEndDatePicker = false
                    }) {
                        Text("Clear")
                    }
                },
            ) {
                DatePicker(state = endDatePickerState)
            }
        }

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
                DAY_LABELS.forEachIndexed { index, label ->
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

        Text("Reminder times (up to $MAX_TIMES_PER_DAY)")
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
        if (selectedTimes.size < MAX_TIMES_PER_DAY) {
            TextButton(onClick = { timeMenuExpanded = true }) {
                Text("Add time")
            }
            DropdownMenu(expanded = timeMenuExpanded, onDismissRequest = { timeMenuExpanded = false }) {
                ALL_TIME_OPTIONS.filterNot { selectedTimes.contains(it) }.forEach { time ->
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
                    selectedTimes.size > MAX_TIMES_PER_DAY -> error = "Up to $MAX_TIMES_PER_DAY times allowed"
                    selectedTimes.toSet().size != selectedTimes.size -> error = "Reminder times must be unique"
                    else -> {
                        error = ""
                        val daysMask = if (frequencyType == FrequencyType.WEEKLY) {
                            selectedDays.fold(0) { acc, bit -> acc or bit }
                        } else 0
                        onSave(
                            name,
                            totalQuantity,
                            dosage,
                            unit,
                            frequencyType,
                            daysMask,
                            startDate,
                            endDate,
                            notes,
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

internal fun isValidTime(value: String): Boolean {
    val parts = value.split(":")
    if (parts.size != 2) return false
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

internal fun parseDate(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        dateFormatter.parse(value)?.time
    } catch (e: Exception) {
        null
    }
}

internal fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null) return "Ongoing"
    return dateFormatter.format(java.util.Date(epochMillis))
}

