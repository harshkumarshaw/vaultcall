package com.vaultcall.domain.rules

import android.app.NotificationManager
import android.content.Context
import android.provider.CalendarContract
import com.vaultcall.data.model.CallRule
import com.vaultcall.data.model.RuleAction
import com.vaultcall.data.model.RuleType
import com.vaultcall.data.repository.RulesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call screening rules engine.
 *
 * Evaluates incoming calls against user-configured rules in priority order.
 * Supports night hours, DND, calendar, and custom schedule rule types.
 *
 * Evaluation order:
 * 1. Whitelisted numbers → always ring
 * 2. Blacklisted numbers → always reject
 * 3. Enabled rules by priority (highest first) → first match wins
 * 4. No match → ring normally
 */
@Singleton
class RulesEngine @Inject constructor(
    private val rulesRepository: RulesRepository,
    private val whitelistBlacklistManager: WhitelistBlacklistManager,
    @ApplicationContext private val context: Context
) {

    /**
     * Evaluate rules asynchronously.
     *
     * @param phoneNumber The incoming caller's phone number.
     * @return The matched [CallRule], or null if no rule matches (ring normally).
     */
    suspend fun evaluate(phoneNumber: String): CallRule? {
        // Check whitelist first
        if (whitelistBlacklistManager.isWhitelisted(phoneNumber)) {
            return null
        }

        // Check blacklist
        if (whitelistBlacklistManager.isBlacklisted(phoneNumber)) {
            return CallRule(
                id = -1,
                name = "Blacklisted",
                type = RuleType.CUSTOM_SCHEDULE,
                startTime = null,
                endTime = null,
                activeDays = "[]",
                action = RuleAction.REJECT,
                smsReplyTemplate = "",
                greetingId = null,
                isEnabled = true,
                priority = Int.MAX_VALUE
            )
        }

        // Evaluate enabled rules by priority
        val enabledRules = rulesRepository.getEnabledRules().first()
        for (rule in enabledRules) {
            if (isRuleActive(rule)) {
                return rule
            }
        }

        return null
    }

    /**
     * Evaluate rules synchronously (for CallScreeningService binder thread).
     *
     * @param phoneNumber The incoming caller's phone number.
     * @return The matched [CallRule], or null if no rule matches.
     */
    fun evaluateSync(phoneNumber: String): CallRule? {
        return runBlocking {
            try {
                // Check whitelist
                if (whitelistBlacklistManager.isWhitelisted(phoneNumber)) {
                    return@runBlocking null
                }

                // Check blacklist
                if (whitelistBlacklistManager.isBlacklisted(phoneNumber)) {
                    return@runBlocking CallRule(
                        id = -1,
                        name = "Blacklisted",
                        type = RuleType.CUSTOM_SCHEDULE,
                        startTime = null,
                        endTime = null,
                        activeDays = "[]",
                        action = RuleAction.REJECT,
                        smsReplyTemplate = "",
                        greetingId = null,
                        isEnabled = true,
                        priority = Int.MAX_VALUE
                    )
                }

                // Get enabled rules synchronously
                val enabledRules = rulesRepository.getEnabledRulesSync()
                for (rule in enabledRules) {
                    if (isRuleActive(rule)) {
                        return@runBlocking rule
                    }
                }

                null
            } catch (e: Exception) {
                null // Fail open — let the call through
            }
        }
    }

    /**
     * Checks if a rule's conditions are currently active.
     */
    private fun isRuleActive(rule: CallRule): Boolean {
        return when (rule.type) {
            RuleType.NIGHT_HOURS -> isNightHoursActive(rule)
            RuleType.DND -> isDndActive()
            RuleType.CALENDAR -> isCalendarEventActive()
            RuleType.CUSTOM_SCHEDULE -> isCustomScheduleActive(rule)
        }
    }

    /**
     * Checks if current time falls within the night hours range.
     *
     * Correctly handles midnight crossover (e.g., 22:00–07:00).
     */
    private fun isNightHoursActive(rule: CallRule): Boolean {
        if (rule.startTime == null || rule.endTime == null) return false

        val now = LocalTime.now()
        val start = LocalTime.parse(rule.startTime)
        val end = LocalTime.parse(rule.endTime)

        // Check if today is an active day
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
        val activeDays = parseActiveDays(rule.activeDays)
        if (today !in activeDays) return false

        // Handle midnight crossover (start > end means spans midnight)
        return if (start.isAfter(end)) {
            now.isAfter(start) || now.isBefore(end)
        } else {
            now.isAfter(start) && now.isBefore(end)
        }
    }

    /**
     * Checks if system Do Not Disturb mode is active.
     */
    private fun isDndActive(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    /**
     * Checks if there's an active calendar event with busy status.
     */
    private fun isCalendarEventActive(): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.AVAILABILITY
            )
            val selection = "${CalendarContract.Events.DTSTART} <= ? AND " +
                    "${CalendarContract.Events.DTEND} >= ? AND " +
                    "${CalendarContract.Events.AVAILABILITY} = ?"
            val selectionArgs = arrayOf(
                now.toString(),
                now.toString(),
                CalendarContract.Events.AVAILABILITY_BUSY.toString()
            )

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: SecurityException) {
            // Calendar permission not granted
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if current day+time falls within a custom schedule rule.
     */
    private fun isCustomScheduleActive(rule: CallRule): Boolean {
        if (rule.startTime == null || rule.endTime == null) return false

        val now = LocalTime.now()
        val start = LocalTime.parse(rule.startTime)
        val end = LocalTime.parse(rule.endTime)

        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        val activeDays = parseActiveDays(rule.activeDays)
        if (today !in activeDays) return false

        return if (start.isAfter(end)) {
            now.isAfter(start) || now.isBefore(end)
        } else {
            now.isAfter(start) && now.isBefore(end)
        }
    }

    /**
     * Parses the JSON array of active day indices.
     */
    private fun parseActiveDays(json: String): List<Int> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
