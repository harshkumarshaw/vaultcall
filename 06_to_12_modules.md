# Module 06 — Voicemail Engine

## Goal
Record, store, and play back voicemails with a full visual inbox.

---

## Task 1 — VoicemailRecorderService.kt

Foreground service that handles auto-answer + recording:

### Flow:
1. Receive broadcast `com.vaultcall.SCREEN_CALL` from `MyCallScreeningService`
2. Start as Foreground Service with notification "Screening call from [number]"
3. Wait for `InCallService` to confirm call is connected
4. Play greeting audio via `AudioTrack` or `MediaPlayer` (earpiece channel)
5. Start `MediaRecorder` to record caller response:
   ```kotlin
   recorder.apply {
       setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
       setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
       setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
       setAudioSamplingRate(16000)
       setAudioEncodingBitRate(32000)
       setMaxDuration(60_000) // 60 second max
       setOutputFile(tempFile.absolutePath)
       prepare()
       start()
   }
   ```
6. After `RECORDING_TIMEOUT` (30s default) or caller hangs up — stop recording
7. Hang up call via `InCallService`
8. Pass raw file to `SecureFileStorage.saveVoicemail()` → get encrypted path
9. Save `Voicemail` entity to DB
10. Trigger transcription job via WorkManager
11. Show "New voicemail from [number]" notification

### For unanswered calls (not screened):
- Monitor call state via `CallStateManager`
- After configurable ring count (default 4), check if user answered
- If not answered, auto-answer + record (same flow as above)

---

## Task 2 — VoicemailListScreen.kt

```
┌─────────────────────────────────┐
│ 🔒 VaultCall    [Search] [⋮]    │
├─────────────────────────────────┤
│ Voicemails (3 unread)           │
├─────────────────────────────────┤
│ ● +91 98765 43210    2 min ago  │
│   Ravi Kumar  • 0:42            │
│   "Hi, I wanted to discuss..."  │  ← transcript preview
│   [▶] [📞] [🗑]                  │
├─────────────────────────────────┤
│   +1 (555) 234-5678  1 hr ago   │
│   Unknown  • 1:15               │
│   ⚠️ "urgent callback needed"   │  ← keyword highlight
│   [▶] [📞] [🗑]                  │
└─────────────────────────────────┘
```

Features:
- Swipe left to delete, swipe right to mark read
- Tap to open detail screen
- Search icon → full-text search screen
- Filter chips: All / Unread / Screened / Archived

---

## Task 3 — VoicemailDetailScreen.kt

```
┌─────────────────────────────────┐
│ ← +91 98765 43210               │
│   Ravi Kumar                    │
│   Today, 2:34 PM • 0:42         │
│   📋 Screened (Night Mode rule) │
├─────────────────────────────────┤
│ [Waveform visualization]        │
│ ━━━━●━━━━━━━━━━━━━━━━━━━━━━━   │
│ 0:18          0:42              │
│ [⏮] [⏪] [▶] [⏩] [⏭]          │
│      0.5x  1x  1.5x  2x         │
├─────────────────────────────────┤
│ TRANSCRIPT                      │
│ "Hi, I wanted to discuss the    │
│ meeting tomorrow about the      │
│ urgent project deadline..."     │
├─────────────────────────────────┤
│ KEYWORDS DETECTED               │
│ [urgent] [meeting] [deadline]   │
├─────────────────────────────────┤
│ [📞 Call Back] [💬 Reply] [⤴ Share] │
└─────────────────────────────────┘
```

### Playback Implementation:
- Decrypt voicemail to temp file via `SecureFileStorage.getDecryptedForPlayback()`
- Use `MediaPlayer` for playback
- Build waveform by sampling audio amplitudes during recording (store as JSON in DB)
- Clean up temp file in `onCleared()` of ViewModel

---

