package com.vaultcall.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vaultcall.ai.WhisperTranscriber
import com.vaultcall.data.model.Transcript
import com.vaultcall.data.repository.TranscriptRepository
import com.vaultcall.data.repository.VoicemailRepository
import com.vaultcall.data.security.SecureFileStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray

/**
 * WorkManager worker that runs Whisper transcription in the background.
 *
 * Decrypts the voicemail, transcribes it on-device, extracts keywords,
 * saves the transcript, then cleans up the temporary decrypted file.
 */
@androidx.hilt.work.HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val whisperTranscriber: WhisperTranscriber,
    private val voicemailRepository: VoicemailRepository,
    private val transcriptRepository: TranscriptRepository,
    private val secureFileStorage: SecureFileStorage
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val voicemailId = inputData.getLong("voicemail_id", -1L)
        if (voicemailId == -1L) return Result.failure()

        val voicemail = voicemailRepository.getById(voicemailId) ?: return Result.failure()

        var decryptedFile: java.io.File? = null
        return try {
            decryptedFile = secureFileStorage.getDecryptedForPlayback(voicemail.encryptedFilePath)

            val result = whisperTranscriber.transcribe(decryptedFile)
            val keywords = whisperTranscriber.extractKeywords(result.text)

            val keywordsJson = JSONArray(keywords).toString()

            transcriptRepository.saveTranscript(
                Transcript(
                    voicemailId = voicemailId,
                    text = result.text,
                    language = result.language,
                    confidence = result.confidence,
                    keywords = keywordsJson
                )
            )

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        } finally {
            // ALWAYS clean up temp file
            decryptedFile?.delete()
        }
    }

    companion object {
        /**
         * Enqueues a transcription job for the given voicemail.
         */
        fun enqueue(context: Context, voicemailId: Long) {
            val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(workDataOf("voicemail_id" to voicemailId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
