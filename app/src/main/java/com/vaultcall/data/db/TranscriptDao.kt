package com.vaultcall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaultcall.data.model.Transcript
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Transcript] entities.
 *
 * Provides queries for voicemail transcriptions, including
 * full-text search across transcript content.
 */
@Dao
interface TranscriptDao {

    /** Get the transcript for a specific voicemail. */
    @Query("SELECT * FROM transcripts WHERE voicemailId = :voicemailId LIMIT 1")
    suspend fun getByVoicemailId(voicemailId: Long): Transcript?

    /** Insert a new transcript. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: Transcript)

    /** Search transcripts by text content (case-insensitive substring match). */
    @Query("SELECT * FROM transcripts WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTranscripts(query: String): Flow<List<Transcript>>

    /** Delete transcript for a specific voicemail. */
    @Query("DELETE FROM transcripts WHERE voicemailId = :voicemailId")
    suspend fun deleteByVoicemailId(voicemailId: Long)

    /** Delete all transcripts. */
    @Query("DELETE FROM transcripts")
    suspend fun deleteAll()

    /** Get total count of transcripts. */
    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTotalCount(): Int
}
