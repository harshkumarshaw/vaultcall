package com.vaultcall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for BOOT_COMPLETED broadcast.
 *
 * Ensures VaultCall's services are ready after device restart.
 * No explicit action needed — Hilt injection and the InCallService
 * registration handle reactivation automatically.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Services will be started on-demand by the system
            // when calls come in. No manual start needed.
        }
    }
}
