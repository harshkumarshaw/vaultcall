package com.vaultcall.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encrypted voicemail file storage.
 *
 * All voicemail recordings are encrypted with AES-256-GCM before
 * writing to the app's private storage directory. Temporary decrypted
 * files are created for playback and must be cleaned up by the caller.
 */
@Singleton
class SecureFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {

    /**
     * Returns the private app storage directory for encrypted voicemails.
     * Creates the directory if it doesn't exist.
     */
    fun getVoicemailDir(): File {
        val dir = File(context.filesDir, "voicemails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Encrypts and saves a raw recorded audio file.
     *
     * After encryption, the raw temp file is deleted to prevent
     * unencrypted audio from persisting on disk.
     *
     * @param rawFile The unencrypted recording file.
     * @param voicemailId The database ID of the voicemail entry.
     * @return The absolute path to the encrypted voicemail file.
     */
    suspend fun saveVoicemail(rawFile: File, voicemailId: Long): String =
        withContext(Dispatchers.IO) {
            val encryptedFile = File(getVoicemailDir(), "vm_${voicemailId}.enc")

            try {
                encryptionManager.encryptFile(rawFile, encryptedFile)
            } finally {
                // Always delete the raw unencrypted file
                rawFile.delete()
            }

            encryptedFile.absolutePath
        }

    /**
     * Decrypts a voicemail to a temporary file for playback.
     *
     * **IMPORTANT**: The caller MUST delete the returned temp file after playback
     * to prevent unencrypted audio from persisting on disk.
     *
     * @param encryptedPath Absolute path to the encrypted voicemail file.
     * @return A temporary File containing the decrypted audio.
     */
    suspend fun getDecryptedForPlayback(encryptedPath: String): File =
        withContext(Dispatchers.IO) {
            val encryptedFile = File(encryptedPath)
            val tempFile = File(context.cacheDir, "playback_${System.currentTimeMillis()}.m4a")

            encryptionManager.decryptFile(encryptedFile, tempFile)

            tempFile
        }

    /**
     * Permanently deletes an encrypted voicemail file from disk.
     *
     * @param encryptedPath Absolute path to the encrypted file.
     * @return true if the file was successfully deleted.
     */
    fun deleteVoicemail(encryptedPath: String): Boolean {
        return try {
            File(encryptedPath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculates the total storage used by all encrypted voicemail files.
     *
     * @return Total size in bytes.
     */
    fun getTotalStorageUsed(): Long {
        val dir = getVoicemailDir()
        if (!dir.exists()) return 0L

        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Wipes ALL voicemail files from the private storage directory.
     *
     * Used for the "Wipe All Data" privacy feature.
     */
    fun wipeAllVoicemails() {
        val dir = getVoicemailDir()
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }

        // Also clean up any temp playback files in cache
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("playback_")) {
                file.delete()
            }
        }
    }
}
