package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.EqualizerConfig
import com.example.audio.MusicPlayerManager
import com.example.audio.PlayerRepeatMode
import com.example.audio.PlayerState
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.service.PlaybackService
import com.example.ui.theme.AccentColorChoice
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ControlSizeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    LIBRARY,
    PLAYER,
    EQUALIZER,
    DISCOVER,
    SETTINGS
}

enum class LibrarySortOrder {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    FAVORITES
}

enum class LibraryFilter {
    ALL,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    FAVORITES,
    PLAYLISTS
}

data class SongEditorUiState(
    val currentTab: NavigationTab = NavigationTab.LIBRARY,
    val permissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val playlists: Map<String, List<Long>> = emptyMap(),
    val selectedCategory: String? = null,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.TITLE,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val searchQuery: String = "",
    val playerState: PlayerState = PlayerState(),
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColorChoice: AccentColorChoice = AccentColorChoice.TURQUOISE,
    val controlSizeChoice: ControlSizeChoice = ControlSizeChoice.NORMAL,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val filteredSongs: List<Song>
        get() {
            var list = when (libraryFilter) {
                LibraryFilter.ALL -> songs
                LibraryFilter.FAVORITES -> songs.filter { it.isFavorite }
                LibraryFilter.ALBUMS -> {
                    if (selectedCategory != null) songs.filter { it.album.equals(selectedCategory, ignoreCase = true) }
                    else songs
                }
                LibraryFilter.ARTISTS -> {
                    if (selectedCategory != null) songs.filter { it.artist.equals(selectedCategory, ignoreCase = true) }
                    else songs
                }
                LibraryFilter.FOLDERS -> {
                    if (selectedCategory != null) songs.filter { it.folderName.equals(selectedCategory, ignoreCase = true) }
                    else songs
                }
                LibraryFilter.PLAYLISTS -> {
                    if (selectedCategory != null) {
                        val ids = playlists[selectedCategory] ?: emptyList()
                        songs.filter { ids.contains(it.id) }
                    } else songs
                }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim()
                list = list.filter {
                    it.title.contains(q, ignoreCase = true) ||
                            it.artist.contains(q, ignoreCase = true) ||
                            it.album.contains(q, ignoreCase = true) ||
                            it.folderName.contains(q, ignoreCase = true)
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
    val equalizerConfig: EqualizerConfig get() = playerState.equalizerConfig
}

class SongEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val songRepository = SongRepository(application)
    val playerManager = MusicPlayerManager.getInstance(application)

    private val _uiState = MutableStateFlow(
        SongEditorUiState(
            themeMode = songRepository.getThemeMode(),
            accentColorChoice = songRepository.getAccentColor(),
            controlSizeChoice = songRepository.getControlSize(),
            playlists = loadPlaylistsMap()
        )
    )

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

    private fun loadPlaylistsMap(): Map<String, List<Long>> {
        val names = songRepository.getPlaylistNames()
        val map = mutableMapOf<String, List<Long>>()
        for (name in names) {
            map[name] = songRepository.getPlaylistSongIds(name)
        }
        return map
    }

    private fun refreshPlaylists() {
        _uiState.update { it.copy(playlists = loadPlaylistsMap()) }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(getApplication(), PlaybackService::class.java)
            getApplication<Application>().startService(intent)
        } catch (_: Exception) {}
    }

    fun navigateTo(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
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
                    songs = mediaStoreSongs,
                    playlists = loadPlaylistsMap()
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(libraryFilter = filter, selectedCategory = null) }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // Listas de Reproducción
    fun createPlaylist(name: String) {
        val success = songRepository.createPlaylist(name)
        if (success) {
            refreshPlaylists()
            _uiState.update { it.copy(infoMessage = "Lista de reproducción '$name' creada.") }
        } else {
            _uiState.update { it.copy(errorMessage = "Ya existe una lista con ese nombre o el nombre no es válido.") }
        }
    }

    fun deletePlaylist(name: String) {
        val success = songRepository.deletePlaylist(name)
        if (success) {
            refreshPlaylists()
            _uiState.update {
                it.copy(
                    selectedCategory = if (it.selectedCategory == name) null else it.selectedCategory,
                    infoMessage = "Lista '$name' eliminada."
                )
            }
        }
    }

    fun addSongToPlaylist(playlistName: String, song: Song) {
        val added = songRepository.addSongToPlaylist(playlistName, song.id)
        if (added) {
            refreshPlaylists()
            _uiState.update { it.copy(infoMessage = "'${song.title}' añadida a '$playlistName'.") }
        } else {
            _uiState.update { it.copy(infoMessage = "La canción ya está en la lista '$playlistName'.") }
        }
    }

    fun removeSongFromPlaylist(playlistName: String, songId: Long) {
        val removed = songRepository.removeSongFromPlaylist(playlistName, songId)
        if (removed) {
            refreshPlaylists()
            _uiState.update { it.copy(infoMessage = "Canción retirada de '$playlistName'.") }
        }
    }

    // Preferencias de personalización
    fun setThemeMode(mode: AppThemeMode) {
        songRepository.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAccentColor(choice: AccentColorChoice) {
        songRepository.setAccentColor(choice)
        _uiState.update { it.copy(accentColorChoice = choice) }
    }

    fun setControlSize(choice: ControlSizeChoice) {
        songRepository.setControlSize(choice)
        _uiState.update { it.copy(controlSizeChoice = choice) }
    }

    fun applyEqualizerPreset(bass: Float, mid: Float, treble: Float, boost: Int) {
        val cfg = EqualizerConfig(
            bassGainDb = bass,
            midGainDb = mid,
            trebleGainDb = treble,
            bassBoostPercent = boost
        )
        playerManager.updateEqualizer(cfg)
    }

    fun saveCustomMetadata(songId: Long, customTitle: String?, customArtist: String?) {
        songRepository.saveCustomMetadata(songId, customTitle, customArtist)
        _uiState.update { state ->
            val updatedSongs = state.songs.map { song ->
                if (song.id == songId) {
                    val finalTitle = if (!customTitle.isNullOrBlank()) customTitle.trim() else song.originalTitle
                    val finalArtist = if (!customArtist.isNullOrBlank()) customArtist.trim() else song.originalArtist
                    val hasCustom = !customTitle.isNullOrBlank() || !customArtist.isNullOrBlank()
                    val updated = song.copy(
                        title = finalTitle,
                        artist = finalArtist,
                        hasCustomMetadata = hasCustom
                    )
                    playerManager.updateSongMetadata(updated)
                    updated
                } else song
            }
            state.copy(
                songs = updatedSongs,
                infoMessage = "Metadatos personalizados guardados. El archivo de audio original permanece intacto."
            )
        }
    }

    fun resetCustomMetadata(songId: Long) {
        songRepository.resetCustomMetadata(songId)
        _uiState.update { state ->
            val updatedSongs = state.songs.map { song ->
                if (song.id == songId) {
                    val updated = song.copy(
                        title = song.originalTitle,
                        artist = song.originalArtist,
                        hasCustomMetadata = false
                    )
                    playerManager.updateSongMetadata(updated)
                    updated
                } else song
            }
            state.copy(
                songs = updatedSongs,
                infoMessage = "Metadatos restaurados a los originales del archivo."
            )
        }
    }

    fun onPickedAudioUri(uri: Uri) {
        viewModelScope.launch {
            val song = songRepository.getSongFromPickedUri(uri)
            if (song != null) {
                val exists = _uiState.value.songs.any { it.uri == uri }
                if (!exists) {
                    _uiState.update { it.copy(songs = listOf(song) + it.songs) }
                }
                playSong(song, autoOpenPlayer = true)
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo leer el archivo de audio seleccionado.") }
            }
        }
    }

    // Funciones del Reproductor de Audio
    fun playSong(song: Song, autoOpenPlayer: Boolean = false) {
        startPlaybackService()
        val queue = _uiState.value.filteredSongs
        val safeQueue = if (queue.contains(song)) queue else listOf(song) + queue
        playerManager.playSong(song, safeQueue)
        if (autoOpenPlayer) {
            _uiState.update { it.copy(currentTab = NavigationTab.PLAYER) }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, autoOpenPlayer: Boolean = false) {
        startPlaybackService()
        playerManager.playQueue(songs, startIndex)
        if (autoOpenPlayer) {
            _uiState.update { it.copy(currentTab = NavigationTab.PLAYER) }
        }
    }

    fun togglePlayPause() {
        startPlaybackService()
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

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
        _uiState.update { it.copy(infoMessage = "'${song.title}' añadida a la cola.") }
    }

    fun playNext(song: Song) {
        playerManager.playNext(song)
        _uiState.update { it.copy(infoMessage = "'${song.title}' sonará a continuación.") }
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun setSleepTimer(minutes: Int?) {
        playerManager.setSleepTimerMinutes(minutes)
        if (minutes != null) {
            _uiState.update { it.copy(infoMessage = "Temporizador configurado para $minutes minutos.") }
        } else {
            _uiState.update { it.copy(infoMessage = "Temporizador de apagado desactivado.") }
        }
    }

    fun toggleFavorite(song: Song) {
        val isFav = songRepository.toggleFavorite(song.id)
        _uiState.update { state ->
            val updated = state.songs.map {
                if (it.id == song.id) it.copy(isFavorite = isFav) else it
            }
            state.copy(songs = updated)
        }
        val current = playerManager.playerState.value.currentSong
        if (current?.id == song.id) {
            playerManager.updateSongMetadata(current.copy(isFavorite = isFav))
        }
    }

    // Funciones de Ecualizador
    fun setBassGain(db: Float) {
        val current = playerManager.playerState.value.equalizerConfig
        playerManager.updateEqualizer(current.copy(bassGainDb = db))
    }

    fun setMidGain(db: Float) {
        val current = playerManager.playerState.value.equalizerConfig
        playerManager.updateEqualizer(current.copy(midGainDb = db))
    }

    fun setTrebleGain(db: Float) {
        val current = playerManager.playerState.value.equalizerConfig
        playerManager.updateEqualizer(current.copy(trebleGainDb = db))
    }

    fun setBassBoost(pct: Int) {
        val current = playerManager.playerState.value.equalizerConfig
        playerManager.updateEqualizer(current.copy(bassBoostPercent = pct))
    }

    fun resetEqualizer() {
        playerManager.updateEqualizer(EqualizerConfig())
    }

    fun clearCache() {
        viewModelScope.launch {
            val cache = getApplication<Application>().cacheDir
            var count = 0
            cache.listFiles()?.forEach { file ->
                if (file.name.endsWith(".tmp") || file.name.startsWith("cache_")) {
                    if (file.delete()) count++
                }
            }
            _uiState.update { it.copy(infoMessage = "Caché de audio optimizada ($count archivos temporales eliminados).") }
        }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
