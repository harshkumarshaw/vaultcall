# VaultCall: System Architecture

VaultCall is an on-device, privacy-first Android application designed to fully replace the Default Dialer. It provides intelligent Call Screening, on-device AI transcription using Whisper, and Voicemail recording without ever transmitting data to the cloud.

## High-Level Components

### 1. Telecom & In-Call Services (`com.vaultcall.service`)
The core foundation of VaultCall relies on two primary Android Telecom services:

* **`MyInCallService`:** Binds to the system `TelecomManager` when VaultCall is set as the Default Dialer. It intercepts incoming call objects and outgoing call requests, acting as the bridge between the Android cellular framework and our custom compose UI.
* **`MyCallScreeningService`:** Acts as the gateway for incoming calls. It checks phone numbers against the `RulesEngine` to determine if a call should be allowed, blocked immediately, or screened (sent to VaultCall's local voicemail pipeline).

### 2. User Interface (`com.vaultcall.ui`)
A fully reactive Modern Android UI built primarily in **Jetpack Compose** using Material 3 guidelines.

* **Activities:** 
  * `MainActivity`: The primary entry point featuring bottom navigation for recents, voicemails, dialer, rules, and settings. Handles Onboarding and Biometric App Lock.
  * `IncomingCallActivity`: A specialized activity that utilizes `WindowManager` flags to bypass the keyguard and display full-screen above the lock screen when a call arrives.
  * `ActiveCallActivity`: Manages the state of an ongoing connected telephone call.

### 3. State Management
* **`CallStateManager`:** A singleton injected via Dagger Hilt that maintains a `StateFlow` of all active calls. It translates raw Android `Call` objects into UI-friendly `CallState` enums (e.g., `RINGING`, `DIALING`, `CONNECTING`, `ACTIVE`, `DISCONNECTED`).

### 4. Machine Learning Pipeline (`com.vaultcall.ai`)
* **`WhisperTranscriber`:** Loads a quantized `whisper-tiny.onnx` model using ONNXRuntime. When a voicemail is recorded locally, this engine unpacks the raw audio into an array, computes a Mel Spectrogram, and feeds it into the model for inference. The extraction handles fallbacks if the underlying model is removed or fails to mount.

### 5. Data Persistence (`com.vaultcall.data`)
* **Room Database:** Manages structured SQL tables for `CallLogEntry` (history), `Rule` (blocking/allowing), and `Voicemail` entities. Includes DAOs and asynchronous Flow emissions for UI binding.
* **`SettingsRepository`:** A SharedPreferences wrapper mapping generic user settings into atomic Kotlin `StateFlow` instances. Provides true persistence for user configuration.

---

## Architectural Data Flow (The "Screening" Path)

1. A call rings standard Android subsystem ->
2. `MyCallScreeningService` receives the caller ID ->
3. Service queries `appDatabase.ruleDao()` for blocklists ->
4. If "SCREEN", service suppresses standard ringtones ->
5. Wait for line to open -> `VoicemailRecorderService` activates ->
6. Service flips on the **Hardware Speakerphone** and plays AI Greeting via TTS ->
7. Service records using `MediaRecorder.AudioSource.MIC` ->
8. File closes, `TranscriptionWorker` triggers `WhisperTranscriber` ->
9. Transcript generated -> Room DB updated -> OS Notification fired.
