package com.vaultcall.data.repository

import com.vaultcall.data.db.TranscriptDao
import com.vaultcall.data.db.VoicemailDao
import com.vaultcall.data.model.Transcript
import com.vaultcall.data.model.Voicemail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class combining a voicemail with its optional transcript.
 */
data class VoicemailWithTranscript(
    val voicemail: Voicemail,
    val transcript: Transcript?
)

/**
 * Repository for voicemail operations.
 *
 * Coordinates between [VoicemailDao] and [TranscriptDao], and handles
 * cleanup of encrypted files on disk when voicemails are deleted.
 */
@Singleton
class VoicemailRepository @Inject constructor(
    private val voicemailDao: VoicemailDao,
    private val transcriptDao: TranscriptDao
) {

    /** Save a new voicemail and return its auto-generated ID. */
    suspend fun saveVoicemail(voicemail: Voicemail): Long = withContext(Dispatchers.IO) {
        voicemailDao.insert(voicemail)
    }

    /** Get all voicemails as a reactive Flow. */
    fun getAllVoicemails(): Flow<List<Voicemail>> = voicemailDao.getAll()

    /** Get only unread voicemails. */
    fun getUnreadVoicemails(): Flow<List<Voicemail>> = voicemailDao.getUnread()

    /** Get only archived voicemails. */
    fun getArchivedVoicemails(): Flow<List<Voicemail>> = voicemailDao.getArchived()

    /** Get only screened voicemails. */
    fun getScreenedVoicemails(): Flow<List<Voicemail>> = voicemailDao.getScreened()

    /** Get a single voicemail by ID. */
    suspend fun getById(id: Long): Voicemail? = withContext(Dispatchers.IO) {
        voicemailDao.getById(id)
    }

    /** Get a voicemail with its transcript, if available. */
    suspend fun getVoicemailWithTranscript(id: Long): VoicemailWithTranscript? =
        withContext(Dispatchers.IO) {
            val voicemail = voicemailDao.getById(id) ?: return@withContext null
            val transcript = transcriptDao.getByVoicemailId(id)
            VoicemailWithTranscript(voicemail, transcript)
        }

    /**
     * Delete a voicemail, its transcript, and its encrypted file on disk.
     */
    suspend fun deleteVoicemail(id: Long) = withContext(Dispatchers.IO) {
        val voicemail = voicemailDao.getById(id)
        if (voicemail != null) {
            // Delete encrypted file from disk
            try {
                val file = File(voicemail.encryptedFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Log but don't fail — DB cleanup should still happen
            }
        }
        transcriptDao.deleteByVoicemailId(id)
        voicemailDao.delete(id)
    }

    /** Mark a voicemail as read. */
    suspend fun markRead(id: Long) = withContext(Dispatchers.IO) {
        voicemailDao.markAsRead(id)
    }

    /** Mark a voicemail as archived. */
    suspend fun markArchived(id: Long) = withContext(Dispatchers.IO) {
        voicemailDao.markAsArchived(id)
    }

    /** Search transcripts by text content. */
    fun searchTranscripts(query: String): Flow<List<Transcript>> =
        transcriptDao.searchTranscripts(query)

    /** Get unread voicemail count as a reactive Flow. */
    fun getUnreadCount(): Flow<Int> = voicemailDao.getUnreadCount()

    /** Get total voicemail count. */
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        voicemailDao.getTotalCount()
    }

    /** Delete all voicemails and their transcripts. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        transcriptDao.deleteAll()
        voicemailDao.deleteAll()
    }

    /** Delete voicemails older than the given timestamp. */
    suspend fun deleteOlderThan(beforeTimestamp: Long) = withContext(Dispatchers.IO) {
        val oldVoicemails = voicemailDao.getOlderThan(beforeTimestamp)
        for (vm in oldVoicemails) {
            try {
                File(vm.encryptedFilePath).delete()
            } catch (_: Exception) { }
            transcriptDao.deleteByVoicemailId(vm.id)
        }
        voicemailDao.deleteOlderThan(beforeTimestamp)
    }
}
