# PrescriptionTracker Docs

## Android Emulator Setup and Test Commands (Windows / PowerShell)

### Prerequisites
- Android Studio installed (SDK + Emulator + at least one AVD created)
- Project path: `D:\dev\PrescriptionTracker`

### 1) Start an emulator

```powershell
# List available virtual devices
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds

# Start a specific AVD (replace Pixel_8_API_35 with your AVD name)
Start-Process -FilePath "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -ArgumentList "-avd Pixel_8_API_35"
```

### 2) Verify a device is connected

```powershell
# List connected devices (wait until emulator appears as "device")
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Optional: wait until boot is complete

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed
```

When boot is complete, the last command returns `1`.

### 3) Run tests

```powershell
Set-Location -Path "D:\dev\PrescriptionTracker"

# Local JVM tests
.\gradlew.bat testDebugUnitTest

# Instrumented Compose/UI tests (requires connected emulator/device)
.\gradlew.bat connectedDebugAndroidTest
```

### 4) Run both unit + instrumented tests

```powershell
Set-Location -Path "D:\dev\PrescriptionTracker"
.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest
```

### 5) Troubleshooting
- If you see `No connected devices!`, restart emulator and check `adb devices`.
- If the emulator is listed as `offline`, run:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" kill-server
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" start-server
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

