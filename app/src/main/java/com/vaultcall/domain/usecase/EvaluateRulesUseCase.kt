package com.vaultcall.domain.usecase

import com.vaultcall.data.model.CallRule
import com.vaultcall.data.model.RuleAction
import com.vaultcall.domain.rules.RulesEngine
import javax.inject.Inject

/**
 * Use case for evaluating call screening rules against an incoming call.
 *
 * Wraps the [RulesEngine] to provide a clean domain-layer API
 * with a structured result type.
 */
class EvaluateRulesUseCase @Inject constructor(
    private val rulesEngine: RulesEngine
) {

    /**
     * Evaluates the rules engine for the given phone number.
     *
     * @param phoneNumber The incoming caller's phone number.
     * @return A [RuleEvaluationResult] indicating whether to intercept and how.
     */
    suspend operator fun invoke(phoneNumber: String): RuleEvaluationResult {
        val matchedRule = rulesEngine.evaluate(phoneNumber)
        return RuleEvaluationResult(
            matchedRule = matchedRule,
            action = matchedRule?.action,
            shouldIntercept = matchedRule != null
        )
    }

    /**
     * Result of evaluating the rules engine.
     *
     * @property matchedRule The rule that matched, or null if no rule matches.
     * @property action The action to take, or null if the call should ring normally.
     * @property shouldIntercept Whether the call should be intercepted.
     */
    data class RuleEvaluationResult(
        val matchedRule: CallRule?,
        val action: RuleAction?,
        val shouldIntercept: Boolean
    )
}