## Verification Checklist
- [ ] Recording starts after greeting plays
- [ ] 60-second max recording enforced
- [ ] Encrypted file saved correctly
- [ ] Voicemail appears in inbox
- [ ] Playback decrypts and plays correctly
- [ ] Speed control works (0.5x–2x)
- [ ] Waveform displays (can be fake/placeholder initially)
- [ ] Swipe gestures work on list items

---

---

# Module 07 — On-Device Transcription (Whisper ONNX)

## Goal
Transcribe every voicemail locally using Whisper tiny ONNX. No audio
ever leaves the device.

---

## Task 1 — Setup

1. Download `whisper-tiny.onnx` — place in `app/src/main/assets/whisper-tiny.onnx`
2. Add to build.gradle: `aaptOptions { noCompress "onnx" }`
3. Load model once at app start using `OrtEnvironment` + `OrtSession`

---

## Task 2 — AudioPreprocessor.kt

Convert .m4a file to Whisper-compatible float array:

```kotlin
object AudioPreprocessor {
    const val SAMPLE_RATE = 16000
    const val MAX_DURATION_SECONDS = 30

    // Returns float array of audio samples, 16kHz mono, normalized -1.0 to 1.0
    suspend fun extractFloatArray(audioFile: File): FloatArray {
        // Use MediaExtractor + AudioTrack to decode .m4a
        // Resample to 16000 Hz if needed
        // Convert to mono if stereo
        // Pad or trim to MAX_DURATION_SECONDS * SAMPLE_RATE
    }

    // Compute log-mel spectrogram (80 mel bins, 3000 frames)
    fun computeMelSpectrogram(samples: FloatArray): Array<FloatArray>
}
```

---

## Task 3 — WhisperTranscriber.kt

```kotlin
@Singleton
class WhisperTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ortSession: OrtSession? = null

    fun initialize() {
        val env = OrtEnvironment.getEnvironment()
        val modelBytes = context.assets.open("whisper-tiny.onnx").readBytes()
        ortSession = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    suspend fun transcribe(audioFile: File): TranscriptionResult {
        return withContext(Dispatchers.Default) {
            val samples = AudioPreprocessor.extractFloatArray(audioFile)
            val mel = AudioPreprocessor.computeMelSpectrogram(samples)

            // Run ONNX inference
            // Input name: "input_features" — shape [1, 80, 3000]
            // Output: token IDs → decode to text

            TranscriptionResult(
                text = decodedText,
                language = detectedLanguage,
                confidence = avgLogProb
            )
        }
    }

    data class TranscriptionResult(
        val text: String,
        val language: String,
        val confidence: Float
    )

    // Detect flagged keywords in transcript
    fun extractKeywords(text: String): List<String> {
        val keywords = listOf(
            "urgent", "emergency", "callback", "call back", "meeting",
            "important", "asap", "deadline", "help", "problem", "issue"
        )
        return keywords.filter { text.lowercase().contains(it) }
    }
}
```

---

## Task 4 — TranscriptionWorker.kt

WorkManager worker that runs transcription in background:

```kotlin
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val whisperTranscriber: WhisperTranscriber,
    private val voicemailRepository: VoicemailRepository,
    private val transcriptRepository: TranscriptRepository,
    private val secureFileStorage: SecureFileStorage
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val voicemailId = inputData.getLong("voicemail_id", -1L)
        if (voicemailId == -1L) return Result.failure()

        val voicemail = voicemailRepository.getById(voicemailId) ?: return Result.failure()
        val decryptedFile = secureFileStorage.getDecryptedForPlayback(voicemail.encryptedFilePath)

        return try {
            val result = whisperTranscriber.transcribe(decryptedFile)
            val keywords = whisperTranscriber.extractKeywords(result.text)

            transcriptRepository.saveTranscript(Transcript(
                voicemailId = voicemailId,
                text = result.text,
                language = result.language,
                confidence = result.confidence,
                keywords = Json.encodeToString(keywords)
            ))
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Transcription failed for voicemail $voicemailId")
            Result.retry()
        } finally {
            decryptedFile.delete() // Always clean up temp file
        }
    }
}
```

