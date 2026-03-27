# Module 05 — Rules Engine

## Goal
Build the smart rules engine that evaluates incoming calls against
user-defined rules and returns the correct action.

---

## Task 1 — RulesEngine.kt

Location: `domain/rules/RulesEngine.kt`

```kotlin
@Singleton
class RulesEngine @Inject constructor(
    private val rulesRepository: RulesRepository,
    @ApplicationContext private val context: Context
) {
    // Suspend version for async use
    suspend fun evaluate(phoneNumber: String): CallRule?

    // Sync version for CallScreeningService (runs on binder thread)
    fun evaluateSync(phoneNumber: String): CallRule?

    private fun isWhitelisted(phoneNumber: String): Boolean
    private fun isBlacklisted(phoneNumber: String): Boolean
    private fun isNightHoursActive(rule: CallRule): Boolean
    private fun isDndActive(): Boolean
    private fun isCalendarEventActive(): Boolean
    private fun isCustomScheduleActive(rule: CallRule): Boolean
}
```

### Evaluation Logic (in order):
```
1. If number is WHITELISTED → return null (ring normally)
2. If number is BLACKLISTED → return REJECT rule
3. For each enabled rule (ordered by priority DESC):
   a. Check rule type:
      - NIGHT_HOURS: compare current time to startTime/endTime, check activeDays
      - DND: check NotificationManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL
      - CALENDAR: query CalendarProvider for active events with busy status
      - CUSTOM_SCHEDULE: compare current day+time to rule schedule
   b. If rule matches → return that rule
4. No match → return null
```

### Night Hours Logic:
```kotlin
private fun isNightHoursActive(rule: CallRule): Boolean {
    val now = LocalTime.now()
    val start = LocalTime.parse(rule.startTime) // e.g. "22:00"
    val end = LocalTime.parse(rule.endTime)     // e.g. "07:00"
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

    val activeDays = Json.decodeFromString<List<Int>>(rule.activeDays)
    if (today !in activeDays) return false

    // Handle overnight ranges (start > end means crosses midnight)
    return if (start.isAfter(end)) {
        now.isAfter(start) || now.isBefore(end)
    } else {
        now.isAfter(start) && now.isBefore(end)
    }
}
```

### DND Check:
```kotlin
private fun isDndActive(): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
}
```

### Calendar Check:
```kotlin
private fun isCalendarEventActive(): Boolean {
    val now = System.currentTimeMillis()
    val projection = arrayOf(
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.AVAILABILITY
    )
    val selection = "${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.AVAILABILITY} = ?"
    val selectionArgs = arrayOf(now.toString(), now.toString(), CalendarContract.Events.AVAILABILITY_BUSY.toString())

    context.contentResolver.query(
        CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, null
    )?.use { cursor ->
        return cursor.count > 0
    }
    return false
}
```

---

## Task 2 — EvaluateRulesUseCase.kt

```kotlin
class EvaluateRulesUseCase @Inject constructor(
    private val rulesEngine: RulesEngine
) {
    suspend operator fun invoke(phoneNumber: String): RuleEvaluationResult

    data class RuleEvaluationResult(
        val matchedRule: CallRule?,
        val action: RuleAction?,
        val shouldIntercept: Boolean
    )
}
```

---

## Task 3 — RulesScreen.kt (Compose UI)

Build a full rules management screen:

### Rules List
- Show all rules as cards with: name, type icon, enabled toggle, edit/delete buttons
- Empty state: "No rules yet. Add your first rule to start screening calls."
- FAB to add new rule

### Rule Editor (Bottom Sheet or separate screen)
Fields:
- **Name** — text field
- **Rule Type** — segmented control: Night Hours / DND / Calendar / Custom Schedule
- **Time Range** — time pickers for start/end (shown for Night Hours + Custom Schedule)
- **Active Days** — day selector chips (Mon–Sun) for Custom Schedule
- **Action** — radio buttons: Send to Voicemail / Auto-Reply SMS / Alert Me / Reject
- **SMS Reply Text** — shown only when action = AUTO_REPLY_SMS
  - Hint: "Available variables: {caller_name}, {time}, {app_name}"
  - Default: "I'm unavailable right now. I'll call you back soon."
- **Priority** — slider 0–10

### Pre-built Rule Templates (shown on empty state)
Offer quick-add templates:
- 🌙 "Night Mode" — 10 PM to 7 AM, all days, send to voicemail
- 📅 "Meeting Mode" — Calendar-based, alert user with reason
- 🤫 "DND Mode" — DND-based, auto-reply SMS

---

## Task 4 — WhitelistBlacklistManager.kt

```kotlin
@Singleton
class WhitelistBlacklistManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Store as JSON sets in DataStore
    suspend fun addToWhitelist(phoneNumber: String)
    suspend fun addToBlacklist(phoneNumber: String)
    suspend fun removeFromWhitelist(phoneNumber: String)
    suspend fun removeFromBlacklist(phoneNumber: String)
    suspend fun isWhitelisted(phoneNumber: String): Boolean
    suspend fun isBlacklisted(phoneNumber: String): Boolean
    fun getWhitelist(): Flow<Set<String>>
    fun getBlacklist(): Flow<Set<String>>
}
```

---

## Verification Checklist
- [ ] Night hours rule correctly handles midnight crossover (e.g. 22:00–07:00)
- [ ] DND rule activates when phone DND is ON
- [ ] Calendar rule reads active busy events
- [ ] Rules are evaluated by priority
- [ ] Whitelist bypasses all rules
- [ ] Blacklist rejects call immediately
- [ ] RulesScreen shows all rules and allows CRUD
- [ ] SMS template variables are substituted correctly
