# Module 03 — Security Layer

## Goal
Implement AES-256-GCM encryption for all voicemail files, SQLCipher for the
database, Android Keystore key management, and biometric authentication.

---

## Task 1 — EncryptionManager.kt

Location: `data/security/EncryptionManager.kt`

Implement a singleton (Hilt @Singleton) that:

### Key Management
- Uses Android Keystore with alias `"vaultcall_master_key"`
- Generates AES-256 key with `KeyGenParameterSpec`:
  - Purpose: ENCRYPT + DECRYPT
  - Block mode: GCM
  - Padding: NONE
  - Key size: 256 bits
  - `setUserAuthenticationRequired(false)` — key accessible without auth
    (biometric is handled at UI layer, not key layer)
- Key is generated once on first launch, never regenerated

### File Encryption
```kotlin
fun encryptFile(inputFile: File, outputFile: File)
fun decryptFile(inputFile: File, outputFile: File)
fun encryptToBytes(data: ByteArray): ByteArray  // returns IV + ciphertext
fun decryptFromBytes(data: ByteArray): ByteArray // expects IV + ciphertext
```

### Implementation Notes
- GCM IV is 12 bytes, generated fresh for each encryption
- Store IV prepended to ciphertext: `[IV (12 bytes)][Ciphertext]`
- Use `Cipher.getInstance("AES/GCM/NoPadding")`

### DB Passphrase
```kotlin
fun getDatabasePassphrase(): ByteArray
// Derives a 32-byte passphrase from Keystore key for SQLCipher
// Use KeyAgreement or HKDF-style derivation
// Cache result in memory — do not re-derive on every DB op
```

---

## Task 2 — DatabaseModule.kt (Hilt)

Location: `di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase {
        val passphrase = encryptionManager.getDatabasePassphrase()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, AppDatabase::class.java, "vaultcall.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideVoicemailDao(db: AppDatabase) = db.voicemailDao()
    @Provides fun provideTranscriptDao(db: AppDatabase) = db.transcriptDao()
    @Provides fun provideRuleDao(db: AppDatabase) = db.ruleDao()
    @Provides fun provideCallLogDao(db: AppDatabase) = db.callLogDao()
}
```

---

## Task 3 — BiometricManager.kt

Location: `data/security/BiometricManager.kt`

Implement a class (Hilt @Singleton) that wraps `BiometricPrompt`:

```kotlin
class VaultBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Verify Identity",
        subtitle: String = "Authenticate to access VaultCall",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    )

    fun isAvailable(): Boolean
    // Check BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
}
```

### Prompt Config
- Title: "VaultCall — Verify Identity"
- Negative button: "Use PIN"
- Allow device credential as fallback
- Confirmation required: false (fingerprint is instant)

---

## Task 4 — SecureFileStorage.kt

Location: `data/security/SecureFileStorage.kt`

Helper class for managing encrypted voicemail files:

```kotlin
class SecureFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    // Returns the private app storage dir for voicemails
    fun getVoicemailDir(): File

    // Save raw recorded audio, encrypts it, returns encrypted file path
    // Deletes the raw temp file after encryption
    suspend fun saveVoicemail(rawFile: File, voicemailId: Long): String

    // Decrypts to a temp file for playback, returns temp file path
    // Caller must delete temp file after playback
    suspend fun getDecryptedForPlayback(encryptedPath: String): File

    // Permanently delete encrypted voicemail file
    fun deleteVoicemail(encryptedPath: String): Boolean

    // Return total size of all voicemail files in bytes
    fun getTotalStorageUsed(): Long

    // Wipe ALL voicemail files from disk
    fun wipeAllVoicemails()
}
```

---

## Task 5 — AppLockManager.kt

Manage app lock state (session-based, not per-screen):

```kotlin
class AppLockManager @Inject constructor() {
    private var isUnlocked = false
    private var unlockTimestamp = 0L
    private val sessionDurationMs = 5 * 60 * 1000L // 5 min session

    fun unlock() { isUnlocked = true; unlockTimestamp = System.currentTimeMillis() }
    fun lock() { isUnlocked = false }
    fun isSessionValid(): Boolean // checks isUnlocked && within session window
    fun requiresAuth(): Boolean = !isSessionValid()
}
```

---

## Verification Checklist
- [ ] Keystore key generates on first launch, persists across app restarts
- [ ] Encrypt then decrypt a test file — verify bytes match original
- [ ] SQLCipher DB opens with derived passphrase
- [ ] Biometric prompt shows on emulator/device
- [ ] Temp decrypted file is cleaned up after playback
- [ ] wipeAllVoicemails() removes all files in voicemail dir
