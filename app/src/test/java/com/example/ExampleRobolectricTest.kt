package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Song
import com.example.ui.viewmodel.SongEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

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
}