### Enqueue transcription:
```kotlin
val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
    .setInputData(workDataOf("voicemail_id" to voicemailId))
    .setConstraints(Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build())
    .build()
WorkManager.getInstance(context).enqueue(request)
```

---

## Verification Checklist
- [ ] ONNX model loads without crash
- [ ] AudioPreprocessor produces correct 16kHz float array
- [ ] Transcription completes within 10 seconds for 30s audio on mid-range device
- [ ] Keywords correctly extracted and stored as JSON
- [ ] Temp file deleted after transcription
- [ ] Worker retries on failure
- [ ] Transcript shows in VoicemailDetailScreen

---

---

# Module 08 — Auto-Answer + Call Screening Flow

## Goal
Complete the auto-answer pipeline: answer call, play greeting,
record caller, trigger post-processing.

---

## Task 1 — GreetingPlayer.kt

```kotlin
@Singleton
class GreetingPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Play TTS greeting during active call
    suspend fun playTTSGreeting(text: String, onComplete: () -> Unit)

    // Play recorded greeting audio file
    suspend fun playRecordedGreeting(filePath: String, onComplete: () -> Unit)

    // Choose correct greeting for rule type
    fun getGreetingForRule(rule: CallRule): GreetingConfig
}
```

### TTS Implementation:
```kotlin
private fun speak(text: String, tts: TextToSpeech, onComplete: () -> Unit) {
    val utteranceId = UUID.randomUUID().toString()
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onDone(utteranceId: String?) { onComplete() }
        override fun onError(utteranceId: String?) { onComplete() } // fail gracefully
        override fun onStart(utteranceId: String?) {}
    })
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
}
```

---

## Task 2 — Complete Screening Flow in VoicemailRecorderService

```
Broadcast received (SCREEN_CALL)
    │
    ▼
Start Foreground Service
    │
    ▼
Wait for call ACTIVE state (timeout: 10s)
    │
    ▼
Set audio to VOICE_COMMUNICATION mode
    │
    ▼
Play greeting (TTS or recorded)
    │
    ▼
Play "beep" tone (500ms, 440Hz)
    │
    ▼
Start MediaRecorder (30s max)
    │
    ▼
Wait for recording complete OR caller hangup
    │
    ▼
Stop recorder, hang up call
    │
    ▼
Encrypt file → save to DB
    │
    ▼
Trigger: TranscriptionWorker, SMSAutoReply (if needed), Notification
```

---

## Task 3 — AutoReplyUseCase.kt

```kotlin
class AutoReplyUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendAutoReply(
        phoneNumber: String,
        template: String,
        callerName: String?,
        rule: CallRule
    ) {
        val message = template
            .replace("{caller_name}", callerName ?: "there")
            .replace("{time}", SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()))
            .replace("{rule_name}", rule.name)
            .replace("{app_name}", "VaultCall")

        val sms = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber, null, message, null, null)
    }
}
```

---

## Task 4 — Screening Notification

Show a rich notification after screening completes:

```
┌─────────────────────────────────┐
│ 📱 VaultCall — Call Screened    │
│ +91 98765 43210 (Ravi Kumar)    │
│ "Wanted to discuss the project" │  ← first line of transcript
│ [Call Back] [View Voicemail]    │
└─────────────────────────────────┘
```

- Priority: HIGH
- Channel: "call_screened"
- Pending intents for both action buttons
- Show even on lock screen

---

## Verification Checklist
- [ ] Greeting plays clearly during call
- [ ] Beep tone plays after greeting
- [ ] Recording captures caller's voice
- [ ] Call ends automatically after recording
- [ ] SMS sent when action = AUTO_REPLY_SMS
- [ ] Notification appears with transcript preview
- [ ] Call Back action button works

---

---

# Module 09 — Dialer UI

## Goal
Build the full custom dialer: keypad, recents, quick-dial, call notes,
dual SIM support, spam detection.

