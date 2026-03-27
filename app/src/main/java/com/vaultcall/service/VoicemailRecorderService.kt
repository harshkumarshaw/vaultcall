package com.vaultcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vaultcall.R
import com.vaultcall.data.model.RuleAction
import com.vaultcall.data.model.Voicemail
import com.vaultcall.data.repository.VoicemailRepository
import com.vaultcall.data.security.SecureFileStorage
import com.vaultcall.domain.usecase.AutoReplyUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Foreground service that handles voicemail recording during screened calls.
 *
 * Flow:
 * 1. Receives SCREEN_CALL broadcast from CallScreeningService
 * 2. Starts as foreground service with notification
 * 3. Waits for call to become ACTIVE
 * 4. Plays greeting (TTS or recorded)
 * 5. Plays beep tone
 * 6. Starts recording caller response (30s max default)
 * 7. Encrypts and saves recording
 * 8. Triggers transcription via WorkManager
 * 9. Shows notification
 */
@AndroidEntryPoint
class VoicemailRecorderService : Service() {

    @Inject lateinit var callStateManager: CallStateManager
    @Inject lateinit var voicemailRepository: VoicemailRepository
    @Inject lateinit var secureFileStorage: SecureFileStorage
    @Inject lateinit var autoReplyUseCase: AutoReplyUseCase
    @Inject lateinit var greetingPlayer: GreetingPlayer
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    companion object {
        const val RECORDING_TIMEOUT_MS = 30_000L
        const val MAX_RECORDING_MS = 60_000L
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screening_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification("Screening service active")
        startForeground(NOTIFICATION_ID, notification)

        if (intent != null && intent.hasExtra("phone_number") && !isRecording) {
            val phoneNumber = intent.getStringExtra("phone_number") ?: return START_STICKY
            val ruleId = intent.getLongExtra("rule_id", -1L)
            val ruleAction = intent.getStringExtra("rule_action") ?: "VOICEMAIL_ONLY"
            val ruleName = intent.getStringExtra("rule_name") ?: "Unknown"
            val smsTemplate = intent.getStringExtra("sms_template") ?: ""
            val greetingId = intent.getStringExtra("greeting_id")

            serviceScope.launch {
                handleScreenedCall(phoneNumber, ruleId, ruleAction, ruleName, smsTemplate, greetingId)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    /**
     * Main screening pipeline.
     */
    private suspend fun handleScreenedCall(
        phoneNumber: String,
        ruleId: Long,
        ruleAction: String,
        ruleName: String,
        smsTemplate: String,
        greetingId: String?
    ) {
        try {
            // Update foreground notification
            val notification = createForegroundNotification("Screening call from $phoneNumber")
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)

            // Wait for call to appear in State Manager (up to 10s)
            var callId: String? = null
            var waitTime = 0L
            while (waitTime < 10_000L) {
                callId = callStateManager.activeCalls.value.values.find {
                    it.phoneNumber.contains(phoneNumber.takeLast(10))
                }?.id
                if (callId != null) break
                delay(500)
                waitTime += 500
            }

            if (callId == null) {
                stopSelf()
                return
            }

            val inCallService = MyInCallService.instance
            if (inCallService != null) {
                // Securely lock the UI so ActiveCallActivity doesn't bounce in
                inCallService.markAsAutoAnswered(callId)
                
                // Aggressively answer the call immediately! Fixes the 10-second deadlock ring
                inCallService.answerCall(callId)

                // Wait for explicit ACTIVE state confirmation before playing audio
                var activeWait = 0L
                while (activeWait < 5000L) {
                    val state = callStateManager.activeCalls.value[callId]?.state
                    if (state == CallStateManager.CallState.ACTIVE) break
                    delay(250)
                    activeWait += 250
                }
                
                // CRITICAL ROUTING: Turn on speaker natively so TTS routes to the cellular uplink
                inCallService.setSpeakerphone(true)
                delay(500)
            }

            // Play greeting
            val greetingText = when {
                greetingId == "night" -> getString(R.string.greeting_night)
                greetingId == "dnd" -> getString(R.string.greeting_dnd)
                greetingId == "meeting" -> getString(R.string.greeting_meeting)
                else -> getString(R.string.greeting_default)
            }
            greetingPlayer.playTTSGreeting(greetingText) {}
            delay(3000) // Wait for greeting to finish

            // Play beep tone
            playBeepTone()
            delay(600)

            // Start recording
            val tempFile = File(cacheDir, "recording_${System.currentTimeMillis()}.m4a")
            startRecording(tempFile)

            // Wait for recording timeout or caller hangup
            var recordingTime = 0L
            while (isRecording && recordingTime < RECORDING_TIMEOUT_MS) {
                delay(500)
                recordingTime += 500

                // Check if call ended
                val currentCall = callStateManager.activeCalls.value.values.find {
                    it.phoneNumber.contains(phoneNumber.takeLast(10))
                }
                if (currentCall == null || currentCall.state == CallStateManager.CallState.DISCONNECTED) {
                    break
                }
            }

            // Stop recording
            stopRecording()
            
            // Turn off speakerphone safely
            try {
                inCallService?.setSpeakerphone(false)
            } catch (_: Exception) {}

            // Hang up
            if (callId != null && inCallService != null) {
                inCallService.endCall(callId)
            }

            // Save voicemail
            if (tempFile.exists() && tempFile.length() > 0) {
                val duration = (recordingTime / 1000).toInt()
                val voicemail = Voicemail(
                    callerId = phoneNumber,
                    callerName = null,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    encryptedFilePath = "", // Will be updated after encryption
                    isRead = false,
                    isScreened = true,
                    triggerRule = ruleName
                )

                val voicemailId = voicemailRepository.saveVoicemail(voicemail)
                val encryptedPath = secureFileStorage.saveVoicemail(tempFile, voicemailId)

                // Update with encrypted path
                voicemailRepository.saveVoicemail(
                    voicemail.copy(id = voicemailId, encryptedFilePath = encryptedPath)
                )

                // Trigger transcription
                TranscriptionWorker.enqueue(this@VoicemailRecorderService, voicemailId)

                // Show notification
                notificationHelper.showScreenedCallNotification(
                    voicemail.copy(id = voicemailId, encryptedFilePath = encryptedPath),
                    "New voicemail from $phoneNumber"
                )

                // Send SMS auto-reply if configured
                if (ruleAction == RuleAction.AUTO_REPLY_SMS.name && smsTemplate.isNotEmpty()) {
                    autoReplyUseCase.sendAutoReply(
                        phoneNumber = phoneNumber,
                        template = smsTemplate,
                        callerName = null,
                        rule = com.vaultcall.data.model.CallRule(
                            id = ruleId,
                            name = ruleName,
                            type = com.vaultcall.data.model.RuleType.CUSTOM_SCHEDULE,
                            startTime = null,
                            endTime = null,
                            activeDays = "[]",
                            action = RuleAction.AUTO_REPLY_SMS,
                            smsReplyTemplate = smsTemplate,
                            greetingId = greetingId,
                            isEnabled = true,
                            priority = 0
                        )
                    )
                }
            }

        } catch (e: Exception) {
            // Log error but don't crash the service
        }
    }

    private fun startRecording(outputFile: File) {
        try {
            mediaRecorder = MediaRecorder().apply {
                // Use MIC instead of VOICE_COMMUNICATION to bypass VoIP echo-cancellation
                // which would otherwise filter out the speakerphone audio.
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(32000)
                setMaxDuration(MAX_RECORDING_MS.toInt())
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) { }
        mediaRecorder = null
        isRecording = false
    }

    /**
     * Plays a short beep tone (440Hz, 500ms) to signal recording start.
     */
    private fun playBeepTone() {
        try {
            val sampleRate = 16000
            val durationMs = 500
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * 440.0 * i / sampleRate
                buffer[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.5).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
        } catch (_: Exception) { }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_foreground_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_foreground_desc)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.screening_foreground_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
