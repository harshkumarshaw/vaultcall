package com.vaultcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.vaultcall.R
import com.vaultcall.data.model.Transcript
import com.vaultcall.data.model.Voicemail
import com.vaultcall.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels and notification creation for VaultCall.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_SCREENED = "call_screened"
        const val CHANNEL_VOICEMAIL = "new_voicemail"
        const val CHANNEL_FOREGROUND = "screening_service"
        const val CHANNEL_MISSED = "missed_call"

        private const val NOTIFICATION_SCREENED_BASE = 2000
        private const val NOTIFICATION_VOICEMAIL_BASE = 3000
        private const val NOTIFICATION_MISSED_BASE = 4000
    }

    /**
     * Creates all notification channels on app start.
     */
    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_SCREENED,
                context.getString(R.string.channel_screened_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_screened_desc)
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_VOICEMAIL,
                context.getString(R.string.channel_voicemail_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_voicemail_desc)
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_FOREGROUND,
                context.getString(R.string.channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_foreground_desc)
            },
            NotificationChannel(
                CHANNEL_MISSED,
                context.getString(R.string.channel_missed_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_missed_desc)
            }
        )

        channels.forEach { nm.createNotificationChannel(it) }
    }

    /**
     * Shows a notification for a new voicemail with optional transcript preview.
     */
    fun showVoicemailNotification(voicemail: Voicemail, transcript: Transcript?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("voicemail_id", voicemail.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerDisplay = voicemail.callerName ?: voicemail.callerId
        val transcriptPreview = transcript?.text?.take(80) ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_VOICEMAIL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(context.getString(R.string.channel_voicemail_name))
            .setContentText("$callerDisplay: $transcriptPreview")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$callerDisplay\n$transcriptPreview"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        nm.notify(NOTIFICATION_VOICEMAIL_BASE + voicemail.id.toInt(), notification)
    }

    /**
     * Shows a rich notification after a call has been screened.
     */
    fun showScreenedCallNotification(voicemail: Voicemail, reasonSnippet: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val viewIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("voicemail_id", voicemail.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callBackIntent = PendingIntent.getActivity(
            context, 1,
            Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:${voicemail.callerId}")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerDisplay = voicemail.callerName ?: voicemail.callerId

        val notification = NotificationCompat.Builder(context, CHANNEL_SCREENED)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.screening_notification_title))
            .setContentText(context.getString(R.string.screening_notification_text, callerDisplay))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$callerDisplay\n$reasonSnippet"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(viewIntent)
            .addAction(android.R.drawable.ic_menu_call, context.getString(R.string.btn_call_back), callBackIntent)
            .addAction(android.R.drawable.ic_menu_view, context.getString(R.string.view_voicemail), viewIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        nm.notify(NOTIFICATION_SCREENED_BASE + voicemail.id.toInt(), notification)
    }

    /**
     * Shows a notification for a missed call.
     */
    fun showMissedCallNotification(phoneNumber: String, callerName: String?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val callerDisplay = callerName ?: phoneNumber

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Missed Call")
            .setContentText(callerDisplay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_MISSED_BASE + phoneNumber.hashCode(), notification)
    }

    /**
     * Creates the foreground service notification for the screening service.
     */
    fun showForegroundServiceNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.screening_foreground_title))
            .setContentText(context.getString(R.string.screening_foreground_text, ""))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Cancels a notification by its ID.
     */
    fun cancelNotification(id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
    }
}
