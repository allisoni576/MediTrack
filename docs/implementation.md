# PrescriptionTracker Implementation Specification

## 1. Purpose
This document defines the formal technical specification for implementing the PrescriptionTracker Android application based on requirements in `docs/instructions.txt`.

The app enables users to:
- Track medications, dosages, schedules, and refill reminders.
- Receive notifications for medication doses and refills.
- Log medication intake and review adherence history.
- Protect app access with local password and optional biometric unlock on startup.

## 2. Scope

### 2.1 In Scope (MVP)
- First-run app security setup with local password.
- App startup unlock using local password.
- Optional biometric unlock after password setup.
- Dashboard with medication summary, upcoming doses/reminders, notifications, and adherence progress snapshot.
- Add Medication and Medication Details (view/edit/delete).
- Medication History with filters by medication name and date range.
- Reminders list with actions: mark taken and snooze.
- Dose and refill notifications shown in system notification tray.
- Customizable notification preferences: sound, vibration, display behavior.

### 2.2 Out of Scope (Initial MVP)
- Multi-device real-time sync conflict resolution.
- Provider/pharmacy integrations.
- Advanced analytics beyond core adherence metrics.
- Any off-device storage, cloud sync, or remote account backends.

## 3. Product Requirements

### 3.1 Functional Requirements
1. User can set a local app password during first-run setup.
2. User can unlock app at startup using password and optional biometric unlock.
3. User can add medications with dosage, frequency, start/end dates, notes, and refill reminder settings.
4. User can configure reminders per medication and custom times/frequencies.
5. App sends local notifications for scheduled doses/refills.
6. User can mark reminders as taken or snooze for later.
7. App stores intake events and shows medication history with filtering.
8. App computes adherence summary (taken on time vs missed).
9. User can edit/delete medications from details screen.

### 3.2 Non-Functional Requirements
- Reliable reminder delivery across app restarts and device reboot.
- Fully offline operation for all core features.
- Responsive UI for large history lists.
- Accessibility support (content descriptions, scalable text, contrast).
- Privacy-first data handling with strict on-device-only data residency for medical information.

## 4. Target Platform and Technology Stack
- Android (current project under `app/`)
- Kotlin
- Jetpack Compose (UI)
- Navigation Compose
- MVVM architecture
- Hilt for dependency injection
- Room (SQLite) for local persistence
- DataStore for preferences
- Coroutines + Flow
- WorkManager for background scheduling
- NotificationCompat and Android notification channels
- Optional exact timing path using AlarmManager for strict reminder precision

## 5. Architecture

### 5.1 Layered Design
- **UI Layer**: Compose screens, state collection, user interactions.
- **Presentation Layer**: ViewModels exposing immutable UI state and events.
- **Domain Layer**: Use cases for medication CRUD, reminder generation, adherence computations.
- **Data Layer**: Repositories coordinating Room and scheduling APIs.
- **Platform Services**: Notifications, workers, boot/time-change receivers.

### 5.2 Proposed Package Structure
Under `app/src/main/java/us/ttyl/prescriptiontracker/`:
- `ui/`
- `navigation/`
- `auth/`
- `domain/`
- `data/local/`
- `data/repository/`
- `notifications/`
- `workers/`
- `core/`

## 6. Data Model

