package com.vaultcall.data.repository

import com.vaultcall.data.db.TranscriptDao
import com.vaultcall.data.model.Transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for transcript operations.
 *
 * Handles saving and retrieval of on-device Whisper transcriptions.
 */
@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao
) {

    /** Save a new transcript. */
    suspend fun saveTranscript(transcript: Transcript) = withContext(Dispatchers.IO) {
        transcriptDao.insert(transcript)
    }

    /** Get the transcript for a specific voicemail. */
    suspend fun getTranscript(voicemailId: Long): Transcript? = withContext(Dispatchers.IO) {
        transcriptDao.getByVoicemailId(voicemailId)
    }

    /** Search transcripts by content. */
    fun searchTranscripts(query: String): Flow<List<Transcript>> =
        transcriptDao.searchTranscripts(query)

    /** Delete transcript for a specific voicemail. */
    suspend fun deleteTranscript(voicemailId: Long) = withContext(Dispatchers.IO) {
        transcriptDao.deleteByVoicemailId(voicemailId)
    }

    /** Get total count of transcripts. */
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        transcriptDao.getTotalCount()
    }
}
