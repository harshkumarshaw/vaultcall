# Module 04 — Telecom Foundation (InCallService + Default Dialer)

## Goal
Build the core telecom layer: MyInCallService, MyCallScreeningService,
default dialer request flow, and call state management.

---

## Task 1 — CallStateManager.kt

Shared singleton that holds current call state across services and UI:

```kotlin
@Singleton
class CallStateManager @Inject constructor() {
    // Current active calls (keyed by call ID)
    private val _activeCalls = MutableStateFlow<Map<String, CallInfo>>(emptyMap())
    val activeCalls: StateFlow<Map<String, CallInfo>> = _activeCalls.asStateFlow()

    data class CallInfo(
        val id: String,
        val phoneNumber: String,
        val contactName: String?,
        val state: CallState,
        val isIncoming: Boolean,
        val startTime: Long
    )

    enum class CallState { RINGING, ACTIVE, HOLDING, DISCONNECTED, SCREENING }

    fun addCall(info: CallInfo)
    fun updateCallState(id: String, state: CallState)
    fun removeCall(id: String)
    fun getCall(id: String): CallInfo?
}
```

---

## Task 2 — MyInCallService.kt

Extends `android.telecom.InCallService`:

```kotlin
@AndroidEntryPoint
class MyInCallService : InCallService() {

    @Inject lateinit var callStateManager: CallStateManager
    @Inject lateinit var callLogRepository: CallLogRepository

    // Map of telecom Call objects
    private val calls = mutableMapOf<String, Call>()

    override fun onCallAdded(call: Call) {
        // Register callback, update CallStateManager
        // Notify UI via CallStateManager flow
    }

    override fun onCallRemoved(call: Call) {
        // Clean up, log call to DB
    }

    // Expose control methods
    fun answerCall(callId: String)
    fun rejectCall(callId: String)
    fun holdCall(callId: String)
    fun muteCall(muted: Boolean)
    fun setSpeakerphone(on: Boolean)
    fun endCall(callId: String)
    fun sendDtmf(callId: String, digit: Char)
}
```

### Call Callback Implementation
Register a `Call.Callback` for each call that updates `CallStateManager` on:
- `onStateChanged` → map telecom states to your `CallState` enum
- `onDetailsChanged` → update caller ID, contact name

---

## Task 3 — MyCallScreeningService.kt

Extends `android.telecom.CallScreeningService`:

```kotlin
@AndroidEntryPoint
class MyCallScreeningService : CallScreeningService() {

    @Inject lateinit var rulesEngine: RulesEngine
    @Inject lateinit var callStateManager: CallStateManager

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        if (!isIncoming) {
            // Never screen outgoing calls
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Run rules engine
        val matchedRule = rulesEngine.evaluateSync(phoneNumber)

        val response = when (matchedRule?.action) {
            RuleAction.REJECT -> CallResponse.Builder()
                .setRejectCall(true)
                .setDisallowCall(false)
                .build()

            RuleAction.SEND_TO_VOICEMAIL,
            RuleAction.AUTO_REPLY_SMS,
            RuleAction.ALERT_USER -> {
                // Signal VoicemailRecorderService to handle this call
                sendBroadcast(Intent("com.vaultcall.SCREEN_CALL").apply {
                    putExtra("phone_number", phoneNumber)
                    putExtra("rule_id", matchedRule.id)
                    putExtra("rule_action", matchedRule.action.name)
                })
                // Allow call through — VoicemailRecorderService will auto-answer
                CallResponse.Builder().build()
            }

            null -> CallResponse.Builder().build() // No rule — ring normally
        }

        respondToCall(callDetails, response)
    }
}
```

---

## Task 4 — DefaultDialerHelper.kt

```kotlin
object DefaultDialerHelper {

    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecom.defaultDialerPackage == context.packageName
    }

    fun requestDefaultDialer(activity: Activity) {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
        }
        activity.startActivity(intent)
    }

    fun makeCall(context: Context, phoneNumber: String, simSlot: Int = 0) {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.parse("tel:$phoneNumber")
        val bundle = Bundle().apply {
            // SIM slot selection if dual SIM
            if (simSlot > 0) putInt("com.android.phone.extra.SLOT_ID", simSlot)
        }
        telecom.placeCall(uri, bundle)
    }
}
```

---

## Task 5 — OnboardingScreen.kt

First-launch screen that walks user through setup:

### Step 1: Welcome
- App name, brief description
- "Get Started" button

### Step 2: Permissions
Show each required permission with explanation, request button:
- Phone (ANSWER_PHONE_CALLS, READ_PHONE_STATE, CALL_PHONE)
- Microphone (RECORD_AUDIO)
- Contacts (READ_CONTACTS)
- Calendar (READ_CALENDAR)
- SMS (SEND_SMS)
- Notifications (POST_NOTIFICATIONS — Android 13+)

Use `rememberPermissionState` from Accompanist or manual `ActivityResultLauncher`.

### Step 3: Default Dialer
- Explain why default dialer is needed
- "Set as Default Dialer" button → calls `DefaultDialerHelper.requestDefaultDialer()`
- Check and show green checkmark if already set

### Step 4: Done
- Summary of what's set up
- "Go to VaultCall" → navigate to main app

---

## Verification Checklist
- [ ] App requests default dialer on onboarding
- [ ] InCallService receives calls when app is default dialer
- [ ] CallScreeningService intercepts incoming calls
- [ ] CallStateManager updates correctly on call state changes
- [ ] makeCall() places an outgoing call correctly
- [ ] Rejecting a call via response works
