package us.ttyl.prescriptiontracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationUnit
import us.ttyl.prescriptiontracker.data.repository.MedicationRepository

class MedicationViewModel(
    private val repository: MedicationRepository,
) : ViewModel() {
    val medications: StateFlow<List<MedicationEntity>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun addMedicationWithSchedule(
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
    ) {
        if (name.isBlank() || timesOfDay.isEmpty()) return
        viewModelScope.launch {
            repository.addMedicationWithSchedule(
                name = name,
                totalQuantity = totalQuantity,
                dosage = dosage,
                unit = unit,
                frequencyType = frequencyType,
                daysOfWeekMask = daysOfWeekMask,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                timesOfDay = timesOfDay,
                refillEnabled = refillEnabled,
            )
        }
    }

    fun updateMedicationWithSchedule(
        medication: MedicationEntity,
        timesOfDay: List<String>,
        refillEnabled: Boolean,
    ) {
        viewModelScope.launch {
            repository.updateMedicationWithSchedule(medication, timesOfDay, refillEnabled)
        }
    }

    fun updateMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.updateMedication(medication)
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }
}

class MedicationViewModelFactory(
    private val repository: MedicationRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MedicationViewModel::class.java))
        return MedicationViewModel(repository) as T
    }
}

