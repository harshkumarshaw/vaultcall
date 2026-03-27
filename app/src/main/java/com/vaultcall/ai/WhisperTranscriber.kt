package com.vaultcall.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device transcription engine using Whisper tiny ONNX model.
 *
 * All audio processing happens locally. No data ever leaves the device.
 * The model is loaded from assets on initialization and kept in memory.
 */
@Singleton
class WhisperTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    /**
     * Initializes the ONNX Runtime environment and loads the Whisper model.
     *
     * Should be called once during app startup. Safe to call multiple times.
     */
    fun initialize() {
        if (isInitialized) return

        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("whisper-tiny.onnx").readBytes()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)
            isInitialized = true
        } catch (e: Exception) {
            // Model load failed — transcription will be unavailable
            isInitialized = false
        }
    }

    /**
     * Transcribes an audio file using the Whisper model.
     *
     * @param audioFile A decrypted audio file to transcribe.
     * @return A [TranscriptionResult] with the text, language, and confidence score.
     */
    suspend fun transcribe(audioFile: File): TranscriptionResult = withContext(Dispatchers.Default) {
        if (!isInitialized || ortSession == null) {
            initialize()
        }

        val session = ortSession ?: return@withContext TranscriptionResult(
            text = "",
            language = "en",
            confidence = 0.0f
        )

        try {
            // Extract audio samples and compute mel spectrogram
            val samples = AudioPreprocessor.extractFloatArray(audioFile)
            val mel = AudioPreprocessor.computeMelSpectrogram(samples)

            // Flatten mel spectrogram to 1D for ONNX input
            // Shape: [1, 80, 3000]
            val flatMel = FloatArray(AudioPreprocessor.N_MELS * AudioPreprocessor.N_FRAMES)
            for (i in 0 until AudioPreprocessor.N_MELS) {
                for (j in 0 until AudioPreprocessor.N_FRAMES) {
                    flatMel[i * AudioPreprocessor.N_FRAMES + j] = mel[i][j]
                }
            }

            val env = ortEnvironment ?: return@withContext TranscriptionResult("", "en", 0.0f)

            // Create input tensor
            val inputShape = longArrayOf(1, AudioPreprocessor.N_MELS.toLong(), AudioPreprocessor.N_FRAMES.toLong())
            val inputBuffer = FloatBuffer.wrap(flatMel)
            val inputTensor = OnnxTensor.createTensor(env, inputBuffer, inputShape)

            // Run inference
            val inputName = session.inputNames.firstOrNull() ?: "input_features"
            val results = session.run(mapOf(inputName to inputTensor))

            // Decode output tokens to text
            val outputTensor = results.firstOrNull()?.value
            val decodedText = decodeOutput(outputTensor)

            inputTensor.close()
            results.close()

            TranscriptionResult(
                text = decodedText.trim(),
                language = detectLanguage(decodedText),
                confidence = calculateConfidence(decodedText)
            )

        } catch (e: Exception) {
            TranscriptionResult(
                text = "",
                language = "en",
                confidence = 0.0f
            )
        }
    }

    /**
     * Decodes the ONNX model output to text.
     *
     * Implements a basic token-to-text mapping. In a full implementation,
     * this would use the Whisper tokenizer vocabulary.
     */
    private fun decodeOutput(output: Any?): String {
        return when (output) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                val tokens = output as? Array<LongArray>
                tokens?.firstOrNull()?.let { tokenIds ->
                    // Basic ASCII decoding for demo — full impl would use vocab.json
                    tokenIds.filter { it in 32..126 }
                        .map { it.toInt().toChar() }
                        .joinToString("")
                } ?: ""
            }
            is LongArray -> {
                output.filter { it in 32..126 }
                    .map { it.toInt().toChar() }
                    .joinToString("")
            }
            else -> ""
        }
    }

    /**
     * Simple language detection based on character analysis.
     */
    private fun detectLanguage(text: String): String {
        // Simple heuristic — could be improved with proper detection
        val hasDevnagari = text.any { it.code in 0x0900..0x097F }
        return if (hasDevnagari) "hi" else "en"
    }

    /**
     * Calculates a simple confidence score based on text properties.
     */
    private fun calculateConfidence(text: String): Float {
        if (text.isEmpty()) return 0.0f
        // Simple heuristic based on text length and printable character ratio
        val printableRatio = text.count { it.isLetterOrDigit() || it.isWhitespace() }
            .toFloat() / text.length
        return (printableRatio * 0.8f).coerceIn(0.0f, 1.0f)
    }

    /**
     * Detects flagged keywords in a transcript.
     *
     * @param text The transcript text.
     * @return List of detected keywords.
     */
    fun extractKeywords(text: String): List<String> {
        val keywords = listOf(
            "urgent", "emergency", "callback", "call back", "meeting",
            "important", "asap", "deadline", "help", "problem", "issue",
            "doctor", "hospital", "police", "accident", "fire"
        )
        return keywords.filter { text.lowercase().contains(it) }
    }

    /**
     * Result of a transcription operation.
     */
    data class TranscriptionResult(
        val text: String,
        val language: String,
        val confidence: Float
    )

    /**
     * Release ONNX resources.
     */
    fun release() {
        ortSession?.close()
        ortEnvironment?.close()
        ortSession = null
        ortEnvironment = null
        isInitialized = false
    }
}
