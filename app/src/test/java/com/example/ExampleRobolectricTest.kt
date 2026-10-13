package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.audio.EqualizerConfig
import com.example.audio.PlayerRepeatMode
import com.example.audio.PlayerState
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.LibrarySortOrder
import com.example.ui.viewmodel.SongEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MeloTune", appName)
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
    fun `library filter by favorites and playlists works properly`() {
        val song1 = Song(1L, "Song 1", "Artist", "Album", 100000L, Uri.parse("content://dummy/1"), isFavorite = true)
        val song2 = Song(2L, "Song 2", "Artist", "Album", 100000L, Uri.parse("content://dummy/2"), isFavorite = false)

        val favState = SongEditorUiState(
            songs = listOf(song1, song2),
            libraryFilter = LibraryFilter.FAVORITES
        )
        assertEquals(1, favState.filteredSongs.size)
        assertEquals(1L, favState.filteredSongs.first().id)

        val playlistState = SongEditorUiState(
            songs = listOf(song1, song2),
            libraryFilter = LibraryFilter.PLAYLISTS,
            selectedCategory = "Mi Lista",
            playlists = mapOf("Mi Lista" to listOf(2L))
        )
        assertEquals(1, playlistState.filteredSongs.size)
        assertEquals(2L, playlistState.filteredSongs.first().id)
    }

    @Test
    fun `player state queue navigation indicators are accurate`() {
        val song1 = Song(1L, "Track 1", "Artist", "Album", 1000L, Uri.parse("content://dummy/1"))
        val song2 = Song(2L, "Track 2", "Artist", "Album", 1000L, Uri.parse("content://dummy/2"))

        val stateAtFirst = PlayerState(
            queue = listOf(song1, song2),
            currentIndex = 0,
            repeatMode = PlayerRepeatMode.OFF
        )
        assertTrue(stateAtFirst.hasNext)
        assertFalse(stateAtFirst.hasPrevious)

        val stateAtLast = PlayerState(
            queue = listOf(song1, song2),
            currentIndex = 1,
            repeatMode = PlayerRepeatMode.OFF
        )
        assertFalse(stateAtLast.hasNext)
        assertTrue(stateAtLast.hasPrevious)
    }

    @Test
    fun `repository handles playlist persistence correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SongRepository(context)

        val playlistName = "TestPlaylist_${System.currentTimeMillis()}"
        val created = repo.createPlaylist(playlistName)
        assertTrue("Playlist should be created", created)
        assertTrue("Playlist should be listed", repo.getPlaylistNames().contains(playlistName))

        repo.addSongToPlaylist(playlistName, 999L)
        val songIds = repo.getPlaylistSongIds(playlistName)
        assertTrue("Song ID 999 should be in playlist", songIds.contains(999L))

        repo.removeSongFromPlaylist(playlistName, 999L)
        val afterRemove = repo.getPlaylistSongIds(playlistName)
        assertFalse("Song ID 999 should be removed", afterRemove.contains(999L))

        repo.deletePlaylist(playlistName)
        assertFalse("Playlist should be deleted", repo.getPlaylistNames().contains(playlistName))
    }
}
