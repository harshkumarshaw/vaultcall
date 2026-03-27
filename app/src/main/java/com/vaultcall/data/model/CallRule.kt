package com.vaultcall.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a call screening rule configured by the user.
 *
 * Rules are evaluated in order of [priority] (highest first) by the [RulesEngine].
 * When an incoming call matches a rule, the corresponding [action] is taken.
 *
 * @property name User-friendly name for the rule (e.g., "Night Mode").
 * @property type The trigger condition type for this rule.
 * @property startTime Start time in HH:mm format (for NIGHT_HOURS, CUSTOM_SCHEDULE).
 * @property endTime End time in HH:mm format (for NIGHT_HOURS, CUSTOM_SCHEDULE).
 * @property activeDays JSON array of active day indices [0..6], where Sunday = 0.
 * @property action What to do when this rule matches an incoming call.
 * @property smsReplyTemplate SMS template text; supports {caller_name}, {time}, {app_name}.
 * @property greetingId Identifier for which greeting audio to play, or null for default.
 * @property isEnabled Whether this rule is currently active.
 * @property priority Evaluation priority (higher = checked first).
 */
@Entity(tableName = "rules")
data class CallRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    val startTime: String?,
    val endTime: String?,
    val activeDays: String,
    val action: RuleAction,
    val smsReplyTemplate: String,
    val greetingId: String?,
    val isEnabled: Boolean = true,
    val priority: Int = 0
)

/**
 * Types of call screening rules.
 */
enum class RuleType {
    /** Screen calls during specific nighttime hours. */
    NIGHT_HOURS,
    /** Screen calls when system Do Not Disturb is active. */
    DND,
    /** Screen calls when a calendar event with busy status is active. */
    CALENDAR,
    /** Screen calls on a custom day+time schedule. */
    CUSTOM_SCHEDULE
}

/**
 * Actions to take when a rule matches an incoming call.
 */
enum class RuleAction {
    /** Auto-answer, play greeting, and record voicemail. */
    SEND_TO_VOICEMAIL,
    /** Send an SMS auto-reply to the caller. */
    AUTO_REPLY_SMS,
    /** Let the call ring but notify the user with the rule reason. */
    ALERT_USER,
    /** Silently reject the call. */
    REJECT
}
