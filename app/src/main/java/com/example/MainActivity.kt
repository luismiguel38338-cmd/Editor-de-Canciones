package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.ExportsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongEditorScreen
import com.example.ui.screens.SplashScreen
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
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "splash_transition"
                    ) { isSplashActive ->
                        if (isSplashActive) {
                            SplashScreen(
                                onAnimationFinished = { showSplash = false }
                            )
                        } else {
                            SongEditorMainApp()
                        }
                    }
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
                // Mini-Player persistente si hay canción cargada y no estamos en la pestaña Editor
                AnimatedVisibility(
                    visible = uiState.selectedSong != null && uiState.currentTab != NavigationTab.EDITOR,
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
                            onOpenEditor = { viewModel.navigateTo(NavigationTab.EDITOR) }
                        )
                    }
                }

                // Barra de navegación inferior fija
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
                        label = { Text("Biblioteca", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
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
                        selected = uiState.currentTab == NavigationTab.EDITOR,
                        onClick = { viewModel.navigateTo(NavigationTab.EDITOR) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Editor"
                            )
                        },
                        label = { Text("Editor", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
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
                        label = { Text("Exportaciones", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
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
                        label = { Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
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
                        onSongSelected = { song -> viewModel.selectSong(song, autoOpenEditor = true) },
                        onPermissionResult = { granted -> viewModel.onPermissionResult(granted) },
                        onSearchQueryChanged = { q -> viewModel.onSearchQueryChanged(q) },
                        onSortOrderChanged = { order -> viewModel.setSortOrder(order) },
                        onRefresh = { viewModel.loadSongs() },
                        onLoadSampleTracks = { viewModel.loadSampleTracksOnly() },
                        onPickedAudioUri = { uri -> viewModel.onPickedAudioUri(uri) }
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
                        onPlayTrack = { song -> viewModel.selectSong(song, autoOpenEditor = true) },
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
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = StudioCobalt,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Ninguna pista cargada",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecciona una canción en tu biblioteca para comenzar a editar tono, velocidad y ecualización en tiempo real.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBrowseLibrary,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioCobalt,
                contentColor = Color.White
            ),
            modifier = Modifier.testTag("browse_library_from_editor")
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Examinar Biblioteca", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
