package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.ttyl.prescriptiontracker.data.local.MedicationEntity

@Composable
fun MedicationListScreen(
    medications: List<MedicationEntity>,
    onOpenMedication: (MedicationEntity) -> Unit,
    onAddMedication: () -> Unit,
    onDeleteMedication: (MedicationEntity) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text("< Back")
                }
                Text("Saved Medications")
            }
            TextButton(onClick = onAddMedication) {
                Text("+", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            }
        }

        if (medications.isEmpty()) {
            Text("No medications saved yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(medications, key = { it.id }) { medication ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(medication.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                                Text("Dosage: ${medication.dosage} ${medication.unit.label()}")
                                Text("Frequency: ${medication.frequencyType.name}")
                                Button(onClick = { onOpenMedication(medication) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("View Details")
                                }
                            }
                            IconButton(
                                onClick = { onDeleteMedication(medication) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Text(
                                    "-",
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


