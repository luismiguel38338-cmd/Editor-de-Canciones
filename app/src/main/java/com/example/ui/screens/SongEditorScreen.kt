package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.ParametricEqVisualizer
import com.example.audio.PlayerRepeatMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SongEditorUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    song: Song,
    uiState: SongEditorUiState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onToggleLoop: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onSemitonesChanged: (Int) -> Unit,
    onIncrementPitch: () -> Unit,
    onDecrementPitch: () -> Unit,
    onBassGainChanged: (Float) -> Unit,
    onMidGainChanged: (Float) -> Unit,
    onTrebleGainChanged: (Float) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onResetPitchAndSpeed: () -> Unit,
    onResetEqualizer: () -> Unit,
    onResetAllEffects: () -> Unit,
    onSaveModifiedCopy: () -> Unit,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState = uiState.playerState
    val scrollState = rememberScrollState()

    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isUserScrubbing) scrubPositionMs.toLong() else playerState.currentPositionMs
    val totalDuration = playerState.totalDurationMs.coerceAtLeast(1L)
    val playbackProgress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    var selectedEffectTab by remember { mutableIntStateOf(0) } // 0 = Tono y Velocidad, 1 = Ecualizador

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        // Encabezado de la pantalla de edición
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estudio de Edición",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = StudioCobalt,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onResetAllEffects,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary,
                    containerColor = StudioSurface
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("reset_all_effects_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restablecer", fontSize = 12.sp)
            }
        }

        // Cuerpo desplazable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de la pista cargada y controles de reproducción
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Carátula de la pista
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioSurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Carátula",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = StudioCobalt,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de progreso y tiempos
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
                            thumbColor = StudioCobalt,
                            activeTrackColor = StudioCobalt,
                            inactiveTrackColor = StudioSurfaceBorder
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
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = formatTime(totalDuration),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botonera de reproducción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleLoop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (playerState.repeatMode != PlayerRepeatMode.OFF) StudioCobalt.copy(alpha = 0.2f) else StudioSurfaceHighlight)
                                .testTag("loop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Bucle",
                                tint = if (playerState.repeatMode != PlayerRepeatMode.OFF) StudioCobalt else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipBackward,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceHighlight)
                                .testTag("rewind_5s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Retroceder 5 segundos",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Botón de Reproducción Principal
                        Button(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("play_pause_fab"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioCobalt,
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipForward,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceHighlight)
                                .testTag("forward_5s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Avanzar 5 segundos",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Indicador de volumen rápido
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (playerState.volume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Fader de volumen discreto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volumen",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.width(54.dp)
                        )
                        Slider(
                            value = playerState.volume,
                            onValueChange = onVolumeChanged,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = TextPrimary,
                                activeTrackColor = TextSecondary,
                                inactiveTrackColor = StudioSurfaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(playerState.volume * 100).toInt()}%",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selector de pestaña de procesamiento (Tono y Velocidad vs Ecualizador)
            SecondaryTabRow(
                selectedTabIndex = selectedEffectTab,
                containerColor = StudioBg,
                contentColor = TextPrimary,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedEffectTab, matchContentSize = true),
                        color = StudioCobalt
                    )
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioSurfaceBorder)
                    )
                }
            ) {
                Tab(
                    selected = selectedEffectTab == 0,
                    onClick = { selectedEffectTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedEffectTab == 0) StudioCobalt else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tono y Velocidad",
                                color = if (selectedEffectTab == 0) StudioCobalt else TextSecondary,
                                fontWeight = if (selectedEffectTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_pitch_speed")
                )

                Tab(
                    selected = selectedEffectTab == 1,
                    onClick = { selectedEffectTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedEffectTab == 1) StudioCobalt else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ecualizador & Graves",
                                color = if (selectedEffectTab == 1) StudioCobalt else TextSecondary,
                                fontWeight = if (selectedEffectTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_equalizer")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CONTENIDO PESTAÑA 1: TONO Y VELOCIDAD
            if (selectedEffectTab == 0) {
                // Control de Velocidad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurface)
                        .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Velocidad de reproducción",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Surface(
                                color = StudioCobalt.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, StudioCobalt.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2fx", playerState.speed),
                                    color = StudioCobalt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Slider(
                            value = playerState.speed,
                            onValueChange = onSpeedChanged,
                            valueRange = 0.5f..2.0f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioCobalt,
                                activeTrackColor = StudioCobalt,
                                inactiveTrackColor = StudioSurfaceBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("speed_slider")
                        )

                        // Presets de velocidad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (speedVal, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(playerState.speed - speedVal) < 0.03f,
                                    onClick = { onSpeedChanged(speedVal) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioCobalt,
                                        selectedLabelColor = Color.White,
                                        containerColor = StudioSurfaceHighlight,
                                        labelColor = TextSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Control de Tono (Pitch)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurface)
                        .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tono (Pitch en semitonos)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Surface(
                                color = StudioAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, StudioAmber.copy(alpha = 0.3f))
                            ) {
                                val sign = if (playerState.semitones > 0) "+" else ""
                                Text(
                                    text = "$sign${playerState.semitones} st (${String.format(Locale.US, "%.2fx", playerState.pitchFactor)})",
                                    color = StudioAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDecrementPitch,
                                enabled = playerState.semitones > -12,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(StudioSurfaceHighlight)
                                    .testTag("pitch_decrement_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Bajar semitono", tint = StudioAmber, modifier = Modifier.size(16.dp))
                            }

                            Slider(
                                value = playerState.semitones.toFloat(),
                                onValueChange = { onSemitonesChanged(it.toInt()) },
                                valueRange = -12f..12f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioAmber,
                                    activeTrackColor = StudioAmber,
                                    inactiveTrackColor = StudioSurfaceBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .testTag("pitch_slider")
                            )

                            IconButton(
                                onClick = onIncrementPitch,
                                enabled = playerState.semitones < 12,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(StudioSurfaceHighlight)
                                    .testTag("pitch_increment_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Subir semitono", tint = StudioAmber, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Presets de semitonos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-5 to "-5st", -2 to "-2st", 0 to "0 (Orig)", 2 to "+2st", 5 to "+5st", 7 to "+7st").forEach { (semi, label) ->
                                FilterChip(
                                    selected = playerState.semitones == semi,
                                    onClick = { onSemitonesChanged(semi) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioAmber,
                                        selectedLabelColor = Color.White,
                                        containerColor = StudioSurfaceHighlight,
                                        labelColor = TextSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // CONTENIDO PESTAÑA 2: ECUALIZADOR & GRAVES
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Gráfico de respuesta en frecuencia paramétrico
                    ParametricEqVisualizer(
                        config = uiState.equalizerConfig,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Banda 1: Graves (Low Shelf 200 Hz)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Graves (< 250 Hz)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    text = "${if (uiState.equalizerConfig.bassGainDb > 0) "+" else ""}${uiState.equalizerConfig.bassGainDb.toInt()} dB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCobalt
                                )
                            }
                            Slider(
                                value = uiState.equalizerConfig.bassGainDb,
                                onValueChange = onBassGainChanged,
                                valueRange = -12f..12f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioCobalt,
                                    activeTrackColor = StudioCobalt,
                                    inactiveTrackColor = StudioSurfaceBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Banda 2: Medios (Peaking 1 kHz)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Medios (~1 kHz)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    text = "${if (uiState.equalizerConfig.midGainDb > 0) "+" else ""}${uiState.equalizerConfig.midGainDb.toInt()} dB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCobalt
                                )
                            }
                            Slider(
                                value = uiState.equalizerConfig.midGainDb,
                                onValueChange = onMidGainChanged,
                                valueRange = -12f..12f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioCobalt,
                                    activeTrackColor = StudioCobalt,
                                    inactiveTrackColor = StudioSurfaceBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Banda 3: Agudos (High Shelf 4 kHz)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Agudos (> 4 kHz)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    text = "${if (uiState.equalizerConfig.trebleGainDb > 0) "+" else ""}${uiState.equalizerConfig.trebleGainDb.toInt()} dB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCobalt
                                )
                            }
                            Slider(
                                value = uiState.equalizerConfig.trebleGainDb,
                                onValueChange = onTrebleGainChanged,
                                valueRange = -12f..12f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioCobalt,
                                    activeTrackColor = StudioCobalt,
                                    inactiveTrackColor = StudioSurfaceBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Refuerzo de graves (Bass Boost 0 .. 100%)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Refuerzo de Graves (Bass Boost)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    text = "${uiState.equalizerConfig.bassBoostPercent}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioIndigo
                                )
                            }
                            Slider(
                                value = uiState.equalizerConfig.bassBoostPercent.toFloat(),
                                onValueChange = { onBassBoostChanged(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioIndigo,
                                    activeTrackColor = StudioIndigo,
                                    inactiveTrackColor = StudioSurfaceBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onResetEqualizer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioSurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioSurface,
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Restablecer ecualizador a plano (0 dB)", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BOTÓN DE ACCIÓN: GUARDAR COPIA MODIFICADA (PROCESAMIENTO REAL)
            Button(
                onClick = onSaveModifiedCopy,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_modified_copy_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCobalt,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Procesando audio (${(uiState.saveProgress * 100).toInt()}%)…",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Copia Procesada",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "✓ El archivo original permanece intacto. Se exporta una nueva copia WAV en segundo plano.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Diálogo de progreso de exportación
    if (uiState.isSaving) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = StudioSurfaceElevated,
            title = {
                Text(
                    text = "Procesando y Exportando Audio",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aplicando velocidad ${String.format(Locale.US, "%.2fx", uiState.speed)}, tono ${if (uiState.semitones >= 0) "+${uiState.semitones}" else "${uiState.semitones}"} st y filtros DSP...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { uiState.saveProgress },
                        color = StudioCobalt,
                        trackColor = StudioSurfaceBorder,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(uiState.saveProgress * 100).toInt()}% completado",
                        color = StudioCobalt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Diálogo de éxito
    if (uiState.saveSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            containerColor = StudioSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StudioEmerald,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Copia Exportada con Éxito",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = uiState.saveSuccessMessage,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismissSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        )
    }

    // Diálogo de error
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            containerColor = StudioSurfaceElevated,
            title = {
                Text(
                    text = "Aviso de Error",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = uiState.errorMessage,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("Cerrar", color = StudioCobalt)
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
