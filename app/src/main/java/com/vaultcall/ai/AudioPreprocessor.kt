package com.vaultcall.ai

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Preprocesses audio files for Whisper ONNX inference.
 *
 * Converts .m4a/AAC audio to the format expected by Whisper:
 * - 16kHz sample rate, mono, normalized float array (-1.0 to 1.0)
 * - Log-mel spectrogram with 80 mel bins and 3000 frames
 */
object AudioPreprocessor {

    const val SAMPLE_RATE = 16000
    const val MAX_DURATION_SECONDS = 30
    private const val N_FFT = 400
    private const val HOP_LENGTH = 160
    const val N_MELS = 80
    const val N_FRAMES = 3000

    /**
     * Extracts and converts audio from an .m4a file to a float array.
     *
     * @param audioFile The input audio file.
     * @return Float array of mono 16kHz samples, normalized -1.0 to 1.0,
     *         padded or trimmed to [MAX_DURATION_SECONDS] * [SAMPLE_RATE].
     */
    suspend fun extractFloatArray(audioFile: File): FloatArray = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        extractor.setDataSource(audioFile.absolutePath)

        // Find audio track
        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }
        }

        if (audioTrackIndex == -1) {
            extractor.release()
            return@withContext FloatArray(MAX_DURATION_SECONDS * SAMPLE_RATE)
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

        // Decode audio using MediaCodec
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmSamples = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var isEos = false

        try {
            while (!isEos) {
                // Feed input
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                // Read output
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val shortBuffer = outputBuffer.asShortBuffer()
                    val samples = ShortArray(shortBuffer.remaining())
                    shortBuffer.get(samples)
                    pcmSamples.addAll(samples.toList())
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        // Convert to mono if stereo
        val monoSamples = if (channelCount > 1) {
            pcmSamples.chunked(channelCount).map { channels ->
                channels.sumOf { it.toInt() } / channelCount
            }.map { it.toShort() }
        } else {
            pcmSamples
        }

        // Resample to 16kHz if needed
        val resampled = if (sampleRate != SAMPLE_RATE) {
            resample(monoSamples, sampleRate, SAMPLE_RATE)
        } else {
            monoSamples
        }

        // Convert to float and normalize
        val targetLength = MAX_DURATION_SECONDS * SAMPLE_RATE
        val floatArray = FloatArray(targetLength)
        val length = min(resampled.size, targetLength)
        for (i in 0 until length) {
            floatArray[i] = resampled[i].toFloat() / 32768.0f
        }

        floatArray
    }

    /**
     * Computes a log-mel spectrogram from audio samples.
     *
     * @param samples Float array of 16kHz mono audio samples.
     * @return 2D array of shape [N_MELS][N_FRAMES] containing log-mel values.
     */
    fun computeMelSpectrogram(samples: FloatArray): Array<FloatArray> {
        val mel = Array(N_MELS) { FloatArray(N_FRAMES) }

        // Compute STFT frames
        for (frame in 0 until N_FRAMES) {
            val start = frame * HOP_LENGTH
            val window = FloatArray(N_FFT)
            for (i in 0 until N_FFT) {
                val sampleIdx = start + i
                if (sampleIdx < samples.size) {
                    // Apply Hann window
                    val hannValue = 0.5f * (1 - cos(2.0 * PI * i / (N_FFT - 1)).toFloat())
                    window[i] = samples[sampleIdx] * hannValue
                }
            }

            // Compute power spectrum via FFT (simplified DFT for correctness)
            val fftSize = N_FFT / 2 + 1
            val powerSpectrum = FloatArray(fftSize)
            for (k in 0 until fftSize) {
                var real = 0.0
                var imag = 0.0
                for (n in 0 until N_FFT) {
                    val angle = -2.0 * PI * k * n / N_FFT
                    real += window[n] * cos(angle)
                    imag += window[n] * kotlin.math.sin(angle)
                }
                powerSpectrum[k] = (real * real + imag * imag).toFloat()
            }

            // Apply mel filterbank
            val melFilters = getMelFilterbank(fftSize)
            for (m in 0 until N_MELS) {
                var sum = 0.0f
                for (k in 0 until fftSize) {
                    sum += melFilters[m][k] * powerSpectrum[k]
                }
                mel[m][frame] = ln(max(sum, 1e-10f))
            }
        }

        return mel
    }

    /**
     * Generates a mel filterbank matrix.
     */
    private fun getMelFilterbank(fftSize: Int): Array<FloatArray> {
        val filters = Array(N_MELS) { FloatArray(fftSize) }

        val fMin = 0.0
        val fMax = SAMPLE_RATE / 2.0
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)

        val melPoints = DoubleArray(N_MELS + 2) { i ->
            melMin + i * (melMax - melMin) / (N_MELS + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }
        val binPoints = hzPoints.map { (it * N_FFT / SAMPLE_RATE).toInt() }

        for (m in 0 until N_MELS) {
            for (k in binPoints[m] until min(binPoints[m + 1], fftSize)) {
                if (binPoints[m + 1] > binPoints[m]) {
                    filters[m][k] = (k - binPoints[m]).toFloat() /
                            (binPoints[m + 1] - binPoints[m]).toFloat()
                }
            }
            for (k in binPoints[m + 1] until min(binPoints[m + 2], fftSize)) {
                if (binPoints[m + 2] > binPoints[m + 1]) {
                    filters[m][k] = (binPoints[m + 2] - k).toFloat() /
                            (binPoints[m + 2] - binPoints[m + 1]).toFloat()
                }
            }
        }

        return filters
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * kotlin.math.log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    /**
     * Simple linear resampling from one sample rate to another.
     */
    private fun resample(samples: List<Short>, fromRate: Int, toRate: Int): List<Short> {
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputLength = (samples.size / ratio).toInt()
        return List(outputLength) { i ->
            val srcIndex = (i * ratio).toInt()
            if (srcIndex < samples.size) samples[srcIndex] else 0
        }
    }
}
