package com.vaultcall.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * AppWidget receiver for the VaultCall voicemail widget.
 *
 * Displays the last 3 voicemails on the home screen.
 * Updates every 15 minutes via AppWidgetManager.
 */
class VoicemailWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Widget update will be implemented with Glance
        // For now, this is a placeholder to satisfy the manifest declaration
    }

    override fun onEnabled(context: Context) {
        // First widget placed
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }
}
