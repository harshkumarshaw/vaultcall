package com.vaultcall.domain.rules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages whitelist and blacklist of phone numbers.
 *
 * Whitelisted numbers always ring through regardless of rules.
 * Blacklisted numbers are always rejected.
 * Data is stored in DataStore as JSON sets.
 */
@Singleton
class WhitelistBlacklistManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_WHITELIST = stringSetPreferencesKey("whitelist_numbers")
        private val KEY_BLACKLIST = stringSetPreferencesKey("blacklist_numbers")
    }

    /** Add a phone number to the whitelist. */
    suspend fun addToWhitelist(phoneNumber: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_WHITELIST] ?: emptySet()
            prefs[KEY_WHITELIST] = current + normalizeNumber(phoneNumber)
        }
    }

    /** Add a phone number to the blacklist. */
    suspend fun addToBlacklist(phoneNumber: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST] ?: emptySet()
            prefs[KEY_BLACKLIST] = current + normalizeNumber(phoneNumber)
        }
    }

    /** Remove a phone number from the whitelist. */
    suspend fun removeFromWhitelist(phoneNumber: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_WHITELIST] ?: emptySet()
            prefs[KEY_WHITELIST] = current - normalizeNumber(phoneNumber)
        }
    }

    /** Remove a phone number from the blacklist. */
    suspend fun removeFromBlacklist(phoneNumber: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST] ?: emptySet()
            prefs[KEY_BLACKLIST] = current - normalizeNumber(phoneNumber)
        }
    }

    /** Check if a phone number is whitelisted. */
    suspend fun isWhitelisted(phoneNumber: String): Boolean {
        val whitelist = dataStore.data.first()[KEY_WHITELIST] ?: emptySet()
        val normalized = normalizeNumber(phoneNumber)
        return whitelist.any { normalizeNumber(it) == normalized }
    }

    /** Check if a phone number is blacklisted. */
    suspend fun isBlacklisted(phoneNumber: String): Boolean {
        val blacklist = dataStore.data.first()[KEY_BLACKLIST] ?: emptySet()
        val normalized = normalizeNumber(phoneNumber)
        return blacklist.any { normalizeNumber(it) == normalized }
    }

    /** Get the whitelist as a reactive Flow. */
    fun getWhitelist(): Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_WHITELIST] ?: emptySet()
    }

    /** Get the blacklist as a reactive Flow. */
    fun getBlacklist(): Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_BLACKLIST] ?: emptySet()
    }

    /**
     * Normalizes a phone number by removing non-digit characters
     * except the leading '+' for international format.
     */
    private fun normalizeNumber(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        return if (cleaned.startsWith("+")) cleaned else cleaned.takeLast(10)
    }
}
