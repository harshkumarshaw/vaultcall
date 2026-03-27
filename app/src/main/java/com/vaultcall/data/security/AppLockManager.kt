package com.vaultcall.data.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app lock state using a session-based approach.
 *
 * After successful biometric/PIN authentication, the app remains
 * unlocked for a configurable session duration (default 5 minutes).
 * After the session expires, re-authentication is required.
 */
@Singleton
class AppLockManager @Inject constructor() {

    private var isUnlocked = false
    private var unlockTimestamp = 0L

    /** Default session duration: 5 minutes. */
    private var sessionDurationMs = 5 * 60 * 1000L

    /** Mark the app as unlocked and start the session timer. */
    fun unlock() {
        isUnlocked = true
        unlockTimestamp = System.currentTimeMillis()
    }

    /** Immediately lock the app. */
    fun lock() {
        isUnlocked = false
        unlockTimestamp = 0L
    }

    /**
     * Checks if the current session is still valid.
     *
     * @return true if the app was unlocked and the session hasn't expired.
     */
    fun isSessionValid(): Boolean {
        if (!isUnlocked) return false
        val elapsed = System.currentTimeMillis() - unlockTimestamp
        return elapsed < sessionDurationMs
    }

    /**
     * Checks if authentication is required.
     *
     * @return true if the session has expired or the app was never unlocked.
     */
    fun requiresAuth(): Boolean = !isSessionValid()

    /**
     * Updates the session duration.
     *
     * @param durationMs New session duration in milliseconds.
     */
    fun setSessionDuration(durationMs: Long) {
        sessionDurationMs = durationMs
    }
}