### 6.1 Core Domain Entities
- **AppProfile**: `id`, `username`, `createdAt`, `biometricEnabled`, `lastUnlockAt`.
- **Medication**: `id`, `name`, `totalQuantity` (Int, prescribed supply count), `remainingQuantity` (Int, auto-decremented by `dosage` each time a dose is marked taken), `dosage` (Int, units to take per dose; must be ≤ `totalQuantity`), `frequencyType` (`DAILY` | `WEEKLY`), `daysOfWeekMask` (used only when `WEEKLY`; bit flags for Sun–Sat; at least one day required), `timesPerDay` (Int, 1–5), `startDate`, `endDate` (optional; null/empty = ongoing/chronic, no end), `notes`, `isActive`, `createdAt`.
- **MedicationSchedule**: `id`, `medicationId`, `timeOfDay` (one row per scheduled time, up to 5 per medication, must be unique within the medication, stored as 30-minute-aligned HH:mm values from 00:00–23:30). Times apply uniformly across every active day (daily or selected weekly days) — no per-day time variation.
- **DoseEvent**: `id`, `medicationId`, `scheduledAt`, `status`, `takenAt`, `snoozedUntil`, `note`.
- **RefillReminder**: `id`, `medicationId`, `numberOfRefills` (Int, fixed count), `refillDates` (user-entered list of individual dates, one per refill), `enabled`.
- **ReminderInstance**: `id`, `type` (`DOSE` | `REFILL`), `relatedEntityId`, `triggerAt`, `state`, `note` (per-reminder note tied to the specific dose/medication).

### 6.2 Status Enumerations
- Dose status: `PENDING`, `TAKEN`, `MISSED`, `SNOOZED`.
- Reminder state: `SCHEDULED`, `FIRED`, `COMPLETED`, `SNOOZED`, `CANCELLED`.

### 6.3 Storage Notes
- All app data is stored in on-device SQLite via Room.
- Room tables indexed for time-based and medication-based queries.
- History retained for adherence reporting.
- App does not transmit or persist medical/user data off device.
- Soft-delete or archival strategy may be added later for data growth control.
- `remainingQuantity` is auto-decremented by `dosage` whenever a dose is marked `TAKEN`; a low-supply warning is derived by comparing `remainingQuantity` against the number of scheduled doses remaining (see Section 11).

## 7. Authentication Specification

### 7.1 Supported Startup Unlock Methods
- Local app password (required).
- Optional local biometric unlock (device capability dependent) after password setup.

### 7.2 Security Requirements
- App password is never stored in plain text.
- Store password hash using a strong KDF with salt.
- Session/unlock state is local-only and stored securely.
- App requires unlock on startup and may re-lock after inactivity timeout.
- Lockout/backoff should be applied after repeated failed unlock attempts.

### 7.3 Local Recovery
- If biometric unlock is enabled, user may unlock with biometric when password is forgotten.
- If no recovery method is configured, recovery requires local data reset (explicit user confirmation required).

## 8. Screen Specifications

### 8.1 First-Run Setup Screen
- Shown once on first app launch.
- Inputs: username (optional display name), password, confirm password.
- Action: `Set Up App`.
- Optional prompt to enable biometric unlock after successful setup.

### 8.2 Unlock Screen
- Inputs: password.
- Actions: `Unlock`.
- Optional biometric unlock action when previously configured.
- Action: `Reset App Data` (destructive, requires confirmation).

### 8.3 Dashboard Screen
- No adherence/taken/missed/pending summary section (removed).
- No "Add Medication" quick action (removed) — medication creation is now initiated from the Medication List screen's "+" button (see 8.3.1).
- "Today's Medications" card — a single merged card (replacing the separate "Missed Doses" and "Today's Medications" cards), tappable, styled consistent with the old adherence card (short label/icon + status breakdown), e.g. "📋 Today's Medications — 5 Taken · 2 Missed · 3 Pending"; sized larger than the other dashboard sections for emphasis; tapping it navigates to the combined Today's Medication & Missed Doses screen (see 8.6.1, which now merges the former 8.6.1 and 8.6.2).
  - All three counts (**Taken**, **Missed**, **Pending**) are scoped to **today only** (doses scheduled for the current calendar day).
  - Each count reflects **distinct medications**, not raw dose-event rows — a medication scheduled multiple times today (e.g. 2x/day) only contributes once to whichever status bucket(s) it falls into, rather than inflating the count per dose instance.
- "View Medications" button (renamed from "Saved Medications (N)") — opens the dedicated Medication List screen (see 8.3.1) instead of an inline scrollable list, avoiding nested-scroll layout issues on the dashboard.
- Quick actions: View Medications, View Reminders, View History, Settings, Lock.

