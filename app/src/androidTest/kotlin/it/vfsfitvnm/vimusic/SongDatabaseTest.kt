package it.vfsfitvnm.vimusic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.vfsfitvnm.vimusic.models.Song
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDatabaseTest {
    private lateinit var database: DatabaseInitializer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DatabaseInitializer::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndLikeRoundTrip() {
        val song = Song(
            id = "local-1",
            title = "Nasheed",
            artistsText = "Artist",
            durationText = "3:00",
            thumbnailUrl = null
        )
        assertTrue(database.database.insert(song) > 0)
        assertEquals(1, database.database.like(song.id, 42L))

        database.query("SELECT title, likedAt FROM Song WHERE id = ?", arrayOf(song.id)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Nasheed", cursor.getString(0))
            assertEquals(42L, cursor.getLong(1))
        }
    }
}
