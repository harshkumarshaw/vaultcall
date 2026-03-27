package com.vaultcall.service

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.vaultcall.data.model.CallLogEntry
import com.vaultcall.data.model.CallType
import com.vaultcall.data.repository.CallLogRepository
import com.vaultcall.ui.call.ActiveCallActivity
import com.vaultcall.ui.call.IncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val calls = mutableMapOf<String, Call>()
    private val callCallbacks = mutableMapOf<String, Call.Callback>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val callId = call.details?.handle?.toString() ?: System.currentTimeMillis().toString()
        val phoneNumber = call.details?.handle?.schemeSpecificPart ?: ""
        val isIncoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING

        calls[callId] = call

        val callInfo = CallStateManager.CallInfo(
            id = callId,
            phoneNumber = phoneNumber,
            contactName = null,
            state = mapCallState(call.state),
            isIncoming = isIncoming,
            startTime = System.currentTimeMillis()
        )

        callStateManager.addCall(callInfo)

        // ── Launch the appropriate call UI ──
        if (isIncoming) {
            // Show full-screen incoming call UI (works on lock screen)
            IncomingCallActivity.launch(
                context = this,
                callId = callId,
                phoneNumber = phoneNumber,
                callerName = null
            )
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

                when (state) {
                    Call.STATE_ACTIVE -> {
                        // If answered externally (e.g. Bluetooth), show active screen
                        if (!isIncoming) return
                        ActiveCallActivity.launch(
                            context = this@MyInCallService,
                            callId = callId,
                            phoneNumber = phoneNumber,
                            callerName = null
                        )
                    }
                    Call.STATE_DISCONNECTED -> handleCallEnded(callId, callInfo, call)
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
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)

        val callId = calls.entries.find { it.value == call }?.key ?: return

        // Unregister callback
        callCallbacks[callId]?.let { call.unregisterCallback(it) }
        callCallbacks.remove(callId)
        calls.remove(callId)

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

    /** Toggle speakerphone. */
    fun setSpeakerphone(on: Boolean) {
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
