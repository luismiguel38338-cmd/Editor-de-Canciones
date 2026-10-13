package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongItem
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioSurface
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
    onSelectCategory: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onSaveCustomMetadata: (Long, String?, String?) -> Unit,
    onResetCustomMetadata: (Long) -> Unit,
    onRefresh: () -> Unit,
    onPickedAudioUri: (Uri) -> Unit,
    onPlayAllFiltered: (List<Song>, Boolean) -> Unit = { _, _ -> },
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
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        // Encabezado compacto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${uiState.filteredSongs.size})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Abrir archivo local
                IconButton(
                    onClick = { filePickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pick_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Abrir archivo de audio",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Menú de ordenación
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar canciones",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
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
                            text = { Text("Por duración", color = TextPrimary) },
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

                // Actualizar
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Buscador sencillo sin bordes excesivos
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Buscar canción, artista o álbum...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Borrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .height(48.dp)
                .testTag("library_search_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = MeloTurquoise.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Filtros horizontales limpios (Canciones, Álbumes, Artistas, Carpetas, Favoritos, Listas)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ALL,
                    onClick = { onFilterChanged(LibraryFilter.ALL) },
                    label = { Text("Canciones", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ALBUMS,
                    onClick = { onFilterChanged(LibraryFilter.ALBUMS) },
                    label = { Text("Álbumes", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ARTISTS,
                    onClick = { onFilterChanged(LibraryFilter.ARTISTS) },
                    label = { Text("Artistas", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.FOLDERS,
                    onClick = { onFilterChanged(LibraryFilter.FOLDERS) },
                    label = { Text("Carpetas", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.FAVORITES,
                    onClick = { onFilterChanged(LibraryFilter.FAVORITES) },
                    label = { Text("Favoritos", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.PLAYLISTS,
                    onClick = { onFilterChanged(LibraryFilter.PLAYLISTS) },
                    label = { Text("Listas", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
        }

        // Barra de navegación de categoría seleccionada o botón crear lista
        if (uiState.selectedCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSelectCategory(null) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MeloTurquoise,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.selectedCategory,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = { onPlayAllFiltered(uiState.filteredSongs, false) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir todo", tint = MeloTurquoise)
                    }
                    IconButton(
                        onClick = { onPlayAllFiltered(uiState.filteredSongs, true) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = TextSecondary)
                    }
                }
            }
        } else if (uiState.libraryFilter == LibraryFilter.PLAYLISTS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showCreatePlaylistDialog = true },
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MeloTurquoise)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Lista", fontSize = 12.sp, color = MeloTurquoise, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Contenido de la biblioteca
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MeloTurquoise, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Cargando biblioteca...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Álbumes
                uiState.libraryFilter == LibraryFilter.ALBUMS && uiState.selectedCategory == null -> {
                    val albumMap = uiState.songs.groupBy { it.album }
                    if (albumMap.isEmpty()) {
                        EmptyListState("No se encontraron álbumes en el teléfono.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(albumMap.keys.sorted()) { albumName ->
                                val count = albumMap[albumName]?.size ?: 0
                                val artist = albumMap[albumName]?.firstOrNull()?.artist ?: "Varios Artistas"
                                CleanCategoryRow(
                                    title = albumName,
                                    subtitle = "$artist • $count canciones",
                                    icon = Icons.Default.Album,
                                    onClick = { onSelectCategory(albumName) }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
                }

                // Artistas
                uiState.libraryFilter == LibraryFilter.ARTISTS && uiState.selectedCategory == null -> {
                    val artistMap = uiState.songs.groupBy { it.artist }
                    if (artistMap.isEmpty()) {
                        EmptyListState("No se encontraron artistas.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(artistMap.keys.sorted()) { artistName ->
                                val count = artistMap[artistName]?.size ?: 0
                                CleanCategoryRow(
                                    title = artistName,
                                    subtitle = "$count canciones",
                                    icon = Icons.Default.Person,
                                    onClick = { onSelectCategory(artistName) }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
                }

                // Carpetas
                uiState.libraryFilter == LibraryFilter.FOLDERS && uiState.selectedCategory == null -> {
                    val folderMap = uiState.songs.groupBy { it.folderName }
                    if (folderMap.isEmpty()) {
                        EmptyListState("No se encontraron carpetas con música.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(folderMap.keys.sorted()) { folderName ->
                                val count = folderMap[folderName]?.size ?: 0
                                CleanCategoryRow(
                                    title = folderName,
                                    subtitle = "$count canciones",
                                    icon = Icons.Default.Folder,
                                    onClick = { onSelectCategory(folderName) }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
                }

                // Listas
                uiState.libraryFilter == LibraryFilter.PLAYLISTS && uiState.selectedCategory == null -> {
                    if (uiState.playlists.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Sin listas de reproducción", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Crea una lista para organizar tus canciones favoritas.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center))
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MeloTurquoise, contentColor = Color.Black),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Crear Lista", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(uiState.playlists.keys.sorted()) { playlistName ->
                                val songIds = uiState.playlists[playlistName] ?: emptyList()
                                CleanCategoryRow(
                                    title = playlistName,
                                    subtitle = "${songIds.size} canciones",
                                    icon = Icons.Default.FolderShared,
                                    onClick = { onSelectCategory(playlistName) },
                                    trailingAction = {
                                        IconButton(
                                            onClick = { onDeletePlaylist(playlistName) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar lista",
                                                tint = StudioCrimson.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
                }

                // Lista de Canciones
                uiState.filteredSongs.isEmpty() -> {
                    EmptyListState(
                        if (uiState.searchQuery.isNotEmpty()) "Sin resultados para '${uiState.searchQuery}'"
                        else if (uiState.libraryFilter == LibraryFilter.FAVORITES) "Aún no tienes canciones favoritas"
                        else "No se encontraron canciones"
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("songs_list"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onEditMetadata = { songToEditMetadata = song }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cuadro de diálogo para crear lista
    if (showCreatePlaylistDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    text = "Nueva Lista de Reproducción",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Nombre de la lista...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MeloTurquoise,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            onCreatePlaylist(playlistNameInput.trim())
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeloTurquoise,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = StudioSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Cuadro de diálogo para añadir una pista a una lista
    songToAddToPlaylist?.let { targetSong ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = {
                Text(
                    text = "Añadir a Lista",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            },
            text = {
                if (uiState.playlists.isEmpty()) {
                    Text(
                        text = "No hay listas creadas. Crea una lista primero.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.playlists.keys.sorted()) { plName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAddToPlaylist(plName, targetSong)
                                        songToAddToPlaylist = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderShared, contentDescription = null, tint = MeloTurquoise, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = plName, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.playlists.isEmpty()) {
                    Button(
                        onClick = {
                            songToAddToPlaylist = null
                            showCreatePlaylistDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeloTurquoise,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Crear lista", fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { songToAddToPlaylist = null }) {
                        Text("Cerrar", color = TextSecondary)
                    }
                }
            },
            dismissButton = {
                if (uiState.playlists.isNotEmpty()) {
                    TextButton(onClick = {
                        songToAddToPlaylist = null
                        showCreatePlaylistDialog = true
                    }) {
                        Text("+ Nueva", color = MeloTurquoise)
                    }
                }
            },
            containerColor = StudioSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Personalizar metadatos
    songToEditMetadata?.let { song ->
        var editTitle by remember(song) { mutableStateOf(song.title) }
        var editArtist by remember(song) { mutableStateOf(song.artist) }

        AlertDialog(
            onDismissRequest = { songToEditMetadata = null },
            title = {
                Text(
                    text = "Editar Nombre en MeloTune",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MeloTurquoise,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
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
                            focusedBorderColor = MeloTurquoise,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomMetadata(song.id, editTitle, editArtist)
                        songToEditMetadata = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeloTurquoise,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
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

@Composable
private fun CleanCategoryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MeloTurquoise,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
private fun EmptyListState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
