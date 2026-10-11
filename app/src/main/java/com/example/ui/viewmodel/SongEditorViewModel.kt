package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioExporter
import com.example.audio.EqualizerConfig
import com.example.audio.MusicPlayerManager
import com.example.audio.PlayerRepeatMode
import com.example.audio.PlayerState
import com.example.data.model.ExportedTrack
import com.example.data.model.Song
import com.example.data.repository.ExportsRepository
import com.example.data.repository.SongRepository
import com.example.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    LIBRARY,
    PLAYER,
    EDITOR,
    EXPORTS,
    SETTINGS
}

enum class LibrarySortOrder {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    FAVORITES
}

data class SongEditorUiState(
    val currentTab: NavigationTab = NavigationTab.LIBRARY,
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val exportedTracks: List<ExportedTrack> = emptyList(),
    val sortOrder: LibrarySortOrder = LibrarySortOrder.TITLE,
    val searchQuery: String = "",
    val playerState: PlayerState = PlayerState(),
    val isSaving: Boolean = false,
    val saveProgress: Float = 0f,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val filteredSongs: List<Song>
        get() {
            var list = if (searchQuery.isBlank()) songs else {
                songs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true) ||
                            it.album.contains(searchQuery, ignoreCase = true)
                }
            }

            return when (sortOrder) {
                LibrarySortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
                LibrarySortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
                LibrarySortOrder.ALBUM -> list.sortedBy { it.album.lowercase() }
                LibrarySortOrder.DURATION -> list.sortedByDescending { it.durationMs }
                LibrarySortOrder.FAVORITES -> list.sortedWith(compareByDescending<Song> { it.isFavorite }.thenBy { it.title.lowercase() })
            }
        }

    val selectedSong: Song? get() = playerState.currentSong
    val isPlaying: Boolean get() = playerState.isPlaying
    val currentPositionMs: Long get() = playerState.currentPositionMs
    val totalDurationMs: Long get() = playerState.totalDurationMs
    val speed: Float get() = playerState.speed
    val semitones: Int get() = playerState.semitones
    val equalizerConfig: EqualizerConfig get() = playerState.equalizerConfig
}

class SongEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val songRepository = SongRepository(application)
    private val exportsRepository = ExportsRepository(application)
    val playerManager = MusicPlayerManager.getInstance(application)

    private val _uiState = MutableStateFlow(SongEditorUiState())
    val uiState: StateFlow<SongEditorUiState> = combine(
        _uiState,
        playerManager.playerState
    ) { currentUiState, currentPlayerState ->
        currentUiState.copy(playerState = currentPlayerState)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _uiState.value
    )

    init {
        startPlaybackService()
        loadExports()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(getApplication(), PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (_: Exception) {}
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
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val mediaStoreSongs = songRepository.getDeviceSongs()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    songs = mediaStoreSongs
                )
            }
            loadExports()
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

    fun toggleFavorite(song: Song) {
        val isFav = songRepository.toggleFavorite(song.id)
        _uiState.update { state ->
            val updated = state.songs.map {
                if (it.id == song.id) it.copy(isFavorite = isFav) else it
            }
            state.copy(songs = updated)
        }
    }

    // Acciones de reproducción y cola
    fun playSong(song: Song, autoOpenPlayer: Boolean = false) {
        val queue = _uiState.value.filteredSongs
        playerManager.playSong(song, queue)
        if (autoOpenPlayer) {
            navigateTo(NavigationTab.PLAYER)
        }
    }

    fun selectSong(song: Song, autoOpenEditor: Boolean = false, autoOpenPlayer: Boolean = false) {
        val queue = _uiState.value.filteredSongs
        playerManager.playSong(song, queue)
        if (autoOpenEditor) {
            navigateTo(NavigationTab.EDITOR)
        } else if (autoOpenPlayer) {
            navigateTo(NavigationTab.PLAYER)
        }
    }

    fun toggleLoop() {
        playerManager.toggleRepeatMode()
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun skipForward(seconds: Int = 5) {
        playerManager.skipForward(seconds)
    }

    fun skipBackward(seconds: Int = 5) {
        playerManager.skipBackward(seconds)
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
        _uiState.update { it.copy(infoMessage = "Añadida a la cola: ${song.title}") }
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        playerManager.reorderQueue(fromIndex, toIndex)
    }

    fun setSleepTimer(minutes: Int?) {
        playerManager.setSleepTimerMinutes(minutes)
        val msg = if (minutes != null) "Temporizador de apagado configurado para $minutes minutos" else "Temporizador de apagado cancelado"
        _uiState.update { it.copy(infoMessage = msg) }
    }

    // Efectos de audio en tiempo real
    fun setSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun setSemitones(semitones: Int) {
        playerManager.setSemitones(semitones)
    }

    fun incrementPitch() {
        playerManager.setSemitones(playerManager.playerState.value.semitones + 1)
    }

    fun decrementPitch() {
        playerManager.setSemitones(playerManager.playerState.value.semitones - 1)
    }

    fun setVolume(vol: Float) {
        playerManager.setVolume(vol)
    }

    fun setBassGain(db: Float) {
        val cfg = playerManager.playerState.value.equalizerConfig.copy(bassGainDb = db)
        playerManager.updateEqualizer(cfg)
    }

    fun setMidGain(db: Float) {
        val cfg = playerManager.playerState.value.equalizerConfig.copy(midGainDb = db)
        playerManager.updateEqualizer(cfg)
    }

    fun setTrebleGain(db: Float) {
        val cfg = playerManager.playerState.value.equalizerConfig.copy(trebleGainDb = db)
        playerManager.updateEqualizer(cfg)
    }

    fun setBassBoost(percent: Int) {
        val cfg = playerManager.playerState.value.equalizerConfig.copy(bassBoostPercent = percent)
        playerManager.updateEqualizer(cfg)
    }

    fun resetPitchAndSpeed() {
        playerManager.resetPitchAndSpeed()
    }

    fun resetEqualizer() {
        playerManager.updateEqualizer(EqualizerConfig())
    }

    fun resetAllEffects() {
        playerManager.resetAllEffects()
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
                playSong(song, autoOpenPlayer = true)
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

    // Exportación real sin sobrescribir original
    fun saveModifiedCopy() {
        val currentSong = playerManager.playerState.value.currentSong ?: return
        val currentSpeed = playerManager.playerState.value.speed
        val currentSemitones = playerManager.playerState.value.semitones
        val currentPitchFactor = playerManager.playerState.value.pitchFactor
        val eqConfig = playerManager.playerState.value.equalizerConfig

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
}
