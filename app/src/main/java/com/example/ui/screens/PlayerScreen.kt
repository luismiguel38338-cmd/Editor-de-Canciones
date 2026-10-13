package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

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

    if (currentSong == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioBg),
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
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ninguna canción en reproducción",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Elige una pista de tu biblioteca para reproducirla con calidad de estudio.",
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

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra superior con acciones de utilidad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Temporizador de apagado
            IconButton(
                onClick = { showSleepTimerDialog = true },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (playerState.sleepTimerRemainingSeconds != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else StudioSurface)
                    .border(1.dp, StudioSurfaceBorder, CircleShape)
                    .testTag("sleep_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = "Temporizador de apagado",
                    tint = if (playerState.sleepTimerRemainingSeconds != null) MaterialTheme.colorScheme.primary else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REPRODUCIENDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (playerState.sleepTimerRemainingSeconds != null) {
                    val mins = playerState.sleepTimerRemainingSeconds / 60
                    val secs = playerState.sleepTimerRemainingSeconds % 60
                    Text(
                        text = "Apagado en ${String.format(Locale.US, "%02d:%02d", mins, secs)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Acceso al Ecualizador y al Editor
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenEqualizer != null) {
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, CircleShape)
                            .testTag("open_equalizer_from_player")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Abrir ecualizador",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Carátula real del álbum
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurfaceElevated)
                .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(16.dp)),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = StudioCobalt,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Información de la pista y botón de favorito
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (currentSong.album.isNotBlank() && currentSong.album != "Álbum Desconocido") {
                    Text(
                        text = currentSong.album,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

        Spacer(modifier = Modifier.height(18.dp))

        // Barra de progreso y tiempos
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
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = StudioSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_timeline_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "-${formatTime(remainingMs)}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controles principales de reproducción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modo Aleatorio
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.testTag("player_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (playerState.isShuffle) MaterialTheme.colorScheme.primary else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Canción Anterior
            IconButton(
                onClick = onSkipToPrevious,
                enabled = playerState.hasPrevious,
                modifier = Modifier
                    .size(controlSizeChoice.iconButtonDp.dp)
                    .clip(CircleShape)
                    .background(StudioSurfaceHighlight)
                    .testTag("player_skip_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Canción anterior",
                    tint = if (playerState.hasPrevious) TextPrimary else TextMuted,
                    modifier = Modifier.size((controlSizeChoice.iconButtonDp / 2 + 2).dp)
                )
            }

            // Reproducir / Pausar
            Button(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(controlSizeChoice.playButtonDp.dp)
                    .testTag("player_play_pause_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size((controlSizeChoice.playButtonDp / 2 + 2).dp)
                    )
                }
            }

            // Canción Siguiente
            IconButton(
                onClick = onSkipToNext,
                enabled = playerState.hasNext,
                modifier = Modifier
                    .size(controlSizeChoice.iconButtonDp.dp)
                    .clip(CircleShape)
                    .background(StudioSurfaceHighlight)
                    .testTag("player_skip_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Canción siguiente",
                    tint = if (playerState.hasNext) TextPrimary else TextMuted,
                    modifier = Modifier.size((controlSizeChoice.iconButtonDp / 2 + 2).dp)
                )
            }

            // Modo Repetición
            IconButton(
                onClick = onToggleRepeatMode,
                modifier = Modifier.testTag("player_repeat_button")
            ) {
                Icon(
                    imageVector = when (playerState.repeatMode) {
                        PlayerRepeatMode.ONE -> Icons.Default.RepeatOne
                        PlayerRepeatMode.ALL -> Icons.Default.Repeat
                        PlayerRepeatMode.OFF -> Icons.Default.Repeat
                    },
                    contentDescription = "Repetición",
                    tint = if (playerState.repeatMode != PlayerRepeatMode.OFF) MaterialTheme.colorScheme.primary else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fader de volumen discreto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Slider(
                value = playerState.volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = TextSecondary,
                    inactiveTrackColor = StudioSurfaceBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón para desplegar la cola de reproducción
        TextButton(
            onClick = { showQueueSheet = true },
            modifier = Modifier.testTag("open_queue_button")
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cola de reproducción (${playerState.queue.size})",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
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
