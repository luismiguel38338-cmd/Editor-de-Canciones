package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.AmberPitch
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SongEditorUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    song: Song,
    uiState: SongEditorUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onToggleLoop: () -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSemitonesChanged: (Int) -> Unit,
    onIncrementPitch: () -> Unit,
    onDecrementPitch: () -> Unit,
    onResetEffects: () -> Unit,
    onSaveModifiedCopy: () -> Unit,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val scrollState = rememberScrollState()

    // Turntable rotation animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (4000 / uiState.speed.coerceIn(0.5f, 2.0f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isUserScrubbing) scrubPositionMs.toLong() else uiState.currentPositionMs
    val totalDuration = uiState.totalDurationMs.coerceAtLeast(1L)
    val playbackProgress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Studio Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a la lista",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick reset button
            OutlinedButton(
                onClick = onResetEffects,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.testTag("reset_effects_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restablecer", fontSize = 12.sp)
            }
        }

        // Scrollable Editor Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vinyl Turntable / Cover Art Centerpiece
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing neon rim
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(NeonCyan, ElectricViolet, AmberPitch, NeonCyan)
                            )
                        )
                )

                // Vinyl grooves body
                Box(
                    modifier = Modifier
                        .size(186.dp)
                        .rotate(if (uiState.isPlaying) rotationAngle else 0f)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1117)),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric vinyl rings
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161A24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center label
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Portada",
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Spindle hole
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(DarkBg)
                    )
                }
            }

            // Waveform Visualizer
            WaveformVisualizer(
                isPlaying = uiState.isPlaying,
                speed = uiState.speed,
                progress = playbackProgress,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Timeline Scrubbing Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = {
                        isUserScrubbing = true
                        scrubPositionMs = it
                    },
                    onValueChangeFinished = {
                        onSeek(scrubPositionMs.toLong())
                        isUserScrubbing = false
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = formatTime(totalDuration),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            // Transport Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop toggle
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isLooping) ElectricViolet.copy(alpha = 0.25f) else DarkSurfaceVariant)
                        .testTag("loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repetir",
                        tint = if (uiState.isLooping) ElectricViolet else TextSecondary
                    )
                }

                // Rewind 5s
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("rewind_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Retroceder 5 segundos",
                        tint = TextPrimary
                    )
                }

                // Play / Pause Large FAB
                FloatingActionButton(
                    onClick = onTogglePlayPause,
                    containerColor = NeonCyan,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("play_pause_fab")
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Forward 5s
                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("forward_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Avanzar 5 segundos",
                        tint = TextPrimary
                    )
                }

                // Visual EQ indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (uiState.isPlaying) NeonCyan else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // STUDIO CONTROL 1: VELOCIDAD (Speed)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Velocidad en tiempo real",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Surface(
                            color = NeonCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, NeonCyan)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fx", uiState.speed),
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = uiState.speed,
                        onValueChange = onSpeedChanged,
                        valueRange = 0.5f..2.0f,
                        steps = 29, // 0.05 step
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("speed_slider")
                    )

                    // Preset chips for Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0.75f to "0.75x", 1.0f to "1.0x (Normal)", 1.25f to "1.25x", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (speedVal, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(uiState.speed - speedVal) < 0.03f,
                                onClick = { onSpeedChanged(speedVal) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // STUDIO CONTROL 2: TONO / PITCH (Semitonos)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AmberPitch,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tono (Pitch) en tiempo real",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Surface(
                            color = AmberPitch.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, AmberPitch)
                        ) {
                            val sign = if (uiState.semitones > 0) "+" else ""
                            Text(
                                text = "$sign${uiState.semitones} st (${String.format(Locale.US, "%.2fx", uiState.pitchFactor)})",
                                color = AmberPitch,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stepper Row: - Button, Slider, + Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDecrementPitch,
                            enabled = uiState.semitones > -12,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("pitch_decrement_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Bajar semitono", tint = AmberPitch)
                        }

                        Slider(
                            value = uiState.semitones.toFloat(),
                            onValueChange = { onSemitonesChanged(it.toInt()) },
                            valueRange = -12f..12f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberPitch,
                                activeTrackColor = AmberPitch,
                                inactiveTrackColor = DarkSurfaceBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("pitch_slider")
                        )

                        IconButton(
                            onClick = onIncrementPitch,
                            enabled = uiState.semitones < 12,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("pitch_increment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Subir semitono", tint = AmberPitch)
                        }
                    }

                    // Pitch Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(-5 to "-5st", -2 to "-2st", 0 to "Original", 2 to "+2st", 5 to "+5st", 7 to "+7st").forEach { (semi, label) ->
                            FilterChip(
                                selected = uiState.semitones == semi,
                                onClick = { onSemitonesChanged(semi) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPitch,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ACTION: GUARDAR COPIA MODIFICADA (Save Modified Copy)
            Button(
                onClick = onSaveModifiedCopy,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_modified_copy_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Exportando copia (${(uiState.saveProgress * 100).toInt()}%)…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Copia Modificada",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "✓ Se creará un nuevo archivo de audio sin sobrescribir ni modificar la canción original.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Saving Dialog
    if (uiState.isSaving) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Exportando Copia Modificada",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aplicando velocidad ${String.format(Locale.US, "%.2fx", uiState.speed)} y tono ${if (uiState.semitones >= 0) "+${uiState.semitones}" else "${uiState.semitones}"} semitonos…",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { uiState.saveProgress },
                        color = NeonCyan,
                        trackColor = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(uiState.saveProgress * 100).toInt()}% completado",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Save Success Alert Dialog
    if (uiState.saveSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            containerColor = DarkSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¡Copia Guardada con Éxito!",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = uiState.saveSuccessMessage,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "La copia está disponible en la pestaña 'Mis Ediciones' y en la carpeta Música de tu dispositivo.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Error Alert Dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Aviso",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = uiState.errorMessage,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("Cerrar", color = NeonCyan)
                }
            }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
