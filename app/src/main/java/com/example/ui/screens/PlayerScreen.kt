package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.audio.PlayerRepeatMode
import com.example.audio.PlayerState
import com.example.data.model.Song
import com.example.ui.components.QueueBottomSheet
import com.example.ui.theme.ControlSizeChoice
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeatMode: () -> Unit,
    onToggleShuffle: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSelectSongFromQueue: (Song) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onOpenEqualizer: (() -> Unit)? = null,
    controlSizeChoice: ControlSizeChoice = ControlSizeChoice.NORMAL,
    modifier: Modifier = Modifier
) {
    val currentSong = playerState.currentSong

    // Fondo ambiental oscuro elegante con degradado sutil turquesa de MeloTune
    val dynamicBackground = remember(currentSong?.id) {
        val topTint = if (currentSong != null) {
            val hash = (currentSong.id xor currentSong.title.hashCode().toLong())
            val hueShift = ((hash and 0xFF) / 255f) * 0.15f
            Color(0xFF071B20).copy(alpha = 0.85f + hueShift)
        } else {
            Color(0xFF091216)
        }
        Brush.verticalGradient(
            colors = listOf(
                topTint,
                Color(0xFF0C1014),
                Color(0xFF07090C)
            )
        )
    }

    if (currentSong == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C10)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Ninguna canción en reproducción",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona una pista en tu biblioteca para comenzar",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isUserScrubbing) scrubPositionMs.toLong() else playerState.currentPositionMs
    val totalDuration = playerState.totalDurationMs.coerceAtLeast(1L)
    val remainingMs = (totalDuration - currentPosition).coerceAtLeast(0L)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(dynamicBackground)
    ) {
        val availableHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Encabezado pequeño con acceso a cola y herramientas discretas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Acceso a la cola
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("open_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Cola",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cola (${playerState.queue.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Herramientas discretas (Ecualizador y Sleep Timer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenEqualizer != null) {
                        IconButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("open_equalizer_from_player")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Ecualizador",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Temporizador de apagado",
                            tint = if (playerState.sleepTimerRemainingSeconds != null) MeloTurquoise else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. Portada cuadrada grande, centrada y adaptable al espacio disponible
            val artworkMaxSize = (availableHeight * 0.42f).coerceIn(180.dp, 340.dp)
            Box(
                modifier = Modifier
                    .size(artworkMaxSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (currentSong.albumArtUri != null) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = remember(currentSong.albumArtUri) {
                            ImageRequest.Builder(context)
                                .data(currentSong.albumArtUri)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = "Carátula de ${currentSong.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MeloTurquoise.copy(alpha = 0.7f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // Bloque inferior agrupado: Metadatos, Progreso, Controles y Volumen
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3. Título de la canción y artista, con favorito a un lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSong.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(currentSong) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (currentSong.isFavorite) StudioCrimson else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Barra de progreso delgada con tiempo transcurrido y duración
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
                            thumbColor = MeloTurquoise,
                            activeTrackColor = MeloTurquoise,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .testTag("player_timeline_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "-${formatTime(remainingMs)}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Controles en una sola fila: aleatorio, anterior, reproducir/pausar, siguiente y repetir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Aleatorio
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (playerState.isShuffle) MeloTurquoise else TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Anterior
                    IconButton(
                        onClick = onSkipToPrevious,
                        enabled = playerState.hasPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_skip_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = if (playerState.hasPrevious) TextPrimary else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Reproducir / Pausar destacado moderadamente en turquesa
                    Button(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_play_pause_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeloTurquoise,
                            contentColor = Color.Black
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Siguiente
                    IconButton(
                        onClick = onSkipToNext,
                        enabled = playerState.hasNext,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_skip_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = if (playerState.hasNext) TextPrimary else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repetir
                    IconButton(
                        onClick = onToggleRepeatMode,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_repeat_button")
                    ) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                PlayerRepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repetir",
                            tint = if (playerState.repeatMode != PlayerRepeatMode.OFF) MeloTurquoise else TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Controles secundarios discretos (Volumen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = playerState.volume,
                        onValueChange = onVolumeChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = TextSecondary,
                            activeTrackColor = TextSecondary.copy(alpha = 0.6f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // Modal de la Cola de Canciones
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playerState.queue,
            currentIndex = playerState.currentIndex,
            onSelectSong = { song ->
                onSelectSongFromQueue(song)
                showQueueSheet = false
            },
            onRemoveFromQueue = onRemoveFromQueue,
            onDismiss = { showQueueSheet = false }
        )
    }

    // Diálogo del Temporizador de Apagado
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = StudioSurfaceElevated,
            title = {
                Text(
                    text = "Temporizador de Apagado",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15 minutos", 30 to "30 minutos", 45 to "45 minutos", 60 to "1 hora").forEach { (mins, label) ->
                        Button(
                            onClick = {
                                onSetSleepTimer(mins)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioSurfaceHighlight,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(label)
                        }
                    }

                    if (playerState.sleepTimerRemainingSeconds != null) {
                        Button(
                            onClick = {
                                onSetSleepTimer(null)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioCrimson.copy(alpha = 0.2f),
                                contentColor = StudioCrimson
                            )
                        ) {
                            Text("Cancelar temporizador activo")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cerrar", color = MeloTurquoise)
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