### 8.3.1 Medication List Screen
- Opened from the Dashboard's "View Medications" button.
- Back button (`< Back`) at the top returns to the Dashboard.
- "+" button at the top-right of the screen navigates to the Add Medication screen (screen 5) — this is now the sole entry point for creating a new medication, replacing the removed Dashboard "Add Medication" action.
- Lazy-loaded card list of all saved medications (name, dosage, frequency) with a "View Details" button per card that navigates to the Medication Details screen.
- Shows an empty-state message ("No medications saved yet.") when there are no medications.

### 8.4 Medication Details Screen
- Display medication fields: name, total quantity (Int), remaining quantity (Int, auto-decremented as doses are taken), dosage (Int, units per dose), start date, end date (shown as "Ongoing" if not set), refill information (number of refills and refill dates), notes.
- Schedule display:
  - If `DAILY`: show "Daily", the number of times per day (up to 5), and the scheduled times for each dose.
  - If `WEEKLY`: show "Weekly", the selected days of the week, the number of times per day (up to 5), and the scheduled times for each dose.
- Low-supply warning banner shown when remaining quantity is insufficient to cover upcoming scheduled doses.
- Notes section displaying any user-entered medication notes/instructions.
- Actions: Edit, Delete.
  - Editing frequency type/schedule (daily/weekly, days, times) regenerates and reschedules all pending reminders/notifications for that medication to match the new schedule.
- Calming color scheme, clean and organized layout with clear labels and easy navigation.

### 8.5 Add Medication Screen
- Reachable only via the Medication List screen's "+" button (see 8.3.1) — there is no longer a direct Dashboard entry point.
- Inputs: medication name, total quantity (Int), dosage (Int, units per dose; validated ≤ total quantity), refill information (number of refills, list of individual refill dates), start date, end date (optional — leave blank for ongoing/chronic medications), notes.
- Schedule input:
  - Radio button group to choose frequency: `Daily` or `Weekly`.
  - If `Weekly` selected, show a checkbox group for days of week (Sunday–Saturday); at least one day must be selected.
  - Time selection: dropdown menu of times in 30-minute increments from 00:00 to 23:30; user can add up to 5 unique times per medication. The same times apply to every active day (daily, or every selected weekly day) — no per-day time customization.
- Actions:
  - `Save`: validates all input (including dosage ≤ total quantity, at least one weekly day if applicable, unique times); on success, persists medication, schedule, and refill records to the user's account, and navigates back to the Medication List screen.
  - `Cancel`: discards changes and navigates back to the Medication List screen without saving.
- Notes section for any relevant information or instructions related to the medication.
- Calming color scheme, clean and organized layout with clear labels and easy navigation.

### 8.6 Medication History Screen
- No adherence/taken/missed/pending summary section (removed).
- Chronological intake list with date/time/status.
- Filters: medication name and date range.
- Optional notes display.

### 8.6.1 Today's Medication & Missed Doses Screen (combined)
- Reachable **only** by tapping the Dashboard's merged "Today's Medications" card (8.3). This screen replaces the two previously-separate screens (the old 8.6.1 Missed Medication History screen and 8.6.2 Today's Medication screen), which are now combined into one.
- Contains three clearly labeled sections, in this order:
  1. **"Missed Medications"** section (first) — lists missed doses only: medication name, dosage, and scheduled date/time. Filterable by medication name and date range (same filter behavior as Screen 8.6).
  2. **"Taken Medications"** section (second) — lists doses scheduled for the **current calendar day only** that have already been marked `TAKEN`, showing medication name, dosage, and scheduled time. Filterable by medication name.
  3. **"Pending Medications"** section (third) — lists doses scheduled for the **current calendar day only** that are still `PENDING`/`SNOOZED` (awaiting action), showing medication name, dosage, and scheduled time. Filterable by medication name.
