package com.vaultcall.di

import android.content.Context
import androidx.room.Room
import com.vaultcall.data.db.AppDatabase
import com.vaultcall.data.db.CallLogDao
import com.vaultcall.data.db.RuleDao
import com.vaultcall.data.db.TranscriptDao
import com.vaultcall.data.db.VoicemailDao
import com.vaultcall.data.security.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAOs.
 *
 * The database is encrypted with SQLCipher using a passphrase
 * derived from the Android Keystore via [EncryptionManager].
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the SQLCipher-encrypted Room database.
     *
     * The passphrase is derived from the Android Keystore master key
     * and cached in memory by the EncryptionManager.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): AppDatabase {
        val passphrase = encryptionManager.getDatabasePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vaultcall.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideVoicemailDao(db: AppDatabase): VoicemailDao = db.voicemailDao()

    @Provides
    fun provideTranscriptDao(db: AppDatabase): TranscriptDao = db.transcriptDao()

    @Provides
    fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideCallLogDao(db: AppDatabase): CallLogDao = db.callLogDao()
}
