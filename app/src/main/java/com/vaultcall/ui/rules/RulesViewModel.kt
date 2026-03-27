package com.vaultcall.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultcall.data.model.CallRule
import com.vaultcall.data.model.RuleAction
import com.vaultcall.data.model.RuleType
import com.vaultcall.data.repository.RulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Rules management screen.
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val rulesRepository: RulesRepository
) : ViewModel() {

    /** All rules as reactive state. */
    val allRules = rulesRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            rulesRepository.setRuleEnabled(id, enabled)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            rulesRepository.deleteRule(id)
        }
    }

    fun addRule(rule: CallRule) {
        viewModelScope.launch {
            rulesRepository.insertRule(rule)
        }
    }

    fun updateRule(rule: CallRule) {
        viewModelScope.launch {
            rulesRepository.updateRule(rule)
        }
    }

    /** Quick-add the Night Mode template. */
    fun addNightModeTemplate() {
        addRule(
            CallRule(
                name = "Night Mode",
                type = RuleType.NIGHT_HOURS,
                startTime = "22:00",
                endTime = "07:00",
                activeDays = "[0,1,2,3,4,5,6]",
                action = RuleAction.SEND_TO_VOICEMAIL,
                smsReplyTemplate = "I'm sleeping. I'll call you back later.",
                greetingId = "night",
                isEnabled = true,
                priority = 5
            )
        )
    }

    /** Quick-add the Meeting Mode template. */
    fun addMeetingModeTemplate() {
        addRule(
            CallRule(
                name = "Meeting Mode",
                type = RuleType.CALENDAR,
                startTime = null,
                endTime = null,
                activeDays = "[1,2,3,4,5]",
                action = RuleAction.ALERT_USER,
                smsReplyTemplate = "I'm in a meeting. I'll call you back soon.",
                greetingId = "meeting",
                isEnabled = true,
                priority = 3
            )
        )
    }

    /** Quick-add the DND Mode template. */
    fun addDndModeTemplate() {
        addRule(
            CallRule(
                name = "DND Mode",
                type = RuleType.DND,
                startTime = null,
                endTime = null,
                activeDays = "[0,1,2,3,4,5,6]",
                action = RuleAction.AUTO_REPLY_SMS,
                smsReplyTemplate = "I'm unavailable right now. I'll call you back soon.",
                greetingId = "dnd",
                isEnabled = true,
                priority = 4
            )
        )
    }
}
