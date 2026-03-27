package com.vaultcall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vaultcall.data.model.CallRule
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CallRule] entities.
 *
 * Provides reactive [Flow]-based queries for the rules engine,
 * ordered by priority for correct evaluation order.
 */
@Dao
interface RuleDao {

    /** Get all enabled rules, ordered by priority descending. */
    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getAllEnabled(): Flow<List<CallRule>>

    /** Get all rules (enabled and disabled). */
    @Query("SELECT * FROM rules ORDER BY priority DESC")
    fun getAll(): Flow<List<CallRule>>

    /** Get all enabled rules synchronously (for CallScreeningService binder thread). */
    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getAllEnabledSync(): List<CallRule>

    /** Insert a new rule and return its auto-generated ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CallRule): Long

    /** Update an existing rule. */
    @Update
    suspend fun update(rule: CallRule)

    /** Delete a rule by ID. */
    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: Long)

    /** Enable or disable a rule. */
    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    /** Get a single rule by ID. */
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: Long): CallRule?

    /** Get total count of rules. */
    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getTotalCount(): Int
}