- Each entry across all three sections is a simple detail card — medication name, dosage, scheduled time/date — with a "Mark as Taken" checkbox on the right side; no additional notes field on this screen.
  - In the Taken Medications section, entries are shown pre-checked and disabled (cannot be un-marked from this screen).
  - In the Missed and Pending Medications sections, the checkbox is interactive, letting the user mark that dose as taken.
- Checkbox state changes across sections are held locally while the user is on the screen; when the user navigates back to the Dashboard, all checked entries are persisted — the corresponding `DoseEvent` records are updated to `TAKEN` (and `remainingQuantity` decremented accordingly) in the database at that time.
- Each section shows its own independent empty-state message (e.g., "No missed doses in this range" / "No medications taken today yet." / "No pending medications for today.") when that section has no entries, even if other sections have entries.
- Back navigation (`< Back` and "Back to Dashboard") returns to the Dashboard screen and triggers the save described above.
- Calming color scheme, clean and organized layout with clear labels and easy navigation.

### 8.7 Reminders Screen
- Chronological list of upcoming reminders (most recent/soonest at the top), generated purely from each medication's configured schedule (daily or weekly, up to 5 times/day) — reminders are not independently/custom editable.
- Actions per item: `Mark Taken` button, `Snooze` button (fixed 15-minute snooze duration).
- Per-reminder notes field tied to the specific dose/medication, where users can add relevant information or instructions for that reminder.
- Adherence summary scoped to **today only** (doses taken on time vs. missed for the current day) — distinct from the Dashboard's all-time/period adherence tracker.
- Back navigation to return to the Dashboard screen.
- Calming color scheme, clean and organized layout with clear labels and easy navigation.

## 9. Reminder and Notification System

### 9.1 Notification Channels
- `dose_reminders` — used exclusively for scheduled dose reminders, derived from medication schedules.
- `refill_reminders` — used exclusively for refill reminders, derived from `RefillReminder.refillDates`; kept fully separate from dose reminders in channel, importance, and action behavior.

### 9.2 Behavior
- Notifications appear in system tray.
- Dose reminder notifications provide actions:
  - Mark as taken (button; decrements `remainingQuantity` by `dosage`).
  - Snooze (fixed 15-minute duration; not user-configurable per snooze).
- Refill reminder notifications provide an acknowledgment/dismiss action tied to the specific `RefillReminder` entry.
- Action handlers update persistence and UI state.

### 9.3 Scheduling Strategy
- Generate reminder instances from medication schedules.
- WorkManager used for resilient scheduling and rescheduling.
- On boot/timezone/time-change, receiver triggers schedule rebuild.
- Optional exact alarm mode for strict-time reminders where product requires precise timing.
- Testing aid: when a brand-new schedule's time-of-day has already passed for "today" (e.g. a medication is added/edited with a scheduled time earlier than the current time), the very first occurrence is generated with its trigger clamped to "now" and fires immediately instead of waiting until the same time tomorrow. This only applies to the first occurrence of a schedule (no prior dose events exist yet); subsequent days/occurrences are unaffected and use their normal future times. This makes it possible to create a medication with a past start time to produce an immediately-firing reminder for manual/automated test cases.

### 9.4 User Notification Preferences
- User-configurable sound, vibration, display behavior.
- Runtime notification permission requested on Android 13+.

