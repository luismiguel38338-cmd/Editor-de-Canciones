package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

object AudioExporter {

    suspend fun exportModifiedSong(
        context: Context,
        song: Song,
        speed: Float,
        semitones: Int,
        pitchFactor: Float,
        equalizerConfig: EqualizerConfig = EqualizerConfig(),
        onProgress: (Float) -> Unit
    ): Result<Song> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        val tempWavFile = File(context.cacheDir, "temp_export_${System.currentTimeMillis()}.wav")

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, song.uri, null)

            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                return@withContext Result.failure(IllegalStateException("No se encontró pista de audio compatible."))
            }

            extractor.selectTrack(audioTrackIndex)

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()

            val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                song.durationMs * 1000L
            }

            var sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            var channelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val sonic = SonicAudioProcessor().apply {
                setSpeed(speed)
                setPitch(pitchFactor)
            }

            var sonicInputFormat = AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT)
            var sonicOutputFormat = sonic.configure(sonicInputFormat)
            sonic.flush()

            val pcmEqualizer = PcmEqualizer(sonicOutputFormat.sampleRate).apply {
                updateConfig(sonicOutputFormat.sampleRate, equalizerConfig)
            }

            val fos = FileOutputStream(tempWavFile)
            // Write placeholder 44-byte WAV header
            writeWavHeader(fos, sonicOutputFormat.sampleRate, sonicOutputFormat.channelCount, 0)

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false
            var totalPcmBytesWritten = 0L

            val timeoutUs = 10_000L

            while (!isOutputEOS) {
                // 1. Feed input buffers to decoder
                if (!isInputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex) ?: continue
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEOS = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            decoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                sampleTime,
                                0
                            )
                            extractor.advance()

                            if (durationUs > 0) {
                                val progress = (sampleTime.toFloat() / durationUs.toFloat()).coerceIn(0f, 0.85f)
                                onProgress(progress)
                            }
                        }
                    }
                }

                // 2. Read decoded PCM buffers from decoder
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    outputBufferIndex >= 0 -> {
                        val outBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            sonic.queueInput(outBuffer)
                            while (true) {
                                val processedBuffer = sonic.output
                                if (processedBuffer.hasRemaining()) {
                                    val bytes = ByteArray(processedBuffer.remaining())
                                    processedBuffer.get(bytes)
                                    pcmEqualizer.processPcm16(bytes, sonicOutputFormat.channelCount)
                                    fos.write(bytes)
                                    totalPcmBytesWritten += bytes.size
                                } else {
                                    break
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputEOS = true
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = decoder.outputFormat
                        sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                        sonicInputFormat = AudioProcessor.AudioFormat(sampleRate, channelCount, C.ENCODING_PCM_16BIT)
                        sonicOutputFormat = sonic.configure(sonicInputFormat)
                        sonic.flush()
                        pcmEqualizer.updateConfig(sonicOutputFormat.sampleRate, equalizerConfig)
                    }
                }
            }

            // Flush remaining data in Sonic
            sonic.queueEndOfStream()
            while (!sonic.isEnded) {
                val processedBuffer = sonic.output
                if (processedBuffer.hasRemaining()) {
                    val bytes = ByteArray(processedBuffer.remaining())
                    processedBuffer.get(bytes)
                    pcmEqualizer.processPcm16(bytes, sonicOutputFormat.channelCount)
                    fos.write(bytes)
                    totalPcmBytesWritten += bytes.size
                } else {
                    break
                }
            }

            fos.flush()
            fos.close()

            // Update WAV header with actual data size
            updateWavHeader(tempWavFile, sonicOutputFormat.sampleRate, sonicOutputFormat.channelCount, totalPcmBytesWritten)
            onProgress(0.95f)

            // Save to MediaStore (Music directory)
            val cleanTitle = song.title.replace("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]".toRegex(), "").trim()
            val speedStr = String.format(Locale.US, "%.2fx", speed)
            val pitchStr = if (semitones >= 0) "+${semitones}st" else "${semitones}st"
            val fileName = "${cleanTitle}_edit_${speedStr}_${pitchStr}_${System.currentTimeMillis() % 10000}.wav"

            val savedUri = saveWavToMediaStore(context, tempWavFile, fileName, cleanTitle, speedStr, pitchStr)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo registrar la copia en la biblioteca de audio."))

            val newDurationMs = if (speed > 0) (song.durationMs / speed).toLong() else song.durationMs

            val savedSong = Song(
                id = System.currentTimeMillis(),
                title = "$cleanTitle (Edit $speedStr $pitchStr)",
                artist = song.artist,
                album = "Editor de Canciones",
                durationMs = newDurationMs,
                uri = savedUri,
                albumArtUri = song.albumArtUri,
                size = totalPcmBytesWritten + 44,
                isSavedEdit = true
            )

            onProgress(1.0f)
            Result.success(savedSong)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
            try {
                if (tempWavFile.exists()) tempWavFile.delete()
            } catch (_: Exception) {}
        }
    }

    private fun writeWavHeader(out: FileOutputStream, sampleRate: Int, channels: Int, dataLength: Long) {
        val totalDataLen = dataLength + 36
        val byteRate = (sampleRate * channels * 16) / 8
        val blockAlign = (channels * 16) / 8

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen.toInt())
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(1.toShort()) // AudioFormat 1 = PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign.toShort())
        bb.putShort(16.toShort()) // Bits per sample
        bb.put("data".toByteArray())
        bb.putInt(dataLength.toInt())

        out.write(header)
    }

    private fun updateWavHeader(file: File, sampleRate: Int, channels: Int, dataLength: Long) {
        val raf = RandomAccessFile(file, "rw")
        val totalDataLen = dataLength + 36
        val byteRate = (sampleRate * channels * 16) / 8
        val blockAlign = (channels * 16) / 8

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen.toInt())
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
        bb.putInt(dataLength.toInt())

        raf.seek(0)
        raf.write(header)
        raf.close()
    }

    private fun saveWavToMediaStore(
        context: Context,
        tempFile: File,
        displayName: String,
        originalTitle: String,
        speedStr: String,
        pitchStr: String
    ): Uri? {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.TITLE, "$originalTitle (Edit $speedStr $pitchStr)")
                put(MediaStore.Audio.Media.ARTIST, "Editor de Canciones")
                put(MediaStore.Audio.Media.ALBUM, "Ediciones Guardadas")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/EditorDeCanciones")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return null

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            @Suppress("DEPRECATION")
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val appDir = File(musicDir, "EditorDeCanciones")
            if (!appDir.exists()) appDir.mkdirs()
            val destFile = File(appDir, displayName)
            tempFile.copyTo(destFile, overwrite = true)

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
                put(MediaStore.Audio.Media.TITLE, "$originalTitle (Edit $speedStr $pitchStr)")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            }
            return resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: Uri.fromFile(destFile)
        }
    }
}
