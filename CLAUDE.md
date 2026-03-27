# VaultCall — Claude Code Agent Master Instructions

## 🎯 What You Are Building
An Android app called **VaultCall** — a privacy-first, on-device smart voicemail app
that replaces the default Android dialer. It includes:
- Visual voicemail (like iPhone) with on-device AI transcription
- Smart call screening with auto-answer and rules engine
- Custom dialer with themes, quick-dial, call notes
- Full AES-256 encryption, biometric lock, zero cloud dependency

---

## 🧠 Agent Rules — Read Before Writing Any Code

1. **Never use placeholders** — write complete, compilable Kotlin code every time
2. **Never skip error handling** — every suspend function needs try/catch
3. **Always add KDoc comments** to every class and public function
4. **Never rewrite existing files** unless fixing a confirmed bug
5. **Always check the project structure** before creating new files
6. **Commit message format:** `[MODULE] Short description` e.g. `[RULES] Add calendar rule type`
7. **If a task is ambiguous** — pick the most secure and privacy-preserving option
8. **All file I/O must be encrypted** — never write raw audio or text to disk

---

## 🛠️ Tech Stack (Locked — Do Not Change)

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9+ |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture + Repository pattern |
| DI | Hilt |
| Database | Room 2.6+ with SQLCipher encryption |
| Call Handling | Android Telecom API, InCallService, CallScreeningService |
| Transcription | Whisper tiny ONNX via onnxruntime-android |
| Encryption | AES-256-GCM + Android Keystore |
| Audio | MediaRecorder, MediaPlayer, AudioTrack |
| Background | WorkManager + Foreground Service |
| Min SDK | 29 (Android 10) |
| Target SDK | 34 (Android 14) |

---

## 📁 Project Structure

```
app/src/main/
├── java/com/vaultcall/
│   ├── di/
│   │   ├── AppModule.kt
│   │   ├── DatabaseModule.kt
│   │   └── ServiceModule.kt
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── VoicemailDao.kt
│   │   │   ├── TranscriptDao.kt
│   │   │   ├── RuleDao.kt
│   │   │   └── CallLogDao.kt
│   │   ├── model/
│   │   │   ├── Voicemail.kt
│   │   │   ├── Transcript.kt
│   │   │   ├── CallRule.kt
│   │   │   └── CallLogEntry.kt
│   │   ├── repository/
│   │   │   ├── VoicemailRepository.kt
│   │   │   ├── TranscriptRepository.kt
│   │   │   └── RulesRepository.kt
│   │   └── security/
│   │       ├── EncryptionManager.kt
│   │       └── BiometricManager.kt
│   ├── domain/
│   │   ├── usecase/
│   │   │   ├── RecordVoicemailUseCase.kt
│   │   │   ├── TranscribeAudioUseCase.kt
│   │   │   ├── EvaluateRulesUseCase.kt
│   │   │   └── AutoReplyUseCase.kt
│   │   └── rules/
│   │       └── RulesEngine.kt
│   ├── service/
│   │   ├── MyInCallService.kt
│   │   ├── MyCallScreeningService.kt
│   │   ├── VoicemailRecorderService.kt
│   │   └── CalendarWatcherService.kt
│   ├── ai/
│   │   ├── WhisperTranscriber.kt
│   │   └── AudioPreprocessor.kt
│   └── ui/
│       ├── MainActivity.kt
│       ├── theme/
│       │   ├── Theme.kt
│       │   ├── Color.kt
│       │   └── Type.kt
│       ├── dialer/
│       │   ├── DialerScreen.kt
│       │   └── DialerViewModel.kt
│       ├── voicemail/
│       │   ├── VoicemailListScreen.kt
│       │   ├── VoicemailDetailScreen.kt
│       │   └── VoicemailViewModel.kt
│       ├── rules/
│       │   ├── RulesScreen.kt
│       │   └── RulesViewModel.kt
│       ├── settings/
│       │   ├── SettingsScreen.kt
│       │   └── GreetingSetupScreen.kt
│       └── components/
│           ├── WaveformPlayer.kt
│           └── CallCard.kt
└── AndroidManifest.xml
```

---

## 🔐 Security Rules (Non-Negotiable)

- All voicemail audio files → AES-256-GCM encrypted before writing to disk
- All DB content → SQLCipher with key from Android Keystore
- Keystore alias: `"vaultcall_master_key"`
- Never log sensitive data (phone numbers, transcripts) in production builds
- Biometric prompt required on: app open, viewing transcript, exporting voicemail
- No network calls except for NTP time sync (no user data ever leaves device)

---

## 📋 Build Order

Build modules in this exact order. Each module's instructions are in `/instructions/`:

1. `01_project_setup.md`
2. `02_data_layer.md`
3. `03_security_layer.md`
4. `04_telecom_foundation.md`
5. `05_rules_engine.md`
6. `06_voicemail_engine.md`
7. `07_transcription.md`
8. `08_call_screening.md`
9. `09_dialer_ui.md`
10. `10_settings_greetings.md`
11. `11_notifications_sms.md`
12. `12_polish_build.md`

---

## ✅ Definition of Done for Each Module

Before moving to the next module, verify:
- [ ] Code compiles with zero errors
- [ ] No unresolved imports
- [ ] All Hilt injections are properly annotated
- [ ] Room entities have proper indices
- [ ] Every coroutine runs on correct dispatcher (IO for db/file, Main for UI)
- [ ] No hardcoded strings (use strings.xml)
