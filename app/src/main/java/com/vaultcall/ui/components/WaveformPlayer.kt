package com.vaultcall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A simple waveform visualization composable.
 *
 * Draws vertical bars representing audio amplitudes.
 * Used in the voicemail detail screen for visual playback feedback.
 *
 * @param amplitudes List of amplitude values (0.0 to 1.0).
 * @param progress Current playback progress (0.0 to 1.0).
 * @param activeColor Color for the already-played portion.
 * @param inactiveColor Color for the unplayed portion.
 */
@Composable
fun WaveformPlayer(
    amplitudes: List<Float> = emptyList(),
    progress: Float = 0f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    modifier: Modifier = Modifier
) {
    val displayAmplitudes = if (amplitudes.isEmpty()) {
        // Generate placeholder waveform
        List(60) { (Math.random() * 0.7 + 0.1).toFloat() }
    } else {
        amplitudes
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barWidth = size.width / displayAmplitudes.size
        val centerY = size.height / 2
        val maxBarHeight = size.height * 0.8f

        displayAmplitudes.forEachIndexed { index, amplitude ->
            val barHeight = amplitude * maxBarHeight
            val x = index * barWidth + barWidth / 2
            val isPlayed = index.toFloat() / displayAmplitudes.size <= progress

            drawLine(
                color = if (isPlayed) activeColor else inactiveColor,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.6f
            )
        }
    }
}
