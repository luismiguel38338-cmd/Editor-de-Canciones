package com.example.data.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedTrack(
    val id: Long,
    val title: String,
    val fileName: String,
    val uri: Uri,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedMs: Long,
    val speed: Float = 1.0f,
    val semitones: Int = 0
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "-- MB"
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.2f MB", mb)
        }

    val formattedDate: String
        get() {
            if (dateAddedMs <= 0) return ""
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(dateAddedMs))
        }

    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = "Editor de Canciones",
            album = "Mis Exportaciones",
            durationMs = durationMs,
            uri = uri,
            albumArtUri = null,
            size = sizeBytes,
            isSavedEdit = true
        )
    }
}
