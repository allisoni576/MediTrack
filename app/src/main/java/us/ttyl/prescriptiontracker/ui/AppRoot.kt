package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.ttyl.prescriptiontracker.auth.AuthState
import us.ttyl.prescriptiontracker.auth.BiometricAuthManager
import us.ttyl.prescriptiontracker.auth.LocalAuthStore
import us.ttyl.prescriptiontracker.data.local.DatabaseProvider
import us.ttyl.prescriptiontracker.data.repository.MedicationRepository
import us.ttyl.prescriptiontracker.data.repository.ReminderRepository
import us.ttyl.prescriptiontracker.notifications.NotificationPreferences
import us.ttyl.prescriptiontracker.notifications.NotificationPreferencesStore
import us.ttyl.prescriptiontracker.workers.ReminderAlarmScheduler
import us.ttyl.prescriptiontracker.workers.ReminderWorkScheduler

private enum class AppScreen {
    SETUP,
    UNLOCK,
    DASHBOARD,
    MEDICATION_LIST,
    REMINDERS,
    ADD_MEDICATION,
    MEDICATION_DETAILS,
    EDIT_MEDICATION,
    HISTORY,
    MEDICATION_OVERVIEW,
    SETTINGS,
}

/** Auto re-lock the app after this long without any user interaction. */
private const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val authStore = remember { LocalAuthStore(context.applicationContext) }
    val authState by authStore.authState.collectAsState(
        initial = AuthState(
            isConfigured = false,
            username = "",
            biometricEnabled = false,
            lastUnlockAt = 0L,
        )
    )
    var screen by remember { mutableStateOf(AppScreen.UNLOCK) }
    var selectedMedicationId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    val database = remember { DatabaseProvider.getInstance(context.applicationContext) }
    val medicationRepository = remember {
        MedicationRepository(database.medicationDao(), database.reminderDao())
    }
    val medicationViewModel: MedicationViewModel = viewModel(
        factory = MedicationViewModelFactory(medicationRepository)
    )
    val medications by medicationViewModel.medications.collectAsState()

    val reminderRepository = remember { ReminderRepository(database.reminderDao(), database.medicationDao()) }
    val reminders by reminderRepository.observeAllReminders().collectAsState(initial = emptyList())
    val doseEvents by reminderRepository.observeAllDoseEvents().collectAsState(initial = emptyList())

    var selectedSchedules by remember {
        mutableStateOf<List<us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity>>(emptyList())
    }
    var selectedRefillReminder by remember {
        mutableStateOf<us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity?>(null)
    }
    var scheduleRefreshTrigger by remember { mutableLongStateOf(0L) }
    LaunchedEffect(selectedMedicationId, scheduleRefreshTrigger) {
        val id = selectedMedicationId
        if (id != null) {
            selectedSchedules = medicationRepository.getSchedulesForMedication(id)
            selectedRefillReminder = medicationRepository.getRefillReminderForMedication(id)
        } else {
            selectedSchedules = emptyList()
            selectedRefillReminder = null
        }
    }

    val notificationPrefsStore = remember { NotificationPreferencesStore(context.applicationContext) }
    val notificationPrefs by notificationPrefsStore.preferencesFlow.collectAsState(
        initial = NotificationPreferences()
    )

    val biometricManager = remember {
        (context as? androidx.fragment.app.FragmentActivity)?.let { BiometricAuthManager(it) }
    }

    // --- Inactivity auto re-lock tracking ---
    var lastInteractionAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    fun markInteraction() {
        lastInteractionAt = System.currentTimeMillis()
    }

    LaunchedEffect(screen) {
        if (screen != AppScreen.UNLOCK && screen != AppScreen.SETUP) {
            while (true) {
                delay(15_000L)
                if (System.currentTimeMillis() - lastInteractionAt >= INACTIVITY_TIMEOUT_MS) {
                    screen = AppScreen.UNLOCK
                    break
                }
            }
        }
    }

    // Re-lock whenever the app goes to the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                screen != AppScreen.UNLOCK && screen != AppScreen.SETUP
            ) {
                screen = AppScreen.UNLOCK
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(authState.isConfigured) {
        screen = if (authState.isConfigured) AppScreen.UNLOCK else AppScreen.SETUP
    }

    LaunchedEffect(Unit) {
        ReminderWorkScheduler.schedulePeriodic(context.applicationContext)
        ReminderWorkScheduler.scheduleNow(context.applicationContext)
    }

    // Proactively mark overdue doses as MISSED while the app is in the foreground, rather than
    // relying solely on the WorkManager periodic job (which only runs every ~15 minutes and may
    // be deferred further by the OS). This ensures the Dashboard's "Missed Doses" card and the
    // Missed Medication History screen reflect newly-overdue doses promptly.
    LaunchedEffect(screen) {
        if (screen != AppScreen.UNLOCK && screen != AppScreen.SETUP) {
            while (true) {
                reminderRepository.markOverdueDosesMissed(System.currentTimeMillis())
                delay(30_000L)
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (screen) {
                AppScreen.SETUP -> SetupScreen(
                    onSetupComplete = { username, password, biometricEnabled ->
                        scope.launch {
                            authStore.setup(username = username, password = password)
                            authStore.setBiometricEnabled(biometricEnabled)
                            markInteraction()
                            screen = AppScreen.DASHBOARD
                        }
                    },
                )

                AppScreen.UNLOCK -> UnlockScreen(
                    username = authState.username,
                    biometricEnabled = authState.biometricEnabled && biometricManager?.canAuthenticate() == true,
                    onUnlock = { password, onResult ->
                        scope.launch {
                            val remainingLockout = authStore.remainingLockoutMillis()
                            if (remainingLockout > 0) {
                                onResult(false)
                            } else {
                                val valid = authStore.validatePassword(password)
                                if (valid) markInteraction()
                                onResult(valid)
                            }
                        }
                    },
                    onResetData = {
                        scope.launch {
                            authStore.resetAll()
                            screen = AppScreen.SETUP
                        }
                    },
                    onUnlockSuccess = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                    onBiometricUnlock = {
                        biometricManager?.authenticate(
                            onSuccess = {
                                scope.launch { authStore.recordUnlock() }
                                markInteraction()
                                screen = AppScreen.DASHBOARD
                            },
                            onError = { },
                        ) ?: run {
                            markInteraction()
                            screen = AppScreen.DASHBOARD
                        }
                    },
                )

                AppScreen.DASHBOARD -> DashboardScreen(
                    username = authState.username.ifBlank { "User" },
                    medications = medications,
                    doseEvents = doseEvents,
                    onOpenMedication = { medication ->
                        markInteraction()
                        selectedMedicationId = medication.id
                        screen = AppScreen.MEDICATION_DETAILS
                    },
                    onOpenMedicationList = {
                        markInteraction()
                        screen = AppScreen.MEDICATION_LIST
                    },
                    onOpenReminders = {
                        markInteraction()
                        screen = AppScreen.REMINDERS
                    },
                    onOpenHistory = {
                        markInteraction()
                        screen = AppScreen.HISTORY
                    },
                    onOpenMedicationOverview = {
                        markInteraction()
                        screen = AppScreen.MEDICATION_OVERVIEW
                    },
                    onOpenSettings = {
                        markInteraction()
                        screen = AppScreen.SETTINGS
                    },
                    onLock = { screen = AppScreen.UNLOCK },
                )

                AppScreen.MEDICATION_LIST -> MedicationListScreen(
                    medications = medications,
                    onOpenMedication = { medication ->
                        markInteraction()
                        selectedMedicationId = medication.id
                        screen = AppScreen.MEDICATION_DETAILS
                    },
                    onAddMedication = {
                        markInteraction()
                        screen = AppScreen.ADD_MEDICATION
                    },
                    onDeleteMedication = { medication ->
                        markInteraction()
                        medicationViewModel.deleteMedication(medication)
                    },
                    onBack = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                )

                AppScreen.ADD_MEDICATION -> AddMedicationScreen(
                    onSave = { name, totalQuantity, dosage, unit, frequencyType, daysMask, startDate, endDate, notes, times, refillEnabled ->
                        markInteraction()
                        medicationViewModel.addMedicationWithSchedule(
                            name = name,
                            totalQuantity = totalQuantity,
                            dosage = dosage,
                            unit = unit,
                            frequencyType = frequencyType,
                            daysOfWeekMask = daysMask,
                            startDate = startDate,
                            endDate = endDate,
                            notes = notes,
                            timesOfDay = times,
                            refillEnabled = refillEnabled,
                        )
                        ReminderWorkScheduler.scheduleNow(context.applicationContext)
                        screen = AppScreen.MEDICATION_LIST
                    },
                    onCancel = {
                        markInteraction()
                        screen = AppScreen.MEDICATION_LIST
                    },
                )

                AppScreen.MEDICATION_DETAILS -> {
                    val medication = medications.find { it.id == selectedMedicationId }
                    if (medication == null) {
                        screen = AppScreen.DASHBOARD
                    } else {
                        val medicationDoseEvents = doseEvents.filter { it.medicationId == medication.id }
                        MedicationDetailsScreen(
                            medication = medication,
                            doseEvents = medicationDoseEvents,
                            schedules = selectedSchedules,
                            refillReminder = selectedRefillReminder,
                            onEdit = {
                                markInteraction()
                                screen = AppScreen.EDIT_MEDICATION
                            },
                            onDelete = {
                                markInteraction()
                                medicationViewModel.deleteMedication(medication)
                                screen = AppScreen.DASHBOARD
                            },
                            onBack = {
                                markInteraction()
                                screen = AppScreen.DASHBOARD
                            },
                        )
                    }
                }

                AppScreen.EDIT_MEDICATION -> {
                    val medication = medications.find { it.id == selectedMedicationId }
                    if (medication == null) {
                        screen = AppScreen.DASHBOARD
                    } else {
                        EditMedicationScreen(
                            medication = medication,
                            schedules = selectedSchedules,
                            refillReminder = selectedRefillReminder,
                            onSave = { updated, timesOfDay, refillEnabled ->
                                markInteraction()
                                medicationViewModel.updateMedicationWithSchedule(updated, timesOfDay, refillEnabled)
                                ReminderWorkScheduler.scheduleNow(context.applicationContext)
                                scheduleRefreshTrigger += 1
                                screen = AppScreen.MEDICATION_DETAILS
                            },
                            onCancel = {
                                markInteraction()
                                screen = AppScreen.MEDICATION_DETAILS
                            },
                        )
                    }
                }

                AppScreen.REMINDERS -> RemindersScreen(
                    reminders = reminders,
                    doseEvents = doseEvents,
                    onMarkTaken = { reminder ->
                        markInteraction()
                        scope.launch {
                            reminderRepository.markCompleted(reminder.id)
                            ReminderAlarmScheduler.cancel(context.applicationContext, reminder.id)
                        }
                    },
                    onSnooze = { reminder ->
                        markInteraction()
                        scope.launch {
                            val newTrigger = reminderRepository.snoozeReminder(reminder.id)
                            if (newTrigger != null) {
                                ReminderAlarmScheduler.schedule(context.applicationContext, reminder.id, newTrigger)
                            }
                        }
                    },
                    onNoteChanged = { reminder, note ->
                        markInteraction()
                        scope.launch { reminderRepository.updateNote(reminder.id, note) }
                    },
                    onBack = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                )

                AppScreen.HISTORY -> HistoryScreen(
                    medications = medications,
                    doseEvents = doseEvents,
                    onMarkTaken = { doseEventIds ->
                        markInteraction()
                        scope.launch {
                            doseEventIds.forEach { id ->
                                reminderRepository.markDoseEventTaken(id)
                            }
                        }
                    },
                    onBack = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                )

                AppScreen.MEDICATION_OVERVIEW -> MedicationOverviewScreen(
                    medications = medications,
                    doseEvents = doseEvents,
                    onMarkTaken = { doseEventIds ->
                        markInteraction()
                        scope.launch {
                            doseEventIds.forEach { id ->
                                reminderRepository.markDoseEventTaken(id)
                            }
                        }
                    },
                    onBack = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    soundEnabled = notificationPrefs.soundEnabled,
                    vibrationEnabled = notificationPrefs.vibrationEnabled,
                    biometricEnabled = authState.biometricEnabled,
                    biometricAvailable = biometricManager?.canAuthenticate() == true,
                    onSoundChanged = { enabled ->
                        markInteraction()
                        scope.launch { notificationPrefsStore.setSoundEnabled(enabled) }
                    },
                    onVibrationChanged = { enabled ->
                        markInteraction()
                        scope.launch { notificationPrefsStore.setVibrationEnabled(enabled) }
                    },
                    onBiometricChanged = { enabled ->
                        markInteraction()
                        scope.launch { authStore.setBiometricEnabled(enabled) }
                    },
                    onBack = {
                        markInteraction()
                        screen = AppScreen.DASHBOARD
                    },
                )
            }
        }
    }
}
