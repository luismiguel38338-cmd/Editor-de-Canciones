package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.audio.MusicPlayerManager

class PlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "melotune_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val playerManager = MusicPlayerManager.getInstance(this)
        val session = playerManager.getOrCreateMediaSession()
        if (session != null) {
            addSession(session)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return MusicPlayerManager.getInstance(this).getOrCreateMediaSession()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = MusicPlayerManager.getInstance(this).exoPlayer
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val playerManager = MusicPlayerManager.getInstance(this)
        playerManager.mediaSession?.let { session ->
            removeSession(session)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción MeloTune",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles multimedia de reproducción"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
