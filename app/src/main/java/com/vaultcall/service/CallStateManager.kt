package com.vaultcall.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared singleton holding current call state across services and UI.
 *
 * Updated by [MyInCallService] when calls are added/removed/changed,
 * and observed by UI screens for real-time call state display.
 */
@Singleton
class CallStateManager @Inject constructor() {

    /**
     * Represents info about a single active call.
     */
    data class CallInfo(
        val id: String,
        val phoneNumber: String,
        val contactName: String?,
        val state: CallState,
        val isIncoming: Boolean,
        val startTime: Long,
        val isEsim: Boolean? = null
    )

    /**
     * Possible states for a call.
     */
    enum class CallState {
        RINGING,
        DIALING,
        CONNECTING,
        ACTIVE,
        HOLDING,
        DISCONNECTED,
        SCREENING
    }

    private val _activeCalls = MutableStateFlow<Map<String, CallInfo>>(emptyMap())

    /** Observable map of currently active calls, keyed by call ID. */
    val activeCalls: StateFlow<Map<String, CallInfo>> = _activeCalls.asStateFlow()

    /** Add a new call to the active calls map. */
    fun addCall(info: CallInfo) {
        _activeCalls.update { current -> current + (info.id to info) }
    }

    /** Update the state of an existing call. */
    fun updateCallState(id: String, state: CallState) {
        _activeCalls.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(state = state))
        }
    }

    /** Update call details (e.g., resolved contact name). */
    fun updateCallDetails(id: String, phoneNumber: String? = null, contactName: String? = null) {
        _activeCalls.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.copy(
                phoneNumber = phoneNumber ?: existing.phoneNumber,
                contactName = contactName ?: existing.contactName
            ))
        }
    }

    /** Remove a call from the active calls map. */
    fun removeCall(id: String) {
        _activeCalls.update { current -> current - id }
    }

    /** Get info about a specific call. */
    fun getCall(id: String): CallInfo? = _activeCalls.value[id]

    /** Check if there are any active (non-disconnected) calls. */
    fun hasActiveCalls(): Boolean = _activeCalls.value.values.any {
        it.state != CallState.DISCONNECTED
    }
}
