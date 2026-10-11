package com.example.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val size: Long = 0L,
    val isSavedEdit: Boolean = false,
    val isSample: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, remainingSeconds)
        }
}
