package com.vaultcall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaultcall.data.model.Voicemail
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Voicemail] entities.
 *
 * Provides reactive [Flow]-based queries for the voicemail inbox
 * and suspend functions for write operations.
 */
@Dao
interface VoicemailDao {

    /** Get all voicemails ordered by newest first. */
    @Query("SELECT * FROM voicemails ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Voicemail>>

    /** Get only unread voicemails ordered by newest first. */
    @Query("SELECT * FROM voicemails WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnread(): Flow<List<Voicemail>>

    /** Get only archived voicemails ordered by newest first. */
    @Query("SELECT * FROM voicemails WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchived(): Flow<List<Voicemail>>

    /** Get only screened voicemails ordered by newest first. */
    @Query("SELECT * FROM voicemails WHERE isScreened = 1 ORDER BY timestamp DESC")
    fun getScreened(): Flow<List<Voicemail>>

    /** Get a single voicemail by its ID. */
    @Query("SELECT * FROM voicemails WHERE id = :id")
    suspend fun getById(id: Long): Voicemail?

    /** Insert a new voicemail and return its auto-generated ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voicemail: Voicemail): Long

    /** Mark a voicemail as read. */
    @Query("UPDATE voicemails SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /** Mark a voicemail as archived. */
    @Query("UPDATE voicemails SET isArchived = 1 WHERE id = :id")
    suspend fun markAsArchived(id: Long)

    /** Delete a single voicemail by ID. */
    @Query("DELETE FROM voicemails WHERE id = :id")
    suspend fun delete(id: Long)

    /** Delete all voicemails from the database. */
    @Query("DELETE FROM voicemails")
    suspend fun deleteAll()

    /** Get all voicemails from a specific caller. */
    @Query("SELECT * FROM voicemails WHERE callerId = :callerId ORDER BY timestamp DESC")
    suspend fun getByCallerId(callerId: String): List<Voicemail>

    /** Get count of unread voicemails. */
    @Query("SELECT COUNT(*) FROM voicemails WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    /** Get total count of voicemails. */
    @Query("SELECT COUNT(*) FROM voicemails")
    suspend fun getTotalCount(): Int

    /** Get voicemails older than a given timestamp (for auto-delete). */
    @Query("SELECT * FROM voicemails WHERE timestamp < :beforeTimestamp")
    suspend fun getOlderThan(beforeTimestamp: Long): List<Voicemail>

    /** Delete voicemails older than a given timestamp. */
    @Query("DELETE FROM voicemails WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
