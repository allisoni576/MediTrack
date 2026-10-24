package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import java.util.Calendar

@Composable
fun DashboardScreen(
    username: String,
    medications: List<MedicationEntity>,
    doseEvents: List<DoseEventEntity>,
    onOpenMedication: (MedicationEntity) -> Unit,
    onOpenMedicationList: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMedicationOverview: () -> Unit,
    onOpenSettings: () -> Unit,
    onLock: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val todaysDoseEvents = remember(doseEvents, now) {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24L * 60 * 60 * 1000
        doseEvents.filter { it.scheduledAt in startOfDay until endOfDay }
    }
    // Counts reflect distinct medications (not raw dose-event rows), so a medication scheduled
    // multiple times today only contributes once to whichever status bucket(s) it falls into.
    val takenTodayCount = remember(todaysDoseEvents) {
        todaysDoseEvents.filter { it.status == DoseStatus.TAKEN }
            .distinctBy { it.medicationId }
            .size
    }
    val missedTodayCount = remember(todaysDoseEvents) {
        todaysDoseEvents.filter { it.status == DoseStatus.MISSED }
            .distinctBy { it.medicationId }
            .size
    }
    val pendingTodayCount = remember(todaysDoseEvents) {
        todaysDoseEvents.filter { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }
            .distinctBy { it.medicationId }
            .size
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
//        Text("Dashboard")
        Text("Hello, $username",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMedicationOverview() },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "📋 Today's Medications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$takenTodayCount Taken · $missedTodayCount Missed · $pendingTodayCount Pending",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("Today — tap for details")
            }
        }

        Button(onClick = onOpenMedicationList, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Medications")
        }

//        Button(onClick = onOpenReminders, modifier = Modifier.fillMaxWidth()) {
//            Text("View Reminders")
//        }

        Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
            Text("View History")
        }

        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }

        Button(onClick = onLock, modifier = Modifier.fillMaxWidth()) {
            Text("Lock")
        }
    }
}
