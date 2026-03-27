package com.vaultcall.data.repository

import com.vaultcall.data.db.CallLogDao
import com.vaultcall.data.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for call log operations.
 *
 * Maintains a private call log independent from the system call log.
 */
@Singleton
class CallLogRepository @Inject constructor(
    private val callLogDao: CallLogDao
) {

    /** Get recent call log entries. */
    fun getRecentCalls(limit: Int = 50): Flow<List<CallLogEntry>> = callLogDao.getRecent(limit)

    /** Get all call log entries. */
    fun getAllCalls(): Flow<List<CallLogEntry>> = callLogDao.getAll()

    /** Insert a new call log entry. */
    suspend fun insertCall(entry: CallLogEntry) = withContext(Dispatchers.IO) {
        callLogDao.insert(entry)
    }

    /** Update the note on a call. */
    suspend fun updateNote(id: Long, note: String) = withContext(Dispatchers.IO) {
        callLogDao.updateNote(id, note)
    }

    /** Delete all call log entries. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        callLogDao.deleteAll()
    }

    /** Get total count of call log entries. */
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        callLogDao.getTotalCount()
    }

    /** Search call logs by phone number or contact name. */
    fun searchCalls(query: String): Flow<List<CallLogEntry>> = callLogDao.search(query)
}
