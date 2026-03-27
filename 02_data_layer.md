# Module 02 — Data Layer (Models, DAOs, Database, Repositories)

## Goal
Build the complete data layer: Room entities, DAOs, encrypted database,
and repository implementations.

---

## Task 1 — Data Models

### Voicemail.kt
```kotlin
@Entity(tableName = "voicemails", indices = [Index("callerId"), Index("timestamp")])
data class Voicemail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerId: String,           // Phone number
    val callerName: String?,        // From contacts, nullable
    val timestamp: Long,            // Unix ms
    val durationSeconds: Int,
    val encryptedFilePath: String,  // Path to encrypted .m4a
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val triggerRule: String?,       // Which rule triggered this (nullable = manual)
    val isScreened: Boolean = false // Was this auto-screened?
)
```

### Transcript.kt
```kotlin
@Entity(tableName = "transcripts", indices = [Index("voicemailId")])
data class Transcript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voicemailId: Long,
    val text: String,
    val language: String = "en",
    val confidence: Float,          // 0.0 to 1.0
    val keywords: String,           // JSON array of flagged keywords
    val createdAt: Long = System.currentTimeMillis()
)
```

### CallRule.kt
```kotlin
@Entity(tableName = "rules")
data class CallRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    val startTime: String?,         // "22:00" — HH:mm
    val endTime: String?,           // "07:00" — HH:mm
    val activeDays: String,         // JSON array [0,1,2,3,4,5,6] Sun=0
    val action: RuleAction,
    val smsReplyTemplate: String,   // Supports {caller_name}, {time}
    val greetingId: String?,        // Which greeting to play
    val isEnabled: Boolean = true,
    val priority: Int = 0           // Higher = evaluated first
)

enum class RuleType { NIGHT_HOURS, DND, CALENDAR, CUSTOM_SCHEDULE }
enum class RuleAction { SEND_TO_VOICEMAIL, AUTO_REPLY_SMS, ALERT_USER, REJECT }
```

### CallLogEntry.kt
```kotlin
@Entity(tableName = "call_logs", indices = [Index("phoneNumber"), Index("timestamp")])
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long,
    val durationSeconds: Int,
    val type: CallType,             // INCOMING, OUTGOING, MISSED, SCREENED
    val note: String? = null,
    val simSlot: Int = 0
)

enum class CallType { INCOMING, OUTGOING, MISSED, SCREENED, REJECTED }
```

---

## Task 2 — DAOs

### VoicemailDao.kt
Implement these queries:
- `getAll(): Flow<List<Voicemail>>` — ordered by timestamp DESC
- `getUnread(): Flow<List<Voicemail>>`
- `getById(id: Long): Voicemail?`
- `insert(voicemail: Voicemail): Long`
- `markAsRead(id: Long)`
- `markAsArchived(id: Long)`
- `delete(id: Long)`
- `deleteAll()`
- `getByCallerId(callerId: String): List<Voicemail>`

### TranscriptDao.kt
Implement:
- `getByVoicemailId(voicemailId: Long): Transcript?`
- `insert(transcript: Transcript)`
- `searchTranscripts(query: String): Flow<List<Transcript>>`
  - Use `LIKE '%' || :query || '%'` on text field
- `deleteByVoicemailId(voicemailId: Long)`
- `deleteAll()`

### RuleDao.kt
Implement:
- `getAllEnabled(): Flow<List<CallRule>>` — ordered by priority DESC
- `getAll(): Flow<List<CallRule>>`
- `insert(rule: CallRule): Long`
- `update(rule: CallRule)`
- `delete(id: Long)`
- `setEnabled(id: Long, enabled: Boolean)`

### CallLogDao.kt
Implement:
- `getRecent(limit: Int = 50): Flow<List<CallLogEntry>>`
- `insert(entry: CallLogEntry)`
- `updateNote(id: Long, note: String)`
- `deleteAll()`

---

## Task 3 — AppDatabase.kt

```kotlin
@Database(
    entities = [Voicemail::class, Transcript::class, CallRule::class, CallLogEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voicemailDao(): VoicemailDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun ruleDao(): RuleDao
    abstract fun callLogDao(): CallLogDao
}
```

The database is initialized with SQLCipher in `DatabaseModule.kt` (see Module 03).
Do NOT initialize it here — leave that to Hilt.

Add a `Converters.kt` for any type conversions needed (e.g., List<Int> to JSON String).

---

## Task 4 — Repositories

### VoicemailRepository.kt
- Inject `VoicemailDao` and `TranscriptDao`
- `saveVoicemail(voicemail: Voicemail): Long`
- `getAllVoicemails(): Flow<List<Voicemail>>`
- `getVoicemailWithTranscript(id: Long): VoicemailWithTranscript`
  - Create a data class `VoicemailWithTranscript(voicemail, transcript?)`
- `deleteVoicemail(id: Long)` — also delete encrypted file from disk
- `markRead(id: Long)`
- `searchTranscripts(query: String): Flow<List<Transcript>>`

### RulesRepository.kt
- Inject `RuleDao`
- Full CRUD for rules
- `getEnabledRules(): Flow<List<CallRule>>`

### TranscriptRepository.kt
- Inject `TranscriptDao`
- `saveTranscript(transcript: Transcript)`
- `getTranscript(voicemailId: Long): Transcript?`

---

## Verification Checklist
- [ ] All entities compile with no missing annotations
- [ ] All DAOs have `@Dao` annotation
- [ ] All suspend functions use correct dispatchers in repositories (Dispatchers.IO)
- [ ] Flow queries work correctly
- [ ] Converters handle all custom types
