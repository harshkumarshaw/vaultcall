package com.vaultcall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vaultcall.data.model.CallLogEntry
import com.vaultcall.data.model.CallRule
import com.vaultcall.data.model.Transcript
import com.vaultcall.data.model.Voicemail

/**
 * VaultCall Room database.
 *
 * All data is encrypted at rest via SQLCipher. The database is initialized
 * in [com.vaultcall.di.DatabaseModule] with a passphrase derived from the
 * Android Keystore.
 *
 * Entities:
 * - [Voicemail] — recorded voicemail metadata
 * - [Transcript] — on-device AI transcriptions
 * - [CallRule] — user-configured call screening rules
 * - [CallLogEntry] — private call history
 */
@Database(
    entities = [
        Voicemail::class,
        Transcript::class,
        CallRule::class,
        CallLogEntry::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /** Access voicemail data operations. */
    abstract fun voicemailDao(): VoicemailDao

    /** Access transcript data operations. */
    abstract fun transcriptDao(): TranscriptDao

    /** Access call rule data operations. */
    abstract fun ruleDao(): RuleDao

    /** Access call log data operations. */
    abstract fun callLogDao(): CallLogDao
}
