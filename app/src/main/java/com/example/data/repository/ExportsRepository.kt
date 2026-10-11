package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.ExportedTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExportsRepository(private val context: Context) {

    suspend fun getExportedTracks(): List<ExportedTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ExportedTrack>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} = ?"
        val selectionArgs = arrayOf("%_edit_%", "Ediciones Guardadas")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: ""
                    val fileName = cursor.getString(nameCol) ?: ""
                    val title = if (rawTitle.isNotBlank()) rawTitle else fileName.substringBeforeLast(".")
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol) * 1000L

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    tracks.add(
                        ExportedTrack(
                            id = id,
                            title = title,
                            fileName = fileName,
                            uri = uri,
                            sizeBytes = size,
                            durationMs = duration,
                            dateAddedMs = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // También revisar la carpeta directa de la app si está en Android 9 o archivos en cache
        try {
            val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "EditorDeCanciones")
            if (musicDir.exists() && musicDir.isDirectory) {
                musicDir.listFiles { file -> file.extension.equals("wav", ignoreCase = true) }?.forEach { f ->
                    if (tracks.none { it.fileName == f.name }) {
                        tracks.add(
                            ExportedTrack(
                                id = f.name.hashCode().toLong(),
                                title = f.nameWithoutExtension,
                                fileName = f.name,
                                uri = android.net.Uri.fromFile(f),
                                sizeBytes = f.length(),
                                durationMs = 0L,
                                dateAddedMs = f.lastModified()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        tracks
    }

    suspend fun deleteExportedTrack(track: ExportedTrack): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(track.uri, null, null)
            if (rows > 0) return@withContext true

            // Intento directo por archivo si es URI de archivo
            if (track.uri.scheme == "file") {
                track.uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) return@withContext file.delete()
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
