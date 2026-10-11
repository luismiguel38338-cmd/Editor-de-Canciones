package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPitch
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.2f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_waves")

    // Continuous sonic wave expansion
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_expansion"
    )

    // Rotating vinyl halo
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Equalizer pulsing bars
    val eqPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_pulse"
    )

    LaunchedEffect(Unit) {
        // Step 1: Reveal logo with spring overshoot
        logoAlpha.animateTo(1f, animationSpec = tween(400))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
        )

        // Step 2: Fade in title & tagline
        textAlpha.animateTo(1f, animationSpec = tween(500))

        // Step 3: Reveal button
        buttonAlpha.animateTo(1f, animationSpec = tween(400))

        // Auto navigate after intro sequence
        delay(1800)
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onAnimationFinished()
            }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric Sound Wave Ripples Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
            val maxRadius = size.width * 0.7f

            for (i in 0..3) {
                val progress = (wavePhase + i * 0.25f) % 1f
                val radius = progress * maxRadius
                val alpha = (1f - progress).coerceIn(0f, 0.7f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = alpha * 0.4f),
                            ElectricViolet.copy(alpha = alpha * 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius.coerceAtLeast(1f)
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Rotating Studio Vinyl / Glowing Icon
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing gradient halo
                Box(
                    modifier = Modifier
                        .size(166.dp)
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(NeonCyan, ElectricViolet, AmberPitch, NeonCyan)
                            )
                        )
                )

                // Vinyl Center
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D0F14)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Editor de Canciones",
                            tint = NeonCyan,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated Equalizer Visualizer Bars
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .alpha(logoAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(14.dp, 26.dp, 18.dp, 28.dp, 22.dp, 12.dp, 25.dp, 16.dp)
                heights.forEachIndexed { index, baseHeight ->
                    val factor = sin((index.toFloat() + eqPulse * 4f) * PI.toFloat() / 4f) * 0.4f + 0.6f
                    val currentHeight = (baseHeight.value * factor).dp.coerceAtLeast(6.dp)

                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(currentHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonCyan, ElectricViolet)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Title with Neon Aesthetic
            Text(
                text = "EDITOR DE CANCIONES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estudio Nativo • Velocidad & Tono en Tiempo Real",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Direct Enter Button
            Button(
                onClick = onAnimationFinished,
                modifier = Modifier
                    .alpha(buttonAlpha.value)
                    .height(48.dp)
                    .testTag("splash_enter_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Entrar al Estudio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
