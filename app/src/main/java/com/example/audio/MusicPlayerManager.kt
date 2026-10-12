package com.example.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

enum class PlayerRepeatMode {
    OFF,
    ONE,
    ALL
}

data class PlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val semitones: Int = 0,
    val equalizerConfig: EqualizerConfig = EqualizerConfig(),
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    val isShuffle: Boolean = false,
    val sleepTimerRemainingSeconds: Int? = null,
    val errorMessage: String? = null
) {
    val pitchFactor: Float
        get() = (2.0).pow(semitones / 12.0).toFloat()

    val hasNext: Boolean
        get() = queue.isNotEmpty() && (repeatMode != PlayerRepeatMode.OFF || currentIndex < queue.size - 1)

    val hasPrevious: Boolean
        get() = queue.isNotEmpty() && (currentIndex > 0 || currentPositionMs > 3000)
}

class MusicPlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    val exoPlayer: ExoPlayer
    var mediaSession: MediaSession? = null
        private set

    val liveAudioEffects = LiveAudioEffectsManager()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        // Configuración de AudioAttributes para Audio Focus e interrupciones del sistema
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true) // Gestiona Audio Focus automáticamente
            .setHandleAudioBecomingNoisy(true)        // Pausa al desconectar auriculares
            .build()

        setupMediaSession()
        setupPlayerListeners()
    }

    private fun setupMediaSession() {
        try {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaSession.Builder(context, exoPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                            )
                        }
                        liveAudioEffects.attachToSession(exoPlayer.audioSessionId)
                        liveAudioEffects.applyConfig(_playerState.value.equalizerConfig)
                    }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
                        handleTrackCompletion()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playerState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Error de reproducción: ${error.localizedMessage ?: "Archivo no reproducible"}"
                    )
                }
            }
        })
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        val queue = newQueue ?: if (_playerState.value.queue.contains(song)) _playerState.value.queue else listOf(song)
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _playerState.update {
            it.copy(
                queue = queue,
                currentIndex = index,
                currentSong = song,
                currentPositionMs = 0L,
                totalDurationMs = song.durationMs,
                errorMessage = null
            )
        }

        loadAndPlayCurrentIndex()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val song = songs[clampedIndex]

        _playerState.update {
            it.copy(
                queue = songs,
                currentIndex = clampedIndex,
                currentSong = song,
                currentPositionMs = 0L,
                totalDurationMs = song.durationMs,
                errorMessage = null
            )
        }

        loadAndPlayCurrentIndex()
    }

    fun getOrCreateMediaSession(): MediaSession? {
        if (mediaSession == null) {
            setupMediaSession()
        }
        return mediaSession
    }

    private fun loadAndPlayCurrentIndex() {
        val state = _playerState.value
        val song = state.currentSong ?: return

        try {
            val serviceIntent = Intent(context, com.example.service.PlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (_: Exception) {}

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(song.toMediaItem())

        // Configurar repetición
        exoPlayer.repeatMode = if (state.repeatMode == PlayerRepeatMode.ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }

        applyEffects()
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    fun skipToNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if (state.repeatMode == PlayerRepeatMode.ONE) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
            return
        }

        val nextIndex = if (state.isShuffle) {
            (0 until state.queue.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
        } else {
            if (state.currentIndex + 1 < state.queue.size) {
                state.currentIndex + 1
            } else if (state.repeatMode == PlayerRepeatMode.ALL) {
                0
            } else {
                return // Fin de cola
            }
        }

        val nextSong = state.queue.getOrNull(nextIndex) ?: return
        _playerState.update {
            it.copy(
                currentIndex = nextIndex,
                currentSong = nextSong,
                currentPositionMs = 0L,
                totalDurationMs = nextSong.durationMs
            )
        }
        loadAndPlayCurrentIndex()
    }

    fun skipToPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        // Si ya han pasado más de 3 segundos, reiniciar la pista actual
        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (state.isShuffle) {
            (0 until state.queue.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
        } else {
            if (state.currentIndex - 1 >= 0) {
                state.currentIndex - 1
            } else if (state.repeatMode == PlayerRepeatMode.ALL) {
                state.queue.size - 1
            } else {
                seekTo(0)
                return
            }
        }

        val prevSong = state.queue.getOrNull(prevIndex) ?: return
        _playerState.update {
            it.copy(
                currentIndex = prevIndex,
                currentSong = prevSong,
                currentPositionMs = 0L,
                totalDurationMs = prevSong.durationMs
            )
        }
        loadAndPlayCurrentIndex()
    }

    private fun handleTrackCompletion() {
        val state = _playerState.value
        if (state.repeatMode == PlayerRepeatMode.ONE) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
        } else if (state.hasNext) {
            skipToNext()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipForward(seconds: Int = 5) {
        val target = (exoPlayer.currentPosition + seconds * 1000).coerceAtMost(exoPlayer.duration)
        seekTo(target)
    }

    fun skipBackward(seconds: Int = 5) {
        val target = (exoPlayer.currentPosition - seconds * 1000).coerceAtLeast(0)
        seekTo(target)
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            PlayerRepeatMode.OFF -> PlayerRepeatMode.ALL
            PlayerRepeatMode.ALL -> PlayerRepeatMode.ONE
            PlayerRepeatMode.ONE -> PlayerRepeatMode.OFF
        }
        _playerState.update { it.copy(repeatMode = nextMode) }
        exoPlayer.repeatMode = if (nextMode == PlayerRepeatMode.ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    // Gestión de cola
    fun addToQueue(song: Song) {
        _playerState.update { state ->
            val updated = state.queue + song
            state.copy(queue = updated)
        }
    }

    fun playNext(song: Song) {
        _playerState.update { state ->
            val insertIndex = (state.currentIndex + 1).coerceAtMost(state.queue.size)
            val updated = state.queue.toMutableList().apply { add(insertIndex, song) }
            state.copy(queue = updated)
        }
    }

    fun updateSongMetadata(updatedSong: Song) {
        _playerState.update { state ->
            val updatedQueue = state.queue.map { if (it.id == updatedSong.id) updatedSong else it }
            val newCurrent = if (state.currentSong?.id == updatedSong.id) updatedSong else state.currentSong
            state.copy(queue = updatedQueue, currentSong = newCurrent)
        }
    }

    fun removeFromQueue(index: Int) {
        _playerState.update { state ->
            if (index !in state.queue.indices) return@update state
            val updated = state.queue.toMutableList().apply { removeAt(index) }
            val newIndex = when {
                index < state.currentIndex -> state.currentIndex - 1
                index == state.currentIndex -> state.currentIndex.coerceAtMost(updated.size - 1)
                else -> state.currentIndex
            }
            state.copy(
                queue = updated,
                currentIndex = newIndex,
                currentSong = updated.getOrNull(newIndex)
            )
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        _playerState.update { state ->
            if (fromIndex !in state.queue.indices || toIndex !in state.queue.indices) return@update state
            val list = state.queue.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val currentId = state.currentSong?.id
            val newCurIndex = list.indexOfFirst { it.id == currentId }
            state.copy(queue = list, currentIndex = newCurIndex)
        }
    }

    // Efectos de audio en tiempo real
    fun setSpeed(speed: Float) {
        val clamped = (Math.round(speed * 20.0) / 20.0).toFloat().coerceIn(0.5f, 2.0f)
        _playerState.update { it.copy(speed = clamped) }
        applyEffects()
    }

    fun setSemitones(semitones: Int) {
        val clamped = semitones.coerceIn(-12, 12)
        _playerState.update { it.copy(semitones = clamped) }
        applyEffects()
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _playerState.update { it.copy(volume = clamped) }
        exoPlayer.volume = clamped
    }

    fun updateEqualizer(config: EqualizerConfig) {
        _playerState.update { it.copy(equalizerConfig = config) }
        liveAudioEffects.applyConfig(config)
    }

    fun resetPitchAndSpeed() {
        _playerState.update { it.copy(speed = 1.0f, semitones = 0) }
        applyEffects()
    }

    fun resetAllEffects() {
        resetPitchAndSpeed()
        updateEqualizer(EqualizerConfig())
    }

    private fun applyEffects() {
        val currentSpeed = _playerState.value.speed
        val pitch = _playerState.value.pitchFactor
        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed, pitch)
    }

    // Temporizador de apagado (Sleep Timer)
    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playerState.update { it.copy(sleepTimerRemainingSeconds = null) }
            return
        }

        var remainingSeconds = minutes * 60
        _playerState.update { it.copy(sleepTimerRemainingSeconds = remainingSeconds) }

        sleepTimerJob = scope.launch {
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                _playerState.update { it.copy(sleepTimerRemainingSeconds = remainingSeconds) }
            }
            // Temporizador expirado: pausar reproducción
            exoPlayer.pause()
            _playerState.update { it.copy(sleepTimerRemainingSeconds = null) }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playerState.update {
                        it.copy(
                            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun clearErrorMessage() {
        _playerState.update { it.copy(errorMessage = null) }
    }

    fun releaseSession() {
        try {
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
