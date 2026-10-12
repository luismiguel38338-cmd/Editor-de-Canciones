package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongItem
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.LibrarySortOrder
import com.example.ui.viewmodel.SongEditorUiState

@Composable
fun LibraryScreen(
    uiState: SongEditorUiState,
    onSongSelected: (Song) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (LibrarySortOrder) -> Unit,
    onFilterChanged: (LibraryFilter) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onOpenSongInEditor: (Song) -> Unit,
    onSaveCustomMetadata: (Long, String?, String?) -> Unit,
    onResetCustomMetadata: (Long) -> Unit,
    onRefresh: () -> Unit,
    onPickedAudioUri: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPickedAudioUri(it) }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    LaunchedEffect(Unit) {
        if (!uiState.permissionGranted) {
            permissionLauncher.launch(audioPermission)
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var songToEditMetadata by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        // Encabezado de la Biblioteca
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${uiState.filteredSongs.size} canciones disponibles",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Menú de ordenación
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioSurface)
                            .border(1.dp, StudioSurfaceBorder, CircleShape)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar canciones",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(StudioSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Por título (A-Z)", color = TextPrimary) },
                            onClick = {
                                onSortOrderChanged(LibrarySortOrder.TITLE)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por artista", color = TextPrimary) },
                            onClick = {
                                onSortOrderChanged(LibrarySortOrder.ARTIST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por álbum", color = TextPrimary) },
                            onClick = {
                                onSortOrderChanged(LibrarySortOrder.ALBUM)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Por duración (Mayor)", color = TextPrimary) },
                            onClick = {
                                onSortOrderChanged(LibrarySortOrder.DURATION)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Favoritos primero", color = TextPrimary) },
                            onClick = {
                                onSortOrderChanged(LibrarySortOrder.FAVORITES)
                                showSortMenu = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioSurface)
                        .border(1.dp, StudioSurfaceBorder, CircleShape)
                        .testTag("refresh_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar biblioteca",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Permiso si aún no se ha otorgado
        if (!uiState.permissionGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StudioAmber,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acceso a música local",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Concede acceso para escanear las canciones de tu dispositivo.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(audioPermission) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("grant_permission_library")
                    ) {
                        Text("Permitir", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Barra de Búsqueda
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Buscar por título, artista o álbum…", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("search_song_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = StudioSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Chips de filtro rápido (Todas, Favoritos, Editadas)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ALL,
                    onClick = { onFilterChanged(LibraryFilter.ALL) },
                    label = { Text("Todas (${uiState.songs.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.ALL,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                val favCount = uiState.songs.count { it.isFavorite }
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.FAVORITES,
                    onClick = { onFilterChanged(LibraryFilter.FAVORITES) },
                    label = { Text("Favoritos ($favCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.FAVORITES,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                val editCount = uiState.songs.count { it.isSavedEdit }
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.EDITED,
                    onClick = { onFilterChanged(LibraryFilter.EDITED) },
                    label = { Text("Editadas ($editCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.EDITED,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Botón para examinar y abrir archivos desde almacenamiento local
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedButton(
                onClick = { filePickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("pick_file_button"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary,
                    containerColor = StudioSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir archivo de audio desde almacenamiento", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Lista de canciones
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Leyendo pistas del dispositivo…",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (uiState.filteredSongs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) {
                            "Sin resultados para '${uiState.searchQuery}'"
                        } else if (uiState.libraryFilter == LibraryFilter.FAVORITES) {
                            "Aún no tienes canciones favoritas"
                        } else if (uiState.libraryFilter == LibraryFilter.EDITED) {
                            "Aún no hay canciones editadas"
                        } else {
                            "No se encontraron canciones en el dispositivo"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Selecciona cualquier archivo de audio de tu almacenamiento con el botón inferior.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("empty_state_browse_audio")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar y abrir archivo de audio", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("songs_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredSongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            isSelected = uiState.playerState.currentSong?.id == song.id,
                            onClick = { onSongSelected(song) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onEditMetadata = { songToEditMetadata = song },
                            onOpenEditor = { onOpenSongInEditor(song) }
                        )
                    }
                }
            }
        }
    }

    // Cuadro de diálogo para personalizar título y artista (sin tocar el audio original)
    songToEditMetadata?.let { song ->
        var editTitle by remember(song) { mutableStateOf(song.title) }
        var editArtist by remember(song) { mutableStateOf(song.artist) }

        AlertDialog(
            onDismissRequest = { songToEditMetadata = null },
            title = {
                Text(
                    text = "Personalizar Información Mostrada",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Modifica cómo se presenta esta canción en MeloTune. El archivo de audio original nunca se sobrescribirá.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título de la pista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = StudioSurfaceBorder
                        )
                    )

                    OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text("Artista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = StudioSurfaceBorder
                        )
                    )

                    if (song.hasCustomMetadata) {
                        Text(
                            text = "Original: \"${song.originalTitle}\" por ${song.originalArtist}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomMetadata(song.id, editTitle, editArtist)
                        songToEditMetadata = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (song.hasCustomMetadata) {
                        TextButton(
                            onClick = {
                                onResetCustomMetadata(song.id)
                                songToEditMetadata = null
                            }
                        ) {
                            Text("Restablecer", color = StudioAmber)
                        }
                    }
                    TextButton(onClick = { songToEditMetadata = null }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            },
            containerColor = StudioSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
