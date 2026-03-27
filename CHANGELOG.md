# VaultCall Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - Current Iteration (Iteration 6)

### Security & Telecom Hardening (Android 14+)
* **STIR/SHAKEN Caller ID Spoofing Defense:** Wired `MyCallScreeningService` to intercept the carrier's `CallDetails.callerNumberVerificationStatus`. VaultCall now instantly rejects verified spoofed robocalls before they attempt to interact with the RulesEngine or Voicemail service.
* **Full-Screen Intent Notifications:** Converted the background Incoming Call UI launch into a legally compliant `IMPORTANCE_HIGH` Heads-Up Notification equipped with a `FullScreenIntent`. This completely bypasses the Android 10+ background activity blockage.
* **Modern Audio Routing:** Scrapped legacy `setAudioRoute()` and `setSpeakerphoneOn()` which fail on Android 14. Rewired `MyInCallService.setSpeakerphone()` to explicitly retrieve the `AudioManager.availableCommunicationDevices` and bind `setCommunicationDevice` for bulletproof recording interactions.
* **OMAPI Hardware Identifiers:** Appended `PackageManager.hasSystemFeature` checks for `FEATURE_SE_OMAPI_ESE` and `FEATURE_SE_OMAPI_UICC` in `CallInfo` state logic to differentiate active calls happening over physical SIM cards versus embedded eSIM interfaces.

## [Unreleased] - Current Iteration (Iteration 5)

### Fixed
* **Intent Calling Loop Bug:** Fixed a critical bug where VaultCall swallowed its own `ACTION_CALL` intents because it was set as the Default Dialer. `MainActivity` now explicitly intercepts outgoing call intents and funnels them directly to the Telecom framework instead of infinitely re-rendering the UI.
* **Dual-SIM Silent Hang Resolution:** On multi-SIM devices, `TelecomManager.placeCall` pauses the call to ask which SIM to use (`STATE_SELECT_PHONE_ACCOUNT`). Since VaultCall overrode the system dialer UI, there was no prompt, leaving the call hanging silently. `MyInCallService` now programmatically detects this state and instantly auto-selects the primary SIM to force the call into `STATE_DIALING`.

### Fixed
* **Dialer Rework (ACTION_CALL Routing):** Completely deprecated `TelecomManager.placeCall` which was triggering an infinite intent loop bug and getting blocked by OEMs. Outgoing calls now route robustly via Android's native `Intent.ACTION_CALL` resolver straight into `MyInCallService`.

### Added
* **Dialer Auto-Complete:** Integrated `ContactsContract` queries into the Dialer T9 keypad. As you type a number, matching contacts instantly appear in a horizontal row above the dialpad.
* **Call Log Cards & Quick Dial:** Call Log entries now open a detailed "Contact Card" BottomSheet on tap, and the Quick Call buttons natively trigger `ACTION_CALL`.
* **Missed Call Auto-Voicemail:** VaultCall now acts as a true answering machine. If a call rings longer than your "Rings before voicemail" setting, VaultCall programmatically answers the call and intercepts it to the local `VoicemailRecorderService` before carrier cloud-voicemail takes over!

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
