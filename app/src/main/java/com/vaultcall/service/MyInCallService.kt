package com.vaultcall.service

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.vaultcall.data.model.CallLogEntry
import com.vaultcall.data.model.CallType
import com.vaultcall.data.repository.CallLogRepository
import com.vaultcall.ui.call.ActiveCallActivity
import com.vaultcall.ui.call.IncomingCallActivity
import com.vaultcall.data.repository.SettingsRepository
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Custom InCallService that manages active phone calls.
 *
 * Registered as the default InCallService when VaultCall is the default dialer.
 * Tracks call state changes, exposes call control methods, and logs calls
 * to the app's private database.
 */
@AndroidEntryPoint
class MyInCallService : InCallService() {

    @Inject lateinit var callStateManager: CallStateManager
    @Inject lateinit var callLogRepository: CallLogRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val calls = mutableMapOf<String, Call>()
    private val callCallbacks = mutableMapOf<String, Call.Callback>()

    // Tracks calls answered automatically by the Voicemail system to suppress foreground UI
    private val autoAnsweredCalls = mutableSetOf<String>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val callId = call.details?.handle?.toString() ?: System.currentTimeMillis().toString()
        val phoneNumber = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING

        var isEsim: Boolean? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val pm = packageManager
            if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SE_OMAPI_ESE)) {
                isEsim = true
            } else if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SE_OMAPI_UICC)) {
                isEsim = false
            }
        }

        calls[callId] = call

        val callInfo = CallStateManager.CallInfo(
            id = callId,
            phoneNumber = phoneNumber,
            contactName = null,
            state = mapCallState(call.state),
            isIncoming = isIncoming,
            startTime = System.currentTimeMillis(),
            isEsim = isEsim
        )

        callStateManager.addCall(callInfo)

        // ── Launch the appropriate call UI ──
        if (isIncoming) {
            // Default Dialers are exempt from Android 10+ Background Activity Restrictions.
            // Directly launch the full-screen IncomingCallActivity rather than using a fragile Notification.
            IncomingCallActivity.launch(
                context = this,
                callId = callId,
                phoneNumber = phoneNumber,
                callerName = null
            )

            // Start auto-answer timer based on user settings
            val rings = settingsRepository.ringsBeforeVoicemail.value
            if (rings > 0) {
                val timeoutMs = rings * 4000L // 4 seconds per ring
                serviceScope.launch {
                    delay(timeoutMs)
                    val currentCall = calls[callId]
                    // If still ringing after timeout, auto-answer for Voicemail
                    if (currentCall != null && currentCall.state == Call.STATE_RINGING) {
                        autoAnsweredCalls.add(callId)
                        currentCall.answer(0)

                        // Send the full explicit payload to instantly boot and execute Voicemail
                        val voicemailIntent = Intent(this@MyInCallService, VoicemailRecorderService::class.java).apply {
                            putExtra("phone_number", phoneNumber)
                            putExtra("rule_id", -1L)
                            putExtra("rule_action", "VOICEMAIL_ONLY")
                            putExtra("rule_name", "Auto Missed Call")
                            putExtra("sms_template", "")
                            putExtra("greeting_id", "default")
                        }
                        try {
                            ContextCompat.startForegroundService(this@MyInCallService, voicemailIntent)
                        } catch (e: Exception) {
                            // Suppress
                        }
                    }
                }
            }

        } else {
            // Outgoing call — go straight to active call screen
            ActiveCallActivity.launch(
                context = this,
                callId = callId,
                phoneNumber = phoneNumber,
                callerName = null
            )
        }

        // Register callback for state changes
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                callStateManager.updateCallState(callId, mapCallState(state))
                handleDualSimSelection(call, state)

                when (state) {
                    Call.STATE_ACTIVE -> {
                        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_INCOMING_CALL)
                        if (!isIncoming) return

                        // If answered externally (e.g. Bluetooth), by user, or by Voicemail, show active screen
                        ActiveCallActivity.launch(
                            context = this@MyInCallService,
                            callId = callId,
                            phoneNumber = phoneNumber,
                            callerName = null
                        )
                    }
                    Call.STATE_DISCONNECTED -> {
                        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_INCOMING_CALL)
                        handleCallEnded(callId, callInfo, call)
                    }
                }
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                super.onDetailsChanged(call, details)
                val number = details.handle?.schemeSpecificPart
                callStateManager.updateCallDetails(
                    id = callId,
                    phoneNumber = number
                )
            }
        }

        callCallbacks[callId] = callback
        call.registerCallback(callback)

        // Evaluate the initial state immediately, as onStateChanged won't fire for the starting state
        handleDualSimSelection(call, call.state)
    }

    private fun handleDualSimSelection(call: Call, state: Int) {
        if (state == Call.STATE_SELECT_PHONE_ACCOUNT) {
            val telecomManager = getSystemService(android.content.Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            var accounts = call.details.intentExtras?.getParcelableArrayList<android.telecom.PhoneAccountHandle>(android.telecom.Call.AVAILABLE_PHONE_ACCOUNTS)
            
            if (accounts == null || accounts.isEmpty()) {
                @Suppress("DEPRECATION", "MissingPermission")
                accounts = ArrayList(telecomManager.callCapablePhoneAccounts)
            }

            if (!accounts.isNullOrEmpty()) {
                // Auto-select the first available SIM to bypass the prompt and immediately route the call
                call.phoneAccountSelected(accounts[0], false)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)

        val callId = calls.entries.find { it.value == call }?.key ?: return

        // Unregister callback
        callCallbacks[callId]?.let { call.unregisterCallback(it) }
        callCallbacks.remove(callId)
        calls.remove(callId)
        autoAnsweredCalls.remove(callId)

        callStateManager.removeCall(callId)
    }

    /**
     * Handles call end: logs the call to the database.
     */
    private fun handleCallEnded(callId: String, callInfo: CallStateManager.CallInfo, call: Call) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val duration = ((System.currentTimeMillis() - callInfo.startTime) / 1000).toInt()
                val callType = when {
                    callInfo.isIncoming && duration > 0 -> CallType.INCOMING
                    callInfo.isIncoming && duration == 0 -> CallType.MISSED
                    else -> CallType.OUTGOING
                }

                callLogRepository.insertCall(
                    CallLogEntry(
                        phoneNumber = callInfo.phoneNumber,
                        contactName = callInfo.contactName,
                        timestamp = callInfo.startTime,
                        durationSeconds = duration,
                        type = callType
                    )
                )
            } catch (e: Exception) {
                // Logging failure shouldn't crash the service
            }
        }
    }

    // ── Call Control Methods ──

    /** Explicitly marks a call as automatically answered by the local voicemail engine to suppress normal Active UI dialogs */
    fun markAsAutoAnswered(callId: String) {
        autoAnsweredCalls.add(callId)
    }

    /** Answer an incoming call. */
    fun answerCall(callId: String) {
        calls[callId]?.answer(0)
    }

    /** Reject/decline an incoming call. */
    fun rejectCall(callId: String) {
        calls[callId]?.reject(false, null)
    }

    /** Put a call on hold. */
    fun holdCall(callId: String) {
        calls[callId]?.hold()
    }

    /** Resume a held call. */
    fun unholdCall(callId: String) {
        calls[callId]?.unhold()
    }

    /** Set the microphone mute state. */
    fun muteCall(muted: Boolean) {
        setMuted(muted)
    }

    /** Toggle speakerphone natively through Telecom audio routes to guarantee caller audibility. */
    fun setSpeakerphone(on: Boolean) {
        @Suppress("DEPRECATION")
        setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
    }

    /** End/hang up a call. */
    fun endCall(callId: String) {
        calls[callId]?.disconnect()
    }

    /** Send a DTMF tone during an active call. */
    fun sendDtmf(callId: String, digit: Char) {
        calls[callId]?.playDtmfTone(digit)
        calls[callId]?.stopDtmfTone()
    }

    /** Get the underlying Call object for a given ID (used by VoicemailRecorderService). */
    fun getCall(callId: String): Call? = calls[callId]

    // ── State Mapping ──

    private fun mapCallState(state: Int): CallStateManager.CallState {
        return when (state) {
            Call.STATE_RINGING, Call.STATE_NEW -> CallStateManager.CallState.RINGING
            Call.STATE_DIALING -> CallStateManager.CallState.DIALING
            Call.STATE_CONNECTING -> CallStateManager.CallState.CONNECTING
            Call.STATE_ACTIVE -> CallStateManager.CallState.ACTIVE
            Call.STATE_HOLDING -> CallStateManager.CallState.HOLDING
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallStateManager.CallState.DISCONNECTED
            Call.STATE_SELECT_PHONE_ACCOUNT -> CallStateManager.CallState.DIALING
            else -> CallStateManager.CallState.RINGING
        }
    }

    companion object {
        /** Static reference to the current instance, used by services that need call control. */
        @Volatile
        var instance: MyInCallService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        calls.clear()
        callCallbacks.clear()
    }
}
