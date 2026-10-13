package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StudioAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acceso a música requerido",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Para escuchar tus canciones locales, concede permiso de lectura de audio.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { permissionLauncher.launch(audioPermission) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Permitir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Barra de búsqueda rápida
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Buscar por título, artista o álbum...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Borrar búsqueda",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("library_search_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = StudioSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Pestañas / Filtros organizados: Canciones, Álbumes, Artistas, Carpetas, Favoritos, Listas
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ALL,
                    onClick = { onFilterChanged(LibraryFilter.ALL) },
                    label = { Text("Canciones (${uiState.songs.size})", fontSize = 12.sp) },
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
                val albumsCount = uiState.songs.map { it.album }.distinct().size
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ALBUMS,
                    onClick = { onFilterChanged(LibraryFilter.ALBUMS) },
                    label = { Text("Álbumes ($albumsCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.ALBUMS,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                val artistsCount = uiState.songs.map { it.artist }.distinct().size
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.ARTISTS,
                    onClick = { onFilterChanged(LibraryFilter.ARTISTS) },
                    label = { Text("Artistas ($artistsCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.ARTISTS,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                val foldersCount = uiState.songs.map { it.folderName }.distinct().size
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.FOLDERS,
                    onClick = { onFilterChanged(LibraryFilter.FOLDERS) },
                    label = { Text("Carpetas ($foldersCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.FOLDERS,
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
                val plCount = uiState.playlists.size
                FilterChip(
                    selected = uiState.libraryFilter == LibraryFilter.PLAYLISTS,
                    onClick = { onFilterChanged(LibraryFilter.PLAYLISTS) },
                    label = { Text("Listas ($plCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.libraryFilter == LibraryFilter.PLAYLISTS,
                        borderColor = StudioSurfaceBorder,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Barra de navegación de categoría seleccionada o botón de abrir archivo
        if (uiState.selectedCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        tint = MaterialTheme.colorScheme.primary,
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir todo", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onPlayAllFiltered(uiState.filteredSongs, true) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = TextSecondary)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("pick_file_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                        containerColor = StudioSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir archivo", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                if (uiState.libraryFilter == LibraryFilter.PLAYLISTS) {
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("create_playlist_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva Lista", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Contenido principal de la biblioteca
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Leyendo pistas del dispositivo…",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Modo Álbumes (sin seleccionar un álbum específico)
                uiState.libraryFilter == LibraryFilter.ALBUMS && uiState.selectedCategory == null -> {
                    val albumMap = uiState.songs.groupBy { it.album }
                    if (albumMap.isEmpty()) {
                        EmptyCategoryState("No se encontraron álbumes en el dispositivo.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(albumMap.keys.sorted()) { albumName ->
                                val count = albumMap[albumName]?.size ?: 0
                                val artist = albumMap[albumName]?.firstOrNull()?.artist ?: "Varios Artistas"
                                CategoryCard(
                                    title = albumName,
                                    subtitle = "$artist • $count canciones",
                                    icon = Icons.Default.Album,
                                    onClick = { onSelectCategory(albumName) }
                                )
                            }
                        }
                    }
                }

                // Modo Artistas (sin seleccionar un artista específico)
                uiState.libraryFilter == LibraryFilter.ARTISTS && uiState.selectedCategory == null -> {
                    val artistMap = uiState.songs.groupBy { it.artist }
                    if (artistMap.isEmpty()) {
                        EmptyCategoryState("No se encontraron artistas en el dispositivo.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(artistMap.keys.sorted()) { artistName ->
                                val count = artistMap[artistName]?.size ?: 0
                                CategoryCard(
                                    title = artistName,
                                    subtitle = "$count pistas disponibles",
                                    icon = Icons.Default.Person,
                                    onClick = { onSelectCategory(artistName) }
                                )
                            }
                        }
                    }
                }

                // Modo Carpetas (sin seleccionar una carpeta específica)
                uiState.libraryFilter == LibraryFilter.FOLDERS && uiState.selectedCategory == null -> {
                    val folderMap = uiState.songs.groupBy { it.folderName }
                    if (folderMap.isEmpty()) {
                        EmptyCategoryState("No se encontraron carpetas con música.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(folderMap.keys.sorted()) { folderName ->
                                val count = folderMap[folderName]?.size ?: 0
                                CategoryCard(
                                    title = folderName,
                                    subtitle = "$count canciones en la carpeta",
                                    icon = Icons.Default.Folder,
                                    onClick = { onSelectCategory(folderName) }
                                )
                            }
                        }
                    }
                }

                // Modo Listas de Reproducción (sin seleccionar una lista específica)
                uiState.libraryFilter == LibraryFilter.PLAYLISTS && uiState.selectedCategory == null -> {
                    if (uiState.playlists.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Aún no tienes listas de reproducción", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Crea una lista de reproducción para agrupar tus canciones favoritas.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, textAlign = TextAlign.Center))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Crear Lista de Reproducción", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.playlists.keys.sorted()) { playlistName ->
                                val songIds = uiState.playlists[playlistName] ?: emptyList()
                                CategoryCard(
                                    title = playlistName,
                                    subtitle = "${songIds.size} canciones en la lista",
                                    icon = Icons.Default.FolderShared,
                                    onClick = { onSelectCategory(playlistName) },
                                    trailingAction = {
                                        IconButton(
                                            onClick = { onDeletePlaylist(playlistName) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar lista",
                                                tint = StudioCrimson,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Lista de Canciones (directa o dentro de una categoría filtrada)
                uiState.filteredSongs.isEmpty() -> {
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
                            } else {
                                "No se encontraron canciones en esta sección"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Añade música a tu teléfono o ábrela con el botón de examinar.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
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
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onEditMetadata = { songToEditMetadata = song }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cuadro de diálogo para crear lista de reproducción
    if (showCreatePlaylistDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(
                    text = "Nueva Lista de Reproducción",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Introduce el nombre para tu nueva lista:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = { Text("Ej. Favoritas del verano") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = StudioSurfaceBorder
                        )
                    )
                }
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
                        containerColor = MaterialTheme.colorScheme.primary,
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

    // Cuadro de diálogo para añadir una pista a una lista de reproducción
    songToAddToPlaylist?.let { targetSong ->
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = {
                Text(
                    text = "Añadir a Lista de Reproducción",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                if (uiState.playlists.isEmpty()) {
                    Text(
                        text = "Aún no has creado ninguna lista de reproducción. Crea una primero.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.playlists.keys.sorted()) { plName ->
                            Surface(
                                color = StudioSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToPlaylist(plName, targetSong)
                                        songToAddToPlaylist = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = plName, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                }
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Crear una lista", fontWeight = FontWeight.Bold)
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
                        Text("+ Nueva Lista", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            containerColor = StudioSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
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

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
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
}

@Composable
private fun EmptyCategoryState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