---

## Task 1 — DialerScreen.kt

Bottom navigation with 3 tabs:
1. **Keypad** — number pad + dial button
2. **Recents** — recent calls list
3. **Contacts** — contact search

### Keypad Tab:
```
┌─────────────────────────────────┐
│                                 │
│      +91 98765 43210            │  ← editable number display
│      [⌫]                        │
├─────────────────────────────────┤
│   1      2 ABC   3 DEF          │
│   4 GHI  5 JKL   6 MNO          │
│   7 PQRS 8 TUV   9 WXYZ         │
│   *      0 +     #              │
├─────────────────────────────────┤
│  [SIM1▼]    [📞 Call]   [+Add]  │
└─────────────────────────────────┘
```

Features:
- Haptic feedback on each key press
- DTMF tone on each key press
- Long press 0 → insert "+"
- Long press 1 → voicemail (if configured)
- Backspace + long press backspace = clear all
- Paste number from clipboard on number field tap
- SIM selector shown only on dual-SIM devices

### Recents Tab:
- Group by date (Today, Yesterday, This Week)
- Color code: Green = outgoing, Blue = incoming, Red = missed, Purple = screened
- Tap → call immediately
- Long press → options (call, message, add note, block, copy number)
- Show spam badge (⚠️) on known spam numbers

### Quick Dial Section (above keypad):
- Up to 8 pinned contacts shown as circular avatars
- Long press to add/remove pins
- Shown on keypad tab when number field is empty

---

## Task 2 — Call Notes

After every call ends, show a bottom sheet:
```
┌─────────────────────────────────┐
│ Add note for Ravi Kumar         │
│ ┌─────────────────────────────┐ │
│ │ Discussed Q3 project...     │ │
│ └─────────────────────────────┘ │
│ [Skip]              [Save Note] │
└─────────────────────────────────┘
```

- Auto-dismiss after 30 seconds if no action
- Notes searchable in recents
- Show note icon on call log entries that have notes

---

## Task 3 — Active Call Screen

```
┌─────────────────────────────────┐
│  Ravi Kumar                     │
│  +91 98765 43210                │
│  0:42  ●                        │  ← live timer
│  [SIM 1 — Airtel]               │
├─────────────────────────────────┤
│  [🔇 Mute] [⌨️ Keypad] [📢 Speaker] │
│  [⏸ Hold]  [📞 Add Call] [↩ Swap]  │
├─────────────────────────────────┤
│          [🔴 End Call]          │
└─────────────────────────────────┘
```

---

## Task 4 — SpamDetector.kt

```kotlin
@Singleton
class SpamDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Load spam number list from assets/spam_numbers.csv on init
    // Simple prefix-based matching for common spam prefixes
    fun isSpam(phoneNumber: String): Boolean
    fun getSpamReason(phoneNumber: String): String? // "Telemarketer", "Robocall", etc.

    // Allow users to report spam
    fun reportAsSpam(phoneNumber: String)
    fun unmarkSpam(phoneNumber: String)
}
```

Bundle `assets/spam_numbers.csv` with common spam prefixes:
- Include 100–200 known Indian spam prefixes (140XXXXXXX, 1800XXXXXX series)

---

## Verification Checklist
- [ ] Keypad dials correctly
- [ ] DTMF tones play on key press
- [ ] Haptic feedback on key press
- [ ] Active call screen shows with live timer
- [ ] Mute, speaker, hold work
- [ ] Quick dial pins save and load
- [ ] Call notes save and show on recents
- [ ] Spam numbers show warning badge
- [ ] Dual SIM selector appears on dual SIM device

---

---

# Module 10 — Settings & Custom Greetings

## Goal
Settings screen with all app preferences and a greeting management system.

---

## Task 1 — SettingsScreen.kt

Sections:
1. **Account & Security**
   - App lock: ON/OFF toggle + PIN/Biometric selector
   - Auto-lock timer: Immediately / 1 min / 5 min / 30 min
   - Change PIN

