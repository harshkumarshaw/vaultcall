package com.vaultcall.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages greeting playback during call screening.
 *
 * Supports both Text-to-Speech (TTS) generated greetings and
 * pre-recorded audio file greetings.
 */
@Singleton
class GreetingPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.getDefault()
                
                // CRITICAL: Force TTS to use the Voice Communication stream instead of Media.
                // Standard Media streams are aggressively muted by Android during active cellular calls.
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            }
        }
    }

    /**
     * Plays a TTS greeting during an active call.
     *
     * @param text The greeting text to speak.
     * @param onComplete Callback when the greeting finishes playing.
     */
    suspend fun playTTSGreeting(text: String, onComplete: () -> Unit) {
        if (!isTtsReady || tts == null) {
            onComplete()
            return
        }

        suspendCancellableCoroutine { continuation ->
            val utteranceId = UUID.randomUUID().toString()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    onComplete()
                    if (continuation.isActive) continuation.resume(Unit)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onComplete()
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onStart(utteranceId: String?) {}
            })

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            continuation.invokeOnCancellation {
                tts?.stop()
            }
        }
    }

    /**
     * Plays a pre-recorded greeting file.
     *
     * @param filePath Path to the greeting audio file.
     * @param onComplete Callback when playback finishes.
     */
    suspend fun playRecordedGreeting(filePath: String, onComplete: () -> Unit) {
        // Will be implemented when custom greeting recording is added
        // For now, fall through to TTS
        onComplete()
    }

    /**
     * Releases TTS resources.
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }
}
