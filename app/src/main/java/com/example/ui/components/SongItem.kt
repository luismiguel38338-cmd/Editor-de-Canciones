package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Song
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) StudioSurfaceHighlight.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Carátula o icono sutil
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                val context = LocalContext.current
                AsyncImage(
                    model = remember(song.albumArtUri) {
                        ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Título y artista
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (song.hasCustomMetadata) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MeloTurquoise.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CUSTOM",
                            color = MeloTurquoise,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${song.artist} • ${song.formattedDuration}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Botón de favorito
        if (onToggleFavorite != null) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("favorite_button_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (song.isFavorite) StudioCrimson else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Menú contextual
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("menu_song_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones para ${song.title}",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(StudioSurfaceElevated)
            ) {
                if (onPlayNext != null) {
                    DropdownMenuItem(
                        text = { Text("Reproducir a continuación", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onPlayNext()
                            showMenu = false
                        }
                    )
                }

                if (onAddToQueue != null) {
                    DropdownMenuItem(
                        text = { Text("Añadir a la cola", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onAddToQueue()
                            showMenu = false
                        }
                    )
                }

                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Añadir a lista de reproducción", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onAddToPlaylist()
                            showMenu = false
                        }
                    )
                }

                if (onEditMetadata != null) {
                    DropdownMenuItem(
                        text = { Text("Personalizar título / artista", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onEditMetadata()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