2. **Voicemail Settings**
   - Max recording length: 30s / 60s / 90s / 120s
   - Auto-delete voicemails after: Never / 7 days / 30 days / 90 days
   - Auto-transcribe: ON/OFF

3. **Greetings**
   - Manage greetings → GreetingSetupScreen

4. **Call Screening**
   - Screening enabled: ON/OFF master toggle
   - Rings before voicemail: 2 / 3 / 4 / 5

5. **Dialer**
   - Theme: Light / Dark / AMOLED
   - Accent color: Color picker (5 presets)
   - Haptic feedback: ON/OFF
   - DTMF tones: ON/OFF
   - Show quick dial: ON/OFF

6. **Privacy**
   - View privacy report → PrivacyReportScreen
   - Export all data (transcripts as .txt)
   - Wipe all data → confirmation dialog → calls `SecureFileStorage.wipeAllVoicemails()` + `db.clearAllTables()`

7. **About**
   - Version, licenses, open source credits

---

## Task 2 — GreetingSetupScreen.kt

```
┌─────────────────────────────────┐
│ Greetings                       │
├─────────────────────────────────┤
│ DEFAULT GREETING                │
│ "This call is being screened..."│
│ [▶ Preview] [✏️ Edit] [🎤 Record]│
├─────────────────────────────────┤
│ DND GREETING                    │
│ Using default                   │
│ [+ Customize]                   │
├─────────────────────────────────┤
│ NIGHT MODE GREETING             │
│ Custom recorded                 │
│ [▶ Preview] [✏️ Edit] [🎤 Record]│
├─────────────────────────────────┤
│ MEETING GREETING                │
│ Using default                   │
│ [+ Customize]                   │
└─────────────────────────────────┘
```

### Greeting Editor:
- Tab 1: TTS — type text, preview with play button, save
- Tab 2: Record — record button (hold to record), waveform display, preview, save
- Character limit: 300 characters for TTS

### Greeting Storage:
Store greetings in `data/greetings/` directory (encrypted)
Greeting metadata in DataStore:
```
KEY_DEFAULT_GREETING_TYPE = "tts" | "recorded"
KEY_DEFAULT_GREETING_TEXT = "..."
KEY_DEFAULT_GREETING_PATH = "..."  // only for recorded
// Repeat for dnd_, night_, meeting_ prefixes
```

---

## Task 3 — PrivacyReportScreen.kt

Show user exactly what data the app has:
- Total voicemails: X
- Total transcripts: X
- Storage used: X MB
- Oldest voicemail date
- Rules configured: X
- Call log entries: X

All data stays on device — zero analytics, zero crash reporting (unless opted in).

---

## Verification Checklist
- [ ] All settings persist via DataStore
- [ ] Theme change applies immediately without restart
- [ ] Recording greeting saves and plays back correctly
- [ ] TTS greeting previews correctly
- [ ] Wipe all data removes all files and clears DB
- [ ] Auto-delete runs via WorkManager on schedule

---

---

# Module 11 — Notifications & SMS

## Goal
Notification channels, rich notifications, SMS auto-reply,
and the home screen widget.

---

## Task 1 — NotificationHelper.kt

```kotlin
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SCREENED = "call_screened"
        const val CHANNEL_VOICEMAIL = "new_voicemail"
        const val CHANNEL_FOREGROUND = "screening_service"
        const val CHANNEL_MISSED = "missed_call"
    }

    fun createChannels() // Call on app start

    fun showVoicemailNotification(voicemail: Voicemail, transcript: Transcript?)
    fun showScreenedCallNotification(voicemail: Voicemail, reasonSnippet: String)
    fun showMissedCallNotification(phoneNumber: String, callerName: String?)
    fun showForegroundServiceNotification(): Notification // For screening service
    fun cancelNotification(id: Int)
}
```

