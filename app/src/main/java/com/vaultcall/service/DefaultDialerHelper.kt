package com.vaultcall.service

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager

/**
 * Utility object for managing default dialer status and placing calls.
 * Uses RoleManager (API 29+) for proper default dialer request.
 */
object DefaultDialerHelper {

    /**
     * Checks if VaultCall is currently the device's default dialer.
     *
     * @param context Application context.
     * @return true if this app is the default dialer.
     */
    fun isDefaultDialer(context: Context): Boolean {
        return try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.defaultDialerPackage == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launches the system dialog to request the user to set VaultCall
     * as the default dialer.
     *
     * Uses RoleManager (API 29+) first, falls back to TelecomManager intent.
     *
     * @param activity The activity to launch the request from.
     * @param requestCode The request code for onActivityResult.
     */
    fun requestDefaultDialer(activity: Activity, requestCode: Int = 200) {
        try {
            // API 29+ — use RoleManager for reliable default dialer request
            val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    @Suppress("DEPRECATION")
                    activity.startActivityForResult(intent, requestCode)
                    return
                }
            }
        } catch (e: Exception) {
            // RoleManager not available, fall through to legacy
        }

        // Legacy fallback
        try {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    activity.packageName
                )
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Last resort — open app info settings
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    /**
     * Places an outgoing call via the system TelecomManager.
     *
     * @param context Application context.
     * @param phoneNumber The phone number to call.
     * @param simSlot SIM slot to use (0 = default, >0 = specific slot on dual-SIM).
     */
    fun makeCall(context: Context, phoneNumber: String, simSlot: Int = 0) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (simSlot > 0) {
                    putExtra("com.android.phone.extra.SLOT_ID", simSlot)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Permission not granted or intent resolution failed.
            // The permissions should be checked by the UI before invoking this.
        }
    }
}
