package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.SongEditorUiState
import com.example.ui.viewmodel.SongEditorViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SongEditorViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = uiState.themeMode,
                accentColorChoice = uiState.accentColorChoice
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showIntro by remember { mutableStateOf(true) }

                    if (showIntro) {
                        ColdStartSplash(onFinish = { showIntro = false })
                    } else {
                        MeloTuneMainApp(viewModel = viewModel, uiState = uiState)
                    }
                }
            }
        }
    }
}

@Composable
fun ColdStartSplash(onFinish: () -> Unit) {
    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(600) // Breve presentación elegante sin bloquear al usuario
        alphaAnim.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinish() }, // Toque para saltar inmediatamente
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim.value)
                .padding(32.dp)
        ) {
            // Círculo degradado turquesa con nota musical negra MeloTune
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MeloTurquoise,
                                Color(0xFF00B4D8),
                                Color(0xFF0077B6)
                            )
                        )
                    )
                    .border(3.dp, Color(0xFF48CAE4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF0D0F14),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MeloTune",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "REPRODUCTOR MUSICAL V2",
                color = MeloTurquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun MeloTuneMainApp(
    viewModel: SongEditorViewModel,
    uiState: SongEditorUiState
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Interceptar botón atrás para regresar a Biblioteca si estamos en otra pestaña
    BackHandler(enabled = uiState.currentTab != NavigationTab.LIBRARY || uiState.selectedCategory != null) {
        if (uiState.selectedCategory != null) {
            viewModel.selectCategory(null)
        } else {
            viewModel.navigateTo(NavigationTab.LIBRARY)
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissInfoMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissErrorMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini-Player persistente si hay canción cargada y no estamos en la pestaña Reproductor a pantalla completa
                AnimatedVisibility(
                    visible = uiState.selectedSong != null &&
                            uiState.currentTab != NavigationTab.PLAYER,
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

                // Barra de navegación inferior compacta con las 5 secciones de MeloTune
                NavigationBar(
                    containerColor = StudioSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
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
                        label = {
                            Text(
                                text = "Biblioteca",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
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
                        label = {
                            Text(
                                text = "Reproductor",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_player")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.EQUALIZER,
                        onClick = { viewModel.navigateTo(NavigationTab.EQUALIZER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Ecualizador"
                            )
                        },
                        label = {
                            Text(
                                text = "Ecualizador",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_equalizer")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.DISCOVER,
                        onClick = { viewModel.navigateTo(NavigationTab.DISCOVER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Descubrir"
                            )
                        },
                        label = {
                            Text(
                                text = "Descubrir",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = StudioSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("nav_discover")
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
                        label = {
                            Text(
                                text = "Ajustes",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
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
                        onFilterChanged = { filter -> viewModel.setLibraryFilter(filter) },
                        onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onDeletePlaylist = { name -> viewModel.deletePlaylist(name) },
                        onAddToPlaylist = { name, song -> viewModel.addSongToPlaylist(name, song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onPlayNext = { song -> viewModel.playNext(song) },
                        onAddToQueue = { song -> viewModel.addToQueue(song) },
                        onSaveCustomMetadata = { id, title, artist -> viewModel.saveCustomMetadata(id, title, artist) },
                        onResetCustomMetadata = { id -> viewModel.resetCustomMetadata(id) },
                        onRefresh = { viewModel.loadSongs() },
                        onPickedAudioUri = { uri -> viewModel.onPickedAudioUri(uri) },
                        onPlayAllFiltered = { songs, shuffle ->
                            if (shuffle) {
                                val shuffled = songs.shuffled()
                                viewModel.playQueue(shuffled, 0, autoOpenPlayer = true)
                            } else {
                                viewModel.playQueue(songs, 0, autoOpenPlayer = true)
                            }
                        }
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
                        onOpenEqualizer = { viewModel.navigateTo(NavigationTab.EQUALIZER) },
                        controlSizeChoice = uiState.controlSizeChoice
                    )
                }

                NavigationTab.EQUALIZER -> {
                    EqualizerScreen(
                        equalizerConfig = uiState.equalizerConfig,
                        onBassGainChanged = { db -> viewModel.setBassGain(db) },
                        onMidGainChanged = { db -> viewModel.setMidGain(db) },
                        onTrebleGainChanged = { db -> viewModel.setTrebleGain(db) },
                        onBassBoostChanged = { pct -> viewModel.setBassBoost(pct) },
                        onResetEqualizer = { viewModel.resetEqualizer() },
                        onApplyPreset = { b, m, t, boost -> viewModel.applyEqualizerPreset(b, m, t, boost) }
                    )
                }

                NavigationTab.DISCOVER -> {
                    DiscoverScreen(
                        onPlayOnlineSong = { onlineSong ->
                            viewModel.playSong(onlineSong, autoOpenPlayer = true)
                        }
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        themeMode = uiState.themeMode,
                        accentColorChoice = uiState.accentColorChoice,
                        controlSizeChoice = uiState.controlSizeChoice,
                        onThemeModeChanged = { mode -> viewModel.setThemeMode(mode) },
                        onAccentColorChanged = { choice -> viewModel.setAccentColor(choice) },
                        onControlSizeChanged = { size -> viewModel.setControlSize(size) },
                        onClearCache = { viewModel.clearCache() }
                    )
                }
            }
        }
    }
}
