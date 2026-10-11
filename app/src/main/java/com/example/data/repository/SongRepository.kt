package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SongRepository(private val context: Context) {

    suspend fun getDeviceSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
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

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("2000") // At least 2 seconds to ignore notification chirps
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

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
                    val title = if (rawTitle.isNotBlank()) rawTitle else displayName
                    val artist = it.getString(artistCol) ?: "<unknown>"
                    val cleanArtist = if (artist == "<unknown>" || artist.isBlank()) "Artista Desconocido" else artist
                    val album = it.getString(albumCol) ?: "Álbum Desconocido"
                    val duration = it.getLong(durationCol)
                    val albumId = it.getLong(albumIdCol)
                    val size = it.getLong(sizeCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val isEdit = title.contains("(Edit", ignoreCase = true) ||
                            displayName.contains("_edit_", ignoreCase = true) ||
                            album.contains("Editor de Canciones", ignoreCase = true)

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = cleanArtist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri,
                            albumArtUri = albumArtUri,
                            size = size,
                            isSavedEdit = isEdit
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
            var fileName = "Canción seleccionada"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                    if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                }
            }

            var title = fileName.substringBeforeLast(".")
            var artist = "Artista Desconocido"
            var album = "Audio importado"
            var durationMs = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                    if (it.isNotBlank()) title = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) artist = it
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

            Song(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uri = uri,
                albumArtUri = null,
                size = fileSize,
                isSavedEdit = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateSampleTracks(): List<Song> = withContext(Dispatchers.IO) {
        val samplesDir = File(context.cacheDir, "sample_audio").apply { if (!exists()) mkdirs() }

        val track1 = createSynthTrack(
            file = File(samplesDir, "cyber_pulse_128bpm.wav"),
            title = "Cyber Pulse",
            artist = "Studio Synth",
            bpm = 128,
            durationSeconds = 12,
            baseFreq = 110.0 // A2
        )

        val track2 = createSynthTrack(
            file = File(samplesDir, "neon_sunset_lofi.wav"),
            title = "Neon Sunset",
            artist = "Lo-Fi Beats",
            bpm = 88,
            durationSeconds = 14,
            baseFreq = 130.81 // C3
        )

        val track3 = createSynthTrack(
            file = File(samplesDir, "acoustic_horizon.wav"),
            title = "Acoustic Horizon",
            artist = "Acoustic Melody",
            bpm = 105,
            durationSeconds = 12,
            baseFreq = 146.83 // D3
        )

        listOf(track1, track2, track3)
    }

    private fun createSynthTrack(
        file: File,
        title: String,
        artist: String,
        bpm: Int,
        durationSeconds: Int,
        baseFreq: Double
    ): Song {
        if (!file.exists() || file.length() < 1000) {
            val sampleRate = 44100
            val totalSamples = sampleRate * durationSeconds
            val bytes = ByteArray(totalSamples * 2) // 16-bit mono
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val beatInterval = (60.0 / bpm) * sampleRate
            // Minor pentatonic intervals: 1, 1.2, 1.334, 1.5, 1.778
            val scaleRatios = doubleArrayOf(1.0, 1.189, 1.335, 1.498, 1.782, 2.0)

            for (i in 0 until totalSamples) {
                val beatIndex = ((i / beatInterval).toInt()) % scaleRatios.size
                val currentFreq = baseFreq * scaleRatios[beatIndex]

                val t = i.toDouble() / sampleRate
                val beatPos = (i % beatInterval.toInt()).toDouble() / beatInterval
                // Envelope decay for plucked synth sound
                val env = (1.0 - beatPos * 0.85).coerceIn(0.0, 1.0)

                // Add fundamental + overtone + soft chorus
                val wave1 = sin(2.0 * PI * currentFreq * t)
                val wave2 = sin(2.0 * PI * (currentFreq * 2.01) * t) * 0.4
                val wave3 = sin(2.0 * PI * (currentFreq * 0.5) * t) * 0.3

                // Subtle drum click on downbeats
                val click = if (beatPos < 0.05) sin(2.0 * PI * 60.0 * (1.0 - beatPos * 20.0)) * 0.5 else 0.0

                val sampleVal = ((wave1 + wave2 + wave3) * env * 0.7 + click) * 16000.0
                val clamped = sampleVal.toInt().coerceIn(-32767, 32767).toShort()
                buffer.putShort(clamped)
            }

            FileOutputStream(file).use { fos ->
                writeWav(fos, sampleRate, 1, bytes)
            }
        }

        return Song(
            id = file.name.hashCode().toLong(),
            title = title,
            artist = artist,
            album = "Pistas Demo",
            durationMs = (durationSeconds * 1000).toLong(),
            uri = Uri.fromFile(file),
            albumArtUri = null,
            size = file.length(),
            isSavedEdit = false,
            isSample = true
        )
    }

    private fun writeWav(fos: FileOutputStream, sampleRate: Int, channels: Int, pcmData: ByteArray) {
        val totalDataLen = pcmData.size + 36
        val byteRate = (sampleRate * channels * 16) / 8
        val blockAlign = (channels * 16) / 8

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16)
        bb.putShort(1.toShort())
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign.toShort())
        bb.putShort(16.toShort())
        bb.put("data".toByteArray())
        bb.putInt(pcmData.size)

        fos.write(header)
        fos.write(pcmData)
        fos.flush()
    }
}
