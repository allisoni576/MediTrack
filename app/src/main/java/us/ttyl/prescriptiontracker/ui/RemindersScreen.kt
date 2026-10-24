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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.usecase.AdherenceCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    reminders: List<ReminderInstanceEntity>,
    doseEvents: List<DoseEventEntity>,
    onMarkTaken: (ReminderInstanceEntity) -> Unit,
    onSnooze: (ReminderInstanceEntity) -> Unit,
    onNoteChanged: (ReminderInstanceEntity, String) -> Unit,
    onBack: () -> Unit,
) {
    val todayAdherence = remember(doseEvents) { AdherenceCalculator.calculateForToday(doseEvents) }
    val sortedReminders = remember(reminders) { reminders.sortedBy { it.triggerAt } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Dashboard")
        }

        Text("Reminders")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today's Adherence")
                Text("${todayAdherence.adherencePercent}% taken on time")
                Text("Taken: ${todayAdherence.takenDoses}  Missed: ${todayAdherence.missedDoses}  Pending: ${todayAdherence.pendingDoses}")
            }
        }

        if (sortedReminders.isEmpty()) {
            Text("No reminders yet")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedReminders, key = { it.id }) { reminder ->
                    var noteText by remember(reminder.id) { mutableStateOf(reminder.note) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Type: ${reminder.type.name}")
                            Text("State: ${reminder.state.name}")
                            Text("Trigger: ${formatReminderTime(reminder.triggerAt)}")

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = {
                                    noteText = it
                                    onNoteChanged(reminder, it)
                                },
                                label = { Text("Note") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (reminder.state == ReminderState.SCHEDULED || reminder.state == ReminderState.FIRED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(onClick = { onMarkTaken(reminder) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Mark Taken")
                                    }
                                }
//                                Button(onClick = { onSnooze(reminder) }, modifier = Modifier.fillMaxWidth()) {
//                                    Text("Snooze 15m")
//                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatReminderTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return formatter.format(Date(epochMillis))
}

