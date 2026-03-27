package com.vaultcall.data.repository

import com.vaultcall.data.db.RuleDao
import com.vaultcall.data.model.CallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for call screening rule operations.
 *
 * Provides full CRUD and enables the rules engine to fetch
 * enabled rules in priority order.
 */
@Singleton
class RulesRepository @Inject constructor(
    private val ruleDao: RuleDao
) {

    /** Get all enabled rules as a reactive Flow, ordered by priority. */
    fun getEnabledRules(): Flow<List<CallRule>> = ruleDao.getAllEnabled()

    /** Get all enabled rules synchronously (used by CallScreeningService). */
    fun getEnabledRulesSync(): List<CallRule> = ruleDao.getAllEnabledSync()

    /** Get all rules (enabled and disabled). */
    fun getAllRules(): Flow<List<CallRule>> = ruleDao.getAll()

    /** Insert a new rule and return its ID. */
    suspend fun insertRule(rule: CallRule): Long = withContext(Dispatchers.IO) {
        ruleDao.insert(rule)
    }

    /** Update an existing rule. */
    suspend fun updateRule(rule: CallRule) = withContext(Dispatchers.IO) {
        ruleDao.update(rule)
    }

    /** Delete a rule by ID. */
    suspend fun deleteRule(id: Long) = withContext(Dispatchers.IO) {
        ruleDao.delete(id)
    }

    /** Toggle a rule's enabled state. */
    suspend fun setRuleEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        ruleDao.setEnabled(id, enabled)
    }

    /** Get a single rule by ID. */
    suspend fun getRuleById(id: Long): CallRule? = withContext(Dispatchers.IO) {
        ruleDao.getById(id)
    }

    /** Get total count of rules. */
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        ruleDao.getTotalCount()
    }
}
