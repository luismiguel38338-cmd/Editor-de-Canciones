package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.audio.AudioExporter
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

data class SongEditorUiState(
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val savedEdits: List<Song> = emptyList(),
    val selectedTab: Int = 0, // 0 = Todas las canciones, 1 = Mis Ediciones
    val searchQuery: String = "",
    val selectedSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val speed: Float = 1.0f,
    val semitones: Int = 0,
    val isLooping: Boolean = false,
    val isSaving: Boolean = false,
    val saveProgress: Float = 0f,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
) {
    val pitchFactor: Float
        get() = (2.0).pow(semitones / 12.0).toFloat()

    val filteredSongs: List<Song>
        get() {
            val list = if (selectedTab == 0) songs else savedEdits
            if (searchQuery.isBlank()) return list
            return list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
}

class SongEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)
    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(SongEditorUiState())
    val uiState: StateFlow<SongEditorUiState> = _uiState.asStateFlow()

    private var progressTrackingJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return

        exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        startProgressTracking()
                    } else {
                        stopProgressTracking()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _uiState.update {
                            it.copy(
                                totalDurationMs = duration.coerceAtLeast(0L)
                            )
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        _uiState.update { it.copy(isPlaying = false, currentPositionMs = it.totalDurationMs) }
                        stopProgressTracking()
                    }
                }
            })
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            loadSongs()
        } else {
            // Still provide sample tracks so users can preview the app even without storage permission
            loadSampleTracksOnly()
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val mediaStoreSongs = repository.getDeviceSongs()
            val sampleSongs = if (mediaStoreSongs.isEmpty()) repository.generateSampleTracks() else emptyList()
            val allSongs = mediaStoreSongs + sampleSongs

            val originalSongs = allSongs.filter { !it.isSavedEdit }
            val editedSongs = allSongs.filter { it.isSavedEdit }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    songs = originalSongs,
                    savedEdits = editedSongs
                )
            }
        }
    }

    fun loadSampleTracksOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val samples = repository.generateSampleTracks()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    songs = samples,
                    savedEdits = emptyList()
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectSong(song: Song) {
        _uiState.update {
            it.copy(
                selectedSong = song,
                currentPositionMs = 0L,
                totalDurationMs = song.durationMs,
                speed = 1.0f,
                semitones = 0,
                saveSuccessMessage = null,
                errorMessage = null
            )
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.uri)
            player.setMediaItem(mediaItem)
            player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            player.repeatMode = if (_uiState.value.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun clearSelectedSong() {
        exoPlayer?.stop()
        stopProgressTracking()
        _uiState.update {
            it.copy(
                selectedSong = null,
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipForward(seconds: Int = 5) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
            player.seekTo(target)
            _uiState.update { it.copy(currentPositionMs = target) }
        }
    }

    fun skipBackward(seconds: Int = 5) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
            player.seekTo(target)
            _uiState.update { it.copy(currentPositionMs = target) }
        }
    }

    fun toggleLoop() {
        val newLoop = !_uiState.value.isLooping
        _uiState.update { it.copy(isLooping = newLoop) }
        exoPlayer?.repeatMode = if (newLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Real-time speed adjustment
    fun setSpeed(newSpeed: Float) {
        val clamped = (Math.round(newSpeed * 20.0) / 20.0).toFloat().coerceIn(0.5f, 2.0f)
        _uiState.update { it.copy(speed = clamped) }
        applyPlaybackParameters()
    }

    // Real-time pitch adjustment in semitones (-12 to +12)
    fun setSemitones(newSemitones: Int) {
        val clamped = newSemitones.coerceIn(-12, 12)
        _uiState.update { it.copy(semitones = clamped) }
        applyPlaybackParameters()
    }

    fun incrementPitch() {
        setSemitones(_uiState.value.semitones + 1)
    }

    fun decrementPitch() {
        setSemitones(_uiState.value.semitones - 1)
    }

    fun resetEffects() {
        _uiState.update { it.copy(speed = 1.0f, semitones = 0) }
        applyPlaybackParameters()
    }

    private fun applyPlaybackParameters() {
        val currentSpeed = _uiState.value.speed
        val pitch = _uiState.value.pitchFactor
        exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, pitch)
    }

    fun onPickedAudioUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val song = repository.getSongFromPickedUri(uri)
            if (song != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        songs = listOf(song) + state.songs.filter { it.id != song.id }
                    )
                }
                selectSong(song)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo leer el archivo de audio seleccionado."
                    )
                }
            }
        }
    }

    fun saveModifiedCopy() {
        val currentSong = _uiState.value.selectedSong ?: return
        val currentSpeed = _uiState.value.speed
        val currentSemitones = _uiState.value.semitones
        val currentPitchFactor = _uiState.value.pitchFactor

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    saveProgress = 0f,
                    errorMessage = null,
                    saveSuccessMessage = null
                )
            }

            val result = AudioExporter.exportModifiedSong(
                context = getApplication(),
                song = currentSong,
                speed = currentSpeed,
                semitones = currentSemitones,
                pitchFactor = currentPitchFactor,
                onProgress = { progress ->
                    _uiState.update { it.copy(saveProgress = progress) }
                }
            )

            result.onSuccess { savedSong ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveProgress = 1.0f,
                        saveSuccessMessage = "¡Copia guardada con éxito como '${savedSong.title}'! El original permanece intacto.",
                        savedEdits = listOf(savedSong) + state.savedEdits
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Error al exportar copia: ${err.localizedMessage ?: "Ocurrió un error inesperado"}"
                    )
                }
            }
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _uiState.update {
                            it.copy(
                                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                                totalDurationMs = player.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}
