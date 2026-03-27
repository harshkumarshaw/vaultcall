# VaultCall Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - Current Iteration (Iteration 4)

### Added
* **Settings Selection Dialogs:** Fully interactive pop-ups for all numeric configuration values (Auto-lock timer, Max Recording length, Rings before voicemail) saving directly to persistent SharedPreferences.
* **Privacy Controls:** Implemented a full data reset "Wipe All Data" action that scrubs SharedPreferences safely.
* **Dialer Registration Fixes:** Fixed `android.telecom.IN_CALL_SERVICE_RINGING` metadata parameter missed during initial dialer implementation, which was preventing incoming line-state changes from passing through to Jetpack Compose `MyInCallService`.

## [Beta 1] - Iteration 3

### Added
* **Live Permissions Dashboard:** Real-time visibility of required and optional permissions with one-click granting and deep-linking to system settings.
* **App Defaults Management:** Dedicated `RoleManager` UI to request and confirm `Default Dialer` and `Call Screener` roles.
* **In-Call UIs:** Added `IncomingCallActivity` (with pulsing lock-screen integration) and `ActiveCallActivity` (with live timer, mute, and speaker controls).
* **Settings Persistence:** `SettingsRepository` using SharedPreferences wrapped in a `SettingsViewModel` for a reactive settings UI.
* **Biometric App Lock:** Configurable `BiometricPrompt` on app launch.
* **AI Model Fallbacks:** Graceful degradation if the `whisper-tiny.onnx` file is missing, replacing the transcript with safe fallback UI text.
* **Speakerphone Voicemail Routing:** Critical hardware fix to force speakerphone output during screened calls, enabling `MediaRecorder.AudioSource.MIC` to capture caller audio cleanly despite modern VoIP echo cancellation.

### Changed
* Refactored `MyInCallService.kt` to accurately detect `STATE_DIALING` and `STATE_CONNECTING` from the `TelecomManager`.
* Shifted dialer intent triggers to use `RoleManager` (API 29+) as primary method, falling back to legacy intents.
* Updated `CallStateManager.kt` logic to emit a reactive StateFlow.

### Fixed
* Lock-screen calls hanging forever by automatically dismissing `IncomingCallActivity` and `ActiveCallActivity` the moment `CallState.DISCONNECTED` is emitted.
* Removed problematic `RECEIVE_SMS` dependencies preventing compilation.
* Missing vector icons (`AutoMirrored` fixes) in the Jetpack Compose components.
