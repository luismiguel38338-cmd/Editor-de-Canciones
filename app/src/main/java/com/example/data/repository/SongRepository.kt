package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.Song
import com.example.ui.theme.AccentColorChoice
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("melotune_prefs", Context.MODE_PRIVATE)

    suspend fun getDeviceSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val favoriteIds = getFavoriteSongIds()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Filtrar audios de al menos 3 segundos para ignorar tonos de notificación diminutos
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("3000")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val rawTitle = it.getString(titleCol) ?: ""
                    val displayName = it.getString(nameCol) ?: ""
                    val origTitle = if (rawTitle.isNotBlank()) rawTitle else displayName
                    val rawArtist = it.getString(artistCol) ?: "<unknown>"
                    val origArtist = if (rawArtist == "<unknown>" || rawArtist.isBlank()) "Artista Desconocido" else rawArtist
                    val album = it.getString(albumCol) ?: "Álbum Desconocido"
                    val duration = it.getLong(durationCol)
                    val albumId = it.getLong(albumIdCol)
                    val size = it.getLong(sizeCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val isEdit = origTitle.contains("(Edit", ignoreCase = true) ||
                            displayName.contains("_edit_", ignoreCase = true) ||
                            album.contains("MeloTune", ignoreCase = true) ||
                            album.contains("Editor de Canciones", ignoreCase = true)

                    // Metadatos personalizados guardados en preferencias
                    val customTitle = prefs.getString("custom_title_$id", null)
                    val customArtist = prefs.getString("custom_artist_$id", null)
                    val hasCustom = customTitle != null || customArtist != null

                    val displayTitle = customTitle ?: origTitle
                    val displayArtist = customArtist ?: origArtist

                    songsList.add(
                        Song(
                            id = id,
                            title = displayTitle,
                            artist = displayArtist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri,
                            albumArtUri = albumArtUri,
                            size = size,
                            isSavedEdit = isEdit,
                            isFavorite = favoriteIds.contains(id.toString()),
                            originalTitle = origTitle,
                            originalArtist = origArtist,
                            hasCustomMetadata = hasCustom
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songsList
    }

    suspend fun getSongFromPickedUri(uri: Uri): Song? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Archivo de audio"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }

            var origTitle = fileName.substringBeforeLast(".")
            var origArtist = "Artista Desconocido"
            var album = "Audio importado"
            var durationMs = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                    if (it.isNotBlank()) origTitle = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) origArtist = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                    if (it.isNotBlank()) album = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                    durationMs = it.toLongOrNull() ?: 0L
                }
            } catch (_: Exception) {} finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            val id = System.currentTimeMillis()
            val customTitle = prefs.getString("custom_title_$id", null)
            val customArtist = prefs.getString("custom_artist_$id", null)

            Song(
                id = id,
                title = customTitle ?: origTitle,
                artist = customArtist ?: origArtist,
                album = album,
                durationMs = durationMs,
                uri = uri,
                albumArtUri = null,
                size = fileSize,
                isSavedEdit = false,
                isFavorite = false,
                originalTitle = origTitle,
                originalArtist = origArtist,
                hasCustomMetadata = customTitle != null || customArtist != null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFavoriteSongIds(): Set<String> {
        return prefs.getStringSet("favorite_song_ids", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(songId: Long): Boolean {
        val current = getFavoriteSongIds().toMutableSet()
        val idStr = songId.toString()
        val isNowFavorite = if (current.contains(idStr)) {
            current.remove(idStr)
            false
        } else {
            current.add(idStr)
            true
        }
        prefs.edit().putStringSet("favorite_song_ids", current).apply()
        return isNowFavorite
    }

    // Personalización de Metadatos (sin tocar el archivo original)
    fun saveCustomMetadata(songId: Long, customTitle: String?, customArtist: String?) {
        val editor = prefs.edit()
        if (customTitle.isNullOrBlank()) {
            editor.remove("custom_title_$songId")
        } else {
            editor.putString("custom_title_$songId", customTitle.trim())
        }
        if (customArtist.isNullOrBlank()) {
            editor.remove("custom_artist_$songId")
        } else {
            editor.putString("custom_artist_$songId", customArtist.trim())
        }
        editor.apply()
    }

    fun resetCustomMetadata(songId: Long) {
        prefs.edit()
            .remove("custom_title_$songId")
            .remove("custom_artist_$songId")
            .apply()
    }

    // Preferencias de Apariencia
    fun getThemeMode(): AppThemeMode {
        val saved = prefs.getString("app_theme_mode", AppThemeMode.DARK.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun getAccentColor(): AccentColorChoice {
        val saved = prefs.getString("app_accent_color", AccentColorChoice.TURQUOISE.name)
        return try {
            AccentColorChoice.valueOf(saved ?: AccentColorChoice.TURQUOISE.name)
        } catch (_: Exception) {
            AccentColorChoice.TURQUOISE
        }
    }

    fun setAccentColor(choice: AccentColorChoice) {
        prefs.edit().putString("app_accent_color", choice.name).apply()
    }
}
