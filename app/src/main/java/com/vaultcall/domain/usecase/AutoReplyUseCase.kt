package com.vaultcall.domain.usecase

import android.content.Context
import android.telephony.SmsManager
import com.vaultcall.data.model.CallRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for sending auto-reply SMS messages to screened callers.
 *
 * Replaces template variables in the SMS text with actual values:
 * - {caller_name} → resolved contact name or "there"
 * - {time} → current time in h:mm a format
 * - {rule_name} → name of the matched rule
 * - {app_name} → "VaultCall"
 */
class AutoReplyUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Sends an auto-reply SMS.
     *
     * @param phoneNumber The recipient phone number.
     * @param template The SMS template with placeholder variables.
     * @param callerName The resolved contact name, or null.
     * @param rule The rule that triggered the auto-reply.
     */
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

        try {
            @Suppress("DEPRECATION")
            val sms = SmsManager.getDefault()
            sms.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            // SMS send failure — log but don't crash
        }
    }
}
