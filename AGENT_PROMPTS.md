# Agent Prompt Templates — Copy-Paste for Each Build Session

## How to Use
1. Open Claude Code
2. Copy the prompt for the current module
3. Paste it as your first message
4. Agent will build that module completely
5. After each module, run the verification checklist
6. Move to next module

---

## SESSION 1 — Project Setup

```
You are building an Android app called VaultCall.
Read the full instructions in CLAUDE.md first, then build Module 01.

Project details:
- Package: com.vaultcall
- Language: Kotlin
- UI: Jetpack Compose Material 3
- Architecture: MVVM + Hilt + Room

Instructions file: instructions/01_project_setup.md

Tasks:
1. Create app/build.gradle.kts with all dependencies listed in the instructions
2. Create AndroidManifest.xml with all permissions and service declarations
3. Create VaultCallApp.kt with Hilt annotation
4. Create res/values/strings.xml with all required strings

Write complete, production-ready code. No placeholders. Handle all imports.
After completing, list every file created.
```

---

## SESSION 2 — Data Layer

```
Continue building VaultCall. Module 01 is complete.

Read instructions/02_data_layer.md and implement the complete data layer.

Existing files:
- app/build.gradle.kts (complete)
- AndroidManifest.xml (complete)
- VaultCallApp.kt (complete)

Tasks:
1. Create all 4 data model files (Voicemail, Transcript, CallRule, CallLogEntry)
2. Create all 4 DAO files with complete query implementations
3. Create AppDatabase.kt
4. Create Converters.kt for type conversions
5. Create all 3 repository files

All suspend functions must use Dispatchers.IO.
All Flow queries must work correctly.
Write complete code, no TODOs.
```

---

## SESSION 3 — Security Layer

```
Continue building VaultCall. Modules 01-02 are complete.

Read instructions/03_security_layer.md and implement the full security layer.

Tasks:
1. Create EncryptionManager.kt — AES-256-GCM + Android Keystore
2. Create DatabaseModule.kt — Hilt module that opens SQLCipher-encrypted Room DB
3. Create BiometricManager.kt — BiometricPrompt wrapper
4. Create SecureFileStorage.kt — encrypted file management
5. Create AppLockManager.kt — session-based app lock

Critical requirements:
- Android Keystore alias must be "vaultcall_master_key"
- IV must be prepended to ciphertext (12 bytes + ciphertext)
- DB passphrase must be derived from Keystore, never hardcoded
- Always delete temp decrypted files after use

Write complete, production-ready code.
```

---

## SESSION 4 — Telecom Foundation

```
Continue building VaultCall. Modules 01-03 are complete.

Read instructions/04_telecom_foundation.md and implement telecom layer.

Tasks:
1. Create CallStateManager.kt — shared singleton for call state
2. Create MyInCallService.kt — full InCallService implementation
3. Create MyCallScreeningService.kt — intercepts and evaluates incoming calls
4. Create DefaultDialerHelper.kt — utility for default dialer management
5. Create OnboardingScreen.kt — 4-step onboarding with permission requests

The CallScreeningService must broadcast "com.vaultcall.SCREEN_CALL" when a rule
matches, passing phone_number, rule_id, and rule_action as extras.

Write complete Kotlin code with all Hilt annotations.
```

---

## SESSION 5 — Rules Engine

```
Continue building VaultCall. Modules 01-04 are complete.

Read instructions/05_rules_engine.md and implement the rules engine.

Tasks:
1. Create RulesEngine.kt — evaluates rules with night hours, DND, calendar, schedule
2. Create EvaluateRulesUseCase.kt
3. Create WhitelistBlacklistManager.kt — DataStore backed
4. Create RulesScreen.kt + RulesViewModel.kt — full CRUD UI

Night hours logic MUST handle midnight crossover correctly.
DND check must use NotificationManager.currentInterruptionFilter.
Calendar check must query CalendarContract.Events.

Include 3 pre-built rule templates on empty state.
```

---

## SESSION 6 — Voicemail Engine + Transcription

```
Continue building VaultCall. Modules 01-05 are complete.

Read instructions/06_to_12_modules.md sections "Module 06" and "Module 07".

Tasks for Module 06 (Voicemail Engine):
1. Create VoicemailRecorderService.kt — full recording pipeline
2. Create VoicemailListScreen.kt + VoicemailDetailScreen.kt
3. Create VoicemailViewModel.kt

Tasks for Module 07 (Transcription):
1. Create AudioPreprocessor.kt — decode .m4a to 16kHz float array
2. Create WhisperTranscriber.kt — ONNX inference wrapper
3. Create TranscriptionWorker.kt — WorkManager worker

Whisper model file will be at: app/src/main/assets/whisper-tiny.onnx
Add aaptOptions { noCompress "onnx" } to build.gradle.
Temp files must ALWAYS be deleted in finally blocks.
```

---

## SESSION 7 — Call Screening Flow + Dialer

```
Continue building VaultCall. Modules 01-07 are complete.

Read instructions/06_to_12_modules.md sections "Module 08" and "Module 09".

Tasks for Module 08 (Screening):
1. Create GreetingPlayer.kt — TTS + recorded greeting playback
2. Complete VoicemailRecorderService with full auto-answer pipeline
3. Create AutoReplyUseCase.kt — SMS sender with template substitution
4. Implement screening notification with action buttons

Tasks for Module 09 (Dialer):
1. Create DialerScreen.kt — keypad, recents, contacts tabs
2. Create DialerViewModel.kt
3. Create ActiveCallScreen.kt
4. Create SpamDetector.kt — prefix-based local detection
5. Add call notes bottom sheet after call ends

Keypad must play DTMF tones and haptic feedback.
Quick dial pins must persist via DataStore.
```

---

## SESSION 8 — Settings, Notifications, Polish

```
Continue building VaultCall. Modules 01-09 are complete.

Read instructions/06_to_12_modules.md sections "Module 10", "Module 11", "Module 12".

Tasks for Module 10 (Settings):
1. Create SettingsScreen.kt — all sections
2. Create GreetingSetupScreen.kt — record or TTS greeting per rule type
3. Create PrivacyReportScreen.kt

Tasks for Module 11 (Notifications):
1. Create NotificationHelper.kt — all channels and notification types
2. Create VoicemailWidget.kt — AppWidget for home screen

Tasks for Module 12 (Polish):
1. Add empty states to all list screens
2. Add ProGuard rules to proguard-rules.pro
3. Configure release build in build.gradle.kts
4. Create test_install.sh script

After completing, run: ./gradlew assembleRelease
Report any build errors.
```

---

## DEBUGGING SESSION (use if build fails)

```
The VaultCall Android build has errors. Here are the errors:

[PASTE ERRORS HERE]

Fix all errors. Do not change working code unnecessarily.
Only modify the files needed to fix these specific errors.
After fixing, confirm which files were changed and why.
```

---

## FEATURE ADD SESSION (use to add features after MVP)

```
VaultCall MVP is complete and working. Add this new feature:

[DESCRIBE FEATURE]

Requirements:
- Follow existing MVVM + Hilt + Clean Architecture patterns
- Add encryption if any new files are stored
- Add to existing navigation graph
- Update strings.xml with any new strings
- Write complete code, no placeholders

Show me which files need to be created or modified.
```
