package com.moviebuzz

import com.moviebuzz.app.domain.LibrarySnapshot
import com.moviebuzz.app.domain.MovieForStats
import com.moviebuzz.app.domain.StatsEngine
import com.moviebuzz.app.domain.WatchEventForStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsEngineTest {

    private fun movie(
        id: Long, title: String, year: Int? = 2020, rating: Double? = 7.0,
        runtime: Int? = 120, genres: List<String> = listOf("Drama"), watched: Boolean = true,
        director: String? = "Some Director", cast: List<String> = listOf("Actor A"), favourite: Boolean = false
    ) = MovieForStats(id, title, year, runtime, director, genres, listOf("USA"), listOf("English"), cast, rating, 7.5, favourite, watched)

    @Test
    fun `counts unique movies not sessions`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A"), movie(2, "B")),
            watchEvents = listOf(
                WatchEventForStats(1, "2024-01-01", 120),
                WatchEventForStats(1, "2024-02-01", 120),
                WatchEventForStats(2, "2024-02-02", 120)
            )
        )
        val engine = StatsEngine()
        assertEquals(2, engine.totalMoviesWatched(s))
        assertEquals(3, engine.totalViewingSessions(s))
    }

    @Test
    fun `missing runtime is excluded from hours not counted as zero`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A", runtime = 120), movie(2, "B", runtime = null)),
            watchEvents = listOf(
                WatchEventForStats(1, "2024-01-01", null),
                WatchEventForStats(2, "2024-02-01", null)
            )
        )
        assertEquals(2.0, StatsEngine().totalWatchHours(s), 0.001)
    }

    @Test
    fun `average rating ignores unrated`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A", rating = 8.0), movie(2, "B", rating = null)),
            watchEvents = emptyList()
        )
        assertEquals(8.0, StatsEngine().averagePersonalRating(s)!!, 0.001)
    }

    @Test
    fun `empty library yields null average`() {
        val s = LibrarySnapshot(emptyList(), emptyList())
        assertNull(StatsEngine().averagePersonalRating(s))
    }

    @Test
    fun `streak counts consecutive days`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A")),
            watchEvents = listOf(
                WatchEventForStats(1, "2024-01-01", null),
                WatchEventForStats(1, "2024-01-02", null),
                WatchEventForStats(1, "2024-01-03", null),
                WatchEventForStats(1, "2024-01-06", null),
                WatchEventForStats(1, "2024-01-07", null)
            )
        )
        assertEquals(3, StatsEngine().longestStreak(s))
    }

    @Test
    fun `rewatches counted per session`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A")),
            watchEvents = listOf(
                WatchEventForStats(1, "2024-01-01", null),
                WatchEventForStats(1, "2024-05-01", null),
                WatchEventForStats(1, "2024-06-01", null)
            )
        )
        val rewatched = StatsEngine().mostRewatched(s)
        assertEquals(1, rewatched.size)
        assertEquals(3, rewatched[0].second)
    }

    @Test
    fun `genre average is the mean over all rated films in that genre`() {
        val s = LibrarySnapshot(
            movies = listOf(
                movie(1, "A", genres = listOf("Drama"), rating = 9.0),
                movie(2, "B", genres = listOf("Drama"), rating = 7.0)
            ),
            watchEvents = emptyList()
        )
        val genres = StatsEngine().genreStats(s)
        assertEquals(1, genres.size)
        assertEquals(2, genres[0].count)
        // Mean over every rated Drama film: (9.0 + 7.0) / 2 = 8.0.
        assertEquals(8.0, genres[0].averageRating!!, 0.001)
        assertEquals(1.0, genres[0].share, 0.001)
    }

    @Test
    fun `genre average is null when no film in genre is rated`() {
        val s = LibrarySnapshot(
            movies = listOf(movie(1, "A", genres = listOf("Drama"), rating = null)),
            watchEvents = emptyList()
        )
        val genres = StatsEngine().genreStats(s)
        assertNull(genres[0].averageRating)
    }
}
