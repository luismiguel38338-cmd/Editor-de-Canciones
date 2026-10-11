package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIndigo
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    speed: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1200 / speed.coerceIn(0.5f, 2.0f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = 38
        val spacing = width / barCount
        val barWidth = spacing * 0.55f

        val centerY = height / 2f

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            val isPlayed = normalizedX <= progress

            // Harmonic amplitude formula combining wave harmonics
            val wave1 = sin(normalizedX * 10f + if (isPlaying) phase else 0f)
            val wave2 = sin(normalizedX * 18f - if (isPlaying) phase * 1.3f else 0f) * 0.5f
            val envelope = sin(normalizedX * PI.toFloat()) // Taper edges

            val baseMagnitude = if (isPlaying) {
                ((wave1 + wave2) * 0.5f + 0.5f) * envelope
            } else {
                (sin(normalizedX * 6f) * 0.3f + 0.35f) * envelope
            }

            val barHeight = (height * 0.85f * baseMagnitude.coerceIn(0.12f, 1.0f)).coerceAtLeast(6f)
            val x = i * spacing + (spacing - barWidth) / 2f
            val y = centerY - barHeight / 2f

            val brush = if (isPlayed) {
                Brush.verticalGradient(
                    colors = listOf(
                        StudioCobalt,
                        StudioIndigo
                    ),
                    startY = y,
                    endY = y + barHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF334155),
                        Color(0xFF1E293B)
                    ),
                    startY = y,
                    endY = y + barHeight
                )
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
