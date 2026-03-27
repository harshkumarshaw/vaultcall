package com.vaultcall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaultcall.data.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CallLogEntry] entities.
 *
 * Provides queries for the in-app call log, separate from
 * the system call log for privacy reasons.
 */
@Dao
interface CallLogDao {

    /** Get recent call log entries, limited to [limit] results. */
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<CallLogEntry>>

    /** Get all call log entries. */
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CallLogEntry>>

    /** Insert a new call log entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CallLogEntry)

    /** Update the note on a call log entry. */
    @Query("UPDATE call_logs SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    /** Delete all call log entries. */
    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()

    /** Get total count of call log entries. */
    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun getTotalCount(): Int

    /** Search call logs by phone number or contact name. */
    @Query(
        "SELECT * FROM call_logs WHERE phoneNumber LIKE '%' || :query || '%' " +
        "OR contactName LIKE '%' || :query || '%' ORDER BY timestamp DESC"
    )
    fun search(query: String): Flow<List<CallLogEntry>>
}
