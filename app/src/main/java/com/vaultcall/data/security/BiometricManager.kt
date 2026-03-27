package com.vaultcall.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android BiometricPrompt for app authentication.
 *
 * Supports biometric (fingerprint/face) with device credential (PIN/pattern) fallback.
 * Used at the UI layer to gate access to the app and sensitive operations.
 */
@Singleton
class VaultBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Checks if biometric or device credential authentication is available.
     *
     * @return true if the device supports BIOMETRIC_STRONG or DEVICE_CREDENTIAL.
     */
    fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric authentication prompt.
     *
     * @param activity The FragmentActivity to host the prompt.
     * @param title Custom title for the prompt dialog.
     * @param subtitle Custom subtitle for the prompt dialog.
     * @param onSuccess Callback invoked on successful authentication.
     * @param onFailure Callback invoked on authentication failure, with error message.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "VaultCall — Verify Identity",
        subtitle: String = "Authenticate to access VaultCall",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't call onFailure here — this is called for each failed attempt
                // The prompt stays open and the user can retry
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onFailure(errString.toString())
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
