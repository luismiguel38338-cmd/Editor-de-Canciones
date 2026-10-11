package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.SongEditorScreen
import com.example.ui.screens.SongListScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SongEditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
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
                            SongEditorApp()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongEditorApp(
    viewModel: SongEditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentSong = uiState.selectedSong

    if (currentSong == null) {
        SongListScreen(
            uiState = uiState,
            onSongSelected = { song -> viewModel.selectSong(song) },
            onPermissionResult = { granted -> viewModel.onPermissionResult(granted) },
            onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
            onTabSelected = { tab -> viewModel.onTabSelected(tab) },
            onRefresh = { viewModel.loadSongs() },
            onLoadSampleTracks = { viewModel.loadSampleTracksOnly() },
            onPickedAudioUri = { uri -> viewModel.onPickedAudioUri(uri) }
        )
    } else {
        SongEditorScreen(
            song = currentSong,
            uiState = uiState,
            onBack = { viewModel.clearSelectedSong() },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeek = { positionMs -> viewModel.seekTo(positionMs) },
            onSkipForward = { viewModel.skipForward(5) },
            onSkipBackward = { viewModel.skipBackward(5) },
            onToggleLoop = { viewModel.toggleLoop() },
            onSpeedChanged = { speed -> viewModel.setSpeed(speed) },
            onSemitonesChanged = { semitones -> viewModel.setSemitones(semitones) },
            onIncrementPitch = { viewModel.incrementPitch() },
            onDecrementPitch = { viewModel.decrementPitch() },
            onResetEffects = { viewModel.resetEffects() },
            onSaveModifiedCopy = { viewModel.saveModifiedCopy() },
            onDismissSuccess = { viewModel.dismissSuccessMessage() },
            onDismissError = { viewModel.dismissErrorMessage() }
        )
    }
}
