package com.vaultcall.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import com.vaultcall.data.model.RuleAction
import com.vaultcall.domain.rules.RulesEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Custom CallScreeningService that intercepts incoming calls.
 *
 * Evaluates each incoming call against the user's configured rules
 * via [RulesEngine]. Based on the matched rule's action, the call
 * is either allowed through, rejected, or sent to voicemail.
 */
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

        try {
            // ── STIR/SHAKEN Carrier Verification (Android 11+) ──
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (callDetails.callerNumberVerificationStatus == android.telecom.Connection.VERIFICATION_STATUS_FAILED) {
                    val response = CallResponse.Builder()
                        .setRejectCall(true)
                        .setDisallowCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)
                        .build()

                    respondToCall(callDetails, response)
                    return
                }
            }

            // Run rules engine synchronously (CallScreeningService runs on binder thread)
            val matchedRule = rulesEngine.evaluateSync(phoneNumber)

            val response = when (matchedRule?.action) {
                RuleAction.REJECT -> {
                    CallResponse.Builder()
                        .setRejectCall(true)
                        .setDisallowCall(true)
                        .setSkipCallLog(false)
                        .build()
                }

                RuleAction.SEND_TO_VOICEMAIL,
                RuleAction.AUTO_REPLY_SMS,
                RuleAction.ALERT_USER -> {
                    // Signal VoicemailRecorderService to handle this call
                    val intent = Intent("com.vaultcall.SCREEN_CALL").apply {
                        setPackage(packageName)
                        putExtra("phone_number", phoneNumber)
                        putExtra("rule_id", matchedRule.id)
                        putExtra("rule_action", matchedRule.action.name)
                        putExtra("rule_name", matchedRule.name)
                        putExtra("sms_template", matchedRule.smsReplyTemplate)
                        putExtra("greeting_id", matchedRule.greetingId)
                    }
                    sendBroadcast(intent)

                    // Allow call through — VoicemailRecorderService will auto-answer
                    CallResponse.Builder().build()
                }

                null -> {
                    // No rule matched — let the call ring normally
                    CallResponse.Builder().build()
                }
            }

            respondToCall(callDetails, response)

        } catch (e: Exception) {
            // On any error, allow the call through (fail open for safety)
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
