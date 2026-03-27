package com.vaultcall.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local spam number detector using prefix-based matching.
 *
 * Checks incoming numbers against known spam prefixes stored locally.
 * No network calls — all detection happens on-device.
 */
@Singleton
class SpamDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val spamPrefixes = mutableSetOf<String>()
    private val spamReasons = mutableMapOf<String, String>()
    private val userReportedSpam = mutableSetOf<String>()
    private var isInitialized = false

    /**
     * Initializes the spam detector by loading known spam prefixes.
     */
    fun initialize() {
        if (isInitialized) return

        // Common Indian spam prefixes
        val telemarketers = listOf(
            "1400", "1401", "1402", "1403", "1404", "1405",
            "1406", "1407", "1408", "1409", "1410", "1411",
            "1412", "1413", "1414", "1415", "1416", "1417"
        )
        telemarketers.forEach {
            spamPrefixes.add(it)
            spamReasons[it] = "Telemarketer"
        }

        // Toll-free spam
        val tollFreeSpam = listOf(
            "18001", "18002", "18003", "18004", "18005",
            "18006", "18007", "18008", "18009"
        )
        tollFreeSpam.forEach {
            spamPrefixes.add(it)
            spamReasons[it] = "Suspected Robocall"
        }

        // Short code spam
        val shortCodes = listOf(
            "56", "57", "58", "59"
        )
        shortCodes.forEach {
            spamPrefixes.add(it)
            spamReasons[it] = "Marketing SMS"
        }

        isInitialized = true
    }

    /**
     * Checks if a phone number is suspected spam.
     *
     * @param phoneNumber The phone number to check.
     * @return true if the number matches a known spam prefix or was user-reported.
     */
    fun isSpam(phoneNumber: String): Boolean {
        if (!isInitialized) initialize()

        val normalized = normalizeNumber(phoneNumber)

        // Check user-reported spam
        if (userReportedSpam.contains(normalized)) return true

        // Check prefix matching
        return spamPrefixes.any { normalized.startsWith(it) }
    }

    /**
     * Gets the reason a number is flagged as spam.
     *
     * @param phoneNumber The phone number to check.
     * @return The spam reason string, or null if not spam.
     */
    fun getSpamReason(phoneNumber: String): String? {
        if (!isInitialized) initialize()

        val normalized = normalizeNumber(phoneNumber)

        if (userReportedSpam.contains(normalized)) return "User Reported"

        val matchedPrefix = spamPrefixes.find { normalized.startsWith(it) }
        return matchedPrefix?.let { spamReasons[it] }
    }

    /**
     * Reports a number as spam.
     */
    fun reportAsSpam(phoneNumber: String) {
        userReportedSpam.add(normalizeNumber(phoneNumber))
    }

    /**
     * Removes a number from the user-reported spam list.
     */
    fun unmarkSpam(phoneNumber: String) {
        userReportedSpam.remove(normalizeNumber(phoneNumber))
    }

    private fun normalizeNumber(number: String): String {
        return number.filter { it.isDigit() }
    }
}