## 10. Adherence and History Rules
- A dose is recorded as `TAKEN` when user confirms via app or notification action.
- A dose can be `SNOOZED` and rescheduled.
- A scheduled dose not taken within a 5-minute grace period (`MISSED_GRACE_PERIOD_MILLIS`) after its scheduled time is marked `MISSED`. This grace-period check runs both in the background `ReminderSchedulerWorker` (periodic + on-demand) and via a lightweight foreground ticker (every 30 seconds while the app is unlocked), so the Dashboard's "Today's Medications" card and the combined Today's Medication & Missed Doses screen reflect newly-overdue doses promptly without waiting for the next WorkManager cycle — this also makes it practical to create a medication with a past start time (see §9.3) and observe it transition to Missed within a short test session.
- A `MISSED` dose can be retroactively marked `TAKEN` from the combined Today's Medication & Missed Doses screen (8.6.1) via its per-entry checkbox; the update (status change + `remainingQuantity` decrement) is committed when the user navigates back to the Dashboard.
- The Dashboard and Medication History screens no longer display an aggregate adherence/taken/missed/pending summary; instead:
  - The Dashboard shows a merged "Today's Medications" card (today-scoped taken/missed/pending counts), linking to the combined Today's Medication & Missed Doses screen.
  - The Reminders screen retains its today-scoped adherence summary (taken vs. missed for the current day only), unchanged.
- Underlying adherence computation (total scheduled, taken on time, missed, percentage) remains available in the domain layer for use by the Reminders screen and the "Today's Medications" card's counts.

## 11. Error Handling and Validation
- Form validation for auth and medication creation is mandatory.
- Date/time and frequency constraints must prevent invalid schedules.
- Reminder scheduling failures should be retried and surfaced as user-safe errors.
- Repository layer returns typed error states for consistent UI messaging.
- Security-sensitive auth errors should avoid disclosing whether a username exists.
- Medication-specific validation:
  - `dosage` must not exceed `totalQuantity`.
  - `WEEKLY` frequency requires at least one day-of-week selected.
  - Up to 5 scheduled times per medication; each time must be unique.
  - Start date required; end date optional (ongoing/chronic medications).
  - Editing frequency/schedule triggers regeneration and rescheduling of all pending reminders/notifications.
  - Low-supply warning is raised when `remainingQuantity` falls below the quantity needed to cover upcoming scheduled doses (computed from `dosage` × remaining scheduled occurrences), surfaced on Medication Details and/or Dashboard.

## 12. Testing Strategy

### 12.1 Unit Tests
- Validation logic.
- Adherence calculations.
- Reminder generation and state transitions.

### 12.2 Data/Integration Tests
- Room DAO queries and relations.
- Repository behavior for CRUD and filters.

### 12.3 UI Tests
- First-run setup and unlock flows.
- Add/edit/delete medication flows.
- Reminder actions and history updates.

### 12.4 Background/Notification Tests
- Worker scheduling and rescheduling.
- Notification action intents update corresponding `DoseEvent`.

## 13. Milestone Plan
1. Foundation: architecture scaffold, navigation, DI, database setup.
2. Startup security: first-run password setup, unlock flow, optional biometric unlock, inactivity re-lock.
3. Medication management: Add, list, details, edit, delete.
4. Reminder engine: scheduling, notifications, actions, reboot resilience.
5. History and adherence: filters + dashboard metrics.
6. Hardening: accessibility, privacy/security validation, testing coverage, bug fixes, polish.

## 14. Risks and Decision Log
- Local auth recovery UX can be challenging without off-device reset.
- Reminder precision vs battery constraints.
- Timezone/daylight-saving behavior for schedules.
- Data export/import policy (if ever added) must preserve on-device-only privacy guarantees.

Decision checkpoints required before implementation starts:
1. Exact alarm requirement level.
2. Biometric unlock support in MVP vs deferred.
3. Local backup policy: disabled by default vs encrypted user-controlled export.
4. Inactivity auto-lock timeout defaults and behavior.

## 15. Privacy and Data Residency Controls
- Medical and account data must remain on device in SQLite and app-private storage.
- Disable Android auto backup for app data unless a future encrypted export/backup feature is explicitly approved.
- Notification content on lock screen should be privacy-safe by default (minimal content mode).
- If analytics/crash reporting is added later, it must exclude PHI and require explicit product approval.

## 16. Implementation Readiness
This specification is implementation-ready and intended to be approved before any feature coding begins. After approval, tasks should be created from Section 13 and implemented incrementally with test coverage per Section 12.
