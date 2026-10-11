package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.ExportsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.SongEditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBg
                ) {
                    SongEditorMainApp()
                }
            }
        }
    }
}

@Composable
fun SongEditorMainApp(
    viewModel: SongEditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Interceptar Back button para regresar a Biblioteca si estamos en otra pestaña
    BackHandler(enabled = uiState.currentTab != NavigationTab.LIBRARY) {
        viewModel.navigateTo(NavigationTab.LIBRARY)
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissInfoMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = StudioBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini-Player persistente si hay canción cargada y no estamos en la pestaña Player ni Editor
                AnimatedVisibility(
                    visible = uiState.selectedSong != null &&
                            uiState.currentTab != NavigationTab.PLAYER &&
                            uiState.currentTab != NavigationTab.EDITOR,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    uiState.selectedSong?.let { song ->
                        MiniPlayerBar(
                            song = song,
                            isPlaying = uiState.isPlaying,
                            currentPositionMs = uiState.currentPositionMs,
                            totalDurationMs = uiState.totalDurationMs,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipToNext = { viewModel.skipToNext() },
                            onOpenPlayer = { viewModel.navigateTo(NavigationTab.PLAYER) }
                        )
                    }
                }

                // Barra de navegación inferior con las 5 secciones requeridas
                NavigationBar(
                    containerColor = StudioSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = StudioSurfaceBorder,
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                        )
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.LIBRARY,
                        onClick = { viewModel.navigateTo(NavigationTab.LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = "Biblioteca"
                            )
                        },
                        label = { Text("Biblioteca", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCobalt,
                            selectedTextColor = StudioCobalt,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_library")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.PLAYER,
                        onClick = { viewModel.navigateTo(NavigationTab.PLAYER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Reproductor"
                            )
                        },
                        label = { Text("Reproductor", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCobalt,
                            selectedTextColor = StudioCobalt,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_player")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.EDITOR,
                        onClick = { viewModel.navigateTo(NavigationTab.EDITOR) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Editor"
                            )
                        },
                        label = { Text("Editor", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCobalt,
                            selectedTextColor = StudioCobalt,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_editor")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.EXPORTS,
                        onClick = { viewModel.navigateTo(NavigationTab.EXPORTS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FolderShared,
                                contentDescription = "Exportaciones"
                            )
                        },
                        label = { Text("Exportaciones", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCobalt,
                            selectedTextColor = StudioCobalt,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_exports")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.SETTINGS,
                        onClick = { viewModel.navigateTo(NavigationTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes"
                            )
                        },
                        label = { Text("Ajustes", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StudioCobalt,
                            selectedTextColor = StudioCobalt,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                NavigationTab.LIBRARY -> {
                    LibraryScreen(
                        uiState = uiState,
                        onSongSelected = { song -> viewModel.playSong(song, autoOpenPlayer = true) },
                        onPermissionResult = { granted -> viewModel.onPermissionResult(granted) },
                        onSearchQueryChanged = { q -> viewModel.onSearchQueryChanged(q) },
                        onSortOrderChanged = { order -> viewModel.setSortOrder(order) },
                        onRefresh = { viewModel.loadSongs() },
                        onPickedAudioUri = { uri -> viewModel.onPickedAudioUri(uri) }
                    )
                }

                NavigationTab.PLAYER -> {
                    PlayerScreen(
                        playerState = uiState.playerState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipToNext = { viewModel.skipToNext() },
                        onSkipToPrevious = { viewModel.skipToPrevious() },
                        onSeek = { pos -> viewModel.seekTo(pos) },
                        onToggleRepeatMode = { viewModel.toggleRepeatMode() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onVolumeChanged = { vol -> viewModel.setVolume(vol) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onSelectSongFromQueue = { song -> viewModel.playSong(song) },
                        onRemoveFromQueue = { idx -> viewModel.removeFromQueue(idx) },
                        onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
                        onOpenEditor = { viewModel.navigateTo(NavigationTab.EDITOR) }
                    )
                }

                NavigationTab.EDITOR -> {
                    val currentSong = uiState.selectedSong
                    if (currentSong != null) {
                        SongEditorScreen(
                            song = currentSong,
                            uiState = uiState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeek = { pos -> viewModel.seekTo(pos) },
                            onSkipForward = { viewModel.skipForward(5) },
                            onSkipBackward = { viewModel.skipBackward(5) },
                            onToggleLoop = { viewModel.toggleLoop() },
                            onVolumeChanged = { vol -> viewModel.setVolume(vol) },
                            onSpeedChanged = { spd -> viewModel.setSpeed(spd) },
                            onSemitonesChanged = { semi -> viewModel.setSemitones(semi) },
                            onIncrementPitch = { viewModel.incrementPitch() },
                            onDecrementPitch = { viewModel.decrementPitch() },
                            onBassGainChanged = { db -> viewModel.setBassGain(db) },
                            onMidGainChanged = { db -> viewModel.setMidGain(db) },
                            onTrebleGainChanged = { db -> viewModel.setTrebleGain(db) },
                            onBassBoostChanged = { pct -> viewModel.setBassBoost(pct) },
                            onResetPitchAndSpeed = { viewModel.resetPitchAndSpeed() },
                            onResetEqualizer = { viewModel.resetEqualizer() },
                            onResetAllEffects = { viewModel.resetAllEffects() },
                            onSaveModifiedCopy = { viewModel.saveModifiedCopy() },
                            onDismissSuccess = { viewModel.dismissSuccessMessage() },
                            onDismissError = { viewModel.dismissErrorMessage() }
                        )
                    } else {
                        // Estado cuando aún no hay pista seleccionada en el Editor
                        EmptyEditorState(
                            onBrowseLibrary = { viewModel.navigateTo(NavigationTab.LIBRARY) }
                        )
                    }
                }

                NavigationTab.EXPORTS -> {
                    ExportsScreen(
                        exportedTracks = uiState.exportedTracks,
                        onPlayTrack = { song -> viewModel.playSong(song, autoOpenPlayer = true) },
                        onDeleteTrack = { track -> viewModel.deleteExportedTrack(track) },
                        onRefresh = { viewModel.loadExports() }
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        onClearCache = { viewModel.clearCache() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorState(
    onBrowseLibrary: () -> Unit
) {
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
                .background(StudioCobalt.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = StudioCobalt,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Ninguna pista seleccionada",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona una canción en la biblioteca o reproductor para ajustar velocidad, tono, ecualización y exportar una copia procesada.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBrowseLibrary,
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioCobalt
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("browse_library_button")
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Explorar Biblioteca", fontWeight = FontWeight.SemiBold)
        }
    }
}
