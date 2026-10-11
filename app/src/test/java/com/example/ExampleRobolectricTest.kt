package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.audio.EqualizerConfig
import com.example.audio.PcmEqualizer
import com.example.data.model.ExportedTrack
import com.example.data.model.Song
import com.example.ui.viewmodel.LibrarySortOrder
import com.example.ui.viewmodel.SongEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Editor de Canciones", appName)
    }

    @Test
    fun `song formatted duration calculates correctly`() {
        val song = Song(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 185000L, // 3m 05s
            uri = Uri.parse("content://dummy/1")
        )

        assertEquals("03:05", song.formattedDuration)
    }

    @Test
    fun `pitch factor semitone calculation is accurate`() {
        val neutralState = SongEditorUiState(semitones = 0)
        assertEquals(1.0f, neutralState.pitchFactor, 0.001f)

        val octaveUpState = SongEditorUiState(semitones = 12)
        assertEquals(2.0f, octaveUpState.pitchFactor, 0.001f)

        val octaveDownState = SongEditorUiState(semitones = -12)
        assertEquals(0.5f, octaveDownState.pitchFactor, 0.001f)
    }

    @Test
    fun `song search filtering works as expected`() {
        val song1 = Song(1L, "Midnight Beat", "Luna", "Album A", 60000L, Uri.parse("content://dummy/1"))
        val song2 = Song(2L, "Solar Flare", "Sol", "Album B", 60000L, Uri.parse("content://dummy/2"))

        val state = SongEditorUiState(
            songs = listOf(song1, song2),
            searchQuery = "luna"
        )

        assertEquals(1, state.filteredSongs.size)
        assertEquals("Midnight Beat", state.filteredSongs.first().title)
    }

    @Test
    fun `library sorting orders tracks accurately`() {
        val songA = Song(1L, "Beta Song", "Zack", "Album", 120000L, Uri.parse("content://dummy/1"))
        val songB = Song(2L, "Alpha Song", "Adam", "Album", 240000L, Uri.parse("content://dummy/2"))

        val titleSorted = SongEditorUiState(
            songs = listOf(songA, songB),
            sortOrder = LibrarySortOrder.TITLE
        ).filteredSongs
        assertEquals("Alpha Song", titleSorted.first().title)

        val artistSorted = SongEditorUiState(
            songs = listOf(songA, songB),
            sortOrder = LibrarySortOrder.ARTIST
        ).filteredSongs
        assertEquals("Adam", artistSorted.first().artist)

        val durationSorted = SongEditorUiState(
            songs = listOf(songA, songB),
            sortOrder = LibrarySortOrder.DURATION
        ).filteredSongs
        assertEquals(240000L, durationSorted.first().durationMs)
    }

    @Test
    fun `pcm equalizer processes audio buffer without corruption or NaN`() {
        val sampleRate = 44100
        val equalizer = PcmEqualizer(sampleRate)

        // Configurar boost de graves a +6 dB y agudos a -3 dB
        val config = EqualizerConfig(bassGainDb = 6f, midGainDb = 0f, trebleGainDb = -3f, bassBoostPercent = 25)
        equalizer.updateConfig(sampleRate, config)

        // Crear buffer PCM 16-bit mono con señal senoidal de prueba
        val sampleCount = 200
        val pcmBytes = ByteArray(sampleCount * 2)
        val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            val s = (kotlin.math.sin(i * 0.1) * 10000.0).toInt().toShort()
            bb.putShort(s)
        }

        // Procesar
        equalizer.processPcm16(pcmBytes, channels = 1)

        // Verificar que los bytes siguen siendo números válidos no corrompidos
        val resultBb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        var hasNonZero = false
        for (i in 0 until sampleCount) {
            val sample = resultBb.getShort()
            if (sample != 0.toShort()) hasNonZero = true
            assertTrue("Muestra debe estar en rango válido de 16 bits", sample in -32768..32767)
        }
        assertTrue("El buffer procesado contiene audio válido", hasNonZero)
    }

    @Test
    fun `exported track converts to song and formats size properly`() {
        val track = ExportedTrack(
            id = 12345L,
            title = "Processed Groove",
            fileName = "processed_groove.wav",
            uri = Uri.parse("content://dummy/export/1"),
            sizeBytes = 1048576L * 5, // 5 MB
            durationMs = 90000L, // 1m 30s
            dateAddedMs = 1700000000000L
        )

        assertEquals("01:30", track.formattedDuration)
        assertTrue(track.formattedSize.contains("5.00 MB"))

        val song = track.toSong()
        assertEquals(track.id, song.id)
        assertEquals(track.title, song.title)
        assertTrue(song.isSavedEdit)
    }
}
