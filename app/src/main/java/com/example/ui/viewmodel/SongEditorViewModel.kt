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
import com.example.audio.EqualizerConfig
import com.example.audio.LiveAudioEffectsManager
import com.example.data.model.ExportedTrack
import com.example.data.model.Song
import com.example.data.repository.ExportsRepository
import com.example.data.repository.SongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.pow

enum class NavigationTab {
    LIBRARY,
    EDITOR,
    EXPORTS,
    SETTINGS
}

enum class LibrarySortOrder {
    TITLE,
    ARTIST,
    DURATION
}

data class SongEditorUiState(
    val currentTab: NavigationTab = NavigationTab.LIBRARY,
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val exportedTracks: List<ExportedTrack> = emptyList(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.TITLE,
    val searchQuery: String = "",
    val selectedSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val semitones: Int = 0,
    val equalizerConfig: EqualizerConfig = EqualizerConfig(),
    val isLooping: Boolean = false,
    val isSaving: Boolean = false,
    val saveProgress: Float = 0f,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val pitchFactor: Float
        get() = (2.0).pow(semitones / 12.0).toFloat()

    val filteredSongs: List<Song>
        get() {
            var list = if (searchQuery.isBlank()) songs else {
                songs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true)
                }
            }

            return when (sortOrder) {
                LibrarySortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
                LibrarySortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
                LibrarySortOrder.DURATION -> list.sortedByDescending { it.durationMs }
            }
        }
}

class SongEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val songRepository = SongRepository(application)
    private val exportsRepository = ExportsRepository(application)
    private val liveAudioEffects = LiveAudioEffectsManager()

    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(SongEditorUiState())
    val uiState: StateFlow<SongEditorUiState> = _uiState.asStateFlow()

    private var progressTrackingJob: Job? = null

    init {
        initializePlayer()
        loadExports()
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
                        liveAudioEffects.attachToSession(audioSessionId)
                        liveAudioEffects.applyConfig(_uiState.value.equalizerConfig)
                    } else if (playbackState == Player.STATE_ENDED) {
                        _uiState.update { it.copy(isPlaying = false, currentPositionMs = it.totalDurationMs) }
                        stopProgressTracking()
                    }
                }
            })
        }
    }

    fun navigateTo(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == NavigationTab.EXPORTS) {
            loadExports()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            loadSongs()
        } else {
            loadSampleTracksOnly()
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val mediaStoreSongs = songRepository.getDeviceSongs()
            val sampleSongs = if (mediaStoreSongs.isEmpty()) songRepository.generateSampleTracks() else emptyList()
            val allSongs = mediaStoreSongs + sampleSongs

            _uiState.update {
                it.copy(
                    isLoading = false,
                    songs = allSongs
                )
            }
            loadExports()
        }
    }

    fun loadSampleTracksOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val samples = songRepository.generateSampleTracks()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    songs = samples
                )
            }
        }
    }

    fun loadExports() {
        viewModelScope.launch {
            val list = exportsRepository.getExportedTracks()
            _uiState.update { it.copy(exportedTracks = list) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun selectSong(song: Song, autoOpenEditor: Boolean = true) {
        _uiState.update {
            it.copy(
                selectedSong = song,
                currentPositionMs = 0L,
                totalDurationMs = song.durationMs,
                currentTab = if (autoOpenEditor) NavigationTab.EDITOR else it.currentTab,
                saveSuccessMessage = null,
                errorMessage = null
            )
        }

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(song.uri)
            player.setMediaItem(mediaItem)
            player.playbackParameters = PlaybackParameters(_uiState.value.speed, _uiState.value.pitchFactor)
            player.repeatMode = if (_uiState.value.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.volume = _uiState.value.volume
            player.prepare()
            player.playWhenReady = true
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

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _uiState.update { it.copy(volume = clamped) }
        exoPlayer?.volume = clamped
    }

    // Real-time speed adjustment (0.50x to 2.00x)
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

    // Ecualizador de 3 bandas en tiempo real
    fun setBassGain(db: Float) {
        val clamped = db.coerceIn(-12f, 12f)
        val newCfg = _uiState.value.equalizerConfig.copy(bassGainDb = clamped)
        _uiState.update { it.copy(equalizerConfig = newCfg) }
        liveAudioEffects.applyConfig(newCfg)
    }

    fun setMidGain(db: Float) {
        val clamped = db.coerceIn(-12f, 12f)
        val newCfg = _uiState.value.equalizerConfig.copy(midGainDb = clamped)
        _uiState.update { it.copy(equalizerConfig = newCfg) }
        liveAudioEffects.applyConfig(newCfg)
    }

    fun setTrebleGain(db: Float) {
        val clamped = db.coerceIn(-12f, 12f)
        val newCfg = _uiState.value.equalizerConfig.copy(trebleGainDb = clamped)
        _uiState.update { it.copy(equalizerConfig = newCfg) }
        liveAudioEffects.applyConfig(newCfg)
    }

    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val newCfg = _uiState.value.equalizerConfig.copy(bassBoostPercent = clamped)
        _uiState.update { it.copy(equalizerConfig = newCfg) }
        liveAudioEffects.applyConfig(newCfg)
    }

    fun resetPitchAndSpeed() {
        _uiState.update { it.copy(speed = 1.0f, semitones = 0) }
        applyPlaybackParameters()
    }

    fun resetEqualizer() {
        val flat = EqualizerConfig()
        _uiState.update { it.copy(equalizerConfig = flat) }
        liveAudioEffects.applyConfig(flat)
    }

    fun resetAllEffects() {
        resetPitchAndSpeed()
        resetEqualizer()
    }

    private fun applyPlaybackParameters() {
        val currentSpeed = _uiState.value.speed
        val pitch = _uiState.value.pitchFactor
        exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, pitch)
    }

    fun onPickedAudioUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val song = songRepository.getSongFromPickedUri(uri)
            if (song != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        songs = listOf(song) + state.songs.filter { it.id != song.id }
                    )
                }
                selectSong(song, autoOpenEditor = true)
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
        val eqConfig = _uiState.value.equalizerConfig

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
                equalizerConfig = eqConfig,
                onProgress = { progress ->
                    _uiState.update { it.copy(saveProgress = progress) }
                }
            )

            result.onSuccess { savedSong ->
                loadExports()
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveProgress = 1.0f,
                        saveSuccessMessage = "¡Copia procesada y guardada como '${savedSong.title}'!\nUbicación: Carpeta Música / EditorDeCanciones. El original permanece intacto."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Error en el procesamiento de exportación: ${err.localizedMessage ?: "Formato no compatible"}"
                    )
                }
            }
        }
    }

    fun deleteExportedTrack(track: ExportedTrack) {
        viewModelScope.launch {
            val deleted = exportsRepository.deleteExportedTrack(track)
            if (deleted) {
                loadExports()
                _uiState.update { it.copy(infoMessage = "Archivo eliminado correctamente.") }
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar el archivo.") }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cache = getApplication<Application>().cacheDir
            var count = 0
            cache.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_export_") || file.name.endsWith(".tmp")) {
                    if (file.delete()) count++
                }
            }
            _uiState.update { it.copy(infoMessage = "Caché de audio optimizada ($count archivos temporales eliminados).") }
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
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
        liveAudioEffects.release()
        exoPlayer?.release()
        exoPlayer = null
    }
}
