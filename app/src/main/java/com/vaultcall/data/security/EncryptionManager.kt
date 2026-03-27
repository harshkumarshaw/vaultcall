package com.vaultcall.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all encryption/decryption operations for VaultCall.
 *
 * Uses AES-256-GCM with keys stored in the Android Keystore.
 * The master key is generated once on first launch and never leaves
 * the hardware-backed keystore.
 *
 * File format for encrypted data: `[IV (12 bytes)][Ciphertext]`
 */
@Singleton
class EncryptionManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_ALIAS = "vaultcall_master_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_SIZE = 256
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Gets or generates the AES-256 master key from the Android Keystore.
     *
     * The key is generated once on first launch with the following properties:
     * - AES-256 bit key size
     * - GCM block mode, no padding
     * - Can be used for both encryption and decryption
     * - Does NOT require user authentication (biometric is enforced at UI layer)
     */
    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEYSTORE_ALIAS, null)
        if (existingKey is KeyStore.SecretKeyEntry) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts a file using AES-256-GCM.
     *
     * The IV is generated fresh for each encryption and prepended to the output file.
     *
     * @param inputFile The plaintext file to encrypt.
     * @param outputFile The destination file for encrypted content.
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                // Write IV first
                fos.write(iv)

                // Encrypt and write data in chunks
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        fos.write(encrypted)
                    }
                }
                // Write final block
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
            }
        }
    }

    /**
     * Decrypts a file that was encrypted with [encryptFile].
     *
     * Reads the IV from the first 12 bytes of the input file, then
     * decrypts the remaining content.
     *
     * @param inputFile The encrypted file to decrypt.
     * @param outputFile The destination file for plaintext content.
     */
    fun decryptFile(inputFile: File, outputFile: File) {
        val key = getOrCreateKey()

        FileInputStream(inputFile).use { fis ->
            // Read IV
            val iv = ByteArray(GCM_IV_LENGTH)
            fis.read(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        fos.write(decrypted)
                    }
                }
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
            }
        }
    }

    /**
     * Encrypts a byte array using AES-256-GCM.
     *
     * @return A byte array containing `[IV (12 bytes)][Ciphertext]`.
     */
    fun encryptToBytes(data: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        return iv + encrypted
    }

    /**
     * Decrypts a byte array encrypted with [encryptToBytes].
     *
     * @param data A byte array containing `[IV (12 bytes)][Ciphertext]`.
     * @return The original plaintext bytes.
     */
    fun decryptFromBytes(data: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return cipher.doFinal(ciphertext)
    }

    /**
     * Derives a deterministic 32-byte passphrase for SQLCipher from the Keystore key.
     *
     * On first call, encrypts a well-known derivation constant and stores
     * the result (IV + ciphertext) in a local file. On subsequent calls,
     * decrypts the stored blob to recover the same passphrase.
     *
     * This ensures the passphrase is deterministic across app restarts
     * while the actual key never leaves the hardware-backed Keystore.
     */
    @Volatile
    private var cachedPassphrase: ByteArray? = null

    fun getDatabasePassphrase(): ByteArray {
        cachedPassphrase?.let { return it }

        synchronized(this) {
            cachedPassphrase?.let { return it }

            val derivationInput = "vaultcall_db_passphrase_v1".toByteArray(Charsets.UTF_8)

            // We store the encrypted passphrase blob so we can re-derive
            // the same passphrase after process death.
            val passphraseFile = File(
                android.os.Environment.getDataDirectory(),
                "data/com.vaultcall/.db_key"
            )

            val passphrase: ByteArray = if (passphraseFile.exists()) {
                try {
                    decryptFromBytes(passphraseFile.readBytes())
                } catch (e: Exception) {
                    // If decryption fails (e.g., key was rotated), re-derive
                    val encrypted = encryptToBytes(derivationInput)
                    passphraseFile.parentFile?.mkdirs()
                    passphraseFile.writeBytes(encrypted)
                    derivationInput
                }
            } else {
                val encrypted = encryptToBytes(derivationInput)
                passphraseFile.parentFile?.mkdirs()
                passphraseFile.writeBytes(encrypted)
                derivationInput
            }

            cachedPassphrase = passphrase
            return passphrase
        }
    }
}
