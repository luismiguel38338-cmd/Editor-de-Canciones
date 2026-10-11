package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongItem
import com.example.ui.theme.AmberPitch
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SongEditorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    uiState: SongEditorUiState,
    onSongSelected: (Song) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadSampleTracks: () -> Unit,
    onPickedAudioUri: (android.net.Uri) -> Unit,
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

    // Auto-request permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(audioPermission)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, ElectricViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Editor de Canciones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.3.sp
                        )
                    )
                    Text(
                        text = "Ajusta velocidad y tono en tiempo real",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("refresh_songs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar lista",
                    tint = NeonCyan
                )
            }
        }

        // Permission Banner (if not granted)
        if (!uiState.permissionGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberPitch.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberPitch,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acceso a música requerido",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Concede acceso para ver y editar las canciones almacenadas en tu dispositivo.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(audioPermission) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPitch,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Conceder Permiso", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Buscar canción o artista…", color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_song_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        )

        // Action Buttons Row: File Picker & Demo Tracks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { filePickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("pick_file_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Abrir archivo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onLoadSampleTracks,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("load_samples_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ElectricViolet
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pistas Demo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Tabs: Canciones vs Mis Ediciones
        SecondaryTabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = DarkBg,
            contentColor = TextPrimary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(uiState.selectedTab, matchContentSize = true),
                    color = if (uiState.selectedTab == 0) NeonCyan else ElectricViolet
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DarkSurfaceBorder)
                )
            }
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.selectedTab == 0) NeonCyan else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Canciones (${uiState.songs.size})",
                            color = if (uiState.selectedTab == 0) NeonCyan else TextSecondary,
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                modifier = Modifier.testTag("tab_all_songs")
            )

            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.selectedTab == 1) ElectricViolet else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mis Ediciones (${uiState.savedEdits.size})",
                            color = if (uiState.selectedTab == 1) ElectricViolet else TextSecondary,
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                modifier = Modifier.testTag("tab_saved_edits")
            )
        }

        // Content Area
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
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Buscando canciones…",
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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.selectedTab == 1) Icons.Default.QueueMusic else Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (uiState.selectedTab == 1) {
                            "Aún no tienes copias editadas"
                        } else if (uiState.searchQuery.isNotEmpty()) {
                            "Sin resultados para '${uiState.searchQuery}'"
                        } else {
                            "No se encontraron canciones en el dispositivo"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (uiState.selectedTab == 1) {
                            "Modifica una canción con velocidad y tono, luego pulsa 'Guardar Copia Modificada'."
                        } else {
                            "Puedes cargar pistas de demostración o seleccionar un archivo de audio directamente."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.selectedTab == 0) {
                        Button(
                            onClick = onLoadSampleTracks,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_state_load_samples")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cargar pistas de prueba", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("songs_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredSongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            onClick = { onSongSelected(song) }
                        )
                    }
                }
            }
        }
    }
}