### Screened Call Notification:
- Large icon: contact photo or default avatar
- Title: "Call screened from [name/number]"
- Text: First 80 chars of transcript
- Action 1: "Call Back" — launches dialer with number
- Action 2: "View Voicemail" — opens voicemail detail
- Show on lock screen (visibility = PUBLIC for non-sensitive, PRIVATE for transcript)

---

## Task 2 — VoicemailWidget.kt

AppWidget showing last 3 voicemails:

```
┌─────────────────────────────────┐
│ 🔒 VaultCall  Voicemails        │
├─────────────────────────────────┤
│ ● Ravi Kumar    2m ago   0:42   │
│ ● Unknown       1h ago   1:15   │
│ ● +1800-123     3h ago   0:28 ⚠│
└─────────────────────────────────┘
```

- Update every 15 minutes via `AppWidgetManager`
- Tap row → open voicemail detail in app
- Tap header → open app main screen

---

## Verification Checklist
- [ ] Notification channels created with correct importance levels
- [ ] Rich notification shows after call screening
- [ ] Call Back action correctly launches dialer
- [ ] SMS sent to correct number with substituted template
- [ ] Widget updates when new voicemail arrives
- [ ] Notifications respect Android 13+ permission requirement

---

---

# Module 12 — Polish, Build & Local Install

## Goal
Final polish, release APK build, and local device installation.

---

## Task 1 — Onboarding Flow

Create a clean 4-step onboarding (if not already done in Module 04):
- Step 1: Welcome + app overview
- Step 2: Permission requests (one by one with explanations)
- Step 3: Request default dialer
- Step 4: Record or set up first greeting

Track onboarding completion in DataStore. Skip on subsequent launches.

---

## Task 2 — Empty States

Every list screen needs a proper empty state:
- VoicemailListScreen: mic icon + "No voicemails yet. Calls will be recorded here."
- RulesScreen: shield icon + "No rules yet. Add a rule to start screening calls."
- RecentsScreen: clock icon + "No recent calls."

---

## Task 3 — ProGuard Rules

Add to `proguard-rules.pro`:
```
# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
```

---

## Task 4 — Release Build

In `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        // Sign with debug key for local testing
        signingConfig = signingConfigs.getByName("debug")
    }
}
```

Build command:
```bash
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

---

## Task 5 — Install & Test Script

Create `test_install.sh`:
```bash
#!/bin/bash
echo "Building release APK..."
./gradlew assembleRelease

echo "Installing on connected device..."
adb install -r app/build/outputs/apk/release/app-release.apk

echo "Launching app..."
adb shell am start -n com.vaultcall/.ui.MainActivity

echo "Done! Check device."
```

---

## Final Test Checklist

```
CORE FEATURES:
□ App installs without crash
□ Onboarding completes successfully
□ Default dialer request works
□ All permissions granted
□ Incoming call → rings normally (no rules)
□ Incoming call → auto-screened (night rule active)
□ Greeting plays during screened call
□ Voicemail recorded and saved
□ Transcript generated (check after ~30s)
□ Notification appears with transcript snippet
□ SMS auto-reply sent
□ Voicemail plays back correctly
□ Speed control works

SECURITY:
□ Biometric prompt on app open
□ Voicemail files are not plain .m4a (check in file browser)
□ Wipe data removes everything

DIALER:
□ Outgoing call works
□ Keypad DTMF tones play
□ Active call screen shows
□ Mute/speaker/hold work
□ Call log entry saved after call
□ Call note saves successfully

SETTINGS:
□ Greeting records and plays back
□ Dark/AMOLED theme applies
□ Rules can be created/edited/deleted
□ Auto-delete schedule set
```

---

## Known Issues to Document

Before calling v1.0 done, document these known limitations in README:

1. Call recording quality varies by OEM (best on Samsung, limited on Pixel)
2. Carrier voicemail not replaced when phone is off/no signal
3. Whisper tiny may struggle with heavy accents or noise
4. Must be set as default dialer for all features to work
5. Calendar rule requires calendar permissions — some users may deny
